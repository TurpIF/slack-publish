package fr.pturpin.slackpublish.block

import com.slack.api.model.block.ContextBlockElement
import com.slack.api.model.block.composition.MarkdownTextObject
import org.assertj.core.api.Assertions.assertThat

internal fun ContextBlockElement.assertIsMarkdown(expected: String) {
    assertThat(this).isInstanceOf(MarkdownTextObject::class.java)
    assertThat(this as MarkdownTextObject).satisfies {
        assertThat(it.text).isEqualTo(expected)
        assertThat(it.verbatim as Boolean?).isNull()
    }
}