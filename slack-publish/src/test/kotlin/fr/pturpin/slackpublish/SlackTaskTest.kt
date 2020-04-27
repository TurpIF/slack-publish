package fr.pturpin.slackpublish

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.spy
import com.slack.api.webhook.Payload
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.mockserver.client.MockServerClient
import org.mockserver.junit.MockServerRule
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response


class SlackTaskTest {

    @Rule
    @JvmField
    var mockServerRule = MockServerRule(this)

    private lateinit var mockServerClient: MockServerClient

    @Test
    fun sendMessage_GivenNoMessage_ThrowException() {
        val task = project().givenTask()

        assertThatCode {
            task.sendMessages()
        }.isInstanceOf(InvalidUserDataException::class.java)
    }

    @Test
    fun sendMessage_GivenMessage_SendItsPayloadToItsWebHook() {
        mockServerClient.`when`(request()).respond(response().withStatusCode(204))

        val project = project()
        val message = project.givenMessage(Payload.builder()
            .text("myText")
            .build())

        val task = project.givenTask()
        task.messages.add(message)
        task.sendMessages()

        mockServerClient.verify(request()
            .withPath("/hook")
            .withBody("""{"text":"myText"}"""))
    }

    @Test
    fun sendMessage_GivenMultipleMessages_SendAllOfThem() {
        mockServerClient.`when`(request()).respond(response().withStatusCode(204))

        val project = project()
        val task = project.givenTask()

        task.message("dummy1") {
            webHook.set(mockServerAddress("hook1"))
        }

        task.message("dummy2") {
            webHook.set(mockServerAddress("hook2"))
        }

        task.sendMessages()

        mockServerClient.verify(request()
            .withPath("/hook1")
            .withBody("""{}"""))

        mockServerClient.verify(request()
            .withPath("/hook2")
            .withBody("""{}"""))

        assertThat(project.tasks.withType(SlackTask::class.java)).hasSize(1)
    }

    private fun Project.givenMessage(expectedPayload: Payload): SlackMessage {

        val realMessage = project.slack.messages.create("name")

        return spy(realMessage) {
            on { payload } doReturn providers.provider { expectedPayload }
        }.also {
            it.webHook.set(mockServerAddress("hook"))
        }
    }

    private fun mockServerAddress(hookPath: String): String {
        return "http://localhost:${mockServerRule.port}/$hookPath"
    }

    private fun Project.givenTask(): SlackTask {
        return tasks.create("task", SlackTask::class.java)
    }

    private fun project(): Project {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(SlackPublishPlugin::class.java)
        return project
    }

}