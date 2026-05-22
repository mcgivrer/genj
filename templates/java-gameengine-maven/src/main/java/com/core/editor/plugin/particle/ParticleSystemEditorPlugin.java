package com.core.editor.plugin.particle;

import java.util.List;

import com.core.behavior.Behavior;
import com.core.behavior.particle.FountainBehavior;
import com.core.behavior.particle.RainBehavior;
import com.core.behavior.particle.SnowBehavior;
import com.core.behavior.particle.TorchBehavior;
import com.core.editor.plugin.EditorPlugin;
import com.core.entity.Entity;
import com.core.entity.ParticleSystem;
import com.core.gfx.plugin.EntityRenderer;
import com.core.gfx.plugin.ParticleSystemRenderer;

/**
 * Level Editor plugin that registers all particle-system–related contributions.
 *
 * <h3>ServiceLoader declaration</h3>
 * <pre>
 * # META-INF/services/com.core.editor.plugin.EditorPlugin
 * com.core.editor.plugin.particle.ParticleSystemEditorPlugin
 * </pre>
 *
 * <h3>What this plugin contributes</h3>
 * <ul>
 *   <li><b>Entity type</b>: {@link ParticleSystem} — appears in the
 *       "Add entity" palette of the Level Editor.</li>
 *   <li><b>Behaviors</b>: {@link RainBehavior}, {@link SnowBehavior},
 *       {@link FountainBehavior}, {@link TorchBehavior} — appear in the
 *       Behaviors catalog; the editor reads
 *       {@link com.core.editor.plugin.BehaviorParam @BehaviorParam} annotations
 *       to build the configuration form automatically.</li>
 *   <li><b>Renderer</b>: {@link ParticleSystemRenderer} — registered at the
 *       <em>head</em> of the engine plugin list so it takes priority over the
 *       default {@code GameObject} renderer.</li>
 *   <li><b>Properties panel</b>: {@link ParticleSystemConfigPanel} — replaces
 *       the default entity Properties panel when a {@link ParticleSystem} is
 *       selected.</li>
 * </ul>
 */
public class ParticleSystemEditorPlugin implements EditorPlugin {

    @Override
    public String getName() {
        return "Particle System";
    }

    @Override
    public List<Class<? extends Behavior>> getBehaviors() {
        return List.of(
                RainBehavior.class,
                SnowBehavior.class,
                FountainBehavior.class,
                TorchBehavior.class);
    }

    @Override
    public List<Class<? extends Entity<?>>> getEntityTypes() {
        return List.of(ParticleSystem.class);
    }

    @Override
    public List<EntityRenderer<?>> getRenderers() {
        return List.of(new ParticleSystemRenderer());
    }

    @Override
    public Class<?> getPropertiesPanelClass(Class<?> entityClass) {
        if (ParticleSystem.class.isAssignableFrom(entityClass)) {
            return ParticleSystemConfigPanel.class;
        }
        return null;
    }

    /**
     * Called by the editor after startup.  Registers the
     * {@link ParticleSystemRenderer} with the engine's renderer so that
     * particle systems are drawn even outside the editor.
     *
     * @param editorApp the {@code EditorApp} instance — unused here because
     *                  renderer registration is handled by {@code PluginRegistry}
     *                  when it processes {@link #getRenderers()}.
     */
    @Override
    public void onEditorInit(Object editorApp) {
        // Renderer registration is performed by PluginRegistry.
        // Override here if additional initialisation is required.
    }
}
