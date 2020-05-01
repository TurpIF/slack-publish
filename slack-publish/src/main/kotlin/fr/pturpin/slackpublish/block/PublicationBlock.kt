package fr.pturpin.slackpublish.block

import com.slack.api.model.block.composition.BlockCompositions.markdownText
import fr.pturpin.slackpublish.SlackMessage
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication
import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * The default format of this block is rendered like this:
 *
 *       |--------------------------------------------------------------------|
 *       | :tada:  Congrats                                                   |
 *       | :rocket:  *publicName* successfully published on *repositoryName*  |
 *       | :date:  Fri, 1 May 2020 13:37:42 GMT                               |
 *       | :package: `groupId:artifactId:version:classifier`                  |
 *       | :+1: Tell your friends!                                            |
 *
 * It is supported to not have repository name, nor classifier.
 */
class PublicationBlock internal constructor(
    private val project: Project,
    private val clock: Clock
) : SlackMessageBlock() {

    constructor(project: Project) : this(project, Clock.systemUTC())

    /**
     * Public name of the publication.
     *
     * The default value is the project name.
     */
    val publicName: Property<String> = project.objects.property(String::class.java).apply {
        set(project.provider { project.name })
    }

    /**
     * Date of the publication.
     *
     * The default value is now in the RFC 1123 format.
     */
    val date: Property<String> = project.objects.property(String::class.java).apply {
        set(project.provider { ZonedDateTime.now(clock).format(DateTimeFormatter.RFC_1123_DATE_TIME) })
    }

    /**
     * Group ID of the publication.
     *
     * This is used in default format to compute the Gradle dependencies string (such as
     * `com.group:artifact:1.2.3:classifier`)
     *
     * The default value is the group of the project.
     */
    val groupId: Property<String> = project.objects.property(String::class.java).apply {
        set(project.provider { project.group.toString() })
    }

    /**
     * Artifact ID of the publication.
     *
     * This is used in default format to compute the Gradle dependencies string (such as
     * `com.group:artifact:1.2.3:classifier`)
     *
     * The default value is the name of the project.
     */
    val artifactId: Property<String> = project.objects.property(String::class.java).apply {
        set(project.provider { project.name.toLowerCase() })
    }

    /**
     * Version of the publication.
     *
     * This is used in default format to compute the Gradle dependencies string (such as
     * `com.group:artifact:1.2.3:classifier`)
     *
     * The default value is the version of the project.
     */
    val version: Property<String> = project.objects.property(String::class.java).apply {
        set(project.provider { project.version.toString() })
    }

    /**
     * Optional classifier of the publication.
     *
     * This is used in default format to compute the Gradle dependencies string (such as
     * `com.group:artifact:1.2.3:classifier`)
     */
    val classifier: Property<String> = project.objects.property(String::class.java)

    /**
     * Optional name of the repository where the publication was sent.
     */
    val repositoryName: Property<String> = project.objects.property(String::class.java)

    /**
     * Helper method to set the [groupId], [artifactId] and [version] of this publication block.
     * The [classifier] property is not set.
     */
    fun publication(publication: MavenPublication) {
        groupId.set(project.provider { publication.groupId })
        artifactId.set(project.provider { publication.artifactId })
        version.set(project.provider { publication.version })
    }

    /**
     * Helper method to set the [repository] name this publication block.
     */
    fun repository(repository: MavenArtifactRepository) {
        this.repositoryName.set(project.provider { repository.name })
    }

    override fun defaultFormat(message: SlackMessage) {
        message.section {
            val repositoryStr = if (repositoryName.isPresent) " on *${repositoryName.get()}*" else ""
            val classifierStr = if (classifier.isPresent) ":${classifier.get()}" else ""

            text = markdownText("""
                    :tada:  Congrats
                    :rocket:  *${publicName.get()} (${version.get()})* successfully published${repositoryStr}
                    :date:  ${date.get()}
                    :package:  `${groupId.get()}:${artifactId.get()}:${version.get()}${classifierStr}`
                    :+1:  Tell your friends!
                    """.trimIndent())
        }
    }
}