package com.core.editor.io.json;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for JSON serialization of {@link com.core.behavior.Behavior} references.
 * Stores the fully-qualified class name and optional parameter map for reconstruction.
 *
 * <p>During deserialization, the plugin registry is consulted to find a matching
 * behavior class and instantiate it with the given parameters (if supported).</p>
 */
public class JsonBehavior {
    public String className = "";
    public Map<String, Object> params = new HashMap<>();

    public JsonBehavior() {
    }

    public JsonBehavior(String className) {
        this.className = className;
    }

    public JsonBehavior(String className, Map<String, Object> params) {
        this.className = className;
        this.params = params != null ? new HashMap<>(params) : new HashMap<>();
    }
}
