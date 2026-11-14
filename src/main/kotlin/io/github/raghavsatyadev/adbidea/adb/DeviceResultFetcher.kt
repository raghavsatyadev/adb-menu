package io.github.raghavsatyadev.adbidea.adb

import com.android.ddmlib.IDevice
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.projectsystem.gradle.getHolderModule
import com.android.tools.idea.util.androidFacet
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.impl.ModuleImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import io.github.raghavsatyadev.adbidea.adb.DeviceResult.DeviceNotFound
import io.github.raghavsatyadev.adbidea.adb.DeviceResult.SuccessfulDeviceResult
import io.github.raghavsatyadev.adbidea.compatibility.BackwardCompatibleGetter
import io.github.raghavsatyadev.adbidea.ui.DeviceChooserDialog
import io.github.raghavsatyadev.adbidea.ui.ModuleChooserDialogHelper
import io.github.raghavsatyadev.adbidea.ui.NotificationHelper
import org.jetbrains.android.facet.AndroidFacet
import org.joor.Reflect

class DeviceResultFetcher(
  private val project: Project,
  private val useSameDevicesHelper: UseSameDevicesHelper,
  private val bridge: Bridge,
) {

  fun fetch(): DeviceResult? {
    val target = resolveTarget() ?: return DeviceResult.Cancelled
    val (facet, packageName) = target

    if (!bridge.isReady()) {
      NotificationHelper.error("No platform configured")
      return DeviceResult.Cancelled
    }

    val rememberedDevices = useSameDevicesHelper.getRememberedDevices()
    if (rememberedDevices.isNotEmpty()) {
      return SuccessfulDeviceResult(rememberedDevices, facet, packageName)
    }

    val devices = bridge.connectedDevices()
    return when {
      devices.size == 1 -> SuccessfulDeviceResult(devices, facet, packageName)
      devices.size > 1 -> showDeviceChooserDialog(facet, packageName)
      else -> DeviceNotFound
    }
  }

  /**
   * Picks the app to act on: an Android app module when the project has one, otherwise the
   * `android/app` folder of a Flutter or React Native project. Returns null (after telling the user
   * why, unless they cancelled) when there is nothing to act on.
   */
  private fun resolveTarget(): Pair<AndroidFacet?, String>? {
    val facets = ProjectFacetManager.getInstance(project).getFacets(AndroidFacet.ID)
    val appFacets = appFacets(facets)
    if (appFacets.isNotEmpty()) {
      val facet = chooseFacet(appFacets) ?: return null
      val packageName =
        AndroidModel.get(facet)?.applicationId
          ?: run {
            NotificationHelper.error("No Android package found")
            return null
          }
      return facet to packageName
    }

    CrossPlatformAppResolver.resolvePackageName(project)?.let {
      return null to it
    }

    NotificationHelper.error(
      "No Android app found. Open an Android app project, or a Flutter / React Native project " +
        "with an <b>android/app</b> folder."
    )
    return null
  }

  private fun appFacets(facets: List<AndroidFacet>): List<AndroidFacet> =
    facets
      .map { HolderModuleGetter(it).get() }
      .mapNotNull { it.androidFacet }
      .filter { it.configuration.isAppProject }
      .distinct()

  private fun chooseFacet(appFacets: List<AndroidFacet>): AndroidFacet? =
    if (appFacets.size > 1) ModuleChooserDialogHelper.showDialogForFacets(project, appFacets)
    else appFacets.first()

  // Can be removed after 242.23339
  class HolderModuleGetter(private val facet: AndroidFacet) : BackwardCompatibleGetter<Module>() {
    override fun getCurrentImplementation(): Module {
      return facet.module.getHolderModule()
    }

    override fun getPreviousImplementation(): Module {
      return Reflect.onClass("com.android.tools.idea.projectsystem.ModuleSystemUtil")
        .call("getHolderModule", facet.module)
        .get<ModuleImpl>()
    }
  }

  private fun showDeviceChooserDialog(facet: AndroidFacet?, packageName: String): DeviceResult {
    val chooser = DeviceChooserDialog(project, facet)
    chooser.show()

    if (chooser.exitCode != DialogWrapper.OK_EXIT_CODE) {
      return DeviceResult.Cancelled
    }

    val selectedDevices = chooser.selectedDevices

    if (chooser.useSameDevices()) {
      useSameDevicesHelper.rememberDevices()
    }

    if (selectedDevices.isEmpty()) {
      return DeviceResult.Cancelled
    }

    return SuccessfulDeviceResult(selectedDevices.asList(), facet, packageName)
  }
}

sealed class DeviceResult {
  data class SuccessfulDeviceResult(
    val devices: List<IDevice>,
    val facet: AndroidFacet?,
    val packageName: String,
  ) : DeviceResult()

  data object Cancelled : DeviceResult()

  data object DeviceNotFound : DeviceResult()
}
