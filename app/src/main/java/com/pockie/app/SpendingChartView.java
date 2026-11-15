package com.pockie.app;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.*;

/** Draws data in the chart area declared by screen_reports.xml. */
public class SpendingChartView extends View {
  private static final int[] COLORS = {
    0xffffb43d, 0xff49bcf1, 0xfff48fba, 0xff9c82fc, 0xff54d9b9, 0xffffd346
  };
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF oval = new RectF();
  private final List<Long> amounts = new ArrayList<>();
  private long total;
  private String totalLabel = "₹ 0";

  public SpendingChartView(Context context) {
    this(context, null);
  }

  public SpendingChartView(Context context, AttributeSet attributes) {
    super(context, attributes);
  }

  public void setData(Map<String, Long> categories, long total, String label) {
    amounts.clear();
    amounts.addAll(categories.values());
    this.total = total;
    totalLabel = label;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (total <= 0) return;
    float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Ui.dp(getContext(), 77);
    oval.set(cx - r, cy - r, cx + r, cy + r);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(Ui.dp(getContext(), 27));
    float start = -90;
    for (int i = 0; i < amounts.size(); i++) {
      float angle = 360f * amounts.get(i) / total;
      paint.setColor(COLORS[i % COLORS.length]);
      if (angle > 0) canvas.drawArc(oval, start + 1, Math.max(1, angle - 2), false, paint);
      start += angle;
    }
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Ui.MUTED);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTypeface(Typeface.DEFAULT);
    paint.setTextSize(Ui.dp(getContext(), 13));
    canvas.drawText("Expenses", cx, cy - Ui.dp(getContext(), 6), paint);
    paint.setColor(Ui.BLACK);
    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    paint.setTextSize(Ui.dp(getContext(), 20));
    canvas.drawText(totalLabel, cx, cy + Ui.dp(getContext(), 20), paint);
  }
}
