package com.lostbearlabs.gonzo

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

class ScanUncommitted {
    fun scan() {
        File("${System.getProperty("user.home")}/dev")
                .walk(FileWalkDirection.TOP_DOWN)
                .forEach { checkFolder(it) }
    }

    private fun checkFolder(dir: File) {
        val probe = File(dir, ".git")
        if( !probe.isDirectory ) return

        val repo = FileRepositoryBuilder.create(probe)
        repo.use {
            val git = Git(repo)
            git.use {
                val status: Status = git.status().call()
                if( status.added.isNotEmpty() or status.uncommittedChanges.isNotEmpty() || status.untracked.isNotEmpty() ) {

                    println("$dir")

                    if( status.added.isNotEmpty()) {
                        println("   Added:")
                        val added: Set<String> = status.added
                        for (add in added) {
                            println("      $add")
                        }
                    }

                    if( status.uncommittedChanges.isNotEmpty()) {
                        println("   Uncommitted:")
                        val uncommittedChanges: Set<String> = status.uncommittedChanges
                        for (uncommitted in uncommittedChanges) {
                            println("      $uncommitted")
                        }
                    }

                    if( status.untracked.isNotEmpty() ) {
                        println("   Untracked:")
                        val untracked: Set<String> = status.untracked
                        for (untrack in untracked) {
                            println("      $untrack")
                        }
                    }

                    println()
                }

            }
        }
    }
}