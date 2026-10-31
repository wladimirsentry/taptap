package app.tocamatik;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Todo lo que Tocamatik guarda: rutinas, rutina activa, consentimiento y estadísticas del día. */
final class Store {

    private Store() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("tocamatik", Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------- consentimiento

    static boolean consent(Context c) {
        return sp(c).getBoolean("consent", false);
    }

    static void setConsent(Context c, boolean v) {
        sp(c).edit().putBoolean("consent", v).apply();
    }

    /** ¿El servicio está activado en Ajustes › Accesibilidad? */
    static boolean serviceEnabled(Context c) {
        if (TocamatikService.instance != null) return true;
        try {
            String list = Settings.Secure.getString(c.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (list == null) return false;
            ComponentName me = new ComponentName(c, TocamatikService.class);
            TextUtils.SimpleStringSplitter split = new TextUtils.SimpleStringSplitter(':');
            split.setString(list);
            while (split.hasNext()) {
                ComponentName cn = ComponentName.unflattenFromString(split.next());
                if (me.equals(cn)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    // ------------------------------------------------------------- rutinas

    static List<Routine> routines(Context c) {
        List<Routine> list = new ArrayList<>();
        String raw = sp(c).getString("routines", null);
        if (raw == null) {
            list = defaults();
            saveRoutines(c, list);
            return list;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(Routine.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        if (list.isEmpty()) {
            list = defaults();
            saveRoutines(c, list);
        }
        return list;
    }

    static void saveRoutines(Context c, List<Routine> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Routine r : list) arr.put(r.toJson());
        } catch (JSONException ignored) {
        }
        sp(c).edit().putString("routines", arr.toString()).apply();
    }

    static Routine byId(Context c, String id) {
        if (id == null) return null;
        for (Routine r : routines(c)) if (r.id.equals(id)) return r;
        return null;
    }

    static Routine active(Context c) {
        List<Routine> list = routines(c);
        String id = sp(c).getString("active", null);
        for (Routine r : list) if (r.id.equals(id)) return r;
        return list.get(0);
    }

    static void setActive(Context c, String id) {
        sp(c).edit().putString("active", id).apply();
    }

    /** Guarda una rutina nueva o reemplaza la que tenga el mismo id. */
    static void upsert(Context c, Routine routine) {
        List<Routine> list = routines(c);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(routine.id)) {
                list.set(i, routine);
                found = true;
                break;
            }
        }
        if (!found) list.add(routine);
        saveRoutines(c, list);
    }

    static void delete(Context c, String id) {
        List<Routine> list = routines(c);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id.equals(id)) list.remove(i);
        }
        if (list.isEmpty()) list = defaults();
        saveRoutines(c, list);
        if (id.equals(sp(c).getString("active", null))) setActive(c, list.get(0).id);
    }

    private static List<Routine> defaults() {
        List<Routine> list = new ArrayList<>();

        Routine fast = new Routine();
        fast.name = "Toque rápido";
        fast.icon = Routine.ICON_ZAP;
        fast.intervalMs = 83;
        fast.natural = true;
        list.add(fast);

        Routine combo = new Routine();
        combo.name = "Combo 3 puntos";
        combo.icon = Routine.ICON_CROSSHAIR;
        combo.intervalMs = 125;
        combo.natural = true;
        combo.stopMode = Routine.STOP_TAPS;
        combo.stopTaps = 1500;
        combo.points.add(new float[]{0.30f, 0.45f, Routine.DEFAULT_ZONE});
        combo.points.add(new float[]{0.50f, 0.55f, Routine.DEFAULT_ZONE});
        combo.points.add(new float[]{0.70f, 0.45f, Routine.DEFAULT_ZONE});
        list.add(combo);

        Routine slow = new Routine();
        slow.name = "Recolectar";
        slow.icon = Routine.ICON_TIMER;
        slow.intervalMs = 30_000;
        slow.natural = true;
        slow.stopMode = Routine.STOP_TIME;
        slow.stopMinutes = 120;
        list.add(slow);
        return list;
    }

    // ------------------------------------------------------------- estadísticas del día

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static void rollDay(Context c) {
        SharedPreferences p = sp(c);
        String t = today();
        if (!t.equals(p.getString("statsDay", ""))) {
            p.edit().putString("statsDay", t).putLong("tapsToday", 0).putLong("msToday", 0).apply();
        }
    }

    static long tapsToday(Context c) {
        rollDay(c);
        return sp(c).getLong("tapsToday", 0);
    }

    static long msToday(Context c) {
        rollDay(c);
        return sp(c).getLong("msToday", 0);
    }

    static void addStats(Context c, long taps, long ms) {
        if (taps <= 0 && ms <= 0) return;
        rollDay(c);
        SharedPreferences p = sp(c);
        p.edit()
                .putLong("tapsToday", p.getLong("tapsToday", 0) + Math.max(0, taps))
                .putLong("msToday", p.getLong("msToday", 0) + Math.max(0, ms))
                .apply();
    }
}
