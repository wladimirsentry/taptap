package app.tocamatik;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Una rutina: velocidad, modo natural, cuándo detenerse y los puntos guardados. */
final class Routine {

    static final int STOP_NEVER = 0;
    static final int STOP_TAPS = 1;
    static final int STOP_TIME = 2;

    static final int ICON_ZAP = 0;
    static final int ICON_CROSSHAIR = 1;
    static final int ICON_TIMER = 2;
    static final int ICON_HAND = 3;

    static final int MAX_POINTS = 20;
    static final int MIN_INTERVAL = 50;      // 20 toques por segundo
    static final int MAX_INTERVAL = 3_600_000; // una hora

    String id = UUID.randomUUID().toString();
    String name = "Nueva rutina";
    int icon = ICON_ZAP;
    int intervalMs = 83;      // 12 toques por segundo
    boolean natural = true;
    int stopMode = STOP_NEVER;
    int stopTaps = 1000;
    int stopMinutes = 10;
    /** Centro de cada punto como fracción del ancho y alto de la pantalla (0..1). */
    final List<float[]> points = new ArrayList<>();

    Routine copy() {
        try {
            return fromJson(toJson());
        } catch (JSONException e) {
            return this;
        }
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("icon", icon);
        o.put("interval", intervalMs);
        o.put("natural", natural);
        o.put("stopMode", stopMode);
        o.put("stopTaps", stopTaps);
        o.put("stopMinutes", stopMinutes);
        JSONArray pts = new JSONArray();
        for (float[] p : points) {
            JSONArray a = new JSONArray();
            a.put((double) p[0]);
            a.put((double) p[1]);
            pts.put(a);
        }
        o.put("points", pts);
        return o;
    }

    static Routine fromJson(JSONObject o) {
        Routine r = new Routine();
        r.id = o.optString("id", r.id);
        r.name = o.optString("name", r.name);
        r.icon = o.optInt("icon", ICON_ZAP);
        r.intervalMs = clamp(o.optInt("interval", 83), MIN_INTERVAL, MAX_INTERVAL);
        r.natural = o.optBoolean("natural", true);
        r.stopMode = clamp(o.optInt("stopMode", STOP_NEVER), STOP_NEVER, STOP_TIME);
        r.stopTaps = clamp(o.optInt("stopTaps", 1000), 1, 10_000_000);
        r.stopMinutes = clamp(o.optInt("stopMinutes", 10), 1, 24 * 60);
        JSONArray pts = o.optJSONArray("points");
        if (pts != null) {
            for (int i = 0; i < pts.length() && i < MAX_POINTS; i++) {
                JSONArray a = pts.optJSONArray(i);
                if (a == null || a.length() < 2) continue;
                float x = (float) a.optDouble(0, 0.5);
                float y = (float) a.optDouble(1, 0.5);
                r.points.add(new float[]{clampF(x), clampF(y)});
            }
        }
        return r;
    }

    // ------------------------------------------------------------- textos

    boolean isFast() {
        return intervalMs < 1000;
    }

    /** Toques por segundo, redondeado (solo para ritmos rápidos). */
    int perSecond() {
        return Math.max(1, Math.round(1000f / intervalMs));
    }

    /** "12/s" o "cada 30 s". */
    String rateShort() {
        if (isFast()) return perSecond() + "/s";
        return "cada " + seconds(intervalMs);
    }

    String stopText() {
        switch (stopMode) {
            case STOP_TAPS:
                return Ui.number(stopTaps) + " toques";
            case STOP_TIME:
                return minutes(stopMinutes);
            default:
                return "";
        }
    }

    String summary() {
        StringBuilder sb = new StringBuilder();
        int n = points.size();
        sb.append(n == 0 ? "Sin puntos" : (n == 1 ? "1 punto" : n + " puntos"));
        sb.append(" · ").append(rateShort());
        if (natural) sb.append(" · natural");
        String stop = stopText();
        if (!stop.isEmpty()) sb.append(" · ").append(stop);
        return sb.toString();
    }

    int iconRes() {
        switch (icon) {
            case ICON_CROSSHAIR: return R.drawable.ic_crosshair;
            case ICON_TIMER: return R.drawable.ic_timer;
            case ICON_HAND: return R.drawable.ic_hand;
            default: return R.drawable.ic_zap;
        }
    }

    int iconColor() {
        switch (icon) {
            case ICON_CROSSHAIR: return Ui.AMBER;
            case ICON_TIMER: return Ui.MINT;
            case ICON_HAND: return 0xFFE0457B;
            default: return Ui.P;
        }
    }

    int iconBg() {
        switch (icon) {
            case ICON_CROSSHAIR: return Ui.AMBER_S;
            case ICON_TIMER: return Ui.MINT_S;
            case ICON_HAND: return 0xFFFDE7EF;
            default: return Ui.PS;
        }
    }

    static String seconds(int ms) {
        if (ms < 60_000) {
            if (ms % 1000 == 0) return (ms / 1000) + " s";
            return String.format(java.util.Locale.US, "%.1f s", ms / 1000f).replace('.', ',');
        }
        int s = ms / 1000;
        if (s % 60 == 0) return (s / 60) + " min";
        return (s / 60) + " min " + (s % 60) + " s";
    }

    static String minutes(int min) {
        if (min < 60) return min + " min";
        if (min % 60 == 0) return (min / 60) + " h";
        return (min / 60) + " h " + (min % 60) + " min";
    }

    static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    static float clampF(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
