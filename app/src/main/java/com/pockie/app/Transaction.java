package com.pockie.app;

public class Transaction {
  public long id;
  public String merchant = "",
      category = "Shopping",
      account = "",
      reference = "",
      notes = "",
      source = "Manual",
      fingerprint = "";
  public long amountPaise, date = System.currentTimeMillis();
  public boolean income, demo;

  public Transaction copy() {
    Transaction t = new Transaction();
    t.id = id;
    t.merchant = merchant;
    t.category = category;
    t.account = account;
    t.reference = reference;
    t.notes = notes;
    t.source = source;
    t.fingerprint = fingerprint;
    t.amountPaise = amountPaise;
    t.date = date;
    t.income = income;
    t.demo = demo;
    return t;
  }
}
