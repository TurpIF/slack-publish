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
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir.root)
            .build()

        project.pluginManager.apply("fr.pturpin.slack-publish")

        assertPluginIsLoaded(project)
    }

    @Test
    fun applyPlugin_GivenPluginClass_ShouldPopulateSlackExtension() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir.root)
            .build()

        project.pluginManager.apply(SlackPublishPlugin::class.java)

        assertPluginIsLoaded(project)
    }

    private fun assertPluginIsLoaded(project: Project) {
        assertThat(project.pluginManager.hasPlugin("fr.pturpin.slack-publish"))

        assertThat(project.extensions.getByName("slack"))
            .isNotNull()
            .isInstanceOf(SlackPublishExtension::class.java)
    }

}