package com.pockie.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PockieFlowTest {
  private MainActivity activity;

  @Before
  public void launch() {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    assertTrue("UI tests must use the separate QA app", c.getPackageName().endsWith(".receiptqa"));
    c.getSharedPreferences("pockie", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("onboarded", true)
        .putBoolean("sms", false)
        .putBoolean("notifications", false)
        .remove("banks")
        .putBoolean("exclude_transfer", true)
        .remove("banks")
        .putBoolean("exclude_transfer", true)
        .commit();
    try (ExpenseStore store = new ExpenseStore(c)) {
      store.getWritableDatabase().delete("transactions", null, null);
    }
    android.content.Intent intent =
        new android.content.Intent(c, MainActivity.class)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    activity =
        (MainActivity) InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
  }

  @After
  public void finish() {
    if (activity != null)
      InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> activity.finish());
  }

  @Test
  public void manualTransactionCanBeAddedEditedAndDeleted() {
    onView(withContentDescription("Add expense or income")).perform(click());
    capture("add-chooser");
    onView(withText("Add Expense & Income")).perform(click());
    capture("add-form");
    onView(withHint("Merchant / Payee (e.g. Swiggy)")).perform(replaceText("Test Coffee"));
    onView(withHint("Amount")).perform(replaceText("116.82"), closeSoftKeyboard());
    onView(withText("Save")).perform(scrollTo(), click());
    onView(withText("Test Coffee")).check(matches(isDisplayed()));
    onView(withText("Test Coffee")).perform(click());
    capture("details");
    onView(withText("✎  Edit")).perform(click());
    capture("edit");
    onView(withHint("Amount")).perform(replaceText("200.50"), closeSoftKeyboard());
    onView(withText("Save")).perform(scrollTo(), click());
    onView(withText("▣  Delete")).perform(click());
    onView(withText("DELETE")).perform(click());
    onView(withText("Your money, beautifully organized")).check(matches(isDisplayed()));
  }

  @Test
  public void sampleDataFiltersAndCategoriesWork() {
    onView(withText("Preview with sample data")).perform(scrollTo(), click());
    capture("home");
    onView(withText("See All")).perform(click());
    capture("transactions");
    onView(withContentDescription("Transactions")).check(matches(isDisplayed()));
    onView(withText("Expense")).perform(click());
    onView(withText("Swiggy")).check(matches(isDisplayed()));
    onView(withText("MSM003")).check(doesNotExist());
    onView(withContentDescription("Categories")).perform(click());
    capture("categories");
    onView(withText("Food")).perform(click());
    onView(withText("Food Transactions")).check(matches(isDisplayed()));
    onView(withText("Swiggy")).check(matches(isDisplayed()));
    onView(withText("GoDaddy")).check(doesNotExist());
    onView(withContentDescription("Reports")).perform(click());
    onView(withContentDescription("Reports")).check(matches(isDisplayed()));
    onView(withContentDescription("Previous month")).perform(click());
    capture("reports");
    onView(withContentDescription("Home")).perform(click());
    onView(withContentDescription("Open settings")).perform(click());
    onView(withText("Backup & Export")).check(matches(isDisplayed()));
    capture("settings");
  }

  @Test
  public void xmlDialogsSupportCategorySearchAndSettings() {
    onView(withContentDescription("Add expense or income")).perform(click());
    android.os.SystemClock.sleep(700);
    onView(withText("Add Expense & Income")).perform(click());
    android.os.SystemClock.sleep(700);
    onView(withContentDescription("Choose category")).perform(click());
    onView(withText("Fuel")).perform(click());
    onView(withText("Fuel")).check(matches(isDisplayed()));
    onView(withText("Cancel")).perform(scrollTo(), click());
    onView(withText("Preview with sample data")).perform(scrollTo(), click());
    onView(withContentDescription("Transactions")).perform(click());
    onView(withContentDescription("Search transactions")).perform(click());
    onView(withHint("Merchant, category or reference"))
        .perform(replaceText("Swiggy"), closeSoftKeyboard());
    onView(withText("Search")).perform(click());
    onView(withText("Swiggy")).check(matches(isDisplayed()));
    onView(withText("GoDaddy")).check(doesNotExist());
    onView(withContentDescription("Home")).perform(click());
    onView(withContentDescription("Open settings")).perform(click());
    onView(withText("Bank Filter")).perform(click());
    onView(withText("KVB")).perform(click());
    onView(withText("Save")).perform(click());
    onView(withText("KVB")).check(matches(isDisplayed()));
    onView(withText("Backup & Export")).perform(click());
    onView(withText("Save JSON backup")).check(matches(isDisplayed()));
    onView(withText("Cancel")).perform(click());
    onView(withText("Privacy & Security")).perform(click());
    onView(withText("Done")).perform(click());
  }

  @Test
  public void storagePersistsExactAmountsAndSkipsDuplicates() {
    Context c = activity;
    Transaction t =
        BankParser.parse(
            "KVB", "Rs 328 debited at SWIGGY. Ref TEST123456", System.currentTimeMillis());
    try (ExpenseStore store = new ExpenseStore(c)) {
      assertTrue(store.save(t) > 0);
      Transaction duplicate = t.copy();
      duplicate.id = 0;
      assertEquals(-1, store.save(duplicate));
    }
    try (ExpenseStore reopened = new ExpenseStore(c)) {
      assertEquals(1, reopened.all().size());
      assertEquals(32800, reopened.all().get(0).amountPaise);
      Transaction edit = reopened.all().get(0);
      edit.amountPaise = 11682;
      reopened.save(edit);
      assertEquals(11682, reopened.all().get(0).amountPaise);
      reopened.delete(edit.id);
      assertTrue(reopened.all().isEmpty());
    }
  }

  @Test
  public void onboardingCanContinueWithoutSms() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> activity.finish());
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    c.getSharedPreferences("pockie", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("onboarded", false)
        .commit();
    activity =
        (MainActivity)
            InstrumentationRegistry.getInstrumentation()
                .startActivitySync(
                    new android.content.Intent(c, MainActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    onView(withText("Get Started")).check(matches(isDisplayed()));
    capture("welcome");
    onView(withText("Get Started")).perform(click());
    onView(withText("Allow SMS Access")).check(matches(isDisplayed()));
    capture("permission");
    onView(withText("Continue without SMS")).perform(click());
    onView(withText("Recent Expense")).check(matches(isDisplayed()));
  }

  private void capture(String name) {
    android.os.SystemClock.sleep(700); // Wait for the sheet's entrance animation to settle.
    // Capture only Pockie's own window, excluding personal notification overlays and toasts.
    onView(isRoot())
        .perform(
            new androidx.test.espresso.ViewAction() {
              public org.hamcrest.Matcher<android.view.View> getConstraints() {
                return isRoot();
              }

              public String getDescription() {
                return "Capture Pockie window";
              }

              public void perform(
                  androidx.test.espresso.UiController controller, android.view.View view) {
                controller.loopMainThreadUntilIdle();
                android.graphics.Bitmap image =
                    android.graphics.Bitmap.createBitmap(
                        view.getWidth(),
                        view.getHeight(),
                        android.graphics.Bitmap.Config.ARGB_8888);
                view.draw(new android.graphics.Canvas(image));
                try (java.io.FileOutputStream out =
                    new java.io.FileOutputStream(
                        new java.io.File(activity.getExternalFilesDir(null), name + ".png"))) {
                  image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
                } catch (java.io.IOException e) {
                  throw new AssertionError(e);
                } finally {
                  image.recycle();
                }
              }
            });
  }

  @Test
  public void bundledReceiptOcrWorksWithoutInternetPermission() throws Exception {
    android.graphics.Bitmap bitmap =
        android.graphics.Bitmap.createBitmap(1000, 700, android.graphics.Bitmap.Config.ARGB_8888);
    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
    canvas.drawColor(android.graphics.Color.WHITE);
    android.graphics.Paint ink = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    ink.setColor(android.graphics.Color.BLACK);
    ink.setTextSize(54);
    ink.setTypeface(android.graphics.Typeface.MONOSPACE);
    canvas.drawText("POCKIE CAFE", 80, 100, ink);
    canvas.drawText("Coffee 200.00", 80, 220, ink);
    canvas.drawText("Tax 10.00", 80, 320, ink);
    canvas.drawText("Grand Total 210.00", 80, 460, ink);
    com.google.mlkit.vision.text.TextRecognizer recognizer =
        com.google.mlkit.vision.text.TextRecognition.getClient(
            com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);
    try {
      com.google.mlkit.vision.text.Text result =
          com.google.android.gms.tasks.Tasks.await(
              recognizer.process(com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)),
              30,
              java.util.concurrent.TimeUnit.SECONDS);
      Transaction parsed = BankParser.receipt(result.getText());
      assertEquals(21000, parsed.amountPaise);
      assertTrue(parsed.merchant.contains("POCKIE"));
    } finally {
      recognizer.close();
      bitmap.recycle();
    }
    assertEquals(
        android.content.pm.PackageManager.PERMISSION_DENIED,
        activity.checkSelfPermission(android.Manifest.permission.INTERNET));
  }
}
