package com.demo.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

import com.core.behavior.particle.FountainBehavior;
import com.core.behavior.particle.RainBehavior;
import com.core.behavior.particle.SnowBehavior;
import com.core.behavior.particle.TorchBehavior;
import com.core.entity.ParticleSystem;
import com.core.entity.World;
import com.core.physics.PhysicsType;

public class ParticleSystemSteps {

    private World          world;
    private ParticleSystem system;

    // ─── Background / world ────────────────────────────────────────────────────

    @Given("^a particle world of size (\\d+)x(\\d+) with gravity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
    public void aParticleWorld(int w, int h, double gx, double gy) {
        world = new World("test-world");
        world.setSize(w, h).setGravity((float) gx, (float) gy);
    }

    // ─── Entity creation ───────────────────────────────────────────────────────

    @Given("^a ParticleSystem named \"([^\"]+)\" at position \\((-?[\\d.]+), (-?[\\d.]+)\\) with maxParticles (\\d+)$")
    public void aParticleSystem(String name, double x, double y, int maxParticles) {
        system = new ParticleSystem(name);
        system.setPosition((float) x, (float) y);
        system.setMaxParticles(maxParticles);
    }

    // ─── Behavior attachment ──────────────────────────────────────────────────

    @And("^the system has a RainBehavior$")
    public void systemHasRainBehavior() {
        system.addBehavior(new RainBehavior(world).setWidth(800));
    }

    @And("^the system has a SnowBehavior$")
    public void systemHasSnowBehavior() {
        system.addBehavior(new SnowBehavior(world).setWidth(800));
    }

    @And("^the system has a FountainBehavior$")
    public void systemHasFountainBehavior() {
        system.addBehavior(new FountainBehavior(world));
    }

    @And("^the system has a TorchBehavior$")
    public void systemHasTorchBehavior() {
        system.addBehavior(new TorchBehavior());
    }

    @And("^the system is inactive$")
    public void systemIsInactive() {
        system.setActive(false);
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    @When("^the system updates for (\\d+) milliseconds$")
    public void systemUpdatesFor(long ms) {
        system.update(ms);
    }

    @When("^the system updates (\\d+) times for (\\d+) milliseconds each$")
    public void systemUpdatesManyTimes(int times, long ms) {
        for (int i = 0; i < times; i++) {
            system.update(ms);
        }
    }

    @And("^the system particles are cleared$")
    public void systemParticlesAreCleared() {
        system.clearParticles();
    }

    // ─── Assertions ───────────────────────────────────────────────────────────

    @Then("^the alive particle count should be greater than (\\d+)$")
    public void aliveCountGreaterThan(int threshold) {
        Assertions.assertTrue(system.aliveCount() > threshold,
                "Expected aliveCount > " + threshold + " but got " + system.aliveCount());
    }

    @Then("^the alive particle count should not exceed (\\d+)$")
    public void aliveCountNotExceed(int cap) {
        Assertions.assertTrue(system.aliveCount() <= cap,
                "Expected aliveCount <= " + cap + " but got " + system.aliveCount());
    }

    @Then("^the alive particle count should equal (\\d+)$")
    public void aliveCountEquals(int expected) {
        Assertions.assertEquals(expected, system.aliveCount());
    }

    @Then("^the system physics type should be STATIC$")
    public void systemPhysicsTypeIsStatic() {
        Assertions.assertEquals(PhysicsType.STATIC, system.physicsType);
    }

    @Then("^the system width should be (\\d+)$")
    public void systemWidthEquals(int expected) {
        Assertions.assertEquals((float) expected, system.width, 0.001f);
    }

    @And("^the system height should be (\\d+)$")
    public void systemHeightEquals(int expected) {
        Assertions.assertEquals((float) expected, system.height, 0.001f);
    }
}
