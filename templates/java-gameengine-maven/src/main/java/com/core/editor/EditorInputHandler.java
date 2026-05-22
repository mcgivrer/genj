package com.core.editor;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JOptionPane;

import com.core.editor.tools.GridTool;
import com.core.editor.tools.ToolManager;
import com.core.editor.viewport.EditorViewport;

/**
 * Captures keyboard and mouse events in the editor.
 * 
 * Implements Swing event listeners (KeyListener, MouseListener, MouseMotionListener, MouseWheelListener)
 * and dispatches to the EditorApp for tool handling and mode management.
 * Supports camera controls: middle-mouse pan, scroll zoom, Home/F/A for framing.
 */
public class EditorInputHandler implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private final EditorApp editorApp;
    private EditorViewport viewport;
    private ToolManager toolManager;
    private GridTool gridTool;
    private EditorClipboard clipboard;

    // Camera pan tracking
    private int panStartX = 0;
    private int panStartY = 0;
    private boolean isPanning = false;

    public EditorInputHandler(EditorApp editorApp) {
        this.editorApp = editorApp;
    }

    public void setViewport(EditorViewport viewport) {
        this.viewport = viewport;
    }

    public void setToolManager(ToolManager toolManager) {
        this.toolManager = toolManager;
    }

    public void setGridTool(GridTool gridTool) {
        this.gridTool = gridTool;
    }

    public void setClipboard(EditorClipboard clipboard) {
        this.clipboard = clipboard;
    }

    // ─── KeyListener ─────────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        int modifiers = e.getModifiersEx();

        // Ctrl+E: Toggle editor mode
        if (keyCode == KeyEvent.VK_E && (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            editorApp.toggleEditorMode();
            e.consume();
            return;
        }

        // Escape: Exit editor mode
        if (keyCode == KeyEvent.VK_ESCAPE) {
            editorApp.exitEditorMode();
            e.consume();
            return;
        }

        if (!editorApp.isEditorModeActive() || viewport == null) return;

        // Ctrl+G: Toggle grid
        if (keyCode == KeyEvent.VK_G && (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0 && (modifiers & KeyEvent.SHIFT_DOWN_MASK) == 0) {
            if (gridTool != null) {
                gridTool.toggleGrid();
                viewport.repaint();
            }
            e.consume();
            return;
        }

        // Ctrl+Shift+G: Cycle grid size
        if (keyCode == KeyEvent.VK_G && (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0 && (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            if (gridTool != null) {
                gridTool.cycleGridSize();
                viewport.repaint();
            }
            e.consume();
            return;
        }

        // Home: Frame entire world
        if (keyCode == KeyEvent.VK_HOME) {
            frameWorld();
            viewport.repaint();
            e.consume();
            return;
        }

        // F: Frame selected entity
        if (keyCode == KeyEvent.VK_F) {
            frameSelected();
            viewport.repaint();
            e.consume();
            return;
        }

        // A: Frame all entities
        if (keyCode == KeyEvent.VK_A) {
            frameAll();
            viewport.repaint();
            e.consume();
            return;
        }

        // Ctrl+M: Edit selected entity's material
        if (keyCode == KeyEvent.VK_M && (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0 && (modifiers & KeyEvent.SHIFT_DOWN_MASK) == 0) {
            editMaterial();
            e.consume();
            return;
        }

        // Ctrl+Shift+M: Create new material
        if (keyCode == KeyEvent.VK_M && (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0 && (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            createMaterial();
            e.consume();
            return;
        }

        // Ctrl+C: Copy selected entity
        if (keyCode == KeyEvent.VK_C && (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0 && (modifiers & KeyEvent.SHIFT_DOWN_MASK) == 0) {
            copyEntity();
            e.consume();
            return;
        }

        // Ctrl+X: Cut selected entity
        if (keyCode == KeyEvent.VK_X && (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0 && (modifiers & KeyEvent.SHIFT_DOWN_MASK) == 0) {
            cutEntity();
            e.consume();
            return;
        }

        // Ctrl+V: Paste entity from clipboard
        if (keyCode == KeyEvent.VK_V && (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0 && (modifiers & KeyEvent.SHIFT_DOWN_MASK) == 0) {
            pasteEntity();
            e.consume();
            return;
        }

        // Ctrl+D: Clone selected entity
        if (keyCode == KeyEvent.VK_D && (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0 && (modifiers & KeyEvent.SHIFT_DOWN_MASK) == 0) {
            cloneEntity();
            e.consume();
            return;
        }

        // Delete: Delete selected entity
        if (keyCode == KeyEvent.VK_DELETE) {
            deleteEntity();
            e.consume();
            return;
        }

        // Other keys: dispatch to current tool (if in editor mode)
        // Placeholder: tool handling will be added in P4
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Placeholder for tool handling
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Typically unused
    }

    // ─── MouseListener ───────────────────────────────────────────────────────

    @Override
    public void mousePressed(MouseEvent e) {
        if (!editorApp.isEditorModeActive() || viewport == null) return;

        // Middle mouse: Start pan
        if (e.getButton() == MouseEvent.BUTTON2) {
            isPanning = true;
            panStartX = e.getX();
            panStartY = e.getY();
            e.consume();
            return;
        }

        // Left mouse: Dispatch to active tool
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (toolManager != null && viewport != null) {
                float worldX = viewport.screenToWorldX(e.getX());
                float worldY = viewport.screenToWorldY(e.getY());
                toolManager.onMousePressed(worldX, worldY);
                editorApp.getStatusBar().setSelection(viewport.getSelectedEntity() != null ? viewport.getSelectedEntity().name : "");
            }
            e.consume();
            return;
        }

        // Placeholder: other tools in P4
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!editorApp.isEditorModeActive() || viewport == null) return;

        if (e.getButton() == MouseEvent.BUTTON2) {
            isPanning = false;
            e.consume();
            return;
        }

        // Left mouse: Dispatch to active tool
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (toolManager != null && viewport != null) {
                float worldX = viewport.screenToWorldX(e.getX());
                float worldY = viewport.screenToWorldY(e.getY());
                toolManager.onMouseReleased(worldX, worldY);
            }
            e.consume();
            return;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Typically handled in mousePressed
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Typically unused
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Typically unused
    }

    // ─── MouseMotionListener ─────────────────────────────────────────────────

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!editorApp.isEditorModeActive() || viewport == null) return;

        // Pan while middle mouse is held
        if (isPanning) {
            int dx = e.getX() - panStartX;
            int dy = e.getY() - panStartY;
            viewport.getCamera().pan(-dx, -dy);
            panStartX = e.getX();
            panStartY = e.getY();
            viewport.repaint();
            e.consume();
            return;
        }

        // Left mouse drag: Dispatch to active tool
        if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            if (toolManager != null) {
                float worldX = viewport.getCamera().screenToWorldX(e.getX());
                float worldY = viewport.getCamera().screenToWorldY(e.getY());
                toolManager.onMouseDragged(worldX, worldY);
            }
            e.consume();
            return;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!editorApp.isEditorModeActive() || viewport == null) return;

        // Update status bar with world coordinates
        float worldX = viewport.screenToWorldX(e.getX());
        float worldY = viewport.screenToWorldY(e.getY());
        editorApp.getStatusBar().setCoordinates((int) worldX, (int) worldY);

        // Dispatch to active tool
        if (toolManager != null) {
            toolManager.onMouseMoved(worldX, worldY);
        }
    }

    // ─── MouseWheelListener ──────────────────────────────────────────────────

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (!editorApp.isEditorModeActive() || viewport == null) return;

        // Scroll up: Zoom in (factor 1.1)
        // Scroll down: Zoom out (factor 0.9)
        float factor = e.getWheelRotation() < 0 ? 1.1f : 0.9f;
        viewport.getCamera().zoomBy(factor);
        viewport.repaint();
        e.consume();
    }

    // ─── Camera Framing Helpers ──────────────────────────────────────────────

    private void frameWorld() {
        if (viewport == null || viewport.getScene() == null) return;
        var world = viewport.getScene().getWorld();
        if (world != null) {
            viewport.getCamera().frame(world.x, world.y, world.width, world.height);
        }
    }

    private void frameSelected() {
        if (viewport == null) return;
        var selected = viewport.getSelectedEntity();
        if (selected != null && selected.width > 0 && selected.height > 0) {
            viewport.getCamera().frame(selected.x, selected.y, selected.width, selected.height);
        }
    }

    private void frameAll() {
        if (viewport == null || viewport.getScene() == null) return;
        var entities = viewport.getScene().getEntities();
        if (entities.isEmpty()) return;

        // Calculate bounding box of all active entities
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (var entity : entities) {
            if (entity.active && entity.width > 0 && entity.height > 0) {
                minX = Math.min(minX, entity.x);
                minY = Math.min(minY, entity.y);
                maxX = Math.max(maxX, entity.x + entity.width);
                maxY = Math.max(maxY, entity.y + entity.height);
            }
        }

        if (minX < Float.MAX_VALUE) {
            float w = maxX - minX;
            float h = maxY - minY;
            viewport.getCamera().frame(minX, minY, w, h);
        }
    }

    // ─── Material Dialog Handlers ─────────────────────────────────────────────

    private void editMaterial() {
        if (viewport == null) return;
        var selected = viewport.getSelectedEntity();
        if (selected == null) {
            JOptionPane.showMessageDialog(null, "No entity selected.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        var dlg = new com.core.editor.ui.MaterialDialog(editorApp, selected.material);
        if (dlg.showDialog() == JOptionPane.OK_OPTION) {
            var material = dlg.getMaterial();
            if (material != null) {
                selected.material = material;
                viewport.repaint();
            }
        }
    }

    private void createMaterial() {
        var dlg = new com.core.editor.ui.MaterialDialog(editorApp, null);
        if (dlg.showDialog() == JOptionPane.OK_OPTION) {
            var material = dlg.getMaterial();
            if (material != null) {
                JOptionPane.showMessageDialog(editorApp,
                    "Material created: " + material.name + "\n\n" +
                    "You can now assign this material to entities by editing them.\n" +
                    "Note: Custom materials must be created programmatically for persistence.",
                    "Material Created", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    // ─── Clipboard Handlers (Copy/Paste/Clone/Delete) ───────────────────────

    private void copyEntity() {
        if (clipboard == null || viewport == null) return;
        var selected = viewport.getSelectedEntity();
        if (selected == null) {
            JOptionPane.showMessageDialog(null, "No entity selected.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        clipboard.copy(selected);
        editorApp.getStatusBar().setInfo("Copied: " + selected.name);
    }

    private void cutEntity() {
        if (clipboard == null || viewport == null) return;
        var selected = viewport.getSelectedEntity();
        if (selected == null) {
            JOptionPane.showMessageDialog(null, "No entity selected.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        var world = viewport.getScene().getWorld();
        if (world != null && clipboard.cut(selected, world)) {
            viewport.setSelectedEntity(null);
            viewport.repaint();
            editorApp.getStatusBar().setInfo("Cut: " + selected.name);
        }
    }

    private void pasteEntity() {
        if (clipboard == null || viewport == null || !clipboard.hasContent()) {
            JOptionPane.showMessageDialog(null, "Clipboard is empty.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        var world = viewport.getScene().getWorld();
        if (world == null) return;
        
        var pasted = clipboard.paste(world);
        if (pasted != null) {
            viewport.setSelectedEntity(pasted);
            viewport.repaint();
            editorApp.getStatusBar().setInfo("Pasted: " + pasted.name);
        }
    }

    private void cloneEntity() {
        if (clipboard == null || viewport == null) return;
        var selected = viewport.getSelectedEntity();
        if (selected == null) {
            JOptionPane.showMessageDialog(null, "No entity selected.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        var world = viewport.getScene().getWorld();
        if (world == null) return;
        
        var cloned = clipboard.clone(selected, world);
        if (cloned != null) {
            viewport.setSelectedEntity(cloned);
            viewport.repaint();
            editorApp.getStatusBar().setInfo("Cloned: " + cloned.name);
        }
    }

    private void deleteEntity() {
        if (clipboard == null || viewport == null) return;
        var selected = viewport.getSelectedEntity();
        if (selected == null) {
            JOptionPane.showMessageDialog(null, "No entity selected.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        var world = viewport.getScene().getWorld();
        if (world == null) return;
        
        String name = selected.name;
        if (clipboard.delete(selected, world)) {
            viewport.setSelectedEntity(null);
            viewport.repaint();
            editorApp.getStatusBar().setInfo("Deleted: " + name);
        }
    }
}


