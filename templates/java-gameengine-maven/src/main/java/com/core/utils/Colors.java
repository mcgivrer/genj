package com.core.utils;

import java.awt.Color;
import java.util.Random;

public class Colors {
    private static final Random RANDOM = new Random(67092);

    public static Color random() {
        return new Color(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
    }
}
