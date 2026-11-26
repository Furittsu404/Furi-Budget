package app.furibudget.data

/** First-run content. Colours match the accent swatches so category dots feel part of the palette. */
object Seed {
    fun accounts() = listOf(
        AccountEntity(name = "Cash", colorArgb = 0xFF4ADE80.toInt(), icon = "cash", sortOrder = 0),
        AccountEntity(name = "Bank", colorArgb = 0xFF4F8DFF.toInt(), icon = "bank", sortOrder = 1),
    )

    fun categories() = listOf(
        CategoryEntity(name = "Food", colorArgb = 0xFFFBA31E.toInt(), icon = "food", kind = TxKind.EXPENSE, sortOrder = 0),
        CategoryEntity(name = "Transport", colorArgb = 0xFF38BDF8.toInt(), icon = "transport", kind = TxKind.EXPENSE, sortOrder = 1),
        CategoryEntity(name = "Bills", colorArgb = 0xFFFF6B6B.toInt(), icon = "bills", kind = TxKind.EXPENSE, sortOrder = 2),
        CategoryEntity(name = "Shopping", colorArgb = 0xFFE066FF.toInt(), icon = "shopping", kind = TxKind.EXPENSE, sortOrder = 3),
        CategoryEntity(name = "Health", colorArgb = 0xFF2DD4BF.toInt(), icon = "health", kind = TxKind.EXPENSE, sortOrder = 4),
        CategoryEntity(name = "Fun", colorArgb = 0xFFFF5C93.toInt(), icon = "fun", kind = TxKind.EXPENSE, sortOrder = 5),
        CategoryEntity(name = "Other", colorArgb = 0xFF94A3B8.toInt(), icon = "other", kind = TxKind.EXPENSE, sortOrder = 6),
        CategoryEntity(name = "Salary", colorArgb = 0xFF4ADE80.toInt(), icon = "salary", kind = TxKind.INCOME, sortOrder = 0),
        CategoryEntity(name = "Gift", colorArgb = 0xFFFACC15.toInt(), icon = "gift", kind = TxKind.INCOME, sortOrder = 1),
        CategoryEntity(name = "Other income", colorArgb = 0xFF94A3B8.toInt(), icon = "other", kind = TxKind.INCOME, sortOrder = 2),
    )
}
