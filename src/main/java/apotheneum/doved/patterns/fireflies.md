# Fireflies Pattern

## Concept

The Fireflies pattern creates a dynamic particle system of glowing points that simulate the organic beauty of fireflies on a summer evening. Each firefly is an independent entity with its own movement, glow characteristics, and lifespan.

## Why Not the Firefly Algorithm?

The Firefly algorithm (from optimization theory) is designed to solve mathematical problems by having agents converge on optimal solutions. This is completely different from what we want - a visual pattern of wandering, glowing fireflies.

## Current Implementation

### Population Management
- **Active population control**: Maintains target firefly count by spawning multiple fireflies per frame when under target
- **Aggressive spawning**: Uses deficit-based spawning to quickly reach and maintain the desired quantity
- **Max quantity**: Up to 200 fireflies simultaneously

### Movement Behavior
- **Horizontal bias**: Fireflies favor horizontal movement (±30° from horizontal) for more natural appearance
- **Variable speeds**: Each firefly has individual speed from 0.3x to 1.3x base speed
- **Speed-based lifespan**: Faster fireflies live shorter lives (0.3-0.8s), slower ones live longer (1.8-4.8s)
- **Wandering**: Random direction changes with bias back toward horizontal
- **Door avoidance**: Fireflies redirect away from door areas
- **Edge behavior**: X-coordinates wrap around, Y-coordinates bounce at boundaries

### Glow System
- **Dual oscillation**: Each firefly combines fast primary pulse + slower secondary pulse for organic breathing
- **Individual variation**: Glow speeds vary 50-300% of base rate for diverse flash patterns
- **Adjustable radius**: Glow Size parameter (1.0-6.0 pixels)
- **Adjustable focus**: Glow Focus parameter (1.0-5.0) controls falloff curve
- **Additive blending**: Overlapping glows create brighter areas

### Lifecycle
- **Birth**: Smooth fade-in over first second
- **Life**: Full brightness with complex pulsing
- **Death**: Fade-out over last 2 seconds
- **Lifespan range**: 1.5-4.0 seconds base (modified by speed)

## User Controls

- **Quantity**: Number of fireflies (10-200)
- **Glow Size**: Radius of glow effect (1.0-6.0 pixels)
- **Glow Focus**: Sharpness of glow falloff (1.0-5.0)
- **Clear**: Button to remove all fireflies

## Technical Details

- **Geometry**: Works on both cube (200px rings, 45 high) and cylinder (120px rings, 43 high)
- **Surfaces**: Renders on both exterior and interior simultaneously
- **Performance**: Optimized for 60+ FPS with up to 200 fireflies
- **Colors**: Monochrome white for external colorization through Chromatik effects

## Visual Characteristics

The pattern creates a living, organic display where fireflies:
- Drift horizontally across the 40-foot installation
- Flash with complex, overlapping rhythms
- Appear and disappear dynamically
- Create natural clustering through additive glow blending
- Maintain population density through intelligent spawning