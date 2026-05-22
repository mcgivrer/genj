package com.core.io;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

import com.core.App;
import com.core.utils.CircularQueue;

public class InputHandler implements KeyListener {
    private final App app;

    public InputHandler(App app) {
        this.app = app;
    }

    public CircularQueue<KeyEvent> keyEvents = new CircularQueue<>(100);
    public static boolean keys[] = new boolean[1024];

    public void keyTyped(KeyEvent ke) {
        keyEvents.add(ke);
    }

    public void keyPressed(KeyEvent ke) {
        keys[ke.getKeyCode()] = true;
    }

    public void keyReleased(KeyEvent ke) {
        keys[ke.getKeyCode()] = false;
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_ESCAPE, KeyEvent.VK_Q -> {
                App.exit = true;
                App.debug(App.class, 1, "Exit state changed to %b", App.exit);
            }
            case KeyEvent.VK_PAUSE, KeyEvent.VK_P -> {
                App.pause = !App.pause;
                App.debug(App.class, 1, "Pause state changed to %b", App.pause);
            }
            case KeyEvent.VK_D -> {
                if (ke.isControlDown()) {
                    App.debug = (App.debug + 1) % 10;
                    App.info(App.class, "Debug level changed to %d", App.debug);
                }
            }
            default -> {
            }
        }
    }

    public void resetKeyEventsStack() {
        keyEvents.clear();
    }

    public KeyEvent pollLastKeyEvent() {
        return keyEvents.pollLast();
    }

    public static boolean isKeyPressed(int keyCode) {
        return keys[keyCode];
    }

    public void attach(JFrame window) {
        window.addKeyListener(this);
    }

}
