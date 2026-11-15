from pathlib import Path
p=Path('tools/create_xml_layouts.py')
s=p.read_text(encoding='utf-8').replace('<style name="Pockie.Text">','<style name="Pockie.Text" parent="">').replace('<style name="Pockie.Input">','<style name="Pockie.Input" parent="">')
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/pockie/app/Ui.java')
s=p.read_text(encoding='utf-8');a=s.index('  public static GradientDrawable bg(');b=s.index('  public static class Icon extends View {',a);s=s[:a]+s[b:];s=s.replace('import android.graphics.drawable.GradientDrawable;\n','').replace('import android.widget.*;\n','');p.write_text(s,encoding='utf-8')
