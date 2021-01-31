package utils

val config = Config(
    jira = JiraUserAuth(
        username = System.getenv("JIRA_USERNAME"),
        password = System.getenv("JIRA_PASSWORD")
    )
)

data class Config(val jira: JiraUserAuth)

data class JiraUserAuth(val username: String, val password: String)
