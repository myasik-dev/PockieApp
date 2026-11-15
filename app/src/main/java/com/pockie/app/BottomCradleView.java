package com.pockie.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;

public class BottomCradleView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path path = new Path();
  private final RectF leftShoulderRect = new RectF();
  private final RectF cradleRect = new RectF();
  private final RectF rightShoulderRect = new RectF();

  private float barTop;
  private float fabRadius;
  private float cradleGap;
  private float shoulderRadius;

  public BottomCradleView(Context context) {
    super(context);
    init(context);
  }

  public BottomCradleView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public BottomCradleView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    float density = context.getResources().getDisplayMetrics().density;
    barTop = 28f * density;
    fabRadius = 28f * density;      // 56dp FAB radius
    cradleGap = 7f * density;       // Uniform gap around circular FAB
    shoulderRadius = 14f * density; // Smooth corner radius where cradle meets top bar

    paint.setColor(0xFFFFFFFF);
    paint.setStyle(Paint.Style.FILL);
    paint.setShadowLayer(10f * density, 0, -2f * density, 0x1A000000);
    setLayerType(LAYER_TYPE_SOFTWARE, null);
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    int w = getWidth();
    int h = getHeight();

    if (w <= 0 || h <= 0) return;

    float cx = w / 2f;
    float cy = barTop; // Center of FAB is aligned with the top edge of navigation bar

    float R = fabRadius + cradleGap; // Cradle circle radius
    float r = shoulderRadius;        // Shoulder circle radius

    // Distance between cradle center (cx, cy) and shoulder center (cx - dx, barTop + r) is D = R + r
    // Vertical distance = r
    // Horizontal distance dx = sqrt((R + r)^2 - r^2)
    float D = R + r;
    float dx = (float) Math.sqrt(D * D - r * r);

    // Angle of vector connecting centers relative to horizontal
    float alphaRad = (float) Math.atan2(r, dx);
    float alphaDeg = (float) Math.toDegrees(alphaRad);

    leftShoulderRect.set(cx - dx - r, barTop, cx - dx + r, barTop + 2f * r);
    cradleRect.set(cx - R, cy - R, cx + R, cy + R);
    rightShoulderRect.set(cx + dx - r, barTop, cx + dx + r, barTop + 2f * r);

    path.reset();

    // Top horizontal line to start of left shoulder
    path.moveTo(0, barTop);
    path.lineTo(cx - dx, barTop);

    // Left shoulder arc curving down into cradle
    path.arcTo(leftShoulderRect, 270f, alphaDeg);

    // Circular cradle arc wrapping around the bottom of the FAB
    float cradleStart = 180f - alphaDeg;
    float cradleSweep = -(180f - 2f * alphaDeg);
    path.arcTo(cradleRect, cradleStart, cradleSweep);

    // Right shoulder arc curving back up to top horizontal line
    path.arcTo(rightShoulderRect, 180f + alphaDeg, alphaDeg);

    // Top horizontal line to right edge of screen
    path.lineTo(w, barTop);

    // Right edge down to bottom of view
    path.lineTo(w, h);

    // Bottom edge to left of view
    path.lineTo(0, h);

    // Close path back to (0, barTop)
    path.close();

    canvas.drawPath(path, paint);
  }
}
