package com.core.editor.ui;

import com.core.entity.Entity;
import com.core.entity.GameObject;
import com.core.entity.Nature;
import com.core.entity.TextObject;
import com.core.physics.PhysicsType;
import com.core.physics.Material;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * PropertiesPanel — édition des propriétés d'une entité sélectionnée via JTable.
 *
 * Utilise un JTable avec 2 colonnes (Property, Value) pour afficher et modifier :
 * - Propriétés de base (Entity): name, x, y, vx, vy, mass, rotation, renderPriority, physicsType, active, material
 * - Propriétés de GameObject: nature, color, fillColor, width, height
 * - Propriétés de TextObject: text, fontSize, hud
 *
 * Synchronisation :
 * - EditorViewport → setSelectedEntity(entity) → PropertiesPanel.refresh()
 * - PropertiesPanel "Apply" → applyChanges() → fireOnApplyChanges()
 */
public class PropertiesPanel extends JPanel {
    private Entity<?> selectedEntity;
    private Consumer<Entity<?>> onApplyChanges;
    
    private PropertyTableModel tableModel;
    private JTable propertiesTable;
    private JButton applyButton;
    private JButton resetButton;

    public PropertiesPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 62));
        initUI();
    }

    private void initUI() {
        // ─── Header Panel ───
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(62, 30, 30));
        headerPanel.setBorder(BorderFactory.createLineBorder(new Color(170, 85, 85), 1));
        
        JLabel headerLabel = new JLabel("Properties Panel");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        headerLabel.setForeground(new Color(245, 160, 160));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);

        // ─── Table Panel ───
        tableModel = new PropertyTableModel();
        propertiesTable = new JTable(tableModel);
        propertiesTable.setBackground(new Color(40, 40, 70));
        propertiesTable.setForeground(new Color(200, 200, 220));
        propertiesTable.setSelectionBackground(new Color(74, 106, 154));
        propertiesTable.setSelectionForeground(Color.WHITE);
        propertiesTable.setRowHeight(24);
        propertiesTable.setColumnSelectionAllowed(false);
        propertiesTable.setRowSelectionAllowed(true);
        
        // Column 0 (Property name) — not editable
        propertiesTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        DefaultTableCellRenderer propRenderer = new DefaultTableCellRenderer();
        propRenderer.setBackground(new Color(35, 35, 65));
        propRenderer.setForeground(new Color(200, 200, 220));
        propRenderer.setFont(new Font("Monospaced", Font.PLAIN, 10));
        propertiesTable.getColumnModel().getColumn(0).setCellRenderer(propRenderer);
        
        // Column 1 (Property value) — editable with custom editors
        propertiesTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        PropertyValueEditor valueEditor = new PropertyValueEditor(this);
        propertiesTable.getColumnModel().getColumn(1).setCellEditor(valueEditor);
        PropertyValueRenderer valueRenderer = new PropertyValueRenderer();
        propertiesTable.getColumnModel().getColumn(1).setCellRenderer(valueRenderer);
        
        JScrollPane scrollPane = new JScrollPane(propertiesTable);
        scrollPane.setBackground(new Color(30, 30, 62));
        scrollPane.getViewport().setBackground(new Color(30, 30, 62));
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);

        // ─── Buttons Panel ───
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonsPanel.setBackground(new Color(30, 30, 62));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 8));
        
        applyButton = new JButton("Apply");
        applyButton.setBackground(new Color(74, 154, 74));
        applyButton.setForeground(Color.WHITE);
        applyButton.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        applyButton.addActionListener(e -> applyChanges());
        
        resetButton = new JButton("Reset");
        resetButton.setBackground(new Color(154, 85, 85));
        resetButton.setForeground(Color.WHITE);
        resetButton.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        resetButton.addActionListener(e -> refresh());
        
        buttonsPanel.add(applyButton);
        buttonsPanel.add(resetButton);
        
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    /**
     * Définir l'entité sélectionnée et rafraîchir l'affichage.
     */
    public void setSelectedEntity(Entity<?> entity) {
        this.selectedEntity = entity;
        tableModel.setEntity(entity);
        refresh();
    }

    /**
     * Rafraîchir l'affichage des propriétés.
     */
    public void refresh() {
        tableModel.fireTableDataChanged();
    }

    /**
     * Appliquer les changements à l'entité sélectionnée.
     */
    private void applyChanges() {
        if (selectedEntity == null) return;

        tableModel.applyChanges();
        
        if (onApplyChanges != null) {
            onApplyChanges.accept(selectedEntity);
        }
    }

    public void setOnApplyChangesListener(Consumer<Entity<?>> listener) {
        this.onApplyChanges = listener;
    }

    // ─── Inner class: PropertyTableModel ───

    private class PropertyTableModel extends AbstractTableModel {
        private Entity<?> entity;
        private List<PropertyRow> rows = new ArrayList<>();

        public void setEntity(Entity<?> entity) {
            this.entity = entity;
            rebuildRows();
            fireTableDataChanged();
        }

        private void rebuildRows() {
            rows.clear();
            if (entity == null) return;

            // Common Entity properties
            addRow("name", "String", () -> entity.name, v -> entity.name = (String) v);
            addRow("x", "Float", () -> entity.x, v -> entity.x = ((Number) v).floatValue());
            addRow("y", "Float", () -> entity.y, v -> entity.y = ((Number) v).floatValue());
            addRow("width", "Integer", () -> entity.width, v -> entity.width = ((Number) v).intValue());
            addRow("height", "Integer", () -> entity.height, v -> entity.height = ((Number) v).intValue());
            addRow("vx", "Float", () -> entity.vx, v -> entity.vx = ((Number) v).floatValue());
            addRow("vy", "Float", () -> entity.vy, v -> entity.vy = ((Number) v).floatValue());
            addRow("mass", "Float", () -> entity.mass, v -> entity.mass = ((Number) v).floatValue());
            addRow("rotation", "Float", () -> entity.rotation, v -> entity.rotation = ((Number) v).floatValue());
            addRow("renderPriority", "Integer", () -> entity.renderPriority, v -> entity.renderPriority = ((Number) v).intValue());
            addRow("physicsType", "PhysicsType", () -> entity.physicsType, v -> entity.physicsType = (PhysicsType) v);
            addRow("active", "Boolean", () -> entity.active, v -> entity.active = (Boolean) v);
            addRow("material", "Material", () -> entity.material != null ? entity.material.name : "DEFAULT", v -> {});

            // GameObject-specific properties
            if (entity instanceof GameObject go) {
                addRow("nature", "Nature", () -> go.nature, v -> go.nature = (Nature) v);
                addRow("color", "Color", () -> go.color, v -> go.color = (Color) v);
                addRow("fillColor", "Color", () -> go.fillColor, v -> go.fillColor = (Color) v);
            }

            // TextObject-specific properties
            if (entity instanceof TextObject to) {
                addRow("text", "String", () -> to.text, v -> to.text = (String) v);
                addRow("fontSize", "Float", () -> to.fontSize, v -> to.fontSize = ((Number) v).floatValue());
                addRow("hud", "Boolean", () -> to.hud, v -> to.hud = (Boolean) v);
            }
        }

        private void addRow(String name, String type, PropertyGetter getter, PropertySetter setter) {
            rows.add(new PropertyRow(name, type, getter, setter));
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PropertyRow row = rows.get(rowIndex);
            if (columnIndex == 0) {
                return row.name;
            } else {
                return row.getter.get();
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                PropertyRow row = rows.get(rowIndex);
                row.setter.set(value);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex == 0) return false; // Property names not editable
            if (entity == null) return false;
            
            PropertyRow row = rows.get(rowIndex);
            // Material is read-only
            return !row.name.equals("material");
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnIndex == 0 ? "Property" : "Value";
        }

        public void applyChanges() {
            // Changes are applied immediately to the entity via setters
            fireTableDataChanged();
        }
    }

    // ─── Inner class: PropertyRow ───

    private static class PropertyRow {
        String name;
        String type;
        PropertyGetter getter;
        PropertySetter setter;

        PropertyRow(String name, String type, PropertyGetter getter, PropertySetter setter) {
            this.name = name;
            this.type = type;
            this.getter = getter;
            this.setter = setter;
        }
    }

    // ─── Inner interfaces ───

    @FunctionalInterface
    private interface PropertyGetter {
        Object get();
    }

    @FunctionalInterface
    private interface PropertySetter {
        void set(Object value);
    }

    // ─── Inner class: PropertyValueRenderer ───

    private static class PropertyValueRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(isSelected ? new Color(74, 106, 154) : new Color(40, 40, 70));
            c.setForeground(isSelected ? Color.WHITE : new Color(200, 200, 220));
            c.setFont(new Font("Monospaced", Font.PLAIN, 10));

            if (value instanceof Color) {
                setText("■");
                setBackground((Color) value);
            } else if (value != null) {
                setText(value.toString());
            } else {
                setText("(null)");
            }
            return c;
        }
    }

    // ─── Inner class: PropertyValueEditor ───

    private static class PropertyValueEditor extends AbstractCellEditor implements TableCellEditor {
        private PropertiesPanel panel;
        private Object value;
        private JComponent component;

        PropertyValueEditor(PropertiesPanel panel) {
            this.panel = panel;
        }

        @Override
        public Object getCellEditorValue() {
            return value;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.value = value;

            if (value instanceof String) {
                JTextField textField = new JTextField(value.toString());
                textField.setBackground(new Color(60, 60, 90));
                textField.setForeground(new Color(200, 200, 220));
                textField.setCaretColor(new Color(200, 200, 220));
                textField.setBorder(BorderFactory.createLineBorder(new Color(100, 120, 160), 1));
                textField.setFont(new Font("Monospaced", Font.PLAIN, 10));
                textField.addActionListener(e -> {
                    this.value = textField.getText();
                    stopCellEditing();
                });
                component = textField;
            } else if (value instanceof Number) {
                JTextField textField = new JTextField(value.toString());
                textField.setBackground(new Color(60, 60, 90));
                textField.setForeground(new Color(200, 200, 220));
                textField.setCaretColor(new Color(200, 200, 220));
                textField.setBorder(BorderFactory.createLineBorder(new Color(100, 120, 160), 1));
                textField.setFont(new Font("Monospaced", Font.PLAIN, 10));
                textField.addActionListener(e -> {
                    try {
                        if (value instanceof Float || value instanceof Double) {
                            this.value = Float.parseFloat(textField.getText());
                        } else if (value instanceof Integer) {
                            this.value = Integer.parseInt(textField.getText());
                        }
                        stopCellEditing();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(panel, "Invalid number format", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                component = textField;
            } else if (value instanceof Boolean) {
                JCheckBox checkBox = new JCheckBox("", (Boolean) value);
                checkBox.setBackground(new Color(40, 40, 70));
                checkBox.setForeground(new Color(200, 200, 220));
                checkBox.addActionListener(e -> {
                    this.value = checkBox.isSelected();
                    stopCellEditing();
                });
                component = checkBox;
            } else if (value instanceof PhysicsType) {
                JComboBox<PhysicsType> combo = new JComboBox<>(PhysicsType.values());
                combo.setSelectedItem(value);
                combo.setBackground(new Color(60, 60, 90));
                combo.setForeground(new Color(200, 200, 220));
                combo.addActionListener(e -> {
                    this.value = combo.getSelectedItem();
                    stopCellEditing();
                });
                component = combo;
            } else if (value instanceof Nature) {
                JComboBox<Nature> combo = new JComboBox<>(Nature.values());
                combo.setSelectedItem(value);
                combo.setBackground(new Color(60, 60, 90));
                combo.setForeground(new Color(200, 200, 220));
                combo.addActionListener(e -> {
                    this.value = combo.getSelectedItem();
                    stopCellEditing();
                });
                component = combo;
            } else if (value instanceof Color) {
                JButton button = new JButton("Choose Color");
                button.setBackground((Color) value);
                button.setForeground(Color.WHITE);
                button.addActionListener(e -> {
                    Color chosen = JColorChooser.showDialog(panel, "Choose Color", (Color) value);
                    if (chosen != null) {
                        this.value = chosen;
                        stopCellEditing();
                    }
                });
                component = button;
            } else {
                JLabel label = new JLabel(value != null ? value.toString() : "(null)");
                label.setBackground(new Color(40, 40, 70));
                label.setForeground(new Color(200, 200, 220));
                label.setFont(new Font("Monospaced", Font.PLAIN, 10));
                component = label;
            }

            return component;
        }
    }
}
