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
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class, BudgetEntity::class, GoalEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun transactions(): TransactionDao
    abstract fun budgets(): BudgetDao
    abstract fun goals(): GoalDao

    companion object {
        fun create(context: Context): BudgetDatabase =
            Room.databaseBuilder(context, BudgetDatabase::class.java, "budget.db")
                // Pre-release only: replaced by real migrations before the first sideloaded build with real data.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
