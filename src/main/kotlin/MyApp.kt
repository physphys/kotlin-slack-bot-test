import com.slack.api.bolt.App
import com.slack.api.bolt.jetty.SlackAppServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import utils.HttpAccessor

import views.SearchAddressesByZipcodeResponse

fun main() {
    val app = App()

    app.command("/hello") { req, ctx ->
        val zipcode: String = req.getPayload().getText()
        val url = "http://zipcloud.ibsnet.co.jp/api/search?zipcode=$zipcode"

        val httpAccessor = HttpAccessor()
        val format = Json { ignoreUnknownKeys = true }
        val zipcodeResponse = format.decodeFromString<SearchAddressesByZipcodeResponse>(httpAccessor.getJson(url).toString())

        ctx.ack(zipcodeResponse.status.toString())
    }

    val server = SlackAppServer(app)
    server.start() // http://localhost:3000/slack/events
}