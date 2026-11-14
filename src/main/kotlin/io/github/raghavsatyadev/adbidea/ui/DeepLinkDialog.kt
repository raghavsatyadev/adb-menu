package io.github.raghavsatyadev.adbidea.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import io.github.raghavsatyadev.adbidea.adb.command.DeepLinkOptions
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.net.URI
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/** Asks for a deep link URL and how to launch it. Remembers the last choice per project. */
class DeepLinkDialog(project: Project) : DialogWrapper(project, true) {
  private val props = PropertiesComponent.getInstance(project)
  private val urlField = JBTextField(props.getValue(KEY_URL, ""))
  private val restrictToApp =
    JBCheckBox("Open in the current app only", props.getBoolean(KEY_RESTRICT, true))
  private val newTask = JBCheckBox("Launch in a new task", props.getBoolean(KEY_NEW_TASK, false))
  private val clearTask =
    JBCheckBox("Clear the existing task first", props.getBoolean(KEY_CLEAR_TASK, false))
  private val validationLabel = JLabel()
  private var isUrlValid = false

  init {
    title = "Send Deep Link"
    init()
    updateValidation()
  }

  override fun createCenterPanel(): JComponent {
    val panel = JPanel(BorderLayout(8, 8))
    panel.add(JLabel("Deep Link URL:"), BorderLayout.NORTH)

    // Inner panel contains the text field and validation message
    val inner = JPanel(BorderLayout(4, 4))

    // Make the text field half the screen width
    val screenWidth = Toolkit.getDefaultToolkit().screenSize.width
    val preferredWidth = (screenWidth / 2)
    val currentPref = urlField.preferredSize
    urlField.preferredSize = Dimension(preferredWidth, currentPref.height)

    // Add live validation listener
    urlField.document.addDocumentListener(object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent?) = updateValidation()
      override fun removeUpdate(e: DocumentEvent?) = updateValidation()
      override fun changedUpdate(e: DocumentEvent?) = updateValidation()
    })

    inner.add(urlField, BorderLayout.CENTER)

    // Add validation message label below the text field
    validationLabel.icon = null
    validationLabel.text = ""
    val validationPanel = JPanel(BorderLayout(4, 0))
    validationPanel.add(validationLabel, BorderLayout.WEST)

    val optionsPanel = JPanel(GridLayout(0, 1))
    optionsPanel.add(restrictToApp)
    optionsPanel.add(newTask)
    optionsPanel.add(clearTask)

    val outerPanel = JPanel(BorderLayout(0, 4))
    outerPanel.add(inner, BorderLayout.NORTH)
    outerPanel.add(validationPanel, BorderLayout.CENTER)
    outerPanel.add(optionsPanel, BorderLayout.SOUTH)

    panel.add(outerPanel, BorderLayout.CENTER)
    return panel
  }

  private fun updateValidation() {
    val url = urlField.text.trim()
    isUrlValid = isValidUrl(url)

    if (url.isEmpty()) {
      validationLabel.icon = null
      validationLabel.text = ""
      validationLabel.foreground = JBColor.foreground()
    } else if (isUrlValid) {
      validationLabel.icon = AllIcons.General.InspectionsOK
      validationLabel.text = "Valid URL"
      validationLabel.foreground = JBColor(0x228B22, 0x228B22) // Dark green
    } else {
      validationLabel.icon = AllIcons.General.Error
      validationLabel.text = "Invalid URL format"
      validationLabel.foreground = JBColor(0xDC143C, 0xDC143C) // Crimson red
    }

    // Disable OK button if URL is not valid
    okAction.isEnabled = isUrlValid
  }

  private fun isValidUrl(urlString: String): Boolean {
    if (urlString.isBlank()) return false

    // Deeplink URL validation:
    // - Must contain a scheme (http://, https://, myapp://, intent://, etc.)
    // - Scheme must be followed by :// or similar
    // - Must have at least some content after the scheme
    val schemePattern = Regex("^[a-zA-Z][a-zA-Z0-9+\\-.]*://.*")

    if (!schemePattern.matches(urlString)) {
      return false
    }

    return try {
      URI(urlString)
      true
    } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
      false
    }
  }

  // Return non-null component to satisfy DialogWrapper signature
  override fun getPreferredFocusedComponent(): JComponent = urlField

  override fun doOKAction() {
    props.setValue(KEY_URL, getUrl())
    props.setValue(KEY_RESTRICT, restrictToApp.isSelected, true)
    props.setValue(KEY_NEW_TASK, newTask.isSelected, false)
    props.setValue(KEY_CLEAR_TASK, clearTask.isSelected, false)
    super.doOKAction()
  }

  fun getUrl(): String = urlField.text.trim()

  fun getOptions() =
    DeepLinkOptions(
      url = getUrl(),
      restrictToApp = restrictToApp.isSelected,
      newTask = newTask.isSelected,
      clearTask = clearTask.isSelected,
    )

  private companion object {
    const val PREFIX = "io.github.raghavsatyadev.adbidea.deeplink."
    const val KEY_URL = PREFIX + "url"
    const val KEY_RESTRICT = PREFIX + "restrictToApp"
    const val KEY_NEW_TASK = PREFIX + "newTask"
    const val KEY_CLEAR_TASK = PREFIX + "clearTask"
  }
}
