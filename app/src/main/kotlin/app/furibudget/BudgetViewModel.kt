package app.furibudget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.furibudget.core.Currency
import app.furibudget.core.Money
import app.furibudget.data.AccountEntity
import app.furibudget.data.CategoryEntity
import app.furibudget.data.Ledger
import app.furibudget.data.TransactionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetState(
    val loaded: Boolean = false,
    val currency: Currency = Currency.PHP,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val balances: Map<Long, Money> = emptyMap(),
) {
    val total: Money get() = balances.values.fold(Money.ZERO) { a, b -> a + b }
    fun category(id: Long?) = categories.firstOrNull { it.id == id }
    fun account(id: Long?) = accounts.firstOrNull { it.id == id }
}

class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as FuriBudgetApp).repository

    val state: StateFlow<BudgetState> = combine(repo.accounts, repo.categories, repo.transactions) { a, c, t ->
        BudgetState(true, Currency.PHP, a, c, t, Ledger.balances(a, t))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetState())

    init {
        viewModelScope.launch { repo.seedIfEmpty() }
    }

    suspend fun load(id: Long): TransactionEntity? = repo.transaction(id)

    fun save(tx: TransactionEntity) {
        viewModelScope.launch { repo.save(tx) }
    }

    fun delete(tx: TransactionEntity) {
        viewModelScope.launch { repo.delete(tx) }
    }

    fun restore(tx: TransactionEntity) {
        viewModelScope.launch { repo.restore(tx) }
    }
}
