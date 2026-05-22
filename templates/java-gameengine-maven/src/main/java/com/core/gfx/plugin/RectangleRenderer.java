package com.core.gfx.plugin;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import com.core.entity.Entity;
import com.core.entity.GameObject;
import com.core.entity.Nature;

/**
 * Renders a {@link GameObject} with {@link Nature#RECTANGLE} as a filled rectangle
 * and/or an outlined rectangle.  This is the default visual for any {@link GameObject}.
 */
public class RectangleRenderer implements EntityRenderer<GameObject> {

    @Override
    public boolean supports(Entity<?> entity) {
        return entity instanceof GameObject go && go.nature == Nature.RECTANGLE;
    }

    @Override
    public void render(Graphics2D g, GameObject e) {
        int x = (int) (e.x + 0.5f);
        int y = (int) (e.y + 0.5f);

        AffineTransform saved = null;
        if (e.rotation != 0f) {
            saved = g.getTransform();
            float cx = e.x + e.width  * 0.5f;
            float cy = e.y + e.height * 0.5f;
            g.rotate(e.rotation, cx, cy);
        }

        if (e.fillColor != null) {
            g.setColor(e.fillColor);
            g.fillRect(x, y, e.width, e.height);
        }
        if (e.color != null) {
            g.setColor(e.color);
            g.drawRect(x, y, e.width, e.height);
        }

        if (saved != null) {
            g.setTransform(saved);
        }
    }
}
