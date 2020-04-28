package fr.pturpin.slackpublish.block

import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.block.composition.TextObject
import org.assertj.core.api.Assertions.assertThat

internal fun TextObject.assertIsMarkdown(expected: String) {
    assertThat(this).isInstanceOf(MarkdownTextObject::class.java)
    assertThat(this as MarkdownTextObject).satisfies {
        assertThat(it.text).isEqualTo(expected)
        assertThat(it.verbatim as Boolean?).isNull()
    }
}