package fr.pturpin.slackpublish

import com.slack.api.model.block.Blocks
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.webhook.Payload
import fr.pturpin.slackpublish.block.GitBlock
import fr.pturpin.slackpublish.block.PublicationBlock
import fr.pturpin.slackpublish.block.SlackMessageBlock
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

class SlackMessage(val name: String, private val project: Project) {

    /**
     * WebHook URL to use for this message.
     *
     * To get a new URL, you need to first setup slack:
     * <ul>
     *     <li>Go to your_team.slack.com/services/new/incoming-webhook</li>
     *     <li>Press Add Incoming WebHooks Integration</li>
     *     <li>Grab your WebHook URL</li>
     * </ul>
     */
    val webHook: Property<String> = project.objects.property(String::class.java)

    internal val payload: Provider<Payload> = project.provider {
        val payload = Payload.builder().build()
        payloadConfigurations.forEach { configure ->
            configure(payload)
        }
        payload
    }

    private val payloadConfigurations = mutableListOf<Payload.() -> Unit>()

    /**
     * Add a new update on the produced payload.
     *
     * Each time this method is called, a new update is stored. When the final is retrieved, all updates a run
     * sequentially. The payload start from an empty one, and get updated each time.
     *
     * Hence, all transformations you add here are lazily executed.
     */
    fun payload(configure: Payload.() -> Unit) {
        payloadConfigurations.add(configure)
    }

    /**
     * Add a new block in the produced payload.
     *
     * As [payload], the given block is lazily appended after the already registered updates.
     *
     * The [insertDivider] parameter let you indicate if you want to separate the appended block with a divider. Note
     * that if there is no previous blocks, no divider is inserted.
     *
     * @param insertDivider `true` to indicate if a divider should be inserted
     * @param block lazy definition of the block
     */
    fun block(insertDivider: Boolean = true, block: () -> LayoutBlock) {
        payload {
            if (blocks == null) {
                blocks = mutableListOf()
            } else if (insertDivider) {
                blocks.add(Blocks.divider())
            }

            blocks.add(block())
        }
    }

    /**
     * Add a new section block in the produced payload.
     *
     * As [block], the given section is lazily appended after the already registered updates.
     * This is a specialization of [block], so it obey the same rules.
     *
     * @param insertDivider `true` to indicate if a divider should be inserted
     * @param configure lazy definition of the block
     */
    fun section(insertDivider: Boolean = true, configure: SectionBlock.() -> Unit) {
        block(insertDivider) {
            val section = Blocks.section { it }
            configure(section)
            section
        }
    }

    /**
     * Add a new section in the produced payload with Git information
     *
     * See [GitBlock] for more details.
     */
    fun git(configure: GitBlock.() -> Unit = {}) {
        customBlock(createGitBlock(), configure)
    }

    internal fun createGitBlock() = GitBlock(project)

    /**
     * Add a new section in the produced payload with publication information
     *
     * See [PublicationBlock] for more details.
     */
    fun publication(configure: PublicationBlock.() -> Unit = {}) {
        customBlock(PublicationBlock(project), configure)
    }

    private fun <T : SlackMessageBlock> customBlock(block: T, configure: T.() -> Unit) {
        configure(block)
        block.format(this)
    }

}