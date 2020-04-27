package fr.pturpin.slackpublish

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SlackPublishExtensionTest {

    @Rule
    @JvmField
    val projectDir = TemporaryFolder()

    @Test
    fun registerMessage_ShouldPopulateContainer() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir.root)
            .build()

        project.pluginManager.apply(SlackPublishPlugin::class.java)
        project.slack.messages {
            register("dummy")
        }

        assertThat(project.slack.messages.getByName("dummy"))
            .isNotNull()
            .satisfies {
                assertThat(it.name).isEqualTo("dummy")
            }
    }

}