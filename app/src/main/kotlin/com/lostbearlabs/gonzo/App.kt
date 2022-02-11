package com.lostbearlabs.gonzo

import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import kotlin.system.exitProcess


data class Command(val cmd: String, val title: String, val params: List<String>, val fn: (List<String>) -> Unit)

val commands: List<Command> = listOf(
        Command("ls", "show branches", listOf()) { _ -> WorkingRepo().use { it.showBranches() } },
        Command("c", "choose branch", listOf("Branch")) { ar: List<String> -> WorkingRepo().use { it.pickBranch(ar[1]) } },
        Command("d", "delete branch", listOf("Branch")) { ar: List<String> -> WorkingRepo().use { it.deleteBranch(ar[1]) } },
        Command("f", "fetch all", listOf()) { _ -> WorkingRepo().use { it.fetchAll() } },
        Command("pd", "pull down", listOf()) { _ -> WorkingRepo().use { it.pull() } },
        Command("pu", "push up", listOf()) { _ -> WorkingRepo().use { it.push() } },
        Command("dg", "delete gone branches", listOf()) { _ -> WorkingRepo().use { it.deleteGone() } },
        Command("br", "create branch", listOf("BranchName")) { ar: List<String> -> WorkingRepo().use { it.createBranch(ar[1]) } },
        Command("bt", "create ticket branch", listOf("TicketId", "BranchName")) { ar: List<String> -> WorkingRepo().use { it.createTicketBranch(ar[1], ar[2]) } },
        Command("ca", "commit all", listOf("CommitMessage")) { ar: List<String> -> WorkingRepo().use { it.commitAll(ar[1]) } },
        Command("scan", "scan ~/dev for repos with uncommitted changes", listOf()) { ScanUncommitted().scan() },
        Command("check.dotfiles", "check whether any cruft has crept into dotfiles", listOf()) { CheckDotFiles().check() },
        Command("help", "show commands", listOf()) { printHelp() },
        Command("q", "quit", listOf()) { exitProcess(0) }
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

    if (args.isNotEmpty()) {
        runCommand(args.toList())
    } else {
        WorkingRepo().use{ it.showBranches() }
        while (true) {
            print("?> ")
            val cmd = readLine()
            if (cmd != null) {
                val ar = cmd.split(" ")
                runCommand(ar)
            }
        }
    }

}

private fun runCommand(ar: List<String>) {
    val cmd = commands.find { it.cmd == ar[0] }
    if (cmd != null) {
        if (ar.size != cmd.params.size + 1) {
            println("Wrong # args:  ${cmd.cmd} ${cmd.params.joinToString(" ")}  (got ${ar.size - 1}, expected ${cmd.params.size}")
            return
        }
        cmd.fn(ar)
    } else {
        println("Unknown command: ${ar[0]}, try 'help'")
    }
}

