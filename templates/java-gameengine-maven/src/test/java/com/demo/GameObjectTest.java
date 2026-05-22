package com.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.core.entity.Entity;
import com.core.entity.GameObject;
import com.core.physics.Material;
import com.core.physics.PhysicsType;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameObject")
class GameObjectTest {

    @Test
    @DisplayName("is an instance of Entity")
    void isEntity() {
        GameObject go = new GameObject("hero");
        assertInstanceOf(Entity.class, go);
    }

    @Test
    @DisplayName("name is set at construction")
    void nameSetAtConstruction() {
        GameObject go = new GameObject("player");
        assertEquals("player", go.name);
    }

    @Test
    @DisplayName("default physicsType is DYNAMIC")
    void defaultPhysicsTypeDynamic() {
        GameObject go = new GameObject("obj");
        assertEquals(PhysicsType.DYNAMIC, go.physicsType);
    }

    @Test
    @DisplayName("default material is Material.DEFAULT")
    void defaultMaterialIsDefault() {
        GameObject go = new GameObject("obj");
        assertEquals(Material.DEFAULT, go.material);
    }

    @Test
    @DisplayName("fluent setters return the same GameObject instance")
    void fluentSettersReturnSelf() {
        GameObject go = new GameObject("obj");
        assertSame(go, go.setPosition(1f, 2f));
        assertSame(go, go.setVelocity(3f, 4f));
        assertSame(go, go.setSize(10, 10));
        assertSame(go, go.setMass(2f));
        assertSame(go, go.setPhysicsType(PhysicsType.STATIC));
        assertSame(go, go.setMaterial(Material.WOOD));
        assertSame(go, go.setActive(false));
    }
}
