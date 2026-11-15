# Editable Android layouts

All app-owned layouts are in `app/src/main/res/layout/`. The Java activity inflates these resources and binds the data and click handlers. Open any XML file in Android Studio and choose **Design**, **Split**, or **Code**.

| Design | XML file |
|---|---|
| Main activity, content area and navigation include | `activity_main.xml` |
| Welcome / Get Started | `screen_welcome.xml` |
| SMS permission introduction | `screen_sms_permission.xml` |
| Home dashboard | `screen_home.xml` |
| Transaction list and filters | `screen_transactions.xml` |
| Category cards | `screen_categories.xml` |
| Monthly reports | `screen_reports.xml` |
| Settings | `screen_settings.xml` |
| Scan SMS / manual entry / receipt options sheet | `bottom_sheet_add_options.xml` |
| Add income or expense sheet | `bottom_sheet_add_transaction.xml` |
| Edit transaction sheet | `bottom_sheet_edit_transaction.xml` |
| Transaction details sheet | `bottom_sheet_transaction_details.xml` |
| Delete confirmation | `dialog_delete_transaction.xml` |
| Bank selection | `dialog_bank_filter.xml` |
| Category, receipt-source and export choices | `dialog_choices.xml` |
| Search | `dialog_search.xml` |
| About, privacy, permission rationale and confirmations | `dialog_message.xml` |
| Scanning progress | `dialog_loading.xml` |

Shared pieces are separate files:

- `view_bottom_navigation.xml`, `view_toolbar.xml`, `view_sheet_header.xml`.
- `view_transaction_filters.xml`, `view_transaction_type.xml`, `view_save_actions.xml`.
- `view_field_merchant.xml`, `view_field_amount_date.xml`, `view_field_category.xml`, `view_field_account.xml`, `view_field_reference.xml`, `view_field_notes.xml`.
- `view_empty_state.xml`.
- `item_transaction.xml`, `item_date_header.xml`, `item_transaction_detail.xml`, `item_transaction_detail_pill.xml`, `item_report_category.xml`, `item_dialog_choice.xml`, `item_bank_choice.xml`.

There are **38 XML layout files**. Shared text styling is in `res/values/layout_styles.xml`, text resources in `res/values/layout_strings.xml`, fonts in `res/font/`, and card/button/field backgrounds in `res/drawable/bg_*.xml`.

The graph and small vector icons use XML-inflatable custom views, `SpendingChartView` and `PockieIcon`. Their position and size are declared in the layouts; their drawing code renders the graph data and icon shapes. Android supplies the native date picker, runtime permission prompts, camera activity and file picker.

The receipt parsing and stored transaction schema are unchanged by the layout migration.
