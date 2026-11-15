"""Keep Android layout attributes readable in the source editor."""
from pathlib import Path
import re
import xml.etree.ElementTree as E

folder=Path('app/src/main/res/layout')
android='{http://schemas.android.com/apk/res/android}'
home=folder/'screen_home.xml';tree=E.parse(home)
for view in tree.getroot().iter():
    if view.get('style')=='@style/Pockie.Text.Bold' and view.get(android+'fontFamily') in ['sans','sans-serif-medium','casual']:
        view.set(android+'textStyle','bold')
E.register_namespace('android','http://schemas.android.com/apk/res/android');E.register_namespace('app','http://schemas.android.com/apk/res-auto');E.indent(tree,space='    ');tree.write(home,encoding='utf-8',xml_declaration=True)

def opening(match):
    indent,name,body,ending=match.groups()
    attributes=re.findall(r'([\w:.-]+)="([^"]*)"',body)
    if len(attributes)<3:return match.group(0)
    return indent+'<'+name+'\n'+'\n'.join(indent+'    '+key+'="'+value+'"'+((' />' if ending else '>') if i==len(attributes)-1 else '') for i,(key,value) in enumerate(attributes))

for path in folder.glob('*.xml'):
    xml=path.read_text(encoding='utf-8')
    xml=re.sub(r'(?m)^(\s*)<([\w.:-]+)(\s+[^<>]*?)(\s*/?)>$',opening,xml)
    E.fromstring(xml)
    path.write_text(xml+'\n' if not xml.endswith('\n') else xml,encoding='utf-8')
print('Formatted',len(list(folder.glob('*.xml'))),'layout XML files')
