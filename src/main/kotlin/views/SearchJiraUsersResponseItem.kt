package views

data class SearchJiraUsersResponseItem(
    val accountId: String = "",
    val accountType: String = "",
    val active: Boolean = false,
    val avatarUrls: AvatarUrls = AvatarUrls(),
    val displayName: String = "",
    val emailAddress: String = "",
    val locale: String = "",
    val self: String = "",
    val timeZone: String = ""
)