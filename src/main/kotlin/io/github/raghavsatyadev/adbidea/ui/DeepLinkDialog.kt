package io.github.raghavsatyadev.adbidea.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.net.URI
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/** Simple dialog that asks the user for a deep link URL. */
class DeepLinkDialog(project: Project) : DialogWrapper(project, true) {
  private val urlField = JBTextField()
  private val validationLabel = JLabel()
  private var isUrlValid = false

  init {
    title = "Send Deep Link"
    init()
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

    val outerPanel = JPanel(BorderLayout(0, 4))
    outerPanel.add(inner, BorderLayout.NORTH)
    outerPanel.add(validationPanel, BorderLayout.SOUTH)

    panel.add(outerPanel, BorderLayout.CENTER)
    return panel
  }

  private fun updateValidation() {
    val url = urlField.text.trim()
    isUrlValid = isValidUrl(url)

    if (url.isEmpty()) {
      validationLabel.icon = null
      validationLabel.text = ""
      validationLabel.foreground = JBColor.BLACK
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

  fun getUrl(): String = urlField.text.trim()
}
