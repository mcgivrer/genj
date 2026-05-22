package com.core.entity;

import java.awt.image.BufferedImage;

public class GameObject extends Entity<GameObject> {

    /** Visual shape/type of this object. Default: {@link Nature#RECTANGLE}. */
    public Nature nature = Nature.RECTANGLE;

    /**
     * Bitmap image used when {@link #nature} is {@link Nature#IMAGE}.
     * {@code null} until set via {@link #setImage(BufferedImage)}.
     */
    public BufferedImage image = null;

    /**
     * X-coordinate of the line end-point when {@link #nature} is {@link Nature#LINE}.
     * Expressed in world space.
     */
    public float x2 = 0f;

    /**
     * Y-coordinate of the line end-point when {@link #nature} is {@link Nature#LINE}.
     * Expressed in world space.
     */
    public float y2 = 0f;

    public GameObject(String name) {
        super(name);
    }

    /**
     * Sets the visual nature of this object.
     *
     * @param n the desired {@link Nature}
     * @return {@code this} for fluent chaining
     */
    public GameObject setNature(Nature n) {
        this.nature = n;
        return this;
    }

    /**
     * Sets the bitmap image and automatically switches the nature to {@link Nature#IMAGE}.
     *
     * @param img the image to render (must not be {@code null})
     * @return {@code this} for fluent chaining
     */
    public GameObject setImage(BufferedImage img) {
        this.image = img;
        this.nature = Nature.IMAGE;
        return this;
    }

    /**
     * Sets the end-point of the line segment used when {@link #nature} is {@link Nature#LINE}.
     *
     * @param x2 world X of the end-point
     * @param y2 world Y of the end-point
     * @return {@code this} for fluent chaining
     */
    public GameObject setEndPoint(float x2, float y2) {
        this.x2 = x2;
        this.y2 = y2;
        return this;
    }
}

