package com.lostbearlabs.gonzo

import java.io.File

class CheckDotFiles {

    // All my home directory dotfiles are written to source my common .profile; they end
    // with a comment saying "anything after this was added by an installer".
    //
    // This command checks to see if anything was added, just by looking for any
    // lines after that comment

    private fun checkFile(name: String) {
        val file = File(System.getProperty("user.home"), name)
        if( !file.isFile ) {
            println("not found to check: $name")
            return
        }

        val lines = file.readLines()
                // drop first comment ("# my standard configs")
                .drop(1)
                // drop lines until next comment
                .dropWhile{ !it.startsWith("#")}
                // drop second comment ("# anything after this ...")
                .drop(1)
                // filter out any blank lines
                .filter{ it.isNotEmpty() }

        // if there's anything left, it's unwanted
        if( lines.isNotEmpty() ) {
            println("UNEXPECTED LINE IN $name")
            println("   line content: ${lines[0]}")
            println("   remove line; maybe put it conf/profile.sh if wanted")
        }

    }

    fun check() {
        checkFile(".bashrc")
        checkFile(".bash_profile")
        checkFile(".profile")
        checkFile(".zshrc")
    }


}