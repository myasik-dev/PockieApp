package com.pockie.app;

import android.content.*;
import java.util.*;

public final class Tracking {
  private Tracking() {}

  public static SharedPreferences prefs(Context c) {
    return c.getSharedPreferences("pockie", Context.MODE_PRIVATE);
  }

  public static boolean importMessage(
      Context c, String sender, String text, long date, String source) {
    Transaction t = BankParser.parse(sender, text, date);
    if (t == null) return false;
    Set<String> banks = prefs(c).getStringSet("banks", new HashSet<>());
    if (!banks.isEmpty()) {
      String message = (sender + " " + text).toLowerCase(Locale.ROOT);
      boolean match = false;
      for (String bank : banks)
        if (message.contains(bank.toLowerCase(Locale.ROOT))) {
          match = true;
          break;
        }
      if (!match) return false;
    }
    t.source = source;
    try (ExpenseStore store = new ExpenseStore(c)) {
      return store.save(t) > 0;
    }
  }
}
