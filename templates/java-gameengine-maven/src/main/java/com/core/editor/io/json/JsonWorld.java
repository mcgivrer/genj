package com.core.editor.io.json;

/**
 * DTO for JSON serialization of {@link com.core.entity.World}.
 * Extends {@link JsonEntity} and adds world-specific properties (gravity).
 */
public class JsonWorld extends JsonEntity {
    public float gravityX = 0.0f;
    public float gravityY = 200.0f;

    public JsonWorld() {
        super("World", "world");
    }
}
