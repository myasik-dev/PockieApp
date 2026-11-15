package com.pockie.app;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.*;
import java.util.Locale;

public class BankNotificationService extends NotificationListenerService {
  @Override
  public void onNotificationPosted(StatusBarNotification sbn) {
    if (!Tracking.prefs(this).getBoolean("notifications", false)
        || sbn.getPackageName().equals(getPackageName())) return;
    String pkg = sbn.getPackageName().toLowerCase(Locale.ROOT);
    // Restrict observation to banking, payment, and SMS apps.
    if (!pkg.matches(
        ".*(bank|kvb|hdfc|icici|sbi|axis|kotak|paytm|phonepe|google.android.apps.nbu.paisa|messaging|mms).*"))
      return;
    Bundle extras = sbn.getNotification().extras;
    CharSequence body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
    if (body == null) body = extras.getCharSequence(Notification.EXTRA_TEXT);
    CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE, "");
    if (body != null)
      Tracking.importMessage(
          this, title.toString(), body.toString(), sbn.getPostTime(), "Notification");
  }
}
