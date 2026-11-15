package io.github.raghavsatyadev.adbidea.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants

/** "ADB Menu" tool window: one button per ADB Menu action, so commands are a single click away. */
class AdbToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val panel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      border = JBUI.Borders.empty(8)
    }
    for (action in menuActions()) {
      if (action is Separator) {
        panel.add(Box.createVerticalStrut(JBUI.scale(4)))
        panel.add(JSeparator().apply { maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(2)) })
        panel.add(Box.createVerticalStrut(JBUI.scale(4)))
      } else {
        panel.add(actionButton(action))
        panel.add(Box.createVerticalStrut(JBUI.scale(2)))
      }
    }
    val content =
      toolWindow.contentManager.factory.createContent(JBScrollPane(panel), null, false)
    toolWindow.contentManager.addContent(content)
  }

  /** Children of the `AdbMenu` group, without the popup launcher and with no leading separator. */
  private fun menuActions(): List<AnAction> {
    val manager = ActionManager.getInstance()
    val group = manager.getAction(MENU_GROUP_ID) as? DefaultActionGroup ?: return emptyList()
    return group.getChildActionsOrStubs()
      .filter { manager.getId(it) != QUICK_LIST_ID }
      .dropWhile { it is Separator }
      .dropLastWhile { it is Separator }
  }

  private fun actionButton(action: AnAction): JButton {
    val presentation = action.templatePresentation
    return JButton(presentation.text).apply {
      toolTipText = presentation.description
      horizontalAlignment = SwingConstants.LEFT
      alignmentX = Component.LEFT_ALIGNMENT
      maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
      addActionListener {
        ActionManager.getInstance()
          .tryToExecute(action, null, this, ActionPlaces.TOOLWINDOW_CONTENT, true)
      }
    }
  }

  private companion object {
    const val MENU_GROUP_ID = "AdbMenu"
    const val QUICK_LIST_ID = "io.github.raghavsatyadev.adbidea.action.AdbIdeaQuickList"
  }
}
