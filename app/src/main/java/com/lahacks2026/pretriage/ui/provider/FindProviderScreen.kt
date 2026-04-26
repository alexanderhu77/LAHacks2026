package com.lahacks2026.pretriage.ui.provider

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lahacks2026.pretriage.data.LaCareNetwork
import com.lahacks2026.pretriage.ui.theme.NoraTheme

/**
 * URL the WebView opens. The provided v3app deep-link (`/a/?<encrypted-state>`) is
 * session-bound and not reproducible from our own values, so we point at the
 * canonical LA Care provider-search landing page and drive the form via JS.
 */
private const val LA_CARE_FIND_PROVIDER_URL = "https://providers.lacare.org/v3app/"

private const val TAG = "FindProvider"

/**
 * WebView screen that opens LA Care's provider search and (best-effort) auto-fills
 * the network dropdown once the page finishes loading. Specialty is left for the
 * user — the LA Care v3app's specialty selector resists JS injection, so we just
 * funnel everyone to the page with their network preselected and let them filter.
 */
@Composable
fun FindProviderScreen(
    network: LaCareNetwork?,
    onBack: () -> Unit,
) {
    val c = NoraTheme.colors
    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.clickable { onBack() }) {
                Text("← Back", color = c.inkSoft, style = NoraTheme.typography.label)
            }
            Text(
                network?.displayName ?: "Network not selected",
                color = c.ink,
                style = NoraTheme.typography.label,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(c.border))
        Spacer(Modifier.height(0.dp))

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                @SuppressLint("SetJavaScriptEnabled")
                val webView = WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            val script = buildAutofillScript(network)
                            Log.i(TAG, "page loaded ($url); injecting network autofill")
                            view.evaluateJavascript(script) { result ->
                                Log.i(TAG, "autofill result: $result")
                            }
                        }
                    }
                    loadUrl(LA_CARE_FIND_PROVIDER_URL)
                }
                webView
            },
        )
    }
}

/**
 * Build a JS snippet that finds the network dropdown via several common
 * strategies (label text → for=id, ARIA / name / id substring, nested select)
 * and sets its value via `dispatchEvent('change', ...)` so React listeners fire.
 *
 * Returns a JSON status string from JS — surfaces in logcat under TAG so we can
 * iterate on selectors against the live page.
 *
 * **TODO(selectors):** the selector strategies are educated guesses. Tweak after
 * inspecting the page in Chrome devtools.
 */
private fun buildAutofillScript(network: LaCareNetwork?): String {
    val networkValue = (network?.dropdownValue ?: "").jsEscape()
    return """
        (function() {
          var report = { network: 'skip' };

          function setNativeSelectByText(select, wantedText) {
            if (!select) return false;
            var opts = select.options;
            for (var i = 0; i < opts.length; i++) {
              if (opts[i].text && opts[i].text.toLowerCase().indexOf(wantedText.toLowerCase()) !== -1) {
                select.value = opts[i].value;
                select.dispatchEvent(new Event('change', { bubbles: true }));
                return true;
              }
            }
            return false;
          }

          function findSelectFor(labelKeywords) {
            // Strategy 1: <label> text → for=id → <select>
            var labels = document.querySelectorAll('label');
            for (var i = 0; i < labels.length; i++) {
              var t = (labels[i].textContent || '').toLowerCase();
              for (var k = 0; k < labelKeywords.length; k++) {
                if (t.indexOf(labelKeywords[k]) !== -1) {
                  var id = labels[i].getAttribute('for');
                  if (id) {
                    var sel = document.getElementById(id);
                    if (sel && sel.tagName === 'SELECT') return sel;
                  }
                  var nested = labels[i].querySelector('select');
                  if (nested) return nested;
                }
              }
            }
            // Strategy 2: aria-label / name / id substring match on <select>
            var selects = document.querySelectorAll('select');
            for (var s = 0; s < selects.length; s++) {
              var blob = ((selects[s].getAttribute('aria-label') || '') + ' ' +
                          (selects[s].getAttribute('name') || '') + ' ' +
                          (selects[s].id || '')).toLowerCase();
              for (var k2 = 0; k2 < labelKeywords.length; k2++) {
                if (blob.indexOf(labelKeywords[k2]) !== -1) return selects[s];
              }
            }
            return null;
          }

          var netSelect = findSelectFor(['network', 'plan']);
          if (netSelect && '${networkValue}'.length) {
            report.network = setNativeSelectByText(netSelect, '${networkValue}') ? 'ok' : 'no-match';
          } else if ('${networkValue}'.length) {
            report.network = 'no-select-found';
          }

          return JSON.stringify(report);
        })();
    """.trimIndent()
}

private fun String.jsEscape(): String =
    replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
