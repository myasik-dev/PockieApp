package com.pockie.app;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative parser: ignores OTPs, promotions, balances and failed payments. */
public final class BankParser {
  private static final Pattern MONEY =
      Pattern.compile("(?i)(?:INR|Rs\\.?|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)");
  private static final Pattern ACTION =
      Pattern.compile("(?i)\\b(debited|credited|spent|paid|received|withdrawn|sent)\\b");

  private BankParser() {}

  public static Transaction parse(String sender, String body, long date) {
    if (body == null) return null;
    String lower = body.toLowerCase(Locale.ROOT);
    if (lower.matches(
        "(?s).*\\b(otp|one.time.password|failed|declined|unsuccessful|will be debited|request to"
            + " pay)\\b.*")) return null;
    Matcher action = ACTION.matcher(body);
    if (!action.find()) return null;
    String verb = action.group(1).toLowerCase(Locale.ROOT);
    int actionStart = action.start(), actionEnd = action.end();
    Matcher money = MONEY.matcher(body);
    long amount = 0;
    int best = Integer.MAX_VALUE;
    while (money.find()) {
      String prefix = lower.substring(Math.max(0, money.start() - 24), money.start());
      if (prefix.matches("(?s).*(?:bal(?:ance)?|limit|outstanding)[: .]*$")) continue;
      int distance =
          money.end() < actionStart
              ? actionStart - money.end()
              : Math.abs(money.start() - actionEnd);
      if (distance < best) {
        amount = paise(money.group(1));
        best = distance;
      }
    }
    if (amount <= 0) return null;
    Transaction t = new Transaction();
    t.amountPaise = amount;
    t.date = date;
    t.income = verb.equals("credited") || verb.equals("received");
    t.merchant = sender == null || sender.isEmpty() ? "Bank transaction" : sender;
    Matcher merchant =
        Pattern.compile(
                "(?i)(?:\\bto\\b|\\bat\\b|\\bfrom\\b)\\s+([A-Za-z][A-Za-z0-9"
                    + " &@_-]{1,45}?)(?=\\s+(?:on|via|ref|using|UPI|a/c|ac|avl|bal|has|is|from|to)\\b|[.;]|$)")
            .matcher(body);
    while (merchant.find()) {
      String name =
          merchant
              .group(1)
              .split("(?i)\\s+(?:on|via|ref|using|UPI|a/c|ac|avl|bal|has|is)\\b")[0]
              .trim();
      if (!name.matches("(?i).*(?:account|your|bank|INR).*")) {
        t.merchant = name;
        break;
      }
    }
    Matcher account =
        Pattern.compile("(?i)(?:a/c|ac(?:count)?|card)[ .:*xX-]*([xX*]*[0-9]{3,6})").matcher(body);
    if (account.find()) t.account = account.group(1);
    Matcher ref =
        Pattern.compile(
                "(?i)(?:ref(?:erence)?(?:\\s*no)?|UPI ref|UTR|txn(?:\\s*id)?)["
                    + " :.#-]*([A-Za-z0-9]{6,})")
            .matcher(body);
    if (ref.find()) t.reference = ref.group(1);
    t.category = category(t.merchant + " " + body);
    t.source = "SMS";
    // A bank reference identifies the same payment across SMS and notifications.
    t.fingerprint =
        t.reference.isEmpty()
            ? "payment:"
                + t.amountPaise
                + ":"
                + t.income
                + ":"
                + t.merchant.toLowerCase(Locale.ROOT)
                + ":"
                + (date / 120000)
            : "ref:" + t.reference + ":" + t.amountPaise + ":" + t.income;
    return t;
  }

  public static long paise(String value) {
    try {
      return new BigDecimal(value.replace(",", "").trim()).movePointRight(2).longValueExact();
    } catch (Exception e) {
      return 0;
    }
  }

  public static String category(String text) {
    String s = text.toLowerCase(Locale.ROOT);
    if (s.matches("(?s).*(swiggy|zomato|restaurant|food|cafe|burger|hotel|dining|kfc|mcdonalds).*"))
      return "Food & Dining";
    if (s.matches("(?s).*(blinkit|zepto|instamart|bigbasket|grocery|supermarket|dmart).*"))
      return "Groceries";
    if (s.matches("(?s).*(uber|ola|rapido|cab|metro|bus|train|irctc|flight|transport).*"))
      return "Transport";
    if (s.matches("(?s).*(petrol|fuel|diesel|iocl|hpcl|bpcl).*")) return "Fuel";
    if (s.matches("(?s).*(amazon|flipkart|myntra|zudio|mall|shopping).*")) return "Shopping";
    if (s.matches("(?s).*(electricity|water|gas|bescom|utility|utilities|bill payment).*"))
      return "Bills & Utilities";
    if (s.matches("(?s).*(rent|house rent|flat rent).*")) return "Rent";
    if (s.matches("(?s).*(health|hospital|pharmacy|doctor|medical|clinic|apollo).*"))
      return "Health & Medical";
    if (s.matches("(?s).*(insurance|lic|policy).*")) return "Insurance";
    if (s.matches("(?s).*(school|college|fee|tuition|education).*")) return "Education";
    if (s.matches("(?s).*(godaddy|hosting|business|salary|vendor|freelance).*")) return "Business";
    if (s.matches("(?s).*(self transfer|own account|internal transfer|transfer).*"))
      return "Transfers";
    if (s.matches("(?s).*(airtel|jio|vi recharge|recharge|bsnl).*")) return "Recharge";
    if (s.matches("(?s).*(netflix|spotify|prime|youtube|subscription|subscriptions).*"))
      return "Subscriptions";
    if (s.matches("(?s).*(travel|hotel|booking|makemytrip|trip).*")) return "Travel";
    if (s.matches("(?s).*(movie|bookmyshow|cinema|entertainment|game).*"))
      return "Entertainment";
    if (s.matches("(?s).*(investment|mutual fund|sip|stocks|zerodha|groww).*"))
      return "Investments";
    if (s.matches("(?s).*(savings|fd|deposit).*")) return "Savings";
    if (s.matches("(?s).*(emi|loan|credit card bill).*")) return "EMI & Loans";
    return "Shopping";
  }

  public static Transaction receipt(String text) {
    return ReceiptParser.parse(text);
  }
}
