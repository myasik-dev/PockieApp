from pathlib import Path
p=Path('app/src/main/java/com/pockie/app/MainActivity.java')
source=p.read_text(encoding='utf-8')
# Preserve a local pre-migration reference for visual comparison; not a second application source.
backup=Path('artifacts/layout-migration/MainActivity.before-xml.txt');backup.parent.mkdir(parents=True,exist_ok=True)
if not backup.exists():backup.write_text(source,encoding='utf-8')
def replace(start,end,value):
 global source
 a=source.index(start);b=source.index(end,a);source=source[:a]+value+source[b:]
source=source.replace('private LinearLayout navigation;','private View navigation;')
source=source.replace('root = new FrameLayout(this);\n    root.setBackgroundColor(Color.WHITE);\n    setContentView(root);','setContentView(R.layout.activity_main);\n    root=findViewById(R.id.app_root);\n    body=findViewById(R.id.screen_container);\n    navigation=findViewById(R.id.bottom_navigation);')
replace('  private LinearLayout.LayoutParams lp(', '  private void go(', '')
replace('  private TextView label(', '  private int artFor(', '')
replace('  private void render() {','  private void finishOnboarding()', '''  private void render() {
    if(isFinishing())return;
    body.removeAllViews();
    boolean hasNav=onboarding==2&&!screen.equals("Settings");
    navigation.setVisibility(hasNav?View.VISIBLE:View.GONE);
    FrameLayout.LayoutParams position=(FrameLayout.LayoutParams)body.getLayoutParams();position.bottomMargin=hasNav?d(70):0;body.setLayoutParams(position);
    if(onboarding<2){renderOnboarding();return;}
    switch(screen){case "Transactions":transactions();break;case "Categories":categoryScreen();break;case "Reports":reports();break;case "Settings":settings();break;default:home();}
    if(hasNav)bottomNav();
  }

  private void renderOnboarding(){
    View page=showLayout(onboarding==0?R.layout.screen_welcome:R.layout.screen_sms_permission);
    if(onboarding==0)action(page,R.id.get_started,()->{onboarding=1;render();});
    else{action(page,R.id.allow_sms,()->requestSms(true));action(page,R.id.skip_sms,this::finishOnboarding);}
  }

''')
replace('  private void bottomNav() {','  private boolean notificationAccess()',Path('tools/xml_ui_methods.java.txt').read_text(encoding='utf-8'))
replace('  private void reports() {','  private class Donut extends View {','''  private void reports(){
    View page=showLayout(R.layout.screen_reports);toolbar(page,"Reports",false);
    bindText(page,R.id.report_month,date(reportMonth.getTimeInMillis(),"MMMM yyyy"));
    action(page,R.id.previous_month,()->{reportMonth.add(Calendar.MONTH,-1);render();});action(page,R.id.next_month,()->{reportMonth.add(Calendar.MONTH,1);render();});
    long expense=0,income=0;Map<String,Long> sums=new LinkedHashMap<>();for(String name:categories)sums.put(name,0L);
    for(Transaction t:store.all())if(sameMonth(t.date,reportMonth)&&included(t)){if(t.income)income+=t.amountPaise;else{expense+=t.amountPaise;sums.put(t.category,sums.getOrDefault(t.category,0L)+t.amountPaise);}}
    bindText(page,R.id.report_spending,money(expense));bindText(page,R.id.report_income,"Income  "+money(income));
    page.findViewById(R.id.spending_chart).setVisibility(expense>0?View.VISIBLE:View.GONE);page.findViewById(R.id.report_category_heading).setVisibility(expense>0?View.VISIBLE:View.GONE);page.findViewById(R.id.report_transfer_note).setVisibility(expense>0?View.VISIBLE:View.GONE);
    LinearLayout list=page.findViewById(R.id.report_categories);
    if(expense==0){empty(page.findViewById(R.id.report_empty),"A fresh month","Your spending report will grow as you add expenses.",false);return;}
    ((SpendingChartView)page.findViewById(R.id.spending_chart)).setData(sums,expense,money(expense));
    for(String name:categories){long amount=sums.get(name);if(amount==0)continue;View row=getLayoutInflater().inflate(R.layout.item_report_category,list,false);row.findViewById(R.id.report_category_badge).setBackgroundTintList(ColorStateList.valueOf(colorFor(name)));((ImageView)row.findViewById(R.id.report_category_art)).setImageResource(artFor(name));bindText(row,R.id.report_category_name,name);bindText(row,R.id.report_category_amount,money(amount)+"  ·  "+Math.round(100.0*amount/expense)+"%");row.setOnClickListener(v->{screen="Transactions";categoryFilter=name;filter="Expense";render();});list.addView(row);}
    bindText(page,R.id.report_transfer_note,prefs.getBoolean("exclude_transfer",true)?"Internal transfers are excluded from totals.":"Internal transfers are included in totals.");
  }

''')
source=source[:source.index('  private class Donut extends View {')]+'}\n'
p.write_text(source,encoding='utf-8')
print('Java screens now inflate res/layout resources')
