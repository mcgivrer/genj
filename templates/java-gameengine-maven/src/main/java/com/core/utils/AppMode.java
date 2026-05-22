package com.core.utils;

/**
 * Enumeration representing the different operational modes of the DemoApp.
 *
 * <p>
 * These modes control the behavior and features available during DemoApp
 * execution:
 * <ul>
 * <li><strong>DEVELOPMENT</strong>: Enables debug features, detailed logging,
 * and development tools</li>
 * <li><strong>PRODUCTION</strong>: Optimized mode with minimal logging for
 * deployment environments</li>
 * </ul>
 *
 * <p>
 * The mode affects logging verbosity, debug information display, and
 * performance optimizations.
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
     * Used for running the DemoApp in a production environment.
     */
    PRODUCTION,
}