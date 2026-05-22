package com.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.core.entity.GameObject;
import com.core.physics.Material;
import com.core.physics.PhysicsType;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Entity")
class EntityTest {

    private GameObject entity;

    @BeforeEach
    void setUp() {
        entity = new GameObject("test");
        entity.setSize(20, 20);
        entity.setPosition(0, 0);
    }

    // ── Identity ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Identity")
    class Identity {

        @Test
        @DisplayName("name is set at construction")
        void nameSetsAtConstruction() {
            assertEquals("test", entity.name);
        }

        @Test
        @DisplayName("each entity gets a unique id")
        void uniqueIds() {
            GameObject other = new GameObject("other");
            assertNotEquals(entity.id, other.id);
        }

        @Test
        @DisplayName("default active state is true")
        void defaultActiveIsTrue() {
            assertTrue(entity.isActive());
        }

        @Test
        @DisplayName("setActive toggles the active flag")
        void setActiveToggles() {
            entity.setActive(false);
            assertFalse(entity.isActive());
            entity.setActive(true);
            assertTrue(entity.isActive());
        }
    }

    // ── Fluent setters ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Fluent setters")
    class FluentSetters {

        @Test
        @DisplayName("setPosition updates x and y")
        void setPositionUpdatesFields() {
            entity.setPosition(10f, 20f);
            assertEquals(10f, entity.x);
            assertEquals(20f, entity.y);
        }

        @Test
        @DisplayName("setVelocity updates vx and vy")
        void setVelocityUpdatesFields() {
            entity.setVelocity(3f, -5f);
            assertEquals(3f, entity.vx);
            assertEquals(-5f, entity.vy);
        }

        @Test
        @DisplayName("setSize updates width and height")
        void setSizeUpdatesFields() {
            entity.setSize(40, 60);
            assertEquals(40, entity.width);
            assertEquals(60, entity.height);
        }

        @Test
        @DisplayName("setMass updates mass field")
        void setMassUpdatesField() {
            entity.setMass(5f);
            assertEquals(5f, entity.mass);
        }

        @Test
        @DisplayName("setPhysicsType updates physicsType field")
        void setPhysicsTypeUpdatesField() {
            entity.setPhysicsType(PhysicsType.STATIC);
            assertEquals(PhysicsType.STATIC, entity.physicsType);
        }

        @Test
        @DisplayName("setMaterial recalculates mass from density and size")
        void setMaterialRecalculatesMass() {
            entity.setSize(10, 10);
            entity.setMaterial(Material.METAL); // density 2.7
            float expected = Material.METAL.computeMass(10, 10);
            assertEquals(expected, entity.mass, 0.0001f);
        }

        @Test
        @DisplayName("setMaterial with null does not recalculate mass")
        void setMaterialNullKeepsMass() {
            entity.setMass(3f);
            entity.setMaterial(null);
            assertEquals(3f, entity.mass);
        }

        @Test
        @DisplayName("setMaterial with zero size does not recalculate mass")
        void setMaterialZeroSizeKeepsMass() {
            entity.setSize(0, 0);
            entity.setMass(7f);
            entity.setMaterial(Material.METAL);
            assertEquals(7f, entity.mass, 0.001f);
        }

        @Test
        @DisplayName("setColor updates color field")
        void setColorUpdatesField() {
            entity.setColor(Color.RED);
            assertEquals(Color.RED, entity.color);
        }

        @Test
        @DisplayName("setFillColor updates fillColor field")
        void setFillColorUpdatesField() {
            entity.setFillColor(Color.GREEN);
            assertEquals(Color.GREEN, entity.fillColor);
        }
    }

    // ── Movement ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Movement (update)")
    class Movement {

        @Test
        @DisplayName("update moves entity by velocity * dt")
        void updateMovesEntity() {
            entity.setPosition(0f, 0f);
            entity.setVelocity(100f, 50f);
            entity.update(100); // 100 ms = 0.1 s
            assertEquals(10f, entity.x, 0.01f);
            assertEquals(5f, entity.y, 0.01f);
        }

        @Test
        @DisplayName("update with zero velocity keeps position")
        void updateZeroVelocityKeepsPosition() {
            entity.setPosition(5f, 7f);
            entity.setVelocity(0f, 0f);
            entity.update(500);
            assertEquals(5f, entity.x);
            assertEquals(7f, entity.y);
        }

        @Test
        @DisplayName("update integrates angular velocity into rotation")
        void updateIntegratesAngularVelocity() {
            entity.setRotation(0f);
            entity.setAngularVelocity(2f); // 2 rad/s
            entity.update(500);            // 0.5 s → +1 rad
            assertEquals(1f, entity.rotation, 0.001f);
        }

        @Test
        @DisplayName("zero angular velocity leaves rotation unchanged")
        void updateZeroAngularVelocityKeepsRotation() {
            entity.setRotation(1.5f);
            entity.setAngularVelocity(0f);
            entity.update(200);
            assertEquals(1.5f, entity.rotation, 0.001f);
        }
    }

    // ── Intersection ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AABB intersection")
    class Intersection {

        @Test
        @DisplayName("overlapping entities return true")
        void overlappingEntities() {
            GameObject other = new GameObject("b");
            other.setPosition(10f, 0f);
            other.setSize(20, 20);
            assertTrue(entity.isIntersect(other));
        }

        @Test
        @DisplayName("adjacent entities (touching) do not intersect")
        void adjacentEntities() {
            GameObject other = new GameObject("b");
            other.setPosition(20f, 0f); // starts exactly at entity right edge
            other.setSize(20, 20);
            assertFalse(entity.isIntersect(other));
        }

        @Test
        @DisplayName("distant entities do not intersect")
        void distantEntities() {
            GameObject other = new GameObject("b");
            other.setPosition(100f, 100f);
            other.setSize(20, 20);
            assertFalse(entity.isIntersect(other));
        }

        @Test
        @DisplayName("entity is to the right of other — no X overlap from left")
        void entityRightOfOtherDoesNotIntersect() {
            // this.x >= other.x + other.width  →  C1 of isIntersect is false
            entity.setPosition(100f, 0f);
            GameObject other = new GameObject("b");
            other.setPosition(0f, 0f);
            other.setSize(20, 20);
            assertFalse(entity.isIntersect(other));
        }

        @Test
        @DisplayName("entity is below other — no Y overlap from top")
        void entityBelowOtherDoesNotIntersect() {
            // this.y >= other.y + other.height  →  C3 of isIntersect is false
            entity.setPosition(5f, 100f);
            GameObject other = new GameObject("b");
            other.setPosition(0f, 0f);
            other.setSize(20, 20);
            assertFalse(entity.isIntersect(other));
        }

        @Test
        @DisplayName("entities touching along bottom-top edge do not intersect")
        void entitiesTouchingVerticallyDoNotIntersect() {
            // this.y + this.height == other.y  →  C4 of isIntersect is false
            entity.setPosition(5f, 0f);
            GameObject other = new GameObject("b");
            other.setPosition(0f, 20f); // starts exactly at entity's bottom edge
            other.setSize(20, 20);
            assertFalse(entity.isIntersect(other));
        }
    }

    // ── Children ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Children")
    class Children {

        @Test
        @DisplayName("add appends child to children list")
        void addAppendsChild() {
            GameObject child = new GameObject("child");
            entity.add(child);
            assertEquals(1, entity.children.size());
            assertSame(child, entity.children.get(0));
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Rendering (draw)")
    class Rendering {

        private Graphics2D g;

        @BeforeEach
        void setUpGraphics() {
            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
            g = img.createGraphics();
            entity.setPosition(10, 10);
            entity.setSize(20, 20);
        }

        @AfterEach
        void tearDownGraphics() {
            g.dispose();
        }

        @Test
        @DisplayName("draw with both colors fills and outlines without error")
        void drawWithBothColors() {
            entity.setFillColor(Color.BLUE);
            entity.setColor(Color.BLACK);
            assertDoesNotThrow(() -> entity.draw(g));
        }

        @Test
        @DisplayName("draw with null fillColor skips fill and draws outline only")
        void drawWithNullFillColor() {
            entity.setFillColor(null);
            entity.setColor(Color.BLACK);
            assertDoesNotThrow(() -> entity.draw(g));
        }

        @Test
        @DisplayName("draw with null color fills without drawing outline")
        void drawWithNullColor() {
            entity.setFillColor(Color.BLUE);
            entity.setColor(null);
            assertDoesNotThrow(() -> entity.draw(g));
        }
    }
}
