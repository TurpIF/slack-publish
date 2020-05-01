package fr.pturpin.slackpublish.block

import com.slack.api.model.block.composition.BlockCompositions.markdownText
import fr.pturpin.slackpublish.SlackMessage
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import java.util.regex.Pattern

/**
 * Block dedicated to extract changelog from a file for a given version.
 *
 * This block expect the project changelog to be in a `CHANGELOG.md` file that is formatted with markdown. Changelog of
 * the project version is detected by recognizing version lines. So version should appear in recognizable lines.
 *
 * For example, a changelog file may looks like:
 *
 * ```
 * ## Version 2.0.1
 *   - Feature 1
 *     - Feature 1.a
 *     - Feature 1.b
 *   - Feature 2
 *   - ...
 *
 * ## Version 2.0.0
 *   - ...
 * ```
 *
 * In this example, version lines can be recognized thank to "## Version" pattern. With version set to 2.0.1, the
 * extracted changelog would be:
 *
 * ```
 * - Feature 1
 *   - Feature 1.a
 *   - Feature 1.b
 * - Feature 2
 * - ...
 * ```
 *
 * Note that the common indents are automatically trimmed.
 *
 * With the default format, a new markdown section is inserted: "*Changelog*\n<extracted changelog>". If no changelog
 * for the version was found, nothing is inserted.
 */
class ChangelogBlock(private val project: Project): SlackMessageBlock() {

    /**
     * Version of the project.
     *
     * This is used to look for the changelog section related to the appropriate version. To be detected, version lines
     * should be recognizable and contains the version.
     *
     * The default value is the version of the project.
     */
    val version: Property<String> = project.objects.property(String::class.java).apply {
        set(project.provider { project.version.toString() })
    }

    /**
     * Changelog file to parse.
     *
     * By default a `CHANGELOG.md` file is search in this project directory. If none is found, parent projects'
     * directories are inspected. If there is still no file that is found, an exception is thrown.
     */
    val changelogFile: Property<Any> = project.objects.property(Any::class.java)

    internal val file: Provider<File> = project.provider {
        if (changelogFile.isPresent) {
            project.file(changelogFile.get())
        } else {
            findChangelogFile()
        }
    }

    private val versionLinePattern: ListProperty<Regex> = project.objects.listProperty(Regex::class.java)

    /**
     * Extracted changelog from the specified file and the given current version.
     *
     * If no changelog section is found, an empty string is provided to avoid having to handle [Provider.isPresent].
     */
    val changelog: Provider<String> = version.flatMap { version ->
        versionLinePattern.flatMap { patterns ->
            file.map { file ->
                parseChangelog(file, version, patterns)
            }
        }
    }

    /**
     * Indicate that version lines are recognizable and start with the given format.
     *
     * Calling this method many times accumulate the given patterns.
     */
    fun versionLinesStartWith(startWith: String) {
        versionLinesHavePattern(Pattern.compile("^${Pattern.quote(startWith)}.*"))
    }

    /**
     * Indicate that version lines are recognizable with the given pattern.
     *
     * Calling this method many times accumulate the given patterns.
     */
    fun versionLinesHavePattern(pattern: Pattern) {
        versionLinePattern.add(pattern.toRegex())
    }

    override fun defaultFormat(message: SlackMessage) {
        val log = changelog.get()
        if (log.isNotEmpty()) {
            message.section {
                text = markdownText("*Changelog*\n$log")
            }
        }
    }

    private fun findChangelogFile(): File {
        var project: Project? = this.project
        while (project != null) {
            val file = project.projectDir.resolve("CHANGELOG.md")
            if (file.exists()) {
                return file
            }

            project = project.parent
        }

        return this.project.projectDir.resolve("CHANGELOG.md")
    }

    private fun parseChangelog(file: File, version: String, versionLinePatterns: List<Regex>): String {
        if (versionLinePatterns.isEmpty()) {
            throw InvalidUserDataException("Impossible to extract changelog from ${file.name}. No indication was given on how to read lines.")
        }

        val changelogLines = mutableListOf<String>()

        file.useLines { lines ->
            var isInVersion = false
            for (line in lines) {
                if (versionLinePatterns.any { line.matches(it) }) {
                    if (isInVersion) {
                        // We're leaving the good version section
                        return@useLines
                    }

                    if (line.contains(version)) {
                        isInVersion = true
                        continue
                    }
                }

                if (isInVersion) {
                    changelogLines.add(line)
                }
            }
        }

        return changelogLines.joinToString(separator="\n") { it }.trimIndent()
    }

}