package com.demo.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

import com.core.entity.GameObject;
import com.core.entity.Nature;
import com.core.entity.World;
import com.core.physics.PhysicsEngine;
import com.core.physics.PhysicsType;

import java.util.List;

public class PhysicsEngineSteps {

    private World world;
    private GameObject entity;
    private final PhysicsEngine physicsEngine = new PhysicsEngine();

    // ── Background / world setup ──────────────────────────────────────────────

    @Given("^a world of size (\\d+)x(\\d+) with gravity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void aWorldWithGravity(int w, int h, double gx, double gy) {
        world = new World("test-world");
        world.setSize(w, h);
        world.setGravity((float) gx, (float) gy);
    }

    // ── Entity creation ───────────────────────────────────────────────────────

    @Given("^a dynamic entity at position \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void aDynamicEntity(double x, double y, int w, int h, double vx, double vy) {
        entity = new GameObject("entity");
        entity.setPosition((float) x, (float) y);
        entity.setSize(w, h);
        entity.setVelocity((float) vx, (float) vy);
        entity.physicsType = PhysicsType.DYNAMIC;
    }

    @Given("^a static entity at position \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void aStaticEntity(double x, double y, int w, int h, double vx, double vy) {
        entity = new GameObject("entity");
        entity.setPosition((float) x, (float) y);
        entity.setSize(w, h);
        entity.setVelocity((float) vx, (float) vy);
        entity.physicsType = PhysicsType.STATIC;
    }

    @Given("^a NONE-type entity at position \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void aNoneEntity(double x, double y, int w, int h, double vx, double vy) {
        entity = new GameObject("entity");
        entity.setPosition((float) x, (float) y);
        entity.setSize(w, h);
        entity.setVelocity((float) vx, (float) vy);
        entity.physicsType = PhysicsType.NONE;
    }

    @Given("^an inactive dynamic entity at position \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void anInactiveDynamicEntity(double x, double y, int w, int h, double vx, double vy) {
        entity = new GameObject("entity");
        entity.setPosition((float) x, (float) y);
        entity.setSize(w, h);
        entity.setVelocity((float) vx, (float) vy);
        entity.physicsType = PhysicsType.DYNAMIC;
        entity.active = false;
    }

    @Given("^a dynamic entity at position \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) and velocity \\((-?[\\d.]+), (-?[\\d.]+)\\) and angular velocity (-?[\\d.]+)$")
    public void aDynamicEntityWithAngularVelocity(double x, double y, int w, int h, double vx, double vy, double av) {
        entity = new GameObject("entity");
        entity.setPosition((float) x, (float) y);
        entity.setSize(w, h);
        entity.setVelocity((float) vx, (float) vy);
        entity.setAngularVelocity((float) av);
        entity.physicsType = PhysicsType.DYNAMIC;
    }

    @Given("^a rectangle entity at position \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) velocity \\((-?[\\d.]+), (-?[\\d.]+)\\) and angular velocity (-?[\\d.]+)$")
    public void aRectangleEntityWithAngularVelocity(double x, double y, int w, int h, double vx, double vy, double av) {
        entity = new GameObject("entity");
        entity.setPosition((float) x, (float) y);
        entity.setSize(w, h);
        entity.setVelocity((float) vx, (float) vy);
        entity.setAngularVelocity((float) av);
        entity.physicsType = PhysicsType.DYNAMIC;
        entity.nature = Nature.RECTANGLE;
    }

    @Given("^an ellipse entity at position \\((-?[\\d.]+), (-?[\\d.]+)\\) with size (\\d+)x(\\d+) velocity \\((-?[\\d.]+), (-?[\\d.]+)\\) and angular velocity (-?[\\d.]+)$")
    public void anEllipseEntityWithAngularVelocity(double x, double y, int w, int h, double vx, double vy, double av) {
        entity = new GameObject("entity");
        entity.setPosition((float) x, (float) y);
        entity.setSize(w, h);
        entity.setVelocity((float) vx, (float) vy);
        entity.setAngularVelocity((float) av);
        entity.physicsType = PhysicsType.DYNAMIC;
        entity.nature = Nature.ELLIPSE;
    }

    // ── Action ────────────────────────────────────────────────────────────────

    @When("the physics engine updates for {int} milliseconds")
    public void physicsEngineUpdates(int ms) {
        physicsEngine.update(world, List.of(entity), ms);
    }

    // ── Assertions ────────────────────────────────────────────────────────────

    @Then("the entity velocity Y should be greater than {double}")
    public void velocityYGreaterThan(double expected) {
        Assertions.assertTrue(entity.vy > (float) expected,
            "Expected vy > " + expected + " but was " + entity.vy);
    }

    @Then("the entity velocity Y should equal {double}")
    public void velocityYEquals(double expected) {
        Assertions.assertEquals((float) expected, entity.vy, 0.001f,
            "Expected vy = " + expected + " but was " + entity.vy);
    }

    @Then("the entity bottom edge should not exceed the world height {int}")
    public void bottomEdgeWithinBounds(int worldHeight) {
        Assertions.assertTrue(entity.y + entity.height <= worldHeight,
            "Bottom edge " + (entity.y + entity.height) + " exceeds world height " + worldHeight);
    }

    @Then("the entity right edge should not exceed the world width {int}")
    public void rightEdgeWithinBounds(int worldWidth) {
        Assertions.assertTrue(entity.x + entity.width <= worldWidth,
            "Right edge " + (entity.x + entity.width) + " exceeds world width " + worldWidth);
    }

    @Then("the entity velocity X should be negative")
    public void velocityXNegative() {
        Assertions.assertTrue(entity.vx < 0,
            "Expected vx < 0 but was " + entity.vx);
    }

    @Then("the entity velocity X should be greater than {double}")
    public void velocityXGreaterThan(double expected) {
        Assertions.assertTrue(entity.vx > (float) expected,
            "Expected vx > " + expected + " but was " + entity.vx);
    }

    @Then("^the entity left edge should not be less than the world origin (\\d+)$")
    public void leftEdgeWithinBounds(int origin) {
        Assertions.assertTrue(entity.x >= origin,
            "Left edge " + entity.x + " is less than world origin " + origin);
    }

    @Then("^the entity top edge should not be less than the world origin (\\d+)$")
    public void topEdgeWithinBounds(int origin) {
        Assertions.assertTrue(entity.y >= origin,
            "Top edge " + entity.y + " is less than world origin " + origin);
    }

    @Then("the entity angular velocity should be less than {double}")
    public void angularVelocityLessThan(double expected) {
        Assertions.assertTrue(entity.angularVelocity < (float) expected,
            "Expected angularVelocity < " + expected + " but was " + entity.angularVelocity);
    }

    @Then("the entity angular velocity should equal {double}")
    public void angularVelocityEquals(double expected) {
        Assertions.assertEquals((float) expected, entity.angularVelocity, 0.001f,
            "Expected angularVelocity = " + expected + " but was " + entity.angularVelocity);
    }

    @Then("the entity angular velocity should be greater than {double}")
    public void angularVelocityGreaterThan(double expected) {
        Assertions.assertTrue(entity.angularVelocity > (float) expected,
            "Expected angularVelocity > " + expected + " but was " + entity.angularVelocity);
    }

    @Then("the entity angular velocity should not equal {double}")
    public void angularVelocityNotEquals(double expected) {
        Assertions.assertNotEquals((float) expected, entity.angularVelocity, 0.001f,
            "Expected angularVelocity != " + expected + " but was " + entity.angularVelocity);
    }
}
