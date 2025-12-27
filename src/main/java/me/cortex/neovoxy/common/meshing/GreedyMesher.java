package me.cortex.neovoxy.common.meshing;

import me.cortex.neovoxy.common.voxelization.VoxelizedSection;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Greedy mesher for generating optimized LOD quads.
 * 
 * <p>Merges adjacent faces with identical properties (block state, biome, light)
 * into larger quads, reducing vertex count significantly.
 */
public class GreedyMesher {
    
    // Quad packed format:
    // Bits 0-2: Face (0-5)
    // Bits 3-7: Width-1 (0-31)
    // Bits 8-12: Height-1 (0-31)
    // Bits 13-17: X position (0-31)
    // Bits 18-22: Y position (0-31)
    // Bits 23-27: Z position (0-31)
    // Bits 28-47: State ID (20 bits)
    // Bits 48-56: Biome ID (9 bits)
    // Bits 57-64: Light level (8 bits)
    
    /**
     * Generate quads from a voxelized section.
     * 
     * @param section Voxelized section data
     * @return Array of packed 64-bit quads
     */
    public long[] mesh(VoxelizedSection section) {
        if (section.isEmpty()) {
            return new long[0];
        }
        
        List<Long> quads = new ArrayList<>();
        
        // Process each face direction
        for (Direction face : Direction.values()) {
            meshFace(section, face, quads);
        }
        
        return quads.stream().mapToLong(Long::longValue).toArray();
    }
    
    private void meshFace(VoxelizedSection section, Direction face, List<Long> quads) {
        int axis = face.getAxis().ordinal();
        boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        
        // Create visited mask
        boolean[][][] visited = new boolean[16][16][16];
        
        // Iterate along the face axis
        for (int d = 0; d < 16; d++) {
            for (int v = 0; v < 16; v++) {
                for (int u = 0; u < 16; u++) {
                    int x, y, z;
                    
                    // Convert uvd coordinates to xyz based on face
                    switch (axis) {
                        case 0 -> { // X axis (EAST/WEST)
                            x = positive ? d : 15 - d;
                            y = v;
                            z = u;
                        }
                        case 1 -> { // Y axis (UP/DOWN)
                            x = u;
                            y = positive ? d : 15 - d;
                            z = v;
                        }
                        default -> { // Z axis (SOUTH/NORTH)
                            x = u;
                            y = v;
                            z = positive ? d : 15 - d;
                        }
                    }
                    
                    if (visited[x][y][z]) continue;
                    if (!section.hasBlock(x, y, z)) continue;
                    
                    // Check if face is exposed
                    if (!isFaceExposed(section, x, y, z, face)) continue;
                    
                    int stateId = section.getStateId(x, y, z);
                    int biomeId = section.getBiomeId(x, y, z);
                    int light = section.getLightLevel(x, y, z);
                    
                    // Find maximum quad size with greedy expansion
                    int width = 1;
                    int height = 1;
                    
                    // Expand along u axis
                    while (u + width < 16) {
                        int nx, ny, nz;
                        switch (axis) {
                            case 0 -> { nx = x; ny = y; nz = z + width; }
                            case 1 -> { nx = x + width; ny = y; nz = z; }
                            default -> { nx = x + width; ny = y; nz = z; }
                        }
                        
                        if (canMerge(section, visited, nx, ny, nz, face, stateId, biomeId, light)) {
                            width++;
                        } else {
                            break;
                        }
                    }
                    
                    // Expand along v axis
                    outer:
                    while (v + height < 16) {
                        for (int wu = 0; wu < width; wu++) {
                            int nx, ny, nz;
                            switch (axis) {
                                case 0 -> { nx = x; ny = y + height; nz = z + wu; }
                                case 1 -> { nx = x + wu; ny = y; nz = z + height; }
                                default -> { nx = x + wu; ny = y + height; nz = z; }
                            }
                            
                            if (!canMerge(section, visited, nx, ny, nz, face, stateId, biomeId, light)) {
                                break outer;
                            }
                        }
                        height++;
                    }
                    
                    // Mark visited
                    for (int vy = 0; vy < height; vy++) {
                        for (int vx = 0; vx < width; vx++) {
                            int mx, my, mz;
                            switch (axis) {
                                case 0 -> { mx = x; my = y + vy; mz = z + vx; }
                                case 1 -> { mx = x + vx; my = y; mz = z + vy; }
                                default -> { mx = x + vx; my = y + vy; mz = z; }
                            }
                            visited[mx][my][mz] = true;
                        }
                    }
                    
                    // Pack quad
                    long quad = packQuad(face.ordinal(), width, height, x, y, z, stateId, biomeId, light);
                    quads.add(quad);
                }
            }
        }
    }
    
    private boolean isFaceExposed(VoxelizedSection section, int x, int y, int z, Direction face) {
        int nx = x + face.getStepX();
        int ny = y + face.getStepY();
        int nz = z + face.getStepZ();
        
        // Face at section boundary is exposed (neighbor section handles occlusion)
        if (nx < 0 || nx >= 16 || ny < 0 || ny >= 16 || nz < 0 || nz >= 16) {
            return true;
        }
        
        // Face is exposed if neighbor is air
        return !section.hasBlock(nx, ny, nz);
    }
    
    private boolean canMerge(VoxelizedSection section, boolean[][][] visited,
                             int x, int y, int z, Direction face,
                             int stateId, int biomeId, int light) {
        if (x < 0 || x >= 16 || y < 0 || y >= 16 || z < 0 || z >= 16) return false;
        if (visited[x][y][z]) return false;
        if (!section.hasBlock(x, y, z)) return false;
        if (!isFaceExposed(section, x, y, z, face)) return false;
        
        return section.getStateId(x, y, z) == stateId
            && section.getBiomeId(x, y, z) == biomeId
            && section.getLightLevel(x, y, z) == light;
    }
    
    private long packQuad(int face, int width, int height, int x, int y, int z,
                          int stateId, int biomeId, int light) {
        return (long)(face & 0x7)
             | ((long)(width - 1) & 0x1F) << 3
             | ((long)(height - 1) & 0x1F) << 8
             | ((long)(x & 0x1F)) << 13
             | ((long)(y & 0x1F)) << 18
             | ((long)(z & 0x1F)) << 23
             | ((long)(stateId & 0xFFFFF)) << 28
             | ((long)(biomeId & 0x1FF)) << 48
             | ((long)(light & 0xFF)) << 57;
    }
}
