package app.furibudget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.furibudget.R
import app.furibudget.core.Repeat

@Composable
fun repeatLabel(r: Repeat) = stringResource(
    when (r) {
        Repeat.NONE -> R.string.repeat_none
        Repeat.DAILY -> R.string.repeat_daily
        Repeat.WEEKLY -> R.string.repeat_weekly
        Repeat.MONTHLY -> R.string.repeat_monthly
        Repeat.YEARLY -> R.string.repeat_yearly
    },
)
