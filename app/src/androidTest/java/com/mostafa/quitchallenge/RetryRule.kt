package com.mostafa.quitchallenge

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Runs a UI test once more if it fails. Software-rendered emulators sometimes can't read a screen
 * that is animating; a real bug fails both runs.
 */
class RetryRule(private val attempts: Int = 2) : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            var last: Throwable? = null
            repeat(attempts) { attempt ->
                try {
                    base.evaluate()
                    return
                } catch (t: Throwable) {
                    last = t
                    System.err.println("${description.methodName} failed on attempt ${attempt + 1}: $t")
                }
            }
            throw last!!
        }
    }
}
