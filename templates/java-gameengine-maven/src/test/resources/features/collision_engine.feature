Feature: CollisionEngine

  Scenario: Non-overlapping entities are not affected by the collision engine
    Given a dynamic entity A at (0.0, 0.0) with size 20x20 and velocity (10.0, 0.0)
    And a dynamic entity B at (100.0, 0.0) with size 20x20 and velocity (-10.0, 0.0)
    When the collision engine resolves
    Then entity A velocity X should equal 10.0
    And entity B velocity X should equal -10.0

  Scenario: Two overlapping dynamic entities are separated after resolution
    Given a dynamic entity A at (0.0, 0.0) with size 20x20 and velocity (10.0, 0.0)
    And a dynamic entity B at (10.0, 0.0) with size 20x20 and velocity (-10.0, 0.0)
    When the collision engine resolves
    Then entity A and entity B should not overlap

  Scenario: A dynamic entity bounces off a static entity
    Given a dynamic entity A at (0.0, 0.0) with size 20x20 and velocity (50.0, 0.0)
    And a static entity B at (10.0, 0.0) with size 20x20
    When the collision engine resolves
    Then entity A velocity X should be negative
    And entity B velocity X should equal 0.0

  Scenario: A heavier entity transfers more positional displacement to a lighter entity
    Given a heavy dynamic entity A at (0.0, 0.0) with size 20x20 mass 10.0 and velocity (30.0, 0.0)
    And a light dynamic entity B at (10.0, 0.0) with size 20x20 mass 1.0 and velocity (0.0, 0.0)
    When the collision engine resolves
    Then entity B position X change should be greater than entity A position X change

  Scenario: Two vertically overlapping dynamic entities are separated along the Y axis
    Given a dynamic entity A at (0.0, 0.0) with size 20x20 and velocity (0.0, 20.0)
    And a dynamic entity B at (0.0, 10.0) with size 20x20 and velocity (0.0, -20.0)
    When the collision engine resolves
    Then entity A and entity B should not overlap

  Scenario: A dynamic entity bouncing off a static entity below along the Y axis
    Given a dynamic entity A at (0.0, 0.0) with size 20x20 and velocity (0.0, 50.0)
    And a static entity B at (0.0, 10.0) with size 20x20
    When the collision engine resolves
    Then entity A velocity Y should be negative
    And entity B velocity Y should equal 0.0

  Scenario: A static entity A deflects a dynamic entity B upward along the Y axis
    Given a static entity A at (0.0, 0.0) with size 20x20
    And a dynamic entity B at (0.0, 10.0) with size 20x20 and velocity (0.0, -50.0)
    When the collision engine resolves
    Then entity B velocity Y should be greater than 0.0
    And entity A velocity Y should equal 0.0

  Scenario: A static entity A deflects a dynamic entity B along the X axis
    Given a static entity A at (0.0, 0.0) with size 20x20
    And a dynamic entity B at (10.0, 0.0) with size 20x20 and velocity (-50.0, 0.0)
    When the collision engine resolves
    Then entity B velocity X should be greater than 0.0
    And entity A velocity X should equal 0.0

  Scenario: An inactive entity B is skipped by the collision engine
    Given a dynamic entity A at (0.0, 0.0) with size 20x20 and velocity (10.0, 0.0)
    And an inactive dynamic entity B at (10.0, 0.0) with size 20x20 and velocity (-10.0, 0.0)
    When the collision engine resolves
    Then entity A velocity X should equal 10.0
    And entity B velocity X should equal -10.0

  Scenario: A NONE-type entity B is skipped by the collision engine
    Given a dynamic entity A at (0.0, 0.0) with size 20x20 and velocity (10.0, 0.0)
    And a NONE-type entity B at (10.0, 0.0) with size 20x20 and velocity (-10.0, 0.0)
    When the collision engine resolves
    Then entity A velocity X should equal 10.0
    And entity B velocity X should equal -10.0
