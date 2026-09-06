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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
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
    public void onUpdate(
        Context context,
        AppWidgetManager manager,
        int[] appWidgetIds
    ) {
        updateWidgets(context, manager, appWidgetIds);
        scheduleDailyUpdate(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        if (ACTION_REFRESH.equals(intent.getAction())) {

            AppWidgetManager manager =
                AppWidgetManager.getInstance(context);

            int[] ids = manager.getAppWidgetIds(
                new ComponentName(
                    context,
                    TideWidgetProvider.class
                )
            );

            updateWidgets(context, manager, ids);
        }
    }

    public static void scheduleDailyUpdate(Context context) {

        AlarmManager alarm =
            (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE
            );

        Intent intent =
            new Intent(context, TideWidgetProvider.class);

        intent.setAction(ACTION_REFRESH);

        PendingIntent pi =
            PendingIntent.getBroadcast(
                context,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                PendingIntent.FLAG_IMMUTABLE
            );

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 10);
        next.set(Calendar.SECOND, 0);

        if (next.getTimeInMillis()
                <= System.currentTimeMillis()) {

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

        for (int id : ids) {

            RemoteViews loading =
                baseViews(context);

            loading.setTextViewText(
                R.id.status,
                "Actualisation…"
            );

            manager.updateAppWidget(id, loading);
        }

        EXECUTOR.execute(() -> {

            try {

                List<Tide> tides =
                    ShomClient.fetch();

                for (int id : ids) {

                    RemoteViews views =
                        baseViews(context);

                    fillTides(views, tides);

                    views.setTextViewText(
                        R.id.status,
                        "Source : SHOM"
                    );

                    manager.updateAppWidget(
                        id,
                        views
                    );
                }

            } catch (Exception e) {

                for (int id : ids) {

                    RemoteViews views =
                        baseViews(context);

                    views.setTextViewText(
                        R.id.status,
                        "Données indisponibles · toucher pour réessayer"
                    );

                    manager.updateAppWidget(
                        id,
                        views
                    );
                }
            }
        });
    }

    private static RemoteViews baseViews(
        Context context
    ) {

        RemoteViews views =
            new RemoteViews(
                context.getPackageName(),
                R.layout.widget_tides
            );

        String date =
            LocalDate.now().format(
                DateTimeFormatter.ofPattern(
                    "EEE d MMM",
                    Locale.FRANCE
                )
            );

        views.setTextViewText(
            R.id.title,
            "🌊  MARÉES · LACANAU"
        );

        views.setTextViewText(
            R.id.date,
            date
        );

        Intent refresh =
            new Intent(
                context,
                TideWidgetProvider.class
            );

        refresh.setAction(ACTION_REFRESH);

        PendingIntent refreshIntent =
            PendingIntent.getBroadcast(
                context,
                1,
                refresh,
                PendingIntent.FLAG_UPDATE_CURRENT |
                PendingIntent.FLAG_IMMUTABLE
            );

        views.setOnClickPendingIntent(
            R.id.root,
            refreshIntent
        );

        int[] rows = {
            R.id.row1,
            R.id.row2,
            R.id.row3,
            R.id.row4
        };

        for (int row : rows) {
            views.setViewVisibility(row, View.GONE);
        }

        return views;
    }

    private static void fillTides(
        RemoteViews views,
        List<Tide> tides
    ) {

        int[] rows = {
            R.id.row1,
            R.id.row2,
            R.id.row3,
            R.id.row4
        };

        int[] symbols = {
            R.id.kind1,
            R.id.kind2,
            R.id.kind3,
            R.id.kind4
        };

        int[] times = {
            R.id.time1,
            R.id.time2,
            R.id.time3,
            R.id.time4
        };

        int[] details = {
            R.id.detail1,
            R.id.detail2,
            R.id.detail3,
            R.id.detail4
        };

        int count =
            Math.min(4, tides.size());

        for (int i = 0; i < count; i++) {

            Tide tide = tides.get(i);

            views.setViewVisibility(
                rows[i],
                View.VISIBLE
            );

            views.setTextViewText(
                symbols[i],
                "PM".equals(tide.type)
                    ? "↑"
                    : "↓"
            );

            views.setTextViewText(
                times[i],
                tide.time
            );

            String detail =
                tide.height;

            if (!tide.coefficient.isEmpty()) {
                detail +=
                    " · coef. " +
                    tide.coefficient;
            }

            views.setTextViewText(
                details[i],
                detail
            );
        }
    }
}
