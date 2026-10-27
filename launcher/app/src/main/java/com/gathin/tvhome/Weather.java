package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Current weather for one place from Open-Meteo (no key), shown as one line in the header. */
final class Weather {
    private static final String PREFS = "weather";
    private static final long REFRESH_MS = 30 * 60 * 1000L;
    private static final long RETRY_MS = 5 * 60 * 1000L;

    private final Context ctx;
    private final TextView view;
    private final String city;
    private final String url;
    private final SharedPreferences prefs;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean loading;
    private long lastAttempt;

    Weather(Context ctx, TextView view) {
        this.ctx = ctx;
        this.view = view;
        this.city = ctx.getString(R.string.weather_city);
        String lat = ctx.getString(R.string.weather_latitude);
        String lon = ctx.getString(R.string.weather_longitude);
        this.url = TextUtils.isEmpty(lat) || TextUtils.isEmpty(lon) ? null
                : "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=" + lat + "&lon=" + lon;
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        view.setVisibility(url == null ? View.GONE : View.VISIBLE);
        show();
    }

    void refreshIfStale() {
        if (url == null || loading) return;
        long now = System.currentTimeMillis();
        if (now - prefs.getLong("at", 0) < REFRESH_MS || now - lastAttempt < RETRY_MS) return;
        lastAttempt = now;
        loading = true;
        worker.execute(() -> {
            try {
                JSONObject data = new JSONObject(fetch(url)).getJSONObject("properties")
                        .getJSONArray("timeseries").getJSONObject(0).getJSONObject("data");
                double temp = data.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature");
                JSONObject next = data.optJSONObject("next_1_hours");
                if (next == null) next = data.optJSONObject("next_6_hours");
                String symbol = next == null ? "" : next.getJSONObject("summary").optString("symbol_code", "");
                prefs.edit()
                        .putFloat("temp", (float) temp)
                        .putString("symbol", symbol)
                        .putLong("at", System.currentTimeMillis())
                        .apply();
            } catch (Exception e) {
                android.util.Log.w("Weather", "fetch failed", e);
            }
            main.post(() -> {
                loading = false;
                show();
            });
        });
    }

    void close() {
        worker.shutdownNow();
    }

    private void show() {
        if (!prefs.contains("symbol")) {
            view.setText(city);
            return;
        }
        int temp = Math.round(prefs.getFloat("temp", 0));
        String symbol = prefs.getString("symbol", "");
        String sign = temp > 0 ? "+" : temp < 0 ? "\u2212" : "";
        String line = String.format(Locale.ROOT, "%s  %s%d°  %s", icon(symbol), sign, Math.abs(temp), describe(symbol));
        view.setText(TextUtils.isEmpty(city) ? line : line + " · " + city);
    }

    private static String fetch(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setRequestProperty("User-Agent", "tvhome-custom/1.0 github.com/Gathin23/cleanyourtv");
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            c.disconnect();
        }
    }

    static String icon(String s) {
        boolean night = s.endsWith("_night") || s.endsWith("_polartwilight");
        if (s.contains("thunder")) return "\u26C8\uFE0F";
        if (s.contains("snow") || s.contains("sleet")) return "\u2744\uFE0F";
        if (s.contains("showers")) return "\uD83C\uDF26\uFE0F";
        if (s.contains("rain")) return "\uD83C\uDF27\uFE0F";
        if (s.startsWith("fog")) return "\uD83C\uDF2B\uFE0F";
        if (s.startsWith("clearsky")) return night ? "\uD83C\uDF19" : "\u2600\uFE0F";
        if (s.startsWith("fair") || s.startsWith("partlycloudy")) return night ? "\u2601\uFE0F" : "\u26C5";
        return "\u2601\uFE0F";
    }

    static String describe(String s) {
        String base = s.replaceAll("_(day|night|polartwilight)$", "");
        if (base.contains("thunder")) return "Гроза";
        if (base.contains("sleet")) return "Мокрый снег";
        if (base.contains("snow")) return base.startsWith("light") ? "Небольшой снег"
                : base.startsWith("heavy") ? "Сильный снег" : "Снег";
        if (base.contains("rain")) return base.startsWith("light") ? "Небольшой дождь"
                : base.startsWith("heavy") ? "Ливень" : "Дождь";
        switch (base) {
            case "clearsky": return "Ясно";
            case "fair": return "Малооблачно";
            case "partlycloudy": return "Переменная облачность";
            case "cloudy": return "Пасмурно";
            case "fog": return "Туман";
            default: return "";
        }
    }
}
