package com.lostbearlabs.gonzo

import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import kotlin.system.exitProcess


// TODO:
//  - allow create branch (replaces gcplb)
//  - allow commit changes (replaces gca)

data class Command(val cmd: String, val title: String, val params: List<String>, val fn: (repo: WorkingRepo, List<String>) -> Unit)

val commands: List<Command> = listOf(
        Command("ls", "show branches", listOf()) { repo: WorkingRepo, _: List<String> -> repo.showBranches() },
        Command("c", "select branch", listOf("Branch")) { repo: WorkingRepo, ar: List<String> -> repo.pickBranch(ar[1]) },
        Command("d", "delete branch", listOf("Branch")) { repo: WorkingRepo, ar: List<String> -> repo.deleteBranch(ar[1]) },
        Command("f", "fetch", listOf()) { repo: WorkingRepo, _: List<String> -> repo.fetch() },
        Command("pd", "pull", listOf()) { repo: WorkingRepo, _: List<String> -> repo.pull() },
        Command("pu", "push", listOf()) { repo: WorkingRepo, _: List<String> -> repo.push() },
        Command("g", "delete gone branches", listOf()) { repo: WorkingRepo, _: List<String> -> repo.deleteGone() },
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
            println("Wrong # args:  ${cmd.title} ${cmd.params.joinToString(" ")}")
            return
        }
        cmd.fn(repo, ar)
    }
}

