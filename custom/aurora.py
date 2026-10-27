import sys, math
import numpy as np
W, H, FPS, SECONDS = 960, 540, 24, 40
N = FPS * SECONDS
bg = np.array([5, 6, 10], np.float32)
blobs = [
    ((27, 42, 107), 0.42, (1, 2), (0.0, 1.0)),
    ((58, 28, 92), 0.36, (2, 1), (2.1, 0.4)),
    ((14, 74, 74), 0.34, (1, 1), (4.2, 2.6)),
    ((34, 38, 79), 0.30, (1, 3), (1.3, 5.0)),
]
yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
xx /= W; yy /= H
rng = np.random.default_rng(1)
out = sys.stdout.buffer
for f in range(N):
    t = 2 * math.pi * f / N
    img = np.broadcast_to(bg, (H, W, 3)).copy()
    for color, r, (fx, fy), (px, py) in blobs:
        cx = 0.5 + 0.36 * math.sin(fx * t + px)
        cy = 0.5 + 0.32 * math.sin(fy * t + py)
        d2 = ((xx - cx) * 16 / 9) ** 2 + (yy - cy) ** 2
        w = np.exp(-d2 / (2 * (r / 2) ** 2))[..., None] * 0.75
        img += (np.array(color, np.float32) - bg) * w
    img += rng.uniform(-0.5, 0.5, img.shape).astype(np.float32)
    out.write(np.clip(img, 0, 255).astype(np.uint8).tobytes())
