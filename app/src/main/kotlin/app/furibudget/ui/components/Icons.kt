package app.furibudget.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** Icon keys are stored in the database; this maps them to drawn icons. */
object FuriIcons {
    fun byKey(key: String): ImageVector = when (key) {
        "cash" -> Icons.Rounded.Payments
        "bank" -> Icons.Rounded.AccountBalance
        "food" -> Icons.Rounded.Restaurant
        "transport" -> Icons.Rounded.DirectionsBus
        "bills" -> Icons.Rounded.ReceiptLong
        "shopping" -> Icons.Rounded.ShoppingBag
        "health" -> Icons.Rounded.HealthAndSafety
        "fun" -> Icons.Rounded.SportsEsports
        "salary" -> Icons.Rounded.Work
        "gift" -> Icons.Rounded.CardGiftcard
        "transfer" -> Icons.Rounded.SwapHoriz
        else -> Icons.Rounded.Category
    }
}
