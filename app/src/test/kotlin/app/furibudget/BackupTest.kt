package app.furibudget

import app.furibudget.core.Repeat
import app.furibudget.data.AccountEntity
import app.furibudget.data.Backup
import app.furibudget.data.BackupFile
import app.furibudget.data.RecurrenceEntity
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
            recurrences = listOf(RecurrenceEntity(1, TxKind.EXPENSE, 9900, 1, repeat = Repeat.MONTHLY, anchorDay = 31, nextDueEpochDay = 20100)),
        )
        val back = Backup.decode(Backup.encode(f))!!
        assertEquals(f.accounts, back.accounts)
        assertEquals(f.transactions, back.transactions)
        assertEquals(f.recurrences, back.recurrences)
    }

    @Test fun rejectsGarbageAndFutureVersions() {
        assertNull(Backup.decode("not json"))
        assertNull(Backup.decode("{\"version\":99}"))
    }

    @Test fun csvQuotesAwkwardFields() {
        val state = BudgetState(
            accounts = listOf(AccountEntity(1, "Cash", 1, "cash")),
            transactions = listOf(TransactionEntity(1, TxKind.EXPENSE, 12_345, 1, note = "a,\"b\"", epochDay = 20000)),
        )
        val lines = Backup.csv(state).trim().lines()
        assertEquals("date,type,amount,account,to_account,category,note", lines[0])
        assertEquals("2024-10-04,expense,123.45,Cash,,,\"a,\"\"b\"\"\"", lines[1])
    }
}
