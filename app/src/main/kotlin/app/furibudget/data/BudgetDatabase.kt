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
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class, BudgetEntity::class, GoalEntity::class, RecurrenceEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun transactions(): TransactionDao
    abstract fun budgets(): BudgetDao
    abstract fun goals(): GoalDao
    abstract fun recurrences(): RecurrenceDao

    companion object {
        fun create(context: Context): BudgetDatabase =
            Room.databaseBuilder(context, BudgetDatabase::class.java, "budget.db")
                // Schema v1 is the baseline (schemas/ holds the exported JSON). Every later change needs a real Migration.
                // Only a downgrade, which can happen when an older build is installed over a newer one, may drop the tables.
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
    }
}
