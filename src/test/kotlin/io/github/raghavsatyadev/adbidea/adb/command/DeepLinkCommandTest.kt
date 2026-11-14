package io.github.raghavsatyadev.adbidea.adb.command

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeepLinkCommandTest {

  @Test
  fun restrictedToApp() {
    val cmd = DeepLinkCommand.buildShellCommand(DeepLinkOptions("myapp://home?a=1&b=2"), "com.pkg")
    assertThat(cmd)
      .isEqualTo("am start -W -a android.intent.action.VIEW -d 'myapp://home?a=1&b=2' com.pkg")
  }

  @Test
  fun clearTaskImpliesNewTask() {
    val cmd =
      DeepLinkCommand.buildShellCommand(
        DeepLinkOptions("https://x.io", restrictToApp = false, clearTask = true),
        "com.pkg",
      )
    assertThat(cmd)
      .isEqualTo(
        "am start -W -a android.intent.action.VIEW -d 'https://x.io' " +
          "--activity-new-task --activity-clear-task"
      )
  }

  @Test
  fun singleQuotesAreEscaped() {
    assertThat(DeepLinkCommand.shellQuote("a'b")).isEqualTo("'a'\\''b'")
  }
}
