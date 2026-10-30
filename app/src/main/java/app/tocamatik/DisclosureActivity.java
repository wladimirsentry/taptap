package app.tocamatik;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Aviso destacado y consentimiento que exige Google Play antes de pedir el permiso de accesibilidad.
 * Se muestra siempre antes de enviar al usuario a Ajustes, y se puede volver a ver desde Ajustes de Tocamatik.
 */
public class DisclosureActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 6));
        LinearLayout back = Ui.iconButton(this, R.drawable.ic_chevron_left, "Volver");
        back.setOnClickListener(v -> finish());
        header.addView(back);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout c = Ui.col(this);
        c.setPadding(Ui.dp(this, 22), Ui.dp(this, 12), Ui.dp(this, 22), Ui.dp(this, 16));
        scroll.addView(c);

        c.addView(Ui.iconBox(this, R.drawable.ic_shield_check, Ui.P, Ui.PS, 56, 28, 18));
        c.addView(Ui.text(this, "Antes de activar Tocamatik", 26, Ui.INK, Ui.W_EXTRA),
                Ui.margins(Ui.lpMatchWrap(), this, 0, 18, 0, 0));
        TextView intro = Ui.text(this,
                "Tocamatik necesita el permiso de Accesibilidad de Android. Tocamatik no es una herramienta para personas con discapacidad: usa este permiso para automatizar toques. Lee qué hace antes de aceptar.",
                14.5f, Ui.MUTED, Ui.W_MEDIUM);
        intro.setLineSpacing(0, 1.35f);
        c.addView(intro, Ui.margins(Ui.lpMatchWrap(), this, 0, 10, 0, 0));

        c.addView(section(R.drawable.ic_pointer, Ui.P, Ui.PS, "Para qué lo usa",
                "• Tocar automáticamente los puntos que tú colocas en la pantalla, a la velocidad que eliges.\n"
                        + "• Mostrar su burbuja y panel flotante encima de otras apps."));
        c.addView(section(R.drawable.ic_eye_off, Ui.MINT, Ui.MINT_S, "Lo que no hace",
                "• No lee, no guarda y no envía el contenido de la pantalla ni lo que escribes.\n"
                        + "• No recopila ni comparte datos personales. Todo queda en tu teléfono.\n"
                        + "• No toca nada hasta que tú presionas ▶ en el panel."));
        c.addView(section(R.drawable.ic_lock, Ui.AMBER, Ui.AMBER_S, "Tú tienes el control",
                "• Puedes detenerlo en cualquier momento desde la burbuja.\n"
                        + "• Puedes quitar el permiso cuando quieras en Ajustes › Accesibilidad › Tocamatik."));

        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout accept = Ui.button(this, "Aceptar y activar", Ui.P, Color.WHITE, R.drawable.ic_check);
        accept.setOnClickListener(v -> {
            Store.setConsent(this, true);
            if (!Store.serviceEnabled(this)) MainActivity.openAccessibilitySettings(this);
            finish();
        });
        root.addView(accept, Ui.margins((LinearLayout.LayoutParams) accept.getLayoutParams(), this, 20, 8, 20, 4));

        TextView decline = Ui.text(this, "No, gracias", 15, Ui.MUTED, Ui.W_BOLD);
        decline.setGravity(Gravity.CENTER);
        decline.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
        decline.setClickable(true);
        decline.setOnClickListener(v -> {
            Store.setConsent(this, false);
            TocamatikService s = TocamatikService.instance;
            if (s != null) s.hideOverlay();
            finish();
        });
        root.addView(decline, Ui.margins(Ui.lpMatchWrap(), this, 20, 0, 20, 8));

        setContentView(root);
        Ui.edgeToEdge(this, root);
    }

    private View section(int icon, int fg, int bg, String title, String body) {
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        LinearLayout head = Ui.row(this);
        head.addView(Ui.iconBox(this, icon, fg, bg, 34, 18, 11));
        TextView t = Ui.text(this, title, 15.5f, Ui.INK, Ui.W_EXTRA);
        t.setPadding(Ui.dp(this, 12), 0, 0, 0);
        head.addView(t);
        card.addView(head);
        TextView b = Ui.text(this, body, 14, 0xFF3D3A55, Ui.W_MEDIUM);
        b.setLineSpacing(0, 1.4f);
        card.addView(b, Ui.margins(Ui.lpMatchWrap(), this, 0, 12, 0, 0));
        LinearLayout.LayoutParams lp = Ui.margins(Ui.lpMatchWrap(), this, 0, 14, 0, 0);
        card.setLayoutParams(lp);
        return card;
    }
}
