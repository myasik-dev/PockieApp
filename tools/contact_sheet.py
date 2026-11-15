from PIL import Image, ImageOps, ImageDraw
from pathlib import Path
p=Path('artifacts/screenshots')
names=['welcome','permission','home','transactions','categories','settings','details','edit']
canvas=Image.new('RGB',(4*288,2*680),'#e9e9ed')
for i,name in enumerate(names):
    f=p/(name+'.png')
    if not f.exists(): continue
    im=Image.open(f).convert('RGB'); im.thumbnail((276,636))
    x=(i%4)*288+6; y=(i//4)*680+32
    canvas.paste(im,(x,y)); ImageDraw.Draw(canvas).text((x,y-20),name,fill='#333333')
canvas.save('artifacts/screenshots/contact-sheet.jpg',quality=90)
