package app.furibudget

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import app.furibudget.data.BudgetDatabase
import app.furibudget.data.Migrations
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), BudgetDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory())

    @Test
    fun v1ToV2KeepsDataAndTurnsRecurrencesIntoAutoPayBills() {
        helper.createDatabase("mig", 1).apply {
            execSQL("INSERT INTO accounts(id,name,colorArgb,icon,startBalance,sortOrder,archived) VALUES(1,'Cash',-1,'cash',500,0,0)")
            execSQL("INSERT INTO categories(id,name,colorArgb,icon,kind,sortOrder,archived) VALUES(3,'Bills',-65536,'bills','EXPENSE',0,0)")
            execSQL("INSERT INTO transactions(id,kind,amountMinor,accountId,toAccountId,categoryId,note,epochDay,createdAt) VALUES(7,'EXPENSE',2500,1,NULL,3,'lunch',20000,1)")
            execSQL("INSERT INTO recurrences(id,kind,amountMinor,accountId,toAccountId,categoryId,note,`repeat`,anchorDay,nextDueEpochDay,endEpochDay,active) VALUES(1,'EXPENSE',29900,1,NULL,3,'Internet','MONTHLY',5,20100,NULL,1)")
            execSQL("INSERT INTO recurrences(id,kind,amountMinor,accountId,toAccountId,categoryId,note,`repeat`,anchorDay,nextDueEpochDay,endEpochDay,active) VALUES(2,'INCOME',500000,1,NULL,NULL,'','WEEKLY',1,20110,20200,0)")
            close()
        }
        val db = helper.runMigrationsAndValidate("mig", 2, true, Migrations.MIGRATION_1_2)

        db.query("SELECT COUNT(*) FROM transactions").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        db.query("SELECT name, type, autoPay, remindDaysBefore, firstDueEpochDay, `repeat`, colorArgb, active FROM bills ORDER BY id").use {
            assertEquals(2, it.count)
            it.moveToFirst()
            assertEquals("Internet", it.getString(0)); assertEquals("RECURRING", it.getString(1)); assertEquals(1, it.getInt(2))
            assertEquals(-1, it.getInt(3)); assertEquals(20100L, it.getLong(4)); assertEquals("MONTHLY", it.getString(5)); assertEquals(-65536, it.getInt(6))
            it.moveToNext()
            assertEquals("Recurring", it.getString(0)); assertEquals(-7667713, it.getInt(6)); assertEquals(0, it.getInt(7))
        }
        db.query("SELECT name FROM sqlite_master WHERE name = 'recurrences'").use { assertEquals(0, it.count) }
    }

    @Test
    fun v2ToV3KeepsRowsAndAddsSoftDeleteAndRole() {
        helper.createDatabase("mig3", 2).apply {
            execSQL("INSERT INTO accounts(id,name,colorArgb,icon,startBalance,sortOrder,archived) VALUES(1,'Cash',-1,'cash',500,0,0)")
            execSQL("INSERT INTO categories(id,name,colorArgb,icon,kind,sortOrder,archived) VALUES(3,'Bills',-65536,'bills','EXPENSE',0,0)")
            execSQL("INSERT INTO transactions(id,kind,amountMinor,accountId,toAccountId,categoryId,note,epochDay,createdAt) VALUES(7,'EXPENSE',2500,1,NULL,3,'lunch',20000,1)")
            execSQL("INSERT INTO bills(id,name,kind,type,amountMinor,accountId,toAccountId,categoryId,`repeat`,repeatEvery,anchorDay,firstDueEpochDay,endEpochDay,paymentsTotal,autoPay,remindDaysBefore,note,icon,colorArgb,active) VALUES(1,'Rent','EXPENSE','BILL',1200000,1,NULL,3,'MONTHLY',1,5,20100,NULL,NULL,0,3,'','bills',-1,1)")
            execSQL("INSERT INTO bill_payments(id,billId,dueEpochDay,status,paidEpochDay,amountMinor,transactionId) VALUES(1,1,20100,'PAID',20100,1200000,7)")
            execSQL("INSERT INTO goals(id,name,targetMinor,savedMinor,deadlineEpochDay,icon,colorArgb,createdAt) VALUES(1,'Trip',100000,5000,NULL,'goal',-1,1)")
            close()
        }
        val db = helper.runMigrationsAndValidate("mig3", 3, true, Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
        db.query("SELECT role, loanId, origAmountMinor, deletedAt FROM transactions WHERE id = 7").use {
            it.moveToFirst(); assertEquals("NORMAL", it.getString(0)); assertEquals(true, it.isNull(1)); assertEquals(true, it.isNull(2)); assertEquals(true, it.isNull(3))
        }
        db.query("SELECT deletedAt FROM bills").use { it.moveToFirst(); assertEquals(true, it.isNull(0)) }
        db.query("SELECT deletedAt FROM goals").use { it.moveToFirst(); assertEquals(true, it.isNull(0)) }
        db.query("SELECT COUNT(*) FROM bill_payments").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        for (t in listOf("tags", "transaction_tags", "transaction_splits", "people", "loans", "funds", "fund_moves", "rates")) {
            db.query("SELECT name FROM sqlite_master WHERE name = '$t'").use { assertEquals(t, 1, it.count) }
        }
    }

    @Test
    fun v3ToV4PutsExistingFundsInTheFirstAccount() {
        helper.createDatabase("mig4", 3).apply {
            execSQL("INSERT INTO accounts(id,name,colorArgb,icon,startBalance,sortOrder,archived) VALUES(1,'Cash',-1,'cash',500,1,0)")
            execSQL("INSERT INTO accounts(id,name,colorArgb,icon,startBalance,sortOrder,archived) VALUES(2,'Bank',-1,'bank',900,0,0)")
            execSQL("INSERT INTO funds(id,name,icon,colorArgb,targetMinor,monthlyMinor,deletedAt) VALUES(1,'Trip','savings',-1,100000,NULL,NULL)")
            execSQL("INSERT INTO fund_moves(id,fundId,amountMinor,epochDay,note,transactionId,monthKey) VALUES(1,1,5000,20000,'',NULL,NULL)")
            close()
        }
        val db = helper.runMigrationsAndValidate("mig4", 4, true, Migrations.MIGRATION_3_4)
        // sortOrder 0 is the Bank, so that is "first".
        db.query("SELECT accountId FROM funds WHERE id = 1").use { it.moveToFirst(); assertEquals(2, it.getInt(0)) }
        db.query("SELECT amountMinor FROM fund_moves").use { it.moveToFirst(); assertEquals(5000L, it.getLong(0)) }
    }
}
