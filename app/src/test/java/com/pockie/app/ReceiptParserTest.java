package com.pockie.app;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class ReceiptParserTest {
  @Test
  public void commonOcrTotalLabelErrorsStillIdentifyThePrintedTotal() {
    assertEquals(
        313500,
        BankParser.receipt("The Chennai Silks\nTUTAL AHDLNT : 3135.00\nPAYABLE AMOUNT : 0.00")
            .amountPaise);
  }

  @Test
  public void chennaiSilksTotalAmountNeedsNoCurrencySymbol() {
    Transaction t =
        BankParser.receipt(
            "The Chennai Silks\n"
                + "Bill Summary\n"
                + "DATE : 25/10/2020 18:27:07\n"
                + "BILL AMOUNT\n"
                + "2000.00\n"
                + "1135.00\n"
                + "TOTAL AMOUNT : 3135.00\n"
                + "CR.CARD AMOUNT : 3135.00\n"
                + "AMOUNT RECEIVED : 0.00\n"
                + "PAYABLE AMOUNT : 0.00\n"
                + "REFUND AMOUNT : 0.00");
    assertEquals(313500, t.amountPaise);
    assertEquals("Shopping", t.category);
    assertDate(t, 2020, 10, 25);
  }

  @Test
  public void zudioInvoiceExtractsShoppingTotalDateAndReference() {
    Transaction t =
        BankParser.receipt(
            "ZUdIO\n"
                + "Zudio - Delhi - Janakpuri\n"
                + "Tell us about your overall experience\n"
                + "Trent Limited\n"
                + "TAX INVOICE\n"
                + "INVOICE No.: Z258 100610006\n"
                + "2025-08-24 14:14:02\n"
                + "COUNTER: 13\n"
                + "WESTERN WOMENSWEAR SHIRT WOVE\n"
                + "599.00\n"
                + "Women's Loungewear Tops\n"
                + "599.00\n"
                + "Women's Wear Top Circular Kni\n"
                + "199.00\n"
                + "Gross Total: ₹1,397.00\n"
                + "Total Invoice Amount: ₹1,397.00");
    assertEquals("Zudio", t.merchant);
    assertEquals("Shopping", t.category);
    assertEquals(139700, t.amountPaise);
    assertEquals("Z258100610006", t.reference);
    assertDate(t, 2025, 8, 24);
  }

  @Test
  public void invoiceIsNotBusinessEvidence() {
    assertEquals("Shopping", BankParser.category("Zudio TAX INVOICE"));
    assertEquals(
        "Shopping", BankParser.receipt("Retail Store\nTax Invoice\nTotal: Rs 200").category);
    assertEquals("Food & Dining", BankParser.receipt("Cafe Pockie\nTax Invoice\nTotal Rs 100").category);
    assertEquals("Business", BankParser.receipt("GoDaddy\nTax Invoice\nTotal Rs 900").category);
  }

  @Test
  public void totalInvoiceAmountOutranksGrossAndItemPrices() {
    Transaction t =
        BankParser.receipt(
            "Zudio\n"
                + "Original price ₹9,999.00\n"
                + "Gross Total: 1,500.00\n"
                + "Total Invoice Amount: 1,397.00\n"
                + "Total Tax: 60.00\n"
                + "Change: ₹3.00");
    assertEquals(139700, t.amountPaise);
  }

  @Test
  public void totalCanBeOnNextLineOrInSeparatedColumns() {
    assertEquals(139700, BankParser.receipt("Zudio\nTotal Invoice Amount:\n₹1,397.00").amountPaise);
    List<ReceiptParser.Line> boxes =
        Arrays.asList(
            new ReceiptParser.Line("Zudio", 10, 0, 80, 20),
            new ReceiptParser.Line("Gross Total:", 10, 100, 130, 112),
            new ReceiptParser.Line("Total Invoice Amount:", 10, 130, 160, 143),
            new ReceiptParser.Line("₹1,397.00", 250, 131, 330, 144),
            new ReceiptParser.Line("₹1,500.00", 250, 101, 330, 114));
    assertEquals(
        139700,
        ReceiptParser.parse("Zudio\nGross Total\nTotal Invoice Amount\n1,500.00\n1,397.00", boxes)
            .amountPaise);
  }

  @Test
  public void labelledGrandTotalOutranksSubtotalTaxAndTender() {
    assertEquals(
        21000,
        BankParser.receipt(
                "Cafe\n"
                    + "Sub Total 200.00\n"
                    + "Total Tax 10.00\n"
                    + "Grand Total 210.00\n"
                    + "Cash 500.00\n"
                    + "Change 290.00")
            .amountPaise);
  }

  @Test
  public void separatedThousandsAndOcrZeroesAreAccepted() {
    assertEquals(139700, BankParser.receipt("Zudio\nTotal Invoice Amount: ₹1 397.OO").amountPaise);
  }

  @Test
  public void noRecognizedTotalDoesNotGuessFromItemPricesOrPhoneNumbers() {
    Transaction t =
        BankParser.receipt("Zudio\nPhone 9250443444\nInvoice No 123456789\nShirt ₹599.00");
    assertEquals(0, t.amountPaise);
  }

  @Test
  public void localAndWrittenDatesAreAcceptedButInvalidDatesAreIgnored() {
    assertDate(BankParser.receipt("Store\nDate: 24/08/2025\nTotal 100"), 2025, 8, 24);
    assertDate(BankParser.receipt("Store\n24 August 2025\nTotal 100"), 2025, 8, 24);
    Transaction invalid = BankParser.receipt("Store\n2025-02-31\nTotal 100");
    assertEquals(0, invalid.date);
    assertEquals(0, BankParser.receipt("Retail Store\nTotal 100").date);
  }

  private static void assertDate(Transaction t, int year, int month, int day) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(t.date);
    assertEquals(year, c.get(Calendar.YEAR));
    assertEquals(month - 1, c.get(Calendar.MONTH));
    assertEquals(day, c.get(Calendar.DAY_OF_MONTH));
  }
}
