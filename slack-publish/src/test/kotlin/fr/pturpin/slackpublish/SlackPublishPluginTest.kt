package fr.pturpin.slackpublish

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SlackPublishPluginTest {

    @Rule
    @JvmField
    val projectDir = TemporaryFolder()

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
                assertThat(it.message.isPresent).isTrue()
                assertThat(it.message.get().name).isEqualTo(messageName)
            }
    }

}