package fr.pturpin.slackpublish.block

import com.slack.api.model.block.Blocks
import com.slack.api.model.block.ContextBlockElement
import com.slack.api.model.block.composition.BlockCompositions.markdownText
import com.slack.api.model.block.element.ImageElement
import fr.pturpin.slackpublish.SlackMessage
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

class ContextBlock(project: Project): SlackMessageBlock() {

    /**
     * List of elements to add the inserted context.
     */
    val elements: ListProperty<ContextBlockElement> = project.objects.listProperty(ContextBlockElement::class.java)

    /**
     * Indicate if a block divider should prepend the added section.
     * Note that if the payload does not contains any other block, or if no fields are specified, no divider will be
     * prepended.
     */
    val insertDivider: Property<Boolean> = project.objects.property(Boolean::class.java).apply {
        set(true)
    }

    /**
     * Add a new markdown element in this context with the given text.
     */
    fun markdown(markdown: String) {
        elements.add(markdownText(markdown))
    }

    /**
     * Add a new image element with given URL, and the given plain-text summary (which should not contains any markup).
     */
    fun image(imageUrl: String, altText: String? = null) {
        elements.add(ImageElement.builder()
            .imageUrl(imageUrl)
            .altText(altText)
            .build())
    }

    override fun defaultFormat(message: SlackMessage) {
        val elements = this.elements.get()
        if (elements.isNotEmpty()) {
            message.block(insertDivider=insertDivider.get()) {
                Blocks.context {
                    it.elements(elements)
                }
            }
        }
    }

}