package com.core.entity;

/**
 * Visual nature of a {@link GameObject}: defines which shape (or image)
 * the renderer will draw for the object.
 *
 * <ul>
 *   <li>{@code POINT}     — a single dot, radius driven by {@code width/2} (min 2 px)</li>
 *   <li>{@code LINE}      — a line segment from {@code (x,y)} to {@code (x2,y2)}</li>
 *   <li>{@code RECTANGLE} — a filled/outlined rectangle (default, backward-compatible)</li>
 *   <li>{@code ELLIPSE}   — a filled/outlined ellipse inscribed in the bounding box</li>
 *   <li>{@code IMAGE}     — a {@link java.awt.image.BufferedImage} scaled to the bounding box</li>
 * </ul>
 */
public enum Nature {
    POINT,
    LINE,
    RECTANGLE,
    ELLIPSE,
    IMAGE
}
