package com.pockie.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExpenseStore extends SQLiteOpenHelper {
  public static final String CHANGED = "com.pockie.app.CHANGED";
  private final Context context;

  public ExpenseStore(Context context) {
    super(context, "pockie.db", null, 1);
    this.context = context.getApplicationContext();
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, merchant TEXT NOT NULL,"
            + " amount INTEGER NOT NULL, date INTEGER NOT NULL, income INTEGER NOT NULL, category"
            + " TEXT, account TEXT, reference TEXT, notes TEXT, source TEXT, fingerprint TEXT"
            + " UNIQUE, demo INTEGER NOT NULL DEFAULT 0)");
    db.execSQL("CREATE INDEX tx_date ON transactions(date DESC)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

  public long save(Transaction t) {
    if (t.id == 0 && t.fingerprint.isEmpty()) t.fingerprint = "local:" + UUID.randomUUID();
    ContentValues v = new ContentValues();
    v.put("merchant", t.merchant);
    v.put("amount", t.amountPaise);
    v.put("date", t.date);
    v.put("income", t.income ? 1 : 0);
    v.put("category", t.category);
    v.put("account", t.account);
    v.put("reference", t.reference);
    v.put("notes", t.notes);
    v.put("source", t.source);
    v.put("demo", t.demo ? 1 : 0);
    if (t.fingerprint.isEmpty()) v.putNull("fingerprint");
    else v.put("fingerprint", t.fingerprint);
    long result;
    if (t.id == 0) {
      result =
          getWritableDatabase()
              .insertWithOnConflict("transactions", null, v, SQLiteDatabase.CONFLICT_IGNORE);
      if (result > 0) t.id = result;
    } else
      result =
          getWritableDatabase()
              .update("transactions", v, "id=?", new String[] {String.valueOf(t.id)});
    if (result > 0) context.sendBroadcast(new Intent(CHANGED).setPackage(context.getPackageName()));
    return result;
  }

  public List<Transaction> all() {
    List<Transaction> result = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .query("transactions", null, null, null, null, null, "date DESC, id ASC")) {
      while (c.moveToNext()) {
        Transaction t = new Transaction();
        t.id = c.getLong(c.getColumnIndexOrThrow("id"));
        t.merchant = str(c, "merchant");
        t.amountPaise = c.getLong(c.getColumnIndexOrThrow("amount"));
        t.date = c.getLong(c.getColumnIndexOrThrow("date"));
        t.income = c.getInt(c.getColumnIndexOrThrow("income")) == 1;
        t.category = str(c, "category");
        t.account = str(c, "account");
        t.reference = str(c, "reference");
        t.notes = str(c, "notes");
        t.source = str(c, "source");
        t.fingerprint = str(c, "fingerprint");
        t.demo = c.getInt(c.getColumnIndexOrThrow("demo")) == 1;
        result.add(t);
      }
    }
    return result;
  }

  private String str(Cursor c, String col) {
    String s = c.getString(c.getColumnIndexOrThrow(col));
    return s == null ? "" : s;
  }

  public void delete(long id) {
    getWritableDatabase().delete("transactions", "id=?", new String[] {String.valueOf(id)});
    context.sendBroadcast(new Intent(CHANGED).setPackage(context.getPackageName()));
  }

  public void removeDemo() {
    getWritableDatabase().delete("transactions", "demo=1", null);
  }

  public void loadDemo() {
    if (!all().isEmpty()) return;
    String[] names = {"Swiggy", "MSM003", "Mr. R Rajkumar", "SarathKumar D", "GoDaddy"};
    long[] amounts = {32800, 1200000, 70000, 12000, 11682};
    String[] categories = {"Food & Dining", "Business", "Transfers", "Transfers", "Business"};
    for (int i = 0; i < 5; i++) {
      Transaction t = new Transaction();
      t.merchant = names[i];
      t.amountPaise = amounts[i];
      t.income = i >= 1 && i <= 3;
      t.category = categories[i];
      Calendar cal = Calendar.getInstance();
      cal.set(2026, Calendar.OCTOBER, i < 2 ? 26 : 25, 10, 52, 0);
      cal.set(Calendar.MILLISECOND, 0);
      t.date = cal.getTimeInMillis();
      t.account = "xxxx5083 (KVB)";
      t.reference = "PAA1254756333211";
      t.demo = true;
      save(t);
    }
  }

  public static String csv(List<Transaction> data) {
    StringBuilder b =
        new StringBuilder(
            "Date,Merchant,Type,Amount (INR),Category,Account,Reference,Notes,Source\r\n");
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    for (Transaction t : data)
      b.append(quote(df.format(new Date(t.date))))
          .append(',')
          .append(quote(t.merchant))
          .append(',')
          .append(t.income ? "Income" : "Expense")
          .append(',')
          .append(java.math.BigDecimal.valueOf(t.amountPaise, 2).toPlainString())
          .append(',')
          .append(quote(t.category))
          .append(',')
          .append(quote(t.account))
          .append(',')
          .append(quote(t.reference))
          .append(',')
          .append(quote(t.notes))
          .append(',')
          .append(quote(t.source))
          .append("\r\n");
    return b.toString();
  }

  private static String quote(String s) {
    if (s.matches("^[=+@-].*")) s = "'" + s;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }
}
