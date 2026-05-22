package com.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.core.behavior.CameraTrackingBehavior;
import com.core.entity.GameObject;
import com.core.entity.World;
import com.core.gfx.Camera;

@DisplayName("CameraTrackingBehavior")
class CameraTrackingBehaviorTest {

    private Camera camera;
    private GameObject target;
    private World world;

    @BeforeEach
    void setUp() {
        // A 400×300 viewport at origin
        camera = new Camera("cam");
        camera.setViewport(0, 0, 400, 300);

        // Target: 24×32 sprite
        target = new GameObject("player");
        target.width  = 24;
        target.height = 32;
        target.x = 200f;
        target.y = 100f;

        world = new World("world")
                .setPosition(0, 0)
                .setSize(1200, 800);
    }

    // ── Default values ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Default values")
    class Defaults {

        @Test
        @DisplayName("tiltX defaults to 0.5")
        void tiltXDefaultHalf() {
            CameraTrackingBehavior b = new CameraTrackingBehavior(target);
            assertEquals(0.5f, b.getTiltX(), 0.001f);
        }

        @Test
        @DisplayName("tiltY defaults to 0.75")
        void tiltYDefaultThreeQuarters() {
            CameraTrackingBehavior b = new CameraTrackingBehavior(target);
            assertEquals(0.75f, b.getTiltY(), 0.001f);
        }

        @Test
        @DisplayName("elasticity defaults to 0 (instant snap)")
        void elasticityDefaultZero() {
            CameraTrackingBehavior b = new CameraTrackingBehavior(target);
            assertEquals(0f, b.getElasticity(), 0.001f);
        }
    }

    // ── Fluent setters ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Fluent setters")
    class Setters {

        @Test
        @DisplayName("setTiltX returns same instance")
        void setTiltXFluent() {
            CameraTrackingBehavior b = new CameraTrackingBehavior(target);
            assertSame(b, b.setTiltX(0.3f));
            assertEquals(0.3f, b.getTiltX(), 0.001f);
        }

        @Test
        @DisplayName("setTiltY returns same instance")
        void setTiltYFluent() {
            CameraTrackingBehavior b = new CameraTrackingBehavior(target);
            assertSame(b, b.setTiltY(0.6f));
            assertEquals(0.6f, b.getTiltY(), 0.001f);
        }

        @Test
        @DisplayName("setElasticity clamps to [0, 0.999]")
        void setElasticityClamps() {
            CameraTrackingBehavior b = new CameraTrackingBehavior(target);
            b.setElasticity(-0.5f);
            assertEquals(0f, b.getElasticity(), 0.001f);
            b.setElasticity(2f);
            assertEquals(0.999f, b.getElasticity(), 0.001f);
        }
    }

    // ── Positioning ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Positioning")
    class Positioning {

        @Test
        @DisplayName("instant snap (elasticity=0) places camera in one update")
        void instantSnap() {
            CameraTrackingBehavior b = new CameraTrackingBehavior(target, world)
                    .setTiltX(0.5f)
                    .setTiltY(0.5f);

            b.update(camera, 16L);

            // desired: target centre at (0.5 × 400, 0.5 × 300) of viewport
            // desiredX = target.x + target.width/2 - 400*0.5 = 212 - 200 = 12
            // desiredY = target.y + target.height/2 - 300*0.5 = 116 - 150 = -34 → clamped to 0
            assertEquals(12f, camera.x, 0.5f);
            assertEquals(0f,  camera.y, 0.5f); // clamped at world minY
        }

        @Test
        @DisplayName("tiltX=0.5 horizontally centres the target in the viewport")
        void tiltXCentresHorizontally() {
            // Place target at a position well inside the world so no clamping
            target.x = 600f;
            target.y = 400f;

            CameraTrackingBehavior b = new CameraTrackingBehavior(target, world)
                    .setTiltX(0.5f)
                    .setTiltY(0.5f);
            b.update(camera, 16L);

            // After update: cam.x = 600 + 12 - 200 = 412
            // target centre screen X = (612 - 412) = 200 = viewport centre ✓
            float targetCentreWorld = target.x + target.width * 0.5f;
            float targetScreenX = targetCentreWorld - camera.x;
            assertEquals(400 * 0.5f, targetScreenX, 0.5f);
        }

        @Test
        @DisplayName("tiltY=0.75 places the target at 75% of viewport height")
        void tiltYAt75Percent() {
            target.x = 600f;
            target.y = 400f;

            CameraTrackingBehavior b = new CameraTrackingBehavior(target, world)
                    .setTiltX(0.5f)
                    .setTiltY(0.75f);
            b.update(camera, 16L);

            float targetCentreWorld = target.y + target.height * 0.5f;
            float targetScreenY = targetCentreWorld - camera.y;
            assertEquals(300 * 0.75f, targetScreenY, 0.5f);
        }

        @Test
        @DisplayName("camera is clamped to world boundaries")
        void clampedToWorldBounds() {
            // Target near top-left corner — desired position would be negative
            target.x = 0f;
            target.y = 0f;

            CameraTrackingBehavior b = new CameraTrackingBehavior(target, world)
                    .setTiltX(0.5f)
                    .setTiltY(0.5f);
            b.update(camera, 16L);

            assertTrue(camera.x >= world.minX());
            assertTrue(camera.y >= world.minY());
        }
    }

    // ── Elasticity ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Elasticity")
    class Elasticity {

        @Test
        @DisplayName("elasticity > 0 does not snap in a single frame")
        void elasticityDoesNotSnapImmediately() {
            target.x = 600f;
            target.y = 400f;
            camera.x = 0f;
            camera.y = 0f;

            CameraTrackingBehavior b = new CameraTrackingBehavior(target, world)
                    .setTiltX(0.5f)
                    .setTiltY(0.5f)
                    .setElasticity(0.85f);

            b.update(camera, 16L);

            // With elasticity=0.85 at 16ms: blend ≈ 0.024 — camera moves but does not reach target
            // desiredX = 612 - 200 = 412; after one step camera.x must be > 0 but < 412
            assertTrue(camera.x > 0f,   "camera should have moved");
            assertTrue(camera.x < 412f, "camera should not have snapped");
        }

        @Test
        @DisplayName("elasticity=0 and elasticity=0 both snap instantly")
        void elasticityZeroSnapsInstantly() {
            target.x = 600f;
            target.y = 400f;

            CameraTrackingBehavior b = new CameraTrackingBehavior(target, world)
                    .setTiltX(0.5f)
                    .setTiltY(0.5f)
                    .setElasticity(0f);
            b.update(camera, 16L);

            float expectedX = target.x + target.width * 0.5f - 400 * 0.5f;
            assertEquals(expectedX, camera.x, 0.5f);
        }
    }
}
