package com.core.editor.io;

import java.util.Map;

import com.core.App;
import com.core.scene.BaseScene;

/**
 * Temporary wrapper scene used during scene deserialization.
 * Allows SceneReader to construct scenes without requiring a live App reference.
 *
 * <p>Once the scene is loaded, it should be reassigned to a live App in the normal way.</p>
 */
public class LoadedScene extends BaseScene {
    public LoadedScene() {
        super(null);
    }

    @Override
    public void create(App app) {
        // No-op: content is loaded via SceneReader
    }

    @Override
    public void update(App app, Map<String, Object> stats, float deltaTime) {
        // No-op: loaded scene may not be active yet
    }
}
