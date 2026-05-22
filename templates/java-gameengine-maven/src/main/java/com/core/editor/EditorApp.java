package com.core.editor;

import com.core.App;
import com.core.editor.tools.DragTool;
import com.core.editor.tools.GridTool;
import com.core.editor.tools.PlaceTool;
import com.core.editor.tools.SelectTool;
import com.core.editor.tools.ToolManager;
import com.core.editor.ui.BehaviorsPanel;
import com.core.editor.ui.EditorMenuBar;
import com.core.editor.ui.PropertiesPanel;
import com.core.editor.ui.SceneTreePanel;
import com.core.editor.ui.StatusBar;
import com.core.editor.ui.ToolBar;
import com.core.editor.viewport.EditorViewport;
import com.core.entity.Entity;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;

/**
 * Main window for the Level Editor.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Creates and manages a {@link com.core.App} instance (the game engine)</li>
 *   <li>Provides Swing UI: menu bar, status bar, dockable panels</li>
 *   <li>Manages editor mode toggle (Ctrl+E to activate, Esc to deactivate)</li>
 *   <li>Coordinates between Swing EDT and game loop thread</li>
 * </ul>
 *
 * <h3>Threading model</h3>
 * <p>The game loop runs in a dedicated thread (managed by {@code App}). Swing events
 * (key presses, mouse clicks) are captured on the EDT and dispatched to the game loop
 * via thread-safe queues or {@code SwingUtilities.invokeLater()}.
 * </p>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * EditorApp editor = new EditorApp();
 * editor.setVisible(true);
 * }</pre>
 */
public class EditorApp extends JFrame implements EditorMenuBar.EditorMenuBarListener {

    /** The embedded game engine instance. */
    private final App app;

    /** The editor scene (empty, waiting for population). */
    private final EditorScene editorScene;

    /** Whether editor mode is currently active. */
    private boolean editorModeActive = false;

    /** UI components. */
    private final StatusBar statusBar;
    private final EditorMenuBar menuBar;
    private final EditorInputHandler inputHandler;
    private final EditorViewport viewport;
    private final ToolBar toolBar;
    private final ToolManager toolManager;
    private final GridTool gridTool;
    private final PropertiesPanel propertiesPanel;
    private final BehaviorsPanel behaviorsPanel;
    private final SceneTreePanel sceneTreePanel;
    private final EditorClipboard clipboard;

    public EditorApp() {
        super("GameEngineDemo — Level Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        // Initialize UI components
        statusBar = new StatusBar();
        menuBar = new EditorMenuBar(this);
        inputHandler = new EditorInputHandler(this);
        toolBar = new ToolBar();
        propertiesPanel = new PropertiesPanel();
        behaviorsPanel = new BehaviorsPanel();
        sceneTreePanel = new SceneTreePanel();
        clipboard = new EditorClipboard();

        // Create the game app and editor scene
        app = new App();
        editorScene = new EditorScene(app, this);

        // Create editor viewport (will be populated with scene content)
        viewport = new EditorViewport(editorScene);

        // Create grid tool
        gridTool = new GridTool();

        // Initialize the scene tree with the World entity
        // (World will be populated later, but we set up the tree structure now)
        if (editorScene.getWorld() != null) {
            sceneTreePanel.setRootEntity(editorScene.getWorld());
        }

        // Wire grid tool to viewport
        viewport.setGridTool(gridTool);

        // Wire viewport selection changes to properties panel, behaviors panel, and scene tree
        viewport.setOnSelectionChangedListener(entity -> {
            setSelectedEntity(entity);
            behaviorsPanel.setSelectedEntity(entity);
            sceneTreePanel.setSelectedEntity(entity);
        });

        // Wire scene tree selection changes to viewport
        sceneTreePanel.setOnSelectionChangedListener(entity -> {
            viewport.setSelectedEntity(entity);
            setSelectedEntity(entity);
        });

        // Create tools and tool manager
        SelectTool selectTool = new SelectTool(viewport);
        DragTool dragTool = new DragTool(viewport, gridTool);
        PlaceTool placeTool = new PlaceTool(viewport, gridTool);
        toolManager = new ToolManager(selectTool);

        // Add tool buttons to toolbar
        toolBar.addToolButton(selectTool, e -> toolManager.setActiveTool(selectTool));
        toolBar.addToolButton(dragTool, e -> toolManager.setActiveTool(dragTool));
        toolBar.addToolButton(placeTool, e -> toolManager.setActiveTool(placeTool));

        // Setup layout
        setupLayout();

        // Wire properties panel and behaviors panel changes to viewport refresh
        propertiesPanel.setOnApplyChangesListener(entity -> viewport.repaint());
        behaviorsPanel.setOnApplyChangesListener(entity -> viewport.repaint());

        // Wire input handler to viewport, tool manager, and grid tool
        inputHandler.setViewport(viewport);
        inputHandler.setToolManager(toolManager);
        inputHandler.setGridTool(gridTool);
        inputHandler.setClipboard(clipboard);

        // Register input handlers
        addKeyListener(inputHandler);
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);
        addMouseWheelListener(inputHandler);
        setFocusable(true);
    }

    /**
     * Configures the window layout.
     *
     * <pre>
     * ┌────────────────────────────────────────────────────────────┐
     * │       EditorMenuBar                                        │
     * ├────────────────────────────────────────────────────────────┤
     * │           ToolBar                                          │ North
     * ├──────────┬──────────────────────┬─────────────────────────┤
     * │          │                      │ PropertiesPanel         │
     * │  Scene   │  EditorViewport      │ (Properties) (Ctrl+1)   │
     * │  Tree    │   (with overlay)     ├─────────────────────────┤
     * │  (15%)   │     (70%)            │ BehaviorsPanel          │
     * │          │                      │ (Behaviors) (Ctrl+2)    │
     * │          │                      │      (15%)              │
     * ├──────────┴──────────────────────┴─────────────────────────┤
     * │           StatusBar                                        │ South
     * └────────────────────────────────────────────────────────────┘
     * </pre>
     *
     * v1.0: Simple horizontal split (viewport left, properties right).
     * v1.1: Added Scene Tree on left (tree 15%, viewport+properties 85%).
     * v1.2: Added BehaviorsPanel below PropertiesPanel (vertical split on right).
     * v2.0+: Multi-panel docking via DockManager.
     */
    private void setupLayout() {
        setJMenuBar(menuBar);

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());

        // North: toolbar for tool selection
        contentPane.add(toolBar, BorderLayout.NORTH);

        // Center: 
        // - Right vertical split: properties (top) + behaviors (bottom)
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setTopComponent(propertiesPanel);
        rightSplit.setBottomComponent(behaviorsPanel);
        rightSplit.setDividerLocation(0.5);  // Properties 50%, Behaviors 50%
        rightSplit.setOneTouchExpandable(true);
        rightSplit.setBackground(new java.awt.Color(20, 20, 30));
        rightSplit.setBorder(null);

        // - Inner split: viewport (left) + right panels (right)
        JSplitPane innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        innerSplit.setLeftComponent(viewport);
        innerSplit.setRightComponent(rightSplit);
        innerSplit.setDividerLocation(0.82);  // Viewport 82%, Right panels 18%
        innerSplit.setOneTouchExpandable(true);
        innerSplit.setBackground(new java.awt.Color(20, 20, 30));
        innerSplit.setBorder(null);

        // - Outer split: scene tree (left) + inner split (right)
        JSplitPane outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        outerSplit.setLeftComponent(sceneTreePanel);
        outerSplit.setRightComponent(innerSplit);
        outerSplit.setDividerLocation(0.15);  // Tree 15%, viewport+right panels 85%
        outerSplit.setOneTouchExpandable(true);
        outerSplit.setBackground(new java.awt.Color(20, 20, 30));
        outerSplit.setBorder(null);
        
        contentPane.add(outerSplit, BorderLayout.CENTER);

        // Bottom: status bar
        contentPane.add(statusBar, BorderLayout.SOUTH);

        updateStatusBar();
    }

    /**
     * Toggles the editor mode ON (pauses simulation).
     * 
     * <p>When editor mode is active:</p>
     * <ul>
     *   <li>Simulation is paused ({@code app.pause = true})</li>
     *   <li>Physics and behavior updates are frozen</li>
     *   <li>Editor overlay is rendered (grid, selection handles, etc.)</li>
     *   <li>Input is dispatched to editor tools — P4</li>
     * </ul>
     */
    public void toggleEditorMode() {
        editorModeActive = !editorModeActive;
        App.pause = editorModeActive;

        if (editorModeActive) {
            statusBar.setMode("✏ MODE ÉDITEUR");
        } else {
            statusBar.setMode("▶ RUN");
        }

        updateStatusBar();
    }

    /**
     * Exits the editor mode (resumes simulation).
     */
    public void exitEditorMode() {
        if (editorModeActive) {
            editorModeActive = false;
            App.pause = false;
            statusBar.setMode("▶ RUN");
            updateStatusBar();
        }
    }

    /**
     * Returns whether editor mode is currently active.
     */
    public boolean isEditorModeActive() {
        return editorModeActive;
    }

    /**
     * Returns the embedded game engine instance.
     */
    public App getApp() {
        return app;
    }

    /**
     * Returns the editor scene.
     */
    public EditorScene getEditorScene() {
        return editorScene;
    }

    /**
     * Returns the editor viewport.
     */
    public EditorViewport getViewport() {
        return viewport;
    }

    public ToolManager getToolManager() {
        return toolManager;
    }

    /**
     * Returns the grid tool.
     */
    public GridTool getGridTool() {
        return gridTool;
    }

    /**
     * Returns the status bar.
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * Returns the properties panel.
     */
    public PropertiesPanel getPropertiesPanel() {
        return propertiesPanel;
    }

    /**
     * Updates the properties panel with the selected entity.
     * Called from EditorViewport or EditorOverlay when selection changes.
     */
    public void setSelectedEntity(Entity<?> entity) {
        propertiesPanel.setSelectedEntity(entity);
    }

    /**
     * Updates the status bar display.
     */
    private void updateStatusBar() {
        statusBar.setMode(editorModeActive ? "✏ MODE ÉDITEUR" : "▶ RUN");
        statusBar.setScene("(unsaved)"); // TODO: actual scene name
        statusBar.setCoordinates(0, 0);  // TODO: world coords under cursor
        statusBar.setSelection("— aucune —"); // TODO: selected entity name
        statusBar.setGrid("— désactivée —"); // TODO: grid status
    }

    /**
     * Starts the editor window and the game loop.
     *
     * <p>This should be called from a static main() method.</p>
     */
    public void start() {
        setVisible(true);
        // The app's game loop is managed by App itself
        // (it typically runs in a dedicated thread)
    }

    // ─── Menu action callbacks ────────────────────────────────────────────────

    @Override
    public void onNewScene() {
        // P2: I/O scene
    }

    @Override
    public void onOpenScene() {
        // P2: I/O scene
    }

    @Override
    public void onSaveScene() {
        // P2: I/O scene
    }

    @Override
    public void onSaveSceneAs() {
        // P2: I/O scene
    }

    @Override
    public void onQuitEditor() {
        System.exit(0);
    }

    @Override
    public void onCopy() {
        // P7: Edit actions
    }

    @Override
    public void onCut() {
        // P7: Edit actions
    }

    @Override
    public void onPaste() {
        // P7: Edit actions
    }

    @Override
    public void onClone() {
        // P7: Edit actions
    }

    @Override
    public void onDelete() {
        // P7: Edit actions
    }

    @Override
    public void onAddGameObject() {
        // P4: Tools (PlaceTool)
    }

    @Override
    public void onAddTextObject() {
        // P4: Tools (PlaceTool)
    }

    @Override
    public void onToggleGrid() {
        // P5: Grid & snap
    }

    @Override
    public void onCycleGridSize() {
        // P5: Grid & snap
    }

    @Override
    public void onTogglePropertiesPanel() {
        // P6: Panels UI
    }

    @Override
    public void onToggleBehaviorsPanel() {
        // P6: Panels UI
    }

    @Override
    public void onResetLayout() {
        // P6: Docking
    }

    @Override
    public void onSceneProperties() {
        // P3: Scene configuration
    }

    @Override
    public void onWorldProperties() {
        // P3: World configuration
    }

    @Override
    public void onRunSimulation() {
        exitEditorMode();
    }

    @Override
    public void onShowShortcuts() {
        // Help dialog
    }

    @Override
    public void onAbout() {
        // About dialog
    }

    /**
     * Main entry point for the Level Editor.
     *
     * <pre>
     * java -cp target/classes com.core.editor.EditorApp
     * </pre>
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EditorApp editor = new EditorApp();
            editor.start();
        });
    }
}
