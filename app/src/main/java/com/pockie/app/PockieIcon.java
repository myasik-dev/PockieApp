package com.pockie.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

/** XML-inflatable version of the app's small vector icon view. */
public class PockieIcon extends Ui.Icon {
  public PockieIcon(Context context) {
    this(context, null);
  }

  public PockieIcon(Context context, AttributeSet attributes) {
    super(context, attributes);
    TypedArray values = context.obtainStyledAttributes(attributes, R.styleable.PockieIcon);
    String icon = values.getString(R.styleable.PockieIcon_iconName);
    setIconName(icon == null ? "grid" : icon);
    setIconColor(values.getColor(R.styleable.PockieIcon_iconTint, Ui.BLACK));
    values.recycle();
  }
}
