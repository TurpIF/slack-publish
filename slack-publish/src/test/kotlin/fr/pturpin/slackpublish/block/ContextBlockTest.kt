package fr.pturpin.slackpublish.block

import com.nhaarman.mockitokotlin2.*
import com.slack.api.model.block.element.ImageElement
import fr.pturpin.slackpublish.SlackMessage
import fr.pturpin.slackpublish.SlackPublishPlugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class ContextBlockTest {

    @Test
    fun markdown_GivenText_AddItInElements() {
        val contextBlock = ContextBlock(project())
        contextBlock.markdown("*myText*")

        val elements = contextBlock.elements.get()

        assertThat(elements).hasSize(1)
        elements[0].assertIsMarkdown("*myText*")
    }

    @Test
    fun image_GivenUrlAndAltText_AddItInElements() {
        val contextBlock = ContextBlock(project())
        contextBlock.image("http://image.url", "alt text")

        val elements = contextBlock.elements.get()

        assertThat(elements).hasSize(1)
        val imageElement = elements[0] as ImageElement

        assertThat(imageElement.imageUrl).isEqualTo("http://image.url")
        assertThat(imageElement.altText).isEqualTo("alt text")
    }

    @Test
    fun insertDivider_GivenNothing_IsTrueByDefault() {
        val contextBlock = ContextBlock(project())

        val insertDivider = contextBlock.insertDivider.get()

        assertThat(insertDivider).isTrue()
    }

    @Test
    fun format_GivenNoElements_DoNothing() {
        val message = mock<SlackMessage>()
        val contextBlock = ContextBlock(project())

        contextBlock.format(message)

        verifyZeroInteractions(message)
    }

    @Test
    fun format_GivenDividerAndManyElements_AddThemInContext() {
        format_GivenManyElements_AddThemInContext(true)
    }

    @Test
    fun format_GivenNoDividerAndManyElements_AddThemInContext() {
        format_GivenManyElements_AddThemInContext(false)
    }

    private fun format_GivenManyElements_AddThemInContext(insertDivider: Boolean) {
        val message = mock<SlackMessage>()
        val contextBlock = ContextBlock(project())
        contextBlock.insertDivider.set(insertDivider)
        contextBlock.markdown("*text1*")
        contextBlock.markdown("text2")

        contextBlock.format(message)

        verify(message).block(eq(insertDivider), check {
            val block = it() as com.slack.api.model.block.ContextBlock

            assertThat(block.elements).hasSize(2)
            block.elements[0].assertIsMarkdown("*text1*")
            block.elements[1].assertIsMarkdown("text2")
        })
    }

    private fun project(): Project {
        val project = ProjectBuilder.builder()
            .build()

        project.pluginManager.apply(SlackPublishPlugin::class.java)
        return project
    }

}