package com.core.editor.plugin;

import com.core.behavior.Behavior;
import com.core.editor.EditorApp;
import com.core.entity.Entity;
import com.core.gfx.plugin.EntityRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Registry for discovering and managing editor plugins via {@link ServiceLoader}.
 *
 * <p>At {@link EditorApp} initialization, this registry loads all available plugins
 * and consolidates their behaviors, entity types, and renderers into searchable catalogs.</p>
 */
public class PluginRegistry {

    private final List<EditorPlugin> plugins = new ArrayList<>();
    private final List<Class<? extends Behavior>> behaviors = new ArrayList<>();
    private final List<Class<? extends Entity<?>>> entityTypes = new ArrayList<>();
    private final List<EntityRenderer<?>> renderers = new ArrayList<>();

    public PluginRegistry() {}

    /**
     * Discovers and loads all available plugins via ServiceLoader.
     * Called during EditorApp initialization.
     *
     * @param editor the editor app instance (passed to each plugin's onEditorInit)
     */
    public void loadPlugins(EditorApp editor) {
        ServiceLoader<EditorPlugin> loader = ServiceLoader.load(EditorPlugin.class);
        for (EditorPlugin plugin : loader) {
            plugins.add(plugin);
            behaviors.addAll(plugin.getBehaviors());
            entityTypes.addAll(plugin.getEntityTypes());
            renderers.addAll(plugin.getRenderers());
            plugin.onEditorInit(editor);
        }
    }

    /**
     * Returns the list of all registered behaviors.
     */
    public List<Class<? extends Behavior>> getBehaviors() {
        return behaviors;
    }

    /**
     * Returns the list of all registered entity types.
     */
    public List<Class<? extends Entity<?>>> getEntityTypes() {
        return entityTypes;
    }

    /**
     * Returns the list of all registered renderers.
     */
    public List<EntityRenderer<?>> getRenderers() {
        return renderers;
    }

    /**
     * Returns the list of loaded plugins.
     */
    public List<EditorPlugin> getPlugins() {
        return plugins;
    }
}
