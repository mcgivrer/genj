package com.core.editor.ui;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.core.editor.tools.EditorTool;

/**
 * Toolbar UI for selecting the active editor tool.
 *
 * <p>Provides toggle buttons for tool selection. Only one tool can be active at a time.</p>
 */
public class ToolBar extends JPanel {
    private final ButtonGroup toolGroup = new ButtonGroup();
    private JToggleButton selectedButton = null;

    public ToolBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
    }

    /**
     * Adds a tool button to the toolbar.
     *
     * @param tool      the tool to add
     * @param listener  callback when tool is selected (receives the tool)
     */
    public void addToolButton(EditorTool tool, ActionListener listener) {
        JToggleButton button = new JToggleButton(tool.getName());
        button.setToolTipText("Activate " + tool.getName());

        // Wrap the listener to pass the tool object
        button.addActionListener(e -> {
            if (button.isSelected()) {
                listener.actionPerformed(new ToolSelectEvent(button, tool));
            }
        });

        toolGroup.add(button);
        add(button);

        // Set first button as initially selected
        if (selectedButton == null) {
            button.setSelected(true);
            selectedButton = button;
        }
    }

    /**
     * Internal event class to carry the selected tool.
     */
    private static class ToolSelectEvent extends java.awt.event.ActionEvent {
        private final EditorTool tool;

        ToolSelectEvent(Object source, EditorTool tool) {
            super(source, 0, "toolSelected");
            this.tool = tool;
        }

        EditorTool getTool() {
            return tool;
        }
    }
}
