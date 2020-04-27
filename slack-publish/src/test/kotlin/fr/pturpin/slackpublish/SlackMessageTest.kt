package fr.pturpin.slackpublish

import com.slack.api.model.block.Blocks
import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.webhook.Payload
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SlackMessageTest {

    @Test
    fun payload_GivenNoUpdate_ReturnsEmpty() {
        val message = createMessage()

        val payload = message.payload.get()

        assertThat(payload).isEqualTo(Payload.builder().build())
    }

    @Test
    fun payload_GivenOneUpdate_ReturnsTheUpdate() {
        val message = createMessage()

        message.payload {
            text = "myText"
        }

        val payload = message.payload.get()

        assertThat(payload).isEqualTo(Payload.builder()
            .text("myText")
            .build())
    }

    @Test
    fun payload_GivenManyUpdates_ReturnsTheUpdateComposed() {
        val message = createMessage()

        message.payload {
            text = "myText"
        }

        message.payload {
            text = "myText2"
        }

        message.payload {
            threadTs = "myThread"
        }

        val payload = message.payload.get()

        assertThat(payload).isEqualTo(Payload.builder()
            .text("myText2")
            .threadTs("myThread")
            .build())
    }

    @Test
    fun payload_GivenErrorInUpdate_EvaluateLazilyAndThrowDuringFinalComputation() {
        val message = createMessage()
        val exception = RuntimeException()

        message.payload {
            throw exception
        }

        val payload = message.payload
        assertThatCode {
            payload.get()
        }.isEqualTo(exception)
    }

    @Test
    fun payload_GivenReferenceToUpdatedProjectProperty_EvaluateLazilyAndCommitDuringFinalComputation() {
        val project = project()
        val message = createMessage(project)

        project.version = "1"

        message.payload {
            text = "version = ${project.version}"
        }

        project.version = "2"

        val payload = message.payload.get()

        assertThat(payload).isEqualTo(Payload.builder()
            .text("version = 2")
            .build())
    }

    @Test
    fun block_GivenSomeBlocks_AddThemSequentially() {
        val message = createMessage()

        var str = "dummy"

        message.block {
            Blocks.section {
                it.blockId("id1")
            }
        }

        message.block {
            Blocks.section {
                it.blockId(str)
            }
        }

        message.block(insertDivider = false) {
            Blocks.section {
                it.blockId("id3")
            }
        }

        str = "id2"

        val payload = message.payload.get()

        assertThat(payload).isEqualTo(Payload.builder()
            .blocks(listOf(
                SectionBlock.builder()
                    .blockId("id1")
                    .build(),
                DividerBlock(),
                SectionBlock.builder()
                    .blockId("id2")
                    .build(),
                SectionBlock.builder()
                    .blockId("id3")
                    .build()
            ))
            .build())
    }

    @Test
    fun section_GivenSomeSections_AddThemSequentially() {
        val message = createMessage()

        var str = "dummy"

        message.section {
            blockId = "id1"
        }

        message.section {
            blockId = str
        }

        message.section(insertDivider = false) {
            blockId = "id3"
        }

        str = "id2"

        val payload = message.payload.get()

        assertThat(payload).isEqualTo(Payload.builder()
            .blocks(listOf(
                SectionBlock.builder()
                    .blockId("id1")
                    .build(),
                DividerBlock(),
                SectionBlock.builder()
                    .blockId("id2")
                    .build(),
                SectionBlock.builder()
                    .blockId("id3")
                    .build()
            ))
            .build())
    }

    private fun createMessage(project: Project = project()): SlackMessage {
        return project.slack.messages.create("message")
    }

    private fun project(): Project {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(SlackPublishPlugin::class.java)
        return project
    }

}