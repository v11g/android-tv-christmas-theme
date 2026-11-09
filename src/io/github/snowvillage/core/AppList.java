package io.github.snowvillage.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Orders the apps shown along the snow road. */
public final class AppList {
    /** Pseudo package for the system Settings tile. */
    public static final String SETTINGS = "android.settings";

    public static final class Entry {
        public final String pkg, label;

        public Entry(String pkg, String label) {
            this.pkg = pkg;
            this.label = label;
        }
    }

    private AppList() {
    }

    /** A to Z by name (case ignored), without this app or repeated packages, Settings last. */
    public static List<Entry> order(List<Entry> apps, String self) {
        List<Entry> out = new ArrayList<>();
        Entry settings = null;
        Set<String> seen = new HashSet<>();
        for (Entry e : apps) {
            if (e.pkg.equals(self) || !seen.add(e.pkg)) continue;
            if (e.pkg.equals(SETTINGS)) settings = e; else out.add(e);
        }
        out.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        if (settings != null) out.add(settings);
        return out;
    }
}
