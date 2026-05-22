package com.core.behavior;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.List;

import com.core.entity.Entity;

/**
 * Moves an entity along an ordered list of waypoints at a constant speed.
 * Supports two looping modes:
 * <ul>
 * <li><b>loop</b> — jumps back to the first waypoint after the last one.</li>
 * <li><b>pingpong</b> — reverses direction at each end of the path.</li>
 * </ul>
 * If neither flag is set, the entity stops at the last waypoint.
 */
public class WaypointBehavior implements Behavior {

    /** Each waypoint is a {@code float[]{x, y}} in world pixels. */
    private final List<float[]> waypoints;
    private final float speed;
    private final boolean loop;
    private final boolean pingpong;

    /** Threshold distance (px) to consider a waypoint reached. */
    private static final float THRESHOLD = 2.0f;

    private int currentIndex = 0;
    private int direction = 1; // +1 forward, -1 backward (ping-pong)

    /**
     * @param waypoints ordered list of {@code [x, y]} world-pixel coordinates
     * @param speed     travel speed in pixels/second
     * @param loop      jump back to waypoint 0 after the last
     * @param pingpong  reverse direction at each end
     */
    public WaypointBehavior(List<float[]> waypoints, float speed, boolean loop, boolean pingpong) {
        if (waypoints == null || waypoints.isEmpty()) {
            throw new IllegalArgumentException("WaypointBehavior requires at least one waypoint");
        }
        this.waypoints = waypoints;
        this.speed = Math.abs(speed);
        this.loop = loop;
        this.pingpong = pingpong;
    }

    @Override
    public void update(Entity<?> entity, long elapsed) {
        if (currentIndex < 0 || currentIndex >= waypoints.size()) {
            entity.vx = 0;
            entity.vy = 0;
            return;
        }

        float[] target = waypoints.get(currentIndex);
        float tx = target[0];
        float ty = target[1];

        // Centre de l'entité vers le waypoint
        float cx = entity.x + entity.width * 0.5f;
        float cy = entity.y + entity.height * 0.5f;
        float dx = tx - cx;
        float dy = ty - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist <= THRESHOLD) {
            // Waypoint reached — advance
            entity.vx = 0;
            entity.vy = 0;
            advance();
        } else {
            float norm = dist;
            entity.vx = (dx / norm) * speed;
            entity.vy = (dy / norm) * speed;
        }
    }

    private void advance() {
        int next = currentIndex + direction;
        if (next >= 0 && next < waypoints.size()) {
            currentIndex = next;
        } else if (pingpong) {
            direction = -direction;
            currentIndex += direction;
        } else if (loop) {
            currentIndex = (direction > 0) ? 0 : waypoints.size() - 1;
        } else {
            // Stop at last waypoint
            currentIndex = Math.max(0, Math.min(currentIndex, waypoints.size() - 1));
        }
    }

    /**
     * Draws the waypoint path (segments + node circles) in yellow, current target
     * highlighted.
     */
    @Override
    public void drawDebug(Graphics2D g, Entity<?> entity) {
        if (waypoints.size() < 2)
            return;

        Stroke saved = g.getStroke();
        g.setColor(new Color(255, 220, 0, 160));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[] { 6f, 4f }, 0f));

        // Segments
        for (int i = 0; i < waypoints.size() - 1; i++) {
            float[] a = waypoints.get(i);
            float[] b = waypoints.get(i + 1);
            g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
        }
        if (loop && waypoints.size() > 2) {
            float[] first = waypoints.get(0);
            float[] last = waypoints.get(waypoints.size() - 1);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[] { 3f, 5f }, 0f));
            g.drawLine((int) last[0], (int) last[1], (int) first[0], (int) first[1]);
        }

        g.setStroke(new BasicStroke(1.5f));
        // Waypoint nodes
        for (int i = 0; i < waypoints.size(); i++) {
            float[] wp = waypoints.get(i);
            int r = (i == currentIndex) ? 5 : 3;
            g.setColor(i == currentIndex
                    ? new Color(255, 255, 80, 220)
                    : new Color(255, 220, 0, 140));
            g.drawOval((int) wp[0] - r, (int) wp[1] - r, r * 2, r * 2);
        }

        g.setStroke(saved);
    }
}
