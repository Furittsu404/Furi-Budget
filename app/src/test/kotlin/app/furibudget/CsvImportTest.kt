package app.furibudget

import app.furibudget.core.Currency
import app.furibudget.data.AccountEntity
import app.furibudget.data.Backup
import app.furibudget.data.CategoryEntity
import app.furibudget.data.CsvImport
import app.furibudget.data.TagEntity
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TxKind
import app.furibudget.data.TxRole
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImportTest {
    private val php = Currency.PHP
    private val cash = AccountEntity(1, "Cash", 0, "cash")
    private val bank = AccountEntity(2, "Bank", 0, "bank")
    private val food = CategoryEntity(1, "Food", 0, "food", TxKind.EXPENSE)
    private val salary = CategoryEntity(2, "Salary", 0, "work", TxKind.INCOME)
    private fun plan(csv: String, existing: List<TransactionEntity> = emptyList(), tags: List<TagEntity> = emptyList()) =
        CsvImport.plan(csv, php, listOf(cash, bank), listOf(food, salary), tags, existing)

    private val header = "date,type,amount,account,to_account,category,note,tags,split,original_currency,original_amount,role\n"

    @Test fun readsTheFileExportWrites() {
        val csv = header +
            "2026-11-01,expense,250.50,Cash,,Food,\"lunch, big\",trip;work,,,,normal\n" +
            "2026-11-02,income,25000,Bank,,Salary,,,,,,normal\n" +
            "2026-11-03,transfer,1000,Bank,Cash,,to cash,,,,,normal\n"
        val p = plan(csv)
        assertEquals(3, p.rows.size)
        assertTrue(p.errors.isEmpty())
        assertEquals(25_050L, p.rows[0].amountMinor)
        assertEquals("lunch, big", p.rows[0].note)
        assertEquals(listOf("trip", "work"), p.rows[0].tags)
        assertEquals(TxKind.TRANSFER, p.rows[2].kind)
        assertEquals("Cash", p.rows[2].toAccount)
        assertEquals(listOf("trip", "work"), p.newTags)
    }

    @Test fun exportThenImportRoundTrips() {
        val tx = TransactionEntity(5, TxKind.EXPENSE, 12_345, 1, categoryId = 1, note = "=SUM(A1)", epochDay = LocalDate.of(2026, 11, 5).toEpochDay())
        val state = BudgetState(currency = php, allAccounts = listOf(cash, bank), accounts = listOf(cash, bank), categories = listOf(food, salary), transactions = listOf(tx))
        val back = plan(Backup.csv(state))
        assertEquals(1, back.rows.size)
        assertEquals("=SUM(A1)", back.rows[0].note)   // the formula guard apostrophe is undone
        assertEquals(12_345L, back.rows[0].amountMinor)
        assertEquals(tx.epochDay, back.rows[0].epochDay)
        assertEquals("Cash", back.rows[0].account)
    }

    @Test fun secondImportSkipsWhatIsAlreadyThere() {
        val existing = listOf(TransactionEntity(1, TxKind.EXPENSE, 25_050, 1, note = "lunch", epochDay = LocalDate.of(2026, 11, 1).toEpochDay()))
        val csv = header + "2026-11-01,expense,250.50,cash,,Food,lunch,,,,,normal\n2026-11-01,expense,250.50,cash,,Food,lunch,,,,,normal\n"
        val p = plan(csv, existing)
        assertEquals(1, p.duplicates)   // one copy exists already, the second is a genuine second purchase
        assertEquals(1, p.rows.size)
    }

    @Test fun unknownNamesAreCreatedAndBadRowsAreReported() {
        val csv = header +
            "2026-11-01,expense,10,Wallet,,Snacks,,,,,,normal\n" +
            "not a date,expense,10,Cash,,,,,,,,normal\n" +
            "2026-11-02,gift,10,Cash,,,,,,,,normal\n" +
            "2026-11-03,expense,abc,Cash,,,,,,,,normal\n" +
            "2026-11-04,transfer,10,Cash,Cash,,,,,,,normal\n"
        val p = plan(csv)
        assertEquals(1, p.rows.size)
        assertEquals(4, p.errors.size)
        assertEquals(listOf("Wallet"), p.newAccounts)
        assertEquals(listOf("Snacks" to TxKind.EXPENSE), p.newCategories)
    }

    @Test fun splitsMustAddUpAndForeignAmountsKeepTheirRate() {
        val ok = plan(header + "2026-11-01,expense,100,Cash,,,,,Food=60;Other=40,USD,1.72,normal\n")
        assertEquals(listOf("Food" to 6000L, "Other" to 4000L), ok.rows[0].splits)
        assertEquals("USD", ok.rows[0].origCurrency)
        assertEquals(172L, ok.rows[0].origAmountMinor)
        assertTrue((ok.rows[0].rateMicro ?: 0) in 58_000_000L..58_300_000L)
        val bad = plan(header + "2026-11-01,expense,100,Cash,,Food,,,Food=60;Other=30,,,normal\n")
        assertTrue(bad.rows[0].splits.isEmpty())
    }

    @Test fun looserFilesWork() {
        // other apps: signed amounts, no type column, a day-first date, quoted multi-line notes, a BOM
        val csv = "\uFEFFDate,Amount,Description\n05/11/2026,\"-1,250.00\",\"line one\nline two\"\n06/11/2026,500,Refund\n"
        val p = plan(csv)
        assertEquals(2, p.rows.size)
        assertEquals(TxKind.EXPENSE, p.rows[0].kind)
        assertEquals(125_000L, p.rows[0].amountMinor)
        assertEquals("line one\nline two", p.rows[0].note)
        assertEquals(TxKind.INCOME, p.rows[1].kind)
        assertEquals(LocalDate.of(2026, 11, 5).toEpochDay(), p.rows[0].epochDay)
        assertEquals("Cash", p.rows[0].account.ifBlank { "Cash" })
    }

    @Test fun nonNormalRolesBecomeAdjustments() {
        val p = plan(header + "2026-11-01,expense,100,Cash,,,Lent,,,,,loan\n")
        assertEquals(TxRole.ADJUSTMENT, p.rows[0].role)
    }

    @Test fun emptyOrHeaderlessFilesAreRefused() {
        assertTrue(plan("").rows.isEmpty())
        assertTrue(plan("foo,bar\n1,2\n").errors.isNotEmpty())
    }
}
