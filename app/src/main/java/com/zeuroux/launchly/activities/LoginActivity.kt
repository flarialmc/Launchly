package com.zeuroux.launchly.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.core.net.toUri
import com.zeuroux.launchly.BuildConfig
import com.zeuroux.launchly.LaunchlyApp
import com.zeuroux.launchly.auth.AuthResult
import com.zeuroux.launchly.auth.AuthSession
import com.zeuroux.launchly.ui.LaunchlyLoadingScreen
import com.zeuroux.launchly.ui.theme.LaunchlyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONTokener
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class LoginActivity : ComponentActivity() {
    private val container by lazy { (application as LaunchlyApp).container }
    private val cookieManager by lazy { CookieManager.getInstance() }
    private val completing = AtomicBoolean(false)
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        setContent {
            LaunchlyTheme { LaunchlyLoadingScreen() }
        }
        lifecycleScope.launch {
            clearCookies()
            if (!isFinishing) createWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView() {
        val view = WebView(this).also { webView = it }
        setContentView(view)
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(view, true)
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowContentAccess = false
            allowFileAccess = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
        }
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (isAllowedGoogleUrl(request.url)) return false
                finishWithError("Google sign-in tried to open an unexpected address.")
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (!isAllowedGoogleUrl(url.toUri())) finishWithError("Google sign-in left an approved HTTPS origin.")
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!isAllowedGoogleUrl(url.toUri()) || completing.get()) return
                val cookies = parseCookies(cookieManager.getCookie(url))
                val oauthToken = cookies[AUTH_TOKEN] ?: return
                view.evaluateJavascript(
                    "(function(){const e=document.querySelector('[data-profile-identifier]');return e?e.textContent:'';})();"
                ) { encodedValue ->
                    val email = runCatching { JSONTokener(encodedValue).nextValue() as? String }
                        .getOrNull()?.trim().orEmpty()
                    if (email.isNotBlank() && completing.compareAndSet(false, true)) {
                        exchangeToken(email, oauthToken)
                    }
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) finishWithError("Google sign-in could not load. Check your connection and try again.")
            }
        }
        view.loadUrl(EMBEDDED_SETUP_URL)
    }

    private fun exchangeToken(email: String, oauthToken: String) {
        lifecycleScope.launch {
            val exchange = withContext(Dispatchers.IO) { requestAasToken(email, oauthToken) }
            when (exchange) {
                is TokenExchange.Success -> {
                    val result = container.authRepository.signIn(exchange.session)
                    if (result == AuthResult.Success) finishSuccessfully()
                    else finishWithError((result as? AuthResult.Failure)?.message ?: "The sign-in session could not be saved.")
                }
                is TokenExchange.Failure -> finishWithError(exchange.message)
            }
        }
    }

    private fun requestAasToken(email: String, oauthToken: String): TokenExchange {
        val body = FormBody.Builder()
            .add("lang", Locale.getDefault().toLanguageTag())
            .add("google_play_services_version", PLAY_SERVICES_VERSION_CODE.toString())
            .add("sdk_version", BUILD_VERSION_SDK.toString())
            .add("device_country", Locale.getDefault().country.lowercase(Locale.US))
            .add("Email", email)
            .add("service", "ac2dm")
            .add("get_accountid", "1")
            .add("ACCESS_TOKEN", "1")
            .add("callerPkg", "com.google.android.gms")
            .add("add_account", "1")
            // CookieManager returns the EmbeddedSetup token in cookie-encoded form.
            // Decode it once before FormBody applies application/x-www-form-urlencoded
            // encoding; otherwise percent escapes are sent to Google double-encoded.
            .add("Token", Uri.decode(oauthToken))
            // Required by Google's current AAS exchange. This matches the upstream
            // GPlayApi 3.5.8 compatibility fix for MissingDroidGuard responses.
            .add("droidguard_results", "null")
            .add("callerSig", "38918a453d07199354f8b19af05ec6562ced5788")
            .build()
        val request = Request.Builder()
            .url(TOKEN_AUTH_URL)
            .post(body)
            .header("app", "com.google.android.gms")
            .header("User-Agent", "")
            .build()
        return try {
            container.okHttpClient.newCall(request).execute().use { response ->
                val values = response.body?.string().orEmpty()
                    .lineSequence()
                    .mapNotNull { line ->
                        val separator = line.indexOf('=')
                        if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
                    }
                    .toMap()
                if (!response.isSuccessful) {
                    return TokenExchange.Failure(authFailureMessage(response.code, values))
                }
                values["Error"]?.let { error ->
                    return TokenExchange.Failure(authFailureMessage(response.code, values))
                }
                val token = values["Token"].orEmpty()
                val responseEmail = values["Email"].orEmpty().ifBlank { email }
                if (token.isBlank()) return TokenExchange.Failure("Google did not return an account token.")
                val displayName = listOf(values["firstName"], values["lastName"])
                    .filterNotNull().joinToString(" ").trim().ifBlank { null }
                TokenExchange.Success(
                    AuthSession(responseEmail, token, displayName, null, System.currentTimeMillis())
                )
            }
        } catch (_: IOException) {
            TokenExchange.Failure("Google sign-in is unavailable. Check your connection and try again.")
        } catch (_: Exception) {
            TokenExchange.Failure("Google sign-in could not be completed safely.")
        }
    }

    private fun authFailureMessage(httpCode: Int, values: Map<String, String>): String {
        return when (val error = values["Error"].orEmpty()) {
            "BadAuthentication" -> "Google rejected the session. Sign in again and verify the account."
            "CaptchaRequired" -> "Google requires an additional account verification step. Complete it in a browser, then try again."
            "AccountDeleted" -> "This Google account has been deleted."
            "AccountDisabled" -> "This Google account is disabled."
            "ServiceUnavailable" -> "Google authentication is temporarily unavailable. Try again later."
            else -> if (error.isNotBlank()) {
                "Google authentication failed: $error (HTTP $httpCode)."
            } else {
                "Google rejected sign-in with HTTP $httpCode."
            }
        }
    }

    private fun parseCookies(header: String?): Map<String, String> = header.orEmpty()
        .split(';')
        .mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) null else part.substring(0, separator).trim() to part.substring(separator + 1).trim()
        }
        .toMap()

    private fun isAllowedGoogleUrl(uri: Uri): Boolean {
        if (uri.toString() == "about:blank") return true
        if (!uri.scheme.equals("https", true)) return false
        return uri.host?.lowercase(Locale.US) in ALLOWED_GOOGLE_HOSTS
    }

    private fun finishSuccessfully() = finishResult(Activity.RESULT_OK, null)

    private fun finishWithError(message: String) {
        if (!completing.compareAndSet(false, true) && webView == null) return
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finishResult(RESULT_AUTH_FAILED, message)
    }

    private fun finishResult(code: Int, error: String?) {
        lifecycleScope.launch {
            clearCookies()
            destroyWebView()
            setResult(code, Intent().apply { error?.let { putExtra(EXTRA_ERROR, it) } })
            finish()
        }
    }

    private suspend fun clearCookies() = suspendCancellableCoroutine { continuation ->
        runCatching {
            cookieManager.removeAllCookies {
                cookieManager.flush()
                if (continuation.isActive) continuation.resume(Unit)
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }

    private fun destroyWebView() {
        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            clearCache(true)
            clearFormData()
            removeAllViews()
            destroy()
        }
        webView = null
    }

    override fun onDestroy() {
        destroyWebView()
        super.onDestroy()
    }

    private sealed interface TokenExchange {
        data class Success(val session: AuthSession) : TokenExchange
        data class Failure(val message: String) : TokenExchange
    }

    companion object {
        private const val EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup"
        private const val AUTH_TOKEN = "oauth_token"
        private const val TOKEN_AUTH_URL = "https://android.clients.google.com/auth"
        private const val BUILD_VERSION_SDK = 28
        private const val PLAY_SERVICES_VERSION_CODE = 19_629_032
        const val RESULT_AUTH_FAILED = 2
        const val EXTRA_ERROR = "auth_error"
        private val ALLOWED_GOOGLE_HOSTS = setOf(
            "accounts.google.com",
            "accounts.googleusercontent.com",
            "myaccount.google.com",
            "support.google.com"
        )
    }
}
