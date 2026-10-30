package app.tocamatik;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Vistas dibujadas a mano para que la app no dependa de librerías externas. */
final class Views {

    private Views() {}

    // =====================================================================
    /** Logo de Tocamatik: fondo degradado violeta, anillo blanco y punto central. */
    static final class LogoView extends View {
        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean circle;
        private final RectF rect = new RectF();

        LogoView(Context c, boolean circle) {
            super(c);
            this.circle = circle;
            ring.setStyle(Paint.Style.STROKE);
            ring.setColor(0xCCFFFFFF);
            dot.setColor(Color.WHITE);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            bg.setShader(new LinearGradient(0, 0, w, h, 0xFF6D5BFF, Ui.P_LIGHT, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), s = Math.min(w, h);
            float cx = w / 2f, cy = h / 2f;
            rect.set(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy + s / 2f);
            if (circle) c.drawOval(rect, bg);
            else c.drawRoundRect(rect, s * 0.3f, s * 0.3f, bg);
            ring.setStrokeWidth(s * 0.075f);
            c.drawCircle(cx, cy, s * 0.24f, ring);
            c.drawCircle(cx, cy, s * 0.095f, dot);
        }
    }

    // =====================================================================
    /** Interruptor redondeado (como el de las maquetas). */
    static final class ToggleView extends View {
        interface OnChange { void changed(boolean on); }

        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumb = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private boolean on;
        private float pos; // 0 = apagado, 1 = encendido
        private OnChange listener;

        ToggleView(Context c) {
            super(c);
            thumb.setColor(Color.WHITE);
            thumb.setShadowLayer(Ui.dp(c, 1.5f), 0, Ui.dp(c, 0.5f), 0x33000000);
            setLayerType(LAYER_TYPE_SOFTWARE, thumb);
            setClickable(true);
            setFocusable(true);
            setOnClickListener(v -> setOn(!on, true));
        }

        void setListener(OnChange l) { listener = l; }

        boolean isOn() { return on; }

        void setOn(boolean value, boolean animate) {
            boolean changed = value != on;
            on = value;
            setContentDescription(on ? "Activado" : "Desactivado");
            if (animate) {
                ValueAnimator a = ValueAnimator.ofFloat(pos, on ? 1f : 0f);
                a.setDuration(160);
                a.addUpdateListener(an -> { pos = (float) an.getAnimatedValue(); invalidate(); });
                a.start();
            } else {
                pos = on ? 1f : 0f;
                invalidate();
            }
            if (changed && animate && listener != null) listener.changed(on);
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            setMeasuredDimension(Ui.dp(getContext(), 46), Ui.dp(getContext(), 28));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            int off = 0xFFD9D6E6;
            track.setColor(blend(off, Ui.P, pos));
            rect.set(0, 0, w, h);
            c.drawRoundRect(rect, h / 2f, h / 2f, track);
            float r = h / 2f - Ui.dp(getContext(), 3);
            float x = h / 2f + (w - h) * pos;
            c.drawCircle(x, h / 2f, r, thumb);
        }
    }

    static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    // =====================================================================
    /** Barras que ilustran el ritmo variable del modo natural. */
    static final class BarsView extends View {
        private static final float[] H = {.60f, .82f, .55f, .74f, .92f, .63f, .70f, .50f, .86f, .66f, .78f, .58f, .72f, .88f};
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private boolean active = true;

        BarsView(Context c) {
            super(c);
        }

        void setActive(boolean a) { active = a; invalidate(); }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            Context c = getContext();
            setMeasuredDimension(Ui.dp(c, 14 * 9 - 4), Ui.dp(c, 34));
        }

        @Override
        protected void onDraw(Canvas c) {
            float bw = Ui.dp(getContext(), 5), gap = Ui.dp(getContext(), 4), h = getHeight();
            p.setColor(active ? Ui.P : 0xFFC9C5D9);
            for (int i = 0; i < H.length; i++) {
                float bh = active ? H[i] * h : 0.66f * h;
                float x = i * (bw + gap);
                r.set(x, h - bh, x + bw, h);
                c.drawRoundRect(r, bw / 2f, bw / 2f, p);
            }
        }
    }

    // =====================================================================
    /** Burbuja flotante. En reposo muestra el logo; tocando, un anillo que gira y el icono de pausa. */
    static final class BubbleView extends View {
        private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint logoBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint logoRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint logoDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private final Drawable pause;
        private boolean running;
        private float angle;
        private float pulse;
        private ValueAnimator spinner;

        BubbleView(Context c) {
            super(c);
            base.setColor(Ui.PANEL);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(Ui.dp(c, 1));
            border.setColor(0x26FFFFFF);
            track.setStyle(Paint.Style.STROKE);
            track.setStrokeWidth(Ui.dp(c, 3));
            track.setColor(0x3310B981);
            arc.setStyle(Paint.Style.STROKE);
            arc.setStrokeWidth(Ui.dp(c, 3));
            arc.setStrokeCap(Paint.Cap.ROUND);
            arc.setColor(0xFF5BF0B5);
            logoRing.setStyle(Paint.Style.STROKE);
            logoRing.setColor(0xCCFFFFFF);
            logoDot.setColor(Color.WHITE);
            pause = c.getDrawable(R.drawable.ic_pause_fill);
            if (pause != null) pause.setTint(Color.WHITE);
            setContentDescription("Tocamatik");
        }

        void setRunning(boolean r) {
            if (running == r) return;
            running = r;
            if (r) {
                spinner = ValueAnimator.ofFloat(0f, 1f);
                spinner.setDuration(1400);
                spinner.setRepeatCount(ValueAnimator.INFINITE);
                spinner.setInterpolator(new LinearInterpolator());
                spinner.addUpdateListener(a -> {
                    float f = (float) a.getAnimatedValue();
                    angle = f * 360f;
                    pulse = (float) (0.5 + 0.5 * Math.sin(f * Math.PI * 2));
                    invalidate();
                });
                spinner.start();
                setContentDescription("Tocamatik tocando. Toca para pausar");
            } else {
                if (spinner != null) spinner.cancel();
                spinner = null;
                setContentDescription("Tocamatik. Toca para abrir");
                invalidate();
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            float s = Math.min(w, h) * 0.58f;
            float cx = w / 2f, cy = h / 2f;
            logoBg.setShader(new LinearGradient(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2,
                    0xFF6D5BFF, Ui.P_LIGHT, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (spinner != null) spinner.cancel();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2f, cy = h / 2f;
            float pad = Ui.dp(getContext(), 4);
            float r = Math.min(w, h) / 2f - pad;
            c.drawCircle(cx, cy, r, base);
            c.drawCircle(cx, cy, r, border);
            if (running) {
                float ar = r - Ui.dp(getContext(), 1);
                oval.set(cx - ar, cy - ar, cx + ar, cy + ar);
                c.drawOval(oval, track);
                arc.setAlpha((int) (170 + 85 * pulse));
                c.drawArc(oval, angle - 90, 130, false, arc);
                if (pause != null) {
                    int s = Ui.dp(getContext(), 22);
                    pause.setBounds((int) (cx - s / 2f), (int) (cy - s / 2f), (int) (cx + s / 2f), (int) (cy + s / 2f));
                    pause.draw(c);
                }
            } else {
                float s = Math.min(w, h) * 0.58f;
                c.drawCircle(cx, cy, s / 2f, logoBg);
                logoRing.setStrokeWidth(s * 0.075f);
                c.drawCircle(cx, cy, s * 0.24f, logoRing);
                c.drawCircle(cx, cy, s * 0.095f, logoDot);
            }
        }
    }

    // =====================================================================
    /** Punto de toque numerado, con onda al tocar. La ventana es más grande que el círculo para la onda. */
    static final class TargetView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ripple = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int number;
        private boolean dim;
        private float wave = -1f;
        private ValueAnimator waveAnim;

        TargetView(Context c, int number) {
            super(c);
            this.number = number;
            fill.setColor(0x8C5B47F5);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Ui.dp(c, 3));
            stroke.setColor(Color.WHITE);
            halo.setStyle(Paint.Style.STROKE);
            halo.setStrokeWidth(Ui.dp(c, 5));
            halo.setColor(0x557C6BFF);
            ripple.setStyle(Paint.Style.STROKE);
            ripple.setStrokeWidth(Ui.dp(c, 2));
            ripple.setColor(Color.WHITE);
            label.setColor(Color.WHITE);
            label.setTextAlign(Paint.Align.CENTER);
            label.setTypeface(Ui.font(c, Ui.W_EXTRA));
            label.setTextSize(Ui.dp(c, 17));
            label.setShadowLayer(Ui.dp(c, 2), 0, 0, 0x88000000);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
            setContentDescription("Punto " + number);
        }

        void setNumber(int n) {
            number = n;
            setContentDescription("Punto " + n);
            invalidate();
        }

        void setDim(boolean d) {
            dim = d;
            invalidate();
        }

        /** Onda corta que se expande cuando Tocamatik toca este punto. */
        void pulse() {
            if (waveAnim != null) waveAnim.cancel();
            waveAnim = ValueAnimator.ofFloat(0f, 1f);
            waveAnim.setDuration(380);
            waveAnim.addUpdateListener(a -> { wave = (float) a.getAnimatedValue(); invalidate(); });
            waveAnim.start();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (waveAnim != null) waveAnim.cancel();
        }

        @Override
        protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float r = Ui.dp(getContext(), 28);
            int alpha = dim ? 150 : 255;
            fill.setAlpha(dim ? 90 : 140);
            stroke.setAlpha(alpha);
            halo.setAlpha(dim ? 50 : 85);
            label.setAlpha(alpha);
            c.drawCircle(cx, cy, r + Ui.dp(getContext(), 4), halo);
            c.drawCircle(cx, cy, r, fill);
            c.drawCircle(cx, cy, r, stroke);
            if (wave >= 0f && wave < 1f) {
                float max = Math.min(getWidth(), getHeight()) / 2f - Ui.dp(getContext(), 2);
                ripple.setAlpha((int) (200 * (1f - wave)));
                c.drawCircle(cx, cy, r + (max - r) * wave, ripple);
            }
            float ty = cy - (label.descent() + label.ascent()) / 2f;
            c.drawText(String.valueOf(number), cx, ty, label);
        }
    }
}
