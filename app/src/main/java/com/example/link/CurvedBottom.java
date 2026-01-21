package com.example.link;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class CurvedBottom extends View {
    private Path path;
    private Paint paint;
    private Paint shadowPaint;
    private Path shadowPath;

    private int navigationBarHeight = 180;
    private int curveRadius = 110;
    private float centerX, centerY;

    public CurvedBottom(Context context) {
        super(context);
        init();
    }

    public CurvedBottom(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CurvedBottom(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        path = new Path();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE); // Add default white color

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(Color.WHITE); // Set white color for shadow base

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        centerX = width / 2f;
        centerY = 0f;

        path.reset();

        path.moveTo(0, 0);
        path.lineTo(centerX - curveRadius, 0);

        path.arcTo(
            centerX - curveRadius,
            -curveRadius,
            centerX + curveRadius,
            curveRadius,
            180,
            -180,
            false
        );

        path.lineTo(width, 0);
        path.lineTo(width, height);
        path.lineTo(0, height);
        path.close();

        // Draw shadow first
        shadowPaint.setColor(Color.WHITE);
        shadowPaint.setShadowLayer(25, 0, -8, Color.argb(50, 0, 0, 0));
        canvas.drawPath(path, shadowPaint);

        // Draw main shape with gradient
        LinearGradient gradient = new LinearGradient(
            0, 0, 0, height,
            new int[]{
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#F8F9FA")
            },
            new float[]{0f, 1f},
            Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
        paint.setShadowLayer(15, 0, -4, Color.argb(25, 0, 0, 0));
        canvas.drawPath(path, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), navigationBarHeight);
    }
}
