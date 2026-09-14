package ai.rever.boss.utils

/**
 * Builds the small `.reg` file used to register the `boss:` URL scheme.
 *
 * `reg add ... /d "\"<exe>\" \"%1\""` cannot safely carry the command value: Java's Windows
 * process launcher treats an argument beginning and ending in a quote as already quoted, so
 * `reg.exe` receives the executable and `%1` as separate arguments. A `.reg` file has no second
 * command-line parser, making the value that Explorer executes explicit and stable.
 */
internal object WindowsProtocolRegistryScript {
    private const val HEADER = "Windows Registry Editor Version 5.00"
    private const val PROTOCOL_KEY = "HKEY_CURRENT_USER\\Software\\Classes\\boss"

    /** The complete registry-import script for the current BOSS executable. */
    fun buildScript(appPath: String): String {
        val executable = regEscape(appPath)
        return buildString {
            appendLine(HEADER)
            appendLine()

            appendLine("[$PROTOCOL_KEY]")
            appendLine("@=\"URL:BOSS Protocol\"")
            appendLine("\"URL Protocol\"=\"\"")
            appendLine()

            appendLine("[$PROTOCOL_KEY\\DefaultIcon]")
            appendLine("@=\"$executable,0\"")
            appendLine()

            appendLine("[$PROTOCOL_KEY\\shell\\open\\command]")
            // Explorer must receive the executable and URL as two separately quoted values.
            appendLine("@=\"\\\"$executable\\\" \\\"%1\\\"\"")
        }
    }

    /** Escapes a string for a registry-script value. */
    private fun regEscape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
}
