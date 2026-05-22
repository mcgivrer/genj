package com.core.editor.io.json;

import java.awt.Color;

/**
 * DTO for JSON serialization of {@link java.awt.Color}.
 * Stores RGBA components as integers (0-255).
 */
public class JsonColor {
    public int r = 0;
    public int g = 0;
    public int b = 0;
    public int a = 255;

    public JsonColor() {
    }

    public JsonColor(Color c) {
        if (c != null) {
            this.r = c.getRed();
            this.g = c.getGreen();
            this.b = c.getBlue();
            this.a = c.getAlpha();
        }
    }

    public Color toColor() {
        return new Color(r, g, b, a);
    }
}
