package fr.manu.mareeslacanau;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        text.setText(
            "🌊 Marées Lacanau\n\n" +
            "Pour ajouter le widget :\n\n" +
            "Appui long sur l'écran d'accueil → Widgets → Marées Lacanau.\n\n" +
            "Touchez ensuite le widget pour actualiser les données."
        );
        text.setTextSize(18);
        text.setPadding(48, 48, 48, 48);
        setContentView(text);

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] ids = manager.getAppWidgetIds(
            new ComponentName(this, TideWidgetProvider.class)
        );
        TideWidgetProvider.updateWidgets(this, manager, ids);
    }
}
