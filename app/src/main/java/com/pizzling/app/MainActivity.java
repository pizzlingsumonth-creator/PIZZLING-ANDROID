package com.pizzling.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import java.util.Map;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import java.io.*;

public class MainActivity extends Activity {
    private WebView web;
    private static final int PICK_HTML = 71;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost()) && "/assets/index.html".equals(u.getPath())) {
                    try {
                        File update = new File(getFilesDir(), "installed-update.html");
                        InputStream stream = update.exists()
                            ? new FileInputStream(update)
                            : getAssets().open("index.html");
                        return new WebResourceResponse("text/html", "UTF-8", stream);
                    } catch (Exception ignored) { }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("text/html");
                i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/html", "application/octet-stream"});
                startActivityForResult(i, PICK_HTML);
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "PizzlingNative");
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    private ValueCallback<Uri[]> fileCallback;

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_HTML && fileCallback != null) {
            Uri[] result = (resultCode == RESULT_OK && data != null) ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    private class Bridge {
        @JavascriptInterface public void installUpdate(String content) {
            try {
                File target = new File(getFilesDir(), "installed-update.html");
                File temp = new File(getFilesDir(), "installed-update.tmp");
                try (FileOutputStream out = new FileOutputStream(temp)) {
                    out.write(content.getBytes("UTF-8"));
                }
                if (target.exists()) target.delete();
                temp.renameTo(target);
                runOnUiThread(() -> web.loadUrl("https://appassets.androidplatform.net/assets/index.html"));
            } catch (Exception e) { }
        }
        @JavascriptInterface public void clearInstalledUpdate() {
            File f = new File(getFilesDir(), "installed-update.html");
            if (f.exists()) f.delete();
            runOnUiThread(() -> web.loadUrl("https://appassets.androidplatform.net/assets/index.html"));
        }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
