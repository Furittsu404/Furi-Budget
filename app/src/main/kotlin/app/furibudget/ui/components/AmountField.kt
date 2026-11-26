package app.furibudget.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import app.furibudget.core.Currency
import app.furibudget.core.Money

/** Text box for an amount. Keeps only characters a [Currency.parse] could accept. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    currency: Currency,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() || it == '.' }.take(14)) },
        label = { Text(label) },
        prefix = { Text(currency.symbol + " ") },
        singleLine = true,
        isError = value.isNotEmpty() && currency.parse(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = if (currency.decimals > 0) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}

fun Currency.plain(money: Money): String = format(money).replace(",", "")
