package io.github.raghavsatyadev.adbidea.adb.command

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProcessCanceledException
import io.github.raghavsatyadev.adbidea.adb.AdbUtil
import io.github.raghavsatyadev.adbidea.adb.command.receiver.GenericReceiver
import io.github.raghavsatyadev.adbidea.ui.DeepLinkDialog
import io.github.raghavsatyadev.adbidea.ui.NotificationHelper
import java.util.concurrent.TimeUnit

class DeepLinkCommand : Command {
  override fun run(context: CommandContext): Boolean =
    with(context) {
      try {
        // Show dialog to get URL from user on the EDT and capture result
        var url: String? = null
        var dialogConfirmed = false

        ApplicationManager.getApplication()
          .invokeAndWait(
            {
              val dialog = DeepLinkDialog(project)
              dialogConfirmed = dialog.showAndGet()
              if (dialogConfirmed) {
                url = dialog.getUrl()
              }
            },
            ModalityState.defaultModalityState(),
          )

        if (!dialogConfirmed) return false

        return fireDeepLink(url)
      } catch (e: ProcessCanceledException) {
        throw e
      } catch (e1: Exception) {
        NotificationHelper.error("Deep link failed... " + e1.message)
      }
      return false
    }

  private fun CommandContext.fireDeepLink(url: String?): Boolean {
    if (!validateURL(url)) return false

    if (AdbUtil.isAppInstalled(device, packageName)) {
      val shell = "am start -W -a android.intent.action.VIEW -d \"$url\""
      device.executeShellCommand(shell, GenericReceiver(), 15L, TimeUnit.SECONDS)
      NotificationHelper.info(
        String.format("<b>%s</b> deep link sent on %s", packageName, device.name)
      )
      return true
    } else {
      NotificationHelper.error(
        String.format("<b>%s</b> is not installed on %s", packageName, device.name)
      )
    }
    return false
  }

  private fun validateURL(url: String?): Boolean {

    if (url.isNullOrBlank()) {
      NotificationHelper.error("Deep link url is empty")
      return false
    }

    // Optional: basic scheme validation
    if (!url.contains("://") && !url.contains(":/")) {
      NotificationHelper.error(
        "Invalid deep link url: missing scheme (e.g. myapp://... or https://...)"
      )
      return false
    }
    return true
  }
}
