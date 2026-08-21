"""One-off generator: foundation logo -> adaptive / legacy / Play Store launcher assets."""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
# Logo lives in the sibling website repo under the shared parent folder.
LOGO = ROOT.parent / "autism-allyship" / "assets" / "brand" / "logo.jpg"
RES = ROOT / "app" / "src" / "main" / "res"
MAIN = ROOT / "app" / "src" / "main"

# Adaptive foreground layer is 108dp; legacy launcher icon is 48dp.
DENSITIES_ADAPTIVE = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}
DENSITIES_LEGACY = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def cut_white_background(src: Image.Image) -> Image.Image:
    src = src.convert("RGBA")
    pixels = src.load()
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    op = out.load()
    width, height = src.size
    for y in range(height):
        for x in range(width):
            r, g, b, _a = pixels[x, y]
            if r > 245 and g > 245 and b > 245:
                continue
            op[x, y] = (r, g, b, 255)
    bbox = out.getbbox()
    if bbox is None:
        raise SystemExit("logo had no non-white pixels")
    return out.crop(bbox)


def monochrome_silhouette(mark: Image.Image) -> Image.Image:
    """Filled black silhouette of the mark for themed (monochrome) icons."""
    mp = mark.load()
    width, height = mark.size
    mono = Image.new("RGBA", mark.size, (0, 0, 0, 0))
    mop = mono.load()
    for y in range(height):
        for x in range(width):
            if mp[x, y][3] > 0:
                mop[x, y] = (0, 0, 0, 255)
    return mono


def place_on_canvas(img: Image.Image, canvas_size: int, scale_fraction: float) -> Image.Image:
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    target = int(canvas_size * scale_fraction)
    iw, ih = img.size
    if iw >= ih:
        nw = target
        nh = max(1, int(ih * (target / iw)))
    else:
        nh = target
        nw = max(1, int(iw * (target / ih)))
    resized = img.resize((nw, nh), Image.Resampling.LANCZOS)
    x = (canvas_size - nw) // 2
    y = (canvas_size - nh) // 2
    canvas.paste(resized, (x, y), resized)
    return canvas


def full_icon(img: Image.Image, size: int, scale_fraction: float) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    placed = place_on_canvas(img, size, scale_fraction)
    canvas.paste(placed, (0, 0), placed)
    return canvas.convert("RGB")


def main() -> None:
    mark = cut_white_background(Image.open(LOGO))
    mono = monochrome_silhouette(mark)
    print(f"mark size {mark.size}")

    for density, px in DENSITIES_ADAPTIVE.items():
        ddir = RES / f"drawable-{density}"
        ddir.mkdir(exist_ok=True)
        place_on_canvas(mark, px, 0.62).save(
            ddir / "ic_launcher_foreground.webp", "WEBP", quality=90
        )
        place_on_canvas(mono, px, 0.62).save(
            ddir / "ic_launcher_monochrome.webp", "WEBP", quality=90
        )
        print(f"adaptive layers {density} {px}px")

    for density, px in DENSITIES_LEGACY.items():
        mdir = RES / f"mipmap-{density}"
        icon = full_icon(mark, px, 0.72)
        icon.save(mdir / "ic_launcher.webp", "WEBP", quality=90)
        icon.save(mdir / "ic_launcher_round.webp", "WEBP", quality=90)
        print(f"legacy mipmap-{density} {px}px")

    play_path = MAIN / "ic_launcher-playstore.png"
    full_icon(mark, 512, 0.78).save(play_path, "PNG")
    print(f"wrote {play_path}")


if __name__ == "__main__":
    main()
