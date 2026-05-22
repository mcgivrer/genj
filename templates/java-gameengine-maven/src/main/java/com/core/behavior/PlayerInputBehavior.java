package com.core.behavior;

import static com.core.io.InputHandler.isKeyPressed;

import java.awt.event.KeyEvent;

import com.core.entity.Entity;

/**
 * Reads keyboard input each frame and translates it into velocity changes
 * on the owning entity (typically the player).
 *
 * <ul>
 * <li>Left / Right arrows — lateral movement at {@code moveSpeed} px/s</li>
 * <li>Space or Up arrow — jump (applies {@code jumpVelocity} to vy) only
 * when the entity is on the ground ({@link Entity#onGround})</li>
 * </ul>
 */
public class PlayerInputBehavior implements Behavior {

    private final float moveSpeed;
    private final float jumpVelocity;

    /**
     * @param moveSpeed    horizontal speed in pixels/second (positive)
     * @param jumpVelocity initial vertical velocity for a jump (negative = upward)
     */
    public PlayerInputBehavior(float moveSpeed, float jumpVelocity) {
        this.moveSpeed = Math.abs(moveSpeed);
        this.jumpVelocity = jumpVelocity;
    }

    @Override
    public void update(Entity<?> entity, long elapsed) {
        // Horizontal movement
        boolean left = isKeyPressed(KeyEvent.VK_LEFT) || isKeyPressed(KeyEvent.VK_A);
        boolean right = isKeyPressed(KeyEvent.VK_RIGHT) || isKeyPressed(KeyEvent.VK_D);

        if (left && !right) {
            entity.vx = -moveSpeed;
        } else if (right && !left) {
            entity.vx = moveSpeed;
        }
        // No horizontal key → let physics damping handle deceleration

        // Jump
        boolean jump = isKeyPressed(KeyEvent.VK_SPACE) || isKeyPressed(KeyEvent.VK_UP);
        if (jump) {
            entity.vy = jumpVelocity;
            entity.onGround = false;
        }
    }
}
