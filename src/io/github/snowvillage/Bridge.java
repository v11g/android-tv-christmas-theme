package io.github.snowvillage;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.snowvillage.core.AppList;

/** What the page may ask the TV for: the list of apps (name + real icon) and opening one of them. */
final class Bridge {
    private static final int ICON_PX = 128;
    private final Activity activity;
    private final PackageManager pm;

    Bridge(Activity activity) {
        this.activity = activity;
        this.pm = activity.getPackageManager();
    }

    /** JSON list of {pkg, label, icon}: TV apps A to Z, then Settings. Icons are PNG data URLs taken from the TV. */
    @JavascriptInterface
    public String apps() {
        List<AppList.Entry> entries = new ArrayList<>();
        Map<String, ResolveInfo> byPkg = collect(entries);
        JSONArray out = new JSONArray();
        for (AppList.Entry e : AppList.order(entries, activity.getPackageName())) {
            try {
                out.put(new JSONObject().put("pkg", e.pkg).put("label", e.label).put("icon", icon(byPkg.get(e.pkg).loadIcon(pm))));
            } catch (Exception ignored) { // an app whose icon cannot be read is simply left off the road
            }
        }
        return out.toString();
    }

    /** Fills `entries` with the TV apps plus Settings and returns how to reach each one's icon. */
    private Map<String, ResolveInfo> collect(List<AppList.Entry> entries) {
        Map<String, ResolveInfo> byPkg = new HashMap<>();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(query, 0)) {
            byPkg.put(r.activityInfo.packageName, r);
            entries.add(new AppList.Entry(r.activityInfo.packageName, r.loadLabel(pm).toString()));
        }
        ResolveInfo settings = pm.resolveActivity(new Intent(Settings.ACTION_SETTINGS), 0);
        if (settings != null) {
            byPkg.put(AppList.SETTINGS, settings);
            entries.add(new AppList.Entry(AppList.SETTINGS, "Settings"));
        }
        return byPkg;
    }

    /** Opens an app by package; unknown or removed apps are ignored rather than crashing the home screen. */
    @JavascriptInterface
    public void launch(String pkg) {
        activity.runOnUiThread(() -> {
            Intent i = AppList.SETTINGS.equals(pkg) ? new Intent(Settings.ACTION_SETTINGS) : launchIntent(pkg);
            if (i == null) return;
            try {
                activity.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (RuntimeException e) {
                android.util.Log.w("SnowVillage", "could not open " + pkg, e);
            }
        });
    }

    private Intent launchIntent(String pkg) {
        Intent tv = pm.getLeanbackLaunchIntentForPackage(pkg);
        return tv != null ? tv : pm.getLaunchIntentForPackage(pkg);
    }

    private static String icon(Drawable d) {
        Bitmap b = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, ICON_PX, ICON_PX);
        d.draw(new Canvas(b));
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, png);
        return "data:image/png;base64," + Base64.encodeToString(png.toByteArray(), Base64.NO_WRAP);
    }
}
