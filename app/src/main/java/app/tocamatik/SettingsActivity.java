package app.tocamatik;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Ajustes: permiso, privacidad, consentimiento y licencias. */
public class SettingsActivity extends Activity {

    private LinearLayout content;

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
        TextView title = Ui.text(this, "Ajustes", 17, Ui.INK, Ui.W_EXTRA);
        title.setPadding(Ui.dp(this, 14), 0, 0, 0);
        header.addView(title);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        content = Ui.col(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        Ui.edgeToEdge(this, root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        content.removeAllViews();
        boolean enabled = Store.serviceEnabled(this);

        content.addView(label("PERMISO"));
        LinearLayout perm = Ui.card(this);
        perm.addView(item(R.drawable.ic_smartphone, "Accesibilidad",
                enabled ? "Activado · toca para cambiarlo" : "Desactivado · toca para activarlo",
                v -> MainActivity.openAccessibilitySettings(this)));
        content.addView(perm);

        content.addView(label("PRIVACIDAD"));
        LinearLayout priv = Ui.card(this);
        priv.addView(item(R.drawable.ic_shield_check, "Política de privacidad", "Se abre en el navegador",
                v -> openUrl(getString(R.string.privacy_url))));
        priv.addView(line());
        priv.addView(item(R.drawable.ic_info, "Ver el aviso de uso del permiso", "Qué hace y qué no hace Tocamatik",
                v -> startActivity(new Intent(this, DisclosureActivity.class))));
        priv.addView(line());
        priv.addView(item(R.drawable.ic_x, "Retirar consentimiento", "Oculta Tocamatik y te lleva a quitar el permiso",
                v -> confirmRevoke()));
        content.addView(priv);

        content.addView(label("ACERCA DE"));
        LinearLayout about = Ui.card(this);
        about.addView(item(R.drawable.ic_info, "Versión", version(), null));
        about.addView(line());
        about.addView(item(R.drawable.ic_layers, "Licencias", "Tipografía e iconos de código abierto",
                v -> showLicenses()));
        content.addView(about);
    }

    private void confirmRevoke() {
        new AlertDialog.Builder(this)
                .setTitle("¿Retirar consentimiento?")
                .setMessage("Tocamatik dejará de mostrarse y te llevaremos a Accesibilidad para que quites el permiso.")
                .setPositiveButton("Retirar", (d, w) -> {
                    Store.setConsent(this, false);
                    TocamatikService s = TocamatikService.instance;
                    if (s != null) s.hideOverlay();
                    Toast.makeText(this, "Desactiva Tocamatik en esta pantalla", Toast.LENGTH_LONG).show();
                    MainActivity.openAccessibilitySettings(this);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showLicenses() {
        new AlertDialog.Builder(this)
                .setTitle("Licencias")
                .setMessage("Tipografía Plus Jakarta Sans — Copyright 2020 The Plus Jakarta Sans Project Authors. "
                        + "SIL Open Font License 1.1.\n\n"
                        + "Iconos Lucide — Copyright (c) Lucide Contributors. Licencia ISC.")
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("deprecation")
    private String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.1.0";
        }
    }

    private TextView label(String s) {
        TextView t = Ui.text(this, s, 12, Ui.MUTED, Ui.W_BOLD);
        t.setLayoutParams(Ui.margins(Ui.lpMatchWrap(), this, 4, 20, 0, 8));
        return t;
    }

    private View item(int icon, String title, String sub, View.OnClickListener click) {
        LinearLayout r = Ui.row(this);
        r.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        r.addView(Ui.iconBox(this, icon, Ui.P, Ui.PS, 38, 18, 12));
        LinearLayout txt = Ui.col(this);
        txt.setPadding(Ui.dp(this, 12), 0, 0, 0);
        txt.addView(Ui.text(this, title, 14.5f, Ui.INK, Ui.W_BOLD));
        TextView s = Ui.text(this, sub, 12.5f, Ui.MUTED, Ui.W_MEDIUM);
        s.setPadding(0, Ui.dp(this, 3), 0, 0);
        txt.addView(s);
        r.addView(txt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (click != null) {
            r.addView(Ui.icon(this, R.drawable.ic_chevron_right, Ui.MUTED, 18));
            r.setBackground(Ui.pressable(Ui.round(0xFFFFFFFF, Ui.dp(this, 22)), 0x155B47F5));
            r.setClickable(true);
            r.setOnClickListener(click);
        }
        return r;
    }

    private View line() {
        View v = new View(this);
        v.setBackgroundColor(Ui.LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));
        return v;
    }
}
