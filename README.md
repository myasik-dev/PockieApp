# Pockie · Android expense tracker

A native **Java** Android app built around the supplied Pockie screens and character artwork. Open this folder in Android Studio, let Gradle sync, and run the `app` configuration. Android 8.0 (API 26) or newer is required.

## Build

Use JDK 17 and Android SDK 36. Set your SDK location in Android Studio or in an untracked `local.properties` file. The Gradle wrapper is included.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
.\gradlew.bat connectedReceiptQaAndroidTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Device tests run in a separate application, `com.pockie.app.receiptqa`, and use only its sample records. They do not reset the installed Pockie app's expense records. Optional real-receipt tests use `zudio-receipt.png` and `chennai-silks-receipt.png` supplied to the QA app's external files directory; the private receipt images are not included in the source archive.

## Included flows

- Illustrated welcome and SMS permission onboarding, with manual entry available when access is declined.
- Cyan home dashboard, live monthly spending/income/average summaries, recent transactions and a raised add button.
- Transactions grouped by day, income/expense filters, merchant/category/account/reference search and category filtering.
- Native rounded sheets for adding income or expenses, transaction details, editing, and confirmed deletion.
- Food, Fuel, Shopping, Business, Recharge and Transfers category cards with supplied artwork and live totals.
- Monthly reports with a category spending chart and previous/next month navigation.
- SMS inbox scanning and an incoming SMS receiver. A conservative parser recognizes INR/Rs/₹ bank debit and credit messages, extracts merchant/account/reference, and ignores OTPs, failures and balance-only messages.
- Optional notification tracking for banking, payment and messaging apps, enabled through Android notification access settings.
- Duplicate prevention using bank transaction references, with a time/merchant fallback for messages without references.
- Receipt capture or image selection, bundled on-device ML Kit text recognition, and an editable review before saving. Small images are enlarged; OCR coordinates reunite total labels with values in separate columns. A contrast-enhanced crop retries unclear totals on small photos. Parsing extracts the merchant, payable total, date and invoice reference. Generic headings such as “Tax Invoice” do not select Business. Unreadable totals and dates remain blank for manual review, and must be entered before saving.
- Bank filters, an internal-transfer totals switch, privacy information and app details.
- CSV export, JSON backup and validated JSON restore/merge through Android's file picker.
- SQLite persistence with amounts stored as integer paise. Local records have stable IDs for duplicate-safe backup restore. Automatic cloud/device-transfer backups are disabled.

## Using the preview

On the empty dashboard, choose **Preview with sample data** to see the screenshot's five example transactions. Sample records are labeled and can be cleared independently in Home or Settings. Demo amounts are not presented as real finances, and all summaries calculate from the actual records. Reports open on the current month; choose October 2026 to inspect the supplied sample transactions.

## Device-dependent features

SMS and notification tracking require explicit Android access. Some devices/installers restrict SMS permissions or notification access for sideloaded apps; manual entry and receipt import remain available. Android may redact some sensitive notification content. Bank message formats vary, so verify automatically imported records. Receipt scanning suggests a merchant and total; confirm the date and amount in the review form.

The app does not send messages or upload transaction/receipt content to a server. CSV/JSON exports contain your financial records and are written to the location you select. Google Play distribution requires reviewing its SMS permissions policy and obtaining any required approval; this project does not claim store approval.

## Source layout

All screen, sheet, dialog and row designs are separate XML files in **`app/src/main/res/layout/`**. Open them directly in Android Studio's Design or Split view. See [the layout index](docs/LAYOUTS.md) for the 38 files and their purpose. Common text styles, strings, fonts and backgrounds are also Android resources.

`MainActivity.java` inflates the XML layouts and connects navigation, data and click handlers. `PockieIcon.java` and `SpendingChartView.java` are XML-inflatable drawing controls. `ExpenseStore.java` handles SQLite persistence/export, `BankParser.java` message parsing, `ReceiptParser.java` receipt field extraction, `ReceiptScanner.java` image preparation and on-device OCR, and `Tracking.java` shared import preferences. `SmsReceiver.java` and `BankNotificationService.java` handle approved background imports.

Supplied illustration assets are bundled as transparent WebP resources. Poppins fonts are distributed under the included SIL Open Font License in `app/src/main/assets/fonts/OFL.txt`.
