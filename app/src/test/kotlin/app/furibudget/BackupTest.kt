package app.furibudget

import app.furibudget.core.Repeat
import app.furibudget.data.AccountEntity
import app.furibudget.data.Backup
import app.furibudget.data.BackupFile
import app.furibudget.data.BillEntity
import app.furibudget.data.LegacyRecurrence
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
        assertEquals("date,type,amount,account,to_account,category,note,tags,split,original_currency,original_amount", lines[0])
        assertEquals("2024-10-04,expense,123.45,Cash,,,\"a,\"\"b\"\"\",,,,", lines[1])
    }
}
