package app.furibudget.data

import app.furibudget.ui.theme.ThemeSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Everything the app stores, in one file the user can keep. Background photos are not included. */
@Serializable
data class BackupFile(
    val version: Int = 3,
    val exportedAt: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    /** Read-only: files from before bills existed carry these; they are turned into auto-pay bills on import. */
    val recurrences: List<LegacyRecurrence> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    val billPayments: List<BillPaymentEntity> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val rates: List<RateEntity> = emptyList(),
    val funds: List<FundEntity> = emptyList(),
    val fundMoves: List<FundMoveEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val transactionTags: List<TransactionTagEntity> = emptyList(),
    val splits: List<TransactionSplitEntity> = emptyList(),
    val theme: ThemeSpec? = null,
    val currency: String? = null,
    val startDay: Int? = null,
)

/** The pre-bills recurring transaction, kept only so older backup files still restore. */
@Serializable
data class LegacyRecurrence(
    val id: Long = 0,
    val kind: TxKind,
    val amountMinor: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String = "",
    val repeat: app.furibudget.core.Repeat,
    val anchorDay: Int,
    val nextDueEpochDay: Long,
    val endEpochDay: Long? = null,
    val active: Boolean = true,
)

object Backup {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun encode(file: BackupFile): String = json.encodeToString(file)

    /** Null when the text is not a backup this version understands. Old recurring transactions become auto-pay bills. */
    fun decode(text: String): BackupFile? {
        val f = runCatching { json.decodeFromString<BackupFile>(text) }.getOrNull()?.takeIf { it.version in 1..3 } ?: return null
        if (f.recurrences.isEmpty()) return f
        var nextId = (f.bills.maxOfOrNull { it.id } ?: 0L) + 1
        val converted = f.recurrences.map { r ->
            val cat = f.categories.firstOrNull { it.id == r.categoryId }
            BillEntity(
                id = nextId++, name = r.note.ifBlank { cat?.name ?: "Recurring" }, kind = r.kind, type = BillType.RECURRING,
                amountMinor = r.amountMinor, accountId = r.accountId, toAccountId = r.toAccountId, categoryId = r.categoryId,
                repeat = r.repeat, anchorDay = r.anchorDay, firstDueEpochDay = r.nextDueEpochDay, endEpochDay = r.endEpochDay,
                autoPay = true, remindDaysBefore = -1, note = "", colorArgb = cat?.colorArgb ?: 0xFF8B7CFF.toInt(), active = r.active,
            )
        }
        return f.copy(bills = f.bills + converted, recurrences = emptyList())
    }

    /** True when every link in the file points at a row that exists and every amount is positive, so a restore cannot half-fail. */
    fun isConsistent(f: BackupFile): Boolean {
        val accounts = f.accounts.map { it.id }
        val categories = f.categories.map { it.id }.toSet()
        if (accounts.size != accounts.toSet().size || f.categories.size != categories.size) return false
        val acc = accounts.toSet()
        return f.transactions.all { t -> t.accountId in acc && (t.toAccountId == null || t.toAccountId in acc) && (t.categoryId == null || t.categoryId in categories) && t.amountMinor > 0 } &&
            f.bills.all { r -> r.accountId in acc && (r.toAccountId == null || r.toAccountId in acc) && (r.categoryId == null || r.categoryId in categories) && r.amountMinor >= 0 } &&
            f.billPayments.all { p -> f.bills.any { it.id == p.billId } && (p.transactionId == null || f.transactions.any { it.id == p.transactionId }) } &&
            f.fundMoves.all { m -> f.funds.any { it.id == m.fundId } } &&
            f.loans.all { l -> f.people.any { it.id == l.personId } } &&
            f.transactionTags.all { tt -> f.transactions.any { it.id == tt.transactionId } && f.tags.any { it.id == tt.tagId } } &&
            f.splits.all { sp -> f.transactions.any { it.id == sp.transactionId } && (sp.categoryId == null || sp.categoryId in categories) } &&
            f.templates.all { t -> t.accountId in acc && (t.categoryId == null || t.categoryId in categories) && t.amountMinor > 0 } &&
            f.budgets.all { b -> b.categoryId == null || b.categoryId in categories } &&
            f.goals.all { it.targetMinor > 0 && it.savedMinor >= 0 }
    }

    /**
     * Spreadsheet-friendly list of transactions. Fields containing commas or quotes are quoted, and text that
     * starts with = + - @ is prefixed so a spreadsheet shows it as text instead of running it as a formula.
     */
    fun csv(state: app.furibudget.BudgetState): String {
        fun safe(s: String) = if (s.isNotEmpty() && s[0] in "=+-@\t\r") "'" + s else s
        fun q(raw: String): String {
            val s = safe(raw)
            return if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
        }
        val sb = StringBuilder("date,type,amount,account,to_account,category,note,tags,split,original_currency,original_amount,role\n")
        val tagNames = state.tags.associate { it.id to it.name }
        val tagsOf = state.transactionTags.groupBy({ it.transactionId }, { tagNames[it.tagId].orEmpty() })
        val splitsOf = state.splits.groupBy { it.transactionId }
        for (t in state.transactions.sortedBy { it.epochDay }) {
            sb.append(java.time.LocalDate.ofEpochDay(t.epochDay)).append(',')
                .append(t.kind.name.lowercase()).append(',')
                .append(state.currency.format(app.furibudget.core.Money(t.amountMinor)).replace(",", "")).append(',')
                .append(q(state.account(t.accountId)?.name.orEmpty())).append(',')
                .append(q(state.account(t.toAccountId)?.name.orEmpty())).append(',')
                .append(q(state.category(t.categoryId)?.name.orEmpty())).append(',')
                .append(q(t.note)).append(',')
                .append(q(tagsOf[t.id].orEmpty().joinToString(";"))).append(',')
                .append(q(splitsOf[t.id].orEmpty().joinToString(";") { sp -> (state.category(sp.categoryId)?.name ?: "?") + "=" + state.currency.format(app.furibudget.core.Money(sp.amountMinor)).replace(",", "") })).append(',')
                .append(q(t.origCurrency.orEmpty())).append(',')
                .append(t.origAmountMinor?.let { m -> app.furibudget.core.Currency.all.firstOrNull { it.code == t.origCurrency }?.format(app.furibudget.core.Money(m))?.replace(",", "") ?: m.toString() }.orEmpty()).append(',')
                .append(t.role.name.lowercase()).append('\n')
        }
        return sb.toString()
    }
}
