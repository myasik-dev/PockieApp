from pathlib import Path
from PIL import Image, ImageDraw
names=['home','transactions','categories']
out=Image.new('RGB',(936,740),'#eeeeF2')
for i,name in enumerate(names):
    im=Image.open(Path('artifacts/screenshots')/(name+'.png')).convert('RGB')
    im.thumbnail((300,680))
    x=12+i*312
    out.paste(im,(x,40))
    ImageDraw.Draw(out).text((x,14),name.title(),fill='#222222')
out.save('artifacts/Pockie-preview.jpg',quality=94)
