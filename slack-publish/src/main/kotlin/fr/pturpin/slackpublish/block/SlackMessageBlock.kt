package fr.pturpin.slackpublish.block

import fr.pturpin.slackpublish.SlackMessage

abstract class SlackMessageBlock {

    private var format: (SlackMessage.() -> Unit)? = null

    /**
     * Override the default format.
     */
    fun format(configure: SlackMessage.() -> Unit) {
        format = configure
    }

    internal fun format(message: SlackMessage) {
        val usedFormat = format ?: ::defaultFormat
        usedFormat(message)
    }

    protected abstract fun defaultFormat(message: SlackMessage)
}