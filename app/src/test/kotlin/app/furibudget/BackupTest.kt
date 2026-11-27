package app.furibudget

import app.furibudget.core.Repeat
import app.furibudget.data.AccountEntity
import app.furibudget.data.Backup
import app.furibudget.data.BackupFile
import app.furibudget.data.BillEntity
import app.furibudget.data.CategoryEntity
import app.furibudget.data.FundEntity
import app.furibudget.data.FundMoveEntity
import app.furibudget.data.LegacyRecurrence
import app.furibudget.data.LoanDirection
import app.furibudget.data.LoanEntity
import app.furibudget.data.PersonEntity
import app.furibudget.data.RateEntity
import app.furibudget.data.TagEntity
import app.furibudget.data.TransactionSplitEntity
import app.furibudget.data.TransactionTagEntity
import app.furibudget.data.TxRole
import app.furibudget.data.TransactionEntity
import app.furibudget.data.TxKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTest {
    @Test fun roundTrip() {
        val f = BackupFile(
            accounts = listOf(AccountEntity(1, "Cash", 1, "cash", 500)),
            transactions = listOf(TransactionEntity(7, TxKind.EXPENSE, 250, 1, note = "lunch, \"big\"", epochDay = 20000)),
            bills = listOf(BillEntity(1, "Rent", TxKind.EXPENSE, app.furibudget.data.BillType.BILL, 9900, 1, repeat = Repeat.MONTHLY, anchorDay = 31, firstDueEpochDay = 20100, colorArgb = 5)),
        )
        val back = Backup.decode(Backup.encode(f))!!
        assertEquals(f.accounts, back.accounts)
        assertEquals(f.transactions, back.transactions)
        assertEquals(f.bills, back.bills)
    }

    @Test fun oldBackupRecurrencesBecomeAutoPayBills() {
        val old = BackupFile(
            version = 1, accounts = listOf(AccountEntity(1, "Cash", 1, "cash")),
            recurrences = listOf(LegacyRecurrence(1, TxKind.EXPENSE, 9900, 1, note = "Internet", repeat = Repeat.MONTHLY, anchorDay = 5, nextDueEpochDay = 20100)),
        )
        val back = Backup.decode(Backup.encode(old))!!
        assertEquals(1, back.bills.size)
        assertEquals("Internet", back.bills[0].name)
        org.junit.Assert.assertTrue(back.bills[0].autoPay)
        assertEquals(20100L, back.bills[0].firstDueEpochDay)
        assertEquals(0, back.recurrences.size)
    }

    @Test fun rejectsGarbageAndFutureVersions() {
        assertNull(Backup.decode("not json"))
        assertNull(Backup.decode("{\"version\":99}"))
    }

    @Test fun csvNeutralisesFormulas() {
        val state = BudgetState(
            allAccounts = listOf(AccountEntity(1, "Cash", 1, "cash")),
            transactions = listOf(TransactionEntity(1, TxKind.EXPENSE, 100, 1, note = "=HYPERLINK(\"x\")", epochDay = 20000)),
        )
        val row = Backup.csv(state).trim().lines()[1]
        assert(row.contains(",\"'=HYPERLINK(")) { row }
    }

    @Test fun consistencyCatchesBrokenLinks() {
        val ok = BackupFile(accounts = listOf(AccountEntity(1, "Cash", 1, "cash")), transactions = listOf(TransactionEntity(1, TxKind.EXPENSE, 5, 1, epochDay = 1)))
        org.junit.Assert.assertTrue(Backup.isConsistent(ok))
        org.junit.Assert.assertFalse(Backup.isConsistent(ok.copy(transactions = listOf(TransactionEntity(1, TxKind.EXPENSE, 5, 9, epochDay = 1)))))
        org.junit.Assert.assertFalse(Backup.isConsistent(ok.copy(transactions = listOf(TransactionEntity(1, TxKind.EXPENSE, 0, 1, epochDay = 1)))))
    }

    @Test fun csvQuotesAwkwardFields() {
        val state = BudgetState(
            allAccounts = listOf(AccountEntity(1, "Cash", 1, "cash")),
            transactions = listOf(TransactionEntity(1, TxKind.EXPENSE, 12_345, 1, note = "a,\"b\"", epochDay = 20000)),
        )
        val lines = Backup.csv(state).trim().lines()
        assertEquals("date,type,amount,account,to_account,category,note,tags,split,original_currency,original_amount,role", lines[0])
        assertEquals("2024-10-04,expense,123.45,Cash,,,\"a,\"\"b\"\"\",,,,,normal", lines[1])
    }

    @Test fun version3TablesRoundTripAndStayConsistent() {
        val f = BackupFile(
            accounts = listOf(AccountEntity(1, "Cash", 1, "cash", 500)),
            categories = listOf(CategoryEntity(1, "Food", 1, "food", TxKind.EXPENSE)),
            transactions = listOf(
                TransactionEntity(7, TxKind.EXPENSE, 1000, 1, epochDay = 20000, deletedAt = 99L, origAmountMinor = 25, origCurrency = "USD", rateMicro = 58_000_000),
                TransactionEntity(8, TxKind.EXPENSE, 500, 1, epochDay = 20001, role = TxRole.LOAN, loanId = 1),
            ),
            tags = listOf(TagEntity(1, "Trip", 5)),
            transactionTags = listOf(TransactionTagEntity(7, 1)),
            splits = listOf(TransactionSplitEntity(1, 7, 1, 600), TransactionSplitEntity(2, 7, null, 400)),
            people = listOf(PersonEntity(1, "Ana", colorArgb = 3)),
            loans = listOf(LoanEntity(1, 1, LoanDirection.LENT, 500, 20001)),
            funds = listOf(FundEntity(1, "Trip", colorArgb = 4, targetMinor = 9000, monthlyMinor = 500)),
            fundMoves = listOf(FundMoveEntity(1, 1, 500, 20001, monthKey = "2026-11")),
            rates = listOf(RateEntity("USD", 58_000_000, 123L, manual = true)),
        )
        val back = Backup.decode(Backup.encode(f))!!
        assertEquals(f.transactions, back.transactions)
        assertEquals(f.tags, back.tags)
        assertEquals(f.transactionTags, back.transactionTags)
        assertEquals(f.splits, back.splits)
        assertEquals(f.people, back.people)
        assertEquals(f.loans, back.loans)
        assertEquals(f.funds, back.funds)
        assertEquals(f.fundMoves, back.fundMoves)
        assertEquals(f.rates, back.rates)
        assertEquals(true, Backup.isConsistent(back))
        // a loan pointing at a person that is not in the file must be refused
        assertEquals(false, Backup.isConsistent(back.copy(people = emptyList())))
        assertEquals(false, Backup.isConsistent(back.copy(funds = emptyList())))
    }

    @Test fun versionTwoFileWithoutNewTablesStillLoads() {
        val v2 = """{"version":2,"accounts":[{"id":1,"name":"Cash","colorArgb":1,"icon":"cash"}],"transactions":[{"id":3,"kind":"EXPENSE","amountMinor":100,"accountId":1,"epochDay":20000}]}"""
        val back = Backup.decode(v2)!!
        assertEquals(TxRole.NORMAL, back.transactions[0].role)
        assertEquals(emptyList<PersonEntity>(), back.people)
        assertEquals(true, Backup.isConsistent(back))
    }

    @Test fun oldGoalsBecomeGoalsThatKeepTheirMoneySetAside() {
        val old = BackupFile(
            version = 3, accounts = listOf(AccountEntity(1, "Cash", 1, "cash", sortOrder = 1), AccountEntity(2, "Bank", 1, "bank", sortOrder = 0)),
            goals = listOf(app.furibudget.data.GoalEntity(1, "Laptop", 500_000, 120_000, deadlineEpochDay = 20_500, colorArgb = 7, createdAt = 86_400_000L * 20_000)),
            funds = listOf(FundEntity(5, "Trip", colorArgb = 1)),
        )
        val back = Backup.decode(Backup.encode(old))!!
        assertEquals(emptyList<app.furibudget.data.GoalEntity>(), back.goals)
        assertEquals(2, back.funds.size)
        val laptop = back.funds.first { it.name == "Laptop" }
        assertEquals(6L, laptop.id)
        assertEquals(500_000L, laptop.targetMinor)
        assertEquals(20_500L, laptop.deadlineEpochDay)
        assertEquals(2L, laptop.accountId)   // the first account by sort order
        assertEquals(app.furibudget.data.HoldMode.RESERVE, laptop.holdMode)
        assertEquals(120_000L, back.fundMoves.single { it.fundId == 6L }.amountMinor)
        assertEquals(true, Backup.isConsistent(back))
    }
}
