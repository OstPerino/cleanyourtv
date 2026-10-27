package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

/** The "Фильмы" tile: opens the movies folder (movies_uri) straight in VLC's network browser. */
public final class MoviesActivity extends Activity {
    static final String VLC = "org.videolan.vlc";
    static final String BROWSER = "org.videolan.television.ui.browser.VerticalGridActivity";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String uri = getString(R.string.movies_uri);
        if (TextUtils.isEmpty(uri)) {
            Toast.makeText(this, R.string.movies_not_set, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Intent browser = new Intent(Intent.ACTION_VIEW)
                .setComponent(new ComponentName(VLC, BROWSER))
                .setData(Uri.parse(uri))
                .putExtra("browser_type", 3L)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(browser);
        } catch (RuntimeException e) {
            Intent app = getPackageManager().getLeanbackLaunchIntentForPackage(VLC);
            if (app != null) {
                startActivity(app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                Toast.makeText(this, R.string.movies_no_vlc, Toast.LENGTH_LONG).show();
            }
        }
        finish();
    }
}
