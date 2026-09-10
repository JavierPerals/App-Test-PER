"""Create the bundled original geometric animations. Requires Pillow with WebP support.
Not part of the Android build; replace the three WebP files directly if desired.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import math

destination = Path(__file__).resolve().parents[1] / 'app/src/main/assets'
destination.mkdir(parents=True, exist_ok=True)
for mood in ('sad', 'good', 'celebration'):
    frames = []
    for frame in range(24):
        image = Image.new('RGBA', (192, 192))
        draw = ImageDraw.Draw(image)
        phase = frame / 24 * math.tau
        y = round(6 * math.sin(phase))
        color = {'sad': '#9AB8D2', 'good': '#6CC9AE', 'celebration': '#FFD16A'}[mood]
        draw.ellipse((38, 155, 154, 169), fill=(21, 101, 192, 35))
        draw.ellipse((37, 37+y, 155, 155+y), fill=color, outline='#204D70', width=4)
        # A small sailor hat keeps the illustrations consistent with the app.
        draw.rounded_rectangle((51, 22+y, 141, 56+y), radius=12, fill='#EDF5FC', outline='#204D70', width=3)
        draw.rectangle((49, 48+y, 143, 59+y), fill='#1565C0')
        blink = frame in (11, 12)
        for x in (73, 119):
            if blink:
                draw.line((x-5, 89+y, x+5, 89+y), fill='#204D70', width=4)
            else:
                draw.ellipse((x-4, 82+y, x+4, 94+y), fill='#204D70')
        if mood == 'sad':
            draw.arc((76, 110+y, 116, 134+y), 190, 350, fill='#204D70', width=4)
            ty = 100 + ((frame*2) % 28) + y
            draw.ellipse((122, ty, 129, ty+11), fill='#2D8BDD')
        else:
            draw.arc((73, 95+y, 119, 128+y), 5, 175, fill='#204D70', width=4)
        if mood == 'celebration':
            for i in range(10):
                x = 12 + i*18
                cy = (i*37+frame*3) % 155
                if 32 < x < 160 and cy > 25:
                    continue
                shade = ('#EF7582', '#42A5F5', '#65CBA5')[i % 3]
                draw.line((x, cy, x+4, cy+7), fill=shade, width=4)
        frames.append(image)
    path = destination / f'result_{mood}.webp'
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=70, loop=0, lossless=True)
    with Image.open(path) as check:
        assert check.n_frames > 1 and check.info.get('loop') == 0
        print(path.name, check.size, check.n_frames, path.stat().st_size)
