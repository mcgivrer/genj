package  ${PACKAGE};


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JFrame;

/**
 * ${PROJECT_NAME} is a comprehensive Java-based game ${MAINCLASS} application framework that provides
 * a complete game loop, window management, input handling, and rendering system.
 *
 * <p>This ${MAINCLASS} class serves as the main entry point for creating
 * 2D games with features including:
 * <ul>
 *   <li>Configurable Application modes (Development/Production)</li>
 *   <li>Internationalization support</li>
 *   <li>Key input handling with event queuing</li>
 *   <li>Frame rate management and statistics tracking</li>
 *   <li>Comprehensive logging system</li>
 *   <li>Graphics utilities for text rendering</li>
 * </ul>
 *
 * <p>The Application follows a standard game loop pattern with initialization,
 * update, render, and cleanup phases. Configuration can be provided through
 * command-line arguments and property files.
 *
 * <p>Example usage:
 * <pre>{@code
 * ${MAINCLASS} ${MAINCLASS} = new ${MAINCLASS}();
 * ${MAINCLASS}.run(new String[]{"${MAINCLASS}.debug=1", "${MAINCLASS}.mode=DEVELOPMENT"});
 * }</pre>
 *
 * @since ${PROJECT_YEAR}
 * @author ${AUTHOR_NAME}<${AUTHOR_EMAIL}>
 * @version ${PROJECT_VERSION}
 */
public class ${MAINCLASS} implements KeyListener {

    /**
     * Enumeration representing the different operational modes of the ${MAINCLASS}.
     *
     * <p>These modes control the behavior and features available during ${MAINCLASS} execution:
     * <ul>
     *   <li><strong>DEVELOPMENT</strong>: Enables debug features, detailed logging, and development tools</li>
     *   <li><strong>PRODUCTION</strong>: Optimized mode with minimal logging for deployment environments</li>
     * </ul>
     *
     * <p>The mode affects logging verbosity, debug information display, and performance optimizations.
     * It can be configured through command-line arguments or configuration files.
     *
     * @see #debug
     * @see #config
     */
    public enum AppMode {
        /**
         * Development mode.
         * Used for debugging and development purposes.
         */
        DEVELOPMENT,
        /**
         * Production mode.
         * Used for running the Game${MAINCLASS} in a production environment.
         */
        PRODUCTION,
    }

    /**
     * Text alignment options for rendering text.
     * Used to specify horizontal alignment when drawing text.
     */
    public enum TextAlign {
        LEFT,
        CENTER,
        RIGHT,
    }

    /**
     * A circular queue implementation that extends LinkedList with a fixed capacity.
     * When the queue reaches its maximum capacity, adding a new element will automatically
     * remove the oldest element (first in the queue) to maintain the size limit.
     *
     * <p>This implementation is useful for maintaining a rolling buffer of recent items,
     * such as event history or logging where only the most recent N items need to be kept.
     *
     * <p>Example usage:
     * <pre>{@code
     * CircularQueue<String> queue = new CircularQueue<>(3);
     * queue.add("A"); // Queue: [A]
     * queue.add("B"); // Queue: [A, B]
     * queue.add("C"); // Queue: [A, B, C]
     * queue.add("D"); // Queue: [B, C, D] (A is removed)
     * }</pre>
     *
     * @param <E> the type of elements held in this queue
     */
    public class CircularQueue<E> extends LinkedList<E> {

        private final int capacity;

        public CircularQueue(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public boolean add(E e) {
            if (size() >= capacity) removeFirst();
            return super.add(e);
        }
    }

    /**
     * Represents a generic entity with properties such as position, velocity, size, color, and active state.
     * This class is intended to be a base model for visual and interactive objects in a graphical ${MAINCLASS}lication.
     *
     * @param <T> the specific type extending this Entity class, enabling fluent API configuration
     */
    public static class Entity<T> {
        public static long idx = 0;
        public long id = idx++;
        public String name = "entity_%d".formatted(id);

        public float x = 0.0f, y = 0.0f;
        public float vx = 0.0f, vy = 0.0f;

        public float mass = 1.0f;

        public int width = 0, height = 0;
        public Color color = Color.BLACK, fillColor = Color.BLUE;

        public List<Entity<?>> children = new ArrayList<>();

        public boolean active = true;

        public Entity() {
        }

        public Entity(String name) {
            this.name = name;
        }

        public T add(Entity<?> b) {
            children.add(b);
            return (T) this;
        }

        public T setPosition(float x, float y) {
            this.x = x;
            this.y = y;
            return (T) this;
        }

        public T setVelocity(float vx, float vy) {
            this.vx = vx;
            this.vy = vy;
            return (T) this;
        }

        public T setSize(int w, int h) {
            this.width = w;
            this.height = h;
            return (T) this;
        }

        public T setColor(Color c) {
            this.color = c;
            return (T) this;
        }

        public T setFillColor(Color fc) {
            this.fillColor = fc;
            return (T) this;
        }

        public T setActive(boolean a) {
            this.active = a;
            return (T) this;
        }

        public T setMass(float m) {
            this.mass = m;
            return (T) this;
        }

        public boolean isIntersect(Entity other) {
            return (x + width) - (other.x + width) > 0 && (y + height) - (other.y + other.height) > 0;
        }

        public boolean isActive() {
            return this.active;
        }

        public void update(long elapsed) {
            x += vx * (elapsed / 1000f);
            y += vy * (elapsed / 1000f);
        }

        public void draw(Graphics2D g) {
            if (fillColor != null) {
                g.setColor(fillColor);
                g.fillRect((int) (x + 0.5f), (int) (y + 0.5f), width, height);
            }
            if (color != null) {
                g.setColor(color);
                g.drawRect((int) (x + 0.5f), (int) (y + 0.5f), width, height);
            }
        }

    }

    /**
     * Represents a game object with properties such as position, velocity, size, color, and active state.
     * This class extends the Entity class and is intended to be a base model for visual and interactive game objects.
     */
    public static class GameObject extends Entity<GameObject> {
        public GameObject(String name) {
            super(name);
        }
    }

    public static class Colors {
        public static Color random() {
            return new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        }
    }

    /**
     * The resource bundle for internationalization.
     */
    public static ResourceBundle i18Text = ResourceBundle.getBundle(
            "i18n/messages"
    );
    /**
     * The Game${MAINCLASS} configuration properties.
     */
    public static Properties config = new Properties();
    /**
     * The debug level for the Game${MAINCLASS}.
     * 0 = no debug, 1 = basic debug, 2> = detailed debug
     */
    public static int debug = 0;
    /**
     * The current Game${MAINCLASS} mode.
     * Default is PRODUCTION.
     */
    public static AppMode mode = AppMode.DEVELOPMENT;

    public boolean exit = false;
    public boolean pause = false;

    public CircularQueue<KeyEvent> keyEvents = new CircularQueue<>(100);
    public static boolean keys[] = new boolean[1024];

    public Dimension winDim;
    public JFrame window;

    private List<Entity<?>> entities = new CopyOnWriteArrayList<>();
    private Map<String, Entity<?>> entitiesMap = new ConcurrentHashMap<>();

    private static long cpt = 0;
    private static Random rand = new Random(67092);

    /**
     * Creates a new instance of the Game${MAINCLASS}.
     */
    public ${MAINCLASS}() {
        info(
                ${MAINCLASS}.class,
                "Create ${MAINCLASS} '%s'",
                getI18n("${MAINCLASS}.name", "${MAINCLASS}lication")
        );
    }

    /**
     * Runs the ${MAINCLASS}.
     *
     * @param args the command-line arguments
     */
    public void run(String[] args) {
        info(
                ${MAINCLASS}.class,
                "Game${MAINCLASS} '%s' is running..",
                getI18n("${MAINCLASS}.name", "1.0.0")
        );
        for (String arg : args) {
            info(${MAINCLASS}.class, "- %s", arg);
        }
        init(args);
        loop();
        dispose();
    }

    /**
     * Initializes the Game${MAINCLASS}.
     *
     * @param args the command-line arguments
     */
    private void init(String[] args) {
        info(
                ${MAINCLASS}.class,
                "${MAINCLASS} '%s' is initializing.",
                getI18n("${MAINCLASS}.name", "${MAINCLASS}lication")
        );
        // default values
        config.put("${MAINCLASS}.config.file", "/config.properties");
        config.put("${MAINCLASS}.debug", 0);
        config.put("${MAINCLASS}.mode", AppMode.DEVELOPMENT);
        config.put("${MAINCLASS}.window.size", "800x600");
        // parsing arguments
        for (String arg : args) {
            String[] kv = arg.split("=", 2);
            if (kv.length == 2) {
                config.put(kv[0], kv[1]);
                info(
                        ${MAINCLASS}.class,
                        "Set config from argument %s = %s",
                        kv[0],
                        kv[1]
                );
            } else {
                warn(${MAINCLASS}.class, "Invalid config argument: %s", arg);
            }
            info(${MAINCLASS}.class, "", arg);
        }
        // load configuration file
        try {
            config.load(
                    ${MAINCLASS}.class.getResourceAsStream(
                            config.getProperty("${MAINCLASS}.config.file")
                    )
            );
        } catch (Exception e) {
            error(
                    ${MAINCLASS}.class,
                    "Failed to load config file: %s",
                    e.getMessage()
            );
        }
        // extract configuration values
        parseConfiguration(config);
    }

    /**
     * Parses the Game${MAINCLASS} configuration.
     *
     * @param config the configuration properties
     */
    private void parseConfiguration(Properties config) {
        info(${MAINCLASS}.class, "Parsing configuration.");
        for (String key : config.stringPropertyNames()) {
            String value = config.getProperty(key);
            switch (key) {
                case "${MAINCLASS}.debug" -> {
                    debug = Integer.parseInt(value);
                    info(${MAINCLASS}.class, "read config '%s' = '%s'", key, value);
                }
                case "${MAINCLASS}.mode" -> {
                    mode = AppMode.valueOf(value.toUpperCase());
                    info(${MAINCLASS}.class, "read config '%s' = '%s'", key, value);
                }
                case "${MAINCLASS}.window.size" -> {
                    String[] keyVal = value.toLowerCase().split("x");
                    winDim = new Dimension(
                            Integer.parseInt(keyVal[0]),
                            Integer.parseInt(keyVal[1])
                    );
                    info(${MAINCLASS}.class, "read config '%s' = '%s'", key, value);
                }
                default -> {
                    info(
                            ${MAINCLASS}.class,
                            "Unknown config '%s' = '%s'",
                            key,
                            value
                    );
                }
            }
            info(${MAINCLASS}.class, "Config '%s' = '%s'", key, value);
        }
    }

    public void add(Entity<?> e) {
        if (!entities.contains(e)) {
            entities.add(e);
            entitiesMap.putIfAbsent(e.name, e);
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
                update(stats, elapsed);
                gameTime += elapsed;
            }
            render(stats, elapsed);

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
                    e.getMessage()
            );
        }
    }

    public void initialize() {
        createWindow();
        createScene();
    }

    public void createWindow() {
        window = new JFrame(
                getI18n("${MAINCLASS}.name", "${MAINCLASS}lication").formatted(
                        getI18n("${MAINCLASS}.version", "1.0.0")
                )
        );
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setPreferredSize(winDim);
        window.addKeyListener(this);
        window.pack();
        window.setVisible(true);
        window.createBufferStrategy(3);
    }

    public void createScene() {
        // add a main object (the future player object)
        add(new GameObject("player")
                .setSize(24, 32)
                .setPosition(window.getWidth() * 0.5f, window.getHeight() * 0.5f)
                .setColor(Color.WHITE)
                .setFillColor(Color.BLUE));

        // and random squares
        Random rand = new Random(1234);
        for (int i = 0; i < 200; i++) {
            add(new GameObject("box_%s".formatted(i))
                    .setPosition(
                            rand.nextFloat(((int) (window.getWidth() - 8) / 8) * 8f),
                            rand.nextFloat(((int) (window.getHeight() - 8) / 8) * 8f))
                    .setSize(8, 8)
                    .setColor(Color.BLACK)
                    .setFillColor(Colors.random()));
        }
    }

    public void update(Map<String, Object> stats, long elapsed) {
        managePlayerInput();
        manageBoxesAnimation(elapsed);
        entities.stream().filter(Entity::isActive).forEach(e -> {
            updateEntity(e, elapsed);
        });
    }

    private void manageBoxesAnimation(long elapsed) {

        cpt += elapsed;
        if (cpt > 100) {
            entities.stream().filter(e -> e.name.startsWith("box_")).forEach(e -> {
                e.vy = rand.nextFloat(-100f, 100f);
                e.vx = rand.nextFloat(-100f, 100f);
            });
            cpt = 0;
        }
    }

    private void managePlayerInput() {
        if (isKeyPressed(KeyEvent.VK_UP)) {
            entitiesMap.get("player").vy = -100f;
        } else if (isKeyPressed(KeyEvent.VK_DOWN)) {
            entitiesMap.get("player").vy = 100f;
        }
        if (isKeyPressed(KeyEvent.VK_LEFT)) {
            entitiesMap.get("player").vx = -100f;
        } else if (isKeyPressed(KeyEvent.VK_RIGHT)) {
            entitiesMap.get("player").vx = 100f;
        }

        if (!isKeyPressed(KeyEvent.VK_UP) && !isKeyPressed(KeyEvent.VK_DOWN)) {
            entitiesMap.get("player").vy *= 0.95f;
        }
        if (!isKeyPressed(KeyEvent.VK_LEFT) && !isKeyPressed(KeyEvent.VK_RIGHT)) {
            entitiesMap.get("player").vx *= 0.95f;
        }
    }

    private void updateEntity(Entity<?> e, long elapsed) {
        e.update(elapsed);
    }

    public void render(Map<String, Object> stats, long elapsed) {
        // prepare drawing graphics API
        BufferStrategy bf = window.getBufferStrategy();
        Graphics2D g = (Graphics2D) bf.getDrawGraphics();
        // set drawing configuration.
        g.setRenderingHints(
                Map.of(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON,
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                )
        );
        // clear window
        g.setBackground(Color.BLACK);
        g.clearRect(0, 0, window.getWidth(), window.getHeight());

        // do you drawings
        // do you drawings
        entities.stream().filter(Entity::isActive).forEach(e -> drawEntity(g, e));

        drawText(
                g,
                getI18n("${MAINCLASS}.message.welcome", "Welcome into this demo"),
                (int) (window.getWidth() * 0.1),
                (int) (window.getHeight() * 0.1),
                TextAlign.LEFT,
                24.0f,
                Color.WHITE,
                Font.BOLD
        );
        drawText(
                g,
                getI18n("${MAINCLASS}.message.exit", "press ESCAPE to exit"),
                (int) (window.getWidth() * 0.95),
                (int) (window.getHeight() * .95),
                TextAlign.RIGHT,
                12.0f,
                Color.GRAY,
                Font.BOLD
        );

        if (debug > 0) {
            drawText(
                    g,
                    "[ dbg:%d | elapsed:%02d | time:%s | FPS:%03d | pause:%s ]".formatted(
                            debug,
                            stats.get("elapsed"),
                            stats.get("gameTime"),
                            stats.get("fps"),
                            pause ? "ON" : "OFF"
                    ),
                    30,
                    window.getHeight() - 40,
                    TextAlign.LEFT,
                    11.0f,
                    Color.ORANGE,
                    Font.PLAIN
            );
        }
        // switch buffer
        g.dispose();
        bf.show();
    }

    private void drawEntity(Graphics2D g, Entity<?> e) {
        e.draw(g);
    }

    /**
     * Disposes the Game${MAINCLASS} resources.
     */
    private void dispose() {
        if (Optional.ofNullable(window).isPresent()) {
            window.dispose();
        }
        info(
                ${MAINCLASS}.class,
                "Game${MAINCLASS} '%s' is ending.",
                i18Text.getString("${MAINCLASS}.name")
        );
    }

    /**
     * The main entry point for the Game${MAINCLASS}.
     */
    public static void main(String[] args) {
        ${MAINCLASS} ${MAINCLASS} = new ${MAINCLASS}();
        ${MAINCLASS}.run(args);
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
            Object... args
    ) {
        System.out.printf(
                "%s;%s;%s;%s%n",
                ZonedDateTime.now(),
                cls.getCanonicalName(),
                level,
                message.formatted(args)
        );
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
            Object... args
    ) {
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
     * Logs a fatal message and exit the ${MAINCLASS}lication with -1 status.
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
            Object... args
    ) {
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
                exit = true;
                debug(${MAINCLASS}.class, 1, "Exit state changed to %b", exit);
            }
            case KeyEvent.VK_PAUSE, KeyEvent.VK_P -> {
                pause = !pause;
                debug(${MAINCLASS}.class, 1, "Pause state changed to %b", pause);
            }
            case KeyEvent.VK_D -> {
                if (ke.isControlDown()) {
                    debug = (debug + 1) % 5;
                    info(${MAINCLASS}.class, "Debug level changed to %d", debug);
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

    /*----- Utilities -----*/

    /**
     * Draw text on the screen.
     *
     * @param g         the graphics context
     * @param text      the text to draw
     * @param x         the x coordinate of the text
     * @param y         the y coordinate of the text
     * @param align     the alignment of the text
     * @param fontSize  the font size of the text
     * @param c         the color of the text
     * @param fontStyle the font style of the text
     */
    public static void drawText(
            Graphics2D g,
            String text,
            int x,
            int y,
            TextAlign align,
            float fontSize,
            Color c,
            int fontStyle
    ) {
        Font font = g.getFont().deriveFont(fontStyle, fontSize);
        drawText(g, text, x, y, align, font, c);
    }

    /**
     * Draw text on the screen.
     *
     * @param g     the graphics context
     * @param text  the text to draw
     * @param x     the x coordinate of the text
     * @param y     the y coordinate of the text
     * @param align the alignment of the text
     * @param font  the font of the text
     * @param c     the color of the text
     */
    public static void drawText(
            Graphics2D g,
            String text,
            int x,
            int y,
            TextAlign align,
            Font font,
            Color c
    ) {
        g.setFont(font);
        g.setColor(c);
        int offsetX = align.equals(TextAlign.CENTER)
                ? (int) -(g.getFontMetrics().stringWidth(text) * 0.5)
                : align.equals(TextAlign.RIGHT)
                ? -g.getFontMetrics().stringWidth(text)
                : 0;
        g.drawString(text, x + offsetX, y);
    }

    /**
     * Get internationalized text.
     *
     * @param key         the key of the text to retrieve
     * @param defaultText the default text to return if the key is not found
     * @return the internationalized text
     */
    public static String getI18n(String key, String defaultText) {
        String msg = i18Text.getString(key);
        if (msg == null) return defaultText;
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
                time % 1000
        );
    }
}

