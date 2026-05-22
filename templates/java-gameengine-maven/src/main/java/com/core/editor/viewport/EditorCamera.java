package com.core.editor.viewport;

/**
 * Manages the camera transformation for the editor viewport.
 * Tracks camera position (world-space) and zoom level.
 *
 * <p>Provides world-to-screen and screen-to-world coordinate transformations.</p>
 */
public class EditorCamera {
    private float x = 0.0f;
    private float y = 0.0f;
    private float zoom = 1.0f;

    // Viewport dimensions (in screen pixels)
    private int viewportWidth = 800;
    private int viewportHeight = 600;

    public EditorCamera() {
    }

    public EditorCamera(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    /**
     * Sets the camera position in world space.
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the zoom level (1.0 = no zoom, 0.5 = zoom out, 2.0 = zoom in).
     */
    public void setZoom(float zoom) {
        this.zoom = Math.max(0.1f, Math.min(5.0f, zoom));
    }

    /**
     * Adjusts zoom multiplicatively (e.g., 1.1 to zoom in 10%, 0.9 to zoom out).
     */
    public void zoomBy(float factor) {
        setZoom(zoom * factor);
    }

    /**
     * Pans the camera (translates in world space).
     */
    public void pan(float dx, float dy) {
        this.x += dx / zoom;
        this.y += dy / zoom;
    }

    /**
     * Frames the camera to show a world-space rectangle.
     *
     * @param wx world-space left edge
     * @param wy world-space top edge
     * @param ww world-space width
     * @param wh world-space height
     */
    public void frame(float wx, float wy, float ww, float wh) {
        if (ww > 0 && wh > 0) {
            float zoomX = viewportWidth / ww;
            float zoomY = viewportHeight / wh;
            zoom = Math.min(zoomX, zoomY) * 0.9f; // 90% to leave margin
            x = wx + ww / 2.0f;
            y = wy + wh / 2.0f;
        }
    }

    /**
     * Converts world-space coordinates to screen-space.
     */
    public float worldToScreenX(float worldX) {
        return (worldX - x) * zoom + viewportWidth / 2.0f;
    }

    public float worldToScreenY(float worldY) {
        return (worldY - y) * zoom + viewportHeight / 2.0f;
    }

    /**
     * Converts screen-space coordinates to world-space.
     */
    public float screenToWorldX(float screenX) {
        return (screenX - viewportWidth / 2.0f) / zoom + x;
    }

    public float screenToWorldY(float screenY) {
        return (screenY - viewportHeight / 2.0f) / zoom + y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZoom() {
        return zoom;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }
}
