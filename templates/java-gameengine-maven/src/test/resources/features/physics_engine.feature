Feature: PhysicsEngine

  Background:
    Given a world of size 800x600 with gravity (0.0, 200.0)

  Scenario: Gravity accelerates a dynamic entity
    Given a dynamic entity at position (100.0, 100.0) with size 20x20 and velocity (0.0, 0.0)
    When the physics engine updates for 100 milliseconds
    Then the entity velocity Y should be greater than 0.0

  Scenario: A static entity is not accelerated by gravity
    Given a static entity at position (100.0, 100.0) with size 20x20 and velocity (0.0, 0.0)
    When the physics engine updates for 100 milliseconds
    Then the entity velocity Y should equal 0.0

  Scenario: A NONE-type entity is completely skipped by the engine
    Given a NONE-type entity at position (100.0, 100.0) with size 20x20 and velocity (0.0, 5.0)
    When the physics engine updates for 100 milliseconds
    Then the entity velocity Y should equal 5.0

  Scenario: An inactive entity is not processed
    Given an inactive dynamic entity at position (100.0, 100.0) with size 20x20 and velocity (0.0, 0.0)
    When the physics engine updates for 100 milliseconds
    Then the entity velocity Y should equal 0.0

  Scenario: A dynamic entity is constrained at the bottom world boundary
    Given a dynamic entity at position (100.0, 590.0) with size 20x20 and velocity (0.0, 5000.0)
    When the physics engine updates for 100 milliseconds
    Then the entity bottom edge should not exceed the world height 600

  Scenario: A dynamic entity bounces off the right world boundary with reversed X velocity
    Given a dynamic entity at position (790.0, 100.0) with size 20x20 and velocity (5000.0, 0.0)
    When the physics engine updates for 100 milliseconds
    Then the entity right edge should not exceed the world width 800
    And the entity velocity X should be negative

  Scenario: A dynamic entity bounces off the left world boundary with reversed X velocity
    Given a dynamic entity at position (5.0, 100.0) with size 20x20 and velocity (-5000.0, 0.0)
    When the physics engine updates for 100 milliseconds
    Then the entity left edge should not be less than the world origin 0
    And the entity velocity X should be greater than 0.0

  Scenario: A dynamic entity bounces off the top world boundary with reversed Y velocity
    Given a dynamic entity at position (100.0, 5.0) with size 20x20 and velocity (0.0, -5000.0)
    When the physics engine updates for 100 milliseconds
    Then the entity top edge should not be less than the world origin 0
    And the entity velocity Y should be greater than 0.0

  Scenario: Angular velocity is damped over time by rotational friction
    Given a dynamic entity at position (100.0, 100.0) with size 20x20 and velocity (0.0, 0.0) and angular velocity 10.0
    When the physics engine updates for 100 milliseconds
    Then the entity angular velocity should be less than 10.0

  Scenario: A rectangle entity below tipping speed has zero angular velocity
    Given a rectangle entity at position (100.0, 100.0) with size 20x20 velocity (1.0, 0.0) and angular velocity 5.0
    When the physics engine updates for 100 milliseconds
    Then the entity angular velocity should equal 0.0

  Scenario: A rectangle entity above tipping speed keeps angular velocity
    Given a rectangle entity at position (100.0, 100.0) with size 20x20 velocity (5000.0, 0.0) and angular velocity 5.0
    When the physics engine updates for 100 milliseconds
    Then the entity angular velocity should be greater than 0.0

  Scenario: An ellipse entity always keeps angular velocity regardless of speed
    Given an ellipse entity at position (100.0, 100.0) with size 20x20 velocity (1.0, 0.0) and angular velocity 5.0
    When the physics engine updates for 100 milliseconds
    Then the entity angular velocity should be greater than 0.0

  Scenario: Bouncing off a wall generates angular velocity from lateral motion
    Given a dynamic entity at position (780.0, 100.0) with size 20x20 and velocity (500.0, 200.0)
    When the physics engine updates for 100 milliseconds
    Then the entity angular velocity should not equal 0.0
