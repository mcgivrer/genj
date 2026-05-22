package com.core.editor.io.json;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for JSON serialization of {@link com.core.entity.Entity}.
 * Stores all relevant entity state: position, velocity, rotation, size, colors, physics, behaviors, and children.
 *
 * <p>For deserialization, a new Entity instance is created and populated with the stored properties.
 * Child entities are recursively reconstructed.</p>
 */
public class JsonEntity {
    public long id = 0;
    public String name = "entity";
    public String type = "GameObject"; // "World", "GameObject", "ParticleSystem", etc.

    // Position and velocity
    public float x = 0.0f;
    public float y = 0.0f;
    public float vx = 0.0f;
    public float vy = 0.0f;

    // Rotation
    public float rotation = 0.0f;
    public float angularVelocity = 0.0f;

    // Physics
    public float mass = 1.0f;
    public String physicsType = "DYNAMIC"; // enum name
    public JsonMaterial material = new JsonMaterial();

    // Dimensions and rendering
    public int width = 0;
    public int height = 0;
    public JsonColor color = new JsonColor();
    public JsonColor fillColor = new JsonColor();
    public int renderPriority = 0;

    // State
    public boolean active = true;

    // Type-specific: TextObject fields
    public String text = "";
    public float fontSize = 14f;
    public boolean hud = false;

    // Composition
    public List<JsonBehavior> behaviors = new ArrayList<>();
    public List<JsonEntity> children = new ArrayList<>();

    public JsonEntity() {
    }

    public JsonEntity(String type, String name) {
        this.type = type;
        this.name = name;
    }
}
