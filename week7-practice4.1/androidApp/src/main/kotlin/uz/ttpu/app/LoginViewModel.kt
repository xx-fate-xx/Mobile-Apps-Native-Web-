package uz.ttpu.app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uz.ttpu.shared.LoginValidator

// Task 14 - android side uses LoginValidator from commonMain directly
class LoginViewModel : ViewModel() {
    private val validator = LoginValidator()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun onLoginClick(email: String, password: String) {
        _error.value = validator.validate(email, password)
    }
}
