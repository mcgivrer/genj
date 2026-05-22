package com.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.core.entity.World;
import com.core.physics.PhysicsType;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("World")
class WorldTest {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World("test-world");
        world.setPosition(0f, 0f);
        world.setSize(800, 600);
    }

    // ── Defaults ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("physicsType is NONE (world bounds enforced by containInWorld, not AABB collision)")
        void physicsTypeIsNone() {
            assertEquals(PhysicsType.NONE, world.physicsType);
        }

        @Test
        @DisplayName("default gravity is (0, 200)")
        void defaultGravity() {
            World fresh = new World("fresh");
            assertEquals(0f, fresh.gravityX);
            assertEquals(200f, fresh.gravityY);
        }
    }

    // ── Gravity ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Gravity")
    class Gravity {

        @Test
        @DisplayName("setGravity updates both components")
        void setGravityUpdatesBoth() {
            world.setGravity(10f, -50f);
            assertEquals(10f, world.gravityX);
            assertEquals(-50f, world.gravityY);
        }

        @Test
        @DisplayName("setGravity returns the World for fluent chaining")
        void setGravityReturnsSelf() {
            assertSame(world, world.setGravity(0f, 100f));
        }

        @Test
        @DisplayName("zero gravity leaves both components at 0")
        void zeroGravity() {
            world.setGravity(0f, 0f);
            assertEquals(0f, world.gravityX);
            assertEquals(0f, world.gravityY);
        }
    }

    // ── Bounds ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Bounds")
    class Bounds {

        @Test
        @DisplayName("minX equals world x position")
        void minXEqualsX() {
            world.setPosition(50f, 0f);
            assertEquals(50f, world.minX());
        }

        @Test
        @DisplayName("minY equals world y position")
        void minYEqualsY() {
            world.setPosition(0f, 30f);
            assertEquals(30f, world.minY());
        }

        @Test
        @DisplayName("maxX equals x + width")
        void maxXEqualsXPlusWidth() {
            world.setPosition(0f, 0f);
            world.setSize(800, 600);
            assertEquals(800f, world.maxX());
        }

        @Test
        @DisplayName("maxY equals y + height")
        void maxYEqualsYPlusHeight() {
            world.setPosition(0f, 0f);
            world.setSize(800, 600);
            assertEquals(600f, world.maxY());
        }

        @Test
        @DisplayName("bounds shift correctly when position changes")
        void boundsShiftWithPosition() {
            world.setPosition(100f, 50f);
            world.setSize(800, 600);
            assertEquals(100f, world.minX());
            assertEquals(50f,  world.minY());
            assertEquals(900f, world.maxX());
            assertEquals(650f, world.maxY());
        }
    }
}
