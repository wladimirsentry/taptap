package app.tocamatik;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Servicio de accesibilidad de Tocamatik.
 * Dibuja la burbuja, el panel flotante y los puntos sobre cualquier app,
 * y hace los toques con dispatchGesture. No lee el contenido de la pantalla.
 */
public class TocamatikService extends AccessibilityService {

    static TocamatikService instance;

    /** Velocidades del botón rápido del panel, en ms entre toques (20/s … 1/s). */
    private static final int[] RATE_PRESETS = {50, 83, 125, 200, 500, 1000};

    private WindowManager wm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();

    private Routine routine;
    private boolean shown, expanded, running;
    private boolean onRight = true;

    // Burbuja
    private Views.BubbleView bubble;
    private WindowManager.LayoutParams bubbleLp;
    private boolean bubbleAttached;
    private ValueAnimator snapAnim;

    // Panel
    private LinearLayout panel;
    private WindowManager.LayoutParams panelLp;
    private boolean panelAttached;
    private LinearLayout playBtn;
    private TextView countTv, rateTv;
    private LinearLayout naturalBtn;
    private ImageView naturalIcon, collapseIcon;

    // Píldora superior mientras toca
    private LinearLayout pill;
    private TextView pillText;
    private WindowManager.LayoutParams pillLp;
    private boolean pillAttached;

    // Zona para cerrar (abajo) mientras se arrastra la burbuja
    private FrameLayout closeZone;
    private LinearLayout closeCircle;
    private WindowManager.LayoutParams closeLp;
    private boolean closeAttached, closeVisible, closeHot;

    // Puntos
    private final List<Target> targets = new ArrayList<>();

    private static final class Target {
        Views.TargetView view;
        WindowManager.LayoutParams lp;
        boolean attached;
    }

    // Motor
    private int index;
    private long sessionTaps, unflushedTaps, startedAt;
    private double drift;

    private AccessibilityButtonController.AccessibilityButtonCallback a11yButton;

    // ================================================================ ciclo de vida

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        try {
            a11yButton = new AccessibilityButtonController.AccessibilityButtonCallback() {
                @Override
                public void onClicked(AccessibilityButtonController controller) {
                    toggleOverlay();
                }
            };
            getAccessibilityButtonController().registerAccessibilityButtonCallback(a11yButton);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Tocamatik no necesita leer eventos ni contenido de otras apps.
    }

    @Override
    public void onInterrupt() {
        // Tocamatik no da retroalimentación hablada ni háptica: no hay nada que interrumpir.
    }

    @Override
    public boolean onUnbind(Intent intent) {
        cleanup();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    private void cleanup() {
        hideOverlay();
        try {
            if (a11yButton != null) {
                getAccessibilityButtonController().unregisterAccessibilityButtonCallback(a11yButton);
            }
        } catch (Exception ignored) {
        }
        a11yButton = null;
        if (instance == this) instance = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!shown) return;
        // Al girar la pantalla se recolocan los puntos según su posición relativa guardada.
        handler.postDelayed(this::relayout, 300);
    }

    // ================================================================ API para las pantallas

    boolean isOverlayShown() {
        return shown;
    }

    void toggleOverlay() {
        if (shown) hideOverlay();
        else showOverlay(true);
    }

    /** Muestra la burbuja o el panel con la rutina activa. */
    void showOverlay(boolean expand) {
        if (wm == null || !Store.consent(this)) return;
        if (shown) {
            reloadRoutine();
            if (expand && !expanded) expand();
            return;
        }
        routine = Store.active(this);
        shown = true;
        restoreTargets();
        if (expand) {
            ensureBubble();
            bubbleLp.y = screenH() / 3;
            showPanel();
        } else {
            showBubble();
        }
    }

    void hideOverlay() {
        stop(null);
        if (shown) savePoints();
        detach(bubble, bubbleAttached);
        bubbleAttached = false;
        detach(panel, panelAttached);
        panelAttached = false;
        removeCloseZone();
        hidePill();
        for (Target t : targets) detachTarget(t);
        targets.clear();
        shown = false;
        expanded = false;
    }

    /** Se llama cuando cambia la rutina activa o se edita desde la app. */
    void reloadRoutine() {
        if (!shown) return;
        stop(null);
        for (Target t : targets) detachTarget(t);
        targets.clear();
        routine = Store.active(this);
        restoreTargets();
        refreshPanel();
    }

    // ================================================================ burbuja

    private void ensureBubble() {
        if (bubble != null) return;
        bubble = new Views.BubbleView(this);
        int size = dp(58);
        bubbleLp = overlayLp(size, size);
        bubbleLp.x = screenW() - size - dp(6);
        bubbleLp.y = screenH() / 3;
        bubble.setOnTouchListener(new BubbleTouch());
        bubble.setOnClickListener(v -> onBubbleTap());
    }

    private void showBubble() {
        ensureBubble();
        bubble.setRunning(running);
        bubble.setAlpha(running ? 1f : 0.82f);
        int size = bubbleLp.width;
        bubbleLp.x = onRight ? screenW() - size - dp(6) : dp(6);
        bubbleLp.y = clamp(bubbleLp.y, dp(48), screenH() - size - dp(48));
        ensureCloseZone();
        if (!bubbleAttached) bubbleAttached = attach(bubble, bubbleLp);
        else update(bubble, bubbleLp);
    }

    private void onBubbleTap() {
        if (running) stop(null);
        expand();
    }

    private void expand() {
        detach(bubble, bubbleAttached);
        bubbleAttached = false;
        showPanel();
    }

    private void collapse() {
        if (panelAttached && panelLp != null) {
            ensureBubble();
            bubbleLp.y = panelLp.y + dp(8);
        }
        detach(panel, panelAttached);
        panelAttached = false;
        expanded = false;
        showBubble();
    }

    /** Arrastre de la burbuja: suelta en un borde, o sobre la ✕ para ocultar. */
    private final class BubbleTouch implements View.OnTouchListener {
        private float downX, downY;
        private int startX, startY;
        private boolean dragging;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (snapAnim != null) snapAnim.cancel();
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startX = bubbleLp.x;
                    startY = bubbleLp.y;
                    dragging = false;
                    bubble.setAlpha(1f);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                    if (!dragging && Math.hypot(dx, dy) > dp(6)) {
                        dragging = true;
                        if (!running) showCloseZone();
                    }
                    if (dragging) {
                        bubbleLp.x = startX + (int) dx;
                        bubbleLp.y = startY + (int) dy;
                        update(bubble, bubbleLp);
                        if (closeVisible) setCloseHot(nearClose());
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (dragging) {
                        boolean close = closeVisible && nearClose();
                        hideCloseZone();
                        if (close) {
                            hideOverlay();
                            toast("Tocamatik oculto. Ábrelo de nuevo desde la app.");
                        } else {
                            snapBubble();
                        }
                    } else {
                        v.performClick();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    hideCloseZone();
                    if (dragging) snapBubble();
                    return true;
                default:
                    return false;
            }
        }
    }

    private void snapBubble() {
        if (!bubbleAttached) return;
        int size = bubbleLp.width;
        int w = screenW(), hgt = screenH();
        onRight = bubbleLp.x + size / 2 > w / 2;
        final int fromX = bubbleLp.x, toX = onRight ? w - size - dp(6) : dp(6);
        final int fromY = bubbleLp.y, toY = clamp(bubbleLp.y, dp(48), hgt - size - dp(48));
        if (snapAnim != null) snapAnim.cancel();
        snapAnim = ValueAnimator.ofFloat(0f, 1f);
        snapAnim.setDuration(220);
        snapAnim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            bubbleLp.x = (int) (fromX + (toX - fromX) * f);
            bubbleLp.y = (int) (fromY + (toY - fromY) * f);
            if (bubbleAttached) update(bubble, bubbleLp);
        });
        snapAnim.start();
        bubble.setAlpha(running ? 1f : 0.82f);
    }

    // ================================================================ zona de cierre

    /** La zona de cierre se agrega antes que la burbuja (queda debajo) y solo se muestra al arrastrar. */
    private void ensureCloseZone() {
        if (closeZone == null) {
            closeZone = new FrameLayout(this);
            closeZone.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x00000000, 0xA6000000}));
            closeCircle = new LinearLayout(this);
            closeCircle.setGravity(Gravity.CENTER);
            closeCircle.addView(Ui.icon(this, R.drawable.ic_x, Color.WHITE, 26));
            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(dp(70), dp(70),
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            clp.bottomMargin = dp(70);
            closeZone.addView(closeCircle, clp);
            TextView hint = Ui.text(this, "Suelta para ocultar", 12.5f, Color.WHITE, Ui.W_BOLD);
            FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            hlp.bottomMargin = dp(36);
            closeZone.addView(hint, hlp);
            closeLp = overlayLp(WindowManager.LayoutParams.MATCH_PARENT, dp(230));
            closeLp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            setCloseHot(false);
        }
        if (!closeAttached) {
            closeLp.y = screenH() - dp(230);
            closeZone.setVisibility(View.INVISIBLE);
            closeAttached = attach(closeZone, closeLp);
        }
    }

    private void showCloseZone() {
        if (!closeAttached) return;
        closeLp.y = screenH() - dp(230);
        update(closeZone, closeLp);
        setCloseHot(false);
        closeZone.setAlpha(0f);
        closeZone.setVisibility(View.VISIBLE);
        closeZone.animate().alpha(1f).setDuration(150).start();
        closeVisible = true;
    }

    private void hideCloseZone() {
        if (!closeAttached || !closeVisible) return;
        closeZone.animate().cancel();
        closeZone.setVisibility(View.INVISIBLE);
        closeVisible = false;
    }

    private void removeCloseZone() {
        if (closeAttached) detach(closeZone, true);
        closeAttached = false;
        closeVisible = false;
    }

    private void setCloseHot(boolean hot) {
        if (closeCircle == null) return;
        if (hot == closeHot && closeCircle.getBackground() != null) return;
        closeHot = hot;
        GradientDrawable d = Ui.oval(hot ? 0x66FF5A63 : 0x1CFFFFFF);
        d.setStroke(dp(2), hot ? Ui.CORAL : 0x55FFFFFF);
        closeCircle.setBackground(d);
        closeCircle.animate().scaleX(hot ? 1.15f : 1f).scaleY(hot ? 1.15f : 1f).setDuration(120).start();
    }

    private boolean nearClose() {
        int size = bubbleLp.width;
        float bx = bubbleLp.x + size / 2f, by = bubbleLp.y + size / 2f;
        float cx = screenW() / 2f, cy = screenH() - dp(70) - dp(35);
        return Math.hypot(bx - cx, by - cy) < dp(90);
    }

    // ================================================================ panel

    private void buildPanel() {
        panel = Ui.col(this);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(0, dp(8), 0, dp(10));
        panel.setBackground(Ui.roundStroke(Ui.PANEL, dp(24), dp(1), 0x1CFFFFFF));
        panel.setElevation(dp(10));

        // Logo: tocar = encoger, arrastrar = mover el panel
        Views.LogoView logo = new Views.LogoView(this, true);
        logo.setContentDescription("Mover o encoger el panel");
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(dp(30), dp(30));
        llp.bottomMargin = dp(8);
        panel.addView(logo, llp);
        logo.setOnTouchListener(new PanelDrag());
        logo.setOnClickListener(v -> collapse());

        playBtn = panelButton(R.drawable.ic_play_fill, 52, 18, Ui.MINT, Color.WHITE, "Iniciar");
        playBtn.setOnClickListener(v -> {
            if (running) stop(null);
            else start();
        });
        panel.addView(playBtn);

        countTv = Ui.text(this, "0", 14, Color.WHITE, Ui.W_EXTRA);
        LinearLayout.LayoutParams clp = Ui.lpWrap();
        clp.topMargin = dp(8);
        panel.addView(countTv, clp);
        TextView countLabel = Ui.text(this, "toques", 9.5f, 0xFFA9A5C4, Ui.W_BOLD);
        LinearLayout.LayoutParams cl2 = Ui.lpWrap();
        cl2.topMargin = dp(2);
        cl2.bottomMargin = dp(6);
        panel.addView(countLabel, cl2);

        rateTv = Ui.text(this, "", 12, 0xFFB9AEFF, Ui.W_EXTRA);
        rateTv.setGravity(Gravity.CENTER);
        rateTv.setPadding(0, dp(6), 0, dp(6));
        rateTv.setBackground(Ui.pressable(Ui.round(0x335B47F5, dp(10)), 0x33FFFFFF));
        rateTv.setContentDescription("Cambiar velocidad");
        rateTv.setOnClickListener(v -> cycleRate());
        panel.addView(rateTv, new LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT));

        panel.addView(divider());

        LinearLayout plus = panelButton(R.drawable.ic_plus, 48, 16, 0x14FFFFFF, Color.WHITE, "Agregar punto");
        plus.setOnClickListener(v -> addTarget());
        panel.addView(plus);

        LinearLayout minus = panelButton(R.drawable.ic_minus, 48, 16, 0x14FFFFFF, Color.WHITE, "Quitar último punto");
        minus.setOnClickListener(v -> removeTarget());
        panel.addView(minus);

        naturalBtn = panelButton(R.drawable.ic_sparkles, 48, 16, 0x14FFFFFF, Color.WHITE, "Modo natural");
        naturalIcon = (ImageView) naturalBtn.getChildAt(0);
        naturalBtn.setOnClickListener(v -> toggleNatural());
        panel.addView(naturalBtn);

        panel.addView(divider());

        LinearLayout collapseBtn = panelButton(R.drawable.ic_chevrons_right, 38, 13, 0x14FFFFFF, 0xFFC9C5E0, "Encoger panel");
        collapseIcon = (ImageView) collapseBtn.getChildAt(0);
        collapseBtn.setOnClickListener(v -> collapse());
        panel.addView(collapseBtn);

        panelLp = overlayLp(dp(66), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout panelButton(int icon, int sizeDp, int radiusDp, int bg, int fg, String desc) {
        LinearLayout b = new LinearLayout(this);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.pressable(Ui.round(bg, dp(radiusDp)), 0x33FFFFFF));
        b.addView(Ui.icon(this, icon, fg, sizeDp >= 50 ? 20 : (sizeDp <= 40 ? 18 : 22)));
        b.setContentDescription(desc);
        b.setClickable(true);
        b.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(4);
        b.setLayoutParams(lp);
        return b;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(0x1AFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(1));
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(6);
        v.setLayoutParams(lp);
        return v;
    }

    private void showPanel() {
        if (panel == null) buildPanel();
        refreshPanel();
        panel.measure(View.MeasureSpec.makeMeasureSpec(dp(66), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int ph = panel.getMeasuredHeight();
        int baseY = bubbleLp != null ? bubbleLp.y - dp(8) : screenH() / 3;
        panelLp.x = onRight ? screenW() - dp(66) - dp(10) : dp(10);
        panelLp.y = clamp(baseY, dp(48), Math.max(dp(48), screenH() - ph - dp(48)));
        if (!panelAttached) panelAttached = attach(panel, panelLp);
        else update(panel, panelLp);
        expanded = true;
    }

    private void refreshPanel() {
        if (panel == null || routine == null) return;
        ((ImageView) playBtn.getChildAt(0)).setImageResource(running ? R.drawable.ic_square_fill : R.drawable.ic_play_fill);
        playBtn.setBackground(Ui.pressable(Ui.round(running ? Ui.CORAL : Ui.MINT, dp(18)), 0x33FFFFFF));
        playBtn.setContentDescription(running ? "Detener" : "Iniciar");
        countTv.setText(compact(sessionTaps));
        rateTv.setText(routine.isFast() ? routine.perSecond() + "/s" : Routine.seconds(routine.intervalMs).replace(" ", ""));
        naturalBtn.setBackground(Ui.pressable(Ui.round(routine.natural ? 0x405B47F5 : 0x14FFFFFF, dp(16)), 0x33FFFFFF));
        naturalIcon.setImageTintList(android.content.res.ColorStateList.valueOf(routine.natural ? 0xFFC9BFFF : 0x80FFFFFF));
        naturalBtn.setContentDescription(routine.natural ? "Modo natural activado" : "Modo natural desactivado");
        collapseIcon.setRotation(onRight ? 0f : 180f);
    }

    /** Arrastre del panel desde el logo; al soltar se pega al borde más cercano. */
    private final class PanelDrag implements View.OnTouchListener {
        private float downX, downY;
        private int startX, startY;
        private boolean dragging;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startX = panelLp.x;
                    startY = panelLp.y;
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                    if (!dragging && Math.hypot(dx, dy) > dp(6)) dragging = true;
                    if (dragging) {
                        panelLp.x = startX + (int) dx;
                        panelLp.y = startY + (int) dy;
                        update(panel, panelLp);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (dragging) {
                        onRight = panelLp.x + dp(33) > screenW() / 2;
                        if (bubbleLp != null) bubbleLp.y = panelLp.y + dp(8);
                        showPanel();
                    } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    private void cycleRate() {
        Routine fresh = freshRoutine();
        int next = RATE_PRESETS[0];
        for (int p : RATE_PRESETS) {
            if (p > fresh.intervalMs) { next = p; break; }
        }
        fresh.intervalMs = next;
        Store.upsert(this, fresh);
        routine = fresh;
        refreshPanel();
        if (running) updatePill();
    }

    private void toggleNatural() {
        Routine fresh = freshRoutine();
        fresh.natural = !fresh.natural;
        Store.upsert(this, fresh);
        routine = fresh;
        refreshPanel();
        if (running) updatePill();
        toast(fresh.natural ? "Modo natural activado" : "Modo natural desactivado");
    }

    // ================================================================ píldora superior

    private void showPill() {
        if (pill == null) {
            pill = Ui.row(this);
            pill.setPadding(dp(10), dp(8), dp(14), dp(8));
            pill.setBackground(Ui.roundStroke(Ui.PANEL, dp(999), dp(1), 0x1CFFFFFF));
            View dot = new View(this);
            GradientDrawable d = Ui.oval(0xFF5BF0B5);
            d.setStroke(dp(3), 0x335BF0B5);
            dot.setBackground(d);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(12), dp(12));
            dlp.rightMargin = dp(8);
            pill.addView(dot, dlp);
            pillText = Ui.text(this, "", 12, Color.WHITE, Ui.W_BOLD);
            pill.addView(pillText);
            pillLp = overlayLp(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            pillLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            pillLp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        pillLp.x = 0;
        pillLp.y = statusBarHeight() + dp(6);
        updatePill();
        if (!pillAttached) pillAttached = attach(pill, pillLp);
    }

    private void updatePill() {
        if (pillText == null || routine == null) return;
        String s = "Tocamatik · " + routine.rateShort() + (routine.natural ? " · natural" : "") + " · " + Ui.number(sessionTaps);
        pillText.setText(s);
    }

    private void hidePill() {
        if (!pillAttached) return;
        detach(pill, true);
        pillAttached = false;
    }

    // ================================================================ puntos

    private void restoreTargets() {
        for (float[] p : routine.points) {
            Target t = makeTarget(targets.size() + 1, p[0], p[1]);
            targets.add(t);
            attachTarget(t);
        }
    }

    private Target makeTarget(int number, float fx, float fy) {
        Target t = new Target();
        t.view = new Views.TargetView(this, number);
        int size = dp(96);
        t.lp = overlayLp(size, size);
        t.lp.x = Math.round(fx * screenW()) - size / 2;
        t.lp.y = Math.round(fy * screenH()) - size / 2;
        t.view.setOnTouchListener(new TargetDrag(t));
        return t;
    }

    private void addTarget() {
        if (running) return;
        if (targets.size() >= Routine.MAX_POINTS) {
            toast("Máximo " + Routine.MAX_POINTS + " puntos");
            return;
        }
        int n = targets.size();
        float off = (n % 5) * 0.06f;
        Target t = makeTarget(n + 1, 0.42f + off * 0.5f, 0.40f + off);
        targets.add(t);
        attachTarget(t);
        // El panel queda encima de los puntos
        handler.post(() -> {
            if (panelAttached) {
                detach(panel, true);
                panelAttached = attach(panel, panelLp);
            }
        });
        savePoints();
        if (n == 0) toast("Arrastra el punto a donde quieres tocar");
    }

    private void removeTarget() {
        if (running || targets.isEmpty()) return;
        Target t = targets.remove(targets.size() - 1);
        detachTarget(t);
        savePoints();
    }

    private void attachTarget(Target t) {
        if (!t.attached) t.attached = attach(t.view, t.lp);
    }

    private void detachTarget(Target t) {
        if (t.attached) detach(t.view, true);
        t.attached = false;
    }

    private void setTargetsTouchable(boolean touchable) {
        for (Target t : targets) {
            if (touchable) t.lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            else t.lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            t.view.setDim(!touchable);
            if (t.attached) update(t.view, t.lp);
        }
    }

    /** Guarda la posición de los puntos en la rutina activa (como fracción de la pantalla). */
    private void savePoints() {
        if (routine == null) return;
        Routine fresh = freshRoutine();
        fresh.points.clear();
        int w = screenW(), hgt = screenH();
        for (Target t : targets) {
            float cx = t.lp.x + t.lp.width / 2f, cy = t.lp.y + t.lp.height / 2f;
            fresh.points.add(new float[]{Routine.clampF(cx / w), Routine.clampF(cy / hgt)});
        }
        Store.upsert(this, fresh);
        routine = fresh;
    }

    private final class TargetDrag implements View.OnTouchListener {
        private final Target t;
        private float downX, downY;
        private int startX, startY;

        TargetDrag(Target t) {
            this.t = t;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (running) return false;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startX = t.lp.x;
                    startY = t.lp.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    t.lp.x = startX + (int) (e.getRawX() - downX);
                    t.lp.y = startY + (int) (e.getRawY() - downY);
                    update(t.view, t.lp);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    savePoints();
                    return true;
                default:
                    return false;
            }
        }
    }

    // ================================================================ motor de toques

    private void start() {
        if (targets.isEmpty()) {
            toast("Agrega al menos un punto con +");
            return;
        }
        routine = freshRoutine();
        running = true;
        index = 0;
        sessionTaps = 0;
        unflushedTaps = 0;
        drift = 0;
        startedAt = SystemClock.elapsedRealtime();
        setTargetsTouchable(false);
        collapse();
        showPill();
        // Pausa corta para que los puntos dejen de capturar toques antes del primero
        handler.postDelayed(tick, 250);
    }

    /** Detiene el motor. {@code reason} se muestra como aviso si no es null. */
    void stop(String reason) {
        if (!running) return;
        running = false;
        handler.removeCallbacks(tick);
        Store.addStats(this, unflushedTaps, SystemClock.elapsedRealtime() - startedAt);
        unflushedTaps = 0;
        setTargetsTouchable(true);
        hidePill();
        if (bubble != null) {
            bubble.setRunning(false);
            bubble.setAlpha(0.82f);
        }
        refreshPanel();
        if (reason != null) toast(reason);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            if (targets.isEmpty()) {
                stop(null);
                return;
            }
            if (index >= targets.size()) index = 0;
            if (routine.stopMode == Routine.STOP_TIME
                    && SystemClock.elapsedRealtime() - startedAt >= routine.stopMinutes * 60_000L) {
                stop("Listo: se cumplió el tiempo (" + Routine.minutes(routine.stopMinutes) + ")");
                return;
            }

            Target t = targets.get(index);
            int[] loc = new int[2];
            t.view.getLocationOnScreen(loc);
            float x = loc[0] + t.view.getWidth() / 2f;
            float y = loc[1] + t.view.getHeight() / 2f;

            int base = routine.intervalMs;
            int duration;
            Path path = new Path();
            if (routine.natural) {
                // Posición: el dedo no cae siempre en el mismo píxel (más denso cerca del centro)
                double angle = rnd.nextDouble() * Math.PI * 2;
                double radius = Math.min(Math.abs(rnd.nextGaussian()) * dp(3), dp(7));
                x += (float) (Math.cos(angle) * radius);
                y += (float) (Math.sin(angle) * radius);
                // Presión: duración variable que nunca supera lo que permite el ritmo
                int maxDur = Math.max(5, (int) (base * 0.55f));
                duration = clamp((int) Math.round(38 + rnd.nextGaussian() * 9), 18, Math.min(70, maxDur));
                // Leve rodada del dedo (1-2 px)
                path.moveTo(Math.max(0, x), Math.max(0, y));
                path.lineTo(Math.max(0, x + (float) (rnd.nextGaussian() * 0.8)),
                        Math.max(0, y + (float) (rnd.nextGaussian() * 0.8 + 0.6)));
            } else {
                duration = Math.min(40, Math.max(5, base - 10));
                path.moveTo(Math.max(0, x), Math.max(0, y));
            }

            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, duration))
                    .build();
            if (!dispatchGesture(gesture, null, null)) {
                stop("Android rechazó el toque. Desactiva y vuelve a activar Tocamatik en Accesibilidad.");
                return;
            }
            t.view.pulse();
            sessionTaps++;
            unflushedTaps++;
            if (unflushedTaps >= 60) {
                Store.addStats(TocamatikService.this, unflushedTaps, 0);
                unflushedTaps = 0;
            }
            if (sessionTaps % 4 == 0 || base >= 250) updatePill();

            // ¿Detener automáticamente?
            if (routine.stopMode == Routine.STOP_TAPS && sessionTaps >= routine.stopTaps) {
                stop("Listo: " + Ui.number(sessionTaps) + " toques");
                return;
            }
            if (routine.stopMode == Routine.STOP_TIME
                    && SystemClock.elapsedRealtime() - startedAt >= routine.stopMinutes * 60_000L) {
                stop("Listo: se cumplió el tiempo (" + Routine.minutes(routine.stopMinutes) + ")");
                return;
            }

            index = (index + 1) % targets.size();

            int interval;
            if (routine.natural) {
                // El promedio se mantiene; cada toque varía ~±12% con una deriva lenta.
                drift = drift * 0.92 + rnd.nextGaussian() * 0.02;
                drift = Math.max(-0.08, Math.min(0.08, drift));
                double jitter = Math.max(-0.3, Math.min(0.3, rnd.nextGaussian() * 0.12));
                interval = (int) Math.round(base * (1 + drift + jitter));
            } else {
                interval = base;
            }
            long delay = Math.max(interval, duration + 10);
            if (routine.stopMode == Routine.STOP_TIME) {
                // Con ritmos lentos, no esperar más allá del límite de tiempo
                long left = routine.stopMinutes * 60_000L - (SystemClock.elapsedRealtime() - startedAt);
                delay = Math.max(10, Math.min(delay, left));
            }
            handler.postDelayed(this, delay);
        }
    };

    // ================================================================ utilidades

    private Routine freshRoutine() {
        Routine r = routine != null ? Store.byId(this, routine.id) : null;
        return r != null ? r : Store.active(this);
    }

    private void relayout() {
        if (!shown) return;
        stop(null);
        for (Target t : targets) detachTarget(t);
        targets.clear();
        routine = freshRoutine();
        restoreTargets();
        if (expanded) {
            // Los puntos se volvieron a agregar: el panel debe quedar encima de ellos
            detach(panel, panelAttached);
            panelAttached = false;
            showPanel();
        } else if (bubbleAttached) {
            detach(bubble, true);
            bubbleAttached = false;
            showBubble(); // mantiene el lado (onRight) y recoloca con el nuevo ancho
        }
    }

    private WindowManager.LayoutParams overlayLp(int w, int h) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                w, h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        if (Build.VERSION.SDK_INT >= 30) {
            // Coordenadas de pantalla completas, sin desplazamiento por barras del sistema
            lp.setFitInsetsTypes(0);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        return lp;
    }

    private boolean attach(View v, WindowManager.LayoutParams lp) {
        try {
            wm.addView(v, lp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void detach(View v, boolean attached) {
        if (v == null || !attached) return;
        try {
            wm.removeView(v);
        } catch (Exception ignored) {
        }
    }

    private void update(View v, WindowManager.LayoutParams lp) {
        try {
            wm.updateViewLayout(v, lp);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    private int[] screenSize() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                Rect b = wm.getMaximumWindowMetrics().getBounds();
                return new int[]{b.width(), b.height()};
            }
            DisplayMetrics m = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(m);
            return new int[]{m.widthPixels, m.heightPixels};
        } catch (Exception e) {
            DisplayMetrics m = getResources().getDisplayMetrics();
            return new int[]{m.widthPixels, m.heightPixels};
        }
    }

    private int screenW() {
        return screenSize()[0];
    }

    private int screenH() {
        return screenSize()[1];
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(28);
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String compact(long n) {
        if (n < 10_000) return Ui.number(n);
        if (n < 1_000_000) return (n / 1000) + "k";
        return (n / 1_000_000) + "M";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
