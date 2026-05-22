package com.core.editor.ui;

import com.core.behavior.Behavior;
import com.core.behavior.HorizontalPatrolBehavior;
import com.core.behavior.VerticalPatrolBehavior;
import com.core.behavior.PlayerInputBehavior;
import com.core.entity.Entity;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * BehaviorsPanel — Gestion des Behaviors d'une entité sélectionnée.
 *
 * Utilise un JList pour afficher les behaviors attachés et permet d'en ajouter/supprimer.
 *
 * Behaviors disponibles :
 * - HorizontalPatrolBehavior : oscillation X entre 2 bornes
 * - VerticalPatrolBehavior : oscillation Y entre 2 bornes
 * - CameraTrackingBehavior : suivi de caméra
 * - PlayerInputBehavior : contrôle au clavier
 * - WaypointBehavior : navigation par waypoints
 *
 * Synchronisation :
 * - PropertiesPanel/SceneTree → setSelectedEntity(entity)
 * - Add/Remove buttons → modified entity.behaviors list
 * - onApplyChangesListener → notify viewport to repaint
 */
public class BehaviorsPanel extends JPanel {
    private Entity<?> selectedEntity;
    private Consumer<Entity<?>> onApplyChanges;
    
    private DefaultListModel<String> behaviorListModel;
    private JList<String> behaviorList;
    private JButton addButton;
    private JButton removeButton;

    public BehaviorsPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 62));
        initUI();
    }

    private void initUI() {
        // ─── Header Panel ───
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(62, 62, 30));
        headerPanel.setBorder(BorderFactory.createLineBorder(new Color(170, 170, 85), 1));
        
        JLabel headerLabel = new JLabel("Behaviors");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        headerLabel.setForeground(new Color(245, 245, 160));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);

        // ─── Behavior List Panel ───
        behaviorListModel = new DefaultListModel<>();
        behaviorList = new JList<>(behaviorListModel);
        behaviorList.setBackground(new Color(40, 40, 70));
        behaviorList.setForeground(new Color(200, 200, 220));
        behaviorList.setSelectionBackground(new Color(74, 106, 154));
        behaviorList.setSelectionForeground(Color.WHITE);
        behaviorList.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        behaviorList.setFixedCellHeight(24);
        
        JScrollPane scrollPane = new JScrollPane(behaviorList);
        scrollPane.setBackground(new Color(30, 30, 62));
        scrollPane.getViewport().setBackground(new Color(30, 30, 62));
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);

        // ─── Buttons Panel ───
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonsPanel.setBackground(new Color(30, 30, 62));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 8));
        
        addButton = new JButton("Add");
        addButton.setBackground(new Color(74, 154, 74));
        addButton.setForeground(Color.WHITE);
        addButton.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        addButton.addActionListener(e -> addBehavior());
        
        removeButton = new JButton("Remove");
        removeButton.setBackground(new Color(154, 85, 85));
        removeButton.setForeground(Color.WHITE);
        removeButton.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        removeButton.addActionListener(e -> removeBehavior());
        
        buttonsPanel.add(addButton);
        buttonsPanel.add(removeButton);
        
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    /**
     * Définir l'entité sélectionnée et rafraîchir la liste des behaviors.
     */
    public void setSelectedEntity(Entity<?> entity) {
        this.selectedEntity = entity;
        refresh();
    }

    /**
     * Rafraîchir l'affichage des behaviors.
     */
    public void refresh() {
        behaviorListModel.clear();
        
        if (selectedEntity == null) {
            setEnabled(false);
            return;
        }
        
        setEnabled(true);
        
        for (Behavior behavior : selectedEntity.behaviors) {
            String displayName = formatBehaviorName(behavior);
            behaviorListModel.addElement(displayName);
        }
    }

    /**
     * Formater le nom d'un behavior pour l'affichage (classe simple).
     */
    private String formatBehaviorName(Behavior behavior) {
        String className = behavior.getClass().getSimpleName();
        // Si c'est HorizontalPatrolBehavior, montrer des infos additionnelles
        if (behavior instanceof HorizontalPatrolBehavior hpb) {
            return className + " (custom)";
        }
        if (behavior instanceof VerticalPatrolBehavior vpb) {
            return className + " (custom)";
        }
        return className;
    }

    /**
     * Ajouter un nouveau behavior à l'entité.
     */
    private void addBehavior() {
        if (selectedEntity == null) return;

        // Dialog de sélection du type de behavior
        String[] options = {
            "HorizontalPatrolBehavior",
            "VerticalPatrolBehavior",
            "PlayerInputBehavior",
            "CameraTrackingBehavior (requires setup)"
        };
        
        int choice = JOptionPane.showOptionDialog(
            this,
            "Sélectionner un Behavior à ajouter :",
            "Add Behavior",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice < 0) return;  // Cancelled

        Behavior newBehavior = createBehavior(options[choice]);
        if (newBehavior != null) {
            selectedEntity.behaviors.add(newBehavior);
            refresh();
            fireOnApplyChanges();
        }
    }

    /**
     * Créer une instance de behavior en fonction de son type.
     * Pour les behaviors avec paramètres, on utilise des valeurs par défaut.
     */
    private Behavior createBehavior(String behaviorType) {
        switch (behaviorType) {
            case "HorizontalPatrolBehavior" -> {
                // Patrol horizontal de 50 à 500 px à 80 px/s
                return new HorizontalPatrolBehavior(50, 500, 80);
            }
            case "VerticalPatrolBehavior" -> {
                // Patrol vertical de 50 à 400 px à 60 px/s
                return new VerticalPatrolBehavior(50, 400, 60);
            }
            case "PlayerInputBehavior" -> {
                // Valeurs par défaut : moveSpeed=150, jumpVelocity=-400
                return new PlayerInputBehavior(150, -400);
            }
            case "CameraTrackingBehavior (requires setup)" -> {
                JOptionPane.showMessageDialog(this,
                    "CameraTrackingBehavior requires Entity target and World.\n" +
                    "Please set it up manually in code for now.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * Supprimer le behavior sélectionné de l'entité.
     */
    private void removeBehavior() {
        if (selectedEntity == null) return;

        int selectedIndex = behaviorList.getSelectedIndex();
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(this, "Select a behavior to remove.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (selectedIndex < selectedEntity.behaviors.size()) {
            selectedEntity.behaviors.remove(selectedIndex);
            refresh();
            fireOnApplyChanges();
        }
    }

    /**
     * Déclencher le callback quand des changements sont appliqués.
     */
    private void fireOnApplyChanges() {
        if (onApplyChanges != null && selectedEntity != null) {
            onApplyChanges.accept(selectedEntity);
        }
    }

    public void setOnApplyChangesListener(Consumer<Entity<?>> listener) {
        this.onApplyChanges = listener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        behaviorList.setEnabled(enabled);
        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }
}
