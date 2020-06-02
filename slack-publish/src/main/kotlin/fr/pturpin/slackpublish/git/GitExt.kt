package fr.pturpin.slackpublish.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.revwalk.RevTag
import org.eclipse.jgit.revwalk.RevWalk
import java.util.*

internal fun Git.describeAllAlways(): String {
    // FIXME Push a PR to JGit to handle the --all argument
    return RevWalk(repository).use { walk ->
        val head = repository.resolve(Constants.HEAD) ?: return Constants.R_HEADS + Constants.MASTER
        val target = walk.parseCommit(head)

        val refsList: Collection<Ref> = repository.refDatabase
            .getRefsByPrefix(Constants.R_REFS)

        val refs = refsList.groupBy { getObjectIdFromRef(it) }

        walk.markStart(target)

        var ref: Ref? = null
        while (true) {
            val commit = walk.next() ?: break
            val candidateRefs = refs[commit]
            if (candidateRefs != null && candidateRefs.isNotEmpty()) {
                ref = candidateRefs.minBy {
                    val t: RevObject = walk.parseAny(it.objectId)
                    walk.parseBody(t)
                    when (t) {
                        is RevTag -> t.taggerIdent.getWhen()
                        is RevCommit -> t.authorIdent.getWhen()
                        else -> Date()
                    }
                }!!
                break
            }
        }

        if (ref != null) {
            ref.name
        } else {
            walk.objectReader.abbreviate(target).name()
        }
    }
}

private fun Git.getObjectIdFromRef(r: Ref): ObjectId? {
    var key: ObjectId? = repository.refDatabase.peel(r).peeledObjectId
    if (key == null) {
        key = r.objectId
    }
    return key
}