package com.pockie.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.*;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

public final class Ui {
  public static final int BLACK = Color.rgb(3, 5, 5),
      BLUE = Color.rgb(20, 139, 255),
      GREEN = Color.rgb(0, 188, 120),
      RED = Color.rgb(255, 36, 45),
      MUTED = Color.rgb(78, 87, 96),
      FIELD = Color.rgb(234, 235, 239),
      CYAN = Color.rgb(194, 241, 252);

  private Ui() {}

  public static int dp(Context c, float value) {
    return Math.round(value * c.getResources().getDisplayMetrics().density);
  }

  public static class Icon extends View {
    String name;
    int color;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Icon(Context context, AttributeSet attributes) {
      super(context, attributes);
      name = "grid";
      color = BLACK;
      setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public Icon(Context c, String name, int color) {
      super(c);
      this.name = name;
      this.color = color;
      setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setIconName(String name) {
      this.name = name;
      invalidate();
    }

    public void setIconColor(int color) {
      this.color = color;
      invalidate();
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2) {
      c.drawLine(x1, y1, x2, y2, p);
    }

    private void path(Canvas c, float... points) {
      Path a = new Path();
      a.moveTo(points[0], points[1]);
      for (int i = 2; i < points.length; i += 2) a.lineTo(points[i], points[i + 1]);
      c.drawPath(a, p);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      if (name != null && !name.isEmpty()) {
        int resId =
            getContext()
                .getResources()
                .getIdentifier(name, "drawable", getContext().getPackageName());
        if (resId != 0) {
          Drawable drawable = AppCompatResources.getDrawable(getContext(), resId);
          if (drawable != null) {
            canvas.save();
            drawable = DrawableCompat.wrap(drawable.mutate());
            DrawableCompat.setTint(drawable, color);
            drawable.setBounds(0, 0, getWidth(), getHeight());
            drawable.draw(canvas);
            canvas.restore();
            return;
          }
        }
      }
      canvas.save();
      float s = Math.min(getWidth(), getHeight()) / 24f;
      canvas.translate((getWidth() - 24 * s) / 2, (getHeight() - 24 * s) / 2);
      canvas.scale(s, s);
      p.setColor(color);
      p.setStrokeWidth(2.0f);
      p.setStrokeCap(Paint.Cap.ROUND);
      p.setStrokeJoin(Paint.Join.ROUND);
      p.setStyle(Paint.Style.FILL_AND_STROKE);
      switch (name) {
        case "back":
          p.setStyle(Paint.Style.STROKE);
          path(canvas, 15, 5, 8, 12, 15, 19);
          break;
        case "next":
          p.setStyle(Paint.Style.STROKE);
          path(canvas, 9, 7, 14, 12, 9, 17);
          break;
        case "down":
          p.setStyle(Paint.Style.STROKE);
          path(canvas, 7, 10, 12, 15, 17, 10);
          break;
        case "search":
          canvas.drawCircle(10.5f, 10.5f, 5.5f, p);
          p.setStyle(Paint.Style.STROKE);
          p.setStrokeWidth(2.5f);
          line(canvas, 15, 15, 21, 21);
          break;
        case "plus":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(10, 3, 14, 21, 2, 2, p);
          canvas.drawRoundRect(3, 10, 21, 14, 2, 2, p);
          break;
        case "close":
          p.setStyle(Paint.Style.STROKE);
          p.setStrokeWidth(2.5f);
          line(canvas, 7, 7, 17, 17);
          line(canvas, 17, 7, 7, 17);
          break;
        case "grid":
          p.setStyle(Paint.Style.FILL);
          for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
              canvas.drawRoundRect(3.5f + x * 9.5f, 3.5f + y * 9.5f, 10.5f + x * 9.5f, 10.5f + y * 9.5f, 2, 2, p);
          break;
        case "category":
          p.setStyle(Paint.Style.FILL);
          for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++) canvas.drawCircle(6.5f + x * 11, 6.5f + y * 11, 3.5f, p);
          break;
        case "home":
          p.setStyle(Paint.Style.FILL);
          Path home = new Path();
          home.moveTo(4, 10);
          home.lineTo(12, 3);
          home.lineTo(20, 10);
          home.lineTo(20, 20);
          home.lineTo(4, 20);
          home.close();
          canvas.drawPath(home, p);
          p.setColor(Color.WHITE);
          canvas.drawRoundRect(10, 14, 14, 20, 1, 1, p);
          break;
        case "transactions":
        case "notes":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(4, 3, 20, 21, 3, 3, p);
          p.setColor(Color.WHITE);
          canvas.drawRoundRect(7, 7, 17, 9, 1, 1, p);
          canvas.drawRoundRect(7, 11, 17, 13, 1, 1, p);
          canvas.drawRoundRect(7, 15, 14, 17, 1, 1, p);
          break;
        case "reports":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(3.5f, 13, 7.5f, 21, 1.5f, 1.5f, p);
          canvas.drawRoundRect(10, 9, 14, 21, 1.5f, 1.5f, p);
          canvas.drawRoundRect(16.5f, 4, 20.5f, 21, 1.5f, 1.5f, p);
          break;
        case "bell":
          p.setStyle(Paint.Style.FILL);
          Path bell = new Path();
          bell.moveTo(12, 3);
          bell.cubicTo(7, 3, 6, 9, 6, 15);
          bell.lineTo(4, 17);
          bell.lineTo(20, 17);
          bell.lineTo(18, 15);
          bell.cubicTo(18, 9, 17, 3, 12, 3);
          bell.close();
          canvas.drawPath(bell, p);
          canvas.drawCircle(12, 19.5f, 2.5f, p);
          break;
        case "food":
        case "trans_food":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(4, 3, 7, 21, 1.5f, 1.5f, p);
          canvas.drawRoundRect(13, 3, 16, 21, 1.5f, 1.5f, p);
          canvas.drawRoundRect(13, 3, 20, 10, 2, 2, p);
          break;
        case "person":
        case "trans_person":
          p.setStyle(Paint.Style.FILL);
          canvas.drawCircle(12, 7, 4.5f, p);
          Path personBody = new Path();
          personBody.moveTo(5, 21);
          personBody.cubicTo(5, 14, 19, 14, 19, 21);
          personBody.close();
          canvas.drawPath(personBody, p);
          break;
        case "shop":
        case "trans_shop":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(4, 8, 20, 21, 3, 3, p);
          Path roof = new Path();
          roof.moveTo(4, 8); roof.lineTo(6, 3); roof.lineTo(18, 3); roof.lineTo(20, 8); roof.close();
          canvas.drawPath(roof, p);
          p.setColor(Color.WHITE);
          canvas.drawCircle(12, 14, 2.5f, p);
          break;
        case "cart":
        case "trans_cart":
          p.setStyle(Paint.Style.FILL);
          Path cart = new Path();
          cart.moveTo(2, 4); cart.lineTo(6, 4); cart.lineTo(9, 15); cart.lineTo(19, 15); cart.lineTo(21, 7); cart.lineTo(6, 7); cart.close();
          canvas.drawPath(cart, p);
          canvas.drawCircle(9.5f, 19.5f, 2.2f, p);
          canvas.drawCircle(17.5f, 19.5f, 2.2f, p);
          break;
        case "mail":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(3, 5, 21, 19, 3, 3, p);
          p.setColor(Color.WHITE);
          Path mailEnv = new Path();
          mailEnv.moveTo(4, 7); mailEnv.lineTo(12, 13); mailEnv.lineTo(20, 7); mailEnv.close();
          p.setStyle(Paint.Style.STROKE);
          p.setStrokeWidth(1.8f);
          canvas.drawPath(mailEnv, p);
          break;
        case "wallet":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(3, 5, 21, 20, 3, 3, p);
          p.setColor(Color.WHITE);
          canvas.drawRoundRect(13, 10, 21, 16, 2, 2, p);
          p.setColor(color);
          canvas.drawCircle(15.5f, 13, 1.2f, p);
          break;
        case "scan":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(3, 3, 8, 8, 1, 1, p);
          canvas.drawRoundRect(16, 3, 21, 8, 1, 1, p);
          canvas.drawRoundRect(16, 16, 21, 21, 1, 1, p);
          canvas.drawRoundRect(3, 16, 8, 21, 1, 1, p);
          canvas.drawRoundRect(10, 10, 14, 14, 1, 1, p);
          break;
        case "calendar":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(3, 4, 21, 21, 3, 3, p);
          p.setColor(Color.WHITE);
          canvas.drawRect(3, 4, 21, 9, p);
          canvas.drawCircle(7, 13, 1.2f, p); canvas.drawCircle(12, 13, 1.2f, p); canvas.drawCircle(17, 13, 1.2f, p);
          canvas.drawCircle(7, 17, 1.2f, p); canvas.drawCircle(12, 17, 1.2f, p); canvas.drawCircle(17, 17, 1.2f, p);
          break;
        case "edit":
          p.setStyle(Paint.Style.FILL_AND_STROKE);
          path(canvas, 4, 17, 16, 5, 20, 9, 8, 21, 4, 21, 4, 17);
          break;
        case "delete":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(5, 7, 19, 21, 2, 2, p);
          canvas.drawRoundRect(4, 4, 20, 7, 1, 1, p);
          canvas.drawRoundRect(9, 2, 15, 5, 1, 1, p);
          p.setColor(Color.WHITE);
          canvas.drawRoundRect(8.5f, 9, 10.5f, 18, 1, 1, p);
          canvas.drawRoundRect(13.5f, 9, 15.5f, 18, 1, 1, p);
          break;
        case "lock":
          p.setStyle(Paint.Style.FILL);
          canvas.drawRoundRect(4, 9, 20, 22, 4, 4, p);
          p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.5f);
          canvas.drawArc(7, 3, 17, 13, 180, 180, false, p);
          p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE);
          canvas.drawCircle(12, 14, 2, p);
          break;
        case "bank":
          p.setStyle(Paint.Style.FILL);
          Path bankRoof = new Path(); bankRoof.moveTo(12, 2); bankRoof.lineTo(2, 8); bankRoof.lineTo(22, 8); bankRoof.close();
          canvas.drawPath(bankRoof, p);
          canvas.drawRect(3, 19, 21, 22, p);
          canvas.drawRect(4, 9, 7, 18, p);
          canvas.drawRect(9, 9, 12, 18, p);
          canvas.drawRect(14, 9, 17, 18, p);
          canvas.drawRect(19, 9, 21, 18, p);
          break;
        case "wifi":
          p.setStyle(Paint.Style.FILL);
          Path drop = new Path();
          drop.moveTo(12, 3);
          drop.cubicTo(18, 11, 20, 14, 20, 16.5f);
          drop.arcTo(4, 9, 20, 23, 0, 180, false);
          drop.cubicTo(4, 14, 6, 11, 12, 3);
          drop.close();
          canvas.drawPath(drop, p);
          break;
        case "check":
          p.setStyle(Paint.Style.STROKE);
          p.setStrokeWidth(3.0f);
          path(canvas, 4, 12, 9, 17, 20, 6);
          break;
        case "rupee":
          p.setStyle(Paint.Style.STROKE);
          p.setStrokeWidth(2.6f);
          line(canvas, 6, 5, 18, 5);
          line(canvas, 6, 9.5f, 18, 9.5f);
          path(canvas, 8, 5, 14, 5, 16, 9, 14, 13, 7, 13, 17, 21);
          break;
        case "transfer":
          p.setStyle(Paint.Style.FILL);
          Path arr1 = new Path(); arr1.moveTo(3, 8); arr1.lineTo(15, 8); arr1.lineTo(15, 5); arr1.lineTo(21, 9.5f); arr1.lineTo(15, 14); arr1.lineTo(15, 11); arr1.lineTo(3, 11); arr1.close();
          canvas.drawPath(arr1, p);
          Path arr2 = new Path(); arr2.moveTo(21, 16); arr2.lineTo(9, 16); arr2.lineTo(9, 13); arr2.lineTo(3, 17.5f); arr2.lineTo(9, 22); arr2.lineTo(9, 19); arr2.lineTo(21, 19); arr2.close();
          canvas.drawPath(arr2, p);
          break;
        default:
          p.setStyle(Paint.Style.FILL);
          canvas.drawCircle(12, 12, 8, p);
          break;
      }
      canvas.restore();
    }
  }
}
