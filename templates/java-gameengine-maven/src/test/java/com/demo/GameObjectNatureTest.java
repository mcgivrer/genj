package com.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.core.entity.GameObject;
import com.core.entity.Nature;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameObjectNature")
class GameObjectNatureTest {

    @Nested
    @DisplayName("Default values")
    class Defaults {

        @Test
        @DisplayName("default nature is RECTANGLE")
        void defaultNatureIsRectangle() {
            GameObject go = new GameObject("obj");
            assertEquals(Nature.RECTANGLE, go.nature);
        }

        @Test
        @DisplayName("image attribute is null by default")
        void imageAttributeNullByDefault() {
            GameObject go = new GameObject("obj");
            assertNull(go.image);
        }

        @Test
        @DisplayName("line end-point defaults to origin (0, 0)")
        void lineEndPointDefaultsToOrigin() {
            GameObject go = new GameObject("obj");
            assertEquals(0f, go.x2, 0.001f);
            assertEquals(0f, go.y2, 0.001f);
        }
    }

    @Nested
    @DisplayName("Fluent setters")
    class FluentSetters {

        @Test
        @DisplayName("setNature returns the same GameObject")
        void setNatureReturnsGameObject() {
            GameObject go = new GameObject("obj");
            assertSame(go, go.setNature(Nature.ELLIPSE));
            assertEquals(Nature.ELLIPSE, go.nature);
        }

        @Test
        @DisplayName("setImage sets nature to IMAGE and stores the image")
        void setImageSetsNatureToImage() {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
            GameObject go = new GameObject("obj");
            go.setImage(img);
            assertEquals(Nature.IMAGE, go.nature);
            assertSame(img, go.image);
        }

        @Test
        @DisplayName("setEndPoint stores end-point coordinates")
        void setEndPointStoresCoordinates() {
            GameObject go = new GameObject("obj");
            go.setNature(Nature.LINE).setEndPoint(200f, 350f);
            assertEquals(200f, go.x2, 0.001f);
            assertEquals(350f, go.y2, 0.001f);
        }

        @Test
        @DisplayName("setEndPoint returns the same GameObject")
        void setEndPointReturnsSelf() {
            GameObject go = new GameObject("obj");
            assertSame(go, go.setEndPoint(10f, 20f));
        }
    }
}
