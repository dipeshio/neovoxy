# NeoVoxy

A NeoForge port of the [Voxy](https://github.com/MCRcortex/voxy) LOD rendering engine for Minecraft 1.21.1.

## Overview

NeoVoxy is a high-performance Level of Detail (LOD) rendering system that uses GPU-driven hierarchical occlusion culling and quad-based geometry to render distant terrain far beyond vanilla render distance.

### Features

- **GPU-Driven Rendering**: Compute shader-based hierarchical traversal for efficient visibility determination
- **Quad-Based Geometry**: Compact 64-bit packed format for efficient LOD mesh storage
- **Persistent Caching**: RocksDB/LMDB storage backends for instant world loading
- **Embeddium Integration**: Seamless integration with Embeddium rendering pipeline

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.172+
- **Embeddium** (required)
- OpenGL 4.6 compatible GPU

## Installation

1. Install NeoForge 1.21.1
2. Install Embeddium
3. Place `neovoxy-x.x.x.jar` in your mods folder

## Building

```bash
./gradlew build
```

The built JAR will be in `build/libs/`.

## Configuration

Config file: `config/neovoxy.toml`

| Setting | Default | Description |
|---------|---------|-------------|
| `enabled` | true | Master enable/disable |
| `sectionRenderDistance` | 16 | LOD render distance (sections) |
| `subdivisionSize` | 64 | Screen-space LOD quality threshold |
| `serviceThreads` | auto | Background processing threads |

## Credits

- **Cortex** - Original Voxy mod author
- Based on [Voxy](https://github.com/MCRcortex/voxy) (Fabric)

## License

All Rights Reserved (matching original Voxy license)
