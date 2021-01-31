import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.json.responseJson
import com.slack.api.bolt.App
import com.slack.api.bolt.jetty.SlackAppServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import utils.HttpAccessor
import com.github.kittinunf.result.Result

import views.SearchAddressesByZipcodeResponse

fun main() {
    val app = App()

    app.command("/hello") { req, ctx ->
        val zipcode: String = req.getPayload().getText()
        val url = "http://zipcloud.ibsnet.co.jp/api/search?zipcode=$zipcode"

        val httpAccessor = HttpAccessor()
        val format = Json { ignoreUnknownKeys = true }
        val zipcodeResponse =
            format.decodeFromString<SearchAddressesByZipcodeResponse>(httpAccessor.getJson(url).toString())

        val address =
            zipcodeResponse.results.first().address1 + zipcodeResponse.results.first().address2 + zipcodeResponse.results.first().address3
        ctx.ack { res -> res.responseType("in_channel").text("その郵便番号の住所は $address です。:+1:") }
    }

    app.command("/jira") { req, ctx ->
        val arguments = req.getPayload().getText().split(" ")
        var slackBotResponse = ""

        if (arguments[0] == "create") {
            // この中でAPIを叩く
            val url = "https://test-jira-ryoma.atlassian.net/rest/api/2/issue"
            val (_, _, result) = Fuel.post(url)
                .jsonBody(
                    """
                    {
                        "fields": {
                            "summary": "test",
                            "issuetype": {
                                "id": "10002"
                            },
                            "project": {
                                "id": "10000"
                            },
                            "description": "test",
                            "reporter": {
                                "id": "600401c7e2a13500694ddf32"
                            }
                        }
                    }
                """.trimIndent()
                )
                .authentication()
                .basic("ryoma.k.0224@gmail.com", "S45GUCnPgAlHel2V4XNHB0D3")
                .responseJson()

            val jiraTicketKey = result.get().obj()["key"].toString()
            slackBotResponse = """
                    JIRAを起票しました。
                    https://test-jira-ryoma.atlassian.net/browse/$jiraTicketKey
                    """.trimIndent()
        }
        ctx.ack { res -> res.responseType("in_channel").text(slackBotResponse) }
    }

    val server = SlackAppServer(app)
    server.start() // http://localhost:3000/slack/events
}