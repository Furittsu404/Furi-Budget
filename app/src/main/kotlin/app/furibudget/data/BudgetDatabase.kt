package app.furibudget.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun kindToString(k: TxKind): String = k.name
    @TypeConverter fun stringToKind(s: String): TxKind = TxKind.valueOf(s)
}

@Database(
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class, BudgetEntity::class, GoalEntity::class, BillEntity::class, BillPaymentEntity::class, BillAlertEntity::class, TemplateEntity::class,
        TagEntity::class, TransactionTagEntity::class, TransactionSplitEntity::class, PersonEntity::class, LoanEntity::class,
        FundEntity::class, FundMoveEntity::class, RateEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun transactions(): TransactionDao
    abstract fun budgets(): BudgetDao
    abstract fun goals(): GoalDao
    abstract fun bills(): BillDao
    abstract fun billPayments(): BillPaymentDao
    abstract fun billAlerts(): BillAlertDao
    abstract fun templates(): TemplateDao
    abstract fun tags(): TagDao
    abstract fun people(): PersonDao
    abstract fun funds(): FundDao
    abstract fun rates(): RateDao
    abstract fun fundMoves(): FundMoveDao
    abstract fun loans(): LoanDao
    abstract fun transactionTags(): TransactionTagDao
    abstract fun splits(): SplitDao

    companion object {
        fun create(context: Context): BudgetDatabase =
            Room.databaseBuilder(context, BudgetDatabase::class.java, "budget.db")
                .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3, Migrations.MIGRATION_3_4, Migrations.MIGRATION_4_5)
                // Schema v1 is the baseline (schemas/ holds the exported JSON). Every later change needs a real Migration.
                // Only a downgrade, which can happen when an older build is installed over a newer one, may drop the tables.
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
    }
}
