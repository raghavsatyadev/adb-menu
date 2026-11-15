package io.github.raghavsatyadev.adbidea.adb

import com.intellij.openapi.project.Project
import java.io.File

/**
 * Finds the Android application id of a Flutter or React Native project opened at its root.
 *
 * Those projects keep the Android app in `android/app` and, unless that folder is opened on its
 * own, Android Studio creates no Android facet for it. This reads the id straight from the
 * Gradle build file (or the manifest) so the ADB commands still work.
 */
object CrossPlatformAppResolver {

  fun resolvePackageName(project: Project): String? =
    project.basePath?.let { resolvePackageName(File(it)) }

  fun resolvePackageName(root: File): String? {
    val app = File(root, "android/app")
    if (!app.isDirectory) return null
    for (name in listOf("build.gradle.kts", "build.gradle")) {
      val file = File(app, name)
      if (file.isFile) parseGradle(file.readText())?.let { return it }
    }
    val manifest = File(app, "src/main/AndroidManifest.xml")
    if (manifest.isFile) MANIFEST_PACKAGE.find(manifest.readText())?.let { return it.groupValues[1] }
    return null
  }

  /** `applicationId` wins over `namespace`, as it does in the Android Gradle plugin. */
  fun parseGradle(text: String): String? {
    val code = text.lines().joinToString("\n") { it.substringBefore("//") }
    return APPLICATION_ID.find(code)?.groupValues?.get(1) ?: NAMESPACE.find(code)?.groupValues?.get(1)
  }

  private val APPLICATION_ID = Regex("""\bapplicationId\s*=?\s*\(?\s*["']([\w.]+)["']""")
  private val NAMESPACE = Regex("""\bnamespace\s*=?\s*\(?\s*["']([\w.]+)["']""")
  private val MANIFEST_PACKAGE = Regex("""<manifest[^>]*\bpackage\s*=\s*["']([\w.]+)["']""")
}
