package app.furibudget.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.furibudget.core.Repeat
import kotlinx.serialization.Serializable

@Serializable
enum class TxKind { INCOME, EXPENSE, TRANSFER }

@Entity(tableName = "accounts")
@Serializable
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val icon: String,
    /** Money in the account before the first recorded transaction, in minor units. */
    val startBalance: Long = 0,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Entity(tableName = "categories")
@Serializable
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val icon: String,
    /** INCOME or EXPENSE; a category never holds transfers. */
    val kind: TxKind,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(AccountEntity::class, ["id"], ["toAccountId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("accountId"), Index("categoryId"), Index("toAccountId"), Index("epochDay")],
)
@Serializable
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: TxKind,
    /** Always positive; the kind gives the direction. Minor units. */
    val amountMinor: Long,
    val accountId: Long,
    /** Set only for transfers. */
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String = "",
    /** LocalDate.toEpochDay(). */
    val epochDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Monthly spending limit. categoryId null is the overall budget for the month. */
@Entity(
    tableName = "budgets",
    foreignKeys = [ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("categoryId", unique = true)],
)
@Serializable
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val limitMinor: Long,
    /** Unspent money from last month is added to this month's limit. */
    val rollover: Boolean = false,
)

@Entity(tableName = "goals")
@Serializable
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetMinor: Long,
    val savedMinor: Long = 0,
    /** LocalDate.toEpochDay(), or null for no deadline. */
    val deadlineEpochDay: Long? = null,
    val icon: String = "goal",
    val colorArgb: Int,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A transaction that repeats. [nextDueEpochDay] is the next date still to be posted. */
@Serializable
@Entity(
    tableName = "recurrences",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("accountId"), Index("categoryId")],
)
data class RecurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: TxKind,
    val amountMinor: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String = "",
    val repeat: Repeat,
    /** Day of month the series is anchored to, so a 31st survives short months. */
    val anchorDay: Int,
    val nextDueEpochDay: Long,
    val endEpochDay: Long? = null,
    val active: Boolean = true,
)
