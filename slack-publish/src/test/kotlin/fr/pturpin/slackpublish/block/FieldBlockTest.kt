package fr.pturpin.slackpublish.block

import com.nhaarman.mockitokotlin2.*
import com.slack.api.model.block.SectionBlock
import fr.pturpin.slackpublish.SlackMessage
import fr.pturpin.slackpublish.SlackPublishPlugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class FieldBlockTest {

    @Test
    fun field_GivenNonNullBody_AddIt() {
        val fieldBlock = FieldBlock(project())
        fieldBlock.field("myTitle", "myBody")

        val fields = fieldBlock.fields.get()

        assertThat(fields).hasSize(1)
        fields[0].assertIsMarkdown("*myTitle*\nmyBody")
    }

    @Test
    fun field_GivenNullBody_AddItInItalic() {
        val fieldBlock = FieldBlock(project())
        fieldBlock.field("myTitle", null)

        val fields = fieldBlock.fields.get()

        assertThat(fields).hasSize(1)
        fields[0].assertIsMarkdown("*myTitle*\n_null_")
    }

    @Test
    fun insertDivider_GivenNothing_IsTrueByDefault() {
        val fieldBlock = FieldBlock(project())

        val insertDivider = fieldBlock.insertDivider.get()

        assertThat(insertDivider).isTrue()
    }

    @Test
    fun format_GivenNoFields_DoNothing() {
        val message = mock<SlackMessage>()
        val fieldBlock = FieldBlock(project())

        fieldBlock.format(message)

        verifyZeroInteractions(message)
    }

    @Test
    fun format_GivenDividerAndManyFields_AddThemInSection() {
        format_GivenManyFields_AddThemInSection(true)
    }

    @Test
    fun format_GivenNoDividerAndManyFields_AddThemInSection() {
        format_GivenManyFields_AddThemInSection(false)
    }

    private fun format_GivenManyFields_AddThemInSection(insertDivider: Boolean) {
        val message = mock<SlackMessage>()
        val fieldBlock = FieldBlock(project())
        fieldBlock.insertDivider.set(insertDivider)
        fieldBlock.field("title1", "body1")
        fieldBlock.field("title2", null)

        fieldBlock.format(message)

        verify(message).section(eq(insertDivider), check {
            val sectionBlock = SectionBlock.builder().build()
            it(sectionBlock)

            assertThat(sectionBlock.fields).hasSize(2)
            sectionBlock.fields[0].assertIsMarkdown("*title1*\nbody1")
            sectionBlock.fields[1].assertIsMarkdown("*title2*\n_null_")
        })
    }

    private fun project(): Project {
        val project = ProjectBuilder.builder()
            .build()

        project.pluginManager.apply(SlackPublishPlugin::class.java)
        return project
    }

}