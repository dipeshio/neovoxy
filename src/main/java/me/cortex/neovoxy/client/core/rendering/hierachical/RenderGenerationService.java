package me.cortex.neovoxy.client.core.rendering.hierachical;

import me.cortex.neovoxy.client.core.model.ModelBakerySubsystem;
import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.common.thread.ServiceManager;
import me.cortex.neovoxy.common.voxelization.VoxelizedSection;
import me.cortex.neovoxy.common.world.WorldEngine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for generating LOD render data from voxelized sections.
 * 
 * <p>
 * Runs on background threads and converts VoxelizedSection data
 * into quad geometry suitable for GPU rendering.
 */
public class RenderGenerationService {

    private final WorldEngine worldEngine;
    private final ModelBakerySubsystem modelBakery;
    private final ServiceManager serviceManager;
    private final boolean useMeshlets;

    private final BlockingQueue<GenerationTask> taskQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public RenderGenerationService(WorldEngine worldEngine, ModelBakerySubsystem modelBakery,
            ServiceManager serviceManager, boolean useMeshlets) {
        this.worldEngine = worldEngine;
        this.modelBakery = modelBakery;
        this.serviceManager = serviceManager;
        this.useMeshlets = useMeshlets;

        Logger.info("RenderGenerationService created (meshlets: {})", useMeshlets);
    }

    /**
     * Start the generation service.
     */
    public void start() {
        isRunning.set(true);

        // Start worker threads
        int workerCount = Math.max(1, serviceManager.getThreadCount() / 2);
        for (int i = 0; i < workerCount; i++) {
            serviceManager.submit(this::workerLoop);
        }

        Logger.info("RenderGenerationService started with {} workers", workerCount);
    }

    /**
     * Stop the generation service.
     */
    public void stop() {
        isRunning.set(false);
        taskQueue.clear();
    }

    /**
     * Queue a section for mesh generation.
     */
    public void queueGeneration(VoxelizedSection section, GenerationCallback callback) {
        if (!isRunning.get())
            return;

        taskQueue.offer(new GenerationTask(section, callback));
    }

    private void workerLoop() {
        while (isRunning.get()) {
            try {
                GenerationTask task = taskQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (task != null) {
                    processTask(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Logger.error("Error in render generation worker", e);
            }
        }
    }

    private void processTask(GenerationTask task) {
        VoxelizedSection section = task.section();

        // Generate quads using greedy meshing
        long[] quads = generateQuads(section);

        if (quads.length > 0) {
            task.callback().onComplete(section, quads);
        }
    }

    /**
     * Generate quads from voxelized section using greedy meshing.
     */
    private long[] generateQuads(VoxelizedSection section) {
        if (section.isEmpty()) {
            return new long[0];
        }

        java.util.List<Long> quads = new java.util.ArrayList<>();

        // Iterate over 6 directions
        for (int face = 0; face < 6; face++) {
            int axis = face / 2; // 0:Y, 1:Z, 2:X
            int direction = (face % 2) == 0 ? -1 : 1;

            for (int s = 0; s < 16; s++) {
                // mask[u][v] stores properties of visible face
                long[] mask = new long[16 * 16];
                boolean hasAny = false;

                for (int u = 0; u < 16; u++) {
                    for (int v = 0; v < 16; v++) {
                        int x, y, z;
                        if (axis == 0) { // Y-axis slice
                            x = u;
                            y = s;
                            z = v;
                        } else if (axis == 1) { // Z-axis slice
                            x = u;
                            y = v;
                            z = s;
                        } else { // X-axis slice
                            x = s;
                            y = u;
                            z = v;
                        }

                        int stateId = section.getStateId(x, y, z);
                        if (stateId == 0)
                            continue;

                        // Check if face is visible (neighbor is air or out of bounds)
                        int nx = x + (axis == 2 ? direction : 0);
                        int ny = y + (axis == 0 ? direction : 0);
                        int nz = z + (axis == 1 ? direction : 0);

                        boolean visible = false;
                        if (nx < 0 || nx >= 16 || ny < 0 || ny >= 16 || nz < 0 || nz >= 16) {
                            visible = true; // Section boundary
                        } else {
                            if (!section.hasBlock(nx, ny, nz)) {
                                visible = true;
                            }
                        }

                        if (visible) {
                            int biomeId = section.getBiomeId(x, y, z);
                            int lightId = section.getLightLevel(x, y, z);
                            // Combine properties into a single key for greedy matching
                            mask[u << 4 | v] = ((long) stateId << 32) | ((long) biomeId << 16) | lightId;
                            hasAny = true;
                        }
                    }
                }

                if (!hasAny)
                    continue;

                // Greedy meshing on the 16x16 mask
                boolean[] visited = new boolean[16 * 16];
                for (int u = 0; u < 16; u++) {
                    for (int v = 0; v < 16; v++) {
                        int idx = u << 4 | v;
                        if (mask[idx] != 0 && !visited[idx]) {
                            long key = mask[idx];
                            int w = 1, h = 1;

                            // Expand width
                            for (int wu = u + 1; wu < 16; wu++) {
                                int nidx = wu << 4 | v;
                                if (mask[nidx] == key && !visited[nidx])
                                    w++;
                                else
                                    break;
                            }

                            // Expand height
                            outer: for (int hv = v + 1; hv < 16; hv++) {
                                for (int wu = u; wu < u + w; wu++) {
                                    int nidx = wu << 4 | hv;
                                    if (mask[nidx] != key || visited[nidx])
                                        break outer;
                                }
                                h++;
                            }

                            // Mark as visited
                            for (int hv = v; hv < v + h; hv++) {
                                for (int wu = u; wu < u + w; wu++) {
                                    visited[wu << 4 | hv] = true;
                                }
                            }

                            // Output quad
                            int stateId = (int) (key >> 32);
                            int biomeId = (int) ((key >> 16) & 0xFFFF);
                            int lightId = (int) (key & 0xFFFF);

                            // Re-calculate local coords based on axis
                            int qx, qy, qz;
                            if (axis == 0) {
                                qx = u;
                                qy = s;
                                qz = v;
                            } else if (axis == 1) {
                                qx = u;
                                qy = v;
                                qz = s;
                            } else {
                                qx = s;
                                qy = u;
                                qz = v;
                            }

                            quads.add(packQuad(face, w, h, qx, qy, qz, stateId, biomeId, lightId));
                        }
                    }
                }
            }
        }

        return quads.stream().mapToLong(Long::longValue).toArray();
    }

    private long packQuad(int face, int sx, int sy, int x, int y, int z, int stateId, int biomeId, int lightId) {
        long quad = 0;
        quad |= (face & 0x7L); // 0-2
        quad |= ((sx - 1) & 0xFL) << 3; // 3-6
        quad |= ((sy - 1) & 0xFL) << 7; // 7-10
        quad |= (z & 0x1FL) << 11; // 11-15
        quad |= (y & 0x1FL) << 16; // 16-20
        quad |= (x & 0x1FL) << 21; // 21-25
        quad |= (stateId & 0xFFFFFL) << 26; // 26-45 (20 bits)
        quad |= (biomeId & 0x1FFL) << 46; // 46-54 (9 bits)
        quad |= (lightId & 0xFFL) << 55; // 55-62 (8 bits)
        return quad;
    }

    /**
     * Get pending task count.
     */
    public int getPendingCount() {
        return taskQueue.size();
    }

    /**
     * Task for mesh generation.
     */
    private record GenerationTask(VoxelizedSection section, GenerationCallback callback) {
    }

    /**
     * Callback for completed mesh generation.
     */
    @FunctionalInterface
    public interface GenerationCallback {
        void onComplete(VoxelizedSection section, long[] quads);
    }
}
