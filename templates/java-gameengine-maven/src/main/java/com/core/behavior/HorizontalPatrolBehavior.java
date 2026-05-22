package com.core.behavior;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import com.core.entity.Entity;

/**
 * Makes an entity oscillate horizontally between two X bounds.
 * Intended for STATIC platforms: the behavior directly writes the entity's
 * position (via velocity) and reverses direction at each bound.
 */
public class HorizontalPatrolBehavior implements Behavior {

    private final float x0;
    private final float x1;
    private final float speed;

    /**
     * @param x0    left bound (world pixels)
     * @param x1    right bound (world pixels)
     * @param speed travel speed in pixels/second (positive)
     */
    public HorizontalPatrolBehavior(float x0, float x1, float speed) {
        this.x0 = x0;
        this.x1 = x1;
        this.speed = Math.abs(speed);
    }

    @Override
    public void update(Entity<?> entity, long elapsed) {
        // Ensure an initial direction is set
        if (entity.vx == 0f) {
            entity.vx = speed;
        }

        // Reverse at bounds
        if (entity.x <= x0) {
            entity.x = x0;
            entity.vx = speed;
        } else if (entity.x + entity.width >= x1) {
            entity.x = x1 - entity.width;
            entity.vx = -speed;
        }
    }

    /** Draws the horizontal patrol path and end-stops in yellow. */
    @Override
    public void drawDebug(Graphics2D g, Entity<?> entity) {
        int cy = (int) (entity.y + entity.height * 0.5f);
        int stopH = entity.height + 8;
        int stopHalf = stopH / 2;

        Stroke saved = g.getStroke();
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{4f, 4f}, 0f));
        g.setColor(new Color(255, 220, 0, 160));

        // Trajectory line
        g.drawLine((int) x0, cy, (int) x1, cy);

        // Left stop
        g.setStroke(new BasicStroke(2f));
        g.drawLine((int) x0, cy - stopHalf, (int) x0, cy + stopHalf);
        // Right stop
        g.drawLine((int) x1, cy - stopHalf, (int) x1, cy + stopHalf);

        g.setStroke(saved);
    }
}
