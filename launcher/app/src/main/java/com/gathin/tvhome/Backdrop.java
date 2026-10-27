package com.gathin.tvhome;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.File;

/** Home's moving background: a looping muted video (backdrop.mp4 in the theme dir), the AuroraView, or nothing. */
final class Backdrop {
    static final String MODE_KEY = "backdrop_mode";
    static final String VIDEO = "video";
    static final String ANIMATED = "animated";
    static final String OFF = "off";
    static final String FILE_NAME = "backdrop.mp4";
    private static final String TAG = "Backdrop";

    private final Context ctx;
    private final FrameLayout root;
    private AuroraView aurora;
    private TextureView texture;
    private MediaPlayer player;
    private Surface surface;
    private String mode = OFF;
    private boolean active;

    Backdrop(Context ctx, FrameLayout root) {
        this.ctx = ctx;
        this.root = root;
    }

    static String mode(Context ctx) {
        String m = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getString(MODE_KEY, VIDEO);
        return VIDEO.equals(m) || ANIMATED.equals(m) || OFF.equals(m) ? m : VIDEO;
    }

    static File videoFile(Context ctx) {
        File dir = Config.dir(ctx);
        return dir == null ? null : new File(dir, FILE_NAME);
    }

    static boolean hasVideo(Context ctx) {
        File f = videoFile(ctx);
        return f != null && f.isFile() && f.length() > 0;
    }

    void start() {
        active = true;
        String wanted = mode(ctx);
        if (VIDEO.equals(wanted) && !hasVideo(ctx)) wanted = ANIMATED;
        if (!wanted.equals(mode)) {
            detach();
            mode = wanted;
        }
        if (ANIMATED.equals(mode)) {
            if (aurora == null) {
                aurora = new AuroraView(ctx);
                root.addView(aurora, 0, full());
            }
            aurora.start();
        } else if (VIDEO.equals(mode)) {
            if (texture == null) {
                texture = new TextureView(ctx);
                texture.setAlpha(0f);
                texture.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                texture.setSurfaceTextureListener(listener);
                root.addView(texture, 0, full());
            } else if (texture.isAvailable() && player == null) {
                play(texture.getSurfaceTexture());
            }
        }
    }

    void stop() {
        active = false;
        if (aurora != null) aurora.stop();
        releasePlayer();
        if (texture != null) texture.setAlpha(0f);
    }

    void close() {
        stop();
        detach();
    }

    private void detach() {
        releasePlayer();
        if (aurora != null) {
            aurora.stop();
            root.removeView(aurora);
            aurora = null;
        }
        if (texture != null) {
            root.removeView(texture);
            texture = null;
        }
    }

    private static FrameLayout.LayoutParams full() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void play(SurfaceTexture st) {
        if (!active || player != null) return;
        File f = videoFile(ctx);
        if (f == null || !f.isFile()) return;
        MediaPlayer mp = new MediaPlayer();
        try {
            surface = new Surface(st);
            mp.setSurface(surface);
            mp.setDataSource(f.getAbsolutePath());
            mp.setLooping(true);
            mp.setVolume(0f, 0f);
            mp.setOnPreparedListener(p -> {
                if (player != p) return;
                p.start();
            });
            mp.setOnInfoListener((p, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && texture != null) {
                    texture.animate().alpha(1f).setDuration(400).start();
                }
                return false;
            });
            mp.setOnErrorListener((p, what, extra) -> {
                Log.w(TAG, "video error " + what + "/" + extra);
                releasePlayer();
                return true;
            });
            player = mp;
            mp.prepareAsync();
        } catch (Exception e) {
            Log.w(TAG, "cannot play " + f, e);
            mp.release();
            player = null;
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.release();
            } catch (RuntimeException ignored) {
            }
            player = null;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }

    private final TextureView.SurfaceTextureListener listener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
            play(st);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
            releasePlayer();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture st) {
        }
    };
}
