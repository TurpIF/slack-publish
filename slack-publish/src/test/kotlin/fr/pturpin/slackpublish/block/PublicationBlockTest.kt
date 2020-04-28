package fr.pturpin.slackpublish.block

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.webhook.Payload
import fr.pturpin.slackpublish.SlackMessage
import fr.pturpin.slackpublish.SlackPublishPlugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

class PublicationBlockTest {

    @Test
    fun defaultFormat_GivenNoFormatAndNoInput_FormatWithDefault() {
        val fixedTime = ZonedDateTime.of(2020, 6, 22, 14, 42, 13, 14, ZoneId.of("Europe/Paris"))
        val clock = Clock.fixed(fixedTime.toInstant(), fixedTime.zone)

        val project = project("projectName")
        val message = SlackMessage("name", project)
        val publicationBlock = PublicationBlock(project, clock)

        // Add a section before to show that divider is added
        message.section {
            blockId = "42"
        }

        project.group = "com.group"
        project.version = "1337"

        publicationBlock.format(message)
        val payload = message.payload.get()

        assertIsDefaultFormat(payload, """:tada:  Congrats
                |:rocket:  *projectName (1337)* successfully published
                |:date:  Mon, 22 Jun 2020 14:42:13 +0200
                |:package:  `com.group:projectname:1337`
                |:+1:  Tell your friends!""".trimMargin())
    }

    @Test
    fun defaultFormat_GivenNoFormatAndInputAreSet_FormatWithDefault() {
        val project = project("my-project")
        val message = SlackMessage("name", project)
        val publicationBlock = PublicationBlock(project).apply {
            publicName.set("MyProject")
            repositoryName.set("Production")
            classifier.set("sources")
            date.set("31/12/2020")
        }

        // Add a section before to show that divider is added
        message.section {
            blockId = "42"
        }

        project.group = "org.group"
        project.version = "1.2.3"

        publicationBlock.format(message)
        val payload = message.payload.get()

        assertIsDefaultFormat(payload, """:tada:  Congrats
                |:rocket:  *MyProject (1.2.3)* successfully published on *Production*
                |:date:  31/12/2020
                |:package:  `org.group:my-project:1.2.3:sources`
                |:+1:  Tell your friends!""".trimMargin())
    }

    @Test
    fun publication_GivenMavenPublication_SetProperties() {
        val mavenPublication = mock<MavenPublication>()

        val project = project("my-project")
        val publicationBlock = PublicationBlock(project)

        publicationBlock.publication(mavenPublication)

        // Values are set after publication call to show lazyness when getting values back
        mavenPublication.stub {
            on { groupId } doReturn "myGroupId"
            on { artifactId } doReturn "myArtifactId"
            on { version } doReturn "myVersion"
        }

        assertThat(publicationBlock.groupId.get()).isEqualTo("myGroupId")
        assertThat(publicationBlock.artifactId.get()).isEqualTo("myArtifactId")
        assertThat(publicationBlock.version.get()).isEqualTo("myVersion")
    }

    @Test
    fun repository_GivenMavenRepository_SetName() {
        val mavenRepository = mock<MavenArtifactRepository>()

        val project = project("my-project")
        val publicationBlock = PublicationBlock(project)

        publicationBlock.repository(mavenRepository)

        // Values are set after publication call to show lazyness when getting values back
        mavenRepository.stub {
            on { name } doReturn "myName"
        }

        assertThat(publicationBlock.repositoryName.get()).isEqualTo("myName")
    }

    private fun assertIsDefaultFormat(payload: Payload, markdown: String) {
        // Equality on Markdown object is broken, so comparison is fastidious
        val blocks = payload.blocks
        payload.blocks = null
        assertThat(payload).isEqualTo(Payload.builder().build())
        assertThat(blocks).hasSize(3)
        assertThat(blocks[0]).isEqualTo(SectionBlock.builder().blockId("42").build())
        assertThat(blocks[1]).isEqualTo(DividerBlock())
        assertThat(blocks[2]).isInstanceOf(SectionBlock::class.java)

        val block = blocks[2] as SectionBlock
        val text = block.text
        block.text = null

        assertThat(block).isEqualTo(SectionBlock.builder().build())
        text.assertIsMarkdown(markdown)
    }

    private fun project(name: String = ""): Project {
        val project = ProjectBuilder.builder()
            .withName(name)
            .build()

        project.pluginManager.apply(SlackPublishPlugin::class.java)
        return project
    }

}