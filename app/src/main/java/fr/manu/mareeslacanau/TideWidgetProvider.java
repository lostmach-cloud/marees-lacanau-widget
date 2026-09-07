package fr.manu.mareeslacanau;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TideWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH =
        "fr.manu.mareeslacanau.REFRESH";

    private static final ExecutorService EXECUTOR =
        Executors.newSingleThreadExecutor();

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        scheduleDailyUpdate(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        updateWidgets(context, manager, ids);
        scheduleDailyUpdate(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager manager =
                AppWidgetManager.getInstance(context);

            int[] ids = manager.getAppWidgetIds(
                new ComponentName(context, TideWidgetProvider.class)
            );

            updateWidgets(context, manager, ids);
        }
    }

    public static void scheduleDailyUpdate(Context context) {
        AlarmManager alarm =
            (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, TideWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);

        PendingIntent pi = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 10);
        next.set(Calendar.SECOND, 0);

        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY,
            pi
        );
    }

    public static void updateWidgets(
        Context context,
        AppWidgetManager manager,
        int[] ids
    ) {
        if (ids == null || ids.length == 0) {
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                List<Tide> tides = ShomClient.fetch();

                for (int id : ids) {
                    RemoteViews views = baseViews(context);
                    fillTides(views, tides);
                    manager.updateAppWidget(id, views);
                }

            } catch (Exception e) {
                for (int id : ids) {
                    RemoteViews views = baseViews(context);

                    views.setViewVisibility(R.id.status, View.VISIBLE);
                    views.setTextViewText(
                        R.id.status,
                        "Données indisponibles"
                    );

                    manager.updateAppWidget(id, views);
                }
            }
        });
    }

    private static RemoteViews baseViews(Context context) {
        RemoteViews views = new RemoteViews(
            context.getPackageName(),
            R.layout.widget_tides
        );

        views.setTextViewText(
            R.id.title,
            "🌊  MARÉES · LACANAU"
        );

        views.setViewVisibility(R.id.status, View.GONE);

        Intent refresh = new Intent(context, TideWidgetProvider.class);
        refresh.setAction(ACTION_REFRESH);

        PendingIntent refreshIntent = PendingIntent.getBroadcast(
            context,
            1,
            refresh,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.root, refreshIntent);

        return views;
    }

    private static void fillTides(RemoteViews views, List<Tide> tides) {
        if (tides.size() < 4) {
            throw new IllegalStateException("Marées incomplètes");
        }

        Tide pm1 = tides.get(0);
        Tide bm1 = tides.get(1);
        Tide pm2 = tides.get(2);
        Tide bm2 = tides.get(3);

        String line1 =
            "↑ " + pm1.time +
            "    ↓ " + bm1.time +
            "    coef. " + pm1.coefficient;

        String line2 =
            "↑ " + pm2.time +
            "    ↓ " + bm2.time +
            "    coef. " + pm2.coefficient;

        views.setTextViewText(R.id.line1, line1);
        views.setTextViewText(R.id.line2, line2);
    }
}
