package me.cortex.neovoxy.common.world.other;

import me.cortex.neovoxy.common.Logger;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Maps block states and biomes to compact integer IDs for LOD storage.
 * 
 * <p>
 * Block state IDs are used in the 64-bit quad format (20 bits).
 * Biome IDs are used for color tinting (9 bits).
 */
public class Mapper {

    // Block state mapping
    private final ConcurrentHashMap<BlockState, Integer> stateToId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, BlockState> idToState = new ConcurrentHashMap<>();
    private final AtomicInteger nextStateId = new AtomicInteger(1); // 0 = air

    // Biome mapping
    private final ConcurrentHashMap<Biome, Integer> biomeToId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, BiomeEntry> biomeEntries = new ConcurrentHashMap<>();
    private final AtomicInteger nextBiomeId = new AtomicInteger(0);

    private Consumer<BiomeEntry> biomeCallback;

    public Mapper() {
        Logger.info("Mapper initialized");
    }

    /**
     * Get or create ID for a block state.
     */
    public int getStateId(BlockState state) {
        if (state.isAir()) {
            return 0;
        }

        return stateToId.computeIfAbsent(state, s -> {
            int id = nextStateId.getAndIncrement();
            if (id >= (1 << 20)) {
                Logger.error("Block state ID overflow!");
                return 0;
            }
            idToState.put(id, s);
            return id;
        });
    }

    /**
     * Get block state from ID.
     */
    public BlockState getState(int id) {
        if (id == 0)
            return null;
        return idToState.get(id);
    }

    /**
     * Get or create ID for a biome.
     */
    public int getBiomeId(Biome biome) {
        return biomeToId.computeIfAbsent(biome, b -> {
            int id = nextBiomeId.getAndIncrement();
            if (id >= (1 << 9)) {
                Logger.error("Biome ID overflow!");
                return 0;
            }

            BiomeEntry entry = new BiomeEntry(id, biome);
            biomeEntries.put(id, entry);

            if (biomeCallback != null) {
                biomeCallback.accept(entry);
            }

            return id;
        });
    }

    /**
     * Set callback for new biome registrations.
     */
    public void setBiomeCallback(Consumer<BiomeEntry> callback) {
        this.biomeCallback = callback;
    }

    /**
     * Get all biome entries.
     */
    public BiomeEntry[] getBiomeEntries() {
        return biomeEntries.values().toArray(new BiomeEntry[0]);
    }

    /**
     * Get current block state count.
     */
    public int getStateCount() {
        return nextStateId.get();
    }

    /**
     * Get current biome count.
     */
    public int getBiomeCount() {
        return nextBiomeId.get();
    }

    /**
     * Save mapper data to a binary file.
     */
    public void save(java.nio.file.Path path) throws java.io.IOException {
        try (java.io.DataOutputStream dos = new java.io.DataOutputStream(
                new java.io.BufferedOutputStream(java.nio.file.Files.newOutputStream(path)))) {
            // Save state mapping
            dos.writeInt(nextStateId.get());
            // We can't easily save BlockState directly without registry access,
            // so we'll need to save the ResourceLocation and properties if needed.
            // For now, let's assume we use a simplified approach or placeholder.
            // TODO: Full BlockState serialization

            // Save biome mapping
            dos.writeInt(nextBiomeId.get());
            for (int i = 0; i < nextBiomeId.get(); i++) {
                BiomeEntry entry = biomeEntries.get(i);
                if (entry != null) {
                    dos.writeInt(entry.id());
                    dos.writeUTF(entry.biome().toString()); // Placeholder for biome identification
                }
            }
        }
    }

    /**
     * Load mapper data from a binary file.
     */
    public void load(java.nio.file.Path path) throws java.io.IOException {
        if (!java.nio.file.Files.exists(path))
            return;

        try (java.io.DataInputStream dis = new java.io.DataInputStream(
                new java.io.BufferedInputStream(java.nio.file.Files.newInputStream(path)))) {
            nextStateId.set(dis.readInt());
            // TODO: Load mappings

            nextBiomeId.set(dis.readInt());
            // TODO: Load mappings
        }
    }

    /**
     * Entry representing a mapped biome with color data.
     */
    public record BiomeEntry(int id, Biome biome) {
        /**
         * Get grass color for this biome.
         */
        public int getGrassColor() {
            return biome.getSpecialEffects().getGrassColorOverride().orElse(0x7CBD6B);
        }

        /**
         * Get foliage color for this biome.
         */
        public int getFoliageColor() {
            return biome.getSpecialEffects().getFoliageColorOverride().orElse(0x59AE30);
        }

        /**
         * Get water color for this biome.
         */
        public int getWaterColor() {
            return biome.getSpecialEffects().getWaterColor();
        }
    }

    /**
     * Entry representing a mapped block state.
     */
    public record StateEntry(int id, BlockState state) {
    }
}
