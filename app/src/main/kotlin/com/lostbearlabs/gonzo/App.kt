package com.lostbearlabs.gonzo

import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.eclipse.jgit.api.errors.NotMergedException
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File


// TODO:
//  - allow cleanup gone branches (replaces git-clean-branches)
//  - allow create branch (replaces gcplb)
//  - allow commit changes (replaces gca)
//  - allow push, pull, fetch (alternatives to gpu, gpd, gf)


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
                    }
                }
            }
        }
    }
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

    try {
        git.branchDelete().setBranchNames(ref.name).call()
    } catch (ex: NotMergedException) {
        println("branch not merged, delete anyway? Y/[N] ")
        val cmd = readLine()
        if (cmd == "Y") {
            git.branchDelete().setBranchNames(ref.name).setForce(true).call()
        } else {
            return
        }
    }

    showBranches(git)
}


fun showBranches(git: Git) {

    val branches = getBranches(git)

    val currentBranch = git.repository.fullBranch

    println()
    for ((n, refBranch) in branches.withIndex()) {
        val prefix = if (refBranch.name == currentBranch) " * " else "   "
        println("$n)  $prefix  ${refBranch.localName()}")

        val status = BranchTrackingStatus.of(git.repository, refBranch.name)
        if (status != null) {
            println("     -> ${status.remoteTrackingBranch}, ahead ${status.aheadCount}, behind ${status.behindCount}")
        }
    }
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

