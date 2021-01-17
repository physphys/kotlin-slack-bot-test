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

        val address = zipcodeResponse.results.first().address1 + zipcodeResponse.results.first().address2 + zipcodeResponse.results.first().address3
        ctx.ack { res -> res.responseType("in_channel").text("その郵便番号の住所は $address です。:+1:") }
    }

    val server = SlackAppServer(app)
    server.start() // http://localhost:3000/slack/events
}