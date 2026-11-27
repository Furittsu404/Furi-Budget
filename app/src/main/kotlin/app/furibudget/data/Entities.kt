package app.furibudget.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.furibudget.core.Repeat
import kotlinx.serialization.Serializable

@Serializable
enum class TxKind { INCOME, EXPENSE, TRANSFER }

/** Why a transaction exists. Only NORMAL counts as spending or income in totals, Insights and budgets. */
@Serializable
/** GOAL: money put into or taken out of a savings goal. It moves an account balance but is not spending or income. */
enum class TxRole { NORMAL, ADJUSTMENT, LOAN, REPAYMENT, GOAL }

@Serializable
enum class LoanDirection { LENT, OWED }

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
    @ColumnInfo(defaultValue = "NORMAL") val role: TxRole = TxRole.NORMAL,
    /** The loan this money movement belongs to (lending, borrowing or a repayment). */
    val loanId: Long? = null,
    /** For foreign-currency entries: what was typed, in which currency, and the rate used (main currency per 1 unit, times 1,000,000). */
    val origAmountMinor: Long? = null,
    val origCurrency: String? = null,
    val rateMicro: Long? = null,
    /** Epoch millis when moved to Recently deleted; null while live. */
    val deletedAt: Long? = null,
)

@Serializable
enum class BudgetPeriod { MONTH, WEEK }

/** Spending limit for a month or a week. categoryId null is the overall budget for the month. */
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
    val period: BudgetPeriod = BudgetPeriod.MONTH,
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
    val deletedAt: Long? = null,
)

@Serializable
enum class BillType { BILL, SUBSCRIPTION, INSTALMENT, LOAN, INCOME, RECURRING }

@Serializable
enum class PayStatus { PAID, SKIPPED }

/**
 * Something that falls due on a schedule: a bill, a subscription, an instalment, expected income, or a
 * recurring transaction. Due dates are worked out from the schedule; only what happened to each one
 * (paid or skipped) is stored, in [BillPaymentEntity]. With [autoPay] on, the app records it on the due date.
 */
@Serializable
@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("accountId"), Index("categoryId")],
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** EXPENSE is a payment out, INCOME something expected in, TRANSFER a move between accounts. */
    val kind: TxKind = TxKind.EXPENSE,
    val type: BillType = BillType.BILL,
    /** Expected amount in minor units; 0 means it changes every time and is typed when paying. */
    val amountMinor: Long = 0,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val repeat: Repeat = Repeat.MONTHLY,
    /** Every N days, weeks, months or years: 2 weekly is every other week. */
    val repeatEvery: Int = 1,
    /** Day of month the series is anchored to, so a 31st survives short months. */
    val anchorDay: Int,
    val firstDueEpochDay: Long,
    val endEpochDay: Long? = null,
    /** Instalments: the series ends after this many payments. */
    val paymentsTotal: Int? = null,
    val autoPay: Boolean = false,
    /** Days before the due date to remind; below zero turns reminders off for this bill. */
    val remindDaysBefore: Int = 3,
    val note: String = "",
    val icon: String = "bills",
    val colorArgb: Int,
    val active: Boolean = true,
    val deletedAt: Long? = null,
)

/** What happened to one due date of a bill. Absence of a row means still waiting. */
@Serializable
@Entity(
    tableName = "bill_payments",
    foreignKeys = [
        ForeignKey(BillEntity::class, ["id"], ["billId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TransactionEntity::class, ["id"], ["transactionId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("billId", "dueEpochDay", unique = true), Index("transactionId")],
)
data class BillPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val dueEpochDay: Long,
    val status: PayStatus,
    val paidEpochDay: Long,
    val amountMinor: Long = 0,
    /** The expense this payment recorded; null for skipped dates. */
    val transactionId: Long? = null,
)

/** Remembers which reminder stages were already shown so each fires once. Stage: 0 early, 1 due today, 2 overdue 1 day, 3 overdue 7 days. */
@Entity(
    tableName = "bill_alerts",
    primaryKeys = ["billId", "dueEpochDay", "stage"],
    foreignKeys = [ForeignKey(BillEntity::class, ["id"], ["billId"], onDelete = ForeignKey.CASCADE)],
)
data class BillAlertEntity(val billId: Long, val dueEpochDay: Long, val stage: Int)

/** A saved transaction you add often, such as a jeep fare. */
@Serializable
@Entity(
    tableName = "templates",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("accountId"), Index("categoryId")],
)
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: TxKind,
    val amountMinor: Long,
    val accountId: Long,
    val categoryId: Long? = null,
    val note: String = "",
)

// ---- Tables for tags, splits, people, funds and exchange rates (their screens arrive milestone by milestone)

@Serializable
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
)

@Serializable
@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tagId"],
    foreignKeys = [
        ForeignKey(TransactionEntity::class, ["id"], ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TagEntity::class, ["id"], ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tagId")],
)
data class TransactionTagEntity(val transactionId: Long, val tagId: Long)

/** One slice of a split transaction. The parent's category is null and the slices add up to its amount. */
@Serializable
@Entity(
    tableName = "transaction_splits",
    foreignKeys = [
        ForeignKey(TransactionEntity::class, ["id"], ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("transactionId"), Index("categoryId")],
)
data class TransactionSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val categoryId: Long? = null,
    val amountMinor: Long,
    val note: String = "",
)

@Serializable
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "person",
    val colorArgb: Int,
    val note: String = "",
    val deletedAt: Long? = null,
)

/** Money lent to someone or borrowed from them. Repayments are transactions with the same loanId. */
@Serializable
@Entity(
    tableName = "loans",
    foreignKeys = [ForeignKey(PersonEntity::class, ["id"], ["personId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personId")],
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val direction: LoanDirection,
    val principalMinor: Long,
    val openedEpochDay: Long,
    val dueEpochDay: Long? = null,
    val note: String = "",
    val deletedAt: Long? = null,
)

/** A sinking fund: money reserved inside your accounts for a future cost. */
@Serializable
@Entity(tableName = "funds")
data class FundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "savings",
    val colorArgb: Int,
    val targetMinor: Long? = null,
    /** Reserved automatically each month; null for manual funds. */
    val monthlyMinor: Long? = null,
    val deletedAt: Long? = null,
)

/** Money put into (positive) or taken out of (negative) a fund. monthKey ("2026-11") makes the monthly reserve idempotent. */
@Serializable
@Entity(
    tableName = "fund_moves",
    foreignKeys = [ForeignKey(FundEntity::class, ["id"], ["fundId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("fundId", "monthKey", unique = true), Index("transactionId")],
)
data class FundMoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fundId: Long,
    val amountMinor: Long,
    val epochDay: Long,
    val note: String = "",
    val transactionId: Long? = null,
    val monthKey: String? = null,
)

/** Cached exchange rate: how many units of the main currency 1 unit of [code] is worth, times 1,000,000 (see core Rates). */
@Serializable
@Entity(tableName = "rates")
data class RateEntity(
    @PrimaryKey val code: String,
    val perBaseMicro: Long,
    val fetchedAt: Long,
    /** True when the user typed this rate; online updates never overwrite it. */
    val manual: Boolean = false,
)
