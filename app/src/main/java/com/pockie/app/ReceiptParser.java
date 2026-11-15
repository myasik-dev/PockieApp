package com.pockie.app;

import java.text.*;
import java.util.*;
import java.util.regex.*;

/** Extracts receipt fields without using generic document headings as spending categories. */
public final class ReceiptParser {
  private static final Pattern TOTAL =
      Pattern.compile(
          "(?i)\\b(total\\s*invoice\\s*amount|grand\\s*total|net\\s*payable|amount\\s*payable|total\\s*payable|total\\s*amount|amount\\s*due|amount\\s*paid|bill\\s*total|net\\s*amount|gross\\s*total|total)\\b");
  private static final Pattern NUMBER =
      Pattern.compile(
          "(?i)(?<![A-Za-z0-9./-])(?:₹|INR|Rs\\.?)?\\s*([0-9]+(?:,[0-9]{2,3})*(?:\\.[0-9Oo]{1,2})?)(?![A-Za-z0-9./-])");
  private static final Pattern ISO_DATE =
      Pattern.compile("(?<!\\d)(20\\d{2})\\s*[-/.]\\s*(\\d{1,2})\\s*[-/.]\\s*(\\d{1,2})(?!\\d)");
  private static final Pattern LOCAL_DATE =
      Pattern.compile("(?<!\\d)(\\d{1,2})\\s*[-/.]\\s*(\\d{1,2})\\s*[-/.]\\s*(20\\d{2})(?!\\d)");
  private static final Pattern WORD_DATE =
      Pattern.compile(
          "(?i)\\b(\\d{1,2})["
              + " -]+(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)["
              + " ,/-]+(20\\d{2})\\b");

  private ReceiptParser() {}

  /** OCR fragments retain coordinates so labels can be reunited with right-aligned values. */
  public static final class Line {
    public final String text;
    public final float left, top, right, bottom;

    public Line(String text, float left, float top, float right, float bottom) {
      this.text = text;
      this.left = left;
      this.top = top;
      this.right = right;
      this.bottom = bottom;
    }

    float center() {
      return (top + bottom) / 2;
    }

    float height() {
      return Math.max(1, bottom - top);
    }
  }

  public static Transaction parse(String text) {
    return parse(text, Collections.emptyList());
  }

  public static Transaction parse(String raw, List<Line> positioned) {
    Transaction t = new Transaction();
    t.source = "Receipt";
    t.date = 0; // An unread receipt date must not silently become today's transaction.
    t.notes = raw == null ? "" : raw;
    String layout = positioned.isEmpty() ? t.notes : rows(positioned);
    String[] lines = layout.replace('\u00a0', ' ').split("\\r?\\n");
    t.merchant = merchant(lines);
    t.amountPaise = total(lines);
    Long parsedDate = receiptDate(layout);
    if (parsedDate != null) t.date = parsedDate;
    t.reference = reference(lines);
    t.category = category(t.merchant, layout);
    return t;
  }

  static String rows(List<Line> fragments) {
    List<Line> sorted = new ArrayList<>(fragments);
    sorted.sort(Comparator.comparingDouble(Line::center));
    List<List<Line>> rows = new ArrayList<>();
    for (Line line : sorted) {
      List<Line> last = rows.isEmpty() ? null : rows.get(rows.size() - 1);
      Line anchor = last == null ? null : last.get(0);
      if (anchor == null
          || Math.abs(anchor.center() - line.center())
              > Math.max(anchor.height(), line.height()) * .55f) {
        last = new ArrayList<>();
        rows.add(last);
      }
      last.add(line);
    }
    StringBuilder output = new StringBuilder();
    for (List<Line> row : rows) {
      row.sort(Comparator.comparingDouble(l -> l.left));
      for (Line line : row) {
        if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') output.append(' ');
        output.append(line.text.trim());
      }
      output.append('\n');
    }
    return output.toString();
  }

  private static long total(String[] lines) {
    long best = 0;
    int bestScore = -1;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim().replaceAll("(?i)\\bt[ou0]tal\\b", "Total");
      if (line.matches(
          "(?i).*\\b(sub\\s*total|total\\s+(?:tax|discount|saving|items|qty|quantity))\\b.*"))
        continue;
      Matcher label = TOTAL.matcher(line);
      if (!label.find()) continue;
      String key = label.group(1).toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
      int score =
          key.equals("totalinvoiceamount") || key.equals("grandtotal")
              ? 130
              : key.contains("payable")
                  ? 125
                  : key.equals("gross total") || key.equals("grosstotal")
                      ? 100
                      : key.equals("total") ? 90 : 115;
      long amount = lastAmount(line.substring(label.end()));
      if (amount > 0) {
        if (score >= bestScore) {
          best = amount;
          bestScore = score;
        }
        continue;
      }
      // OCR can place a value in a following text block even for an ordinary text receipt.
      for (int j = i + 1; j < Math.min(lines.length, i + 4); j++) {
        String candidate = lines[j].trim();
        if (TOTAL.matcher(candidate).find()) continue;
        if (!candidate.matches("(?i)[₹Rs.INR\\s0-9,Oo.:-]+")) break;
        amount = lastAmount(candidate);
        if (amount > 0) {
          if (score - 5 >= bestScore) {
            best = amount;
            bestScore = score - 5;
          }
          break;
        }
      }
    }
    return best;
  }

  private static long lastAmount(String text) {
    // Some OCR engines separate the thousands group with a space.
    text = text.replaceAll("(?<=\\d) +(?=\\d{3}\\.[0-9Oo]{2}(?:\\D|$))", ",");
    Matcher number = NUMBER.matcher(text);
    long amount = 0;
    while (number.find()) {
      String token = number.group(1).replace('O', '0').replace('o', '0');
      long candidate = BankParser.paise(token);
      if (candidate > 0 && candidate <= 999999999999L) amount = candidate;
    }
    return amount;
  }

  private static String merchant(String[] lines) {
    for (int i = 0; i < Math.min(12, lines.length); i++) {
      String header = lines[i].trim();
      if (header.matches("(?i).*\\bzudio\\b.*")) return "Zudio";
    }
    for (String raw : lines) {
      String line = raw.trim();
      if (!line.matches(".*[A-Za-z]{3,}.*")) continue;
      if (line.matches(
          "(?i).*(tax\\s*invoice|invoice\\s*(?:no|number)|store details|tell us|experience|thank"
              + " you|customer|cashier|counter|gstin|place of"
              + " supply|mobile|phone|receipt|^date|^bill$|^invoice$).*")) continue;
      return line;
    }
    return "";
  }

  static String category(String merchant, String receipt) {
    String name = merchant.toLowerCase(Locale.ROOT), body = receipt.toLowerCase(Locale.ROOT);
    // Retail brands and the items purchased outweigh legal boilerplate such as "Tax Invoice".
    if (name.matches(".*\\b(zudio|zara|myntra|ajio|westside|pantaloons|lifestyle|dmart|h&m)\\b.*")
        || body.matches(
            "(?s).*\\b(womenswear|menswear|loungewear|garments|apparel|clothing|shirt|shirts|trousers|kurti|footwear|saree)\\b.*"))
      return "Shopping";
    String named = BankParser.category(merchant);
    if (!named.equals("Shopping")) return named;
    String withoutHeadings = body.replaceAll("(?i)\\b(?:tax\\s*)?invoice\\b", "");
    return BankParser.category(withoutHeadings);
  }

  private static Long receiptDate(String text) {
    Matcher iso = ISO_DATE.matcher(text);
    while (iso.find()) {
      Long date = date(iso.group(1), iso.group(2), iso.group(3));
      if (date != null) return date;
    }
    Matcher local = LOCAL_DATE.matcher(text);
    while (local.find()) {
      Long date = date(local.group(3), local.group(2), local.group(1));
      if (date != null) return date;
    }
    Matcher words = WORD_DATE.matcher(text);
    while (words.find()) {
      String month = words.group(2).substring(0, 3).toLowerCase(Locale.ROOT);
      int index =
          Arrays.asList(
                  "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov",
                  "dec")
              .indexOf(month);
      Long date = date(words.group(3), Integer.toString(index + 1), words.group(1));
      if (date != null) return date;
    }
    return null;
  }

  private static Long date(String year, String month, String day) {
    try {
      Calendar calendar = Calendar.getInstance();
      calendar.clear();
      calendar.setLenient(false);
      calendar.set(
          Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day), 12, 0, 0);
      return calendar.getTimeInMillis();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String reference(String[] lines) {
    Pattern p =
        Pattern.compile(
            "(?i)\\b(?:invoice|bill|receipt)\\s*(?:no\\.?|number|#)\\s*[:.-]?\\s*([A-Za-z0-9][A-Za-z0-9"
                + " /-]{2,38})");
    for (String line : lines) {
      Matcher m = p.matcher(line);
      if (m.find()) {
        String value =
            m.group(1)
                .split("(?i)\\s+(?:date|counter|cashier|customer)\\b|\\s+20\\d{2}[-/]")[0]
                .trim();
        return value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
      }
    }
    return "";
  }
}
