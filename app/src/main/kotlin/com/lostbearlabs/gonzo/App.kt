package com.lostbearlabs.gonzo

import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.eclipse.jgit.api.errors.NotMergedException
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.util.concurrent.TimeUnit


// TODO:
//  - allow create branch (replaces gcplb)
//  - allow commit changes (replaces gca)

fun main(args: Array<String>) {
    BasicConfigurator.configure()
    Logger.getRootLogger().level = Level.OFF

    val workingDirectory = System.getProperty("user.dir")
    val repo: Repository = FileRepositoryBuilder.create(File(workingDirectory, ".git"))
    repo.use {
        val git = Git(repo)
        git.use {

            showBranches(git)

            while (true) {
                print("?> ")
                val cmd = readLine()
                if (cmd != null) {
                    val ar = cmd.split(" ")
                    when (ar[0]) {
                        "q" -> break
                        "ls" -> showBranches(git)
                        "c" -> pickBranch(ar, git)
                        "d" -> deleteBranch(ar, git)
                        "f" -> fetch(git)
                        "pd" -> pull(git)
                        "pu" -> push(git)
                        "g" -> deleteGone(git)
                    }
                }
            }
        }
    }
}

fun deleteGone(git: Git)  {
    // TODO: call fetch() here once it's working!

    val currentBranch = git.repository.fullBranch
    val branches = getBranches(git).filter{ isGone(git, it.name) && it.name!=currentBranch }
    if( branches.isEmpty()) {
        println("NO GONE BRANCHES TO DELETE")
        return
    }

    println("Deleting GONE branches:")
    branches.forEach {
        val shortBranchName = Repository.shortenRefName(it.name)
        println("   $shortBranchName")
    }

    print("Confirm delete?  Y/[N] ")
    val resp = readLine()
    if( resp=="Y") {
        branches.forEach {
            deleteBranch(git, it)
        }
    }

    showBranches(git)
}

// https://stackoverflow.com/a/41495542/4540
fun String.runCommand(workingDir: File) {
    ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(5, TimeUnit.MINUTES)

}

fun fetch(git: Git) {
    println("fetch ...")

    // TODO: ideally would just use jgit fetch, but I'm having SSH config issues?
    "git fetch -p --all --quiet".runCommand(git.repository.directory.parentFile)

    println("... fetch done")
    showBranches(git)
}

fun pull(git: Git) {
    println("pull ...")

    // TODO: ideally would just use jgit pull, but I'm having SSH config issues?
    "git pull -p --quiet".runCommand(git.repository.directory.parentFile)

    println("... pull done")
    showBranches(git)
}

fun push(git: Git)  {
    println("push ...")

    val currentBranch = git.repository.fullBranch
    val shortBranchName = Repository.shortenRefName(currentBranch)

    // TODO: ideally would just use jgit push, but I'm having SSH config issues?
    "git push --set-upstream origin $shortBranchName".runCommand(git.repository.directory.parentFile)

    println("... pull done")
    showBranches(git)
}

fun pickBranch(ar: List<String>, git: Git) {
    if (ar.size != 2) {
        println("usage: c N")
        return
    }

    var idx: Int
    try {
        idx = ar[1].toInt()
    } catch (ex: NumberFormatException) {
        idx = -1
    }

    val branches = getBranches(git)
    if (idx < 0 || idx > ar.size) {
        println("bad index $idx")
        return
    }

    val ref = branches[idx]
    git.checkout().setName(ref.name).call()

    showBranches(git)
}

fun deleteBranch(ar: List<String>, git: Git) {
    if (ar.size != 2) {
        println("usage: d N")
        return
    }

    var idx: Int
    try {
        idx = ar[1].toInt()
    } catch (ex: NumberFormatException) {
        idx = -1
    }

    val branches = getBranches(git)
    if (idx < 0 || idx > ar.size) {
        println("bad index $idx")
        return
    }

    val currentBranch = git.repository.fullBranch
    val ref = branches[idx]
    if (ref.name == currentBranch) {
        println("cannot delete selected branch")
        return
    }

    if (!deleteBranch(git, ref)) return

    showBranches(git)
}

private fun deleteBranch(git: Git, ref: Ref): Boolean {
    try {
        git.branchDelete().setBranchNames(ref.name).call()
    } catch (ex: NotMergedException) {
        val shortBranchName = Repository.shortenRefName(ref.name)
        println("branch $shortBranchName not merged, delete anyway? Y/[N] ")
        val cmd = readLine()
        if (cmd == "Y") {
            git.branchDelete().setBranchNames(ref.name).setForce(true).call()
        } else {
            return false
        }
    }
    return true
}


fun showBranches(git: Git) {

    val branches = getBranches(git)

    val currentBranch = git.repository.fullBranch

    println()
    for ((n, refBranch) in branches.withIndex()) {
        val prefix = if (refBranch.name == currentBranch) " * " else "   "
        println("$n)  $prefix  ${refBranch.localName()}")

        if( isGone(git, refBranch.name)) {
            val shortBranchName = Repository.shortenRefName(refBranch.name)
            val config = BranchConfig(git.repository.config,
                    shortBranchName)
            println("         -> (GONE) ${config.trackingBranch}")
        } else {
            val status = BranchTrackingStatus.of(git.repository, refBranch.name)
            if (status != null) {
                println("         -> ${status.remoteTrackingBranch}, ahead ${status.aheadCount}, behind ${status.behindCount}")
            }
        }
    }
}

private fun isGone(git: Git, fullBranchName: String) : Boolean {
    val shortBranchName = Repository.shortenRefName(fullBranchName)
    val config = BranchConfig(git.repository.config, shortBranchName)

    val trackingBranch: String? = config.trackingBranch
    if( trackingBranch!=null ) {
        git.repository.exactRef(trackingBranch) ?: return true
    }
    return false
}

private fun getBranches(git: Git): List<Ref> {
    val listRefsBranches = git.branchList().setListMode(ListMode.ALL).call()
            .filter { it.isLocal() }
            .sortedBy { it.sortName() }
    return listRefsBranches
}


fun Ref.isLocal(): Boolean {
    return this.name.startsWith("refs/heads/")
}

fun Ref.localName(): String {
    return this.name.removePrefix("refs/heads/")
}

fun Ref.sortName(): String {
    return when (this.name) {
        "refs/heads/develop" -> "_0${this.name}"
        "refs/heads/main" -> "_1${this.name}"
        "refs/heads/master" -> "_2${this.name}"
        else -> this.name
    }
}

