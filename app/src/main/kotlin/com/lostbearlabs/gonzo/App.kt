package com.lostbearlabs.gonzo

import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import kotlin.system.exitProcess


data class Command(val cmd: String, val title: String, val params: List<String>, val fn: (repo: WorkingRepo, List<String>) -> Unit)

val commands: List<Command> = listOf(
        Command("ls", "show branches", listOf()) { repo: WorkingRepo, _: List<String> -> repo.showBranches() },
        Command("c", "choose branch", listOf("Branch")) { repo: WorkingRepo, ar: List<String> -> repo.pickBranch(ar[1]) },
        Command("d", "delete branch", listOf("Branch")) { repo: WorkingRepo, ar: List<String> -> repo.deleteBranch(ar[1]) },
        Command("f", "fetch", listOf()) { repo: WorkingRepo, _: List<String> -> repo.fetch() },
        Command("pd", "pull", listOf()) { repo: WorkingRepo, _: List<String> -> repo.pull() },
        Command("pu", "push", listOf()) { repo: WorkingRepo, _: List<String> -> repo.push() },
        Command("g", "delete gone branches", listOf()) { repo: WorkingRepo, _: List<String> -> repo.deleteGone() },
        Command("br", "create branch", listOf("BranchName")) { repo: WorkingRepo, ar: List<String> -> repo.createBranch(ar[1]) },
        Command("bt", "create ticket branch", listOf("TicketId", "BranchName")) { repo: WorkingRepo, ar: List<String> -> repo.createTicketBranch(ar[1], ar[2]) },
        Command("ca", "commit all", listOf("CommitMessage")) {repo: WorkingRepo, ar: List<String> -> repo.commitAll(ar[1]) },
        Command("scan", "scan ~/dev for repos with uncommitted changes", listOf()) {_: WorkingRepo, _: List<String> -> ScanUncommitted().scan() },
        Command("?", "show usage", listOf()) { _: WorkingRepo, _: List<String> -> printHelp() },
        Command("q", "quit", listOf()) { _: WorkingRepo, _: List<String> -> exitProcess(0) }
)

fun printHelp() {
    println("Usage: ")
    commands.forEach {
        println("  ${it.cmd} ${it.params.joinToString(" ")} : ${it.title}")
    }
    println()
}

fun main(args: Array<String>) {
    BasicConfigurator.configure()
    Logger.getRootLogger().level = Level.OFF

    val repo = WorkingRepo()
    repo.use {

        if (args.isNotEmpty()) {
            runCommand(args.toList(), repo)
        } else {
            repo.showBranches()
            while (true) {
                print("?> ")
                val cmd = readLine()
                if (cmd != null) {
                    val ar = cmd.split(" ")
                    runCommand(ar, repo)
                }
            }
        }

    }
}

private fun runCommand(ar: List<String>, repo: WorkingRepo) {
    val cmd = commands.find { it.cmd == ar[0] }
    if (cmd != null) {
        if (ar.size != cmd.params.size + 1) {
            println("Wrong # args:  ${cmd.cmd} ${cmd.params.joinToString(" ")}  (got ${ar.size-1}, expected ${cmd.params.size}")
            return
        }
        cmd.fn(repo, ar)
    }
}

