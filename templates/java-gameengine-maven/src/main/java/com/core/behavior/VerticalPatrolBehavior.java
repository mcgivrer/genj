package com.core.behavior;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import com.core.entity.Entity;

/**
 * Makes an entity oscillate vertically between two Y bounds.
 * Intended for STATIC platforms: the behavior directly writes the entity's
 * position (via velocity) and reverses direction at each bound.
 */
public class VerticalPatrolBehavior implements Behavior {

    private final float y0;
    private final float y1;
    private final float speed;

    /**
     * @param y0    top bound (world pixels)
     * @param y1    bottom bound (world pixels)
     * @param speed travel speed in pixels/second (positive)
     */
    public VerticalPatrolBehavior(float y0, float y1, float speed) {
        this.y0 = y0;
        this.y1 = y1;
        this.speed = Math.abs(speed);
    }

    @Override
    public void update(Entity<?> entity, long elapsed) {
        // Ensure an initial direction is set
        if (entity.vy == 0f) {
            entity.vy = speed;
        }

        // Reverse at bounds
        if (entity.y <= y0) {
            entity.y = y0;
            entity.vy = speed;
        } else if (entity.y + entity.height >= y1) {
            entity.y = y1 - entity.height;
            entity.vy = -speed;
        }
    }

    /** Draws the vertical patrol path and end-stops in yellow. */
    @Override
    public void drawDebug(Graphics2D g, Entity<?> entity) {
        int cx = (int) (entity.x + entity.width * 0.5f);
        int stopW = entity.width + 8;
        int stopHalf = stopW / 2;

        Stroke saved = g.getStroke();
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{4f, 4f}, 0f));
        g.setColor(new Color(255, 220, 0, 160));

        // Trajectory line
        g.drawLine(cx, (int) y0, cx, (int) y1);

        // Top stop
        g.setStroke(new BasicStroke(2f));
        g.drawLine(cx - stopHalf, (int) y0, cx + stopHalf, (int) y0);
        // Bottom stop
        g.drawLine(cx - stopHalf, (int) y1, cx + stopHalf, (int) y1);

        g.setStroke(saved);
    }
}
