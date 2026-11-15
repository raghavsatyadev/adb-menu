package io.github.raghavsatyadev.adbidea.adb.command

import com.intellij.openapi.progress.ProcessCanceledException
import io.github.raghavsatyadev.adbidea.adb.AdbUtil
import io.github.raghavsatyadev.adbidea.adb.command.receiver.GenericReceiver
import io.github.raghavsatyadev.adbidea.ui.NotificationHelper
import java.util.concurrent.TimeUnit

/** How a deep link should be fired. Picked once in the dialog, then sent to every device. */
data class DeepLinkOptions(
  val url: String,
  /** Resolve the link only inside the current app (`am start ... <package>`). */
  val restrictToApp: Boolean = true,
  /** Launch in a new task (`--activity-new-task`). */
  val newTask: Boolean = false,
  /** Clear the existing task first (`--activity-clear-task`, implies a new task). */
  val clearTask: Boolean = false,
)

class DeepLinkCommand(private val options: DeepLinkOptions) : Command {
  override fun run(context: CommandContext): Boolean =
    with(context) {
      try {
        if (options.restrictToApp && !AdbUtil.isAppInstalled(device, packageName)) {
          NotificationHelper.error(
            String.format("<b>%s</b> is not installed on %s", packageName, device.name)
          )
          return false
        }
        val receiver = GenericReceiver()
        device.executeShellCommand(buildShellCommand(options, packageName), receiver, 15L, TimeUnit.SECONDS)
        val failure = receiver.adbOutputLines.firstOrNull { it.startsWith("Error") }
        if (failure != null) {
          NotificationHelper.error(
            String.format("Deep link failed on %s.<br/><b>ADB Output:</b> %s", device.name, failure)
          )
          return false
        }
        NotificationHelper.info(String.format("Deep link sent on %s: %s", device.name, options.url))
        return true
      } catch (e: ProcessCanceledException) {
        throw e
      } catch (e: Exception) {
        NotificationHelper.error("Deep link failed... " + e.message)
      }
      return false
    }

  companion object {
    fun buildShellCommand(options: DeepLinkOptions, packageName: String): String {
      val sb = StringBuilder("am start -W -a android.intent.action.VIEW")
      sb.append(" -d ").append(shellQuote(options.url))
      if (options.newTask || options.clearTask) sb.append(" --activity-new-task")
      if (options.clearTask) sb.append(" --activity-clear-task")
      if (options.restrictToApp) sb.append(' ').append(packageName)
      return sb.toString()
    }

    /** Single-quote for the device shell so `&`, `?`, `$` in the URL reach `am` untouched. */
    fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
  }
}
