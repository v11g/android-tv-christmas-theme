package io.github.snowvillage.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Plain-Java tests for the Android-free core. Run with ./test.sh; exits non-zero on failure. */
public final class AppListTest {
    private static int failures = 0;

    public static void main(String[] args) {
        sortsByNameIgnoringCase();
        hidesSelfAndDuplicates();
        settingsGoesLast();
        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    static void sortsByNameIgnoringCase() {
        List<AppList.Entry> in = Arrays.asList(e("p.yt", "YouTube"), e("p.jf", "jellyfin"), e("p.ap", "Apple TV"));
        check("a-z, case ignored", labels(AppList.order(in, "self")), Arrays.asList("Apple TV", "jellyfin", "YouTube"));
    }

    static void hidesSelfAndDuplicates() {
        List<AppList.Entry> in = Arrays.asList(e("self", "Snow Village"), e("p.kodi", "Kodi"), e("p.kodi", "Kodi"));
        check("own app and repeated packages hidden", labels(AppList.order(in, "self")), Arrays.asList("Kodi"));
    }

    static void settingsGoesLast() {
        List<AppList.Entry> in = Arrays.asList(e(AppList.SETTINGS, "Settings"), e("p.vlc", "VLC"), e("p.a", "Amazon"));
        check("settings at the end", labels(AppList.order(in, "self")), Arrays.asList("Amazon", "VLC", "Settings"));
    }

    private static AppList.Entry e(String pkg, String label) {
        return new AppList.Entry(pkg, label);
    }

    private static List<String> labels(List<AppList.Entry> list) {
        List<String> out = new ArrayList<>();
        for (AppList.Entry x : list) out.add(x.label);
        return out;
    }

    private static void check(String name, Object got, Object want) {
        if (!got.equals(want)) {
            failures++;
            System.out.println("FAIL " + name + ": got " + got + ", want " + want);
        }
    }
}
