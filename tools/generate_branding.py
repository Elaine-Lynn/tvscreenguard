"""Export inherited TV vector with teal colours + shield; create localized TV banners."""
from pathlib import Path
import xml.etree.ElementTree as ET
import subprocess
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parents[1];res=root/'app/src/main/res';out=root/'output/artwork';out.mkdir(parents=True,exist_ok=True)
svg=(root/'artwork/icon.svg').read_text()
(out/'tv-guard-icon.svg').write_text(svg)
subprocess.run(['qlmanage','-t','-s','1024','-o',str(out),str(out/'tv-guard-icon.svg')],check=True,stdout=subprocess.DEVNULL)
icon=Image.open(out/'tv-guard-icon.svg.png').convert('RGBA').resize((512,512),Image.Resampling.LANCZOS);icon.save(out/'tv-guard-icon-512.png')
font_en='/System/Library/Fonts/Supplemental/Arial Bold.ttf'
fonts=list(Path('/System/Library/AssetsV2').rglob('PingFang.ttc'));font_zh=str(fonts[0]) if fonts else font_en
for locale,name,font in [('en','TV ScreenGuard',font_en),('zh','电视防沉迷系统',font_zh)]:
 im=Image.new('RGB',(1280,720),'#102D30');im.paste(icon.resize((430,430)),(425,50));draw=ImageDraw.Draw(im);f=ImageFont.truetype(font,74 if locale=='zh' else 64);draw.text((640,552),name,font=f,fill='#F8FAFC',anchor='mm');im.save(out/f'tv-guard-banner-{locale}.png')
 folder=res/('drawable-nodpi' if locale=='en' else 'drawable-zh-rCN-nodpi');folder.mkdir(exist_ok=True);im.resize((320,180),Image.Resampling.LANCZOS).save(folder/'banner.png')
(res/'drawable/banner.xml').unlink(missing_ok=True)
# Legacy icon for TV launchers that do not consume adaptive icons.
for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
 folder=res/('mipmap-'+density);folder.mkdir(exist_ok=True);icon.resize((size,size),Image.Resampling.LANCZOS).save(folder/'ic_launcher.png')
(res/'mipmap-anydpi/ic_launcher.xml').unlink(missing_ok=True)
print('Wrote vector preview, raster launcher icons and English/Chinese TV banners.')
