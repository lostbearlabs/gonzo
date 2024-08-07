package com.lostbearlabs.gonzo

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.errors.NotMergedException
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class WorkingRepo : AutoCloseable {
    private val workingDirectory: String = System.getProperty("user.dir")
    private val repo: Repository
    private val git: Git

    init {
        var dir = File(workingDirectory)
        while (true) {
            if (dir.absolutePath == "/") {
                println("NO .git FOLDER FOUND AT THIS PATH OR ABOVE, $workingDirectory")
                exitProcess(1)
            }
            val probe = File(dir, ".git")
            if (probe.isDirectory) {
                break
            }
            dir = dir.parentFile
        }

        repo = FileRepositoryBuilder.create(File(dir, ".git"))
        git = Git(repo)
    }

    override fun close() {
        this.git.close()
        this.repo.close()
    }

    fun deleteGone() {
        this.fetchAll()

        val currentBranch = git.repository.fullBranch
        val branches = this.getBranches().filter { this.isGone(it.name) && it.name != currentBranch }
        if (branches.isEmpty()) {
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
        if (resp == "Y") {
            branches.forEach {
                this.deleteBranch(it)
            }
        }

        this.showBranches()
    }

    // https://stackoverflow.com/a/41495542/4540
    private fun String.runCommand(workingDir: File) {
        ProcessBuilder(*split(" ").toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor(5, TimeUnit.MINUTES)

    }

    fun fetchAll() {
        println("fetch all ...")

        // TODO: ideally would just use jgit fetch, but I'm having SSH config issues?
        "git fetch -p --all --quiet".runCommand(git.repository.directory.parentFile)

        println("... fetch all done")
        this.showBranches()
    }

    fun pull() {
        println("pull ...")

        // TODO: ideally would just use jgit pull, but I'm having SSH config issues?
        "git pull -p --quiet".runCommand(git.repository.directory.parentFile)

        println("... pull done")
        this.showBranches()
    }

    fun push() {
        println("push ...")

        val currentBranch = git.repository.fullBranch
        val shortBranchName = Repository.shortenRefName(currentBranch)

        // TODO: ideally would just use jgit push, but I'm having SSH config issues?
        "git push --set-upstream origin $shortBranchName".runCommand(git.repository.directory.parentFile)

        println("... push done")
        this.showBranches()
    }

    fun pushForce() {
        println("push (force) ...")

        val currentBranch = git.repository.fullBranch
        val shortBranchName = Repository.shortenRefName(currentBranch)

        // TODO: ideally would just use jgit push, but I'm having SSH config issues?
        "git push --force --set-upstream origin $shortBranchName".runCommand(git.repository.directory.parentFile)

        println("... push done")
        this.showBranches()
    }

    fun pickBranch(idxString: String) {
        val idx: Int = try {
            idxString.toInt()
        } catch (ex: NumberFormatException) {
            -1
        }

        val branches = this.getBranches()
        if (idx < 0 || idx >= branches.size) {
            println("bad index $idx")
            return
        }

        val ref = branches[idx]
        git.checkout().setName(ref.name).call()

        this.showBranches()
    }

    fun deleteBranch(idxString: String) {

        val idx: Int = try {
            idxString.toInt()
        } catch (ex: NumberFormatException) {
            -1
        }

        val branches = getBranches()
        if (idx < 0 || idx >= branches.size) {
            println("bad index $idx")
            return
        }

        val currentBranch = git.repository.fullBranch
        val ref = branches[idx]
        if (ref.name == currentBranch) {
            println("cannot delete selected branch")
            return
        }

        if (!this.deleteBranch(ref)) return

        this.showBranches()
    }

    private fun deleteBranch(ref: Ref): Boolean {
        try {
            git.branchDelete().setBranchNames(ref.name).call()
        } catch (ex: NotMergedException) {
            val shortBranchName = Repository.shortenRefName(ref.name)
            print("branch $shortBranchName not merged, delete anyway? Y/[N] ")
            val cmd = readLine()
            if (cmd == "Y") {
                git.branchDelete().setBranchNames(ref.name).setForce(true).call()
            } else {
                return false
            }
        }
        return true
    }


    fun showBranches() {

        val branches = this.getBranches()

        val currentBranch = git.repository.fullBranch

        println()
        for ((n, refBranch) in branches.withIndex()) {
            val prefix = if (refBranch.name == currentBranch) " * " else "   "
            println("$n)  $prefix  ${refBranch.localName()}")

            if (this.isGone(refBranch.name)) {
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

    fun createBranch(name: String) {
        // TODO: if wanted, this would be the place to add parent branch tracking
        git.checkout().setCreateBranch(true).setName(name).call()
        this.showBranches()
    }

    fun createTicketBranch(ticket: String, name: String) {
        this.createBranch("${System.getProperty("user.name")}/$ticket/$name")
    }

    fun createFeatureBranch(ticket: String, name: String) {
        this.createBranch("feature/${ticket}/${System.getProperty("user.name")}/$name")
    }

    fun createBugBranch(ticket: String, name: String) {
        this.createBranch("bug/${ticket}/${System.getProperty("user.name")}/$name")
    }

    fun createChoreBranch(ticket: String, name: String) {
        this.createBranch("chore/${ticket}/${System.getProperty("user.name")}/$name")
    }

    fun commitAll(message: String) {

        runSpotless()

        // if ticket number present as XXX/ticket-number/... then start the commit message with it
        var prefix = ""
        val currentBranch = git.repository.fullBranch
        val shortBranchName = Repository.shortenRefName(currentBranch)
        val ar = shortBranchName.split("/")
        if (ar.size > 1) {
            prefix = ar[1] + " "
        }

        git.add().addFilepattern(".").call()
        git.commit().setMessage(prefix + message).call()
    }

    fun commitAmend() {

        runSpotless()

        val commits = git.log().setMaxCount(1).call()
        val lastCommit = commits.iterator().next()

        git.add().addFilepattern(".").call()
        git.commit().setAmend(true).setMessage(lastCommit.fullMessage).call()
    }


    private fun runSpotless() {
        val spotless = File(File(git.repository.directory.parentFile, "tool_build"), "spotless")
        if (spotless.isDirectory) {
            println("running spotless before commit...")
            "./gradlew spotlessApply".runCommand(git.repository.directory.parentFile)
            println("... ran spotless")
        }
    }

    private fun isGone(fullBranchName: String): Boolean {
        val shortBranchName = Repository.shortenRefName(fullBranchName)
        val config = BranchConfig(git.repository.config, shortBranchName)

        val trackingBranch: String? = config.trackingBranch
        if (trackingBranch != null) {
            git.repository.exactRef(trackingBranch) ?: return true
        }
        return false
    }

    private fun getBranches(): List<Ref> {
        return git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
                .filter { it.isLocal() }
                .sortedBy { it.sortName() }
    }


    private fun Ref.isLocal(): Boolean {
        return this.name.startsWith("refs/heads/")
    }

    private fun Ref.localName(): String {
        return this.name.removePrefix("refs/heads/")
    }

    private fun Ref.sortName(): String {
        return when (this.name) {
            "refs/heads/develop" -> "_0${this.name}"
            "refs/heads/main" -> "_1${this.name}"
            "refs/heads/master" -> "_2${this.name}"
            else -> this.name
        }
    }


}