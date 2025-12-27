package me.cortex.neovoxy.client.core.rendering;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Viewport configuration for LOD rendering.
 * 
 * <p>Contains camera matrices, frustum planes, and viewport dimensions
 * needed for culling and rendering.
 */
public class Viewport {
    
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private final Matrix4f mvp = new Matrix4f();
    private final Matrix4f vanillaProjection = new Matrix4f();
    
    private double cameraX, cameraY, cameraZ;
    private int width, height;
    
    // Frustum planes (6 planes, each as vec4: normal.xyz, distance)
    private final float[] frustumPlanes = new float[24];
    
    public Viewport() {}
    
    /**
     * Set projection matrix.
     */
    public Viewport setProjection(Matrix4fc projection) {
        this.projection.set(projection);
        updateMVP();
        return this;
    }
    
    /**
     * Set model-view matrix.
     */
    public Viewport setModelView(Matrix4fc modelView) {
        this.modelView.set(modelView);
        updateMVP();
        return this;
    }
    
    /**
     * Set vanilla projection (for UI alignment).
     */
    public Viewport setVanillaProjection(Matrix4fc projection) {
        this.vanillaProjection.set(projection);
        return this;
    }
    
    /**
     * Set camera position.
     */
    public Viewport setCameraPosition(double x, double y, double z) {
        this.cameraX = x;
        this.cameraY = y;
        this.cameraZ = z;
        return this;
    }
    
    /**
     * Set viewport dimensions.
     */
    public Viewport setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }
    
    private void updateMVP() {
        projection.mul(modelView, mvp);
        extractFrustumPlanes();
    }
    
    private void extractFrustumPlanes() {
        // Extract frustum planes from MVP matrix
        // Left plane
        frustumPlanes[0] = mvp.m03() + mvp.m00();
        frustumPlanes[1] = mvp.m13() + mvp.m10();
        frustumPlanes[2] = mvp.m23() + mvp.m20();
        frustumPlanes[3] = mvp.m33() + mvp.m30();
        
        // Right plane
        frustumPlanes[4] = mvp.m03() - mvp.m00();
        frustumPlanes[5] = mvp.m13() - mvp.m10();
        frustumPlanes[6] = mvp.m23() - mvp.m20();
        frustumPlanes[7] = mvp.m33() - mvp.m30();
        
        // Bottom plane
        frustumPlanes[8] = mvp.m03() + mvp.m01();
        frustumPlanes[9] = mvp.m13() + mvp.m11();
        frustumPlanes[10] = mvp.m23() + mvp.m21();
        frustumPlanes[11] = mvp.m33() + mvp.m31();
        
        // Top plane
        frustumPlanes[12] = mvp.m03() - mvp.m01();
        frustumPlanes[13] = mvp.m13() - mvp.m11();
        frustumPlanes[14] = mvp.m23() - mvp.m21();
        frustumPlanes[15] = mvp.m33() - mvp.m31();
        
        // Near plane
        frustumPlanes[16] = mvp.m03() + mvp.m02();
        frustumPlanes[17] = mvp.m13() + mvp.m12();
        frustumPlanes[18] = mvp.m23() + mvp.m22();
        frustumPlanes[19] = mvp.m33() + mvp.m32();
        
        // Far plane
        frustumPlanes[20] = mvp.m03() - mvp.m02();
        frustumPlanes[21] = mvp.m13() - mvp.m12();
        frustumPlanes[22] = mvp.m23() - mvp.m22();
        frustumPlanes[23] = mvp.m33() - mvp.m32();
        
        // Normalize planes
        for (int i = 0; i < 6; i++) {
            int idx = i * 4;
            float length = (float) Math.sqrt(
                frustumPlanes[idx] * frustumPlanes[idx] +
                frustumPlanes[idx + 1] * frustumPlanes[idx + 1] +
                frustumPlanes[idx + 2] * frustumPlanes[idx + 2]
            );
            if (length > 0) {
                frustumPlanes[idx] /= length;
                frustumPlanes[idx + 1] /= length;
                frustumPlanes[idx + 2] /= length;
                frustumPlanes[idx + 3] /= length;
            }
        }
    }
    
    // Getters
    
    public Matrix4f getProjection() {
        return projection;
    }
    
    public Matrix4f getModelView() {
        return modelView;
    }
    
    public Matrix4f getMVP() {
        return mvp;
    }
    
    public double getCameraX() {
        return cameraX;
    }
    
    public double getCameraY() {
        return cameraY;
    }
    
    public double getCameraZ() {
        return cameraZ;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public float[] getFrustumPlanes() {
        return frustumPlanes;
    }
}
