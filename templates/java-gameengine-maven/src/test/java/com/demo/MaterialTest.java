package com.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.core.physics.Material;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Material")
class MaterialTest {

    // ── Predefined constants ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Predefined constants")
    class Predefined {

        @Test
        @DisplayName("DEFAULT has correct properties")
        void defaultMaterial() {
            assertEquals("default", Material.DEFAULT.name);
            assertEquals(1.0f, Material.DEFAULT.density);
            assertEquals(0.2f, Material.DEFAULT.friction);
            assertEquals(0.4f, Material.DEFAULT.elasticity);
            assertEquals(0.3f, Material.DEFAULT.rotationalFriction, 0.001f);
        }

        @Test
        @DisplayName("ICE has lower rotational friction than STONE")
        void iceLowestRotationalFriction() {
            assertTrue(Material.ICE.rotationalFriction < Material.STONE.rotationalFriction);
            assertTrue(Material.ICE.rotationalFriction < Material.WOOD.rotationalFriction);
        }

        @Test
        @DisplayName("RUBBER has highest rotational friction")
        void rubberHighestRotationalFriction() {
            assertTrue(Material.RUBBER.rotationalFriction > Material.DEFAULT.rotationalFriction);
            assertTrue(Material.RUBBER.rotationalFriction > Material.METAL.rotationalFriction);
        }

        @Test
        @DisplayName("METAL has higher density than WOOD")
        void metalDensierThanWood() {
            assertTrue(Material.METAL.density > Material.WOOD.density);
        }

        @Test
        @DisplayName("RUBBER has highest elasticity")
        void rubberHighestElasticity() {
            float rubberE = Material.RUBBER.elasticity;
            assertTrue(rubberE > Material.DEFAULT.elasticity);
            assertTrue(rubberE > Material.METAL.elasticity);
            assertTrue(rubberE > Material.WOOD.elasticity);
            assertTrue(rubberE > Material.ICE.elasticity);
            assertTrue(rubberE > Material.STONE.elasticity);
        }

        @Test
        @DisplayName("ICE has lowest friction")
        void iceLowestFriction() {
            float iceFriction = Material.ICE.friction;
            assertTrue(iceFriction < Material.DEFAULT.friction);
            assertTrue(iceFriction < Material.WOOD.friction);
            assertTrue(iceFriction < Material.METAL.friction);
        }
    }

    // ── Constructor clamping ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Constructor clamping")
    class Clamping {

        @Test
        @DisplayName("friction is clamped to [0, 1]")
        void frictionClamped() {
            Material tooHigh = new Material("x", 1f, 5f, 0.5f);
            Material tooLow  = new Material("x", 1f, -1f, 0.5f);
            assertEquals(1f, tooHigh.friction);
            assertEquals(0f, tooLow.friction);
        }

        @Test
        @DisplayName("elasticity is clamped to [0, 1]")
        void elasticityClamped() {
            Material tooHigh = new Material("x", 1f, 0.5f, 3f);
            Material tooLow  = new Material("x", 1f, 0.5f, -2f);
            assertEquals(1f, tooHigh.elasticity);
            assertEquals(0f, tooLow.elasticity);
        }

        @Test
        @DisplayName("rotationalFriction is clamped to [0, 1]")
        void rotationalFrictionClamped() {
            Material tooHigh = new Material("x", 1f, 0.5f, 0.5f, 5f);
            Material tooLow  = new Material("x", 1f, 0.5f, 0.5f, -1f);
            assertEquals(1f, tooHigh.rotationalFriction, 0.001f);
            assertEquals(0f, tooLow.rotationalFriction,  0.001f);
        }

        @Test
        @DisplayName("4-arg constructor defaults rotationalFriction to 0.3")
        void defaultRotationalFriction() {
            Material m = new Material("x", 1f, 0.5f, 0.5f);
            assertEquals(0.3f, m.rotationalFriction, 0.001f);
        }
    }

    // ── computeMass ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("computeMass")
    class ComputeMass {

        @Test
        @DisplayName("mass grows with size")
        void massGrowsWithSize() {
            float small = Material.DEFAULT.computeMass(10, 10);
            float large = Material.DEFAULT.computeMass(50, 50);
            assertTrue(large > small);
        }

        @Test
        @DisplayName("mass is at least 0.1 for zero-sized entity")
        void minimumMass() {
            float mass = Material.DEFAULT.computeMass(0, 0);
            assertEquals(0.1f, mass, 0.001f);
        }

        @Test
        @DisplayName("denser material produces higher mass for same size")
        void densityImpactsMass() {
            float woodMass  = Material.WOOD.computeMass(20, 20);
            float metalMass = Material.METAL.computeMass(20, 20);
            assertTrue(metalMass > woodMass);
        }

        @ParameterizedTest(name = "size {0}x{1} -> mass >= 0.1")
        @CsvSource({"0,0", "1,0", "0,1", "1,1", "10,10", "100,100"})
        @DisplayName("mass is never negative or zero")
        void massNeverNegative(int w, int h) {
            float mass = Material.DEFAULT.computeMass(w, h);
            assertTrue(mass >= 0.1f, "mass should be >= 0.1 but was " + mass);
        }
    }
}
