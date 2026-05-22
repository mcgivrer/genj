package com.core.gfx.plugin;

import java.awt.Graphics2D;

import com.core.entity.Entity;
import com.core.entity.GameObject;
import com.core.entity.Nature;

/**
 * Renders a {@link GameObject} with {@link Nature#IMAGE} by drawing its
 * {@link GameObject#image} scaled to the entity's bounding box.
 * If {@code image} is {@code null} this plugin will not claim the entity
 * (see {@link #supports(Entity)}) so the fallback renderer takes over.
 */
public class ImageRenderer implements EntityRenderer<GameObject> {

    @Override
    public boolean supports(Entity<?> entity) {
        return entity instanceof GameObject go
                && go.nature == Nature.IMAGE
                && go.image != null;
    }

    @Override
    public void render(Graphics2D g, GameObject e) {
        g.drawImage(e.image, (int) (e.x + 0.5f), (int) (e.y + 0.5f), e.width, e.height, null);
    }
}
