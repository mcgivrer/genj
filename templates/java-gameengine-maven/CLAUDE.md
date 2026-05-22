# CLAUDE.md — GameEngineDemo

This file provides context and conventions for AI assistants (Claude and others) working on this codebase.

---

## Project Overview

**GameEngineDemo** is a Java 2D demo application with a game loop, rendering layer, entity system, and a lightweight 2D physics stack.

- **Language**: Java 25 (Zulu JDK, managed via SDKMAN)
- **Build tool**: Maven 3.9.5
- **Group / artifact**: `com.demo` / `GameEngineDemo` / `1.0.0`
- **Main class**: `com.demo.DemoApp`

---

## Essential Commands

```bash
# Compile only
mvn -B -ntp compile

# Run all tests (unit + Cucumber BDD)
mvn -B -ntp test

# Full build (compile + test + package)
mvn -B -ntp clean package -DskipTests

# Run the application
mvn -B -ntp exec:java

# Run directly with Java
java -cp target/classes com.demo.DemoApp
```

> The project uses `.sdkmanrc` to pin `java=25-zulu` and `maven=3.9.5`. Run `sdk env` to activate the correct versions.

---

## Source Layout

```
src/
├── main/
│   ├── java/com/demo/
│   │   ├── DemoApp.java          ← main entry point, game loop, rendering
│   │   ├── Entity.java           ← generic base class for all simulation objects
│   │   ├── GameObject.java       ← concrete Entity subclass
│   │   ├── PhysicsType.java      ← enum: NONE | STATIC | DYNAMIC
│   │   ├── Material.java         ← physical material profiles + computeMass()
│   │   ├── World.java            ← simulation bounds + gravity
│   │   ├── PhysicsEngine.java    ← gravity, damping, containment, collision pass
│   │   ├── CollisionEngine.java  ← AABB detection + mass-based elastic response
│   │   ├── CircularQueue.java    ← utility: fixed-size circular queue
│   │   └── Colors.java           ← utility: color helpers
│   └── resources/
│       ├── config.properties
│       └── i18n/messages.properties
└── test/
    ├── java/com/demo/
    │   ├── EntityTest.java           ← JUnit 5 unit tests
    │   ├── GameObjectTest.java
    │   ├── MaterialTest.java
    │   ├── WorldTest.java
    │   ├── CucumberTestSuite.java    ← JUnit Platform Suite runner for Cucumber
    │   └── steps/
    │       ├── PhysicsEngineSteps.java
    │       └── CollisionEngineSteps.java
    └── resources/
        ├── junit-platform.properties
        └── features/
            ├── physics_engine.feature
            └── collision_engine.feature
```

---

## Architecture

### Physics pipeline (per frame)

```
PhysicsEngine.update(world, entities, elapsed)
  └─ for each DYNAMIC entity:
       1. apply gravity  (vx += gx*dt, vy += gy*dt)
       2. apply damping  (v *= 1 - friction*0.02)
       3. entity.update(elapsed)  → move by velocity
       4. containInWorld()        → clamp to bounds, bounce
  └─ CollisionEngine.resolve(entities)
       └─ AABB pair loop → resolvePair(a, b)
            mass-weighted positional separation
            1D elastic collision formula with restitution
```

### Key design decisions

- All physics fields (`x`, `y`, `vx`, `vy`, `mass`, etc.) are public `float` on `Entity` — intentional for simplicity in a demo context.
- `Entity<T>` is generic to support fluent builder chains returning the concrete subtype.
- `Material.computeMass(w, h)` = `max(0.1, density * max(1,w) * max(1,h) * 0.01)`.
- `CollisionEngine` uses a coefficient of restitution averaged from the two materials' `elasticity` values.

---

## Testing Conventions

### Unit tests (`*Test.java`)

- One test class per production class, in `com.demo` package under `src/test/java`.
- Use `@Nested` to group by method or concern.
- Use `@DisplayName` on every class and `@Test` method.
- Use `@BeforeEach` for state reset; never share mutable state across tests.
- Use `assertEquals(expected, actual, delta)` for all `float` comparisons (delta `0.001f`).
- Use `@ParameterizedTest` + `@CsvSource` for boundary/combinatorial cases.

### Cucumber BDD scenarios (`*.feature`)

- Feature files live in `src/test/resources/features/`.
- Glue package: `com.demo.steps`.
- **Do not use Cucumber Expressions** when step text contains parentheses `(…)` — use **regex** instead (Cucumber Expressions parse `(` as an optional group):
  ```java
  // ✓ correct
  @Given("^a world of size (\\d+)x(\\d+) with gravity \\((-?[\\d.]+), (-?[\\d.]+)\\)$")
  ```
- One `*Steps.java` class per production component under test.
- `CucumberTestSuite` is the only Surefire entry point for BDD tests (matched by the `*Suite` include pattern in `pom.xml`).

---

## Build Profiles and Packaging

| Profile | OS trigger | Output |
|---|---|---|
| `package-windows` | `family=Windows` | `target/dist/*.exe` (requires WiX) |
| `package-linux` | `name=Linux` | `target/dist/*.deb` + `*.rpm` (requires `fakeroot`, `rpm`) |
| `package-macos` | `family=mac` | `target/dist/*.dmg` |

Packaging uses `maven-jlink-plugin` to build a trimmed runtime (`target/runtime`) with modules `java.base`, `java.desktop`, `java.logging`, then `jpackage` via `exec-maven-plugin`.

---

## Documentation

All technical and functional documentation is in `src/docs/`:

| File | Topic |
|---|---|
| `00-overview.md` | Project overview and simulation pipeline |
| `01-entity.md` | `Entity` class |
| `02-gameobject.md` | `GameObject` class |
| `03-physicsengine.md` | `PhysicsEngine` class |
| `04-world.md` | `World` class |
| `05-material.md` | `Material` class |
| `06-collisionengine.md` | `CollisionEngine` class |
| `07-build-packaging.md` | Maven build and OS packaging |
| `08-testing.md` | JUnit 5 unit tests + Cucumber BDD |

Diagrams use Mermaid (inline in Markdown) and PlantUML. SVG illustrations are in `src/docs/illustrations/`.

---

## What to Avoid

- Do not add `System.out.println` debug output — use the existing logging or omit.
- Do not change `float` fields on `Entity` to `double` — the physics engine assumes `float` throughout.
- Do not add dependencies without updating `src/docs/08-testing.md` (for test deps) or `src/docs/07-build-packaging.md` (for build deps).
- Do not use Cucumber Expressions in step definitions that contain literal parentheses in the step text.
