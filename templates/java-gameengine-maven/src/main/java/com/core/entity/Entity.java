package com.core.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import com.core.behavior.Behavior;
import com.core.physics.Material;
import com.core.physics.PhysicsType;

public class Entity<T> {
    public static long idx = 0;
    public long id = idx++;
    public String name = "entity_%d".formatted(id);

    public float x = 0.0f, y = 0.0f;
    public float vx = 0.0f, vy = 0.0f;

    /** Rotation angle in radians. Updated each frame by integrating {@link #angularVelocity}. */
    public float rotation = 0.0f;

    /** Angular velocity in radians per second. Positive = counter-clockwise. */
    public float angularVelocity = 0.0f;

    public float mass = 1.0f;
    public PhysicsType physicsType = PhysicsType.DYNAMIC;
    public Material material = Material.DEFAULT;

    public int width = 0, height = 0;
    public Color color = Color.BLACK, fillColor = Color.BLUE;

    public List<Entity<?>> children = new ArrayList<>();
    public List<Behavior> behaviors = new ArrayList<>();

    /** Set to true by CollisionEngine when this entity rests on a surface below it. */
    public boolean onGround = false;

    /** The STATIC entity this entity is currently riding (set by CollisionEngine). */
    public Entity<?> ridingPlatform = null;

    /** Sides in contact during the current frame (reset each frame by PhysicsEngine). */
    public boolean collisionTop    = false;
    public boolean collisionBottom = false;
    public boolean collisionLeft   = false;
    public boolean collisionRight  = false;

    public boolean active = true;

    /**
     * Controls the order in which entities are drawn within a single camera pass.
     * Lower values are drawn first (appear behind higher-priority entities).
     * {@link com.core.entity.World} defaults to {@code -100} so it always renders
     * before any game object.
     */
    public int renderPriority = 0;

    public Entity() {
    }

    public Entity(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public T add(Entity<?> b) {
        children.add(b);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setVelocity(float vx, float vy) {
        this.vx = vx;
        this.vy = vy;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setRotation(float rotation) {
        this.rotation = rotation;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setAngularVelocity(float av) {
        this.angularVelocity = av;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setSize(int w, int h) {
        this.width = w;
        this.height = h;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setColor(Color c) {
        this.color = c;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setFillColor(Color fc) {
        this.fillColor = fc;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setActive(boolean a) {
        this.active = a;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setMass(float m) {
        this.mass = m;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setPhysicsType(PhysicsType pt) {
        this.physicsType = pt;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setMaterial(Material material) {
        this.material = material;
        if (material != null && width > 0 && height > 0) {
            this.mass = material.computeMass(width, height);
        }
        return (T) this;
    }

    public boolean isIntersect(Entity<?> other) {
        return this.x < other.x + other.width
                && this.x + this.width > other.x
                && this.y < other.y + other.height
                && this.y + this.height > other.y;
    }

    public boolean isActive() {
        return this.active;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @SuppressWarnings("unchecked")
    public T setRenderPriority(int priority) {
        this.renderPriority = priority;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T addBehavior(Behavior b) {
        behaviors.add(b);
        return (T) this;
    }

    public List<Behavior> getBehaviors() {
        return behaviors;
    }

    public void update(long elapsed) {
        // Run behaviors before position integration
        behaviors.forEach(b -> b.update(this, elapsed));
        float dt = elapsed / 1000f;
        x += vx * dt;
        y += vy * dt;
        rotation += angularVelocity * dt;
    }

    public void draw(Graphics2D g) {
        if (fillColor != null) {
            g.setColor(fillColor);
            g.fillRect((int) (x + 0.5f), (int) (y + 0.5f), width, height);
        }
        if (color != null) {
            g.setColor(color);
            g.drawRect((int) (x + 0.5f), (int) (y + 0.5f), width, height);
        }
    }
}
