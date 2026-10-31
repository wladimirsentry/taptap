package app.tocamatik;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/** Crear o editar una rutina: nombre, icono, velocidad, ráfagas, modo natural, zona y detención automática. */
public class EditorActivity extends Activity {

    static final String EXTRA_ID = "id";

    private static final int[] FAST_PRESETS = {2, 5, 8, 12, 20};           // toques por segundo
    private static final int[] SLOW_PRESETS = {2, 5, 10, 30, 60};          // segundos entre toques

    private Routine r;
    private boolean isNew;

    private EditText nameIn;
    private final LinearLayout[] iconBoxes = new LinearLayout[4];
    private TextView[] speedSeg, stopSeg;
    private TextView bigNumber, bigUnit, stopValue;
    private LinearLayout fastBox, slowBox, stopRow, fastPresets, slowPresets;
    private SeekBar seek;
    private TextView slowValue;
    private Views.ToggleView natural;
    private Views.BarsView bars;
    private boolean fastMode;
    private LinearLayout burstBox;
    private TextView burstTapsValue, burstEveryValue, burstHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String id = getIntent().getStringExtra(EXTRA_ID);
        Routine existing = Store.byId(this, id);
        isNew = existing == null;
        r = isNew ? new Routine() : existing;
        if (isNew) r.name = "";
        fastMode = r.isFast();

        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.BG);

        // Cabecera
        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 6));
        LinearLayout back = Ui.iconButton(this, R.drawable.ic_chevron_left, "Volver");
        back.setOnClickListener(v -> finish());
        header.addView(back);
        TextView title = Ui.text(this, isNew ? "Nueva rutina" : "Editar rutina", 17, Ui.INK, Ui.W_EXTRA);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (!isNew) {
            LinearLayout del = Ui.iconButton(this, R.drawable.ic_trash_2, "Eliminar rutina");
            del.setOnClickListener(v -> confirmDelete());
            header.addView(del);
        } else {
            header.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 40), 1));
        }
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout c = Ui.col(this);
        c.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 20));
        scroll.addView(c);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        c.addView(nameCard());
        c.addView(speedCard(), gap());
        c.addView(burstCard(), gap());
        c.addView(naturalCard(), gap());
        c.addView(zoneCard(), gap());
        c.addView(stopCard(), gap());
        c.addView(pointsCard(), gap());

        LinearLayout save = Ui.button(this, "Guardar rutina", Ui.P, Color.WHITE, 0);
        save.setElevation(Ui.dp(this, 6));
        save.setOnClickListener(v -> save());
        root.addView(save, Ui.margins((LinearLayout.LayoutParams) save.getLayoutParams(), this, 20, 8, 20, 14));

        setContentView(root);
        Ui.edgeToEdge(this, root);

        refreshSpeed();
        refreshStop();
        refreshIcons();
    }

    private LinearLayout.LayoutParams gap() {
        return Ui.margins(Ui.lpMatchWrap(), this, 0, 12, 0, 0);
    }

    private LinearLayout padCard() {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        return card;
    }

    // ------------------------------------------------------------------ nombre e icono

    private View nameCard() {
        LinearLayout card = padCard();
        card.addView(Ui.text(this, "NOMBRE", 12, Ui.MUTED, Ui.W_BOLD));
        nameIn = new EditText(this);
        nameIn.setText(r.name);
        nameIn.setHint("Ej: Toque rápido");
        nameIn.setSingleLine(true);
        nameIn.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameIn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        nameIn.setTypeface(Ui.font(this, Ui.W_BOLD));
        nameIn.setTextColor(Ui.INK);
        nameIn.setHintTextColor(0xFFB0ACC4);
        nameIn.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        nameIn.setBackground(Ui.roundStroke(Ui.BG, Ui.dp(this, 14), Ui.dp(this, 1), Ui.LINE));
        card.addView(nameIn, Ui.margins(Ui.lpMatchWrap(), this, 0, 8, 0, 0));

        LinearLayout icons = Ui.row(this);
        int[] res = {R.drawable.ic_zap, R.drawable.ic_crosshair, R.drawable.ic_timer, R.drawable.ic_hand};
        for (int i = 0; i < 4; i++) {
            Routine probe = new Routine();
            probe.icon = i;
            LinearLayout box = Ui.iconBox(this, res[i], probe.iconColor(), probe.iconBg(), 46, 20, 15);
            final int idx = i;
            box.setClickable(true);
            box.setContentDescription("Icono " + (i + 1));
            box.setOnClickListener(v -> {
                r.icon = idx;
                refreshIcons();
            });
            iconBoxes[i] = box;
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) box.getLayoutParams();
            lp.rightMargin = Ui.dp(this, 10);
            icons.addView(box, lp);
        }
        card.addView(icons, Ui.margins(Ui.lpMatchWrap(), this, 0, 12, 0, 0));
        return card;
    }

    private void refreshIcons() {
        for (int i = 0; i < 4; i++) {
            Routine probe = new Routine();
            probe.icon = i;
            boolean sel = r.icon == i;
            iconBoxes[i].setBackground(sel
                    ? Ui.roundStroke(probe.iconBg(), Ui.dp(this, 15), Ui.dp(this, 2), probe.iconColor())
                    : Ui.round(probe.iconBg(), Ui.dp(this, 15)));
            iconBoxes[i].setAlpha(sel ? 1f : 0.55f);
        }
    }

    // ------------------------------------------------------------------ velocidad

    private View speedCard() {
        LinearLayout card = padCard();
        TextView label = Ui.text(this, "VELOCIDAD", 12, Ui.MUTED, Ui.W_BOLD);
        label.setGravity(Gravity.CENTER);
        card.addView(label, Ui.lpMatchWrap());

        speedSeg = segmented(card, new String[]{"Rápido", "Lento"}, i -> {
            if ((i == 0) == fastMode) return;
            fastMode = i == 0;
            r.intervalMs = fastMode ? 83 : 30_000;
            refreshSpeed();
        });

        LinearLayout big = Ui.row(this);
        big.setGravity(Gravity.CENTER);
        bigNumber = Ui.text(this, "", 72, Ui.P, Ui.W_EXTRA);
        big.addView(bigNumber);
        bigUnit = Ui.text(this, "", 15, Ui.MUTED, Ui.W_EXTRA);
        bigUnit.setPadding(Ui.dp(this, 8), 0, 0, 0);
        bigUnit.setLineSpacing(0, 1.1f);
        big.addView(bigUnit);
        card.addView(big, Ui.margins(Ui.lpMatchWrap(), this, 0, 14, 0, 0));

        // Modo rápido: deslizador 1..20 toques por segundo
        fastBox = Ui.col(this);
        seek = new SeekBar(this);
        seek.setMax(19);
        seek.setProgressTintList(ColorStateList.valueOf(Ui.P));
        seek.setThumbTintList(ColorStateList.valueOf(Ui.P));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(0xFFD9D6E6));
        seek.setContentDescription("Toques por segundo");
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (!fromUser) return;
                r.intervalMs = Math.round(1000f / (p + 1));
                refreshSpeed();
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        fastBox.addView(seek, Ui.margins(Ui.lpMatchWrap(), this, 0, 12, 0, 0));
        LinearLayout ends = Ui.row(this);
        ends.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        ends.addView(Ui.text(this, "1/s", 11, Ui.MUTED, Ui.W_BOLD));
        ends.addView(Ui.weightSpace(this));
        ends.addView(Ui.text(this, "20/s", 11, Ui.MUTED, Ui.W_BOLD));
        fastBox.addView(ends, Ui.margins(Ui.lpMatchWrap(), this, 0, 4, 0, 0));
        fastPresets = presetRow(FAST_PRESETS, "", p -> {
            r.intervalMs = Math.round(1000f / p);
            refreshSpeed();
        });
        fastBox.addView(fastPresets, Ui.margins(Ui.lpMatchWrap(), this, 0, 14, 0, 0));
        card.addView(fastBox, Ui.lpMatchWrap());

        // Modo lento: segundos entre toques
        slowBox = Ui.col(this);
        LinearLayout stepper = Ui.row(this);
        stepper.setGravity(Gravity.CENTER);
        LinearLayout minus = smallButton(R.drawable.ic_minus, "Menos tiempo");
        minus.setOnClickListener(v -> stepSlow(-1));
        stepper.addView(minus);
        slowValue = Ui.text(this, "", 15, Ui.INK, Ui.W_EXTRA);
        slowValue.setGravity(Gravity.CENTER);
        stepper.addView(slowValue, new LinearLayout.LayoutParams(Ui.dp(this, 120), LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout plus = smallButton(R.drawable.ic_plus, "Más tiempo");
        plus.setOnClickListener(v -> stepSlow(1));
        stepper.addView(plus);
        slowBox.addView(stepper, Ui.margins(Ui.lpMatchWrap(), this, 0, 12, 0, 0));
        slowPresets = presetRow(SLOW_PRESETS, " s", p -> {
            r.intervalMs = p * 1000;
            refreshSpeed();
        });
        slowBox.addView(slowPresets, Ui.margins(Ui.lpMatchWrap(), this, 0, 14, 0, 0));
        card.addView(slowBox, Ui.lpMatchWrap());
        return card;
    }

    private void stepSlow(int dir) {
        r.intervalMs = stepSeconds(Math.max(1, r.intervalMs / 1000), dir) * 1000;
        refreshSpeed();
    }

    /** Siguiente valor en segundos para los botones − y +: pasos más grandes cuanto mayor es el valor. */
    private static int stepSeconds(int s, int dir) {
        int step = s < 10 ? 1 : (s < 60 ? 5 : (s < 600 ? 30 : 60));
        if (dir < 0 && s <= 10 && s > 1) step = 1;
        return Routine.clamp(s + dir * step, 1, 3600);
    }

    private void refreshSpeed() {
        selectSeg(speedSeg, fastMode ? 0 : 1);
        fastBox.setVisibility(fastMode ? View.VISIBLE : View.GONE);
        slowBox.setVisibility(fastMode ? View.GONE : View.VISIBLE);
        if (fastMode) {
            int ps = r.perSecond();
            bigNumber.setText(String.valueOf(ps));
            bigUnit.setText("toques\npor segundo");
            seek.setProgress(Routine.clamp(ps - 1, 0, 19));
            selectPreset(fastPresets, FAST_PRESETS, ps);
        } else {
            int s = Math.max(1, r.intervalMs / 1000);
            bigNumber.setText(s >= 60 && s % 60 == 0 ? String.valueOf(s / 60) : String.valueOf(s));
            bigUnit.setText(s >= 60 && s % 60 == 0 ? "minutos\nentre toques" : "segundos\nentre toques");
            slowValue.setText(Routine.seconds(r.intervalMs));
            selectPreset(slowPresets, SLOW_PRESETS, s);
        }
        refreshBurst();
    }

    // ------------------------------------------------------------------ ráfagas

    private View burstCard() {
        LinearLayout card = padCard();
        Views.ToggleView toggle = new Views.ToggleView(this);
        toggle.setOn(r.burst, false);
        toggle.setListener(on -> {
            r.burst = on;
            refreshBurst();
        });
        card.addView(toggleHeader(R.drawable.ic_timer, Ui.MINT, Ui.MINT_S, "Ráfagas", "Toca en tandas y descansa", toggle));

        burstBox = Ui.col(this);
        burstTapsValue = stepperRow(burstBox, "Toques por ráfaga", this::stepBurstTaps);
        burstEveryValue = stepperRow(burstBox, "Repetir cada", this::stepBurstEvery);
        burstHint = Ui.text(this, "", 12, Ui.MUTED, Ui.W_MEDIUM);
        burstHint.setLineSpacing(0, 1.3f);
        burstHint.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        burstHint.setBackground(Ui.round(Ui.BG, Ui.dp(this, 14)));
        burstBox.addView(burstHint, Ui.margins(Ui.lpMatchWrap(), this, 0, 14, 0, 0));
        card.addView(burstBox, Ui.lpMatchWrap());
        return card;
    }

    private void stepBurstTaps(int dir) {
        int v = r.burstTaps;
        int step = v < 20 ? 1 : (v < 100 ? 5 : 50);
        if (dir < 0 && v <= 20) step = 1;
        else if (dir < 0 && v <= 100) step = 5;
        r.burstTaps = Routine.clamp(v + dir * step, 1, Routine.MAX_BURST_TAPS);
        refreshBurst();
    }

    private void stepBurstEvery(int dir) {
        r.burstEveryMs = stepSeconds(Math.max(1, r.burstEveryMs / 1000), dir) * 1000;
        refreshBurst();
    }

    private void refreshBurst() {
        if (burstBox == null) return;
        burstBox.setVisibility(r.burst ? View.VISIBLE : View.GONE);
        burstTapsValue.setText(Ui.number(r.burstTaps));
        burstEveryValue.setText(Routine.seconds(r.burstEveryMs));
        long busy = (long) r.burstTaps * r.intervalMs;
        String speed = r.isFast() ? "a " + r.speedShort() : "(uno " + r.speedShort() + ")";
        String hint;
        if (busy < r.burstEveryMs) {
            hint = r.burstTaps + " toques " + speed + " duran ≈" + Routine.seconds((int) busy)
                    + " y luego descansa ≈" + Routine.seconds((int) (r.burstEveryMs - busy)) + ".";
        } else {
            hint = "La ráfaga dura ≈" + Routine.seconds((int) Math.min(busy, Integer.MAX_VALUE))
                    + ", más que el ciclo: no habrá pausa. Baja los toques o sube el tiempo.";
        }
        if (r.natural) hint += "\nCon modo natural, la cantidad y la pausa cambian un poco en cada ráfaga.";
        burstHint.setText(hint);
    }

    // ------------------------------------------------------------------ modo natural

    private View naturalCard() {
        LinearLayout card = padCard();
        natural = new Views.ToggleView(this);
        natural.setOn(r.natural, false);
        natural.setListener(on -> {
            r.natural = on;
            bars.setActive(on);
            refreshBurst();
        });
        card.addView(toggleHeader(R.drawable.ic_sparkles, Ui.P, Ui.PS, "Modo natural", "Toca como una persona", natural));

        LinearLayout viz = Ui.row(this);
        viz.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        viz.setBackground(Ui.round(Ui.BG, Ui.dp(this, 14)));
        bars = new Views.BarsView(this);
        bars.setActive(r.natural);
        viz.addView(bars);
        TextView vt = Ui.text(this, "El ritmo varía un poco,\nel promedio se mantiene.", 11.5f, Ui.MUTED, Ui.W_BOLD);
        vt.setPadding(Ui.dp(this, 12), 0, 0, 0);
        vt.setLineSpacing(0, 1.25f);
        viz.addView(vt);
        card.addView(viz, Ui.margins(Ui.lpMatchWrap(), this, 0, 14, 0, 0));

        LinearLayout mini = Ui.row(this);
        mini.addView(miniItem(R.drawable.ic_gauge, "Ritmo"), miniLp(0));
        mini.addView(miniItem(R.drawable.ic_crosshair, "Posición"), miniLp(8));
        mini.addView(miniItem(R.drawable.ic_pointer, "Presión"), miniLp(8));
        card.addView(mini, Ui.margins(Ui.lpMatchWrap(), this, 0, 12, 0, 0));
        return card;
    }

    private LinearLayout.LayoutParams miniLp(int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = Ui.dp(this, leftDp);
        return lp;
    }

    private View miniItem(int icon, String label) {
        LinearLayout m = Ui.col(this);
        m.setGravity(Gravity.CENTER_HORIZONTAL);
        m.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
        m.setBackground(Ui.round(Ui.BG, Ui.dp(this, 12)));
        m.addView(Ui.icon(this, icon, Ui.P, 18));
        m.addView(Ui.text(this, label, 11, Ui.MUTED, Ui.W_BOLD), Ui.margins(Ui.lpWrap(), this, 0, 4, 0, 0));
        return m;
    }

    // ------------------------------------------------------------------ zona

    private View zoneCard() {
        LinearLayout card = padCard();
        Views.ToggleView toggle = new Views.ToggleView(this);
        toggle.setOn(r.zone, false);
        toggle.setListener(on -> r.zone = on);
        card.addView(toggleHeader(R.drawable.ic_circle_dashed, Ui.AMBER, Ui.AMBER_S,
                "Zona aleatoria", "Cada toque cae en un lugar distinto", toggle));
        TextView body = Ui.text(this,
                "Cada punto se vuelve un círculo y los toques caen al azar dentro de él. En el panel flotante "
                        + "arrástralo para moverlo y usa el tirador del borde para cambiar su tamaño.",
                12.5f, Ui.MUTED, Ui.W_MEDIUM);
        body.setLineSpacing(0, 1.3f);
        card.addView(body, Ui.margins(Ui.lpMatchWrap(), this, 0, 12, 0, 0));
        return card;
    }

    // ------------------------------------------------------------------ detener

    private View stopCard() {
        LinearLayout card = padCard();
        LinearLayout head = Ui.row(this);
        head.addView(Ui.icon(this, R.drawable.ic_repeat, Ui.P, 20));
        TextView t = Ui.text(this, "Detener automáticamente", 15, Ui.INK, Ui.W_EXTRA);
        t.setPadding(Ui.dp(this, 10), 0, 0, 0);
        head.addView(t);
        card.addView(head);

        stopSeg = segmented(card, new String[]{"Nunca", "Toques", "Tiempo"}, i -> {
            r.stopMode = i;
            refreshStop();
        });

        stopRow = Ui.row(this);
        stopRow.addView(Ui.text(this, "Después de", 14, Ui.MUTED, Ui.W_BOLD));
        stopRow.addView(Ui.weightSpace(this));
        LinearLayout minus = smallButton(R.drawable.ic_minus, "Menos");
        minus.setOnClickListener(v -> stepStop(-1));
        stopRow.addView(minus);
        stopValue = Ui.text(this, "", 14, Ui.INK, Ui.W_EXTRA);
        stopValue.setGravity(Gravity.CENTER);
        stopRow.addView(stopValue, new LinearLayout.LayoutParams(Ui.dp(this, 100), LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout plus = smallButton(R.drawable.ic_plus, "Más");
        plus.setOnClickListener(v -> stepStop(1));
        stopRow.addView(plus);
        card.addView(stopRow, Ui.margins(Ui.lpMatchWrap(), this, 0, 14, 0, 0));
        return card;
    }

    private void stepStop(int dir) {
        if (r.stopMode == Routine.STOP_TAPS) {
            int v = r.stopTaps;
            int step = v < 100 ? 10 : (v < 1000 ? 50 : (v < 10_000 ? 500 : 5000));
            if (dir < 0 && v <= 100) step = 10;
            r.stopTaps = Routine.clamp(v + dir * step, 10, 10_000_000);
        } else if (r.stopMode == Routine.STOP_TIME) {
            int v = r.stopMinutes;
            int step = v < 10 ? 1 : (v < 60 ? 5 : 15);
            if (dir < 0 && v <= 10) step = 1;
            r.stopMinutes = Routine.clamp(v + dir * step, 1, 24 * 60);
        }
        refreshStop();
    }

    private void refreshStop() {
        selectSeg(stopSeg, r.stopMode);
        stopRow.setVisibility(r.stopMode == Routine.STOP_NEVER ? View.GONE : View.VISIBLE);
        if (r.stopMode == Routine.STOP_TAPS) stopValue.setText(Ui.number(r.stopTaps) + " toques");
        else if (r.stopMode == Routine.STOP_TIME) stopValue.setText(Routine.minutes(r.stopMinutes));
    }

    // ------------------------------------------------------------------ puntos

    private View pointsCard() {
        LinearLayout card = padCard();
        LinearLayout head = Ui.row(this);
        head.addView(Ui.icon(this, R.drawable.ic_crosshair, Ui.P, 20));
        int n = r.points.size();
        TextView t = Ui.text(this, n == 0 ? "Sin puntos todavía" : (n == 1 ? "1 punto guardado" : n + " puntos guardados"),
                15, Ui.INK, Ui.W_EXTRA);
        t.setPadding(Ui.dp(this, 10), 0, 0, 0);
        head.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (n > 0) {
            TextView clear = Ui.text(this, "Borrar", 13, Ui.CORAL, Ui.W_EXTRA);
            clear.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
            clear.setClickable(true);
            clear.setOnClickListener(v -> {
                r.points.clear();
                t.setText("Sin puntos todavía");
                clear.setVisibility(View.GONE);
            });
            head.addView(clear);
        }
        card.addView(head);
        TextView body = Ui.text(this,
                "Los puntos se colocan desde el panel flotante con + y se guardan solos en la rutina que está en uso.",
                12.5f, Ui.MUTED, Ui.W_MEDIUM);
        body.setLineSpacing(0, 1.3f);
        card.addView(body, Ui.margins(Ui.lpMatchWrap(), this, 0, 8, 0, 0));
        return card;
    }

    // ------------------------------------------------------------------ guardar / borrar

    private void save() {
        String name = nameIn.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Ponle un nombre a la rutina", Toast.LENGTH_SHORT).show();
            nameIn.requestFocus();
            return;
        }
        r.name = name.length() > 40 ? name.substring(0, 40) : name;
        // Si la rutina está en uso en el panel, conserva los puntos que se hayan movido mientras se editaba
        Routine latest = Store.byId(this, r.id);
        if (latest != null && !r.points.isEmpty()) {
            r.points.clear();
            r.points.addAll(latest.points);
        }
        Store.upsert(this, r);
        if (isNew) Store.setActive(this, r.id);
        TocamatikService s = TocamatikService.instance;
        if (s != null) s.reloadRoutine();
        Toast.makeText(this, "Rutina guardada", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("¿Eliminar \"" + r.name + "\"?")
                .setMessage("Se borrarán su configuración y sus puntos.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    Store.delete(this, r.id);
                    TocamatikService s = TocamatikService.instance;
                    if (s != null) s.reloadRoutine();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ------------------------------------------------------------------ piezas

    private interface OnPick { void picked(int value); }

    /** Cabecera de tarjeta: icono de color, título, subtítulo y un interruptor a la derecha. */
    private LinearLayout toggleHeader(int icon, int fg, int bg, String title, String subtitle, Views.ToggleView toggle) {
        LinearLayout head = Ui.row(this);
        head.addView(Ui.iconBox(this, icon, fg, bg, 42, 20, 14));
        LinearLayout t = Ui.col(this);
        t.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 8), 0);
        t.addView(Ui.text(this, title, 15, Ui.INK, Ui.W_EXTRA));
        TextView sub = Ui.text(this, subtitle, 12.5f, Ui.MUTED, Ui.W_BOLD);
        sub.setPadding(0, Ui.dp(this, 3), 0, 0);
        t.addView(sub);
        head.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(toggle);
        return head;
    }

    /** Fila "Etiqueta   −  valor  +" agregada a {@code parent}; devuelve el texto del valor. */
    private TextView stepperRow(LinearLayout parent, String label, OnPick onStep) {
        LinearLayout row = Ui.row(this);
        row.addView(Ui.text(this, label, 14, Ui.MUTED, Ui.W_BOLD));
        row.addView(Ui.weightSpace(this));
        LinearLayout minus = smallButton(R.drawable.ic_minus, "Menos");
        minus.setOnClickListener(v -> onStep.picked(-1));
        row.addView(minus);
        TextView value = Ui.text(this, "", 14, Ui.INK, Ui.W_EXTRA);
        value.setGravity(Gravity.CENTER);
        row.addView(value, new LinearLayout.LayoutParams(Ui.dp(this, 90), LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout plus = smallButton(R.drawable.ic_plus, "Más");
        plus.setOnClickListener(v -> onStep.picked(1));
        row.addView(plus);
        parent.addView(row, Ui.margins(Ui.lpMatchWrap(), this, 0, 14, 0, 0));
        return value;
    }

    private TextView[] segmented(LinearLayout parent, String[] labels, OnPick onPick) {
        LinearLayout seg = Ui.row(this);
        seg.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        seg.setBackground(Ui.round(Ui.BG, Ui.dp(this, 14)));
        TextView[] items = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            TextView t = Ui.text(this, labels[i], 12.5f, Ui.MUTED, Ui.W_BOLD);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
            final int idx = i;
            t.setClickable(true);
            t.setOnClickListener(v -> onPick.picked(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = Ui.dp(this, 4);
            seg.addView(t, lp);
            items[i] = t;
        }
        parent.addView(seg, Ui.margins(Ui.lpMatchWrap(), this, 0, 10, 0, 0));
        return items;
    }

    private void selectSeg(TextView[] items, int sel) {
        for (int i = 0; i < items.length; i++) {
            boolean on = i == sel;
            items[i].setTextColor(on ? Ui.INK : Ui.MUTED);
            items[i].setBackground(on ? Ui.round(Color.WHITE, Ui.dp(this, 11)) : null);
            items[i].setElevation(on ? Ui.dp(this, 1) : 0);
        }
    }

    private LinearLayout presetRow(int[] values, String suffix, OnPick onPick) {
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER);
        for (int i = 0; i < values.length; i++) {
            TextView chip = Ui.chip(this, values[i] + suffix, Ui.MUTED, Ui.BG);
            chip.setMinWidth(Ui.dp(this, 40));
            chip.setGravity(Gravity.CENTER);
            final int v = values[i];
            chip.setClickable(true);
            chip.setOnClickListener(x -> onPick.picked(v));
            LinearLayout.LayoutParams lp = Ui.lpWrap();
            if (i > 0) lp.leftMargin = Ui.dp(this, 8);
            row.addView(chip, lp);
        }
        return row;
    }

    private void selectPreset(LinearLayout row, int[] values, int current) {
        for (int i = 0; i < values.length; i++) {
            TextView chip = (TextView) row.getChildAt(i);
            boolean on = values[i] == current;
            chip.setTextColor(on ? Color.WHITE : Ui.MUTED);
            chip.setBackground(on ? Ui.round(Ui.P, Ui.dp(this, 999))
                    : Ui.roundStroke(Ui.BG, Ui.dp(this, 999), Ui.dp(this, 1), Ui.LINE));
        }
    }

    private LinearLayout smallButton(int icon, String desc) {
        LinearLayout b = Ui.iconButton(this, icon, desc);
        b.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)));
        return b;
    }
}
