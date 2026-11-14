package io.github.raghavsatyadev.adbidea.adb

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import org.junit.Test

class CrossPlatformAppResolverTest {

  @Test
  fun flutterKtsTemplate() {
    val gradle =
      """
      android {
          namespace = "com.example.ns"
          defaultConfig {
              // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
              applicationId = "com.example.flutter_app"
          }
      }
      """
    assertThat(CrossPlatformAppResolver.parseGradle(gradle)).isEqualTo("com.example.flutter_app")
  }

  @Test
  fun groovySyntax() {
    assertThat(CrossPlatformAppResolver.parseGradle("defaultConfig {\n applicationId 'com.rn.app'\n}"))
      .isEqualTo("com.rn.app")
  }

  @Test
  fun commentedOutIdIsIgnoredAndNamespaceUsed() {
    val gradle = "namespace \"com.example.ns\"\n// applicationId \"com.old\""
    assertThat(CrossPlatformAppResolver.parseGradle(gradle)).isEqualTo("com.example.ns")
  }

  @Test
  fun resolvesFromProjectFolderAndFallsBackToManifest() {
    val root = Files.createTempDirectory("xp").toFile()
    try {
      assertThat(CrossPlatformAppResolver.resolvePackageName(root)).isNull()
      val main = File(root, "android/app/src/main").apply { mkdirs() }
      File(main, "AndroidManifest.xml").writeText("<manifest package=\"com.manifest.app\"/>")
      assertThat(CrossPlatformAppResolver.resolvePackageName(root)).isEqualTo("com.manifest.app")
      File(root, "android/app/build.gradle.kts").writeText("applicationId = \"com.gradle.app\"")
      assertThat(CrossPlatformAppResolver.resolvePackageName(root)).isEqualTo("com.gradle.app")
    } finally {
      root.deleteRecursively()
    }
  }
}
