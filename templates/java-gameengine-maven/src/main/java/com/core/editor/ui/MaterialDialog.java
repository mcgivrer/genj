package com.core.editor.ui;

import com.core.physics.Material;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

/**
 * MaterialDialog — Création / édition d'un Material.
 *
 * Boîte de dialogue modale permettant de créer un nouveau Material ou
 * de modifier un existant. Supporte les presets (DEFAULT, WOOD, STONE, METAL, RUBBER, ICE)
 * et affiche la masse calculée pour 32×32 px en temps réel.
 *
 * Utilisation :
 * - Créer : MaterialDialog dlg = new MaterialDialog(parentFrame, null);
 *           if (dlg.showDialog() == JOptionPane.OK_OPTION) { Material m = dlg.getMaterial(); }
 * - Modifier : MaterialDialog dlg = new MaterialDialog(parentFrame, existingMaterial);
 *              if (dlg.showDialog() == JOptionPane.OK_OPTION) { Material m = dlg.getMaterial(); }
 */
public class MaterialDialog extends JDialog {
    private Material resultMaterial = null;
    
    private JTextField nameField;
    private JSlider densitySlider;
    private JSpinner densitySpinner;
    private JSlider frictionSlider;
    private JSpinner frictionSpinner;
    private JSlider elasticitySlider;
    private JSpinner elasticitySpinner;
    private JSlider rotationalFrictionSlider;
    private JSpinner rotationalFrictionSpinner;
    private JLabel massLabel;
    
    private int dialogResult = JOptionPane.CANCEL_OPTION;

    /**
     * Initialiser le dialogue.
     * @param parent fenêtre parente
     * @param initialMaterial null pour créer, ou Material existant pour modifier
     */
    public MaterialDialog(Frame parent, Material initialMaterial) {
        super(parent, "Material Editor", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(500, 420);
        setLocationRelativeTo(parent);
        
        initUI(initialMaterial);
    }

    private void initUI(Material initialMaterial) {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBackground(new Color(30, 30, 62));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // ─── Name ───
        addLabel("Name:", contentPanel, gbc, row);
        nameField = new JTextField(20);
        nameField.setBackground(new Color(42, 42, 74));
        nameField.setForeground(new Color(208, 208, 224));
        nameField.setCaretColor(new Color(208, 208, 224));
        nameField.setBorder(BorderFactory.createLineBorder(new Color(90, 106, 154), 1));
        nameField.setFont(new Font("Monospaced", Font.PLAIN, 10));
        gbc.gridx = 1;
        gbc.gridy = row;
        contentPanel.add(nameField, gbc);
        row++;

        // ─── Density ───
        addLabel("Density:", contentPanel, gbc, row);
        JPanel densityPanel = createSliderSpinnerPanel(0.1f, 10.0f, 1.0f);
        densitySlider = (JSlider) densityPanel.getComponent(0);
        densitySpinner = (JSpinner) densityPanel.getComponent(1);
        densitySlider.addChangeListener(e -> densitySpinner.setValue(densitySlider.getValue() / 100.0f));
        densitySpinner.addChangeListener(e -> updateDensitySlider());
        gbc.gridx = 1;
        gbc.gridy = row;
        contentPanel.add(densityPanel, gbc);
        row++;

        // ─── Friction ───
        addLabel("Friction:", contentPanel, gbc, row);
        JPanel frictionPanel = createSliderSpinnerPanel(0.0f, 1.0f, 0.2f);
        frictionSlider = (JSlider) frictionPanel.getComponent(0);
        frictionSpinner = (JSpinner) frictionPanel.getComponent(1);
        frictionSlider.addChangeListener(e -> frictionSpinner.setValue(frictionSlider.getValue() / 100.0f));
        frictionSpinner.addChangeListener(e -> updateFrictionSlider());
        gbc.gridx = 1;
        gbc.gridy = row;
        contentPanel.add(frictionPanel, gbc);
        row++;

        // ─── Elasticity ───
        addLabel("Elasticity:", contentPanel, gbc, row);
        JPanel elasticityPanel = createSliderSpinnerPanel(0.0f, 1.0f, 0.4f);
        elasticitySlider = (JSlider) elasticityPanel.getComponent(0);
        elasticitySpinner = (JSpinner) elasticityPanel.getComponent(1);
        elasticitySlider.addChangeListener(e -> elasticitySpinner.setValue(elasticitySlider.getValue() / 100.0f));
        elasticitySpinner.addChangeListener(e -> updateElasticitySlider());
        gbc.gridx = 1;
        gbc.gridy = row;
        contentPanel.add(elasticityPanel, gbc);
        row++;

        // ─── Rotational Friction ───
        addLabel("Rotational Friction:", contentPanel, gbc, row);
        JPanel rotationalPanel = createSliderSpinnerPanel(0.0f, 1.0f, 0.3f);
        rotationalFrictionSlider = (JSlider) rotationalPanel.getComponent(0);
        rotationalFrictionSpinner = (JSpinner) rotationalPanel.getComponent(1);
        rotationalFrictionSlider.addChangeListener(e -> rotationalFrictionSpinner.setValue(rotationalFrictionSlider.getValue() / 100.0f));
        rotationalFrictionSpinner.addChangeListener(e -> updateRotationalSlider());
        gbc.gridx = 1;
        gbc.gridy = row;
        contentPanel.add(rotationalPanel, gbc);
        row++;

        // ─── Mass calculated ───
        gbc.gridx = 0;
        gbc.gridy = row;
        massLabel = new JLabel("Mass (32×32): 0.10");
        massLabel.setForeground(new Color(200, 200, 220));
        massLabel.setFont(new Font("Segoe UI", Font.ITALIC, 9));
        contentPanel.add(massLabel, gbc);
        row++;

        // ─── Presets buttons ───
        JPanel presetsPanel = new JPanel(new GridLayout(2, 3, 4, 4));
        presetsPanel.setOpaque(false);
        String[] presets = {"DEFAULT", "WOOD", "METAL", "RUBBER", "ICE", "STONE"};
        for (String preset : presets) {
            JButton btn = new JButton(preset);
            btn.setBackground(new Color(74, 106, 154));
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            btn.addActionListener(e -> applyPreset(preset));
            presetsPanel.add(btn);
        }
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        contentPanel.add(presetsPanel, gbc);
        row++;

        // ─── OK / Cancel buttons ───
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsPanel.setOpaque(false);
        
        JButton okBtn = new JButton("OK");
        okBtn.setBackground(new Color(74, 154, 74));
        okBtn.setForeground(Color.WHITE);
        okBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        okBtn.addActionListener(e -> {
            resultMaterial = buildMaterial();
            dialogResult = JOptionPane.OK_OPTION;
            dispose();
        });
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(new Color(154, 85, 85));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cancelBtn.addActionListener(e -> {
            dialogResult = JOptionPane.CANCEL_OPTION;
            dispose();
        });
        
        buttonsPanel.add(okBtn);
        buttonsPanel.add(cancelBtn);
        
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        contentPanel.add(buttonsPanel, gbc);

        // Initialize with material or defaults
        if (initialMaterial != null) {
            nameField.setText(initialMaterial.name);
            densitySpinner.setValue(initialMaterial.density);
            frictionSpinner.setValue(initialMaterial.friction);
            elasticitySpinner.setValue(initialMaterial.elasticity);
            rotationalFrictionSpinner.setValue(initialMaterial.rotationalFriction);
        } else {
            nameField.setText("custom_material");
            densitySpinner.setValue(1.0f);
            frictionSpinner.setValue(0.2f);
            elasticitySpinner.setValue(0.4f);
            rotationalFrictionSpinner.setValue(0.3f);
        }

        updateMassLabel();

        setContentPane(contentPanel);
    }

    /**
     * Créer un panel avec slider (0-100) et spinner (0.0-max).
     */
    private JPanel createSliderSpinnerPanel(float minVal, float maxVal, float defaultVal) {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.setOpaque(false);
        
        JSlider slider = new JSlider(0, 100, (int)(defaultVal * 100));
        slider.setBackground(new Color(40, 40, 70));
        slider.setForeground(new Color(200, 200, 220));
        
        SpinnerNumberModel model = new SpinnerNumberModel(defaultVal, minVal, maxVal, 0.05);
        JSpinner spinner = new JSpinner(model);
        spinner.setPreferredSize(new Dimension(60, 24));
        
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor) spinner.getEditor();
        editor.getTextField().setBackground(new Color(42, 42, 74));
        editor.getTextField().setForeground(new Color(208, 208, 224));
        editor.getTextField().setBorder(BorderFactory.createLineBorder(new Color(90, 106, 154), 1));
        
        panel.add(slider, BorderLayout.CENTER);
        panel.add(spinner, BorderLayout.EAST);
        
        return panel;
    }

    private void addLabel(String text, JPanel panel, GridBagConstraints gbc, int row) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(200, 200, 220));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        label.setPreferredSize(new Dimension(140, 24));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(label, gbc);
        gbc.weightx = 1.0;
    }

    private void updateDensitySlider() {
        float val = ((Number) densitySpinner.getValue()).floatValue();
        densitySlider.setValue((int)(val * 100));
        updateMassLabel();
    }

    private void updateFrictionSlider() {
        float val = ((Number) frictionSpinner.getValue()).floatValue();
        frictionSlider.setValue((int)(val * 100));
    }

    private void updateElasticitySlider() {
        float val = ((Number) elasticitySpinner.getValue()).floatValue();
        elasticitySlider.setValue((int)(val * 100));
    }

    private void updateRotationalSlider() {
        float val = ((Number) rotationalFrictionSpinner.getValue()).floatValue();
        rotationalFrictionSlider.setValue((int)(val * 100));
    }

    private void updateMassLabel() {
        float density = ((Number) densitySpinner.getValue()).floatValue();
        float mass = Math.max(0.1f, density * 32 * 32 * 0.01f);
        massLabel.setText(String.format("Mass (32×32): %.2f", mass));
    }

    private void applyPreset(String presetName) {
        Material preset = switch (presetName) {
            case "DEFAULT" -> Material.DEFAULT;
            case "WOOD" -> Material.WOOD;
            case "METAL" -> Material.METAL;
            case "RUBBER" -> Material.RUBBER;
            case "ICE" -> Material.ICE;
            case "STONE" -> Material.STONE;
            default -> Material.DEFAULT;
        };

        nameField.setText(preset.name);
        densitySpinner.setValue(preset.density);
        frictionSpinner.setValue(preset.friction);
        elasticitySpinner.setValue(preset.elasticity);
        rotationalFrictionSpinner.setValue(preset.rotationalFriction);
        
        updateDensitySlider();
        updateFrictionSlider();
        updateElasticitySlider();
        updateRotationalSlider();
    }

    private Material buildMaterial() {
        String name = nameField.getText().trim().isEmpty() ? "custom_material" : nameField.getText().trim();
        float density = ((Number) densitySpinner.getValue()).floatValue();
        float friction = ((Number) frictionSpinner.getValue()).floatValue();
        float elasticity = ((Number) elasticitySpinner.getValue()).floatValue();
        float rotationalFriction = ((Number) rotationalFrictionSpinner.getValue()).floatValue();
        
        return new Material(name, density, friction, elasticity, rotationalFriction);
    }

    /**
     * Afficher le dialogue en mode modal et retourner le code résultat.
     */
    public int showDialog() {
        setVisible(true);
        return dialogResult;
    }

    /**
     * Récupérer le Material créé/modifié (null si annulation).
     */
    public Material getMaterial() {
        return resultMaterial;
    }
}
