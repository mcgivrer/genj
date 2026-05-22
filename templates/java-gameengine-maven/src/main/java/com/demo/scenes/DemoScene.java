package com.demo.scenes;

import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.Random;

import com.core.App;
import com.core.behavior.CameraTrackingBehavior;
import com.core.behavior.HorizontalPatrolBehavior;
import com.core.behavior.PlayerInputBehavior;
import com.core.behavior.VerticalPatrolBehavior;
import com.core.behavior.VisualDebugBehavior;
import com.core.behavior.WaypointBehavior;
import com.core.behavior.particle.RainBehavior;
import com.core.entity.GameObject;
import com.core.entity.ParticleSystem;
import com.core.gfx.plugin.ParticleSystemRenderer;
import com.core.entity.Nature;
import com.core.entity.TextObject;
import com.core.entity.World;
import com.core.gfx.Camera;
import com.core.physics.Material;
import com.core.physics.PhysicsType;
import com.core.scene.BaseScene;
import com.core.utils.Colors;
import com.core.utils.TextAlign;

import static com.core.App.getI18n;

public class DemoScene extends BaseScene {

        public DemoScene(App app) {
                super(app);
        }

        @Override
        public void create(App app) {
                // Gravity: 15 m/s² downward (Earth ≈ 9.81 — deliberately stronger for snappier
                // platformer feel)
                // PPM=50 → 15 × 50 = 750 px/s²
                setWorld(new World("world")
                                .setPosition(0, 0)
                                .setSize(1200, 800)
                                .setGravityMs2(0f, 15f));

                // add a main object (the future player object)
                // Material.STONE: densité 2.4 → masse ~18.4 (vs 4.6 avec WOOD), friction
                // élevée, faible rebond
                add(new GameObject("player")
                                .setSize(24, 32)
                                .setPosition(world.getWidth() * 0.5f, world.getHeight() * 0.5f)
                                .setPhysicsType(PhysicsType.DYNAMIC)
                                .setMaterial(Material.STONE)
                                .setColor(Color.WHITE)
                                .setMass(20.0f)
                                .setFillColor(Color.BLUE)
                                .addBehavior(new PlayerInputBehavior(200f, -480f))
                                .setRenderPriority(10));

                // and random squares
                Random rand = new Random(1234);
                for (int i = 0; i < 200; i++) {
                        add(new GameObject("box_%s".formatted(i))
                                        .setPosition(
                                                        rand.nextFloat(((int) (world.getWidth() - 8) / 8) * 8f),
                                                        rand.nextFloat(((int) (world.getHeight() * 0.5) / 8) * 8f))
                                        .setSize(8, 8)
                                        .setPhysicsType(PhysicsType.DYNAMIC)
                                        .setMaterial(i % 2 == 0 ? Material.RUBBER : Material.METAL)
                                        .setColor(Color.BLACK)
                                        .setFillColor(Colors.random())
                                        .setNature(Nature.ELLIPSE));
                }

                // ── Ground ──────────────────────────────────────────────────────────────
                add(new GameObject("ground")
                                .setPosition(0, world.getHeight() - 24)
                                .setSize(world.getWidth(), 24)
                                .setPhysicsType(PhysicsType.STATIC)
                                .setMaterial(Material.STONE)
                                .setColor(Color.DARK_GRAY)
                                .setFillColor(new Color(60, 60, 60)));

                // ── Fixed platforms ──────────────────────────────────────────────────────
                // ICE : friction≈0, élasticité≈0 — le joueur glisse latéralement sans s'arrêter
                add(new GameObject("plat_high_left")
                                .setPosition(100, 550)
                                .setSize(250, 16)
                                .setPhysicsType(PhysicsType.STATIC)
                                .setMaterial(Material.ICE)
                                .setColor(new Color(160, 220, 240))
                                .setFillColor(new Color(100, 180, 210)));

                // RUBBER : élasticité 0.85 — l'atterrissage fait rebondir le joueur
                add(new GameObject("plat_mid_1")
                                .setPosition(350, 480)
                                .setSize(180, 16)
                                .setPhysicsType(PhysicsType.STATIC)
                                .setMaterial(Material.RUBBER)
                                .setColor(new Color(220, 160, 50))
                                .setFillColor(new Color(180, 110, 20)));

                // METAL : friction 0.2, élasticité 0.1 — légèrement glissant, peu rebondissant
                add(new GameObject("plat_mid_2")
                                .setPosition(650, 420)
                                .setSize(180, 16)
                                .setPhysicsType(PhysicsType.STATIC)
                                .setMaterial(Material.METAL)
                                .setColor(new Color(180, 190, 200))
                                .setFillColor(new Color(120, 130, 145)));

                add(new GameObject("plat_high_right")
                                .setPosition(900, 350)
                                .setSize(250, 16)
                                .setPhysicsType(PhysicsType.STATIC)
                                .setMaterial(Material.STONE)
                                .setColor(Color.GRAY)
                                .setFillColor(new Color(80, 80, 80)));

                // ── Moving platforms ─────────────────────────────────────────────────────

                // Horizontal patrol: moves between x=200 and x=520
                // ICE : plateforme mobile glissante — difficile à rester dessus
                add(new GameObject("plat_h_moving")
                                .setPosition(200, 650)
                                .setSize(160, 16)
                                .setPhysicsType(PhysicsType.STATIC)
                                .setMaterial(Material.ICE)
                                .setColor(new Color(160, 220, 240))
                                .setFillColor(new Color(100, 180, 210))
                                .addBehavior(new HorizontalPatrolBehavior(200f, 520f, 90f)));

                // Vertical patrol: moves between y=500 and y=680
                // RUBBER : ascenseur rebondissant — le joueur rebondit légèrement à chaque
                // montée
                add(new GameObject("plat_v_moving")
                                .setPosition(600, 600)
                                .setSize(160, 16)
                                .setPhysicsType(PhysicsType.STATIC)
                                .setMaterial(Material.RUBBER)
                                .setColor(new Color(220, 160, 50))
                                .setFillColor(new Color(180, 110, 20))
                                .addBehavior(new VerticalPatrolBehavior(500f, 680f, 70f)));

                // Waypoint patrol: follows a triangular path, looping
                add(new GameObject("plat_waypoint")
                                .setPosition(800, 550)
                                .setSize(140, 16)
                                .setPhysicsType(PhysicsType.STATIC)
                                .setMaterial(Material.WOOD)
                                .setColor(new Color(80, 160, 80))
                                .setFillColor(new Color(50, 120, 50))
                                .addBehavior(new WaypointBehavior(
                                                List.of(
                                                                new float[] { 870f, 550f },
                                                                new float[] { 1000f, 460f },
                                                                new float[] { 1050f, 580f },
                                                                new float[] { 870f, 640f }),
                                                80f, true, false)));

                add(new TextObject("hud_welcome")
                                .setText(getI18n("app.message.welcome", "Welcome into this demo"))
                                .setFontSize(24f)
                                .setFontStyle(Font.BOLD)
                                .setAlign(TextAlign.LEFT)
                                .setColor(Color.WHITE)
                                .setHud(true)
                                .setRelX(0.1f)
                                .setRelY(0.1f)
                                .setRenderPriority(100));

                add(new TextObject("hud_exit")
                                .setText(getI18n("app.message.exit", "press ESCAPE to exit"))
                                .setFontSize(12f)
                                .setFontStyle(Font.BOLD)
                                .setAlign(TextAlign.RIGHT)
                                .setColor(Color.GRAY)
                                .setHud(true)
                                .setRelX(0.95f)
                                .setRelY(0.95f)
                                .setRenderPriority(100));

                // ── Cameras ─────────────────────────────────────────────────────────────

                // Main camera: follows the player with elastic tracking
                // tiltX=0.5 → player centred horizontally
                // tiltY=0.75 → player sits at 75% of viewport height (more sky visible)
                // elasticity=0.85 → smooth, slightly laggy follow
                addCamera(new Camera("main_camera")
                                .setZoom(1.0f)
                                .addBehavior(new CameraTrackingBehavior(getEntity("player"), world)
                                                .setTiltX(0.5f)
                                                .setTiltY(0.75f)
                                                .setElasticity(0.85f)));

                // Minimap camera: fixed top-left at world origin, shows the full scene
                // in a 100×100 overlay positioned in the bottom-right corner of the window
                int minimapW = 100;
                int minimapH = 100;
                int winW = app.getRenderer().getWindowWidth();
                int winH = app.getRenderer().getWindowHeight();
                float minimapZoom = Math.min((float) minimapW / world.getWidth(),
                                (float) minimapH / world.getHeight());
                addCamera(new Camera("minimap_camera")
                                .setViewport(winW - minimapW - 10, winH - minimapH - 10, minimapW, minimapH)
                                .setZoom(minimapZoom)
                                .setDrawBorder(true));

                // ── Rain particle system ─────────────────────────────────────────────────
                app.getRenderer().registerPlugin(new ParticleSystemRenderer());

                RainBehavior rain = new RainBehavior(world)
                                .setWidth(world.getWidth())
                                .setWind(20f);
                add(new ParticleSystem("rain")
                                .setPosition(world.getWidth() * 0.5f, -10)
                                .setMaxParticles(400)
                                .addBehavior(rain)
                                .setRenderPriority(50));

                // ── VisualDebugBehavior — attached to every entity ───────────────────────
                getEntities().forEach(e -> e.addBehavior(new VisualDebugBehavior(0)));

                // Provide the live entity list so rain particles bounce off static objects
                rain.setObstacles(getEntities());
        }

        @Override
        public void update(com.core.App app, java.util.Map<String, Object> stats, float deltaTime) {
                // Update your scene logic here using the provided stats and deltaTime
        }

}
