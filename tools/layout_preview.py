from PIL import Image,ImageDraw
from pathlib import Path
root=Path('artifacts/layout-migration/screenshots')
out=Image.new('RGB',(936,740),'#e9e9ed')
for i,name in enumerate(['home','categories','edit']):
    path=root/(name+'.png')
    if not path.exists(): continue
    im=Image.open(path).convert('RGB');im.thumbnail((300,680));out.paste(im,(12+i*312,40));ImageDraw.Draw(out).text((12+i*312,14),name,fill='#222222')
out.save(root/'preview.jpg')
