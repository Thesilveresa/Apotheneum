# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture

### Core Components

The codebase is structured around the LX lighting framework with these key classes:

- **Apotheneum.java**: Main model class defining the physical LED structure
  - `Cube` class: Represents the outer cubic chamber (4 faces, 50×45 grid each)
  - `Cylinder` class: Represents the inner cylindrical chamber (120 columns, 43 pixels high)
  - Both have `Orientation` subclasses for exterior/interior surfaces
- **ApotheneumPattern.java**: Base class for all visual effects
- **ApotheneumEffect.java**: Base class for effects/filters
- **ApotheneumRasterPattern.java**: Base class for raster-based patterns

### Pattern Class Hierarchy

The codebase uses a three-tier inheritance structure:

**LXPattern** (LX Studio framework base)
├── **ApotheneumPattern** (abstract base for Apotheneum-specific patterns)
│   ├── **ApotheneumRasterPattern** (abstract base for 2D raster patterns)
│   │   └── Most patterns (26 total) - geometry-aware patterns
│   └── **RasterOval** (1 pattern) - 2D graphics patterns
└── **Direct LXPattern** (2 patterns) - general 3D or specialized patterns

### Pattern Organization

Patterns are organized by author in separate packages:
- `apotheneum.mcslee.*` - Core patterns by Mark C. Slee (16 patterns)
- `apotheneum.thesilveresa.*` - Patterns by The Silver Era (13 patterns)
- `apotheneum.doved.*` - Patterns by Doved (1 pattern)
- `apotheneum.examples.*` - Example patterns (2 patterns)