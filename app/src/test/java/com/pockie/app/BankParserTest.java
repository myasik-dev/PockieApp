package com.pockie.app;

import static org.junit.Assert.*;

import org.junit.Test;

public class BankParserTest {
  @Test
  public void debitExtractsAmountMerchantAndAccount() {
    Transaction t =
        BankParser.parse(
            "VM-KVBBNK",
            "Your a/c XXXX5083 is debited Rs 328.00 to SWIGGY on 26-10-2026. Avl Bal Rs 23,000. Ref"
                + " PAA1254756333211",
            1000);
    assertNotNull(t);
    assertEquals(32800, t.amountPaise);
    assertEquals("SWIGGY", t.merchant);
    assertEquals("5083", t.account);
    assertEquals("Food & Dining", t.category);
    assertFalse(t.income);
    assertEquals("PAA1254756333211", t.reference);
  }

  @Test
  public void creditDoesNotUseAvailableBalance() {
    Transaction t =
        BankParser.parse(
            "HDFCBK",
            "INR 12,000.50 credited to your account XX9001 from MSM003 via UPI. Available balance"
                + " INR 80,000.00",
            1000);
    assertNotNull(t);
    assertTrue(t.income);
    assertEquals(1200050, t.amountPaise);
    assertEquals("MSM003", t.merchant);
  }

  @Test
  public void otpFailedAndFuturePaymentsAreIgnored() {
    assertNull(BankParser.parse("BANK", "OTP 123456 to pay Rs 500.00", 0));
    assertNull(BankParser.parse("BANK", "Payment failed. Rs 500 paid to SWIGGY", 0));
    assertNull(BankParser.parse("BANK", "Rs 100 will be debited tomorrow", 0));
    assertNull(BankParser.parse("BANK", "Your available balance is Rs 10,000", 0));
  }

  @Test
  public void transactionReferenceDeduplicatesAcrossSources() {
    Transaction a = BankParser.parse("KVB", "Rs 328 debited at SWIGGY. Ref ABC123456", 1000);
    Transaction b = BankParser.parse("KVB App", "Rs 328 debited at SWIGGY. Ref ABC123456", 50000);
    assertNotNull(a);
    assertNotNull(b);
    assertEquals(a.fingerprint, b.fingerprint);
  }

  @Test
  public void exactMoneyConversionRejectsInvalidAmounts() {
    assertEquals(11682, BankParser.paise("116.82"));
    assertEquals(10000000, BankParser.paise("1,00,000"));
    assertEquals(0, BankParser.paise("12.345"));
    assertEquals(0, BankParser.paise("not a number"));
  }

  @Test
  public void receiptPrefersFinalTotal() {
    Transaction t =
        BankParser.receipt("CAFE POCKIE\nSubtotal 200.00\nTax 10.00\nGrand Total: 210.00");
    assertEquals("CAFE POCKIE", t.merchant);
    assertEquals(21000, t.amountPaise);
    assertEquals("Food & Dining", t.category);
    assertEquals("Receipt", t.source);
  }
}
