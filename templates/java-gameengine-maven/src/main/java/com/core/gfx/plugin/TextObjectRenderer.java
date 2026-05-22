package com.core.gfx.plugin;

import java.awt.Graphics2D;

import com.core.entity.Entity;
import com.core.entity.TextObject;
import com.core.gfx.Renderer;

/**
 * Renders a {@link TextObject} in world space.
 *
 * <p>When the entity has {@code hud = true} this plugin does nothing —
 * HUD text objects are rendered by {@link Renderer} in a dedicated
 * screen-space pass after all camera transforms have been restored.</p>
 */
public class TextObjectRenderer implements EntityRenderer<TextObject> {

    @Override
    public boolean supports(Entity<?> entity) {
        return entity instanceof TextObject;
    }

    @Override
    public void render(Graphics2D g, TextObject to) {
        if (to.hud) return; // deferred to the HUD screen-space pass in Renderer
        if (to.text == null || to.text.isEmpty()) return;
        Renderer.drawText(
                g,
                to.text,
                (int) (to.x + 0.5f),
                (int) (to.y + 0.5f),
                to.align,
                to.fontSize,
                to.color,
                to.fontStyle);
    }
}
