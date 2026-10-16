package uz.ttpu.feature.auth

data class User(val email: String)

class AuthRepository {
    var currentUser: User? = null
        private set

    fun login(email: String) {
        currentUser = User(email)
    }

    fun logout() {
        currentUser = null
    }
}
