package com.playm3u.app;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.webkit.ConsoleMessage;
import android.os.Build;
import android.util.Log;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;
    private static final String TAG = "PlayM3U";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Force landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Hide system UI
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        webView = new WebView(this);
        setContentView(webView);

        setupWebView();

        // Load dari assets (file lokal = TIDAK ADA CORS!)
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // JavaScript wajib
        settings.setJavaScriptEnabled(true);

        // Izinkan mixed content (HTTP stream dari halaman HTTPS/file://)
        // Ini solusi utama masalah Mixed Content!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Media autoplay tanpa gesture user
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }

        // Storage & cache
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Zoom
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // User agent TV-friendly
        String ua = settings.getUserAgentString();
        settings.setUserAgentString(ua + " PlayM3U/1.0 AndroidTV");

        // Hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // WebViewClient — intercept request untuk bypass CORS pada fetch M3U
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Semua navigasi tetap di WebView
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Intercept request ke URL eksternal yang mungkin kena CORS
                // Tambahkan CORS headers ke response
                if (url.startsWith("http") && !url.contains("android_asset")) {
                    try {
                        return fetchWithCorsHeaders(url, request);
                    } catch (Exception e) {
                        Log.w(TAG, "Intercept failed for: " + url + " - " + e.getMessage());
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "WebView error: " + errorCode + " - " + description + " - " + failingUrl);
            }
        });

        // WebChromeClient — untuk video fullscreen & console log
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage msg) {
                Log.d(TAG, "JS Console [" + msg.messageLevel() + "]: " + msg.message());
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Video fullscreen
                getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                );
            }
        });

        // JavaScript Interface — bridge Java <-> JS jika diperlukan
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
    }

    /**
     * Fetch URL langsung dari Java (bypass CORS sepenuhnya)
     * dan kembalikan sebagai WebResourceResponse dengan CORS headers
     */
    private WebResourceResponse fetchWithCorsHeaders(String urlStr, WebResourceRequest request) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(request.getMethod() != null ? request.getMethod() : "GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 PlayM3U/1.0");

            // Forward request headers
            Map<String, String> reqHeaders = request.getRequestHeaders();
            if (reqHeaders != null) {
                for (Map.Entry<String, String> h : reqHeaders.entrySet()) {
                    String key = h.getKey();
                    // Skip headers yang bisa menyebabkan masalah
                    if (!key.equalsIgnoreCase("Origin") && !key.equalsIgnoreCase("Referer")) {
                        conn.setRequestProperty(key, h.getValue());
                    }
                }
            }

            conn.connect();
            int code = conn.getResponseCode();

            // Buat CORS headers untuk response
            Map<String, String> corsHeaders = new HashMap<>();
            corsHeaders.put("Access-Control-Allow-Origin", "*");
            corsHeaders.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            corsHeaders.put("Access-Control-Allow-Headers", "*");

            // Copy Content-Type dari response asli
            String contentType = conn.getContentType();
            if (contentType == null) contentType = "application/octet-stream";

            // Pisah mime type dan charset
            String mimeType = contentType.split(";")[0].trim();
            String encoding = "utf-8";
            if (contentType.contains("charset=")) {
                encoding = contentType.split("charset=")[1].trim().split(";")[0].trim();
            }

            InputStream stream;
            if (code >= 200 && code < 300) {
                stream = conn.getInputStream();
            } else {
                // Kembalikan error response
                return new WebResourceResponse(mimeType, encoding, code,
                    "Error", corsHeaders,
                    new ByteArrayInputStream(new byte[0]));
            }

            return new WebResourceResponse(mimeType, encoding, code,
                "OK", corsHeaders, stream);

        } catch (Exception e) {
            Log.w(TAG, "fetchWithCorsHeaders error: " + e.getMessage());
            return null; // Fallback ke default WebView behavior
        }
    }

    // JavaScript Bridge
    public class AndroidBridge {
        @JavascriptInterface
        public String getPlatform() {
            return "android";
        }

        @JavascriptInterface
        public boolean isAndroidTV() {
            return getPackageManager().hasSystemFeature("android.hardware.type.television") ||
                   getPackageManager().hasSystemFeature("android.software.leanback");
        }

        @JavascriptInterface
        public void log(String msg) {
            Log.d(TAG, "JS: " + msg);
        }
    }

    // Handle tombol remote TV
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // Kirim event ke JavaScript
                webView.evaluateJavascript(
                    "(function(){ var e = new KeyboardEvent('keydown', {key:'Backspace',bubbles:true}); document.dispatchEvent(e); })()",
                    null
                );
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                webView.evaluateJavascript(
                    "(function(){ var e = new KeyboardEvent('keydown', {key:'ArrowUp',bubbles:true}); document.dispatchEvent(e); })()",
                    null
                );
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                webView.evaluateJavascript(
                    "(function(){ var e = new KeyboardEvent('keydown', {key:'ArrowDown',bubbles:true}); document.dispatchEvent(e); })()",
                    null
                );
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                webView.evaluateJavascript(
                    "(function(){ var e = new KeyboardEvent('keydown', {key:'ArrowLeft',bubbles:true}); document.dispatchEvent(e); })()",
                    null
                );
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                webView.evaluateJavascript(
                    "(function(){ var e = new KeyboardEvent('keydown', {key:'ArrowRight',bubbles:true}); document.dispatchEvent(e); })()",
                    null
                );
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                webView.evaluateJavascript(
                    "(function(){ var e = new KeyboardEvent('keydown', {key:'Enter',bubbles:true}); document.dispatchEvent(e); })()",
                    null
                );
                return true;

            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_PAGE_UP:
                webView.evaluateJavascript(
                    "(function(){ var e = new KeyboardEvent('keydown', {key:'ChannelUp',bubbles:true}); document.dispatchEvent(e); })()",
                    null
                );
                return true;

            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                webView.evaluateJavascript(
                    "(function(){ var e = new KeyboardEvent('keydown', {key:'ChannelDown',bubbles:true}); document.dispatchEvent(e); })()",
                    null
                );
                return true;

            case KeyEvent.KEYCODE_INFO:
                webView.evaluateJavascript(
                    "(function(){ var e = new KeyboardEvent('keydown', {key:'Info',bubbles:true}); document.dispatchEvent(e); })()",
                    null
                );
                return true;

            default:
                // Angka 0-9 untuk input channel
                if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                    int digit = keyCode - KeyEvent.KEYCODE_0;
                    webView.evaluateJavascript(
                        "(function(){ var e = new KeyboardEvent('keydown', {key:'" + digit + "',bubbles:true}); document.dispatchEvent(e); })()",
                        null
                    );
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            webView.evaluateJavascript(
                "(function(){ var e = new KeyboardEvent('keydown', {key:'Backspace',bubbles:true}); document.dispatchEvent(e); })()",
                null
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        webView.pauseTimers();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
