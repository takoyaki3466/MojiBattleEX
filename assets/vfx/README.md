# Combat VFX

`combat-vfx-sheet.png` was generated with the built-in OpenAI image generation tool and then deterministically cropped into `effect-00.png` through `effect-15.png`.

Final generation prompt:

> Create a transparent 4-by-4 sprite sheet containing sixteen distinct polished anime arcade fighting-game effects: shockwaves, starbursts, electric arcs, speed streaks, magic rings, impact flashes, fiery bursts, glittering particles, and cosmic rainbow energy. One centered effect per uniform cell, generous transparent padding, emissive additive glow, no characters, letters, numbers, logos, watermark, grid lines, or opaque backdrop.

The individual sprites are loaded once by `ArenaRenderer`, rendered with additive blending, and reused for projectile auras, hit flashes, ultimate warnings, and impacts.

Movement trails and stun indicators also reuse these sprites; larger moving and falling attacks use the separate images in `assets/objects`.
