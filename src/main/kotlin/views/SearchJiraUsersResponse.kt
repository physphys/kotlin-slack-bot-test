package views

import kotlinx.serialization.Serializable

@Serializable(with=)
class SearchJiraUsersResponse : ArrayList<SearchJiraUsersResponseItem>()