package com.core.editor.ui;

import com.core.entity.Entity;
import com.core.entity.GameObject;
import com.core.entity.TextObject;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * SceneTreePanel — Affichage hiérarchique des entités de la scène.
 *
 * Utilise un JTree pour afficher World → children de manière hiérarchique.
 * Double-clic sur une entité déclenche la sélection (callback).
 *
 * Synchronisation :
 * - EditorViewport.setSelectedEntity() → SceneTreePanel.setSelectedEntity()
 * - User double-click → onSelectionChangedListener.accept(entity)
 *   → EditorViewport.setSelectedEntity(entity) → PropertiesPanel.refresh()
 */
public class SceneTreePanel extends JPanel {
    private Entity<?> rootEntity;
    private Consumer<Entity<?>> onSelectionChanged;
    
    private JTree sceneTree;
    private DefaultTreeModel treeModel;
    private JTextField filterField;
    
    // Map to track current filter (to rebuild tree when filter changes)
    private String currentFilter = "";

    public SceneTreePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 62));
        initUI();
    }

    private void initUI() {
        // ─── Header Panel ───
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(62, 30, 30));
        headerPanel.setBorder(BorderFactory.createLineBorder(new Color(170, 85, 85), 1));
        
        JLabel headerLabel = new JLabel("Scene Tree");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        headerLabel.setForeground(new Color(245, 160, 160));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);

        // ─── Filter Panel ───
        JPanel filterPanel = new JPanel(new BorderLayout(4, 4));
        filterPanel.setBackground(new Color(30, 30, 62));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        filterLabel.setForeground(new Color(200, 200, 220));
        
        filterField = new JTextField();
        filterField.setBackground(new Color(42, 42, 74));
        filterField.setForeground(new Color(208, 208, 224));
        filterField.setCaretColor(new Color(208, 208, 224));
        filterField.setBorder(BorderFactory.createLineBorder(new Color(90, 106, 154), 1));
        filterField.setFont(new Font("Monospaced", Font.PLAIN, 9));
        filterField.setPreferredSize(new Dimension(100, 20));
        filterField.addActionListener(e -> applyFilter());
        
        filterPanel.add(filterLabel, BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        
        add(filterPanel, BorderLayout.NORTH);

        // ─── Tree Panel ───
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("World");
        treeModel = new DefaultTreeModel(root);
        sceneTree = new JTree(treeModel);
        sceneTree.setBackground(new Color(40, 40, 70));
        sceneTree.setForeground(new Color(200, 200, 220));
        sceneTree.setRowHeight(24);
        sceneTree.setCellRenderer(new EntityTreeCellRenderer());
        sceneTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = sceneTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.getUserObject() instanceof Entity<?> entity) {
                            if (onSelectionChanged != null) {
                                onSelectionChanged.accept(entity);
                            }
                        }
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(sceneTree);
        scrollPane.setBackground(new Color(30, 30, 62));
        scrollPane.getViewport().setBackground(new Color(30, 30, 62));
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Définir l'entité racine (World) et construire l'arborescence.
     */
    public void setRootEntity(Entity<?> entity) {
        this.rootEntity = entity;
        rebuildTree();
    }

    /**
     * Marquer une entité comme sélectionnée dans l'arborescence.
     */
    public void setSelectedEntity(Entity<?> entity) {
        if (entity == null) {
            sceneTree.clearSelection();
            return;
        }
        
        // Chercher et sélectionner le nœud correspondant
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        DefaultMutableTreeNode node = findNodeForEntity(root, entity);
        if (node != null) {
            TreePath path = new TreePath(node.getPath());
            sceneTree.setSelectionPath(path);
            sceneTree.scrollPathToVisible(path);
        }
    }

    /**
     * Reconstruire l'arborescence à partir de rootEntity.
     */
    private void rebuildTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();
        
        if (rootEntity == null) {
            treeModel.reload();
            return;
        }
        
        // Remplir récursivement
        populateNode(root, rootEntity);
        treeModel.reload();
    }

    /**
     * Remplir récursivement un nœud et ses enfants.
     */
    private void populateNode(DefaultMutableTreeNode parent, Entity<?> entity) {
        for (Entity<?> child : entity.children) {
            if (matchesFilter(child)) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                parent.add(childNode);
                populateNode(childNode, child);
            }
        }
    }

    /**
     * Vérifier si une entité correspond au filtre courant.
     */
    private boolean matchesFilter(Entity<?> entity) {
        if (currentFilter.isEmpty()) return true;
        return entity.name != null && entity.name.toLowerCase().contains(currentFilter.toLowerCase());
    }

    /**
     * Appliquer le filtre et reconstruire.
     */
    private void applyFilter() {
        currentFilter = filterField.getText();
        rebuildTree();
    }

    /**
     * Chercher récursivement le nœud pour une entité donnée.
     */
    private DefaultMutableTreeNode findNodeForEntity(DefaultMutableTreeNode node, Entity<?> entity) {
        if (node.getUserObject() == entity) {
            return node;
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode found = findNodeForEntity(child, entity);
            if (found != null) return found;
        }
        
        return null;
    }

    public void setOnSelectionChangedListener(Consumer<Entity<?>> listener) {
        this.onSelectionChanged = listener;
    }

    // ─── Inner class: EntityTreeCellRenderer ───

    private static class EntityTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            
            setBackground(selected ? new Color(74, 106, 154) : new Color(40, 40, 70));
            setForeground(selected ? Color.WHITE : new Color(200, 200, 220));
            setFont(new Font("Segoe UI", Font.PLAIN, 10));
            
            if (value instanceof DefaultMutableTreeNode node) {
                Object userObject = node.getUserObject();
                if (userObject instanceof Entity<?> entity) {
                    // Format: name [type] (x, y)
                    String type = entity.getClass().getSimpleName();
                    String text = String.format("%s [%s] (%.0f, %.0f)", 
                        entity.name != null ? entity.name : "unnamed",
                        type,
                        entity.x, entity.y);
                    setText(text);
                    
                    // Icons for different entity types
                    if (entity instanceof TextObject) {
                        setIcon(null); // T
                    } else if (entity instanceof GameObject) {
                        setIcon(null); // G
                    }
                } else if (userObject instanceof String) {
                    setText((String) userObject);
                }
            }
            
            return this;
        }
    }
}
