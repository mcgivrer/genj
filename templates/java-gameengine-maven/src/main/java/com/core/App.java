package com.core;

import java.awt.Dimension;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.core.entity.Entity;
import com.core.gfx.Renderer;
import com.core.io.InputHandler;
import com.core.physics.PhysicsEngine;
import com.core.scene.Scene;
import com.core.utils.AppMode;
import com.demo.scenes.DemoScene;

/**
 * GameEngineDemo is a comprehensive Java-based game App application
 * framework that provides
 * a complete game loop, window management, input handling, and rendering
 * system.
 *
 * <p>
 * This App class serves as the main entry point for creating
 * 2D games with features including:
 * <ul>
 * <li>Configurable Application modes (Development/Production)</li>
 * <li>Internationalization support</li>
 * <li>Key input handling with event queuing</li>
 * <li>Frame rate management and statistics tracking</li>
 * <li>Comprehensive logging system</li>
 * <li>Graphics utilities for text rendering</li>
 * </ul>
 *
 * <p>
 * The Application follows a standard game loop pattern with initialization,
 * update, render, and cleanup phases. Configuration can be provided through
 * command-line arguments and property files.
 *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * App App = new App();
 * App.run(new String[] { "App.debug=1", "App.mode=DEVELOPMENT" });
 * }</pre>
 *
 * @since 2026
 * @author Frédéric Delorme<frederic.delorme@gmail.com>
 * @version 1.0.0
 */
public class App {

    /**
     * The resource bundle for internationalization.
     */
    public static ResourceBundle i18Text = ResourceBundle.getBundle(
            "i18n/messages");
    /**
     * The App configuration properties.
     */
    public static Properties config = new Properties();
    /**
     * The debug level for the App.
     * 0 = no debug, 1 = basic debug, 2> = detailed debug
     */
    public static int debug = 0;
    /**
     * Wildcard patterns for entity names on which the visual-debug overlay is
     * rendered. {@code *} alone means "all entities". Configured via the
     * {@code app.debug.filter} key (CLI or config file).
     */
    public static List<String> debugFilter = List.of("*");
    /**
     * The current App mode.
     * Default is PRODUCTION.
     */
    private static AppMode mode = AppMode.DEVELOPMENT;

    public static boolean exit = false;
    public static boolean pause = false;

    private Dimension winDim;

    private List<Scene> scenes = new ArrayList<>();
    private Scene currentScene;
    private List<String> sceneClassNames = new ArrayList<>();
    private String defaultSceneClassName;

    /**
     * The input handler for the App, responsible for managing keyboard input
     */
    private InputHandler inputHandler;
    /**
     * The renderer for the App, responsible for managing the rendering of the
     * game window
     */
    private Renderer renderer;
    /**
     * The physics engine for the App, responsible for managing the physics
     * simulation of the game world
     */
    private PhysicsEngine physicsEngine;

    private static long cpt = 0;
    private static Random rand = new Random(67092);

    /**
     * Creates a new instance of the App.
     */
    public App() {
        info(
                App.class,
                "Create App '%s'",
                getI18n("app.main.name", "Application").formatted(getI18n("app.main.version", "0.0.1")));
    }

    /**
     * Runs the App.
     *
     * @param args the command-line arguments
     */
    public void run(String[] args) {
        info(
                App.class,
                "App '%s' is running..",
                getI18n("app.main.name", "Application").formatted(getI18n("app.main.version", "0.0.1")));
        for (String arg : args) {
            info(App.class, "- %s", arg);
        }
        init(args);
        loop();
        dispose();
    }

    /**
     * Initializes the App.
     *
     * @param args the command-line arguments
     */
    private void init(String[] args) {
        info(
                App.class,
                "App '%s' is initializing.",
                getI18n("app.main.name", "Application"));
        // default values
        config.put("app.config.file", "/config.properties");
        config.put("app.debug", 0);
        config.put("app.mode", AppMode.DEVELOPMENT);
        config.put("app.window.size", "800x600");
        // parsing arguments
        for (String arg : args) {
            String[] kv = arg.split("=", 2);
            if (kv.length == 2) {
                config.put(kv[0], kv[1]);
                info(
                        App.class,
                        "Set config from argument %s = %s",
                        kv[0],
                        kv[1]);
            } else {
                warn(App.class, "Invalid config argument: %s", arg);
            }
            info(App.class, "", arg);
        }
        // load configuration file
        try {
            config.load(
                    App.class.getResourceAsStream(
                            config.getProperty("app.config.file")));
        } catch (Exception e) {
            error(
                    App.class,
                    "Failed to load config file: %s",
                    e.getMessage());
        }
        // load scenes configuration file
        try {
            var scenesStream = App.class.getResourceAsStream("/scenes.properties");
            if (scenesStream != null) {
                config.load(scenesStream);
                info(App.class, "Loaded scenes configuration from scenes.properties");
            }
        } catch (Exception e) {
            warn(App.class, "Failed to load scenes.properties: %s", e.getMessage());
        }
        // re-apply command-line arguments so they override any property files
        for (String arg : args) {
            String[] kv = arg.split("=", 2);
            if (kv.length == 2) {
                config.put(kv[0], kv[1]);
            }
        }
        // extract configuration values
        parseConfiguration(config);
    }

    /**
     * Parses the App configuration.
     *
     * @param config the configuration properties
     */
    private void parseConfiguration(Properties config) {
        info(App.class, "Parsing configuration.");
        for (String key : config.stringPropertyNames()) {
            String value = config.getProperty(key);
            switch (key) {
                case "app.debug" -> {
                    debug = Integer.parseInt(value);
                    info(App.class, "read config '%s' = '%s'", key, value);
                }
                case "app.mode" -> {
                    mode = AppMode.valueOf(value.toUpperCase());
                    info(App.class, "read config '%s' = '%s'", key, value);
                }
                case "app.window.size" -> {
                    String[] keyVal = value.toLowerCase().split("x");
                    winDim = new Dimension(
                            Integer.parseInt(keyVal[0]),
                            Integer.parseInt(keyVal[1]));
                    info(App.class, "read config '%s' = '%s'", key, value);
                }
                case "app.scenes" -> {
                    sceneClassNames.clear();
                    for (String cls : value.split(",")) {
                        String s = cls.trim();
                        if (!s.isEmpty())
                            sceneClassNames.add(s);
                    }
                    info(App.class, "read config '%s' = '%s'", key, value);
                }
                case "app.scene.default" -> {
                    defaultSceneClassName = value.trim();
                    info(App.class, "read config '%s' = '%s'", key, value);
                }
                case "app.debug.filter" -> {
                    debugFilter = new ArrayList<>();
                    for (String p : value.split(",")) {
                        String trimmed = p.trim();
                        if (!trimmed.isEmpty())
                            debugFilter.add(trimmed);
                    }
                    if (debugFilter.isEmpty())
                        debugFilter = List.of("*");
                    info(App.class, "read config '%s' = '%s'", key, value);
                }
                default -> {
                    warn(
                            App.class,
                            "Unknown config '%s' = '%s'",
                            key,
                            value);
                }
            }
            info(App.class, "Config '%s' = '%s'", key, value);
        }
    }

    public void loop() {
        long startTime = 0,
                endTime = 0,
                elapsed = 0,
                gameTime = 0;
        long fps = 0,
                fpsFrame = 0,
                fpsTimeFrame = 0;
        Map<String, Object> stats = new ConcurrentHashMap<>();
        initialize();
        startTime = endTime = System.currentTimeMillis();
        do {
            startTime = endTime;
            if (!pause) {
                update(currentScene, stats, elapsed);
                gameTime += elapsed;
            }
            renderer.render(currentScene, stats, elapsed);

            sleep((1000 / 60) - elapsed > 0 ? (1000 / 60) - elapsed : 1);
            fpsTimeFrame += elapsed;
            fpsFrame++;
            if (fpsTimeFrame > 1000) {
                fps = fpsFrame;
                fpsTimeFrame = 0;
                fpsFrame = 0;
            }
            endTime = System.currentTimeMillis();
            elapsed = endTime - startTime;

            // store statistic metrics.
            stats.put("startTime", startTime);
            stats.put("endTime", endTime);
            stats.put("elapsed", elapsed);
            stats.put("gameTime", convertLongTimeToString(gameTime));
            stats.put("fps", fps);
        } while (!exit);
        dispose();
    }

    private void sleep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException e) {
            fatal(
                    this.getClass(),
                    -1,
                    "Unable to wait for %d ms : %s",
                    l,
                    e.getMessage());
        }
    }

    private void initialize() {
        inputHandler = new InputHandler(this);
        renderer = new Renderer(this);
        renderer.createWindow(this, winDim);
        inputHandler.attach(renderer.getWindow());
        physicsEngine = new PhysicsEngine();
        createScene();
    }

    private void createScene() {
        if (sceneClassNames.isEmpty()) {
            add(new DemoScene(this));
        } else {
            for (String className : sceneClassNames) {
                try {
                    Class<?> cls = Class.forName(className);
                    Scene scene = (Scene) cls.getDeclaredConstructor(App.class).newInstance(this);
                    add(scene);
                    info(App.class, "Loaded scene '%s'", className);
                } catch (Exception e) {
                    error(App.class, "Failed to load scene '%s': %s", className, e.getMessage());
                }
            }
            if (defaultSceneClassName != null && !defaultSceneClassName.isEmpty()) {
                scenes.stream()
                        .filter(s -> s.getClass().getName().equals(defaultSceneClassName))
                        .findFirst()
                        .ifPresent(s -> currentScene = s);
            }
        }
        if (currentScene != null) {
            currentScene.create(this);
        }
    }

    private void add(Scene scene) {
        this.scenes.add(scene);
        if (currentScene == null) {
            currentScene = scene;
        }
    }

    private void update(Scene scene, Map<String, Object> stats, long elapsed) {
        manageBoxesAnimation(scene, elapsed);
        if (physicsEngine != null && scene.getWorld() != null) {
            physicsEngine.update(scene.getWorld(), scene.getEntities(), elapsed);
        } else {
            scene.getEntities().stream().filter(Entity::isActive).forEach(e -> {
                updateEntity(e, elapsed);
            });
        }
        // Rebuild spatial index after all positions have been updated
        scene.rebuildSpatialIndex();
    }

    private void manageBoxesAnimation(Scene scene, long elapsed) {

        cpt += elapsed;
        if (cpt > 100) {
            scene.getEntities().stream().filter(e -> e.name.startsWith("box_")).forEach(e -> {
                e.vy = rand.nextFloat(-100f, 100f);
                e.vx = rand.nextFloat(-100f, 100f);
            });
            cpt = 0;
        }
    }

    private void updateEntity(Entity<?> e, long elapsed) {
        e.update(elapsed);
    }

    /**
     * Disposes the App resources.
     */
    private void dispose() {
        renderer.dispose();
        info(
                App.class,
                "App '%s' is ending.",
                i18Text.getString("app.main.name"));
    }

    /**
     * Returns the Renderer instance, allowing scenes to query window dimensions.
     */
    public Renderer getRenderer() {
        return renderer;
    }

    /**
     * The main entry point for the App.
     */
    public static void main(String[] args) {
        App App = new App();
        App.run(args);
    }

    /**
     * Logs a message with the specified log level.
     *
     * @param cls     the class logging the message
     * @param level   the log level
     * @param message the log message
     * @param args    optional arguments for the message
     */
    private static void log(
            Class<?> cls,
            String level,
            String message,
            Object... args) {
        System.out.printf(
                "%s;%s;%s;%s%n",
                ZonedDateTime.now(),
                cls.getCanonicalName(),
                level,
                message.formatted(args));
    }

    /**
     * Logs an informational message.
     *
     * @param cls     the class logging the message
     * @param message the log message
     * @param args    optional arguments for the message
     */
    public static void info(Class<?> cls, String message, Object... args) {
        log(cls, "INFO", message, args);
    }

    /**
     * Logs a warning message.
     *
     * @param cls     the class logging the message
     * @param message the log message
     * @param args    optional arguments for the message
     */
    public static void warn(Class<?> cls, String message, Object... args) {
        log(cls, "WARN", message, args);
    }

    /**
     * Logs a debug message.
     *
     * @param cls        the class logging the message
     * @param debugLevel the debug level
     * @param message    the log message
     * @param args       optional arguments for the message
     */
    public static void debug(
            Class<?> cls,
            int debugLevel,
            String message,
            Object... args) {
        if (isDebugGreaterThan(debugLevel) && mode == AppMode.DEVELOPMENT) {
            log(cls, "DEBUG", message, args);
        }
    }

    /**
     * Logs an error message.
     *
     * @param cls     the class logging the message
     * @param message the log message
     * @param args    optional arguments for the message
     */
    public static void error(Class<?> cls, String message, Object... args) {
        log(cls, "ERROR", message, args);
    }

    /**
     * Logs a fatal message and exit the Application with -1 status.
     *
     * @param cls        the class logging the message
     * @param exitStatus the status value return on execution stop.
     * @param message    the log message
     * @param args       optional arguments for the message
     */
    public static void fatal(
            Class<?> cls,
            int exitStatus,
            String message,
            Object... args) {
        log(cls, "FATAL", message, args);
        System.exit(exitStatus);
    }

    /**
     * Checks if the current debug level is greater than the specified level.
     *
     * @param level the debug level to compare against
     * @return true if the current debug level is greater, false otherwise
     */
    public static boolean isDebugGreaterThan(int level) {
        return debug > level;
    }

    /**
     * Returns {@code true} if {@code name} matches at least one pattern in
     * {@link #debugFilter}. Patterns use {@code *} as a wildcard (zero or more
     * characters). An empty filter or a filter containing {@code *} alone
     * matches everything.
     *
     * @param name the entity name to test
     * @return true if the entity should receive the visual-debug overlay
     */
    public static boolean matchesDebugFilter(String name) {
        if (name == null)
            return false;
        for (String pattern : debugFilter) {
            if ("*".equals(pattern))
                return true;
            // Split on '*', quote each part, join with '.*'
            String[] parts = pattern.split("\\*", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0)
                    sb.append(".*");
                sb.append(Pattern.quote(parts[i]));
            }
            if (Pattern.matches(sb.toString(), name))
                return true;
        }
        return false;
    }

    /*----- Utilities -----*/

    /**
     * Get internationalized text.
     *
     * @param key         the key of the text to retrieve
     * @param defaultText the default text to return if the key is not found
     * @return the internationalized text
     */
    public static String getI18n(String key, String defaultText) {
        String msg = i18Text.getString(key);
        if (msg == null)
            return defaultText;
        return msg;
    }

    /**
     * Convert a long time value to a string representation.
     *
     * @param time the time value to convert
     * @return the string representation of the time value
     */
    public static String convertLongTimeToString(long time) {
        long days = (time / (24 * 3600 * 1000));
        long hours = (time / (3600 * 1000)) % 3600;
        long mins = (time / (60 * 1000)) % 60;
        long secs = (time / (1000)) % 60;
        return "%d-%02d:%02d:%02d.%03d".formatted(
                days,
                hours,
                mins,
                secs,
                time % 1000);
    }

}
