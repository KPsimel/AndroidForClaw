package com.xiaomo.androidforclaw.agent.tools

enum class TermuxSetupStep {
    TERMUX_NOT_INSTALLED,
    TERMUX_API_NOT_INSTALLED,
    KEYPAIR_MISSING,
    RUN_COMMAND_PERMISSION_DENIED,
    RUN_COMMAND_SERVICE_MISSING,
    AUTO_SETUP_DISPATCH_FAILED,
    SSHD_NOT_REACHABLE,
    SSH_CONFIG_MISSING,
    SSH_AUTH_FAILED,
    READY,
    UNKNOWN
}

data class TermuxStatus(
    val termuxInstalled: Boolean,
    val termuxApiInstalled: Boolean,
    val runCommandPermissionDeclared: Boolean,
    val runCommandServiceAvailable: Boolean,
    val sshReachable: Boolean,
    val sshConfigPresent: Boolean,
    val keypairPresent: Boolean,
    val lastStep: TermuxSetupStep,
    val message: String
) {
    val ready: Boolean
        get() = termuxInstalled && sshReachable && sshConfigPresent && lastStep == TermuxSetupStep.READY
}

object TermuxStatusFormatter {
    fun fallbackMessage(status: TermuxStatus): String {
        return when (status.lastStep) {
            TermuxSetupStep.TERMUX_API_NOT_INSTALLED -> "Termux:API is not installed yet."
            TermuxSetupStep.RUN_COMMAND_PERMISSION_DENIED -> "RUN_COMMAND permission is unavailable for this app build."
            TermuxSetupStep.RUN_COMMAND_SERVICE_MISSING -> "Termux RUN_COMMAND service is unavailable."
            TermuxSetupStep.KEYPAIR_MISSING -> "SSH keypair is missing."
            TermuxSetupStep.SSHD_NOT_REACHABLE -> "sshd is not reachable on 127.0.0.1:8022."
            TermuxSetupStep.SSH_CONFIG_MISSING -> "SSH config file was not generated."
            else -> "Please open Termux and run: pkg install openssh && sshd"
        }
    }

    fun userFacingMessage(status: TermuxStatus): String {
        return "Termux is not ready: ${status.message} ${fallbackMessage(status)}".trim()
    }
}
