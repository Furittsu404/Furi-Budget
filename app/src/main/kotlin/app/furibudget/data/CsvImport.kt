package app.furibudget.data

import app.furibudget.core.Currency
import app.furibudget.core.Money
import app.furibudget.core.Rates
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Reads the transaction CSV that Export writes (and similar files with the same column names). Reading is done
 * in two steps: [plan] checks every row and says what would happen, [BudgetRepository.importCsv] then does it.
 */
object CsvImport {
    const val MAX_ROWS = 20_000

    /** One row that passed every check. Names are kept as text; accounts, categories and tags are matched or created when importing. */
    data class Row(
        val line: Int, val epochDay: Long, val kind: TxKind, val amountMinor: Long,
        val account: String, val toAccount: String?, val category: String?, val note: String,
        val tags: List<String>, val splits: List<Pair<String, Long>>,
        val origCurrency: String?, val origAmountMinor: Long?, val rateMicro: Long?, val role: TxRole,
    )

    data class Plan(
        val rows: List<Row>, val errors: List<String>, val duplicates: Int,
        val newAccounts: List<String>, val newCategories: List<Pair<String, TxKind>>, val newTags: List<String>,
    ) {
        val isEmpty: Boolean get() = rows.isEmpty()
    }

    /** RFC 4180 style: commas, quoted fields, doubled quotes and line breaks inside quotes. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        val src = text.removePrefix("\uFEFF")
        while (i < src.length) {
            val ch = src[i]
            when {
                quoted && ch == '"' && i + 1 < src.length && src[i + 1] == '"' -> { cell.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                !quoted && ch == ',' -> { row.add(cell.toString()); cell.setLength(0) }
                !quoted && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && i + 1 < src.length && src[i + 1] == '\n') i++
                    row.add(cell.toString()); cell.setLength(0)
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = ArrayList()
                }
                else -> cell.append(ch)
            }
            i++
        }
        row.add(cell.toString())
        if (row.any { it.isNotBlank() }) rows.add(row)
        return rows
    }

    private val aliases = mapOf(
        "date" to listOf("date", "day", "when"),
        "type" to listOf("type", "kind", "direction"),
        "amount" to listOf("amount", "value", "sum"),
        "account" to listOf("account", "from_account", "from account", "wallet"),
        "to_account" to listOf("to_account", "to account", "transfer_to"),
        "category" to listOf("category", "categories"),
        "note" to listOf("note", "notes", "description", "memo", "title"),
        "tags" to listOf("tags", "tag", "labels"),
        "split" to listOf("split", "splits"),
        "original_currency" to listOf("original_currency", "orig_currency"),
        "original_amount" to listOf("original_amount", "orig_amount"),
        "role" to listOf("role"),
    )

    /** Text that Export prefixed with an apostrophe so a spreadsheet would not run it as a formula. */
    private fun clean(s: String): String {
        val t = s.trim()
        return if (t.length > 1 && t[0] == '\'' && t[1] in "=+-@\t\r") t.substring(1) else t
    }

    private fun parseDate(s: String): LocalDate? {
        val t = s.trim()
        return runCatching { LocalDate.parse(t) }.getOrNull()
            ?: runCatching { LocalDate.parse(t, DateTimeFormatter.ofPattern("yyyy/M/d")) }.getOrNull()
            ?: runCatching { LocalDate.parse(t, DateTimeFormatter.ofPattern("d/M/yyyy")) }.getOrNull()
    }

    /** "1,250.5", "-₱12.00" or "(12.00)" as minor units of [cur], rounded half up. Returns null if it is not a number. */
    fun parseAmount(raw: String, cur: Currency): Long? {
        var t = raw.trim()
        val negative = t.startsWith("-") || t.startsWith("−") || (t.startsWith("(") && t.endsWith(")"))
        t = t.replace(cur.symbol, "").replace(",", "").replace(" ", "").replace("(", "").replace(")", "").replace("-", "").replace("−", "").replace("+", "")
        val d = t.toBigDecimalOrNull() ?: return null
        val minor = d.movePointRight(cur.decimals).setScale(0, RoundingMode.HALF_UP).toLong()
        return if (negative) -minor else minor
    }

    private fun typeOf(s: String): TxKind? = when (s.trim().lowercase()) {
        "income", "in", "credit", "deposit", "+" -> TxKind.INCOME
        "expense", "out", "debit", "spending", "withdrawal", "-" -> TxKind.EXPENSE
        "transfer" -> TxKind.TRANSFER
        else -> null
    }

    /** Checks every row of [text] against what already exists and says what an import would do. */
    fun plan(
        text: String, currency: Currency, accounts: List<AccountEntity>, categories: List<CategoryEntity>,
        tags: List<TagEntity>, existing: List<TransactionEntity>,
    ): Plan {
        val table = parseCsv(text)
        if (table.size < 2) return Plan(emptyList(), listOf("The file has no rows."), 0, emptyList(), emptyList(), emptyList())
        val header = table[0].map { it.trim().lowercase() }
        fun col(name: String) = aliases.getValue(name).firstNotNullOfOrNull { a -> header.indexOf(a).takeIf { it >= 0 } } ?: -1
        val cDate = col("date"); val cType = col("type"); val cAmount = col("amount"); val cAccount = col("account")
        val cTo = col("to_account"); val cCat = col("category"); val cNote = col("note"); val cTags = col("tags")
        val cSplit = col("split"); val cOrigCur = col("original_currency"); val cOrigAmt = col("original_amount"); val cRole = col("role")
        if (cDate < 0 || cAmount < 0) return Plan(emptyList(), listOf("The first row must name at least a date and an amount column."), 0, emptyList(), emptyList(), emptyList())

        val errors = ArrayList<String>()
        val rows = ArrayList<Row>()
        val accNames = accounts.associateBy { it.name.trim().lowercase() }
        val catNames = categories.associateBy { it.name.trim().lowercase() to it.kind }
        val tagNames = tags.map { it.name.trim().lowercase() }.toSet()
        val newAcc = LinkedHashMap<String, String>()
        val newCat = LinkedHashMap<Pair<String, TxKind>, String>()
        val newTag = LinkedHashMap<String, String>()
        fun cell(r: List<String>, i: Int) = if (i >= 0 && i < r.size) clean(r[i]) else ""

        // Existing transactions, counted by what makes one look like another, so a second import of the same file adds nothing.
        val accById = accounts.associateBy { it.id }
        fun key(day: Long, kind: TxKind, amount: Long, account: String, note: String) = "$day|$kind|$amount|${account.trim().lowercase()}|${note.trim().lowercase()}"
        val seen = HashMap<String, Int>()
        for (t in existing) { val k = key(t.epochDay, t.kind, t.amountMinor, accById[t.accountId]?.name.orEmpty(), t.note); seen[k] = (seen[k] ?: 0) + 1 }
        var duplicates = 0

        for (i in 1 until table.size) {
            val r = table[i]
            val line = i + 1
            if (rows.size >= MAX_ROWS) { errors.add("Stopped at $MAX_ROWS rows."); break }
            val date = parseDate(cell(r, cDate)) ?: run { errors.add("Line $line: \"${cell(r, cDate)}\" is not a date (use 2026-11-27)."); null } ?: continue
            val signed = parseAmount(cell(r, cAmount), currency) ?: run { errors.add("Line $line: \"${cell(r, cAmount)}\" is not an amount."); null } ?: continue
            val kind = if (cType >= 0) typeOf(cell(r, cType)) ?: run { errors.add("Line $line: type \"${cell(r, cType)}\" is not income, expense or transfer."); null } ?: continue
                else if (signed < 0) TxKind.EXPENSE else TxKind.INCOME
            val amount = kotlin.math.abs(signed)
            if (amount == 0L) { errors.add("Line $line: the amount is zero."); continue }
            val accountName = cell(r, cAccount).ifBlank { accounts.firstOrNull()?.name.orEmpty() }
            val toName = cell(r, cTo).ifBlank { null }
            if (kind == TxKind.TRANSFER && (toName == null || toName.equals(accountName, ignoreCase = true))) { errors.add("Line $line: a transfer needs a different to_account."); continue }
            val catName = cell(r, cCat).ifBlank { null }.takeIf { kind != TxKind.TRANSFER }
            val note = cell(r, cNote)
            val tagList = cell(r, cTags).split(";").map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
            var splits = cell(r, cSplit).split(";").mapNotNull { part ->
                val eq = part.lastIndexOf('=')
                if (eq <= 0) null else part.substring(0, eq).trim() to (parseAmount(part.substring(eq + 1), currency) ?: return@mapNotNull null)
            }.filter { it.first.isNotEmpty() && it.second > 0 }
            // Slices must add up to the amount, or they are dropped and the row keeps its single category.
            if (kind == TxKind.TRANSFER || splits.sumOf { it.second } != amount) splits = emptyList()
            val origCur = cell(r, cOrigCur).uppercase().let { code -> Currency.all.firstOrNull { it.code == code } }
            val origMinor = origCur?.let { parseAmount(cell(r, cOrigAmt), it) }?.takeIf { it > 0 }
            val rate = if (origCur != null && origMinor != null) {
                val base = BigDecimal(amount).movePointLeft(currency.decimals)
                val foreign = BigDecimal(origMinor).movePointLeft(origCur.decimals)
                base.divide(foreign, 6, RoundingMode.HALF_UP).movePointRight(6).setScale(0, RoundingMode.HALF_UP).toLong().takeIf { it > 0 }
            } else null
            val role = when (cell(r, cRole).uppercase()) { "", "NORMAL" -> TxRole.NORMAL; else -> TxRole.ADJUSTMENT }

            val k = key(date.toEpochDay(), kind, amount, accountName, note)
            val left = seen[k] ?: 0
            if (left > 0) { seen[k] = left - 1; duplicates++; continue }

            if (accountName.isNotBlank() && accNames[accountName.lowercase()] == null) newAcc.putIfAbsent(accountName.lowercase(), accountName)
            if (toName != null && accNames[toName.lowercase()] == null) newAcc.putIfAbsent(toName.lowercase(), toName)
            catName?.let { if (catNames[it.lowercase() to kind] == null) newCat.putIfAbsent(it.lowercase() to kind, it) }
            splits.forEach { (n, _) -> if (catNames[n.lowercase() to kind] == null) newCat.putIfAbsent(n.lowercase() to kind, n) }
            tagList.forEach { if (it.lowercase() !in tagNames) newTag.putIfAbsent(it.lowercase(), it) }
            rows.add(
                Row(
                    line, date.toEpochDay(), kind, amount, accountName, toName, catName, note, tagList, splits,
                    origCur?.code, origMinor.takeIf { rate != null }, rate, role,
                ),
            )
        }
        return Plan(rows, errors, duplicates, newAcc.values.toList(), newCat.map { it.value to it.key.second }, newTag.values.toList())
    }
}
