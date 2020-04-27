package fr.pturpin.slackpublish

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

open class SlackTask: DefaultTask() {

    @Input
    val message: Property<SlackMessage> = project.objects.property(SlackMessage::class.java)

}