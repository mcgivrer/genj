package com.core.editor.io.json;

import com.core.physics.Material;

/**
 * DTO for JSON serialization of {@link com.core.physics.Material}.
 * Stores all material properties: name, density, friction, elasticity, rotationalFriction.
 */
public class JsonMaterial {
    public String name = "default";
    public float density = 1.0f;
    public float friction = 0.2f;
    public float elasticity = 0.4f;
    public float rotationalFriction = 0.3f;

    public JsonMaterial() {
    }

    public JsonMaterial(Material m) {
        if (m != null) {
            this.name = m.name;
            this.density = m.density;
            this.friction = m.friction;
            this.elasticity = m.elasticity;
            this.rotationalFriction = m.rotationalFriction;
        }
    }

    public Material toMaterial() {
        return new Material(name, density, friction, elasticity, rotationalFriction);
    }
}
