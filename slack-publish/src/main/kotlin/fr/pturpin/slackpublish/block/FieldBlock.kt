package fr.pturpin.slackpublish.block

import com.slack.api.model.block.composition.BlockCompositions.markdownText
import com.slack.api.model.block.composition.TextObject
import fr.pturpin.slackpublish.SlackMessage
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

class FieldBlock(project: Project): SlackMessageBlock() {

    /**
     * List of fields to add the inserted section.
     */
    val fields: ListProperty<TextObject> = project.objects.listProperty(TextObject::class.java)

    /**
     * Indicate if a block divider should prepend the added section.
     * Note that if the payload does not contains any other block, or if no fields are specified, no divider will be
     * prepended.
     */
    val insertDivider: Property<Boolean> = project.objects.property(Boolean::class.java).apply {
        set(true)
    }

    /**
     * Add a new field with a title and a body.
     *
     * This result in a markdown field represented as "*<title>*\n<body>". Both can embed markdown.
     * If `null` is given, it is written null in italic to indicate the nullity.
     */
    fun field(title: String, body: String?) {
        fields.add(markdownText("*$title*\n${body ?: "_null_"}"))
    }

    override fun defaultFormat(message: SlackMessage) {
        val fields = fields.get()
        if (fields.isNotEmpty()) {
            message.section(insertDivider=insertDivider.get()) {
                this.fields = fields
            }
        }
    }

}