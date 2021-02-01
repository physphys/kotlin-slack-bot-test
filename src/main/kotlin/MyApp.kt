import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.json.responseJson
import com.slack.api.app_backend.views.response.ViewSubmissionResponse.ViewSubmissionResponseBuilder
import com.slack.api.bolt.App
import com.slack.api.bolt.context.builtin.ViewSubmissionContext
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler
import com.slack.api.bolt.jetty.SlackAppServer
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest
import com.slack.api.bolt.response.Response
import com.slack.api.bolt.util.BuilderConfigurator
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

import utils.HttpAccessor
import views.SearchAddressesByZipcodeResponse

import com.slack.api.model.block.element.BlockElements.plainTextInput
import com.slack.api.model.block.Blocks.input
import com.slack.api.model.block.element.BlockElements.staticSelect
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.Blocks.asBlocks
import com.slack.api.model.block.InputBlock.InputBlockBuilder
import com.slack.api.model.block.SectionBlock.SectionBlockBuilder
import com.slack.api.model.block.element.PlainTextInputElement.PlainTextInputElementBuilder
import com.slack.api.model.block.element.StaticSelectElement.StaticSelectElementBuilder
import com.slack.api.model.view.View.ViewBuilder
import com.slack.api.model.view.ViewClose.ViewCloseBuilder
import com.slack.api.model.view.ViewSubmit.ViewSubmitBuilder
import com.slack.api.model.view.ViewTitle.ViewTitleBuilder
import com.slack.api.model.block.Blocks.*
import com.slack.api.model.block.composition.BlockCompositions.*
import com.slack.api.model.block.element.BlockElements.*
import com.slack.api.model.view.Views.*
import com.slack.api.model.view.View
import com.slack.api.methods.response.views.ViewsOpenResponse
import com.slack.api.model.block.element.MultiStaticSelectElement
import java.util.HashMap

import com.slack.api.model.view.ViewState


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
        val res = ctx.client().viewsOpen {
            it
                .triggerId(ctx.triggerId)
                .view(buildJiraView())
        }
        if (res.isOk) ctx.ack()
        else Response.builder().statusCode(500).body(res.error).build()
    }

    app.viewSubmission("create-jira") { req, ctx ->
        val stateValues = req.payload.view.state.values
        val reporterID = stateValues["reporter-block"]!!["reporter-action"]!!.selectedOption.value
        val summary = stateValues["summary-block"]!!["summary-action"]!!.value
        val description = stateValues["description-block"]!!["description-action"]!!.value


        // validation
        val errors: MutableMap<String, String> = HashMap()
        if (summary.length == 0) {
            errors["summary-block"] = "タイトルを入力してください"
        }

        if (!errors.isEmpty()) {
            return@viewSubmission ctx.ack { r: ViewSubmissionResponseBuilder ->
                r.responseAction(
                    "errors"
                ).errors(errors)
            }
        } else {
            val (_, _, result) = Fuel.post("https://test-jira-ryoma.atlassian.net/rest/api/2/issue")
                .jsonBody(
                    """
                    {
                        "fields": {
                            "summary": "$summary",
                            "issuetype": {
                                "id": "10002"
                            },
                            "project": {
                                "id": "10000"
                            },
                            "description": "$description",
                            "reporter": {
                                "id": "$reporterID"
                            }
                        }
                    }
                """.trimIndent()
                )
                .authentication()
                .basic(utils.config.jira.username, utils.config.jira.password)
                .responseJson()

            val jiraTicketKey = result.get().obj()["key"].toString()
            val slackBotResponse = """
                    JIRAを起票しました。
                    https://test-jira-ryoma.atlassian.net/browse/$jiraTicketKey
                    """.trimIndent()
            return@viewSubmission ctx.ack()
        }
    }

    app.command("/meeting") { req, ctx ->
        println("/meeting")

        val res = ctx.client().viewsOpen {
            it
                .triggerId(ctx.triggerId)
                .view(buildView())
        }
        if (res.isOk) ctx.ack()
        else Response.builder().statusCode(500).body(res.error).build()
    }

    // ユーザーが "Submit" ボタンをクリックしたとき
    app.viewSubmission("meeting-arrangement") { req: ViewSubmissionRequest, ctx: ViewSubmissionContext ->
        val stateValues = req.payload.view.state.values
        val agenda = stateValues["agenda-block"]!!["agenda-action"]!!.value
        val errors: MutableMap<String, String> = HashMap()
        if (agenda.length <= 10) {
            errors["agenda-block"] = "Agenda needs to be longer than 10 characters."
        }
        if (!errors.isEmpty()) {
            return@viewSubmission ctx.ack { r: ViewSubmissionResponseBuilder ->
                r.responseAction(
                    "errors"
                ).errors(errors)
            }
        } else {
            // TODO: ここで stateValues や privateMetadata を保存したりする

            // 空のボディで応答すると、このモーダルは閉じられる
            // モーダルを書き換えて次のステップを見せる場合は response_action と新しいモーダルの view を応答する
            return@viewSubmission ctx.ack()
        }
    }

    val server = SlackAppServer(app)
    server.start() // http://localhost:3000/slack/events
}


fun buildView(): View? {
    return view { view: ViewBuilder ->
        view
            .callbackId("meeting-arrangement")
            .type("modal")
            .notifyOnClose(true)
            .title(viewTitle { title: ViewTitleBuilder ->
                title.type("plain_text").text("Meeting Arrangement").emoji(true)
            })
            .submit(viewSubmit { submit: ViewSubmitBuilder ->
                submit.type("plain_text").text("Submit").emoji(true)
            })
            .privateMetadata("""{"response_url":"https://hooks.slack.com/actions/T1ABCD2E12/330361579271/0dAEyLY19ofpLwxqozy3firz"}""")
            .blocks(asBlocks(
                section { section: SectionBlockBuilder ->
                    section
                        .blockId("category-block")
                        .text(markdownText("Select a category of the meeting!"))
                        .accessory(staticSelect { staticSelect: StaticSelectElementBuilder ->
                            staticSelect
                                .actionId("category-selection-action")
                                .placeholder(plainText("Select a category"))
                                .options(
                                    asOptions(
                                        option(plainText("Customer"), "customer"),
                                        option(plainText("Partner"), "partner"),
                                        option(plainText("Internal"), "internal")
                                    )
                                )
                        })
                },
                input { input: InputBlockBuilder ->
                    input
                        .blockId("agenda-block")
                        .element(plainTextInput { pti: PlainTextInputElementBuilder ->
                            pti.actionId(
                                "agenda-action"
                            ).multiline(true)
                        })
                        .label(plainText { pt -> pt.text("Detailed Agenda").emoji(true) })
                }
            ))
    }
}

fun buildJiraView(): View? {
    return view { view: ViewBuilder ->
        view
            .callbackId("create-jira")
            .type("modal")
            .notifyOnClose(true)
            .title(viewTitle { title: ViewTitleBuilder ->
                title.type("plain_text").text("Create JIRA")
            })
            .submit(viewSubmit { submit: ViewSubmitBuilder ->
                submit.type("plain_text").text("作成")
            })
            .close(viewClose { close: ViewCloseBuilder ->
                close.type("plain_text").text("キャンセル")
            })
            .blocks(asBlocks(
                input { input: InputBlockBuilder ->
                    input
                        .blockId("reporter-block")
                        .element(staticSelect { staticSelect: StaticSelectElementBuilder ->
                            staticSelect.actionId("reporter-action")
                                .placeholder(plainText("報告者を選んでください"))
                                .options(
                                    asOptions(
                                        option(plainText("Ryoma Kawahara"), "600401c7e2a13500694ddf32"),
                                    )
                                )
                        })
                        .label(plainText("報告者"))
                },
                input { input: InputBlockBuilder ->
                    input
                        .blockId("summary-block")
                        .element(plainTextInput { pti: PlainTextInputElementBuilder ->
                            pti.actionId(
                                "summary-action"
                            ).multiline(false)
                        })
                        .label(plainText("タイトル"))
                },
                input { input: InputBlockBuilder ->
                    input
                        .blockId("description-block")
                        .element(plainTextInput { pti: PlainTextInputElementBuilder ->
                            pti.actionId(
                                "description-action"
                            ).multiline(true)
                        })
                        .label(plainText("詳細"))
                }
            ))
    }
}