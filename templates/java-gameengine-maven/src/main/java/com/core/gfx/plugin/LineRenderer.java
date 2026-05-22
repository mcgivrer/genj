package com.core.gfx.plugin;

import java.awt.Graphics2D;

import com.core.entity.Entity;
import com.core.entity.GameObject;
import com.core.entity.Nature;

/**
 * Renders a {@link GameObject} with {@link Nature#LINE} as a line segment
 * from {@code (x, y)} to {@code (x2, y2)}, drawn with {@code color}.
 */
public class LineRenderer implements EntityRenderer<GameObject> {

    @Override
    public boolean supports(Entity<?> entity) {
        return entity instanceof GameObject go && go.nature == Nature.LINE;
    }

    @Override
    public void render(Graphics2D g, GameObject e) {
        if (e.color != null) {
            g.setColor(e.color);
            g.drawLine(
                    (int) (e.x  + 0.5f), (int) (e.y  + 0.5f),
                    (int) (e.x2 + 0.5f), (int) (e.y2 + 0.5f));
        }
    }
}
