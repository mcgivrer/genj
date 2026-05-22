package com.core.gfx.plugin;

import java.awt.Graphics2D;

import com.core.entity.Entity;
import com.core.entity.GameObject;
import com.core.entity.Nature;

/**
 * Renders a {@link GameObject} with {@link Nature#POINT} as a filled circle.
 * The radius is {@code max(2, width / 2)}, so a GameObject with {@code width=0}
 * is still visible as a 2-pixel dot.
 */
public class PointRenderer implements EntityRenderer<GameObject> {

    @Override
    public boolean supports(Entity<?> entity) {
        return entity instanceof GameObject go && go.nature == Nature.POINT;
    }

    @Override
    public void render(Graphics2D g, GameObject e) {
        int r = Math.max(2, e.width / 2);
        int cx = (int) (e.x + 0.5f);
        int cy = (int) (e.y + 0.5f);
        if (e.fillColor != null) {
            g.setColor(e.fillColor);
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        if (e.color != null) {
            g.setColor(e.color);
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
        }
    }
}
