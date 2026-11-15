package com.pockie.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;

/** Runs exclusively against the separate receiptQa application; does not modify expense records. */
@RunWith(AndroidJUnit4.class)
public class ReceiptRegressionTest {
  @Test
  public void providedChennaiSilksReceiptFindsTotalWithoutCurrencySymbol() throws Exception {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    assertTrue(c.getPackageName().endsWith(".receiptqa"));
    File image = new File(c.getExternalFilesDir(null), "chennai-silks-receipt.png");
    if ("true".equals(InstrumentationRegistry.getArguments().getString("requireReceiptFixture")))
      assertTrue("Receipt fixture must be supplied to the QA app", image.exists());
    else Assume.assumeTrue(image.exists());
    Transaction t = Tasks.await(ReceiptScanner.scan(c, Uri.fromFile(image)), 45, TimeUnit.SECONDS);
    try (FileWriter out =
        new FileWriter(new File(c.getExternalFilesDir(null), "chennai-silks-ocr.txt"))) {
      out.write(t.notes);
    }
    assertEquals(313500, t.amountPaise);
    assertEquals("Shopping", t.category);
  }

  @Test
  public void providedZudioReceiptFillsAllFields() throws Exception {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    assertTrue(c.getPackageName().endsWith(".receiptqa"));
    File file = new File(c.getExternalFilesDir(null), "zudio-receipt.png");
    if ("true".equals(InstrumentationRegistry.getArguments().getString("requireReceiptFixture")))
      assertTrue("Receipt fixture must be supplied to the QA app", file.exists());
    else Assume.assumeTrue(file.exists());
    Transaction t = Tasks.await(ReceiptScanner.scan(c, Uri.fromFile(file)), 45, TimeUnit.SECONDS);
    try (FileWriter out = new FileWriter(new File(c.getExternalFilesDir(null), "zudio-ocr.txt"))) {
      out.write(t.notes);
    }
    System.out.println(
        "Zudio result: merchant="
            + t.merchant
            + ", paise="
            + t.amountPaise
            + ", category="
            + t.category
            + ", reference="
            + t.reference
            + ", date="
            + new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(t.date)));
    assertEquals("Zudio", t.merchant);
    assertEquals("Shopping", t.category);
    assertEquals(139700, t.amountPaise);
    Calendar date = Calendar.getInstance();
    date.setTimeInMillis(t.date);
    assertEquals(2025, date.get(Calendar.YEAR));
    assertEquals(Calendar.AUGUST, date.get(Calendar.MONTH));
    assertEquals(24, date.get(Calendar.DAY_OF_MONTH));
    assertEquals("Z258100610006", t.reference);
    c.getSharedPreferences("pockie", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("onboarded", true)
        .commit();
    MainActivity activity =
        (MainActivity)
            InstrumentationRegistry.getInstrumentation()
                .startActivitySync(
                    new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              () -> {
                try {
                  java.lang.reflect.Method editor =
                      MainActivity.class.getDeclaredMethod(
                          "editor",
                          Transaction.class,
                          com.google.android.material.bottomsheet.BottomSheetDialog.class);
                  editor.setAccessible(true);
                  editor.invoke(activity, t, null);
                } catch (Exception e) {
                  throw new AssertionError(e);
                }
              });
      android.os.SystemClock.sleep(700);
      onView(withHint("Merchant / Payee (e.g. Swiggy)")).check(matches(withText("Zudio")));
      onView(withHint("Amount")).check(matches(withText("1397")));
      onView(withText("Shopping")).check(matches(isDisplayed()));
      onView(withText("24 Aug 2025")).check(matches(isDisplayed()));
      onView(withHint("Reference No (optional)")).check(matches(withText("Z258100610006")));
      onView(isRoot())
          .perform(
              new androidx.test.espresso.ViewAction() {
                public org.hamcrest.Matcher<android.view.View> getConstraints() {
                  return isRoot();
                }

                public String getDescription() {
                  return "Capture receipt review";
                }

                public void perform(
                    androidx.test.espresso.UiController controller, android.view.View view) {
                  Bitmap image =
                      Bitmap.createBitmap(
                          view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
                  view.draw(new Canvas(image));
                  try (FileOutputStream out =
                      new FileOutputStream(
                          new File(c.getExternalFilesDir(null), "zudio-review.png"))) {
                    image.compress(Bitmap.CompressFormat.PNG, 100, out);
                  } catch (IOException e) {
                    throw new AssertionError(e);
                  } finally {
                    image.recycle();
                  }
                }
              });
    } finally {
      InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);
    }
  }

  @Test
  public void invoiceHeadingDoesNotMakeAnOcrReceiptBusiness() throws Exception {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Bitmap bitmap = Bitmap.createBitmap(600, 850, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(Color.WHITE);
    Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    ink.setColor(Color.BLACK);
    ink.setTextSize(30);
    ink.setTypeface(Typeface.MONOSPACE);
    canvas.drawText("ZUDIO", 35, 70, ink);
    canvas.drawText("TAX INVOICE", 35, 160, ink);
    canvas.drawText("Invoice No: Z258100610006", 35, 220, ink);
    canvas.drawText("Date: 2025-08-24", 35, 280, ink);
    canvas.drawText("Womenswear Shirt", 35, 400, ink);
    canvas.drawText("Total Invoice Amount:", 35, 650, ink);
    canvas.drawText("1,397.00", 380, 695, ink);
    File image = new File(c.getCacheDir(), "synthetic-retail.png");
    try (FileOutputStream out = new FileOutputStream(image)) {
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
    } finally {
      bitmap.recycle();
    }
    Transaction t = Tasks.await(ReceiptScanner.scan(c, Uri.fromFile(image)), 45, TimeUnit.SECONDS);
    assertEquals(139700, t.amountPaise);
    assertEquals("Shopping", t.category);
    assertEquals("Zudio", t.merchant);
  }
}
