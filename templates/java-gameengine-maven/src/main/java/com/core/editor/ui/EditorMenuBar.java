package com.core.editor.ui;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Menu bar for the Level Editor.
 * Organizes editor actions into structured menus: File, Edit, View, Scene, Help.
 */
public class EditorMenuBar extends JMenuBar {

    private final EditorMenuBarListener listener;

    public EditorMenuBar(EditorMenuBarListener listener) {
        this.listener = listener;
        setupMenus();
    }

    private void setupMenus() {
        add(createFileMenu());
        add(createEditMenu());
        add(createViewMenu());
        add(createSceneMenu());
        add(createHelpMenu());
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu("Fichier");

        JMenuItem newScene = new JMenuItem("Nouvelle scène", KeyEvent.VK_N);
        newScene.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        newScene.addActionListener(e -> listener.onNewScene());
        menu.add(newScene);

        JMenuItem open = new JMenuItem("Ouvrir…", KeyEvent.VK_O);
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        open.addActionListener(e -> listener.onOpenScene());
        menu.add(open);

        JMenuItem save = new JMenuItem("Enregistrer", KeyEvent.VK_S);
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        save.addActionListener(e -> listener.onSaveScene());
        menu.add(save);

        JMenuItem saveAs = new JMenuItem("Enregistrer sous…");
        saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        saveAs.addActionListener(e -> listener.onSaveSceneAs());
        menu.add(saveAs);

        menu.addSeparator();

        JMenuItem quit = new JMenuItem("Quitter l'éditeur");
        quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        quit.addActionListener(e -> listener.onQuitEditor());
        menu.add(quit);

        return menu;
    }

    private JMenu createEditMenu() {
        JMenu menu = new JMenu("Édition");

        JMenuItem copy = new JMenuItem("Copier", KeyEvent.VK_C);
        copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        copy.addActionListener(e -> listener.onCopy());
        menu.add(copy);

        JMenuItem cut = new JMenuItem("Couper", KeyEvent.VK_X);
        cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        cut.addActionListener(e -> listener.onCut());
        menu.add(cut);

        JMenuItem paste = new JMenuItem("Coller", KeyEvent.VK_V);
        paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        paste.addActionListener(e -> listener.onPaste());
        menu.add(paste);

        JMenuItem clone = new JMenuItem("Cloner", KeyEvent.VK_D);
        clone.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
        clone.addActionListener(e -> listener.onClone());
        menu.add(clone);

        JMenuItem delete = new JMenuItem("Supprimer", KeyEvent.VK_DELETE);
        delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        delete.addActionListener(e -> listener.onDelete());
        menu.add(delete);

        menu.addSeparator();

        JMenuItem addGameObject = new JMenuItem("Ajouter GameObject", KeyEvent.VK_L);
        addGameObject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
        addGameObject.addActionListener(e -> listener.onAddGameObject());
        menu.add(addGameObject);

        JMenuItem addTextObject = new JMenuItem("Ajouter TextObject", KeyEvent.VK_T);
        addTextObject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));
        addTextObject.addActionListener(e -> listener.onAddTextObject());
        menu.add(addTextObject);

        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu("Affichage");

        JMenuItem gridToggle = new JMenuItem("Grille ON/OFF", KeyEvent.VK_G);
        gridToggle.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.CTRL_MASK));
        gridToggle.addActionListener(e -> listener.onToggleGrid());
        menu.add(gridToggle);

        JMenuItem gridCycle = new JMenuItem("Cycle taille grille");
        gridCycle.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        gridCycle.addActionListener(e -> listener.onCycleGridSize());
        menu.add(gridCycle);

        menu.addSeparator();

        JMenuItem propertiesPanel = new JMenuItem("Panel Propriétés", KeyEvent.VK_1);
        propertiesPanel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.CTRL_MASK));
        propertiesPanel.addActionListener(e -> listener.onTogglePropertiesPanel());
        menu.add(propertiesPanel);

        JMenuItem behaviorsPanel = new JMenuItem("Panel Behaviors", KeyEvent.VK_2);
        behaviorsPanel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.CTRL_MASK));
        behaviorsPanel.addActionListener(e -> listener.onToggleBehaviorsPanel());
        menu.add(behaviorsPanel);

        menu.addSeparator();

        JMenuItem resetLayout = new JMenuItem("Réinitialiser layout");
        resetLayout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        resetLayout.addActionListener(e -> listener.onResetLayout());
        menu.add(resetLayout);

        return menu;
    }

    private JMenu createSceneMenu() {
        JMenu menu = new JMenu("Scène");

        JMenuItem sceneProps = new JMenuItem("Propriétés de la scène…", KeyEvent.VK_F4);
        sceneProps.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
        sceneProps.addActionListener(e -> listener.onSceneProperties());
        menu.add(sceneProps);

        JMenuItem worldProps = new JMenuItem("Configurer le monde…", KeyEvent.VK_F5);
        worldProps.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        worldProps.addActionListener(e -> listener.onWorldProperties());
        menu.add(worldProps);

        menu.addSeparator();

        JMenuItem runSimulation = new JMenuItem("Lancer la simulation", KeyEvent.VK_F5);
        runSimulation.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        runSimulation.addActionListener(e -> listener.onRunSimulation());
        menu.add(runSimulation);

        return menu;
    }

    private JMenu createHelpMenu() {
        JMenu menu = new JMenu("Aide");

        JMenuItem shortcuts = new JMenuItem("Raccourcis clavier…", KeyEvent.VK_F1);
        shortcuts.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        shortcuts.addActionListener(e -> listener.onShowShortcuts());
        menu.add(shortcuts);

        JMenuItem about = new JMenuItem("À propos…");
        about.addActionListener(e -> listener.onAbout());
        menu.add(about);

        return menu;
    }

    /**
     * Interface for menu action callbacks.
     */
    public interface EditorMenuBarListener {
        void onNewScene();
        void onOpenScene();
        void onSaveScene();
        void onSaveSceneAs();
        void onQuitEditor();
        void onCopy();
        void onCut();
        void onPaste();
        void onClone();
        void onDelete();
        void onAddGameObject();
        void onAddTextObject();
        void onToggleGrid();
        void onCycleGridSize();
        void onTogglePropertiesPanel();
        void onToggleBehaviorsPanel();
        void onResetLayout();
        void onSceneProperties();
        void onWorldProperties();
        void onRunSimulation();
        void onShowShortcuts();
        void onAbout();
    }
}
