package fr.pturpin.slackpublish.block

import fr.pturpin.slackpublish.SlackMessage
import fr.pturpin.slackpublish.git.describeAllAlways
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevCommit
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import java.io.IOException

/**
 * The default format add a divided section block with those fields:
 * - git branch
 * - git commit
 * - git author
 * - git SHA-1
 *
 * It is rendered like this:
 *
 *      |-----------------------------------------------------------------------------------|
 *      | *Git Branch*                           *Git Author*                               |
 *      | Master                                 <mailto:author@mail.com|author@mail.com>   |
 *      |                                                                                   |
 *      | *Git Commit*                           *Git SHA-1*                                |
 *      | The short message of the last commit   `7d06b2aec79d64065eeefdcab505eba5ab22675b` |
 */
class GitBlock(project: Project): SlackMessageBlock() {

    /**
     * Directory that is used as starting point to search for a git repository.
     *
     * Git repository is searched from this starting point and, tries on parent directories if not found.
     *
     * Its default value is the project directory.
     */
    val root: Property<Any> = project.objects.property(Any::class.java).apply {
        set(project.projectDir)
    }

    private val gitProvider: Provider<Git> = root.map {
        findGitRepository(project.file(root))
    }

    internal fun git() = try {
        gitProvider.get()
    } catch (e: IOException) {
        throw InvalidUserDataException("Impossible to fetch Git repository at " + root.get(), e)
    }

    /**
     * Returns the name of the current branch that HEAD point to.
     *
     * If the current branch is detached, a SHA-1 is returned instead.
     */
    fun currentBranchName(): String {
        return git().repository.branch
    }

    /**
     * Returns the description of the last commit.
     *
     * This is equivalent to `git describe --all --always`
     */
    fun lastCommitDescribe(): String {
        return git().describeAllAlways()
    }

    /**
     * Returns the SHA-1 of the last commit, or null if there is no commit.
     */
    fun lastCommitSha1(): String? {
        return lastCommit()?.id?.name()
    }

    /**
     * Returns the author's email address of the last commit, or null if there is no commit.
     */
    fun lastCommitAuthorEmail(): String? {
        return lastCommit()?.authorIdent?.emailAddress
    }

    /**
     * Returns the first line of the last commit message, or null if there is no commit.
     */
    fun lastCommitShortMessage(): String? {
        return lastCommit()?.shortMessage
    }

    private fun lastCommit(): RevCommit? {
        git().repository.resolve(Constants.HEAD) ?: return null

        return git().log().setMaxCount(1).call().singleOrNull()
    }

    override fun defaultFormat(message: SlackMessage) {
        message.fields {
            field("Git Branch", currentBranchName())
            field("Git Author", "<mailto:${lastCommitAuthorEmail()}|${lastCommitAuthorEmail()}>")
            field("Git Commit", lastCommitShortMessage())
            field("Git SHA-1", "`${lastCommitSha1()}`")
        }
    }

    private fun findGitRepository(from: File): Git {
        var gitDirectory: File? = from
        var exception: RepositoryNotFoundException? = null

        while (gitDirectory != null) {
            try {
                return Git.open(gitDirectory)
            } catch (e: RepositoryNotFoundException) {
                if (exception == null) {
                    exception = e
                } else {
                    exception.addSuppressed(e)
                }

                gitDirectory = gitDirectory.parentFile
            }
        }

        throw exception!!
    }
}