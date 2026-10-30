package app.tocamatik;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Pantalla de bienvenida (mientras falta el permiso) o de inicio (cuando ya está activo). */
public class MainActivity extends Activity {

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = Ui.col(this);
        root.setBackgroundColor(Ui.BG);
        setContentView(root);
        Ui.edgeToEdge(this, root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        root.removeAllViews();
        boolean ready = Store.consent(this) && Store.serviceEnabled(this);
        if (ready) buildHome();
        else buildWelcome();
    }

    /** Abre Ajustes › Accesibilidad, intentando resaltar Tocamatik donde Android lo permite. */
    static void openAccessibilitySettings(Context c) {
        Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        String key = new ComponentName(c, TocamatikService.class).flattenToString();
        Bundle args = new Bundle();
        args.putString(":settings:fragment_args_key", key);
        i.putExtra(":settings:fragment_args_key", key);
        i.putExtra(":settings:show_fragment_args", args);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
        } catch (Exception e) {
            c.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    // =================================================================== bienvenida

    private void buildWelcome() {
        boolean consent = Store.consent(this);
        boolean enabled = Store.serviceEnabled(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = Ui.col(this);
        content.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), Ui.dp(this, 16));
        scroll.addView(content);

        content.addView(hero());

        content.addView(Ui.text(this, "Toca por ti,\na tu ritmo.", 30, Ui.INK, Ui.W_EXTRA));
        TextView sub = Ui.text(this,
                "Marca puntos en la pantalla y Tocamatik los toca solo, sobre cualquier app. Solo necesita un permiso.",
                14.5f, Ui.MUTED, Ui.W_MEDIUM);
        sub.setLineSpacing(0, 1.3f);
        content.addView(sub, Ui.margins(Ui.lpMatchWrap(), this, 0, 10, 0, 0));

        LinearLayout steps = Ui.card(this);
        steps.addView(step("1", "Instalar Tocamatik", "Listo", true, false));
        steps.addView(line());
        steps.addView(step("2", "Leer y aceptar el aviso", "Qué hace Tocamatik con el permiso", consent, !consent));
        steps.addView(line());
        steps.addView(step("3", "Activar accesibilidad",
                enabled ? "Activado" : "Ajustes › Accesibilidad › Tocamatik", enabled, consent && !enabled));
        content.addView(steps, Ui.margins(Ui.lpMatchWrap(), this, 0, 20, 0, 0));

        LinearLayout note = Ui.row(this);
        note.addView(Ui.icon(this, R.drawable.ic_shield_check, Ui.MINT, 16));
        TextView nt = Ui.text(this, "Tocamatik no lee ni guarda nada de tu pantalla.", 12, Ui.MUTED, Ui.W_BOLD);
        nt.setPadding(Ui.dp(this, 8), 0, 0, 0);
        note.addView(nt);
        content.addView(note, Ui.margins(Ui.lpMatchWrap(), this, 4, 14, 0, 0));

        if (consent && !enabled) {
            TextView help = Ui.text(this,
                    "¿No encuentras Tocamatik? En algunos teléfonos está dentro de \"Apps instaladas\" o \"Servicios descargados\". "
                            + "Si aparece bloqueado (\"Ajuste restringido\"), ve a Ajustes › Apps › Tocamatik › ⋮ › Permitir ajustes restringidos.",
                    12, Ui.MUTED, Ui.W_MEDIUM);
            help.setLineSpacing(0, 1.3f);
            content.addView(help, Ui.margins(Ui.lpMatchWrap(), this, 4, 10, 0, 0));
        }

        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        String label = !consent ? "Continuar" : "Abrir ajustes de accesibilidad";
        LinearLayout cta = Ui.button(this, label, Ui.INK, Color.WHITE, R.drawable.ic_chevron_right);
        cta.setOnClickListener(v -> {
            if (!Store.consent(this)) startActivity(new Intent(this, DisclosureActivity.class));
            else openAccessibilitySettings(this);
        });
        root.addView(cta, Ui.margins((LinearLayout.LayoutParams) cta.getLayoutParams(), this, 20, 8, 20, 16));
    }

    private View hero() {
        FrameLayout f = new FrameLayout(this);
        f.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 300)));

        int[] rings = {150, 220, 290};
        for (int i = 0; i < rings.length; i++) {
            View r = new View(this);
            GradientDrawable d = Ui.oval(Color.TRANSPARENT);
            d.setStroke(Ui.dp(this, 1.5f), i == 2 ? 0x125B47F5 : 0x225B47F5);
            r.setBackground(d);
            f.addView(r, new FrameLayout.LayoutParams(Ui.dp(this, rings[i]), Ui.dp(this, rings[i]), Gravity.CENTER));
        }
        Views.LogoView logo = new Views.LogoView(this, false);
        logo.setRotation(-6f);
        logo.setElevation(Ui.dp(this, 12));
        f.addView(logo, new FrameLayout.LayoutParams(Ui.dp(this, 112), Ui.dp(this, 112), Gravity.CENTER));

        f.addView(floatChip(R.drawable.ic_gauge, Ui.P, "12 toques/s"), chipLp(Gravity.TOP | Gravity.START, 14, 56));
        f.addView(floatChip(R.drawable.ic_sparkles, Ui.MINT, "Modo natural"), chipLp(Gravity.TOP | Gravity.END, 10, 110));
        f.addView(floatChip(R.drawable.ic_crosshair, Ui.AMBER, "Hasta 20 puntos"), chipLp(Gravity.BOTTOM | Gravity.START, 32, 40));
        return f;
    }

    private FrameLayout.LayoutParams chipLp(int gravity, int sideDp, int vertDp) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, gravity);
        int side = Ui.dp(this, sideDp), vert = Ui.dp(this, vertDp);
        if ((gravity & Gravity.END) == Gravity.END) lp.rightMargin = side;
        else lp.leftMargin = side;
        if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) lp.bottomMargin = vert;
        else lp.topMargin = vert;
        return lp;
    }

    private View floatChip(int icon, int color, String label) {
        LinearLayout c = Ui.row(this);
        c.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        c.setBackground(Ui.roundStroke(Color.WHITE, Ui.dp(this, 14), Ui.dp(this, 1), Ui.LINE));
        c.setElevation(Ui.dp(this, 6));
        c.addView(Ui.icon(this, icon, color, 16));
        TextView t = Ui.text(this, label, 12, Ui.INK, Ui.W_BOLD);
        t.setPadding(Ui.dp(this, 7), 0, 0, 0);
        c.addView(t);
        return c;
    }

    private View step(String num, String title, String sub, boolean done, boolean current) {
        LinearLayout r = Ui.row(this);
        r.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        LinearLayout badge;
        if (done) {
            badge = Ui.iconBox(this, R.drawable.ic_check, Ui.MINT, Ui.MINT_S, 34, 18, 12);
        } else {
            badge = Ui.row(this);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(Ui.round(current ? Ui.P : Ui.BG, Ui.dp(this, 12)));
            badge.addView(Ui.text(this, num, 14, current ? Color.WHITE : Ui.MUTED, Ui.W_EXTRA));
            badge.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)));
        }
        r.addView(badge);
        LinearLayout txt = Ui.col(this);
        txt.setPadding(Ui.dp(this, 14), 0, 0, 0);
        txt.addView(Ui.text(this, title, 14.5f, Ui.INK, Ui.W_BOLD));
        TextView s = Ui.text(this, sub, 12.5f, Ui.MUTED, Ui.W_MEDIUM);
        s.setPadding(0, Ui.dp(this, 3), 0, 0);
        txt.addView(s);
        r.addView(txt);
        return r;
    }

    private View line() {
        View v = new View(this);
        v.setBackgroundColor(Ui.LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));
        return v;
    }

    // =================================================================== inicio

    private void buildHome() {
        Routine active = Store.active(this);

        // Cabecera
        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 6));
        Views.LogoView logo = new Views.LogoView(this, false);
        header.addView(logo, new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)));
        TextView brand = Ui.text(this, "Tocamatik", 21, Ui.INK, Ui.W_EXTRA);
        brand.setPadding(Ui.dp(this, 10), 0, 0, 0);
        header.addView(brand);
        header.addView(Ui.weightSpace(this));
        LinearLayout settings = Ui.iconButton(this, R.drawable.ic_settings_2, "Ajustes");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settings);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.col(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // Tarjeta de estado
        LinearLayout status = Ui.col(this);
        status.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        status.setBackground(Ui.gradient(Ui.dp(this, 26), Ui.P, Ui.P_DARK));
        LinearLayout st = Ui.row(this);
        View dot = new View(this);
        GradientDrawable dd = Ui.oval(0xFF5BF0B5);
        dd.setStroke(Ui.dp(this, 3), 0x335BF0B5);
        dot.setBackground(dd);
        st.addView(dot, new LinearLayout.LayoutParams(Ui.dp(this, 12), Ui.dp(this, 12)));
        TextView stt = Ui.text(this, "Servicio activo", 13, 0xF2FFFFFF, Ui.W_BOLD);
        stt.setPadding(Ui.dp(this, 9), 0, 0, 0);
        st.addView(stt);
        status.addView(st);
        status.addView(Ui.text(this, active.name, 25, Color.WHITE, Ui.W_EXTRA),
                Ui.margins(Ui.lpWrap(), this, 0, 12, 0, 0));

        LinearLayout chips = Ui.row(this);
        chips.addView(Ui.chip(this, active.rateShort(), Color.WHITE, 0x1FFFFFFF));
        if (active.natural) chips.addView(Ui.chip(this, "Natural", Color.WHITE, 0x1FFFFFFF), Ui.margins(Ui.lpWrap(), this, 7, 0, 0, 0));
        int n = active.points.size();
        chips.addView(Ui.chip(this, n == 1 ? "1 punto" : n + " puntos", Color.WHITE, 0x1FFFFFFF),
                Ui.margins(Ui.lpWrap(), this, 7, 0, 0, 0));
        status.addView(chips, Ui.margins(Ui.lpMatchWrap(), this, 0, 12, 0, 0));

        LinearLayout open = Ui.button(this, "Abrir panel flotante", Color.WHITE, Ui.P, R.drawable.ic_layers);
        open.setOnClickListener(v -> openPanel());
        LinearLayout.LayoutParams olp = (LinearLayout.LayoutParams) open.getLayoutParams();
        olp.height = Ui.dp(this, 52);
        olp.topMargin = Ui.dp(this, 18);
        status.addView(open, olp);
        content.addView(status);

        // Estadísticas
        LinearLayout stats = Ui.row(this);
        stats.addView(stat("Toques hoy", Ui.number(Store.tapsToday(this))), statLp(0));
        stats.addView(stat("Tiempo activo", formatDuration(Store.msToday(this))), statLp(10));
        content.addView(stats, Ui.margins(Ui.lpMatchWrap(), this, 0, 14, 0, 0));

        // Rutinas
        LinearLayout rh = Ui.row(this);
        rh.addView(Ui.text(this, "MIS RUTINAS", 12, Ui.MUTED, Ui.W_BOLD));
        rh.addView(Ui.weightSpace(this));
        LinearLayout add = Ui.row(this);
        add.addView(Ui.icon(this, R.drawable.ic_plus, Ui.P, 16));
        TextView at = Ui.text(this, "Nueva", 13, Ui.P, Ui.W_EXTRA);
        at.setPadding(Ui.dp(this, 4), 0, 0, 0);
        add.addView(at);
        add.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 2), Ui.dp(this, 6));
        add.setClickable(true);
        add.setContentDescription("Nueva rutina");
        add.setOnClickListener(v -> startActivity(new Intent(this, EditorActivity.class)));
        rh.addView(add);
        content.addView(rh, Ui.margins(Ui.lpMatchWrap(), this, 2, 22, 0, 6));

        LinearLayout list = Ui.card(this);
        List<Routine> routines = Store.routines(this);
        for (int i = 0; i < routines.size(); i++) {
            if (i > 0) list.addView(line());
            list.addView(routineRow(routines.get(i), routines.get(i).id.equals(active.id)));
        }
        content.addView(list);

        TextView tip = Ui.text(this,
                "Toca una rutina para editarla. Toca ▶ para usarla. También puedes abrir el panel con el botón de accesibilidad de Android.",
                12, Ui.MUTED, Ui.W_MEDIUM);
        tip.setLineSpacing(0, 1.3f);
        content.addView(tip, Ui.margins(Ui.lpMatchWrap(), this, 4, 14, 4, 0));
    }

    private void openPanel() {
        TocamatikService s = TocamatikService.instance;
        if (s == null) {
            Toast.makeText(this, "Tocamatik se está iniciando. Si no aparece, desactívalo y actívalo en Accesibilidad.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        s.showOverlay(true);
        moveTaskToBack(true);
    }

    private LinearLayout.LayoutParams statLp(int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = Ui.dp(this, leftDp);
        return lp;
    }

    private View stat(String k, String v) {
        LinearLayout c = Ui.card(this);
        c.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        c.addView(Ui.text(this, k, 12, Ui.MUTED, Ui.W_BOLD));
        c.addView(Ui.text(this, v, 22, Ui.INK, Ui.W_EXTRA), Ui.margins(Ui.lpWrap(), this, 0, 6, 0, 0));
        return c;
    }

    private View routineRow(Routine r, boolean isActive) {
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        row.setBackground(Ui.pressable(Ui.round(isActive ? 0xFFFAF9FF : Color.WHITE, Ui.dp(this, 22)), 0x155B47F5));
        row.setClickable(true);
        row.setContentDescription("Editar " + r.name);
        row.setOnClickListener(v -> {
            Intent i = new Intent(this, EditorActivity.class);
            i.putExtra(EditorActivity.EXTRA_ID, r.id);
            startActivity(i);
        });

        row.addView(Ui.iconBox(this, r.iconRes(), r.iconColor(), r.iconBg(), 46, 20, 15));
        LinearLayout txt = Ui.col(this);
        txt.setPadding(Ui.dp(this, 13), 0, Ui.dp(this, 8), 0);
        txt.addView(Ui.text(this, r.name, 15, Ui.INK, Ui.W_BOLD));
        TextView s = Ui.text(this, r.summary(), 12.5f, Ui.MUTED, Ui.W_BOLD);
        s.setPadding(0, Ui.dp(this, 3), 0, 0);
        txt.addView(s);
        row.addView(txt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout action = Ui.row(this);
        action.setGravity(Gravity.CENTER);
        action.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        if (isActive) {
            action.setBackground(Ui.oval(Ui.P));
            action.addView(Ui.icon(this, R.drawable.ic_check, Color.WHITE, 16));
            action.setContentDescription(r.name + " está en uso");
        } else {
            action.setBackground(Ui.pressable(Ui.oval(Ui.BG), 0x225B47F5));
            action.addView(Ui.icon(this, R.drawable.ic_play, Ui.INK, 16));
            action.setContentDescription("Usar " + r.name);
            action.setClickable(true);
            action.setOnClickListener(v -> {
                Store.setActive(this, r.id);
                TocamatikService s2 = TocamatikService.instance;
                if (s2 != null) s2.reloadRoutine();
                render();
            });
        }
        row.addView(action);
        return row;
    }

    static String formatDuration(long ms) {
        long min = ms / 60_000;
        if (min < 1) return (ms / 1000) + " s";
        if (min < 60) return min + " min";
        return (min / 60) + " h " + (min % 60) + " min";
    }
}
