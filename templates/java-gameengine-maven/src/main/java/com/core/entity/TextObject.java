package com.core.entity;

import java.awt.Font;

import com.core.physics.PhysicsType;
import com.core.utils.TextAlign;

/**
 * An entity that displays a text string.
 *
 * <p>Two rendering modes are supported:
 * <ul>
 *   <li><b>World-space</b> ({@code hud = false}): the text is drawn inside the
 *       camera pass at the entity's {@code (x, y)} world coordinates.</li>
 *   <li><b>Screen-space / HUD</b> ({@code hud = true}): the text is drawn after
 *       the camera pass in pixel coordinates.  If {@code relX} / {@code relY} are
 *       set to a value in [0, 1] they are interpreted as fractions of the window
 *       width / height; otherwise the raw {@code x} / {@code y} values are used
 *       as pixel offsets.</li>
 * </ul>
 *
 * <p>Physics is disabled by default ({@link PhysicsType#NONE}).
 */
public class TextObject extends Entity<TextObject> {

    /** The string to display. */
    public String text = "";

    /** Font size in points. */
    public float fontSize = 14f;

    /** Font style — one of {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}. */
    public int fontStyle = Font.PLAIN;

    /** Horizontal alignment applied at the anchor point. */
    public TextAlign align = TextAlign.LEFT;

    /**
     * When {@code true} this entity is drawn in screen space (HUD pass) rather
     * than in world space.
     */
    public boolean hud = false;

    /**
     * Relative horizontal position [0..1] as a fraction of the window width.
     * Used only when {@code hud = true}.  A negative value means use {@code x}
     * directly as a pixel coordinate.
     */
    public float relX = -1f;

    /**
     * Relative vertical position [0..1] as a fraction of the window height.
     * Used only when {@code hud = true}.  A negative value means use {@code y}
     * directly as a pixel coordinate.
     */
    public float relY = -1f;

    public TextObject(String name) {
        super(name);
        this.physicsType = PhysicsType.NONE;
    }

    public TextObject setText(String text) {
        this.text = text;
        return this;
    }

    public TextObject setFontSize(float size) {
        this.fontSize = size;
        return this;
    }

    public TextObject setFontStyle(int style) {
        this.fontStyle = style;
        return this;
    }

    public TextObject setAlign(TextAlign align) {
        this.align = align;
        return this;
    }

    public TextObject setHud(boolean hud) {
        this.hud = hud;
        return this;
    }

    public TextObject setRelX(float relX) {
        this.relX = relX;
        return this;
    }

    public TextObject setRelY(float relY) {
        this.relY = relY;
        return this;
    }
}
