from PIL import Image
from pathlib import Path
assets = {
'mascot_settings':'Mint Chat Bubble and Gear Sticker.png',
'category_transfers':'Glossy User Transfer Arrow Icon.png',
'category_recharge':'Glossy Purple Call Smartphone Icon.png',
'category_business':'Glossy Blue Laptop Sticker.png',
'category_shopping':'Glossy Yellow and Cyan Shopping Bags.png',
'category_fuel':'Glossy Blue Gas Pump Icon.png',
'category_food':'Cheerful Cartoon Burger Icon.png',
'mascot_home':'Cheerful Waving Pink Mascot.png',
'mascot_welcome':'Cheerful Yellow Emoji with Smartphone.png',
'mascot_sms':'Glossy Purple Mascot with SMS Phone.png',
}
out=Path('app/src/main/res/drawable-nodpi')
for key,name in assets.items():
    im=Image.open(Path('C:/Users/moham/Downloads')/name).convert('RGBA')
    bbox=im.getchannel('A').point(lambda p: 255 if p > 24 else 0).getbbox()
    if bbox: im=im.crop(bbox)
    im.thumbnail((900,900),Image.Resampling.LANCZOS)
    im.save(out/(key+'.webp'), 'WEBP', quality=94, method=6)
    print(key, im.size)
