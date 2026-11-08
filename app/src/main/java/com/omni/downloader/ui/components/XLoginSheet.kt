package com.omni.downloader.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omni.downloader.ui.localization.LocalAppStrings
import com.omni.downloader.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * X (Twitter) 内置网页登录。
 *
 * 与 B 站登录一致：用户在内置 WebView 里正常登录，登录成功后从 CookieManager
 * 中截获 auth_token 与 ct0，回传给设置页保存。
 *
 * 注意：X 的登录页对 WebView 有一定风控，若网页登录受阻，
 * 用户仍可在设置页直接粘贴浏览器里复制出来的 Cookie。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XLoginSheet(
    onDismissRequest: () -> Unit,
    onCookieCaptured: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var captured by remember { mutableStateOf(false) }

    fun captureCookie(): String? {
        val cm = CookieManager.getInstance()
        // 登录成功后 auth_token / ct0 都挂在 .x.com 上；twitter.com 作为历史域名兜底
        val candidates = listOf(
            "https://x.com",
            "https://api.x.com",
            "https://twitter.com"
        )
        for (domain in candidates) {
            val cookie = try { cm.getCookie(domain) } catch (_: Exception) { null } ?: continue
            if (cookie.contains("auth_token=") && cookie.contains("ct0=")) {
                return cookie
            }
        }
        return null
    }

    // 定时轮询，登录一旦完成立刻截获并退出
    LaunchedEffect(captured) {
        if (captured) return@LaunchedEffect
        while (isActive && !captured) {
            val cookie = captureCookie()
            if (!cookie.isNullOrBlank()) {
                captured = true
                onCookieCaptured(cookie)
                return@LaunchedEffect
            }
            delay(1000)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strings.xLoginDialogTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.xLoginAutoDetecting,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = strings.bilibiliCloseDialog
                        )
                    }
                }

                AnimatedVisibility(visible = isLoading) {
                    LinearProgressIndicator(
                        progress = { loadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                            }

                            val currentWebView = this
                            CookieManager.getInstance().apply {
                                setAcceptCookie(true)
                                setAcceptThirdPartyCookies(currentWebView, true)
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    if (!captured) {
                                        val cookie = captureCookie()
                                        if (!cookie.isNullOrBlank()) {
                                            captured = true
                                            onCookieCaptured(cookie)
                                        }
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    // 登录过程中的跳转留在内置浏览器内完成
                                    if (url.contains("x.com") || url.contains("twitter.com")) {
                                        return false
                                    }
                                    return super.shouldOverrideUrlLoading(view, request)
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    loadProgress = newProgress / 100f
                                    if (newProgress >= 100) {
                                        isLoading = false
                                    }
                                }
                            }

                            loadUrl("https://x.com/i/flow/login")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}
