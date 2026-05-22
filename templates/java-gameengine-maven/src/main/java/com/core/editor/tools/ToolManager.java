package com.core.editor.tools;

/**
 * Manages the active editor tool and dispatches input events.
 *
 * <p>Allows switching between different tools (Select, Drag, Place) and ensures
 * that only the active tool receives input events.</p>
 */
public class ToolManager {
    private EditorTool activeTool;

    public ToolManager(EditorTool initialTool) {
        setActiveTool(initialTool);
    }

    /**
     * Sets the active tool, deactivating the previous one.
     */
    public void setActiveTool(EditorTool tool) {
        if (activeTool != null) {
            activeTool.onDeactivate();
        }
        activeTool = tool;
        if (activeTool != null) {
            activeTool.onActivate();
        }
    }

    /**
     * Returns the currently active tool.
     */
    public EditorTool getActiveTool() {
        return activeTool;
    }

    /**
     * Dispatches a mouse press event to the active tool.
     */
    public void onMousePressed(float x, float y) {
        if (activeTool != null) {
            activeTool.onMousePressed(x, y);
        }
    }

    /**
     * Dispatches a mouse release event to the active tool.
     */
    public void onMouseReleased(float x, float y) {
        if (activeTool != null) {
            activeTool.onMouseReleased(x, y);
        }
    }

    /**
     * Dispatches a mouse drag event to the active tool.
     */
    public void onMouseDragged(float x, float y) {
        if (activeTool != null) {
            activeTool.onMouseDragged(x, y);
        }
    }

    /**
     * Dispatches a mouse move event to the active tool.
     */
    public void onMouseMoved(float x, float y) {
        if (activeTool != null) {
            activeTool.onMouseMoved(x, y);
        }
    }
}
