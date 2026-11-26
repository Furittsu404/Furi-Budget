package app.furibudget

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.furibudget.data.TemplateEntity

object Shortcuts {
    const val EXTRA_TEMPLATE = "template"

    /** Publishes up to three templates as dynamic shortcuts that add the transaction in one tap. */
    fun publishTemplates(context: Context, templates: List<TemplateEntity>) {
        val list = templates.take(3).map { t ->
            ShortcutInfoCompat.Builder(context, "template_${t.id}")
                .setShortLabel(t.name)
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_add))
                .setIntent(
                    Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW)
                        .putExtra(BillNotifier.EXTRA_ROUTE, "template").putExtra(EXTRA_TEMPLATE, t.id),
                )
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, list) }
    }
}
