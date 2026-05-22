# 06 - CollisionEngine Component

**Package:** `com.core.physics`

## Functional Role

CollisionEngine detects AABB overlaps and applies collision response.

- detection: rectangle-rectangle overlap test (AABB)
- separation axis selection: smallest overlap (X or Y)
- type handling: DYNAMIC vs DYNAMIC, DYNAMIC vs STATIC, ignore NONE

## AABB Detection

```mermaid
flowchart LR
    A[entity a] --> T{ intersect a et b }
    B[entity b] --> T
    T -- true --> R[resolvePair]
    T -- false --> N[no collision]
```

## Mass-Based Response (DYNAMIC vs DYNAMIC)

1. Mass-weighted positional separation.
2. Velocity update with average restitution `e`.

1D formulas per axis:

$$
v'_a = \frac{(m_a - e m_b) v_a + (1 + e)m_b v_b}{m_a + m_b}
$$

$$
v'_b = \frac{(m_b - e m_a) v_b + (1 + e)m_a v_a}{m_a + m_b}
$$

Average friction reduces tangential velocity after impact.

### Angular Impulse on Collision

After resolving each pair's linear velocities, `CollisionEngine` injects an angular impulse on both entities via `applyCollisionAngularImpulse()`. The impulse is proportional to the change in tangential velocity at the contact edge:

$$
I = \frac{m(w^2 + h^2)}{12} \qquad \ell = \begin{cases} h/2 & \text{X-axis separation (vertical wall contact)} \\ w/2 & \text{Y-axis separation (horizontal contact)} \end{cases}
$$

$$
\omega \leftarrow \omega + \frac{v_{tangent,\,pre} \cdot \ell \cdot friction}{I}
$$

where $v_{tangent,\,pre}$ is the tangential velocity component **before** the collision response, $friction$ is the average of the two entities' `material.friction`, and $I$ is the rectangular moment of inertia.

This produces physically plausible spin: an entity hit from the side picks up rotation in the direction of the tangential impact force.

## Sequence Diagram

```plantuml
@startuml
participant PhysicsEngine
participant CollisionEngine
participant EntityA
participant EntityB

PhysicsEngine -> CollisionEngine : resolve(entities)
CollisionEngine -> EntityA : isIntersect(EntityB)
CollisionEngine -> CollisionEngine : compute overlap axis
CollisionEngine -> CollisionEngine : separate by inverse mass
CollisionEngine -> CollisionEngine : compute new velocities
CollisionEngine -> EntityA : apply vx/vy
CollisionEngine -> EntityB : apply vx/vy
CollisionEngine -> CollisionEngine : applyCollisionAngularImpulse(EntityA)
CollisionEngine -> CollisionEngine : applyCollisionAngularImpulse(EntityB)
CollisionEngine -> EntityA : apply angularVelocity
CollisionEngine -> EntityB : apply angularVelocity
@enduml
```

## Class Diagram (PlantUML)

```plantuml
@startuml
class PhysicsEngine {
    - collisionEngine : CollisionEngine
    + update(world : World, entities : List<Entity>, elapsed : long)
}

class CollisionEngine {
    + resolve(entities : List<Entity>)
    - resolvePair(a : Entity, b : Entity)
    - applyCollisionAngularImpulse(e : Entity, preVx : float, preVy : float, separateX : boolean, friction : float)
}

class Entity {
    + x : float
    + y : float
    + vx : float
    + vy : float
    + rotation : float
    + angularVelocity : float
    + width : int
    + height : int
    + mass : float
    + physicsType : PhysicsType
    + material : Material
    + active : boolean
    + isIntersect(other : Entity) : boolean
}

class Material {
    + density : float
    + friction : float
    + elasticity : float
    + rotationalFriction : float
}

class World {
    + gravityX : float
    + gravityY : float
}

enum PhysicsType {
    NONE
    STATIC
    DYNAMIC
}

PhysicsEngine --> CollisionEngine : uses
PhysicsEngine --> World : reads gravity
PhysicsEngine --> Entity : updates
CollisionEngine --> Entity : resolves collisions
Entity --> Material : references
Entity --> PhysicsType : typed by
World --|> Entity
@enduml
```

## Illustration

![Mass-based collision](./illustrations/collision-mass-response.svg)
