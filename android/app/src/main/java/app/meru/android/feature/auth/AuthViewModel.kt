package app.meru.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.network.AuthRequest
import app.meru.android.core.network.GoogleAuthRequest
import app.meru.android.core.network.MeruApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val modeRegister: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: MeruApi,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onDisplayName(value: String) = _state.update { it.copy(displayName = value, error = null) }
    fun toggleMode() = _state.update { it.copy(modeRegister = !it.modeRegister, error = null) }

    fun submit() {
        val snapshot = _state.value
        if (snapshot.email.isBlank() || snapshot.password.length < 6) {
            _state.update { it.copy(error = "Use a valid email and 6+ character password.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val body = AuthRequest(
                    email = snapshot.email.trim(),
                    password = snapshot.password,
                    displayName = snapshot.displayName.ifBlank { snapshot.email.substringBefore("@") },
                )
                if (snapshot.modeRegister) api.register(body) else api.login(body)
            }.onSuccess { response ->
                sessionStore.saveSession(
                    accessToken = response.accessToken,
                    displayName = response.user.displayName,
                    email = response.user.email,
                )
                _state.update { it.copy(loading = false) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Auth failed. Is the Meru API running?",
                    )
                }
            }
        }
    }

    fun continueWithGoogleDev() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { api.google(GoogleAuthRequest()) }
                .onSuccess { response ->
                    sessionStore.saveSession(
                        accessToken = response.accessToken,
                        displayName = response.user.displayName,
                        email = response.user.email,
                    )
                    _state.update { it.copy(loading = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, error = error.message ?: "Google auth stub failed")
                    }
                }
        }
    }
}
