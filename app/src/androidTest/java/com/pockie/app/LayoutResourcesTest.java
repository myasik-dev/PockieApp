package com.pockie.app;

import static org.junit.Assert.*;

import android.content.Context;
import android.view.*;
import android.widget.FrameLayout;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

public class LayoutResourcesTest {
  @Test
  public void allAppLayoutsInflateAndKeepTheirXmlIds() {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    assertTrue(context.getPackageName().endsWith(".receiptqa"));
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              Context themed = new ContextThemeWrapper(context, R.style.Theme_Pockie_Navigation);
              LayoutInflater inflater = LayoutInflater.from(themed);
              FrameLayout parent = new FrameLayout(themed);
              for (java.lang.reflect.Field resource : R.layout.class.getFields()) {
                try {
                  View view = inflater.inflate(resource.getInt(null), parent, false);
                  assertNotNull(resource.getName(), view);
                } catch (Exception error) {
                  throw new AssertionError("Cannot inflate " + resource.getName(), error);
                }
              }
              View activity = inflater.inflate(R.layout.activity_main, parent, false);
              assertTrue(activity.findViewById(R.id.nav_home_icon) instanceof PockieIcon);
              assertTrue(activity.findViewById(R.id.nav_categories_icon) instanceof PockieIcon);
              assertNotNull(activity.findViewById(R.id.screen_container));
            });
  }
}
