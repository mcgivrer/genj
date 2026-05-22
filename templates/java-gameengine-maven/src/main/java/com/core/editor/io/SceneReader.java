package com.core.editor.io;

import java.io.FileReader;
import java.io.IOException;

import com.core.behavior.Behavior;
import com.core.editor.io.json.JsonBehavior;
import com.core.editor.io.json.JsonEntity;
import com.core.editor.io.json.JsonScene;
import com.core.entity.Entity;
import com.core.entity.GameObject;
import com.core.entity.World;
import com.core.physics.PhysicsType;
import com.core.scene.Scene;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Deserializes a scene from JSON format (.scene.json).
 *
 * <p>Converts JSON-serialized DTOs back into {@link Scene}, {@link Entity}, {@link World},
 * and {@link Behavior} objects. Supports reconstruction of behaviors via reflection.</p>
 */
public class SceneReader {
    private static final Gson gson = new GsonBuilder().create();

    public SceneReader() {
    }

    /**
     * Reads a scene from a JSON file.
     *
     * @param path file path (e.g., "path/to/scene.scene.json")
     * @return the loaded scene
     * @throws IOException if file read fails
     */
    public Scene read(String path) throws IOException {
        JsonScene jsonScene;
        try (FileReader reader = new FileReader(path)) {
            jsonScene = gson.fromJson(reader, JsonScene.class);
        }

        // Create a LoadedScene container (supports null App for deserialization)
        LoadedScene scene = new LoadedScene();

        // Reconstruct World
        if (jsonScene.world != null) {
            World world = reconstructWorld(jsonScene.world);
            // Note: BaseScene stores entities in a list; World is typically managed separately
            scene.getEntities().add(world);
        }

        // Reconstruct Entities
        for (JsonEntity je : jsonScene.entities) {
            Entity<?> entity = reconstructEntity(je);
            scene.getEntities().add(entity);
        }

        // Reconstruct Cameras
        // TODO: Camera reconstruction when Camera class is explored

        return scene;
    }

    private World reconstructWorld(JsonEntity jsonWorld) {
        World world = new World(jsonWorld.name);
        world.id = jsonWorld.id;
        world.x = jsonWorld.x;
        world.y = jsonWorld.y;
        world.vx = jsonWorld.vx;
        world.vy = jsonWorld.vy;
        world.width = jsonWorld.width;
        world.height = jsonWorld.height;
        world.color = jsonWorld.color.toColor();
        world.fillColor = jsonWorld.fillColor.toColor();
        world.renderPriority = jsonWorld.renderPriority;
        world.active = jsonWorld.active;

        if (jsonWorld instanceof com.core.editor.io.json.JsonWorld jw) {
            world.gravityX = jw.gravityX;
            world.gravityY = jw.gravityY;
            // Set material from JsonMaterial
            if (jw.material != null) {
                world.material = jw.material.toMaterial();
            }
        }

        // Reconstruct behaviors
        for (JsonBehavior jb : jsonWorld.behaviors) {
            Behavior b = reconstructBehavior(jb);
            if (b != null) {
                world.behaviors.add(b);
            }
        }

        // Reconstruct children
        for (JsonEntity je : jsonWorld.children) {
            Entity<?> child = reconstructEntity(je);
            world.children.add(child);
        }

        return world;
    }

    private Entity<?> reconstructEntity(JsonEntity jsonEntity) {
        // Determine entity type
        Entity<?> entity;
        if ("World".equals(jsonEntity.type)) {
            entity = reconstructWorld(jsonEntity);
        } else {
            // Default to GameObject for most entities
            entity = new GameObject(jsonEntity.name);
            entity.id = jsonEntity.id;
            entity.x = jsonEntity.x;
            entity.y = jsonEntity.y;
            entity.vx = jsonEntity.vx;
            entity.vy = jsonEntity.vy;
            entity.rotation = jsonEntity.rotation;
            entity.angularVelocity = jsonEntity.angularVelocity;
            entity.mass = jsonEntity.mass;
            entity.width = jsonEntity.width;
            entity.height = jsonEntity.height;
            entity.color = jsonEntity.color.toColor();
            entity.fillColor = jsonEntity.fillColor.toColor();
            entity.renderPriority = jsonEntity.renderPriority;
            entity.active = jsonEntity.active;

            // Set physics type
            try {
                entity.physicsType = PhysicsType.valueOf(jsonEntity.physicsType);
            } catch (IllegalArgumentException e) {
                entity.physicsType = PhysicsType.DYNAMIC;
            }

            // Set material from JsonMaterial (now a complete Material with all attributes)
            if (jsonEntity.material != null) {
                entity.material = jsonEntity.material.toMaterial();
            }

            // Reconstruct behaviors
            for (JsonBehavior jb : jsonEntity.behaviors) {
                Behavior b = reconstructBehavior(jb);
                if (b != null) {
                    entity.behaviors.add(b);
                }
            }

            // Reconstruct children
            for (JsonEntity je : jsonEntity.children) {
                Entity<?> child = reconstructEntity(je);
                entity.children.add(child);
            }
        }

        return entity;
    }

    private Behavior reconstructBehavior(JsonBehavior jsonBehavior) {
        try {
            Class<?> behaviorClass = Class.forName(jsonBehavior.className);
            if (Behavior.class.isAssignableFrom(behaviorClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends Behavior> bc = (Class<? extends Behavior>) behaviorClass;

                // Try to instantiate with no-arg constructor
                // TODO: Support parameterized construction via @BehaviorParam and jsonBehavior.params
                return bc.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            System.err.println("Failed to reconstruct behavior: " + jsonBehavior.className + ": " + e.getMessage());
        }
        return null;
    }
}

