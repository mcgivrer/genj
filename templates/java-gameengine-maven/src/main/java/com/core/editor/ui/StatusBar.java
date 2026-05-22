package com.core.editor.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Status bar displayed at the bottom of the editor window.
 * Shows mode, scene name, world coordinates, selection info, grid status.
 */
public class StatusBar extends JPanel {

    private final JLabel modeLabel    = new JLabel("✏ MODE ÉDITEUR");
    private final JLabel sceneLabel   = new JLabel("(unsaved)");
    private final JLabel coordsLabel  = new JLabel("x: 0.0  y: 0.0");
    private final JLabel selectionLabel = new JLabel("— aucune —");
    private final JLabel gridLabel    = new JLabel("— désactivée —");
    private final JLabel actionLabel  = new JLabel("");

    public StatusBar() {
        setLayout(new BorderLayout(4, 0));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));

        add(modeLabel,        BorderLayout.WEST);
        add(actionLabel,      BorderLayout.CENTER);
        add(coordsLabel,      BorderLayout.EAST);

        // Additional labels in a flow or grid layout if needed
        // For now, keep it simple with three zones
    }

    public void setMode(String text) {
        modeLabel.setText(text);
    }

    public void setScene(String text) {
        sceneLabel.setText(text);
    }

    public void setCoordinates(float x, float y) {
        coordsLabel.setText(String.format("x: %.1f  y: %.1f", x, y));
    }

    public void setSelection(String text) {
        selectionLabel.setText(text);
    }

    public void setGrid(String text) {
        gridLabel.setText(text);
    }

    public void setInfo(String text) {
        actionLabel.setText(text);
    }
}
