package com.example.pinsk

import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PanoramaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panorama)

        val webView = findViewById<WebView>(R.id.webViewPanorama)

        webView.webChromeClient = android.webkit.WebChromeClient()
        webView.webViewClient = android.webkit.WebViewClient()

        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            domStorageEnabled = true

            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true

            databaseEnabled = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.loadUrl("file:///android_asset/tour.html")

        findViewById<Button>(R.id.btnExitPano).setOnClickListener { finish() }
    }
}