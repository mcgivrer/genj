package com.core.behavior.particle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.core.behavior.Behavior;
import com.core.entity.Entity;
import com.core.entity.Particle;
import com.core.entity.ParticleSystem;
import com.core.entity.World;
import com.core.physics.PhysicsType;

/**
 * Abstract base for all particle-emitter behaviours.
 *
 * <p>Each frame ({@link #update}) this class:</p>
 * <ol>
 *   <li>Advances every alive {@link Particle} (integrates velocity, applies
 *       forces, updates visual state).</li>
 *   <li>Spawns new particles at the configured {@link EmitterConfig#emitRate},
 *       recycling dead pool slots before allocating new ones.</li>
 * </ol>
 *
 * <p>Concrete subclasses only need to implement {@link #spawnParticle}, which
 * sets the initial state of one fresh particle.</p>
 */
public abstract class ParticleEmitterBehavior implements Behavior {

    /** Shared pseudo-random number generator (not thread-safe, but single-threaded game loop). */
    protected final Random rng = new Random();

    /** Emitter configuration — readable and writable from the Level Editor. */
    protected final EmitterConfig config;

    /** Fractional particle accumulator — carries sub-integer emit counts across frames. */
    private float spawnAccumulator = 0f;

    /** Optional world reference used to feed world gravity into the config each frame. */
    private World world;

    /**
     * Optional list of scene entities used for particle–obstacle collision.
     * Only STATIC entities with a non-zero AABB are tested.
     * Pass {@code scene.getEntities()} for a live reference that automatically
     * reflects any entity added or removed after the behavior was created.
     */
    private List<Entity<?>> obstacles;

    /** Fraction of speed retained after a bounce (0 = fully inelastic, 1 = elastic). */
    private static final float BOUNCE_RESTITUTION = 0.35f;

    /** Lateral friction factor applied to the tangential velocity on bounce. */
    private static final float BOUNCE_FRICTION = 0.5f;

    /**
     * When {@code true}, particles that hit the top face of a static obstacle are
     * destroyed and replaced by a small fan of tiny splash droplets.
     */
    private boolean splashEnabled = false;

    /**
     * Per-frame impact records collected while iterating particles.
     * Each entry is {@code float[]{x, surfaceY, incomingVx}}.
     * Cleared at the start of each update; avoids allocating inside the loop.
     */
    private final ArrayList<float[]> pendingSplashes = new ArrayList<>();

    protected ParticleEmitterBehavior(EmitterConfig config) {
        this.config = config;
    }

    protected ParticleEmitterBehavior(EmitterConfig config, World world) {
        this.config = config;
        this.world  = world;
    }

    /**
     * Attaches a world so the behaviour can read the authoritative gravity
     * values each frame.
     */
    public ParticleEmitterBehavior setWorld(World w) {
        this.world = w;
        return this;
    }

    /**
     * Provides a (usually live) list of entities against which alive particles
     * will be tested for AABB collision each frame.  Only entities with
     * {@link PhysicsType#STATIC} and a non-zero bounding box are considered.
     *
     * @param obstacles the entity list (may be the scene's live entity list)
     * @return {@code this} for fluent chaining
     */
    public ParticleEmitterBehavior setObstacles(List<Entity<?>> obstacles) {
        this.obstacles = obstacles;
        return this;
    }

    /**
     * Enables or disables the splash effect when particles hit the top of a
     * static obstacle.  Disabled by default; {@link RainBehavior} enables it
     * automatically in its constructors.
     *
     * @param enabled {@code true} to show splashes, {@code false} to just bounce
     * @return {@code this} for fluent chaining
     */
    public ParticleEmitterBehavior setSplashEnabled(boolean enabled) {
        this.splashEnabled = enabled;
        return this;
    }

    public EmitterConfig getConfig() {
        return config;
    }

    // ─── Behavior ────────────────────────────────────────────────────────────

    @Override
    public void update(Entity<?> entity, long elapsed) {
        if (!(entity instanceof ParticleSystem ps) || !ps.isActive()) return;
        float dt = elapsed / 1000f;
        if (dt <= 0f) return;

        // Pre-update hook: subclasses may modulate config (e.g., flicker) before
        // the main particle loop runs.
        onPreUpdate(dt);

        // Sync world gravity into config so subclasses don't need to reference World.
        if (world != null) {
            config.worldGravityX = world.gravityX;
            config.worldGravityY = world.gravityY;
        }

        if (splashEnabled) pendingSplashes.clear();
        updateParticles(ps, dt);
        if (splashEnabled && !pendingSplashes.isEmpty()) {
            spawnSplashParticles(ps);
            pendingSplashes.clear();
        }
        spawnParticles(ps, dt);
    }

    /**
     * Called once per frame before the particle-update loop.
     * Subclasses may override to modulate {@link #config} fields each frame
     * (e.g., TorchBehavior uses this to flicker {@link EmitterConfig#emitRate}).
     *
     * @param dt frame delta in seconds
     */
    protected void onPreUpdate(float dt) {}

    // ─── Internal update loop ─────────────────────────────────────────────────

    private void updateParticles(ParticleSystem ps, float dt) {
        float gx = config.worldGravityX * config.gravityFactor + config.windX;
        float gy = config.worldGravityY * config.gravityFactor + config.windY;

        for (Particle p : ps.particles) {
            if (!p.alive) continue;

            p.life -= dt;
            if (p.life <= 0f) {
                p.alive = false;
                continue;
            }

            // Integrate forces
            p.vx += gx * dt;
            p.vy += gy * dt;

            // Integrate position
            p.x += p.vx * dt;
            p.y += p.vy * dt;

            // Angular integration (also used as wave-phase for snow)
            p.rotation += p.angularVelocity * dt;

            // Obstacle collision response
            resolveObstacleCollisions(p);

            float ratio = p.life / p.maxLife;   // 1=fresh, 0=dying

            // Visual updates
            if (config.fadeOut) {
                p.alpha = Math.max(0f, ratio);
            }
            if (config.shrink) {
                p.size = Math.max(0.5f, p.initialSize * ratio);
            }
            p.color = config.interpolateColor(ratio);

            // Let subclasses apply per-particle per-frame logic (e.g., snow waviness)
            updateParticle(p, dt, ratio);
        }
    }

    private void spawnParticles(ParticleSystem ps, float dt) {
        spawnAccumulator += config.emitRate * dt;
        int toSpawn = (int) spawnAccumulator;
        spawnAccumulator -= toSpawn;

        for (int i = 0; i < toSpawn; i++) {
            if (ps.aliveCount() >= ps.maxParticles) break;

            Particle slot = findDeadSlot(ps);
            if (slot == null) {
                if (ps.particles.size() >= ps.maxParticles) break;
                slot = new Particle();
                ps.particles.add(slot);
            }
            slot.reset();
            initParticlePosition(ps, slot);
            spawnParticle(ps, slot);
            slot.alive = true;
        }
    }

    /** Places the new particle at the emitter origin, with optional area jitter. */
    private void initParticlePosition(ParticleSystem ps, Particle p) {
        p.x = ps.x + nextFloat(-config.emitAreaW * 0.5f, config.emitAreaW * 0.5f);
        p.y = ps.y + nextFloat(-config.emitAreaH * 0.5f, config.emitAreaH * 0.5f);
    }

    private static Particle findDeadSlot(ParticleSystem ps) {
        for (Particle p : ps.particles) {
            if (!p.alive) return p;
        }
        return null;
    }

    // ─── Extension points ─────────────────────────────────────────────────────

    /**
     * Sets the initial velocity, size, lifetime and colour of a freshly
     * allocated particle.  Position is already set by the base class.
     *
     * @param ps the owning system (provides context such as emitter position)
     * @param p  the particle to initialise; {@code alive} will be set to
     *           {@code true} by the base class after this call returns
     */
    protected abstract void spawnParticle(ParticleSystem ps, Particle p);

    /**
     * Optional per-particle per-frame hook for subclasses that need to apply
     * custom logic (e.g., sinusoidal drift for snow).
     * Default implementation is a no-op.
     *
     * @param p     the particle being updated
     * @param dt    frame delta in seconds
     * @param ratio age ratio: 1 = just spawned, 0 = about to die
     */
    protected void updateParticle(Particle p, float dt, float ratio) {}

    // ─── Obstacle collision ───────────────────────────────────────────────────

    /**
     * Resolves the collision of a particle against all registered STATIC obstacles.
     *
     * <p>The particle is treated as a point.  When it overlaps an obstacle's AABB
     * the dominant velocity axis determines the collision face:</p>
     * <ul>
     *   <li>Vertical dominant (|vy| ≥ |vx|): top or bottom face — vy is reflected
     *       and vx receives lateral friction.</li>
     *   <li>Horizontal dominant: left or right face — vx is reflected and vy
     *       receives lateral friction.</li>
     * </ul>
     */
    private void resolveObstacleCollisions(Particle p) {
        if (obstacles == null) return;
        for (Entity<?> obs : obstacles) {
            if (obs.physicsType != PhysicsType.STATIC) continue;
            if (obs.width == 0 || obs.height == 0) continue;
            // Quick AABB rejection
            if (p.x < obs.x || p.x > obs.x + obs.width) continue;
            if (p.y < obs.y || p.y > obs.y + obs.height) continue;
            // Overlap detected — resolve by dominant velocity axis
            if (Math.abs(p.vy) >= Math.abs(p.vx)) {
                if (p.vy >= 0) {
                    // Particle hits the top face
                    if (splashEnabled && p.data == 0f) {
                        pendingSplashes.add(new float[]{p.x, obs.y, p.vx});
                        p.alive = false;
                        return;   // no further collision processing needed
                    }
                    p.y = obs.y;                    // push to top surface
                } else {
                    p.y = obs.y + obs.height;       // push to bottom surface
                }
                p.vy = -p.vy * BOUNCE_RESTITUTION;
                p.vx *=  BOUNCE_FRICTION;
            } else {
                if (p.vx >= 0) {
                    p.x = obs.x;                    // push to left face
                } else {
                    p.x = obs.x + obs.width;        // push to right face
                }
                p.vx = -p.vx * BOUNCE_RESTITUTION;
                p.vy *=  BOUNCE_FRICTION;
            }
        }
    }

    // ─── Splash ───────────────────────────────────────────────────────────────

    /**
     * For each pending impact, spawns 2-4 tiny droplets that scatter upward
     * in a fan shape, inheriting a fraction of the rain-drop's horizontal
     * velocity.  Uses the particle pool; will not exceed {@code maxParticles}.
     */
    private void spawnSplashParticles(ParticleSystem ps) {
        Color dropColor = config.startColor != null ? config.startColor
                                                    : new Color(160, 200, 255, 200);
        for (float[] splash : pendingSplashes) {
            float sx    = splash[0];
            float sy    = splash[1];
            float inVx  = splash[2];
            int count = 2 + rng.nextInt(3);   // 2..4 droplets per impact
            for (int i = 0; i < count; i++) {
                // Grab a dead slot or allocate if under limit
                Particle slot = null;
                for (Particle q : ps.particles) {
                    if (!q.alive) { slot = q; break; }
                }
                if (slot == null) {
                    if (ps.particles.size() >= ps.maxParticles) break;
                    slot = new Particle();
                    ps.particles.add(slot);
                }
                slot.reset();
                slot.x = sx + nextFloat(-4f, 4f);
                slot.y = sy;
                // Fan upward: angle in (0.1, π-0.1) → all directions point upward
                float angle = nextFloat(0.1f, (float) Math.PI - 0.1f);
                float speed = nextFloat(20f, 70f);
                slot.vx = (float) (Math.cos(angle) * speed) + inVx * 0.4f;
                slot.vy = -(float) (Math.sin(angle) * speed);   // upward on screen
                slot.life        = nextFloat(0.15f, 0.35f);
                slot.maxLife     = slot.life;
                slot.size        = nextFloat(1f, 2.5f);
                slot.initialSize = slot.size;
                slot.alpha       = 0.6f + rng.nextFloat() * 0.4f;
                slot.color       = dropColor;
                slot.data        = 1f;   // mark as splash: prevents cascade re-triggering
                slot.alive       = true;
            }
        }
    }

    // ─── Utility helpers ──────────────────────────────────────────────────────

    /**
     * Samples a direction vector from the configured emission cone and applies
     * it to the given particle at the given speed.
     *
     * @param p     the target particle
     * @param speed speed in pixels/second
     */
    protected void applyVelocityFromCone(Particle p, float speed) {
        float angle = config.direction + nextFloat(-config.spread, config.spread);
        p.vx = (float) Math.cos(angle) * speed;
        p.vy = (float) Math.sin(angle) * speed;
    }

    /**
     * Returns a uniform random float in {@code [min, max]}.
     */
    protected float nextFloat(float min, float max) {
        if (min >= max) return min;
        return min + rng.nextFloat() * (max - min);
    }
}
