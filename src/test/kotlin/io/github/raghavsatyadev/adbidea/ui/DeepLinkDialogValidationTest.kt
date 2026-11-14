package io.github.raghavsatyadev.adbidea.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeepLinkDialogValidationTest {

  @Test
  fun acceptsHierarchicalAndOpaqueUris() {
    listOf("https://x.io/a?b=1", "myapp://home", "tel:123", "mailto:user@example.com", "geo:0,0?q=x")
      .forEach { assertThat(DeepLinkDialog.isValidUrl(it)).isTrue() }
  }

  @Test
  fun rejectsMissingSchemeOrBadSyntax() {
    listOf("", "example.com", "tel:", "1http://x", "myapp://a b")
      .forEach { assertThat(DeepLinkDialog.isValidUrl(it)).isFalse() }
  }
}
