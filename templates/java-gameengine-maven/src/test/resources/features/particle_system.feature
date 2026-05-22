Feature: ParticleSystem

  Background:
    Given a particle world of size 1200x800 with gravity (0.0, 200.0)

  # ─── Emitter lifecycle ─────────────────────────────────────────────────────

  Scenario: Rain emitter spawns particles after the first frame
    Given a ParticleSystem named "rain" at position (600.0, 0.0) with maxParticles 300
    And the system has a RainBehavior
    When the system updates for 100 milliseconds
    Then the alive particle count should be greater than 0

  Scenario: Snow emitter spawns particles after the first frame
    Given a ParticleSystem named "snow" at position (600.0, 0.0) with maxParticles 300
    And the system has a SnowBehavior
    When the system updates for 100 milliseconds
    Then the alive particle count should be greater than 0

  Scenario: Fountain emitter spawns particles after the first frame
    Given a ParticleSystem named "fountain" at position (600.0, 500.0) with maxParticles 200
    And the system has a FountainBehavior
    When the system updates for 100 milliseconds
    Then the alive particle count should be greater than 0

  Scenario: Torch emitter spawns particles after the first frame
    Given a ParticleSystem named "torch" at position (300.0, 480.0) with maxParticles 120
    And the system has a TorchBehavior
    When the system updates for 100 milliseconds
    Then the alive particle count should be greater than 0

  # ─── maxParticles cap ──────────────────────────────────────────────────────

  Scenario: The alive count never exceeds maxParticles
    Given a ParticleSystem named "capped" at position (600.0, 0.0) with maxParticles 10
    And the system has a RainBehavior
    When the system updates 50 times for 100 milliseconds each
    Then the alive particle count should not exceed 10

  # ─── Particle death and recycling ─────────────────────────────────────────

  Scenario: An inactive system spawns no particles
    Given a ParticleSystem named "inactive" at position (600.0, 0.0) with maxParticles 200
    And the system has a RainBehavior
    And the system is inactive
    When the system updates for 100 milliseconds
    Then the alive particle count should equal 0

  Scenario: clearParticles kills all alive particles immediately
    Given a ParticleSystem named "clearable" at position (600.0, 0.0) with maxParticles 200
    And the system has a RainBehavior
    When the system updates for 100 milliseconds
    And the system particles are cleared
    Then the alive particle count should equal 0

  # ─── Physics type ──────────────────────────────────────────────────────────

  Scenario: A ParticleSystem entity has STATIC physics type
    Given a ParticleSystem named "static-check" at position (0.0, 0.0) with maxParticles 50
    Then the system physics type should be STATIC

  Scenario: A ParticleSystem entity has a zero bounding box
    Given a ParticleSystem named "zero-bb" at position (0.0, 0.0) with maxParticles 50
    Then the system width should be 0
    And the system height should be 0
