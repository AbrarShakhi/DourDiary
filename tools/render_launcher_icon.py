from __future__ import annotations

import math
import pathlib

from PIL import Image, ImageDraw

VIEWPORT = 108.0
SAFE_INSET = 18.0
SUPERSAMPLE = 8

BACKGROUND = ((0.0, "#C8431B"), (0.55, "#A22D0F"), (1.0, "#8A2207"))
GRADIENT_FROM = (12.0, 8.0)
GRADIENT_TO = (96.0, 104.0)
ROUTE_COLOUR = "#FFF6F0"
NODE_COLOUR = "#F2CD86"
STROKE = 6.0
MARKER_OUTER = 7.5
MARKER_INNER = 4.8
MARKER_RING = "#FFF6F0"

RES_DIR = pathlib.Path("app/src/main/res")
PLAY_ICON = pathlib.Path("docs/play-store-icon-512.png")
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def _hex(value: str) -> tuple[int, int, int]:
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def _bezier(p0, p1, p2, p3, steps=160):
    points = []
    for index in range(steps + 1):
        t = index / steps
        u = 1 - t
        points.append((
            u * u * u * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t * t * t * p3[0],
            u * u * u * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t * t * t * p3[1],
        ))
    return points


_D1 = dict(stem=30.0, bowl=49.0, top=33.0, base=59.0)
_D2 = dict(stem=60.0, bowl=79.0, top=46.0, base=72.0)
_SHOULDER = 0.62
_D2_STOP = 0.90


def _bowl_curves(d: dict) -> tuple[tuple, tuple]:
    half = (d["base"] - d["top"]) / 2
    shoulder = d["stem"] + (d["bowl"] - d["stem"]) * _SHOULDER
    out = ((shoulder, d["top"]), (d["bowl"], d["top"] + half * 0.5), (d["bowl"], d["top"] + half))
    back = ((d["bowl"], d["base"] - half * 0.5), (shoulder, d["base"]), (d["stem"], d["base"]))
    return out, back


def _reverse(start, curve):
    return (curve[1], curve[0], start)


def _split_cubic(p0, p1, p2, p3, t: float):
    lerp = lambda a, b: (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)
    q0, q1, q2 = lerp(p0, p1), lerp(p1, p2), lerp(p2, p3)
    r0, r1 = lerp(q0, q1), lerp(q1, q2)
    return q0, r0, lerp(r0, r1)


START = (_D1["bowl"], (_D1["top"] + _D1["base"]) / 2)


def segments() -> list[tuple[str, tuple]]:
    d1_out, d1_back = _bowl_curves(_D1)
    d2_out, d2_back = _bowl_curves(_D2)
    d2_top = (_D2["stem"], _D2["top"])
    d2_base = (_D2["stem"], _D2["base"])
    d2_widest = d2_out[2]
    closing = _reverse(d2_top, d2_out)
    return [
        ("C", d1_back),
        ("L", ((_D1["stem"], _D1["top"]),)),
        ("C", d1_out),
        ("L", (d2_top,)),
        ("L", (d2_base,)),
        ("C", _reverse(d2_widest, d2_back)),
        ("C", _split_cubic(d2_widest, *closing, _D2_STOP)),
    ]


MARKER_CENTRE = segments()[-1][1][-1]


def route_points() -> list[tuple[float, float]]:
    cursor = START
    points = [cursor]
    for kind, seg in segments():
        points += _bezier(cursor, *seg)[1:] if kind == "C" else [seg[0]]
        cursor = seg[-1]
    return points


def path_data() -> str:
    parts = [f"M{START[0]:g},{START[1]:g}"]
    for kind, seg in segments():
        parts.append(kind + " ".join(f"{x:g},{y:g}" for x, y in seg))
    return " ".join(parts)


def marker_data() -> str:
    cx, cy = MARKER_CENTRE
    r = MARKER_OUTER
    return f"M{cx + r:g},{cy:g} A{r:g},{r:g} 0 1,1 {cx - r:g},{cy:g} A{r:g},{r:g} 0 1,1 {cx + r:g},{cy:g} Z"


def _gradient(size: int) -> Image.Image:
    image = Image.new("RGB", (size, size))
    pixels = image.load()
    scale = size / VIEWPORT
    ax, ay = GRADIENT_FROM[0] * scale, GRADIENT_FROM[1] * scale
    bx, by = GRADIENT_TO[0] * scale, GRADIENT_TO[1] * scale
    dx, dy = bx - ax, by - ay
    length_squared = dx * dx + dy * dy

    stops = [(offset, _hex(colour)) for offset, colour in BACKGROUND]

    for y in range(size):
        for x in range(size):
            t = ((x - ax) * dx + (y - ay) * dy) / length_squared
            t = min(1.0, max(0.0, t))
            for i in range(len(stops) - 1):
                t0, c0 = stops[i]
                t1, c1 = stops[i + 1]
                if t0 <= t <= t1:
                    local = 0.0 if t1 == t0 else (t - t0) / (t1 - t0)
                    pixels[x, y] = tuple(
                        round(c0[channel] + (c1[channel] - c0[channel]) * local)
                        for channel in range(3)
                    )
                    break
    return image


def _stroke_round(draw: ImageDraw.ImageDraw, points, width: float, colour) -> None:

    radius = width / 2
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        dx, dy = x1 - x0, y1 - y0
        length = math.hypot(dx, dy)
        if length == 0:
            continue
        nx, ny = -dy / length * radius, dx / length * radius
        draw.polygon(
            [(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)],
            fill=colour,
        )
    for x, y in points:
        draw.ellipse([x - radius, y - radius, x + radius, y + radius], fill=colour)


def render(size: int, *, crop_to_safe_zone: bool, circular: bool) -> Image.Image:
    work = size * SUPERSAMPLE
    canvas = _gradient(work).convert("RGBA")
    draw = ImageDraw.Draw(canvas)
    scale = work / VIEWPORT

    _stroke_round(
        draw,
        [(x * scale, y * scale) for x, y in route_points()],
        STROKE * scale,
        _hex(ROUTE_COLOUR),
    )

    cx, cy = MARKER_CENTRE[0] * scale, MARKER_CENTRE[1] * scale
    for radius, colour in ((MARKER_OUTER * scale, MARKER_RING), (MARKER_INNER * scale, NODE_COLOUR)):
        draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=_hex(colour))

    if crop_to_safe_zone:
        inset = round(SAFE_INSET * scale)
        canvas = canvas.crop((inset, inset, work - inset, work - inset))

    if circular:
        mask = Image.new("L", canvas.size, 0)
        ImageDraw.Draw(mask).ellipse([0, 0, canvas.size[0] - 1, canvas.size[1] - 1], fill=255)
        canvas.putalpha(mask)

    return canvas.resize((size, size), Image.LANCZOS)


def main() -> None:
    PLAY_ICON.parent.mkdir(parents=True, exist_ok=True)
    render(512, crop_to_safe_zone=True, circular=False).convert("RGB").save(PLAY_ICON)
    print(f"wrote {PLAY_ICON}")

    for density, size in DENSITIES.items():
        folder = RES_DIR / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        render(size, crop_to_safe_zone=True, circular=False).save(folder / "ic_launcher.png")
        render(size, crop_to_safe_zone=True, circular=True).save(folder / "ic_launcher_round.png")
        print(f"wrote {folder}/ic_launcher.png and ic_launcher_round.png ({size}px)")


if __name__ == "__main__":
    main()
