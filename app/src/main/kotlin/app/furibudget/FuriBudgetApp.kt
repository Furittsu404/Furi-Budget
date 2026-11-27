package app.furibudget

import android.app.Application
import app.furibudget.data.BudgetDatabase
import app.furibudget.data.BudgetRepository
import app.furibudget.data.SettingsRepository

class FuriBudgetApp : Application() {
    val repository: BudgetRepository by lazy { BudgetRepository(BudgetDatabase.create(this)) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val sounds: app.furibudget.feedback.SoundPlayer by lazy { app.furibudget.feedback.SoundPlayer(this) }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        Notifier.scheduleRecurring(this)
    }
}
