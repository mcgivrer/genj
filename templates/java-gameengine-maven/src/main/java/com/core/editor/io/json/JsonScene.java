package com.core.editor.io.json;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for JSON serialization of a complete scene.
 * Contains the world, all entities, and all cameras.
 *
 * <p>Example JSON structure:</p>
 * <pre>{@code
 * {
 *   "sceneName": "Level1",
 *   "world": {
 *     "type": "World",
 *     "name": "world",
 *     "width": 1600,
 *     "height": 900,
 *     "gravityX": 0.0,
 *     "gravityY": 200.0
 *   },
 *   "entities": [
 *     {
 *       "type": "GameObject",
 *       "name": "player",
 *       "x": 100.0,
 *       "y": 200.0,
 *       "width": 50,
 *       "height": 50
 *     },
 *     ...
 *   ],
 *   "cameras": [
 *     {
 *       "name": "main",
 *       "x": 0.0,
 *       "y": 0.0,
 *       "width": 800,
 *       "height": 600,
 *       "zoom": 1.0
 *     }
 *   ]
 * }
 * }</pre>
 */
public class JsonScene {
    public String sceneName = "unnamed";
    public JsonWorld world = new JsonWorld();
    public List<JsonEntity> entities = new ArrayList<>();
    public List<JsonCamera> cameras = new ArrayList<>();

    public JsonScene() {
    }

    public JsonScene(String sceneName) {
        this.sceneName = sceneName;
    }
}
