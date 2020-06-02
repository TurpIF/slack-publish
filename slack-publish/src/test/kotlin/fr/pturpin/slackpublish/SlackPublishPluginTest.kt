package fr.pturpin.slackpublish

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockserver.client.MockServerClient
import org.mockserver.junit.MockServerRule
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.JsonBody.json

class SlackPublishPluginTest {

    @Rule
    @JvmField
    val projectDir = TemporaryFolder()

    @Rule
    @JvmField
    var mockServerRule = MockServerRule(this)

    private lateinit var mockServerClient: MockServerClient

    @Test
    fun applyPlugin_GivenPluginId_ShouldPopulateSlackExtension() {
        val project = givenProject()

        project.pluginManager.apply("fr.pturpin.slack-publish")

        assertPluginIsLoaded(project)
    }

    @Test
    fun applyPlugin_GivenPluginClass_ShouldPopulateSlackExtension() {
        val project = givenProject()

        project.pluginManager.apply(SlackPublishPlugin::class.java)

        assertPluginIsLoaded(project)
    }

    @Test
    fun registerMessage_CreateTaskToSendMessage() {
        val project = givenProject()

        project.pluginManager.apply(SlackPublishPlugin::class.java)
        project.slack.messages {
            register("dummy1")
            register("dummy2")
        }

        project.assertTaskIsRegistered("sendDummy1MessageToSlack", "dummy1")
        project.assertTaskIsRegistered("sendDummy2MessageToSlack", "dummy2")
    }

    @Test
    fun kotlinSample_GivenBlockSampleTask_SendAFixedSlackMessage() {
        javaClass.getResourceAsStream("sample.build.gradle.kts").use {
            projectDir.newFile("build.gradle.kts").writeBytes(it.readBytes())
        }

        mockServerClient.`when`(request()).respond(response().withStatusCode(204))

        val result = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()
            .withArguments("sendBlockMessageToSlack", "-PwebHook=http://localhost:${mockServerRule.port}")
            .build()

        assertThat(result.task(":sendBlockMessageToSlack")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)

        mockServerClient.verify(request()
            .withBody(json("""{
                |  "channel": "#alerts",
                |  "username": "Good Bot",
                |  "icon_emoji": ":robot:",
                |  "blocks": [
                |    {
                |      "type": "section",
                |      "text": {
                |        "type": "mrkdwn",
                |        "text": "This is my *first* section :+1:"
                |      }
                |    },
                |    {
                |      "type": "section",
                |      "fields": [
                |        {
                |          "type": "mrkdwn",
                |          "text": "*:rocket:*\nField 1"
                |        },
                |        {
                |          "type": "mrkdwn",
                |          "text": "*:tada:*\nField 2"
                |        },
                |      ]
                |    },
                |    {
                |      "type": "divider"
                |    },
                |    {
                |      "type": "context",
                |      "elements": [
                |        {
                |          "type": "mrkdwn",
                |          "text": "A context line"
                |        },
                |        {
                |          "type": "image",
                |          "image_url": "http://my.image/path",
                |          "alt_text": "alt text"
                |        }
                |      ]
                |    },
                |    {
                |      "type": "divider"
                |    }
                |  ]
                |}""".trimMargin())))
    }

    private fun givenProject(): Project {
        return ProjectBuilder.builder()
            .withProjectDir(projectDir.root)
            .build()
    }

    private fun assertPluginIsLoaded(project: Project) {
        assertThat(project.pluginManager.hasPlugin("fr.pturpin.slack-publish"))

        assertThat(project.extensions.getByName("slack"))
            .isNotNull()
            .isInstanceOf(SlackPublishExtension::class.java)
    }

    private fun Project.assertTaskIsRegistered(taskName: String, messageName: String) {
        assertThat(tasks.findByName(taskName))
            .isInstanceOf(SlackTask::class.java)
            .extracting { it as SlackTask }
            .satisfies {
                assertThat(it.name).isEqualTo(taskName)
                assertThat(it.description).isEqualTo("Send the message `$messageName` to Slack")
                assertThat(it.group).isEqualTo("slack")
                assertThat(it.messages.isPresent).isTrue()
                assertThat(it.messages.get()).hasSize(1).allSatisfy {
                    assertThat(it.name).isEqualTo(messageName)
                }
            }
    }

}