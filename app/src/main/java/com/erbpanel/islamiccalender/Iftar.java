package com.erbpanel.islamiccalender;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Iftar extends AppWidgetProvider {
    private static final String TAG = "IftarWidget";
    private static final String ACTION_UPDATE_WIDGET = "com.erbpanel.islamiccalender.UPDATE_WIDGET";
    private static FusedLocationProviderClient fusedLocationClient;
    private static String BASE_URL;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        BASE_URL = context.getString(R.string.base_api_url);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.iftar);
        views.setTextViewText(R.id.hijri_date, getHijriDate());
        views.setTextViewText(R.id.sehri_time, "Fetching...");
        views.setTextViewText(R.id.iftar_time, "Fetching...");

        fetchLocationAndPrayerTimes(context, appWidgetManager, appWidgetId, views);

        // Refresh on click
        Intent intent = new Intent(context, Iftar.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.hijri_date, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, Iftar.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    private static String getHijriDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }

    private static void fetchLocationAndPrayerTimes(Context context, AppWidgetManager appWidgetManager, int appWidgetId, RemoteViews views) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted!");
            views.setTextViewText(R.id.sehri_time, "Permission Needed");
            views.setTextViewText(R.id.iftar_time, "Enable Location");
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        Log.d(TAG, "Latitude: " + latitude + ", Longitude: " + longitude);
                        fetchPrayerTimes(context, appWidgetManager, appWidgetId, views, latitude, longitude);
                    } else {
                        Log.e(TAG, "Location Unavailable");
                        views.setTextViewText(R.id.sehri_time, "Location Unavailable");
                        views.setTextViewText(R.id.iftar_time, "Enable GPS");
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location: " + e.getMessage());
                    views.setTextViewText(R.id.sehri_time, "Location Error");
                    views.setTextViewText(R.id.iftar_time, "Check GPS");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                });
    }

    private static void fetchPrayerTimes(Context context, AppWidgetManager appWidgetManager, int appWidgetId, RemoteViews views, double latitude, double longitude) {
        String url = BASE_URL + "timings/" + new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime()) + "?latitude=" + latitude + "&longitude=" + longitude;

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject data = response.getJSONObject("data").getJSONObject("timings");
                        String sehri = convertTo12HourFormat(data.getString("Fajr"));
                        String iftar = convertTo12HourFormat(data.getString("Maghrib"));

                        views.setTextViewText(R.id.sehri_time, "Sehri: " + sehri);
                        views.setTextViewText(R.id.iftar_time, "Iftar: " + iftar);
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        views.setTextViewText(R.id.sehri_time, "API Error");
                        views.setTextViewText(R.id.iftar_time, "Try Again");
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                }, error -> {
            Log.e(TAG, "API Request Failed: " + error.getMessage());
            views.setTextViewText(R.id.sehri_time, "Network Error");
            views.setTextViewText(R.id.iftar_time, "Try Again");
            appWidgetManager.updateAppWidget(appWidgetId, views);
        });

        queue.add(jsonObjectRequest);
    }

    private static String convertTo12HourFormat(String time) {
        try {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new SimpleDateFormat("HH:mm", Locale.getDefault()).parse(time));
        } catch (Exception e) {
            return time;
        }
    }
}
