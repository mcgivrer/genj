package com.demo.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

import com.core.entity.GameObject;
import com.core.physics.CollisionEngine;
import com.core.physics.PhysicsType;

import java.util.List;

public class CollisionEngineSteps {

    private GameObject entityA;
    private GameObject entityB;
    private float initialAx;
    private float initialBx;
    private final CollisionEngine collisionEngine = new CollisionEngine();

    // ── Entity creation ───────────────────────────────────────────────────────

    @Given("^a dynamic entity A at \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void dynamicEntityA(double x, double y, int w, int h, double vx, double vy) {
        entityA = new GameObject("A");
        entityA.setPosition((float) x, (float) y);
        entityA.setSize(w, h);
        entityA.setVelocity((float) vx, (float) vy);
        entityA.physicsType = PhysicsType.DYNAMIC;
    }

    @Given("^a dynamic entity B at \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void dynamicEntityB(double x, double y, int w, int h, double vx, double vy) {
        entityB = new GameObject("B");
        entityB.setPosition((float) x, (float) y);
        entityB.setSize(w, h);
        entityB.setVelocity((float) vx, (float) vy);
        entityB.physicsType = PhysicsType.DYNAMIC;
    }

    @Given("^a static entity B at \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+)$")
    public void staticEntityB(double x, double y, int w, int h) {
        entityB = new GameObject("B");
        entityB.setPosition((float) x, (float) y);
        entityB.setSize(w, h);
        entityB.setVelocity(0.0f, 0.0f);
        entityB.physicsType = PhysicsType.STATIC;
    }

    @Given("^a static entity A at \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+)$")
    public void staticEntityA(double x, double y, int w, int h) {
        entityA = new GameObject("A");
        entityA.setPosition((float) x, (float) y);
        entityA.setSize(w, h);
        entityA.setVelocity(0.0f, 0.0f);
        entityA.physicsType = PhysicsType.STATIC;
    }

    @Given("^an inactive dynamic entity B at \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void inactiveDynamicEntityB(double x, double y, int w, int h, double vx, double vy) {
        entityB = new GameObject("B");
        entityB.setPosition((float) x, (float) y);
        entityB.setSize(w, h);
        entityB.setVelocity((float) vx, (float) vy);
        entityB.physicsType = PhysicsType.DYNAMIC;
        entityB.active = false;
    }

    @Given("^a NONE-type entity B at \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void noneTypeEntityB(double x, double y, int w, int h, double vx, double vy) {
        entityB = new GameObject("B");
        entityB.setPosition((float) x, (float) y);
        entityB.setSize(w, h);
        entityB.setVelocity((float) vx, (float) vy);
        entityB.physicsType = PhysicsType.NONE;
    }

    @Given("^a heavy dynamic entity A at \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) mass (-?[\\d.]+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void heavyDynamicEntityA(double x, double y, int w, int h, double mass, double vx, double vy) {
        entityA = new GameObject("A");
        entityA.setPosition((float) x, (float) y);
        entityA.setSize(w, h);
        entityA.setVelocity((float) vx, (float) vy);
        entityA.physicsType = PhysicsType.DYNAMIC;
        entityA.mass = (float) mass;
    }

    @Given("^a light dynamic entity B at \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) mass (-?[\\d.]+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void lightDynamicEntityB(double x, double y, int w, int h, double mass, double vx, double vy) {
        entityB = new GameObject("B");
        entityB.setPosition((float) x, (float) y);
        entityB.setSize(w, h);
        entityB.setVelocity((float) vx, (float) vy);
        entityB.physicsType = PhysicsType.DYNAMIC;
        entityB.mass = (float) mass;
    }

    // ── Action ────────────────────────────────────────────────────────────────

    @When("the collision engine resolves")
    public void collisionEngineResolves() {
        initialAx = entityA.x;
        initialBx = entityB.x;
        collisionEngine.resolve(List.of(entityA, entityB));
    }

    // ── Assertions ────────────────────────────────────────────────────────────

    @Then("entity A velocity X should equal {double}")
    public void entityAVelocityXEquals(double expected) {
        Assertions.assertEquals((float) expected, entityA.vx, 0.001f,
            "Expected A.vx = " + expected + " but was " + entityA.vx);
    }

    @Then("entity B velocity X should equal {double}")
    public void entityBVelocityXEquals(double expected) {
        Assertions.assertEquals((float) expected, entityB.vx, 0.001f,
            "Expected B.vx = " + expected + " but was " + entityB.vx);
    }

    @Then("entity A and entity B should not overlap")
    public void entitiesShouldNotOverlap() {
        Assertions.assertFalse(entityA.isIntersect(entityB),
            "Entities A and B still overlap after collision resolution");
    }

    @Then("entity A velocity X should be negative")
    public void entityAVelocityXNegative() {
        Assertions.assertTrue(entityA.vx < 0,
            "Expected A.vx < 0 but was " + entityA.vx);
    }

    @Then("entity A velocity Y should be negative")
    public void entityAVelocityYNegative() {
        Assertions.assertTrue(entityA.vy < 0,
            "Expected A.vy < 0 but was " + entityA.vy);
    }

    @Then("entity A velocity Y should equal {double}")
    public void entityAVelocityYEquals(double expected) {
        Assertions.assertEquals((float) expected, entityA.vy, 0.001f,
            "Expected A.vy = " + expected + " but was " + entityA.vy);
    }

    @Then("entity B velocity Y should equal {double}")
    public void entityBVelocityYEquals(double expected) {
        Assertions.assertEquals((float) expected, entityB.vy, 0.001f,
            "Expected B.vy = " + expected + " but was " + entityB.vy);
    }

    @Then("entity B velocity Y should be greater than {double}")
    public void entityBVelocityYGreaterThan(double expected) {
        Assertions.assertTrue(entityB.vy > (float) expected,
            "Expected B.vy > " + expected + " but was " + entityB.vy);
    }

    @Then("entity B velocity X should be greater than {double}")
    public void entityBVelocityXGreaterThan(double expected) {
        Assertions.assertTrue(entityB.vx > (float) expected,
            "Expected B.vx > " + expected + " but was " + entityB.vx);
    }

    @Then("entity B position X change should be greater than entity A position X change")
    public void entityBMovesMoreThanEntityA() {
        float changeA = Math.abs(entityA.x - initialAx);
        float changeB = Math.abs(entityB.x - initialBx);
        Assertions.assertTrue(changeB > changeA,
            "Expected |B.x change| (" + changeB + ") > |A.x change| (" + changeA + ")");
    }
}
