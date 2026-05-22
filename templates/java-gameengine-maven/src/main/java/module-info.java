module com.demo {
    requires transitive java.desktop;
    requires java.logging;
    requires com.google.gson;

    exports com.core;
    exports com.core.behavior;
    exports com.core.entity;
    exports com.core.behavior.particle;
    exports com.core.gfx.plugin;
    exports com.core.physics;
    exports com.core.spatial;
    exports com.core.scene;
    exports com.core.editor;
    exports com.core.editor.ui;
    exports com.core.editor.tools;
    exports com.core.editor.io;
    exports com.core.editor.io.json;
    exports com.core.editor.viewport;
    exports com.core.editor.plugin;
    exports com.core.editor.plugin.particle;
    exports com.demo.scenes;
}
