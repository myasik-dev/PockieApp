package com.pockie.app;

import static com.pockie.app.Ui.*;

import android.Manifest;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.bottomsheet.*;
import java.io.*;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends AppCompatActivity {
  private ExpenseStore store;
  private android.content.SharedPreferences prefs;
  private FrameLayout root, body;
  private View navigation;
  private String screen = "Home", filter = "All", categoryFilter = "", query = "";
  private int onboarding = 0;
  private final Calendar reportMonth = Calendar.getInstance();
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private Uri cameraUri;
  private String pendingExport = "csv";
  private boolean registered, scanning;
  private final String[] categories = {
    "Food & Dining",
    "Groceries",
    "Transport",
    "Fuel",
    "Shopping",
    "Bills & Utilities",
    "Rent",
    "Home",
    "Health & Medical",
    "Insurance",
    "Education",
    "Business",
    "Transfers",
    "Recharge",
    "Subscriptions",
    "Travel",
    "Entertainment",
    "Personal Care",
    "Gift & Donations",
    "Investments",
    "Savings",
    "EMI & Loans",
    "Family",
    "Pets",
    "Others"
  };
  private final int[] categoryColors = {
    0xffFBF3E0, 0xffEAF8ED, 0xffEBF5FE, 0xffE8F7FF, 0xffFDF0F6,
    0xffFFF2EC, 0xffF5F6F8, 0xffFFFBEA, 0xffFFF0F0, 0xffEBF9FA,
    0xffF4EFFF, 0xffF2EFFF, 0xffFFFBE0, 0xffEAFDF8, 0xffFFF0F4,
    0xffEEFBF3, 0xffF8F2FC, 0xffFFF0F2, 0xffFFF8F5, 0xffEBF6FC,
    0xffF0FAF1, 0xffFFFDEB, 0xffFDE8F0, 0xffFAF5F0, 0xffF0F3F5
  };
  private final BroadcastReceiver refresh =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
          if (onboarding == 2) render();
        }
      };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    store = new ExpenseStore(this);
    prefs = Tracking.prefs(this);
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new androidx.activity.OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (onboarding == 1) {
                  onboarding = 0;
                  render();
                } else if (!screen.equals("Home")) {
                  go("Home");
                } else {
                  setEnabled(false);
                  getOnBackPressedDispatcher().onBackPressed();
                  setEnabled(true);
                }
              }
            });
    getWindow()
        .setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    getWindow().setNavigationBarColor(Color.WHITE);
    if (Build.VERSION.SDK_INT >= 28) {
      WindowManager.LayoutParams attributes = getWindow().getAttributes();
      attributes.layoutInDisplayCutoutMode =
          WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      getWindow().setAttributes(attributes);
    }
    if (state != null) {
      screen = state.getString("screen", "Home");
      filter = state.getString("filter", "All");
      categoryFilter = state.getString("category", "");
      query = state.getString("query", "");
      pendingExport = state.getString("export", "csv");
      String uri = state.getString("camera");
      if (uri != null) cameraUri = Uri.parse(uri);
    }
    onboarding = prefs.getBoolean("onboarded", false) ? 2 : 0;
    if (state != null) onboarding = state.getInt("onboarding", onboarding);
    setContentView(R.layout.activity_main);
    root = findViewById(R.id.app_root);
    body = findViewById(R.id.screen_container);
    navigation = findViewById(R.id.bottom_navigation);
    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          androidx.core.graphics.Insets bars =
              insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
          androidx.core.graphics.Insets ime =
              insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
          v.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
          return insets;
        });
    render();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putString("screen", screen);
    state.putString("filter", filter);
    state.putString("category", categoryFilter);
    state.putString("query", query);
    state.putInt("onboarding", onboarding);
    state.putString("export", pendingExport);
    if (cameraUri != null) state.putString("camera", cameraUri.toString());
  }

  @Override
  protected void onStart() {
    super.onStart();
    IntentFilter f = new IntentFilter(ExpenseStore.CHANGED);
    androidx.core.content.ContextCompat.registerReceiver(
        this, refresh, f, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
    registered = true;
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (root != null && onboarding == 2 && screen.equals("Settings")) render();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (registered) unregisterReceiver(refresh);
    registered = false;
  }

  @Override
  protected void onDestroy() {
    worker.shutdown();
    store.close();
    super.onDestroy();
  }

  private int d(float n) {
    return dp(this, n);
  }

  private void go(String next) {
    screen = next;
    filter = "All";
    categoryFilter = "";
    query = "";
    render();
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
  }

  private int artFor(String category) {
    if (category == null) return R.drawable.category_shopping;
    switch (category) {
      case "Food & Dining":
      case "Food":
        return R.drawable.category_food;
      case "Fuel":
        return R.drawable.category_fuel;
      case "Business":
        return R.drawable.category_business;
      case "Recharge":
        return R.drawable.category_recharge;
      case "Transfers":
        return R.drawable.category_transfers;
      default:
        return R.drawable.category_shopping;
    }
  }

  private String iconForCategory(String category) {
    if (category == null) return "category";
    switch (category) {
      case "Food & Dining":
      case "Food":
        return "food";
      case "Groceries":
        return "cart";
      case "Transport":
      case "Travel":
        return "transfer";
      case "Fuel":
        return "wifi";
      case "Shopping":
        return "shop";
      case "Bills & Utilities":
        return "transactions";
      case "Rent":
      case "Home":
        return "home";
      case "Health & Medical":
        return "plus";
      case "Insurance":
        return "lock";
      case "Education":
        return "notes";
      case "Business":
        return "bank";
      case "Transfers":
        return "transfer";
      case "Recharge":
        return "bell";
      case "Subscriptions":
        return "calendar";
      case "Entertainment":
        return "grid";
      case "Personal Care":
      case "Family":
        return "person";
      case "Gift & Donations":
        return "wallet";
      case "Investments":
      case "EMI & Loans":
        return "rupee";
      case "Savings":
        return "bank";
      default:
        return "category";
    }
  }

  private int colorFor(String category) {
    if (category != null && category.equals("Food")) return categoryColors[0];
    for (int i = 0; i < categories.length; i++)
      if (categories[i].equals(category)) return categoryColors[i];
    return categoryColors[4];
  }

  private int categoryBadgeColor(String category) {
    int main = colorFor(category);
    float[] hsv = new float[3];
    Color.colorToHSV(main, hsv);
    hsv[1] = Math.min(1.0f, hsv[1] * 1.8f + 0.15f);
    hsv[2] = Math.max(0.0f, hsv[2] * 0.90f);
    return Color.HSVToColor(hsv);
  }

  private int categoryIconColor(String category) {
    int main = colorFor(category);
    float[] hsv = new float[3];
    Color.colorToHSV(main, hsv);
    hsv[1] = Math.min(1.0f, hsv[1] * 3.2f + 0.40f);
    hsv[2] = Math.max(0.0f, hsv[2] * 0.65f);
    return Color.HSVToColor(hsv);
  }

  private String money(long paise) {
    NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
    nf.setMaximumFractionDigits(paise % 100 == 0 ? 0 : 2);
    nf.setMinimumFractionDigits(paise % 100 == 0 ? 0 : 2);
    return "₹ " + nf.format(BigDecimal.valueOf(paise, 2));
  }

  private String date(long timestamp, String pattern) {
    return new SimpleDateFormat(pattern, Locale.ENGLISH).format(new Date(timestamp));
  }

  private boolean included(Transaction t) {
    return !prefs.getBoolean("exclude_transfer", true) || !t.category.equals("Transfers");
  }

  private boolean sameMonth(long timestamp, Calendar month) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(timestamp);
    return c.get(Calendar.YEAR) == month.get(Calendar.YEAR)
        && c.get(Calendar.MONTH) == month.get(Calendar.MONTH);
  }

  private void render() {
    if (isFinishing()) return;
    body.removeAllViews();
    boolean hasNav = onboarding == 2 && !screen.equals("Settings");
    navigation.setVisibility(hasNav ? View.VISIBLE : View.GONE);
    FrameLayout.LayoutParams position = (FrameLayout.LayoutParams) body.getLayoutParams();
    position.bottomMargin = hasNav ? d(70) : 0;
    body.setLayoutParams(position);
    if (onboarding < 2) {
      renderOnboarding();
      return;
    }
    switch (screen) {
      case "Transactions":
        transactions();
        break;
      case "Categories":
        categoryScreen();
        break;
      case "Reports":
        reports();
        break;
      case "Settings":
        settings();
        break;
      default:
        home();
    }
    if (hasNav) bottomNav();
  }

  private void renderOnboarding() {
    View page =
        showLayout(onboarding == 0 ? R.layout.screen_welcome : R.layout.screen_sms_permission);
    if (onboarding == 0) {
      ImageView backeffect = page.findViewById(R.id.backeffect);
      if (backeffect != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
          ImageDecoder.Source source =
              ImageDecoder.createSource(getResources(), R.drawable.backeffect);
          Drawable drawable = ImageDecoder.decodeDrawable(source);
          backeffect.setImageDrawable(drawable);
          if (drawable instanceof AnimatedImageDrawable) {
            ((AnimatedImageDrawable) drawable).start();
          }
        } catch (IOException ignored) {
        }
      }
      startWiggleAnimation(page.findViewById(R.id.spl_img3), -8f, 8f, 1800, 0);
      startWiggleAnimation(page.findViewById(R.id.spl_img5), 8f, -8f, 2000, 200);
      startWiggleAnimation(page.findViewById(R.id.mascot_welcome), -4f, 4f, 2400, 100);
      startWiggleAnimation(page.findViewById(R.id.layout_1), 4f, 10f, 2100, 300);
      startWiggleAnimation(page.findViewById(R.id.layout_2), 10f, 4f, 1900, 150);
      startWiggleAnimation(page.findViewById(R.id.layout_3), -8f, -2f, 2200, 400);
      TextView tvTyping = page.findViewById(R.id.tv_typing);
      if (tvTyping != null) {
        startTypingEffect(
            tvTyping,
            getString(R.string.we_read_your_bank_sms_and_organize_your_spending_for_you),
            35);
      }
      action(
          page,
          R.id.get_started,
          () -> {
            onboarding = 1;
            render();
          });
    } else {
      startWiggleAnimation(page.findViewById(R.id.mascot_sms), -4f, 4f, 2400, 0);
      action(page, R.id.allow_sms, () -> requestSms(true));
      action(page, R.id.skip_sms, this::finishOnboarding);
    }
  }

  private void startWiggleAnimation(
      View view, float fromDegrees, float toDegrees, long duration, long startDelay) {
    if (view == null) return;
    ObjectAnimator animator = ObjectAnimator.ofFloat(view, "rotation", fromDegrees, toDegrees);
    animator.setDuration(duration);
    animator.setRepeatCount(ValueAnimator.INFINITE);
    animator.setRepeatMode(ValueAnimator.REVERSE);
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    if (startDelay > 0) {
      animator.setStartDelay(startDelay);
    }
    animator.start();
  }

  private void startTypingEffect(TextView textView, String fullText, long charDelay) {
    if (textView == null || fullText == null) return;
    textView.setText("");
    Handler handler = new Handler(Looper.getMainLooper());
    for (int i = 0; i <= fullText.length(); i++) {
      final int index = i;
      handler.postDelayed(() -> textView.setText(fullText.substring(0, index)), i * charDelay);
    }
  }

  private void startHandWavingAnimation(View view) {
    if (view == null) return;
    view.post(
        () -> {
          view.setPivotX(view.getWidth() > 0 ? view.getWidth() * 0.7f : 140f);
          view.setPivotY(view.getHeight() > 0 ? view.getHeight() * 0.8f : 130f);

          Keyframe k0 = Keyframe.ofFloat(0f, 0f);
          Keyframe k1 = Keyframe.ofFloat(0.12f, -14f);
          Keyframe k2 = Keyframe.ofFloat(0.24f, 14f);
          Keyframe k3 = Keyframe.ofFloat(0.36f, -10f);
          Keyframe k4 = Keyframe.ofFloat(0.48f, 10f);
          Keyframe k5 = Keyframe.ofFloat(0.60f, -4f);
          Keyframe k6 = Keyframe.ofFloat(0.70f, 0f);
          Keyframe k7 = Keyframe.ofFloat(1.0f, 0f);

          PropertyValuesHolder pvhRotation =
              PropertyValuesHolder.ofKeyframe("rotation", k0, k1, k2, k3, k4, k5, k6, k7);
          ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, pvhRotation);
          animator.setDuration(2000);
          animator.setRepeatCount(0);
          animator.setInterpolator(new AccelerateDecelerateInterpolator());
          animator.start();
        });
  }

  private void finishOnboarding() {
    prefs.edit().putBoolean("onboarded", true).apply();
    onboarding = 2;
    render();
  }

  private void requestSms(boolean onboard) {
    if (checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        && checkSelfPermission(Manifest.permission.RECEIVE_SMS)
            == PackageManager.PERMISSION_GRANTED) {
      prefs.edit().putBoolean("sms", true).apply();
      scanSms();
      requestNotificationPermission(onboard);
      return;
    }
    messageDialog(
        "Automatically track bank payments",
        "Pockie reads bank transaction messages on this phone to create income and expense records."
            + " Messages stay on your device. You can turn this off in Settings, or continue with"
            + " manual entry.",
        "Not now",
        () -> requestNotificationPermission(onboard),
        "Allow access",
        () ->
            requestPermissions(
                new String[] {Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS},
                onboard ? 101 : 102));
  }

  private void requestNotificationPermission(boolean onboard) {
    if (notificationAccess()) {
      prefs.edit().putBoolean("notifications", true).apply();
      if (onboard) finishOnboarding();
      else render();
      return;
    }
    messageDialog(
        "Notification Access",
        "Allow Pockie notification access to automatically read bank payment notifications and track expenses.",
        "Not now",
        () -> {
          if (onboard) finishOnboarding();
          else render();
        },
        "Allow Access",
        () -> {
          prefs.edit().putBoolean("notifications", true).apply();
          startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
          if (onboard) finishOnboarding();
          else render();
        });
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(request, permissions, results);
    if (request == 101 || request == 102) {
      boolean granted =
          checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
      boolean receive =
          checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
      prefs.edit().putBoolean("sms", granted && receive).apply();
      if (granted) scanSms();
      else toast("SMS access is off. You can still add expenses manually.");
      requestNotificationPermission(request == 101);
    }
  }

  private View showLayout(int layout) {
    View view = getLayoutInflater().inflate(layout, body, false);
    body.addView(view);
    return view;
  }

  private void bindText(View parent, int id, String value) {
    ((TextView) parent.findViewById(id)).setText(value);
  }

  private void action(View parent, int id, Runnable callback) {
    parent.findViewById(id).setOnClickListener(v -> callback.run());
  }

  private void toolbar(View page, String title, boolean search) {
    bindText(page, R.id.toolbar_title, title);
    action(page, R.id.toolbar_back, () -> go("Home"));
    View button = page.findViewById(R.id.toolbar_search);
    button.setVisibility(search ? View.VISIBLE : View.INVISIBLE);
    button.setOnClickListener(v -> searchDialog());
  }

  private void bottomNav() {
    int[] items = {R.id.nav_home, R.id.nav_transactions, R.id.nav_reports, R.id.nav_categories};
    int[] icons = {
      R.id.nav_home_icon,
      R.id.nav_transactions_icon,
      R.id.nav_reports_icon,
      R.id.nav_categories_icon
    };
    int[] labels = {
      R.id.nav_home_label,
      R.id.nav_transactions_label,
      R.id.nav_reports_label,
      R.id.nav_categories_label
    };
    String[] names = {"Home", "Transactions", "Reports", "Categories"};
    for (int i = 0; i < names.length; i++) {
      String name = names[i];
      boolean selected = screen.equals(name);
      int tint = selected ? BLACK : 0xffbfbfbf;
      action(navigation, items[i], () -> go(name));
      ((ImageView) navigation.findViewById(icons[i])).setColorFilter(tint);
      TextView label = navigation.findViewById(labels[i]);
      label.setTextColor(tint);
      label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }
    action(navigation, R.id.add_transaction, this::addChooser);
  }

  private void home() {
    View page = showLayout(R.layout.screen_home);
    startHandWavingAnimation(page.findViewById(R.id.mascot_home));
    action(page, R.id.open_settings, () -> go("Settings"));
    action(page, R.id.tracking_status, this::trackingStatus);
    action(page, R.id.see_all, () -> go("Transactions"));
    List<Transaction> all = store.all();
    boolean demo = !all.isEmpty() && all.stream().allMatch(t -> t.demo);
    Calendar month = Calendar.getInstance();
    if (demo) month.setTimeInMillis(all.get(0).date);
    long income = 0, expense = 0;
    for (Transaction t : all)
      if (sameMonth(t.date, month) && included(t)) {
        if (t.income) income += t.amountPaise;
        else expense += t.amountPaise;
      }
    bindText(page, R.id.home_month, demo ? "October · Sample data" : "This Month");
    bindText(page, R.id.home_spending, money(expense).replace("₹", "").trim());
    bindText(page, R.id.home_income, money(income));
    bindText(
        page, R.id.home_average, money(expense / Math.max(1, month.get(Calendar.DAY_OF_MONTH))));
    bindText(page, R.id.home_expense, money(expense));
    page.findViewById(R.id.home_recent)
        .setMinimumHeight(
            Math.max(d(200), getResources().getDisplayMetrics().heightPixels - d(420)));
    LinearLayout list = page.findViewById(R.id.recent_transactions);
    if (all.isEmpty())
      empty(
          list,
          "Your money, beautifully organized",
          "Add your first expense or scan your bank SMS.",
          true);
    else
      for (int i = 0; i < Math.min(8, all.size()); i++)
        transactionRow(list, all.get(i), false, false);
    page.findViewById(R.id.clear_demo_home).setVisibility(demo ? View.VISIBLE : View.GONE);
    action(page, R.id.clear_demo_home, this::clearDemo);
  }

  private void empty(ViewGroup parent, String title, String message, boolean demo) {
    View view = getLayoutInflater().inflate(R.layout.view_empty_state, parent, false);
    bindText(view, R.id.empty_title, title);
    bindText(view, R.id.empty_message, message);
    bindText(view, R.id.empty_action, demo ? "Preview with sample data" : "Add a transaction");
    view.findViewById(R.id.empty_action)
        .setBackgroundResource(demo ? R.drawable.bg_pill_blue : R.drawable.bg_button_black);
    action(
        view,
        R.id.empty_action,
        () -> {
          if (demo) {
            store.loadDemo();
            render();
          } else addChooser();
        });
    parent.addView(view);
  }

  private void transactionRow(ViewGroup parent, Transaction t, boolean signed, boolean detail) {
    View row = getLayoutInflater().inflate(R.layout.item_transaction, parent, false);
    bindTransactionRow(row, t, signed, detail);
    parent.addView(row);
  }

  private void bindTransactionRow(View row, Transaction t, boolean signed, boolean detail) {
    boolean godaddy = t.merchant.toLowerCase(Locale.ROOT).contains("godaddy");
    String icon =
        t.category.equals("Food")
            ? "trans_food"
            : t.category.equals("Transfers") ? "trans_person" : godaddy ? "trans_cart" : "trans_shop";
    int color =
        t.category.equals("Food")
            ? 0xffff7a00
            : t.category.equals("Transfers")
                ? (t.merchant.startsWith("Sarath") ? 0xffffad30 : 0xff35b768)
                : godaddy ? BLUE : 0xff8040ff;
    row.findViewById(R.id.transaction_badge).setBackgroundTintList(ColorStateList.valueOf(color));
    ((PockieIcon) row.findViewById(R.id.transaction_icon)).setIconName(icon);
    bindText(row, R.id.transaction_merchant, t.merchant);
    String subtitle =
        detail
            ? date(t.date, "dd MMM yyyy, hh:mm a")
            : signed
                ? (t.source.equals("SMS") || t.source.equals("Notification") || t.demo
                    ? "UPI • " + (t.reference.isEmpty() ? t.account : t.reference)
                    : t.source + " • " + t.category)
                : date(t.date, "dd MMM yyyy");
    bindText(row, R.id.transaction_subtitle, subtitle);
    TextView amount = row.findViewById(R.id.transaction_amount);
    amount.setText((signed ? (t.income ? "+ " : "- ") : "") + money(t.amountPaise));
    amount.setTextColor(signed ? (t.income ? GREEN : RED) : BLACK);
    row.setOnClickListener(detail ? null : v -> details(t));
    row.setContentDescription(
        t.merchant + ", " + (t.income ? "income " : "expense ") + money(t.amountPaise));
  }

  private void transactions() {
    View page = showLayout(R.layout.screen_transactions);
    toolbar(
        page, categoryFilter.isEmpty() ? "Transactions" : categoryFilter + " Transactions", true);

    View searchBarContainer = page.findViewById(R.id.search_bar_container);
    EditText searchInput = page.findViewById(R.id.search_input);
    View searchClear = page.findViewById(R.id.search_clear);
    View toolbarSearchBtn = page.findViewById(R.id.toolbar_search);

    if (searchInput != null) {
      if (!query.isEmpty()) {
        if (searchBarContainer != null) searchBarContainer.setVisibility(View.VISIBLE);
        searchInput.setText(query);
        searchInput.setSelection(query.length());
      }
      searchInput.addTextChangedListener(
          new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
              query = s.toString().trim();
              populateTransactionList(page);
            }

            @Override
            public void afterTextChanged(Editable s) {}
          });
    }

    if (searchClear != null) {
      searchClear.setOnClickListener(
          v -> {
            if (searchInput != null && searchInput.getText().length() > 0) {
              searchInput.setText("");
            } else {
              if (searchBarContainer != null) searchBarContainer.setVisibility(View.GONE);
              if (searchInput != null) hideKeyboard(searchInput);
            }
          });
    }

    if (toolbarSearchBtn != null) {
      toolbarSearchBtn.setOnClickListener(
          v -> {
            if (searchBarContainer != null) {
              if (searchBarContainer.getVisibility() == View.VISIBLE && query.isEmpty()) {
                searchBarContainer.setVisibility(View.GONE);
                if (searchInput != null) hideKeyboard(searchInput);
              } else {
                searchBarContainer.setVisibility(View.VISIBLE);
                if (searchInput != null) {
                  searchInput.post(
                      () -> {
                        searchInput.requestFocus();
                        showKeyboard(searchInput);
                      });
                }
              }
            }
          });
    }

    TextView category = page.findViewById(R.id.category_filter_chip);
    category.setVisibility(categoryFilter.isEmpty() ? View.GONE : View.VISIBLE);
    category.setText("Category: " + categoryFilter + "   ×");
    category.setOnClickListener(
        v -> {
          categoryFilter = "";
          render();
        });

    TextView searched = page.findViewById(R.id.search_filter_chip);
    boolean showChip =
        !query.isEmpty()
            && (searchBarContainer == null
                || searchBarContainer.getVisibility() != View.VISIBLE);
    searched.setVisibility(showChip ? View.VISIBLE : View.GONE);
    searched.setText("Search: " + query + "   ×");
    searched.setOnClickListener(
        v -> {
          query = "";
          render();
        });

    int[] ids = {R.id.filter_all, R.id.filter_expense, R.id.filter_income};
    String[] names = {"All", "Expense", "Income"};
    for (int i = 0; i < ids.length; i++) {
      String name = names[i];
      TextView tab = page.findViewById(ids[i]);
      boolean selected = filter.equals(name);
      tab.setBackgroundResource(selected ? R.drawable.bg_pill_black : R.drawable.bg_pill_outline);
      tab.setTextColor(selected ? Color.WHITE : 0xffb7b7b7);
      tab.setOnClickListener(
          v -> {
            filter = name;
            render();
          });
    }

    populateTransactionList(page);
  }

  private void populateTransactionList(View page) {
    LinearLayout list = page.findViewById(R.id.transaction_list);
    if (list == null) return;
    list.removeAllViews();
    String last = "";
    int count = 0;
    for (Transaction t : store.all()) {
      if ((filter.equals("Expense") && t.income)
          || (filter.equals("Income") && !t.income)
          || (!categoryFilter.isEmpty() && !categoryFilter.equals(t.category))) continue;
      if (!query.isEmpty()
          && !(t.merchant + " " + t.reference + " " + t.category + " " + t.account + " " + t.notes)
              .toLowerCase(Locale.ROOT)
              .contains(query.toLowerCase(Locale.ROOT))) continue;
      String day = date(t.date, "dd MMMM yyyy");
      if (!day.equals(last)) {
        TextView header =
            (TextView) getLayoutInflater().inflate(R.layout.item_date_header, list, false);
        header.setText(day);
        if (count > 0) ((LinearLayout.LayoutParams) header.getLayoutParams()).topMargin = d(18);
        list.addView(header);
        last = day;
      }
      transactionRow(list, t, true, false);
      count++;
    }
    if (count == 0)
      empty(
          list,
          "No transactions found",
          query.isEmpty()
              ? "Your transactions will appear here."
              : "Try a different merchant, category, or reference.",
          false);
  }

  private void showKeyboard(View view) {
    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }
  }

  private void hideKeyboard(View view) {
    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    if (imm != null && view != null) {
      imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
  }

  private void searchDialog() {
    View view = getLayoutInflater().inflate(R.layout.dialog_search, null);
    EditText input = view.findViewById(R.id.search_input);
    input.setText(query);
    AlertDialog dialog = xmlDialog(view);
    action(view, R.id.dialog_cancel, dialog::dismiss);
    action(
        view,
        R.id.dialog_clear,
        () -> {
          query = "";
          dialog.dismiss();
          render();
        });
    action(
        view,
        R.id.dialog_confirm,
        () -> {
          query = input.getText().toString().trim();
          screen = "Transactions";
          dialog.dismiss();
          render();
        });
    dialog.setOnShowListener(
        v -> {
          input.requestFocus();
          dialog
              .getWindow()
              .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
    dialog.show();
    styleDialog(dialog);
  }

  private void choiceDialog(
      String title, String[] options, java.util.function.IntConsumer selected) {
    View view = getLayoutInflater().inflate(R.layout.dialog_choices, null);
    bindText(view, R.id.dialog_title, title);
    AlertDialog dialog = xmlDialog(view);
    ListView choices = view.findViewById(R.id.dialog_choices);
    choices.setAdapter(
        new ArrayAdapter<>(this, R.layout.item_dialog_choice, R.id.choice_text, options));
    choices.setOnItemClickListener(
        (parent, item, index, id) -> {
          dialog.dismiss();
          selected.accept(index);
        });
    action(view, R.id.dialog_cancel, dialog::dismiss);
    dialog.show();
    styleDialog(dialog);
  }

  private AlertDialog loadingDialog(String title, String message) {
    View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
    bindText(view, R.id.dialog_title, title);
    bindText(view, R.id.dialog_message, message);
    AlertDialog dialog = xmlDialog(view);
    dialog.setCancelable(false);
    dialog.show();
    styleDialog(dialog);
    return dialog;
  }

  private void categoryChoiceDialog(
      String selectedCategory, java.util.function.Consumer<String> selected) {
    Sheet sheet =
        new Sheet(
            R.layout.bottom_sheet_choose_category,
            "Choose Category",
            "Select a category for your transaction");
    EditText search = sheet.view.findViewById(R.id.category_search);
    LinearLayout container = sheet.view.findViewById(R.id.category_list_container);

    Runnable updateList =
        () -> {
          container.removeAllViews();
          String q =
              search.getText() == null
                  ? ""
                  : search.getText().toString().trim().toLowerCase(Locale.ROOT);
          for (String cat : categories) {
            if (!q.isEmpty() && !cat.toLowerCase(Locale.ROOT).contains(q)) {
              continue;
            }
            View item =
                getLayoutInflater().inflate(R.layout.item_category_choice, container, false);
            int cardColor = colorFor(cat);
            int badgeColor = categoryBadgeColor(cat);
            int iconColor = categoryIconColor(cat);

            item.setBackgroundTintList(ColorStateList.valueOf(cardColor));
            View badge = item.findViewById(R.id.category_badge);
            badge.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
            PockieIcon icon = item.findViewById(R.id.category_icon);
            icon.setIconName(iconForCategory(cat));
            icon.setIconColor(iconColor);
            bindText(item, R.id.category_name, cat);
            View check = item.findViewById(R.id.category_check);
            boolean isSelected =
                cat.equalsIgnoreCase(selectedCategory)
                    || (cat.startsWith("Food")
                        && selectedCategory != null
                        && selectedCategory.startsWith("Food"));
            check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            item.setOnClickListener(
                v -> {
                  sheet.dialog.dismiss();
                  selected.accept(cat);
                });
            container.addView(item);
          }
        };

    updateList.run();

    search.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateList.run();
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });

    View scroll = sheet.view.findViewById(R.id.category_scroll);
    View searchContainer = sheet.view.findViewById(R.id.category_search_container);
    if (scroll != null && searchContainer != null && Build.VERSION.SDK_INT >= 23) {
      scroll.setOnScrollChangeListener(
          (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            int dy = scrollY - oldScrollY;
            if (dy > 12 && searchContainer.getVisibility() == View.VISIBLE) {
              searchContainer
                  .animate()
                  .alpha(0f)
                  .translationY(-searchContainer.getHeight())
                  .setDuration(200)
                  .withEndAction(() -> searchContainer.setVisibility(View.GONE))
                  .start();
            } else if (dy < -12 && searchContainer.getVisibility() == View.GONE) {
              searchContainer.setVisibility(View.VISIBLE);
              searchContainer
                  .animate()
                  .alpha(1f)
                  .translationY(0)
                  .setDuration(200)
                  .start();
            }
          });
    }

    sheet.dialog.getBehavior().setHideable(true);
    sheet.dialog.getBehavior().addBottomSheetCallback(
        new BottomSheetBehavior.BottomSheetCallback() {
          @Override
          public void onStateChanged(View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
              sheet.dialog.dismiss();
            }
          }

          @Override
          public void onSlide(View bottomSheet, float slideOffset) {}
        });

    sheet.dialog.show();
  }

  private void categoryScreen() {
    View page = showLayout(R.layout.screen_categories);
    toolbar(page, "Categories", false);
    GridLayout grid = page.findViewById(R.id.categories_grid);
    if (grid != null) grid.removeAllViews();
    List<Transaction> all = store.all();
    int visibleCount = 0;
    for (String name : categories) {
      long amount = 0;
      int count = 0;
      for (Transaction t : all)
        if (t.category.equalsIgnoreCase(name)
            || (name.startsWith("Food") && t.category.startsWith("Food"))) {
          amount += t.amountPaise;
          count++;
        }
      if (count > 0 && grid != null) {
        View card = getLayoutInflater().inflate(R.layout.item_category_card, grid, false);
        card.setBackgroundTintList(ColorStateList.valueOf(colorFor(name)));
        ((ImageView) card.findViewById(R.id.category_card_art)).setImageResource(artFor(name));
        bindText(card, R.id.category_card_title, name);
        bindText(card, R.id.category_card_amount, money(amount));
        bindText(card, R.id.category_card_count, "(" + count + ")");
        card.setContentDescription(name + ", " + count + " transactions");

        int row = visibleCount / 2;
        int col = visibleCount % 2;
        GridLayout.LayoutParams params =
            new GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(col, 1f));
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.setMarginEnd(col == 0 ? d(16) : 0);
        params.bottomMargin = d(16);
        grid.addView(card, params);

        card.setOnClickListener(
            v -> {
              screen = "Transactions";
              categoryFilter = name;
              filter = "All";
              query = "";
              render();
            });
        visibleCount++;
      }
    }
    View emptyView = page.findViewById(R.id.categories_empty);
    View scrollView = page.findViewById(R.id.categories_scroll);
    if (visibleCount == 0) {
      if (scrollView != null) scrollView.setVisibility(View.GONE);
      if (emptyView != null) {
        emptyView.setVisibility(View.VISIBLE);
        boolean demo = !all.isEmpty() && all.stream().allMatch(t -> t.demo);
        empty(
            (ViewGroup) emptyView,
            "No categories available",
            "Categories will appear here once you add transactions.",
            demo);
      }
    } else {
      if (scrollView != null) scrollView.setVisibility(View.VISIBLE);
      if (emptyView != null) emptyView.setVisibility(View.GONE);
    }
  }

  private final class Sheet {
    final BottomSheetDialog dialog;
    final View view;

    Sheet(int layout, String title, String subtitle) {
      dialog = new BottomSheetDialog(MainActivity.this);
      view = getLayoutInflater().inflate(layout, null);
      bindText(view, R.id.sheet_title, title);
      bindText(view, R.id.sheet_subtitle, subtitle);
      action(view, R.id.sheet_close, dialog::dismiss);
      dialog.setContentView(view);
      dialog.getBehavior().setSkipCollapsed(true);
      dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
      dialog.setOnShowListener(
          x -> {
            if (dialog.getWindow() != null) {
              dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
              if (Build.VERSION.SDK_INT >= 28) {
                WindowManager.LayoutParams a = dialog.getWindow().getAttributes();
                a.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                dialog.getWindow().setAttributes(a);
              }
            }
            FrameLayout bottom =
                dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottom != null) {
              bottom.setBackgroundResource(R.drawable.bg_sheet);
              bottom.setClipToOutline(true);
              bottom.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
            dialog
                .getBehavior()
                .setMaxHeight(getResources().getDisplayMetrics().heightPixels - d(36));
          });
      if (dialog.getWindow() != null)
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
  }

  private void addChooser() {
    Sheet sheet =
        new Sheet(
            R.layout.bottom_sheet_add_options,
            "Add or track expense",
            "Choose a quick way to add your income or\nexpense");
    action(
        sheet.view,
        R.id.scan_sms_choice,
        () -> {
          sheet.dialog.dismiss();
          scanSms();
        });
    action(
        sheet.view,
        R.id.manual_choice,
        () -> {
          sheet.dialog.dismiss();
          editor(null, null);
        });
    action(
        sheet.view,
        R.id.receipt_choice,
        () -> {
          sheet.dialog.dismiss();
          receiptOptions();
        });
    sheet.dialog.show();
  }

  private void details(Transaction t) {
    Sheet sheet =
        new Sheet(
            R.layout.bottom_sheet_transaction_details,
            "Transaction Details",
            "Choose a quick way to add your income or\nexpense");
    bindTransactionRow(sheet.view.findViewById(R.id.detail_transaction_summary), t, true, true);
    LinearLayout fields = sheet.view.findViewById(R.id.transaction_detail_fields);
    detailLine(
        fields,
        "Type",
        t.income ? "Income" : "Expense",
        t.income ? GREEN : RED,
        t.income ? 0xffddf8e8 : 0xffffe2e4);
    detailLine(fields, "Category", t.category, BLACK, colorFor(t.category));
    detailLine(
        fields, "Account", t.account.isEmpty() ? "Cash / not specified" : t.account, BLACK, 0);
    detailLine(
        fields,
        "Reference No",
        t.reference.isEmpty() ? t.source + "/" + t.id : t.reference,
        BLACK,
        0);
    if (!t.notes.isEmpty()) detailLine(fields, "Notes", t.notes, BLACK, 0);
    action(sheet.view, R.id.edit_transaction, () -> editor(t, sheet.dialog));
    action(sheet.view, R.id.delete_transaction, () -> deleteDialog(t, sheet.dialog));
    sheet.dialog.show();
  }

  private void detailLine(ViewGroup parent, String title, String value, int color, int background) {
    View row =
        getLayoutInflater()
            .inflate(
                background != 0
                    ? R.layout.item_transaction_detail_pill
                    : R.layout.item_transaction_detail,
                parent,
                false);
    bindText(row, R.id.detail_label, title);
    TextView label = row.findViewById(R.id.detail_value);
    label.setText(value);
    label.setTextColor(color);
    if (background != 0) {
      label.setBackgroundResource(R.drawable.bg_value_pill);
      label.setBackgroundTintList(ColorStateList.valueOf(background));
    }
    parent.addView(row);
  }

  private void deleteDialog(Transaction t, BottomSheetDialog details) {
    View view = getLayoutInflater().inflate(R.layout.dialog_delete_transaction, null);
    bindText(view, R.id.dialog_title, "Delete " + (t.income ? "Income" : "Expense"));
    bindText(
        view,
        R.id.dialog_message,
        "Are you sure you want to delete this " + (t.income ? "income" : "expense") + " record?");
    AlertDialog dialog = xmlDialog(view);
    action(view, R.id.dialog_cancel, dialog::dismiss);
    action(
        view,
        R.id.dialog_confirm,
        () -> {
          store.delete(t.id);
          dialog.dismiss();
          details.dismiss();
          render();
        });
    dialog.show();
    styleDialog(dialog);
  }

  private AlertDialog xmlDialog(View view) {
    return new AlertDialog.Builder(this).setView(view).create();
  }

  private void styleDialog(AlertDialog dialog) {
    if (dialog.getWindow() != null) {
      dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
      dialog
          .getWindow()
          .setLayout(
              getResources().getDisplayMetrics().widthPixels - d(48),
              ViewGroup.LayoutParams.WRAP_CONTENT);
    }
  }

  private void messageDialog(
      String title,
      String message,
      String negative,
      Runnable cancelled,
      String positive,
      Runnable confirmed) {
    View view = getLayoutInflater().inflate(R.layout.dialog_message, null);
    bindText(view, R.id.dialog_title, title);
    bindText(view, R.id.dialog_message, message);
    AlertDialog dialog = xmlDialog(view);
    View cancel = view.findViewById(R.id.dialog_cancel);
    cancel.setVisibility(negative == null ? View.GONE : View.VISIBLE);
    if (negative != null) {
      bindText(view, R.id.dialog_cancel, negative);
      cancel.setOnClickListener(
          v -> {
            dialog.dismiss();
            if (cancelled != null) cancelled.run();
          });
    }
    bindText(view, R.id.dialog_confirm, positive);
    action(
        view,
        R.id.dialog_confirm,
        () -> {
          dialog.dismiss();
          if (confirmed != null) confirmed.run();
        });
    dialog.show();
    styleDialog(dialog);
  }

  private void editor(Transaction original, BottomSheetDialog details) {
    boolean editing = original != null && original.id > 0;
    Transaction t = original == null ? new Transaction() : original.copy();
    Sheet sheet =
        new Sheet(
            editing
                ? R.layout.bottom_sheet_edit_transaction
                : R.layout.bottom_sheet_add_transaction,
            editing ? "Edit " + (t.income ? "Income" : "Expense") : "Add Expense & Income",
            "Choose a quick way to add your income or\nexpense");
    View view = sheet.view;
    if (!editing) {
      TextView expense = view.findViewById(R.id.type_expense),
          income = view.findViewById(R.id.type_income);
      Runnable paint =
          () -> {
            expense.setBackgroundResource(
                t.income ? android.R.color.transparent : R.drawable.bg_pill_blue);
            expense.setTextColor(t.income ? BLACK : Color.WHITE);
            income.setBackgroundResource(
                t.income ? R.drawable.bg_pill_blue : android.R.color.transparent);
            income.setTextColor(t.income ? Color.WHITE : BLACK);
          };
      expense.setOnClickListener(
          v -> {
            t.income = false;
            paint.run();
          });
      income.setOnClickListener(
          v -> {
            t.income = true;
            paint.run();
          });
      paint.run();
    }
    EditText merchant = view.findViewById(R.id.merchant_input),
        amount = view.findViewById(R.id.amount_input),
        notes = view.findViewById(R.id.notes_input);
    EditText account = view.findViewById(R.id.account_input),
        reference = view.findViewById(R.id.reference_input);
    merchant.setText(t.merchant);
    amount.setText(
        t.amountPaise == 0
            ? ""
            : BigDecimal.valueOf(t.amountPaise, 2).stripTrailingZeros().toPlainString());
    notes.setText(t.notes);
    if (account != null) account.setText(t.account);
    if (reference != null) reference.setText(t.reference);
    TextView selectedDate = view.findViewById(R.id.selected_date);
    selectedDate.setText(t.date > 0 ? date(t.date, "dd MMM yyyy") : "Date");
    selectedDate.setTextColor(t.date > 0 ? BLACK : 0xffa6a8ae);
    action(
        view,
        R.id.date_selector,
        () -> {
          Calendar initial = Calendar.getInstance();
          initial.setTimeInMillis(t.date > 0 ? t.date : System.currentTimeMillis());
          new DatePickerDialog(
                  this,
                  (picker, year, month, day) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.setTimeInMillis(t.date > 0 ? t.date : System.currentTimeMillis());
                    chosen.set(year, month, day);
                    t.date = chosen.getTimeInMillis();
                    selectedDate.setText(date(t.date, "dd MMM yyyy"));
                    selectedDate.setTextColor(BLACK);
                  },
                  initial.get(Calendar.YEAR),
                  initial.get(Calendar.MONTH),
                  initial.get(Calendar.DAY_OF_MONTH))
              .show();
        });
    TextView selectedCategory = view.findViewById(R.id.selected_category);
    selectedCategory.setText(t.category);
    action(
        view,
        R.id.category_selector,
        () ->
            categoryChoiceDialog(
                t.category,
                cat -> {
                  t.category = cat;
                  selectedCategory.setText(t.category);
                }));
    TextView hint = view.findViewById(R.id.receipt_review_hint);
    hint.setVisibility(t.source.equals("Receipt") ? View.VISIBLE : View.GONE);
    if (t.source.equals("Receipt"))
      hint.setText(
          t.amountPaise == 0
              ? "Couldn’t identify the receipt total. Enter the amount and review the other fields"
                  + " before saving."
              : "Review the scanned merchant, total, and date before saving.");
    action(view, R.id.cancel_transaction, sheet.dialog::dismiss);
    action(
        view,
        R.id.save_transaction,
        () -> {
          String name = merchant.getText().toString().trim();
          long value = BankParser.paise(amount.getText().toString().trim());
          if (name.isEmpty()) {
            merchant.setError("Enter a merchant or payee");
            merchant.requestFocus();
            return;
          }
          if (value <= 0 || value > 999999999999L) {
            amount.setError("Enter a valid amount with up to 2 decimal places");
            amount.requestFocus();
            return;
          }
          if (t.date <= 0) {
            toast("Choose the receipt date before saving.");
            return;
          }
          t.merchant = name;
          t.amountPaise = value;
          t.notes = notes.getText().toString().trim();
          if (reference != null) t.reference = reference.getText().toString().trim();
          if (account != null) t.account = account.getText().toString().trim();
          if (store.save(t) <= 0) {
            toast("This transaction already exists.");
            return;
          }
          ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
              .hideSoftInputFromWindow(merchant.getWindowToken(), 0);
          sheet.dialog.dismiss();
          if (details != null) {
            details.dismiss();
            details(t);
          }
          render();
          toast(editing ? "Transaction updated" : "Transaction saved");
        });
    sheet.dialog.show();
  }

  private void settings() {
    View page = showLayout(R.layout.screen_settings);
    toolbar(page, "Settings", false);
    boolean sms =
        checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED
            && prefs.getBoolean("sms", false);
    boolean notifications = notificationAccess() && prefs.getBoolean("notifications", false);
    bindText(
        page, R.id.tracking_title, sms || notifications ? "All Set 🎉" : "Make tracking automatic");
    bindText(
        page,
        R.id.tracking_description,
        sms || notifications
            ? "Your tracking is active"
            : "Enable access to track\nbank transactions");
    settingSwitch(
        page.findViewById(R.id.setting_sms_switch),
        sms,
        enabled -> {
          if (enabled) requestSms(false);
          else {
            prefs.edit().putBoolean("sms", false).apply();
            render();
          }
        });
    settingSwitch(
        page.findViewById(R.id.setting_notifications_switch),
        notifications,
        enabled -> {
          if (enabled) {
            prefs.edit().putBoolean("notifications", true).apply();
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
          } else {
            prefs.edit().putBoolean("notifications", false).apply();
            render();
          }
        });
    Set<String> banks = prefs.getStringSet("banks", new HashSet<>());
    bindText(
        page.findViewById(R.id.setting_banks),
        R.id.setting_banks_subtitle,
        banks.isEmpty() ? "All banks" : String.join(", ", banks));
    action(page, R.id.setting_banks, this::bankFilter);
    settingSwitch(
        page.findViewById(R.id.setting_transfers_switch),
        prefs.getBoolean("exclude_transfer", true),
        enabled -> {
          prefs.edit().putBoolean("exclude_transfer", enabled).apply();
          render();
        });
    action(page, R.id.setting_backup, this::backupOptions);
    action(
        page,
        R.id.setting_privacy,
        () ->
            messageDialog(
                "Privacy & Security",
                "Your transaction database is stored in Pockie’s private app storage. Pockie does"
                    + " not upload your messages or transactions. Device backups are disabled."
                    + " Receipt text is recognized on your device.\n\n"
                    + "Only transaction messages are saved; OTPs, failed payments and promotions"
                    + " are ignored. You control SMS and notification access in Android Settings."
                    + " Exported files are saved only where you choose.",
                null,
                null,
                "Done",
                null));
    action(
        page,
        R.id.setting_about,
        () ->
            messageDialog(
                "Pockie 1.0.2",
                "A little friend for your everyday expenses.\n\n"
                    + "Built with native Android XML layouts and Java. Track income, expenses and"
                    + " categories, scan receipts, and keep your records offline.",
                null,
                null,
                "Done",
                null));
    page.findViewById(R.id.setting_clear_demo)
        .setVisibility(store.all().stream().anyMatch(t -> t.demo) ? View.VISIBLE : View.GONE);
    action(page, R.id.setting_clear_demo, this::clearDemo);
  }

  private void settingSwitch(
      androidx.appcompat.widget.SwitchCompat toggle,
      boolean checked,
      java.util.function.Consumer<Boolean> action) {
    toggle.setChecked(checked);
    toggle.setOnCheckedChangeListener((button, value) -> action.accept(value));
  }

  private boolean notificationAccess() {
    String enabled =
        Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
    return enabled != null && enabled.contains(getPackageName() + "/");
  }

  private void bankFilter() {
    String[] banks = {
      "KVB",
      "SBI",
      "HDFC",
      "ICICI",
      "AXIS",
      "KOTAK",
      "IDFC",
      "PNB",
      "BOB",
      "CANARA",
      "INDIANBANK",
      "UNION"
    };
    Set<String> chosen = new HashSet<>(prefs.getStringSet("banks", new HashSet<>()));
    View view = getLayoutInflater().inflate(R.layout.dialog_bank_filter, null);
    AlertDialog dialog = xmlDialog(view);
    ListView list = view.findViewById(R.id.bank_choices);
    list.setAdapter(
        new ArrayAdapter<>(this, R.layout.item_bank_choice, R.id.bank_choice_label, banks));
    for (int i = 0; i < banks.length; i++) list.setItemChecked(i, chosen.contains(banks[i]));
    list.setOnItemClickListener(
        (parent, item, index, id) -> {
          if (list.isItemChecked(index)) chosen.add(banks[index]);
          else chosen.remove(banks[index]);
        });
    action(view, R.id.dialog_cancel, dialog::dismiss);
    action(
        view,
        R.id.all_banks,
        () -> {
          prefs.edit().remove("banks").apply();
          dialog.dismiss();
          render();
        });
    action(
        view,
        R.id.dialog_confirm,
        () -> {
          prefs.edit().putStringSet("banks", chosen).apply();
          dialog.dismiss();
          render();
        });
    dialog.show();
    styleDialog(dialog);
  }

  private void trackingStatus() {
    boolean active =
        (prefs.getBoolean("sms", false)
                && checkSelfPermission(Manifest.permission.READ_SMS)
                    == PackageManager.PERMISSION_GRANTED)
            || (prefs.getBoolean("notifications", false) && notificationAccess());
    messageDialog(
        active ? "Tracking is enabled" : "Track your expenses automatically",
        "Manage SMS and notification access, choose your banks, and control internal transfers in"
            + " Settings.",
        "Done",
        null,
        "Open Settings",
        () -> go("Settings"));
  }

  private void clearDemo() {
    messageDialog(
        "Clear sample data?",
        "Only the preview transactions will be removed. Your own transactions will stay.",
        "Cancel",
        null,
        "Clear samples",
        () -> {
          store.removeDemo();
          render();
        });
  }

  private void scanSms() {
    if (scanning) return;
    if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
      requestSms(false);
      return;
    }
    scanning = true;
    AlertDialog progress = loadingDialog("Scan SMS", "Looking for bank transactions…");
    worker.execute(
        () -> {
          int found = 0;
          try (Cursor cursor =
              getContentResolver()
                  .query(
                      Telephony.Sms.Inbox.CONTENT_URI,
                      new String[] {"address", "body", "date"},
                      null,
                      null,
                      "date DESC")) {
            if (cursor != null)
              while (cursor.moveToNext()) {
                if (Tracking.importMessage(
                    this, cursor.getString(0), cursor.getString(1), cursor.getLong(2), "SMS"))
                  found++;
              }
            int count = found;
            runOnUiThread(
                () -> {
                  scanning = false;
                  if (!isFinishing()) {
                    progress.dismiss();
                    render();
                    toast(
                        count == 0
                            ? "No new bank transactions found."
                            : count + " bank transactions added.");
                  }
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  scanning = false;
                  if (!isFinishing()) {
                    progress.dismiss();
                    toast("Unable to scan SMS. Check SMS access in Android Settings.");
                  }
                });
          }
        });
  }

  private void receiptOptions() {
    choiceDialog(
        "Scan Receipts",
        new String[] {"Take a photo", "Choose receipt image"},
        which -> {
          if (which == 0) camera();
          else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 201);
          }
        });
  }

  private void camera() {
    try {
      File dir = new File(getCacheDir(), "receipts");
      dir.mkdirs();
      File photo = File.createTempFile("receipt-", ".jpg", dir);
      cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".files", photo);
      Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
      intent.addFlags(
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
      intent.setClipData(ClipData.newRawUri("Receipt photo", cameraUri));
      startActivityForResult(intent, 202);
    } catch (Exception e) {
      toast("No camera app is available. Choose a receipt image instead.");
    }
  }

  private void recognize(Uri uri) {
    AlertDialog progress = loadingDialog("Scan Receipt", "Reading your receipt on this device…");
    ReceiptScanner.scan(this, uri)
        .addOnSuccessListener(
            this,
            draft -> {
              progress.dismiss();
              editor(draft, null);
            })
        .addOnFailureListener(
            this,
            e -> {
              progress.dismiss();
              toast(
                  "Could not read this receipt. Try a clearer image or add the expense manually.");
            });
  }

  private void backupOptions() {
    choiceDialog(
        "Backup & Export",
        new String[] {"Export CSV spreadsheet", "Save JSON backup", "Restore JSON backup"},
        which -> {
          if (which == 2) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 302);
          } else {
            pendingExport = which == 0 ? "csv" : "json";
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(which == 0 ? "text/csv" : "application/json");
            intent.putExtra(
                Intent.EXTRA_TITLE,
                "Pockie-" + date(System.currentTimeMillis(), "yyyy-MM-dd") + "." + pendingExport);
            startActivityForResult(intent, 301);
          }
        });
  }

  private String backup() throws JSONException {
    JSONObject root = new JSONObject();
    root.put("version", 1);
    JSONArray items = new JSONArray();
    for (Transaction t : store.all()) {
      JSONObject o = new JSONObject();
      o.put("merchant", t.merchant);
      o.put("amountPaise", t.amountPaise);
      o.put("date", t.date);
      o.put("income", t.income);
      o.put("category", t.category);
      o.put("account", t.account);
      o.put("reference", t.reference);
      o.put("notes", t.notes);
      o.put("source", t.source);
      o.put("demo", t.demo);
      o.put(
          "fingerprint",
          t.fingerprint.isEmpty()
              ? "backup:" + t.date + ":" + t.amountPaise + ":" + t.income + ":" + t.merchant
              : t.fingerprint);
      items.put(o);
    }
    root.put("transactions", items);
    return root.toString(2);
  }

  private void export(Uri uri) {
    try {
      String output = pendingExport.equals("csv") ? ExpenseStore.csv(store.all()) : backup();
      try (OutputStream stream = getContentResolver().openOutputStream(uri, "wt")) {
        if (stream == null) throw new IOException();
        stream.write(output.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      toast("Export saved.");
    } catch (Exception e) {
      toast("Unable to save export. Try another location.");
    }
  }

  private void restore(Uri uri) {
    worker.execute(
        () -> {
          try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IOException();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) != -1) {
              if (bytes.size() + read > 10 * 1024 * 1024) throw new IOException();
              bytes.write(chunk, 0, read);
            }
            JSONObject root = new JSONObject(bytes.toString("UTF-8"));
            if (root.getInt("version") != 1) throw new JSONException("Unsupported version");
            JSONArray rows = root.getJSONArray("transactions");
            List<Transaction> drafts = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
              JSONObject o = rows.getJSONObject(i);
              Transaction t = new Transaction();
              t.merchant = o.getString("merchant");
              t.amountPaise = o.getLong("amountPaise");
              t.date = o.getLong("date");
              t.income = o.getBoolean("income");
              t.category = o.getString("category");
              t.account = o.optString("account");
              t.reference = o.optString("reference");
              t.notes = o.optString("notes");
              t.source = o.optString("source", "Backup");
              t.demo = o.optBoolean("demo");
              t.fingerprint = o.getString("fingerprint");
              if (t.merchant.trim().isEmpty()
                  || t.amountPaise <= 0
                  || t.amountPaise > 999999999999L
                  || t.date <= 0
                  || !Arrays.asList(categories).contains(t.category)
                  || t.fingerprint.isEmpty()) throw new JSONException("Invalid transaction");
              drafts.add(t);
            }
            runOnUiThread(
                () ->
                    messageDialog(
                        "Restore backup?",
                        "Merge "
                            + drafts.size()
                            + " transactions with your current records? Existing records are kept"
                            + " and matching backup records are skipped.",
                        "Cancel",
                        null,
                        "Restore",
                        () -> mergeBackup(drafts)));
          } catch (Exception e) {
            runOnUiThread(
                () -> toast("This is not a valid Pockie backup. No records were changed."));
          }
        });
  }

  private void mergeBackup(List<Transaction> drafts) {
    int added = 0;
    android.database.sqlite.SQLiteDatabase db = store.getWritableDatabase();
    db.beginTransaction();
    try {
      for (Transaction transaction : drafts) if (store.save(transaction) > 0) added++;
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    render();
    toast(added + " transactions restored.");
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK) return;
    if (request == 202 && cameraUri != null) recognize(cameraUri);
    else if (data != null && data.getData() != null) {
      Uri uri = data.getData();
      if (request == 201) recognize(uri);
      else if (request == 301) export(uri);
      else if (request == 302) restore(uri);
    }
  }

  private void reports() {
    View page = showLayout(R.layout.screen_reports);
    toolbar(page, "Reports", false);
    bindText(page, R.id.report_month, date(reportMonth.getTimeInMillis(), "MMMM yyyy"));
    action(
        page,
        R.id.previous_month,
        () -> {
          reportMonth.add(Calendar.MONTH, -1);
          render();
        });
    action(
        page,
        R.id.next_month,
        () -> {
          reportMonth.add(Calendar.MONTH, 1);
          render();
        });
    long expense = 0, income = 0;
    Map<String, Long> sums = new LinkedHashMap<>();
    for (String name : categories) sums.put(name, 0L);
    for (Transaction t : store.all())
      if (sameMonth(t.date, reportMonth) && included(t)) {
        if (t.income) income += t.amountPaise;
        else {
          expense += t.amountPaise;
          sums.put(t.category, sums.getOrDefault(t.category, 0L) + t.amountPaise);
        }
      }
    bindText(page, R.id.report_spending, money(expense));
    bindText(page, R.id.report_income, "Income  " + money(income));
    page.findViewById(R.id.spending_chart).setVisibility(expense > 0 ? View.VISIBLE : View.GONE);
    page.findViewById(R.id.report_category_heading)
        .setVisibility(expense > 0 ? View.VISIBLE : View.GONE);
    page.findViewById(R.id.report_transfer_note)
        .setVisibility(expense > 0 ? View.VISIBLE : View.GONE);
    LinearLayout list = page.findViewById(R.id.report_categories);
    if (expense == 0) {
      empty(
          page.findViewById(R.id.report_empty),
          "A fresh month",
          "Your spending report will grow as you add expenses.",
          false);
      return;
    }
    ((SpendingChartView) page.findViewById(R.id.spending_chart))
        .setData(sums, expense, money(expense));
    for (String name : categories) {
      long amount = sums.get(name);
      if (amount == 0) continue;
      View row = getLayoutInflater().inflate(R.layout.item_report_category, list, false);
      row.findViewById(R.id.report_category_badge)
          .setBackgroundTintList(ColorStateList.valueOf(colorFor(name)));
      ((ImageView) row.findViewById(R.id.report_category_art)).setImageResource(artFor(name));
      bindText(row, R.id.report_category_name, name);
      bindText(
          row,
          R.id.report_category_amount,
          money(amount) + "  ·  " + Math.round(100.0 * amount / expense) + "%");
      row.setOnClickListener(
          v -> {
            screen = "Transactions";
            categoryFilter = name;
            filter = "Expense";
            render();
          });
      list.addView(row);
    }
    bindText(
        page,
        R.id.report_transfer_note,
        prefs.getBoolean("exclude_transfer", true)
            ? "Internal transfers are excluded from totals."
            : "Internal transfers are included in totals.");
  }
}
