package app.furibudget.data

import app.furibudget.ui.theme.ThemeSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Everything the app stores, in one file the user can keep. Background photos are not included. */
@Serializable
data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val recurrences: List<RecurrenceEntity> = emptyList(),
    val theme: ThemeSpec? = null,
    val currency: String? = null,
    val startDay: Int? = null,
)

object Backup {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun encode(file: BackupFile): String = json.encodeToString(file)

    /** Null when the text is not a backup this version understands. */
    fun decode(text: String): BackupFile? = runCatching { json.decodeFromString<BackupFile>(text) }.getOrNull()?.takeIf { it.version in 1..1 }

    /** True when every link in the file points at a row that exists and every amount is positive, so a restore cannot half-fail. */
    fun isConsistent(f: BackupFile): Boolean {
        val accounts = f.accounts.map { it.id }
        val categories = f.categories.map { it.id }.toSet()
        if (accounts.size != accounts.toSet().size || f.categories.size != categories.size) return false
        val acc = accounts.toSet()
        return f.transactions.all { t -> t.accountId in acc && (t.toAccountId == null || t.toAccountId in acc) && (t.categoryId == null || t.categoryId in categories) && t.amountMinor > 0 } &&
            f.recurrences.all { r -> r.accountId in acc && (r.toAccountId == null || r.toAccountId in acc) && (r.categoryId == null || r.categoryId in categories) && r.amountMinor > 0 } &&
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
        val sb = StringBuilder("date,type,amount,account,to_account,category,note\n")
        for (t in state.transactions.sortedBy { it.epochDay }) {
            sb.append(java.time.LocalDate.ofEpochDay(t.epochDay)).append(',')
                .append(t.kind.name.lowercase()).append(',')
                .append(state.currency.format(app.furibudget.core.Money(t.amountMinor)).replace(",", "")).append(',')
                .append(q(state.account(t.accountId)?.name.orEmpty())).append(',')
                .append(q(state.account(t.toAccountId)?.name.orEmpty())).append(',')
                .append(q(state.category(t.categoryId)?.name.orEmpty())).append(',')
                .append(q(t.note)).append('\n')
        }
        return sb.toString()
    }
}
