package hello;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;

public class MyApp {
    public static void main(String[] args) throws Exception {
        // SLACK_BOT_TOKEN, SLACK_SIGNING_SECRET という環境変数が設定されている前提
        App app = new App();

        app.command("/hello", (req, ctx) -> {
            return ctx.ack(":candy: はい、アメちゃん！");
        });

        SlackAppServer server = new SlackAppServer(app);
        server.start(); // http://localhost:3000/slack/events
    }
}