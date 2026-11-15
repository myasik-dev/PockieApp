# Pockie 1.0.2 verification

Checked on 3 November 2026 with JDK 17, Android SDK 36 and the bundled Gradle wrapper.

## XML layout migration

- All 38 app-owned screen, sheet, dialog, field, navigation and row layouts are editable XML resources in `app/src/main/res/layout/` and are used by the Java activity.
- `assembleDebug` and `assembleReceiptQaAndroidTest` succeed.
- All 16 JVM tests pass: six bank/money tests and ten receipt parser regression cases.
- Android lint completes with zero errors and 113 warnings, primarily layout/accessibility advice, dependency notices and custom-view suggestions.
- The isolated Android resource-inflation test passed for all 38 layouts, including XML icon IDs.
- Full interaction and screenshot checks were interrupted when the connected phone locked. The attempted temporary emulator could not start because its AVD disk lacked free space. The user requested finishing with build checks only, so final interaction and visual checks were not completed for version 1.0.2.
- No test reset or migration was performed against the user's installed Pockie database. The database schema and receipt processing are unchanged by this version.

## Previous receipt verification (version 1.0.1)

Before the XML migration, five app flow tests and three receipt integration tests passed on Android 14. The supplied Zudio image extracted Shopping, ₹1,397.00, 24 August 2025 and invoice Z258100610006; the review form values were asserted. The supplied Chennai Silks photo extracted ₹3,135.00 and Shopping using the enhanced OCR retry. Those parsing regressions remain covered by the passing JVM suite, but their full UI paths were not reverified after this layout migration.

Live bank SMS delivery, banking-app notification formats and external file-picker providers require device-specific validation. The app supports API 26 and newer; Android 14 was used for the earlier physical-device tests. Exports contain financial records and are saved only to a selected location. The app does not declare internet or network-state permission.
