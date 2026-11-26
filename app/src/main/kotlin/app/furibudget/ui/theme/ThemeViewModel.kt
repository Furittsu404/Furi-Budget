package app.furibudget.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ThemeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ThemeRepository(app)
    private val _spec = MutableStateFlow<ThemeSpec?>(null)

    /** Null until the saved choice is read, so the first frame never flashes the default look. */
    val spec: StateFlow<ThemeSpec?> = _spec.asStateFlow()

    init {
        viewModelScope.launch { _spec.value = repo.spec.first() }
    }

    fun update(change: (ThemeSpec) -> ThemeSpec) {
        val next = change(_spec.value ?: return)
        _spec.value = next
        viewModelScope.launch { repo.save(next) }
    }
}
