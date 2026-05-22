package com.core.editor.plugin;

import java.util.List;

import com.core.behavior.Behavior;
import com.core.entity.Entity;
import com.core.gfx.plugin.EntityRenderer;

/**
 * Extension point for the Level Editor.
 *
 * <p>Implement this interface (and declare it in
 * {@code META-INF/services/com.core.editor.plugin.EditorPlugin}) to extend
 * the editor with:</p>
 * <ul>
 *   <li>New entity types ({@link #getEntityTypes()})</li>
 *   <li>New behaviors visible in the Behaviors catalog ({@link #getBehaviors()})</li>
 *   <li>Custom {@link EntityRenderer} plugins registered in the engine renderer
 *       ({@link #getRenderers()})</li>
 *   <li>Custom Swing panels that replace or extend the built-in Properties panel
 *       ({@link #getPropertiesPanelClass(Class)})</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>At {@code EditorApp} startup, {@code PluginRegistry} calls
 *       {@link java.util.ServiceLoader#load(Class)} to discover all implementations.</li>
 *   <li>{@link #onEditorInit(Object)} is called once (the parameter type is
 *       {@code Object} here to avoid a circular dependency on the editor module;
 *       cast to {@code EditorApp} in your implementation).</li>
 *   <li>Behaviors, entity types, and renderers are registered automatically by
 *       {@code PluginRegistry}.</li>
 * </ol>
 *
 * <h3>Example (particle system plugin)</h3>
 * <pre>{@code
 * // META-INF/services/com.core.editor.plugin.EditorPlugin:
 * //   com.core.editor.plugin.particle.ParticleSystemEditorPlugin
 *
 * public class ParticleSystemEditorPlugin implements EditorPlugin {
 *     \@Override public String getName() { return "Particle System"; }
 *     \@Override public List<Class<? extends Behavior>> getBehaviors() {
 *         return List.of(RainBehavior.class, SnowBehavior.class,
 *                        FountainBehavior.class, TorchBehavior.class);
 *     }
 *     \@Override public List<Class<? extends Entity<?>>> getEntityTypes() {
 *         return List.of(ParticleSystem.class);
 *     }
 *     \@Override public List<EntityRenderer<?>> getRenderers() {
 *         return List.of(new ParticleSystemRenderer());
 *     }
 * }
 * }</pre>
 */
public interface EditorPlugin {

    /**
     * Display name shown in the plugin list of the editor.
     */
    String getName();

    /**
     * Behavior classes to add to the Behaviors catalog panel.
     * The editor will introspect constructors (via {@link BehaviorParam}) to
     * build the configuration form.
     *
     * @return immutable list; may be empty
     */
    List<Class<? extends Behavior>> getBehaviors();

    /**
     * Entity sub-types to add to the "Add entity" menus and the entity type
     * selector in the Properties panel.
     *
     * @return immutable list; may be empty
     */
    List<Class<? extends Entity<?>>> getEntityTypes();

    /**
     * {@link EntityRenderer} instances to register with the engine's
     * {@link com.core.gfx.Renderer} (prepended to the plugin list, highest priority).
     *
     * @return immutable list; may be empty
     */
    List<EntityRenderer<?>> getRenderers();

    /**
     * Returns the custom Swing panel class to use in the Properties panel for
     * the given entity class, or {@code null} to fall back to the default panel.
     *
     * <p>The returned class must have a no-arg constructor and extend
     * {@code javax.swing.JPanel}.  The editor will call
     * {@code setEntity(Entity<?>)} by convention (reflection) after
     * instantiation.</p>
     *
     * @param entityClass the entity type currently selected in the editor
     * @return panel class or {@code null}
     */
    default Class<?> getPropertiesPanelClass(Class<?> entityClass) {
        return null;
    }

    /**
     * Called once after all plugins have been loaded and the editor window
     * has been created.
     *
     * @param editorApp the {@code EditorApp} instance (cast to it in your impl)
     */
    default void onEditorInit(Object editorApp) {}
}
