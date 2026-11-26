package app.furibudget

import android.app.Application
import app.furibudget.data.BudgetDatabase
import app.furibudget.data.BudgetRepository

class FuriBudgetApp : Application() {
    val repository: BudgetRepository by lazy { BudgetRepository(BudgetDatabase.create(this)) }
}
