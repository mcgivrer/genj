package com.core.gfx.plugin;

import java.awt.Graphics2D;

import com.core.entity.Entity;

/**
 * Fallback renderer: delegates to {@link Entity#draw(Graphics2D)}.
 * This preserves backward-compatible rendering for any entity subtype
 * that does not match a more specific plugin (e.g. {@link com.core.entity.World},
 * or a future custom subclass).
 *
 * <p>This plugin must be registered <em>last</em> (lowest priority) so it
 * only fires when no other plugin claims the entity.</p>
 */
public class DefaultEntityRenderer implements EntityRenderer<Entity<?>> {

    @Override
    public boolean supports(Entity<?> entity) {
        return true; // final catch-all
    }

    @Override
    public void render(Graphics2D g, Entity<?> entity) {
        entity.draw(g);
    }
}
