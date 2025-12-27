package me.cortex.neovoxy.common.voxelization;

import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.common.world.other.Mapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Factory for converting Minecraft chunks into voxelized LOD sections.
 * 
 * <p>Processes chunk sections and extracts:
 * <ul>
 *   <li>Block state IDs</li>
 *   <li>Biome IDs</li>
 *   <li>Light levels</li>
 * </ul>
 */
public class WorldConversionFactory {
    
    private final Mapper mapper;
    
    // Cache for reusing during conversion
    private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(Cache::new);
    
    public WorldConversionFactory(Mapper mapper) {
        this.mapper = mapper;
    }
    
    /**
     * Convert a chunk section to voxelized data.
     * 
     * @param chunk The chunk containing the section
     * @param sectionY The section Y coordinate
     * @return Voxelized section data, or null if section is empty
     */
    public VoxelizedSection convertSection(LevelChunk chunk, int sectionY) {
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
        
        // Skip empty sections
        if (section == null || section.hasOnlyAir()) {
            return null;
        }
        
        Cache cache = CACHE.get();
        cache.reset();
        
        int baseY = sectionY << 4;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        // Iterate through all blocks in the section
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = section.getBlockState(x, y, z);
                    
                    if (state.isAir()) {
                        continue;
                    }
                    
                    int stateId = mapper.getStateId(state);
                    
                    // Get biome
                    pos.set(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                    Biome biome = chunk.getLevel().getBiome(pos).value();
                    int biomeId = mapper.getBiomeId(biome);
                    
                    // Get light level
                    int blockLight = chunk.getLevel().getBrightness(LightLayer.BLOCK, pos);
                    int skyLight = chunk.getLevel().getBrightness(LightLayer.SKY, pos);
                    int light = (skyLight << 4) | blockLight;
                    
                    // Store in cache
                    int idx = (y << 8) | (z << 4) | x;
                    cache.stateIds[idx] = stateId;
                    cache.biomeIds[idx] = biomeId;
                    cache.lightLevels[idx] = light;
                    cache.blockCount++;
                }
            }
        }
        
        if (cache.blockCount == 0) {
            return null;
        }
        
        return new VoxelizedSection(
            chunk.getPos().x, sectionY, chunk.getPos().z,
            cache.stateIds.clone(),
            cache.biomeIds.clone(),
            cache.lightLevels.clone(),
            cache.blockCount
        );
    }
    
    /**
     * Thread-local cache for conversion to avoid allocations.
     */
    private static class Cache {
        final int[] stateIds = new int[4096];
        final int[] biomeIds = new int[4096];
        final int[] lightLevels = new int[4096];
        int blockCount = 0;
        
        void reset() {
            blockCount = 0;
            // Arrays don't need to be cleared since we track blockCount
        }
    }
}
