from pathlib import Path
p=Path('app/src/main/java/com/pockie/app/MainActivity.java');s=p.read_text(encoding='utf-8')
def method(start,end,replacement):
 global s
 a=s.index(start);b=s.index(end,a);s=s[:a]+replacement+'\n\n'+s[b:]

# Each view has a unique XML ID; the adapter code only supplies values and events.
s=s.replace('bindText(card, R.id.category_amount, money(amount));','bindText(card, amountIds[i], money(amount));').replace('bindText(card, R.id.category_count, "(" + count + ")");','bindText(card, countIds[i], "(" + count + ")");')
start=s.index('  private void categoryScreen()');pos=s.index('    List<Transaction> all = store.all();',start)
s=s[:pos]+'''    int[] amountIds={R.id.category_amount_food,R.id.category_amount_fuel,R.id.category_amount_shopping,R.id.category_amount_business,R.id.category_amount_recharge,R.id.category_amount_transfers};
    int[] countIds={R.id.category_count_food,R.id.category_count_fuel,R.id.category_count_shopping,R.id.category_count_business,R.id.category_count_recharge,R.id.category_count_transfers};
'''+s[pos:]
for kind in ['sms','notifications','transfers']:s=s.replace('settingSwitch(\n        page.findViewById(R.id.setting_'+kind+'),','settingSwitch(\n        page.findViewById(R.id.setting_'+kind+'_switch),')
s=s.replace('R.id.setting_subtitle,\n        banks.isEmpty()','R.id.setting_banks_subtitle,\n        banks.isEmpty()')
method('  private void settingSwitch(', '  private boolean notificationAccess()', '''  private void settingSwitch(androidx.appcompat.widget.SwitchCompat toggle,boolean checked,java.util.function.Consumer<Boolean> action) {
    toggle.setChecked(checked);toggle.setOnCheckedChangeListener((button,value)->action.accept(value));
  }''')
s=s.replace('inflate(R.layout.item_transaction_detail, parent, false)','inflate(background!=0?R.layout.item_transaction_detail_pill:R.layout.item_transaction_detail, parent, false)').replace('      label.setPadding(d(10), d(5), d(10), d(5));\n','')

method('  private void searchDialog() {','  private void categoryScreen()', '''  private void searchDialog(){
    View view=getLayoutInflater().inflate(R.layout.dialog_search,null);EditText input=view.findViewById(R.id.search_input);input.setText(query);AlertDialog dialog=xmlDialog(view);
    action(view,R.id.dialog_cancel,dialog::dismiss);
    action(view,R.id.dialog_clear,()->{query="";dialog.dismiss();render();});
    action(view,R.id.dialog_confirm,()->{query=input.getText().toString().trim();screen="Transactions";dialog.dismiss();render();});
    dialog.setOnShowListener(v->{input.requestFocus();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);});dialog.show();styleDialog(dialog);
  }

  private void choiceDialog(String title,String[] options,java.util.function.IntConsumer selected){
    View view=getLayoutInflater().inflate(R.layout.dialog_choices,null);bindText(view,R.id.dialog_title,title);AlertDialog dialog=xmlDialog(view);ListView choices=view.findViewById(R.id.dialog_choices);
    choices.setAdapter(new ArrayAdapter<>(this,R.layout.item_dialog_choice,R.id.choice_text,options));choices.setOnItemClickListener((parent,item,index,id)->{dialog.dismiss();selected.accept(index);});action(view,R.id.dialog_cancel,dialog::dismiss);dialog.show();styleDialog(dialog);
  }

  private AlertDialog loadingDialog(String title,String message){
    View view=getLayoutInflater().inflate(R.layout.dialog_loading,null);bindText(view,R.id.dialog_title,title);bindText(view,R.id.dialog_message,message);AlertDialog dialog=xmlDialog(view);dialog.setCancelable(false);dialog.show();styleDialog(dialog);return dialog;
  }''')
# Rationale, status and confirmation dialogs share an editable XML template.
start=s.index('    new AlertDialog.Builder(this)',s.index('  private void requestSms('));end=s.index('\n  @Override',start)
s=s[:start]+'''    messageDialog("Automatically track bank payments","Pockie reads bank transaction messages on this phone to create income and expense records. Messages stay on your device. You can turn this off in Settings, or continue with manual entry.","Not now",()->{if(onboard)finishOnboarding();else render();},"Allow access",()->requestPermissions(new String[]{Manifest.permission.READ_SMS,Manifest.permission.RECEIVE_SMS},onboard?101:102));
  }
'''+s[end:]
method('  private void bankFilter() {','  private void trackingStatus()', '''  private void bankFilter(){
    String[] banks={"KVB","SBI","HDFC","ICICI","AXIS","KOTAK","IDFC","PNB","BOB","CANARA","INDIANBANK","UNION"};Set<String> chosen=new HashSet<>(prefs.getStringSet("banks",new HashSet<>()));
    View view=getLayoutInflater().inflate(R.layout.dialog_bank_filter,null);AlertDialog dialog=xmlDialog(view);ListView list=view.findViewById(R.id.bank_choices);list.setAdapter(new ArrayAdapter<>(this,R.layout.item_bank_choice,R.id.bank_choice_label,banks));for(int i=0;i<banks.length;i++)list.setItemChecked(i,chosen.contains(banks[i]));
    list.setOnItemClickListener((parent,item,index,id)->{if(list.isItemChecked(index))chosen.add(banks[index]);else chosen.remove(banks[index]);});
    action(view,R.id.dialog_cancel,dialog::dismiss);action(view,R.id.all_banks,()->{prefs.edit().remove("banks").apply();dialog.dismiss();render();});action(view,R.id.dialog_confirm,()->{prefs.edit().putStringSet("banks",chosen).apply();dialog.dismiss();render();});dialog.show();styleDialog(dialog);
  }''')
method('  private void trackingStatus() {','  private void clearDemo()', '''  private void trackingStatus(){
    boolean active=(prefs.getBoolean("sms",false)&&checkSelfPermission(Manifest.permission.READ_SMS)==PackageManager.PERMISSION_GRANTED)||(prefs.getBoolean("notifications",false)&&notificationAccess());
    messageDialog(active?"Tracking is enabled":"Track your expenses automatically","Manage SMS and notification access, choose your banks, and control internal transfers in Settings.","Done",null,"Open Settings",()->go("Settings"));
  }''')
method('  private void clearDemo() {','  private void scanSms()', '''  private void clearDemo(){
    messageDialog("Clear sample data?","Only the preview transactions will be removed. Your own transactions will stay.","Cancel",null,"Clear samples",()->{store.removeDemo();render();});
  }''')
s=s.replace('ProgressDialog progress =\n        ProgressDialog.show(this, "Scan SMS", "Looking for bank transactions…", true, false);','AlertDialog progress=loadingDialog("Scan SMS","Looking for bank transactions…");')
s=s.replace('ProgressDialog progress =\n        ProgressDialog.show(\n            this, "Scan Receipt", "Reading your receipt on this device…", true, false);','AlertDialog progress=loadingDialog("Scan Receipt","Reading your receipt on this device…");')
method('  private void receiptOptions() {','  private void camera()', '''  private void receiptOptions(){
    choiceDialog("Scan Receipts",new String[]{"Take a photo","Choose receipt image"},which->{if(which==0)camera();else{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,201);}});
  }''')
method('  private void backupOptions() {','  private String backup()', '''  private void backupOptions(){
    choiceDialog("Backup & Export",new String[]{"Export CSV spreadsheet","Save JSON backup","Restore JSON backup"},which->{
      if(which==2){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,302);}
      else{pendingExport=which==0?"csv":"json";Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(which==0?"text/csv":"application/json");intent.putExtra(Intent.EXTRA_TITLE,"Pockie-"+date(System.currentTimeMillis(),"yyyy-MM-dd")+"."+pendingExport);startActivityForResult(intent,301);}
    });
  }''')
# The category picker uses the same XML-backed choice dialog.
a=s.index('    action(\n        view,\n        R.id.category_selector,');b=s.index('    TextView hint =',a)
s=s[:a]+'''    action(view,R.id.category_selector,()->choiceDialog("Choose category",categories,index->{t.category=categories[index];selectedCategory.setText(t.category);}));
'''+s[b:]
# Keep backup merging logic unchanged while moving its confirmation to dialog_message.xml.
a=s.index('            runOnUiThread(',s.index('  private void restore('));b=s.index('                        .show());',a)+len('                        .show());')
s=s[:a]+'''            runOnUiThread(()->messageDialog("Restore backup?","Merge "+drafts.size()+" transactions with your current records? Existing records are kept and matching backup records are skipped.","Cancel",null,"Restore",()->mergeBackup(drafts)));'''+s[b:]
a=s.index('  @Override\n  protected void onActivityResult')
s=s[:a]+'''  private void mergeBackup(List<Transaction> drafts){
    int added=0;android.database.sqlite.SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();try{for(Transaction transaction:drafts)if(store.save(transaction)>0)added++;db.setTransactionSuccessful();}finally{db.endTransaction();}render();toast(added+" transactions restored.");
  }

'''+s[a:]
p.write_text(s,encoding='utf-8')
print('Dialogs and dynamic binding now use XML resources')
