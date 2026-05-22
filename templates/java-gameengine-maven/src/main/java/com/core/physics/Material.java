package com.core.physics;

public class Material {
    public final String name;
    public final float density;
    public final float friction;
    public final float elasticity;
    /** Rotational damping factor in [0, 1]. Higher values damp angular velocity faster. */
    public final float rotationalFriction;

    public static final Material DEFAULT = new Material("default", 1.0f, 0.2f, 0.4f, 0.3f);
    public static final Material WOOD    = new Material("wood",    0.6f, 0.5f, 0.2f, 0.4f);
    public static final Material METAL   = new Material("metal",   2.7f, 0.2f, 0.1f, 0.15f);
    public static final Material RUBBER  = new Material("rubber",  1.1f, 0.9f, 0.85f, 0.8f);
    public static final Material ICE     = new Material("ice",     0.9f, 0.02f, 0.05f, 0.02f);
    public static final Material STONE   = new Material("stone",   2.4f, 0.7f, 0.15f, 0.5f);

    /** Full constructor with explicit rotational friction. */
    public Material(String name, float density, float friction, float elasticity, float rotationalFriction) {
        this.name = name;
        this.density = density;
        this.friction = Math.max(0f, Math.min(1f, friction));
        this.elasticity = Math.max(0f, Math.min(1f, elasticity));
        this.rotationalFriction = Math.max(0f, Math.min(1f, rotationalFriction));
    }

    /** Backward-compatible constructor; rotational friction defaults to 0.3. */
    public Material(String name, float density, float friction, float elasticity) {
        this(name, density, friction, elasticity, 0.3f);
    }

    public float computeMass(int width, int height) {
        return Math.max(0.1f, density * Math.max(1, width) * Math.max(1, height) * 0.01f);
    }
}
