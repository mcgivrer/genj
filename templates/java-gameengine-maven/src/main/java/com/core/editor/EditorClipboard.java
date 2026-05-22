package com.core.editor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.core.editor.io.json.JsonEntity;
import com.core.entity.Entity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Clipboard manager for editor copy/paste/clone operations.
 * 
 * Stores serialized entity JSON internally and provides methods for:
 * - copy(entity) — copy entity to clipboard
 * - paste(world) — paste from clipboard into world (with +16px offset)
 * - clone(entity, world) — copy + paste as single operation
 * - delete(entity, world) — remove entity with confirmation if has children
 * 
 * Uses Gson for entity serialization/deserialization.
 */
public class EditorClipboard {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private JsonEntity clipboardContent;
    private Set<String> worldEntityNames = new HashSet<>();

    public EditorClipboard() {
        this.clipboardContent = null;
    }

    /**
     * Copy entity to clipboard.
     * 
     * @param entity the entity to copy
     */
    public void copy(Entity<?> entity) {
        if (entity == null) return;
        this.clipboardContent = entityToJson(entity);
    }

    /**
     * Cut entity (copy + delete from world).
     * 
     * @param entity the entity to cut
     * @param world the world containing the entity
     * @return true if cut succeeded
     */
    public boolean cut(Entity<?> entity, com.core.entity.World world) {
        if (entity == null) return false;
        copy(entity);
        return delete(entity, world);
    }

    /**
     * Paste entity from clipboard into world.
     * Creates a new entity with offset position (+16px) and unique name.
     * 
     * @param world the world to paste into
     * @return the pasted entity, or null if clipboard is empty
     */
    public Entity<?> paste(com.core.entity.World world) {
        if (clipboardContent == null || world == null) return null;
        
        // Update world entity names
        updateWorldEntityNames(world);
        
        // Create unique name for pasted entity
        String baseName = clipboardContent.name != null ? clipboardContent.name : "entity";
        String uniqueName = generateUniqueName(baseName);
        
        // Clone JSON and apply offset
        JsonEntity pastedJson = cloneJsonEntity(clipboardContent);
        pastedJson.name = uniqueName;
        pastedJson.x = clipboardContent.x + 16f;
        pastedJson.y = clipboardContent.y + 16f;
        
        // Reconstruct entity from JSON
        Entity<?> pastedEntity = jsonToEntity(pastedJson);
        if (pastedEntity != null) {
            world.add(pastedEntity);
        }
        return pastedEntity;
    }

    /**
     * Clone entity (copy + paste as single operation).
     * Does not clear clipboard.
     * 
     * @param entity the entity to clone
     * @param world the world to add cloned entity to
     * @return the cloned entity, or null if failed
     */
    public Entity<?> clone(Entity<?> entity, com.core.entity.World world) {
        if (entity == null || world == null) return null;
        
        copy(entity);
        Entity<?> cloned = paste(world);
        
        // Clipboard still contains the original for further pastes
        return cloned;
    }

    /**
     * Delete entity from world with confirmation if has children.
     * 
     * @param entity the entity to delete
     * @param world the world containing the entity
     * @return true if deleted or user confirmed, false if cancelled
     */
    public boolean delete(Entity<?> entity, com.core.entity.World world) {
        if (entity == null || world == null) return false;
        
        // Check if has children
        if (!entity.children.isEmpty()) {
            int response = javax.swing.JOptionPane.showConfirmDialog(
                null,
                "Entity '" + entity.name + "' has " + entity.children.size() + " child entity(ies).\n" +
                "Delete this entity and all its children?",
                "Delete Entity",
                javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            if (response != javax.swing.JOptionPane.YES_OPTION) {
                return false;
            }
        }
        
        // Find entity in world or its children and remove it
        boolean removed = world.children.remove(entity);
        if (!removed) {
            // Search in world's children recursively
            removed = removeEntityFromChildren(world, entity);
        }
        
        return removed;
    }

    /**
     * Check if clipboard has content.
     * 
     * @return true if clipboard is not empty
     */
    public boolean hasContent() {
        return clipboardContent != null;
    }

    /**
     * Clear clipboard.
     */
    public void clear() {
        clipboardContent = null;
    }

    // ─── Helper methods ──────────────────────────────────────────────────────

    /**
     * Converts an Entity to JSON DTO.
     */
    private JsonEntity entityToJson(Entity<?> entity) {
        if (entity == null) return null;
        
        JsonEntity json = new JsonEntity();
        json.name = entity.name;
        json.x = entity.x;
        json.y = entity.y;
        json.width = entity.width;
        json.height = entity.height;
        json.vx = entity.vx;
        json.vy = entity.vy;
        json.mass = entity.mass;
        json.rotation = entity.rotation;
        json.angularVelocity = entity.angularVelocity;
        json.renderPriority = entity.renderPriority;
        json.physicsType = entity.physicsType.name();
        json.active = entity.active;
        
        // Material
        if (entity.material != null) {
            json.material = new com.core.editor.io.json.JsonMaterial(entity.material);
        }
        
        // Color fields (use JsonColor constructor)
        json.color = new com.core.editor.io.json.JsonColor(entity.color);
        json.fillColor = new com.core.editor.io.json.JsonColor(entity.fillColor);
        
        // Type-specific fields
        if (entity instanceof com.core.entity.GameObject go) {
            json.type = "GameObject";
        } else if (entity instanceof com.core.entity.TextObject to) {
            json.type = "TextObject";
            json.text = to.text;
            json.fontSize = to.fontSize;
            json.hud = to.hud;
        } else {
            json.type = entity.getClass().getSimpleName();
        }
        
        // Behaviors
        json.behaviors = new ArrayList<>();
        for (var behavior : entity.behaviors) {
            json.behaviors.add(new com.core.editor.io.json.JsonBehavior(
                behavior.getClass().getName()
            ));
        }
        
        return json;
    }

    /**
     * Converts JSON DTO back to Entity.
     */
    private Entity<?> jsonToEntity(JsonEntity json) {
        if (json == null) return null;
        
        try {
            Entity<?> entity = null;
            
            // Reconstruct type-specific entity
            if ("GameObject".equals(json.type)) {
                var go = new com.core.entity.GameObject(json.name);
                go.x = json.x;
                go.y = json.y;
                go.width = json.width;
                go.height = json.height;
                go.vx = json.vx;
                go.vy = json.vy;
                go.mass = json.mass;
                go.rotation = json.rotation;
                go.angularVelocity = json.angularVelocity;
                go.renderPriority = json.renderPriority;
                go.physicsType = com.core.physics.PhysicsType.valueOf(json.physicsType);
                go.active = json.active;
                
                if (json.color != null) {
                    go.color = json.color.toColor();
                }
                if (json.fillColor != null) {
                    go.fillColor = json.fillColor.toColor();
                }
                
                entity = go;
            } else if ("TextObject".equals(json.type)) {
                var to = new com.core.entity.TextObject(json.name);
                to.x = json.x;
                to.y = json.y;
                to.width = json.width;
                to.height = json.height;
                to.vx = json.vx;
                to.vy = json.vy;
                to.mass = json.mass;
                to.rotation = json.rotation;
                to.angularVelocity = json.angularVelocity;
                to.renderPriority = json.renderPriority;
                to.physicsType = com.core.physics.PhysicsType.valueOf(json.physicsType);
                to.active = json.active;
                to.text = json.text != null ? json.text : "Text";
                to.fontSize = json.fontSize > 0 ? json.fontSize : 12f;
                to.hud = json.hud;
                
                entity = to;
            } else {
                // Generic Entity
                var e = new com.core.entity.Entity<>(json.name);
                e.x = json.x;
                e.y = json.y;
                e.width = json.width;
                e.height = json.height;
                e.vx = json.vx;
                e.vy = json.vy;
                e.mass = json.mass;
                e.rotation = json.rotation;
                e.angularVelocity = json.angularVelocity;
                e.renderPriority = json.renderPriority;
                e.physicsType = com.core.physics.PhysicsType.valueOf(json.physicsType);
                e.active = json.active;
                
                if (json.color != null) {
                    e.color = json.color.toColor();
                }
                if (json.fillColor != null) {
                    e.fillColor = json.fillColor.toColor();
                }
                
                entity = e;
            }
            
            // Set material
            if (json.material != null) {
                entity.material = json.material.toMaterial();
            }
            
            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Clone a JsonEntity (deep copy).
     */
    private JsonEntity cloneJsonEntity(JsonEntity json) {
        String jsonStr = gson.toJson(json);
        return gson.fromJson(jsonStr, JsonEntity.class);
    }

    /**
     * Update internal set of world entity names for uniqueness checking.
     */
    private void updateWorldEntityNames(com.core.entity.World world) {
        worldEntityNames.clear();
        collectEntityNames(world);
    }

    /**
     * Recursively collect entity names from world and all children.
     */
    private void collectEntityNames(Entity<?> entity) {
        worldEntityNames.add(entity.name);
        for (var child : entity.children) {
            collectEntityNames(child);
        }
    }

    /**
     * Generate unique name by appending counter if necessary.
     */
    private String generateUniqueName(String baseName) {
        if (!worldEntityNames.contains(baseName)) {
            return baseName;
        }
        
        int counter = 1;
        while (worldEntityNames.contains(baseName + "_" + counter)) {
            counter++;
        }
        return baseName + "_" + counter;
    }

    /**
     * Recursively remove an entity from the children tree.
     */
    private boolean removeEntityFromChildren(Entity<?> parent, Entity<?> target) {
        if (parent.children.remove(target)) {
            return true;
        }
        for (var child : parent.children) {
            if (removeEntityFromChildren(child, target)) {
                return true;
            }
        }
        return false;
    }
}
