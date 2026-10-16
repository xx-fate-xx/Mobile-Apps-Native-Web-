package uz.ttpu.feature.profile

data class Profile(val name: String, val group: String)

class ProfileRepository {
    fun loadProfile(): Profile = Profile("Student", "SE-24")
}
