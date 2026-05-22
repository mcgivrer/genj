package com.core.editor.io.json;

/**
 * DTO for JSON serialization of {@link com.core.gfx.Camera}.
 * Stores camera position, dimensions, and zoom level.
 */
public class JsonCamera {
    public String name = "camera";
    public float x = 0.0f;
    public float y = 0.0f;
    public int width = 800;
    public int height = 600;
    public float zoom = 1.0f;

    public JsonCamera() {
    }

    public JsonCamera(String name) {
        this.name = name;
    }
}
