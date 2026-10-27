package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** Dark slowly drifting colour blobs, painted into a 160x90 bitmap and stretched over the screen. */
public class AuroraView extends View {
    private static final int W = 160;
    private static final int H = 90;
    private static final long FRAME_MS = 50;
    private static final int BACKGROUND = 0xFF05060A;
    private static final int[] COLORS = {0xFF1B2A6B, 0xFF3A1C5C, 0xFF0E4A4A, 0xFF22264F};
    private static final float[] PERIODS = {47f, 61f, 53f, 71f};
    private static final float[] PHASES = {0f, 2.1f, 4.2f, 1.3f};
    private static final float[] RADII = {70f, 62f, 58f, 50f};

    private final Bitmap frame = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
    private final Canvas canvas = new Canvas(frame);
    private final Bitmap[] sprites = new Bitmap[COLORS.length];
    private final Paint spritePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint scalePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect dest = new Rect();
    private final long origin = SystemClock.uptimeMillis();
    private boolean running;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            invalidate();
            postDelayed(this, FRAME_MS);
        }
    };

    public AuroraView(Context context) {
        this(context, null);
    }

    public AuroraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        for (int i = 0; i < COLORS.length; i++) sprites[i] = sprite(COLORS[i], (int) (RADII[i] * 2));
        spritePaint.setAlpha(170);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private static Bitmap sprite(int color, int size) {
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = size / 2f;
        p.setShader(new RadialGradient(r, r, r, color, Color.argb(0, Color.red(color), Color.green(color),
                Color.blue(color)), Shader.TileMode.CLAMP));
        new Canvas(b).drawCircle(r, r, r, p);
        return b;
    }

    public void start() {
        if (running) return;
        running = true;
        removeCallbacks(tick);
        post(tick);
    }

    public void stop() {
        running = false;
        removeCallbacks(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    @Override
    protected void onDraw(Canvas out) {
        float t = (SystemClock.uptimeMillis() - origin) / 1000f;
        canvas.drawColor(BACKGROUND);
        for (int i = 0; i < sprites.length; i++) {
            double a = 2 * Math.PI * t / PERIODS[i] + PHASES[i];
            float cx = W * (0.5f + 0.38f * (float) Math.sin(a));
            float cy = H * (0.5f + 0.34f * (float) Math.sin(2 * a + PHASES[i]));
            canvas.drawBitmap(sprites[i], cx - RADII[i], cy - RADII[i], spritePaint);
        }
        dest.set(0, 0, getWidth(), getHeight());
        out.drawBitmap(frame, null, dest, scalePaint);
    }
}
