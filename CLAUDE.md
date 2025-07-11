# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Apotheneum is a visual, sonic and haptic instrument for immersive LED art installations. It consists of two nested chambers (cube and cylinder) with 13,280 LED nodes total, built on the Chromatik Digital Lighting Workstation framework.

## Build Commands

- **Build**: `mvn compile` - Compiles the Java source code
- **Install**: `mvn -Pinstall install` - Builds and installs the package to ~/Chromatik/Packages
- **Quick Install**: `./update.command` - Convenience script that runs `mvn install`

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

### Key Constants

- `GRID_WIDTH = 50`, `GRID_HEIGHT = 45` - Cube face dimensions
- `CYLINDER_HEIGHT = 43` - Cylinder height
- `DOOR_WIDTH = 10`, `DOOR_HEIGHT = 11` - Door cutout dimensions
- `RING_LENGTH = 120` (cylinder) or `200` (cube) - Ring circumference

### Physical Layout

The installation has doors that affect pixel availability:
- Cube doors start at column 20 on each face
- Cylinder doors start at column 10
- Use `orientation.available(columnIndex)` to get available pixels per column

## Development Patterns

### Choosing the Right Base Class

**Extend ApotheneumPattern when:**
- Pattern needs Apotheneum-specific geometry utilities
- Pattern works with cube faces or cylinder orientations
- Pattern needs to copy between exterior/interior surfaces
- Examples: Raindrops, Quilt, CubeBlinks

**Extend ApotheneumRasterPattern when:**
- Pattern needs 2D graphics rendering (Graphics2D, BufferedImage)
- Pattern benefits from pixel-based approach with face controls
- Example: RasterOval

**Extend LXPattern directly when:**
- Pattern doesn't need Apotheneum-specific utilities
- Pattern uses general 3D geometry or specialized components
- Examples: StripePattern (3D geometry), DeformableImagePattern (image rendering)

### ApotheneumPattern Features

- **Model Safety**: Only renders when `Apotheneum.exists` is true
- **Automatic Initialization**: Calls `Apotheneum.initialize(lx)` in constructor
- **Geometry Utilities**: 
  - `copyCubeFace(face)` - Copy one face to all cube faces
  - `copyExterior()` - Mirror exterior surfaces to interior
  - `copyCylinderExterior()` - Copy cylinder exterior to interior
  - `copyMirror(from, to)` - Mirror copy with column reversal
  - `setApotheneumColor(color)` - Set entire installation to one color

### Working with Geometry

- Access points via `orientation.point(x, y)` or `orientation.column(x).points[y]`
- Use `Ring` objects for circular operations around cube/cylinder
- Faces are ordered: front, right, back, left (clockwise when viewed from above)

### Common Utilities

- `copyCubeFace(face)` - Copy one face to all cube faces
- `copyExterior()` - Mirror exterior surfaces to interior
- `setApotheneumColor(color)` - Set entire installation to one color

## Dependencies

- Java 21+ required
- Maven for build management
- LX Framework (Chromatik) - provided dependency
- No external testing framework configured

## File Structure

- `src/main/java/apotheneum/` - Main source code
- `src/main/resources/` - Assets (fixtures, images, project files)
- `scripts/` - PHP utility scripts for fixture generation
- `target/` - Build output (ignored by git)