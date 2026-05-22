package com.core.editor.io;

import java.io.FileWriter;
import java.io.IOException;

import com.core.behavior.Behavior;
import com.core.editor.io.json.JsonBehavior;
import com.core.editor.io.json.JsonColor;
import com.core.editor.io.json.JsonEntity;
import com.core.editor.io.json.JsonMaterial;
import com.core.editor.io.json.JsonScene;
import com.core.editor.io.json.JsonWorld;
import com.core.entity.Entity;
import com.core.entity.World;
import com.core.gfx.Camera;
import com.core.scene.Scene;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Serializes a scene to JSON format (.scene.json).
 *
 * <p>Converts {@link Scene}, {@link Entity}, {@link World}, {@link Camera}, and
 * {@link Behavior} objects into JSON-serializable DTOs, then writes to disk using Gson.</p>
 */
public class SceneWriter {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public SceneWriter() {
    }

    /**
     * Writes a scene to a JSON file.
     *
     * @param scene the scene to write
     * @param path  file path (e.g., "path/to/scene.scene.json")
     * @throws IOException if file write fails
     */
    public void write(Scene scene, String path) throws IOException {
        JsonScene jsonScene = new JsonScene(path.substring(path.lastIndexOf('/') + 1).replace(".scene.json", ""));

        // Convert World
        World world = scene.getWorld();
        if (world != null) {
            jsonScene.world = convertWorld(world);
        }

        // Convert Entities
        for (Entity<?> entity : scene.getEntities()) {
            if (!(entity instanceof World)) {
                jsonScene.entities.add(convertEntity(entity));
            }
        }

        // Convert Cameras
        // TODO: Camera conversion when Camera class is explored
        // for (Camera cam : scene.getCameras()) {
        //     // Store cam.x, cam.y, cam.width, cam.height, cam.zoom, etc.
        // }

        // Write to file
        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(jsonScene, writer);
        }
    }

    private JsonWorld convertWorld(World world) {
        JsonWorld jw = new JsonWorld();
        jw.id = world.id;
        jw.name = world.name;
        jw.x = world.x;
        jw.y = world.y;
        jw.width = world.width;
        jw.height = world.height;
        jw.color = new JsonColor(world.color);
        jw.fillColor = new JsonColor(world.fillColor);
        jw.gravityX = world.gravityX;
        jw.gravityY = world.gravityY;
        jw.renderPriority = world.renderPriority;
        jw.active = world.active;
        jw.material = new JsonMaterial(world.material);

        // Convert behaviors
        for (Behavior b : world.behaviors) {
            jw.behaviors.add(convertBehavior(b));
        }

        // Convert children
        for (Entity<?> child : world.children) {
            jw.children.add(convertEntity(child));
        }

        return jw;
    }

    private JsonEntity convertEntity(Entity<?> entity) {
        JsonEntity je = new JsonEntity();
        je.id = entity.id;
        je.name = entity.name;
        je.type = entity.getClass().getSimpleName();
        je.x = entity.x;
        je.y = entity.y;
        je.vx = entity.vx;
        je.vy = entity.vy;
        je.rotation = entity.rotation;
        je.angularVelocity = entity.angularVelocity;
        je.mass = entity.mass;
        je.physicsType = entity.physicsType.name();
        je.material = new JsonMaterial(entity.material);
        je.width = entity.width;
        je.height = entity.height;
        je.color = new JsonColor(entity.color);
        je.fillColor = new JsonColor(entity.fillColor);
        je.renderPriority = entity.renderPriority;
        je.active = entity.active;

        // Convert behaviors
        for (Behavior b : entity.behaviors) {
            je.behaviors.add(convertBehavior(b));
        }

        // Convert children
        for (Entity<?> child : entity.children) {
            je.children.add(convertEntity(child));
        }

        return je;
    }

    private JsonBehavior convertBehavior(Behavior behavior) {
        return new JsonBehavior(behavior.getClass().getName());
        // TODO: Extract @BehaviorParam annotated constructor parameters for serialization
    }
}

