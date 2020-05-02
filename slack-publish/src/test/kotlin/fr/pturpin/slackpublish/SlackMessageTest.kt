package fr.pturpin.slackpublish

import com.nhaarman.mockitokotlin2.*
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.webhook.Payload
import fr.pturpin.slackpublish.block.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.eclipse.jgit.api.Git
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import org.mockito.Answers

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

    @Test
    fun git_GivenMockedGitRepositoryAndSetFormat_UseTheDefaultFormat() {
        val project = project()
        var message = createMessage(project)
        val git = mock<Git>(defaultAnswer=Answers.RETURNS_DEEP_STUBS) {
            on { repository.branch } doReturn "myMaster"
        }

        val gitBlock = spy(message.createGitBlock()) {
            doReturn(git).whenever(mock).git()
        }

        message = spy(message) {
            on {createGitBlock() } doReturn gitBlock
        }

        message.git {
            format {
                section {
                    blockId = currentBranchName()
                }

                section {
                    blockId = "${project.version}"
                }
            }
        }

        project.version = "42"

        val payload = message.payload.get()

        assertThat(payload).isEqualTo(Payload.builder()
            .blocks(listOf(
                SectionBlock.builder()
                    .blockId("myMaster")
                    .build(),
                DividerBlock(),
                SectionBlock.builder()
                    .blockId("42")
                    .build()
            ))
            .build())
    }

    @Test
    fun customBlock_GivenAnyEagerConfiguration_ExecuteConfigurationEagerlyButFormattingLazilyWhenPayloadIsRetrieved() {
        val project = project()

        val customBlock = object : SlackMessageBlock() {
            var element = project.objects.property(String::class.java)

            override fun defaultFormat(message: SlackMessage) {
                val e = element.get()
                message.section {
                    blockId = e
                }
            }
        }

        var myProperty = "1337"

        val message = createMessage(project)

        message.section {
            blockId = "42"
        }

        message.customBlock(customBlock, isConfigurationLazy=false) {
            element.set(myProperty)
        }

        myProperty = "0"
        val payload = message.payload.get()

        assertThat(payload).isEqualTo(Payload.builder()
            .blocks(listOf(
                SectionBlock.builder()
                    .blockId("42")
                    .build(),
                DividerBlock(),
                SectionBlock.builder()
                    .blockId("1337")
                    .build()
            ))
            .build())
    }

    @Test
    fun customBlock_GivenAnyLazyConfiguration_ExecuteAllLazilyWhenPayloadIsRetrieved() {
        val project = project()

        val customBlock = object : SlackMessageBlock() {
            var element = project.objects.property(String::class.java)

            override fun defaultFormat(message: SlackMessage) {
                val e = element.get()
                message.section {
                    blockId = e
                }
            }
        }

        var myProperty = "0"

        val message = createMessage(project)

        message.section {
            blockId = "42"
        }

        message.customBlock(customBlock, isConfigurationLazy=true) {
            element.set(myProperty)
        }

        myProperty = "1337"
        val payload = message.payload.get()

        assertThat(payload).isEqualTo(Payload.builder()
            .blocks(listOf(
                SectionBlock.builder()
                    .blockId("42")
                    .build(),
                DividerBlock(),
                SectionBlock.builder()
                    .blockId("1337")
                    .build()
            ))
            .build())
    }

    @Test
    fun changelog_GivenConfiguration_UseItAsCustomBlock() {
        val project = project()
        val message = spy(createMessage(project))
        val configure: ChangelogBlock.() -> Unit = { }

        message.changelog(configure)

        verify(message).customBlock(any(), eq(false), same(configure))
    }

    @Test
    fun publication_GivenConfiguration_UseItAsCustomBlock() {
        val project = project()
        val message = spy(createMessage(project))
        val configure: PublicationBlock.() -> Unit = { }

        message.publication(configure)

        verify(message).customBlock(any(), eq(false), same(configure))
    }

    @Test
    fun fields_GivenConfiguration_UseItAsLazyCustomBlock() {
        val project = project()
        val message = spy(createMessage(project))
        val configure: FieldBlock.() -> Unit = { }

        message.fields(configure)

        verify(message).customBlock(any(), eq(true), same(configure))
    }

    @Test
    fun context_GivenConfiguration_UseItAsLazyCustomBlock() {
        val project = project()
        val message = spy(createMessage(project))
        val configure: ContextBlock.() -> Unit = { }

        message.context(configure)

        verify(message).customBlock(any(), eq(true), same(configure))
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