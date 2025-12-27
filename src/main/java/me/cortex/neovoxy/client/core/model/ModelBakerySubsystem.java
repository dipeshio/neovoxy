package me.cortex.neovoxy.client.core.model;

import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.common.world.other.Mapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Subsystem for baking block models into LOD-compatible format.
 * 
 * <p>
 * Converts Minecraft's BakedModel quads into compact LOD quad data
 * suitable for GPU rendering.
 */
public class ModelBakerySubsystem implements AutoCloseable {

    private final ModelStore store;
    private final Mapper mapper;

    private static final RandomSource RANDOM = RandomSource.create();

    public ModelBakerySubsystem(Mapper mapper) {
        this.mapper = mapper;
        this.store = new ModelStore(1 << 16); // 65536 models max

        Logger.info("ModelBakerySubsystem initialized");
    }

    /**
     * Get the model store.
     */
    public ModelStore getStore() {
        return store;
    }

    /**
     * Add a biome for color lookup.
     */
    public void addBiome(Mapper.BiomeEntry biome) {
        int biomeId = biome.id();
        for (int modelId = 1; modelId < store.getModelCount(); modelId++) {
            // For now, assume grass color for all biome-dependent models
            // In a better implementation, we'd check if the block is foliage or water
            store.uploadColour(modelId, biomeId, biome.getGrassColor());
        }
    }

    /**
     * Bake a block state into model data.
     * 
     * @param state The block state to bake
     * @return Model ID for this state
     */
    public int bakeBlockState(BlockState state) {
        if (state.isAir()) {
            return 0;
        }

        int modelId = store.getModelId(state);

        // Get the baked model from Minecraft
        BlockModelShaper shaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        BakedModel bakedModel = shaper.getBlockModel(state);

        if (bakedModel == null) {
            return modelId;
        }

        int[] faceData = new int[6];
        int flags = 0;

        if (isTranslucent(state)) {
            flags |= 4; // modelIsTranslucent bit
        }

        // Extract face data
        for (Direction dir : Direction.values()) {
            List<BakedQuad> quads = bakedModel.getQuads(state, dir, RANDOM);
            if (!quads.isEmpty()) {
                BakedQuad quad = quads.get(0); // Take the first quad as representative
                int faceIdx = dir.get3DDataValue();

                int faceVal = 0;
                // minU, maxU, minV, maxV (0-15 scale)
                // For simplified LOD, we just use 0-15 (full face)
                faceVal |= (0 & 0xF); // minU
                faceVal |= (15 & 0xF) << 4; // maxU
                faceVal |= (0 & 0xF) << 8; // minV
                faceVal |= (15 & 0xF) << 12; // maxV

                // Indentation (0 for full block faces)
                faceVal |= (0 & 63) << 16;

                if (quad.isTinted()) {
                    faceVal |= (1 << 24); // Tinted
                    flags |= 2; // modelHasBiomeLUT
                }

                faceData[faceIdx] = faceVal;
            }
        }

        // Build the 16 uints (64 bytes)
        int[] modelData = new int[16];
        System.arraycopy(faceData, 0, modelData, 0, 6);
        modelData[6] = flags;
        modelData[7] = 0xFFFFFFFF; // colorTint (white)
        modelData[8] = BuiltInRegistries.BLOCK.getId(state.getBlock()); // customId

        store.uploadModel(modelId, modelData);

        return modelId;
    }

    /**
     * Check if a block state is translucent.
     */
    public boolean isTranslucent(BlockState state) {
        // Check render layer
        return !state.canOcclude();
    }

    @Override
    public void close() {
        store.close();
        Logger.info("ModelBakerySubsystem closed");
    }
}
