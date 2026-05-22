package com.core.editor.plugin.particle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import com.core.behavior.particle.EmitterConfig;
import com.core.behavior.particle.EmitterConfig.ParticleShape;
import com.core.entity.ParticleSystem;

/**
 * Swing panel for editing a {@link ParticleSystem}'s {@link EmitterConfig}.
 *
 * <p>The panel is used by the Level Editor's Properties panel when a
 * {@link ParticleSystem} entity is selected.  The
 * {@link com.core.editor.plugin.particle.ParticleSystemEditorPlugin} returns
 * this class from {@code getPropertiesPanelClass(ParticleSystem.class)}.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ParticleSystemConfigPanel panel = new ParticleSystemConfigPanel();
 * panel.setEntity(myParticleSystem);
 * editorPropertiesContainer.add(panel);
 * }</pre>
 *
 * <p>Pressing <em>Apply</em> writes the current UI values back to the bound
 * {@link EmitterConfig} (and updates {@code maxParticles} on the system itself).
 * Changes take effect immediately on the next engine frame.</p>
 */
public class ParticleSystemConfigPanel extends JPanel {

    // ─── Widgets ─────────────────────────────────────────────────────────────

    private final JSpinner   spnMaxParticles = new JSpinner(new SpinnerNumberModel(200, 1, 5000, 10));
    private final JSpinner   spnEmitRate     = new JSpinner(new SpinnerNumberModel(60.0, 0.1, 1000.0, 1.0));
    private final JSpinner   spnMinLife      = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 60.0, 0.1));
    private final JSpinner   spnMaxLife      = new JSpinner(new SpinnerNumberModel(3.0, 0.1, 60.0, 0.1));

    private final JSlider    sldDirection    = new JSlider(-180, 180, -90);
    private final JSlider    sldSpread       = new JSlider(0, 180, 20);
    private final JSpinner   spnMinSpeed     = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 2000.0, 10.0));
    private final JSpinner   spnMaxSpeed     = new JSpinner(new SpinnerNumberModel(200.0, 0.0, 2000.0, 10.0));
    private final JSpinner   spnMinSize      = new JSpinner(new SpinnerNumberModel(2.0, 1.0, 200.0, 1.0));
    private final JSpinner   spnMaxSize      = new JSpinner(new SpinnerNumberModel(8.0, 1.0, 200.0, 1.0));
    private final JSpinner   spnGravityFact  = new JSpinner(new SpinnerNumberModel(0.0, -5.0, 5.0, 0.05));
    private final JSpinner   spnWindX        = new JSpinner(new SpinnerNumberModel(0.0, -500.0, 500.0, 5.0));
    private final JSpinner   spnWindY        = new JSpinner(new SpinnerNumberModel(0.0, -500.0, 500.0, 5.0));

    private final JCheckBox  chkFadeOut      = new JCheckBox("Fade out", true);
    private final JCheckBox  chkShrink       = new JCheckBox("Shrink", false);

    private final JComboBox<ParticleShape> cmbShape =
            new JComboBox<>(ParticleShape.values());

    private final JButton    btnStartColor   = new JButton("Start colour");
    private final JButton    btnEndColor     = new JButton("End colour");
    private final JButton    btnApply        = new JButton("Apply");

    // ─── Bound state ─────────────────────────────────────────────────────────

    private ParticleSystem system;
    private EmitterConfig  config;

    private Color currentStartColor = new Color(255, 200, 30);
    private Color currentEndColor   = new Color(120, 10, 0);

    // ─── Constructor ─────────────────────────────────────────────────────────

    public ParticleSystemConfigPanel() {
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Particle System"));

        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonBar(),  BorderLayout.SOUTH);

        // Wire apply button
        btnApply.addActionListener(e -> applyToTarget());

        // Colour picker buttons
        btnStartColor.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Start colour", currentStartColor);
            if (c != null) { currentStartColor = c; updateColorButton(btnStartColor, c); }
        });
        btnEndColor.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "End colour", currentEndColor);
            if (c != null) { currentEndColor = c; updateColorButton(btnEndColor, c); }
        });

        updateColorButton(btnStartColor, currentStartColor);
        updateColorButton(btnEndColor,   currentEndColor);

        // Direction slider tooltip
        sldDirection.setToolTipText("Direction (degrees, 0=right, -90=up, 90=down)");
        sldDirection.setPaintTicks(true);
        sldDirection.setMajorTickSpacing(90);
        sldDirection.setMinorTickSpacing(15);
        sldSpread.setToolTipText("Spread (half-cone in degrees)");
        sldSpread.setPaintTicks(true);
        sldSpread.setMajorTickSpacing(45);
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Binds the panel to a {@link ParticleSystem}.  Reads the current
     * {@link EmitterConfig} from the first {@code ParticleEmitterBehavior}
     * attached to the system (if any) and populates the UI.
     *
     * @param entity the selected {@link ParticleSystem}
     */
    public void setEntity(ParticleSystem entity) {
        this.system = entity;
        this.config = extractConfig(entity);
        populateFromConfig();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor  = GridBagConstraints.EAST;
        lc.insets  = new Insets(2, 4, 2, 4);
        lc.fill    = GridBagConstraints.NONE;
        lc.gridx   = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.anchor  = GridBagConstraints.WEST;
        fc.insets  = new Insets(2, 0, 2, 4);
        fc.fill    = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.gridx   = 1;

        int row = 0;
        row = addRow(p, lc, fc, row, "Max particles",   spnMaxParticles);
        row = addRow(p, lc, fc, row, "Emit rate (p/s)", spnEmitRate);
        row = addRow(p, lc, fc, row, "Min life (s)",    spnMinLife);
        row = addRow(p, lc, fc, row, "Max life (s)",    spnMaxLife);
        row = addRow(p, lc, fc, row, "Direction (°)",   sldDirection);
        row = addRow(p, lc, fc, row, "Spread (°)",      sldSpread);
        row = addRow(p, lc, fc, row, "Min speed (px/s)", spnMinSpeed);
        row = addRow(p, lc, fc, row, "Max speed (px/s)", spnMaxSpeed);
        row = addRow(p, lc, fc, row, "Min size (px)",   spnMinSize);
        row = addRow(p, lc, fc, row, "Max size (px)",   spnMaxSize);
        row = addRow(p, lc, fc, row, "Gravity factor",  spnGravityFact);
        row = addRow(p, lc, fc, row, "Wind X (px/s²)",  spnWindX);
        row = addRow(p, lc, fc, row, "Wind Y (px/s²)",  spnWindY);
        row = addRow(p, lc, fc, row, "Shape",           cmbShape);

        // Checkboxes row
        lc.gridy = row;
        fc.gridy = row;
        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checks.add(chkFadeOut);
        checks.add(chkShrink);
        p.add(new JLabel("Options"), lc);
        p.add(checks, fc);
        row++;

        // Colour buttons row
        lc.gridy = row;
        fc.gridy = row;
        JPanel colours = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        colours.add(btnStartColor);
        colours.add(btnEndColor);
        p.add(new JLabel("Colours"), lc);
        p.add(colours, fc);

        return p;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bar.add(btnApply);
        return bar;
    }

    private static int addRow(JPanel p,
                              GridBagConstraints lc, GridBagConstraints fc,
                              int row, String label, java.awt.Component widget) {
        lc.gridy = row;
        fc.gridy = row;
        p.add(new JLabel(label, SwingConstants.RIGHT), lc);
        p.add(widget, fc);
        return row + 1;
    }

    private void populateFromConfig() {
        if (system != null) {
            spnMaxParticles.setValue(system.maxParticles);
        }
        if (config == null) return;

        spnEmitRate.setValue((double) config.emitRate);
        spnMinLife.setValue((double) config.minLife);
        spnMaxLife.setValue((double) config.maxLife);
        sldDirection.setValue(Math.round((float) Math.toDegrees(config.direction)));
        sldSpread.setValue(Math.round((float) Math.toDegrees(config.spread)));
        spnMinSpeed.setValue((double) config.minSpeed);
        spnMaxSpeed.setValue((double) config.maxSpeed);
        spnMinSize.setValue((double) config.minSize);
        spnMaxSize.setValue((double) config.maxSize);
        spnGravityFact.setValue((double) config.gravityFactor);
        spnWindX.setValue((double) config.windX);
        spnWindY.setValue((double) config.windY);
        chkFadeOut.setSelected(config.fadeOut);
        chkShrink.setSelected(config.shrink);
        cmbShape.setSelectedItem(config.shape);
        if (config.startColor != null) {
            currentStartColor = config.startColor;
            updateColorButton(btnStartColor, currentStartColor);
        }
        if (config.endColor != null) {
            currentEndColor = config.endColor;
            updateColorButton(btnEndColor, currentEndColor);
        }
    }

    private void applyToTarget() {
        if (system != null) {
            system.setMaxParticles(((Number) spnMaxParticles.getValue()).intValue());
        }
        if (config == null) return;

        config.emitRate      = ((Number) spnEmitRate.getValue()).floatValue();
        config.minLife       = ((Number) spnMinLife.getValue()).floatValue();
        config.maxLife       = ((Number) spnMaxLife.getValue()).floatValue();
        config.direction     = (float) Math.toRadians(sldDirection.getValue());
        config.spread        = (float) Math.toRadians(sldSpread.getValue());
        config.minSpeed      = ((Number) spnMinSpeed.getValue()).floatValue();
        config.maxSpeed      = ((Number) spnMaxSpeed.getValue()).floatValue();
        config.minSize       = ((Number) spnMinSize.getValue()).floatValue();
        config.maxSize       = ((Number) spnMaxSize.getValue()).floatValue();
        config.gravityFactor = ((Number) spnGravityFact.getValue()).floatValue();
        config.windX         = ((Number) spnWindX.getValue()).floatValue();
        config.windY         = ((Number) spnWindY.getValue()).floatValue();
        config.fadeOut       = chkFadeOut.isSelected();
        config.shrink        = chkShrink.isSelected();
        config.shape         = (ParticleShape) cmbShape.getSelectedItem();
        config.startColor    = currentStartColor;
        config.endColor      = currentEndColor;
    }

    private static void updateColorButton(JButton btn, Color c) {
        btn.setBackground(c);
        btn.setForeground(isLight(c) ? Color.BLACK : Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
    }

    private static boolean isLight(Color c) {
        double luminance = 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
        return luminance > 128;
    }

    /**
     * Extracts the {@link EmitterConfig} from the first
     * {@code ParticleEmitterBehavior} found on the system, or {@code null}.
     */
    private static EmitterConfig extractConfig(ParticleSystem ps) {
        if (ps == null) return null;
        for (var b : ps.behaviors) {
            if (b instanceof com.core.behavior.particle.ParticleEmitterBehavior peb) {
                return peb.getConfig();
            }
        }
        return null;
    }
}
