import com.slack.api.bolt.App
import com.slack.api.bolt.jetty.SlackAppServer

fun main() {
    val app = App()

    app.command("/hello") { req, ctx ->
        val zipCode: String = req.getPayload().getText()
        // api投げる
        val accessor = utils.HttpAccessor()
        val resultJson = accessor.getJson("http://zipcloud.ibsnet.co.jp/api/search?zipcode=$zipCode")
        val results = resultJson.get("results")
        ctx.ack(results.toString())
    }

    val server = SlackAppServer(app)
    server.start() // http://localhost:3000/slack/events
}