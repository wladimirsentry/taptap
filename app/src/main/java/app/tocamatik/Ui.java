package app.tocamatik;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Colores, tipografía y piezas de interfaz compartidas por todas las pantallas. */
final class Ui {

    // Paleta
    static final int BG = 0xFFF5F4FA;
    static final int CARD = 0xFFFFFFFF;
    static final int INK = 0xFF17152B;
    static final int MUTED = 0xFF6E6A86;
    static final int LINE = 0xFFEAE8F3;
    static final int P = 0xFF5B47F5;
    static final int P_DARK = 0xFF4433D6;
    static final int P_LIGHT = 0xFF9B5CFF;
    static final int PS = 0xFFEFECFF;
    static final int MINT = 0xFF10B981;
    static final int MINT_S = 0xFFE3F7EF;
    static final int CORAL = 0xFFFF5A63;
    static final int AMBER = 0xFFE08600;
    static final int AMBER_S = 0xFFFFF2DD;
    static final int PANEL = 0xE6141224;

    static final int W_MEDIUM = 500;
    static final int W_BOLD = 700;
    static final int W_EXTRA = 800;

    private static Typeface medium, bold, extra;

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static Typeface font(Context c, int weight) {
        try {
            if (weight >= W_EXTRA) {
                if (extra == null) extra = c.getResources().getFont(R.font.jakarta_extrabold);
                return extra;
            }
            if (weight >= W_BOLD) {
                if (bold == null) bold = c.getResources().getFont(R.font.jakarta_bold);
                return bold;
            }
            if (medium == null) medium = c.getResources().getFont(R.font.jakarta_medium);
            return medium;
        } catch (Exception e) {
            return weight >= W_BOLD ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        }
    }

    static TextView text(Context c, CharSequence s, float sp, int color, int weight) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(font(c, weight));
        t.setIncludeFontPadding(false);
        return t;
    }

    static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    static GradientDrawable roundStroke(int color, float radiusPx, int strokePx, int strokeColor) {
        GradientDrawable d = round(color, radiusPx);
        d.setStroke(strokePx, strokeColor);
        return d;
    }

    static GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    static GradientDrawable gradient(float radiusPx, int... colors) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(radiusPx);
        return d;
    }

    /** Fondo con efecto de toque (ripple). */
    static Drawable pressable(Drawable content, int rippleColor) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }

    static ImageView icon(Context c, int res, int color, int sizeDp) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setImageTintList(ColorStateList.valueOf(color));
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return iv;
    }

    /** Cuadro redondeado con un icono centrado (los iconos de color de las rutinas). */
    static LinearLayout iconBox(Context c, int res, int fg, int bg, int boxDp, int iconDp, int radiusDp) {
        LinearLayout box = new LinearLayout(c);
        box.setGravity(Gravity.CENTER);
        box.setBackground(round(bg, dp(c, radiusDp)));
        box.addView(icon(c, res, fg, iconDp));
        box.setLayoutParams(new LinearLayout.LayoutParams(dp(c, boxDp), dp(c, boxDp)));
        return box;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = col(c);
        l.setBackground(roundStroke(CARD, dp(c, 22), dp(c, 1), LINE));
        return l;
    }

    /** Botón grande de ancho completo, con icono opcional (0 = sin icono). */
    static LinearLayout button(Context c, String label, int bg, int fg, int iconRes) {
        LinearLayout b = row(c);
        b.setGravity(Gravity.CENTER);
        b.setBackground(pressable(round(bg, dp(c, 18)), fg == Color.WHITE ? 0x33FFFFFF : 0x225B47F5));
        b.setClickable(true);
        b.setFocusable(true);
        if (iconRes != 0) {
            ImageView iv = icon(c, iconRes, fg, 18);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
            lp.rightMargin = dp(c, 8);
            b.addView(iv);
        }
        b.addView(text(c, label, 15.5f, fg, W_EXTRA));
        b.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 54)));
        b.setContentDescription(label);
        return b;
    }

    /** Botón cuadrado blanco de la cabecera. */
    static LinearLayout iconButton(Context c, int res, String description) {
        LinearLayout b = row(c);
        b.setGravity(Gravity.CENTER);
        b.setBackground(pressable(roundStroke(CARD, dp(c, 14), dp(c, 1), LINE), 0x225B47F5));
        b.addView(icon(c, res, INK, 20));
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40)));
        b.setClickable(true);
        b.setFocusable(true);
        b.setContentDescription(description);
        return b;
    }

    static TextView chip(Context c, String label, int fg, int bg) {
        TextView t = text(c, label, 12.5f, fg, W_BOLD);
        t.setPadding(dp(c, 11), dp(c, 7), dp(c, 11), dp(c, 7));
        t.setBackground(round(bg, dp(c, 999)));
        return t;
    }

    static LinearLayout.LayoutParams lpMatchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout.LayoutParams lpWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout.LayoutParams margins(LinearLayout.LayoutParams lp, Context c, int l, int t, int r, int b) {
        lp.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return lp;
    }

    static View space(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    static View weightSpace(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    static String number(long n) {
        return String.format(java.util.Locale.forLanguageTag("es-CL"), "%,d", n);
    }

    /**
     * Dibuja la app de borde a borde (obligatorio en Android 15+) y agrega a {@code root}
     * el espacio de la barra de estado, la de navegación y el teclado.
     */
    @SuppressWarnings("deprecation")
    static void edgeToEdge(Activity a, View root) {
        Window w = a.getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            WindowInsetsController ctl = w.getInsetsController();
            if (ctl != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                ctl.setSystemBarsAppearance(mask, mask);
            }
        } else {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR; // API 26+ (minSdk 26)
            w.getDecorView().setSystemUiVisibility(flags);
        }

        final int pl = root.getPaddingLeft(), pt = root.getPaddingTop();
        final int pr = root.getPaddingRight(), pb = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                l = bars.left;
                t = bars.top;
                r = bars.right;
                b = Math.max(bars.bottom, ime.bottom);
            } else {
                l = insets.getSystemWindowInsetLeft();
                t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight();
                b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(pl + l, pt + t, pr + r, pb + b);
            return insets;
        });
        root.requestApplyInsets();
    }
}
