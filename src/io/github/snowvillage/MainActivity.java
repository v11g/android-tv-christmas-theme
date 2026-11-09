package io.github.snowvillage;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Full-screen WebView showing the snow road. Works as a normal app, or as the TV's Home screen if the user sets it. */
public final class MainActivity extends Activity {
    private static final String PAGE = "file:///android_asset/web/index.html";
    private WebView web;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true); // remembers the chosen theme
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !request.getUrl().toString().startsWith("file:///android_asset/"); // the page never leaves the app
            }
        });
        web.addJavascriptInterface(new Bridge(this), "Android");
        web.loadUrl(PAGE);
        setContentView(web);
        web.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.evaluateJavascript("window.SV && SV.resume()", null); // back from an app: reset the opening animation
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            web.evaluateJavascript("window.SV && SV.press('Menu')", null);
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    /** Back closes the theme panel first; on the snow road it leaves the app, unless this app is the Home screen. */
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.SV ? SV.back() : false", used -> {
            if (!"true".equals(used) && !isHome()) finish();
        });
    }

    private boolean isHome() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo r = getPackageManager().resolveActivity(home, 0);
        return r != null && getPackageName().equals(r.activityInfo.packageName);
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
