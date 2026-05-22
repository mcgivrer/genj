package com.core.editor;

import com.core.App;
import com.core.scene.BaseScene;

/**
 * Scene dedicated to the Level Editor.
 * 
 * Extends {@link BaseScene} with editor-specific logic:
 * - Renders normally (all entities visible)
 * - Overlay with grid, selection handles, and entity names
 * - Physics paused when editor is active
 */
public class EditorScene extends BaseScene {

    /** Reference to the editor app for callbacks and state management. */
    private EditorApp editorApp;

    public EditorScene(App app, EditorApp editorApp) {
        super(app);
        this.editorApp = editorApp;
    }

    @Override
    public void create(App app) {
        // Initialize a basic empty scene for editing.
        // Subclasses or scene loaders will populate this.
        super.create(app);
    }

    @Override
    public void update(App app, java.util.Map<String, Object> stats, float deltaTime) {
        // In editor mode, simulation is paused (app.pause = true).
        // Update can still be called, but physics step is frozen.
        // For now, just call parent (which updates cameras and other non-physics logic).
        super.update(app, stats, deltaTime);
    }

    /**
     * Returns the editor app reference.
     */
    public EditorApp getEditorApp() {
        return editorApp;
    }
}
