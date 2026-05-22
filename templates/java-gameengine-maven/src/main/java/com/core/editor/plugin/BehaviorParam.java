package com.core.editor.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor parameter of a {@link com.core.behavior.Behavior}
 * implementation as an editable field in the Level Editor's Behaviors panel.
 *
 * <p>The Level Editor reads this annotation at runtime via reflection to build
 * a dynamic form for each behavior.  If a constructor parameter is <em>not</em>
 * annotated, the editor falls back to reflection (using the parameter name from
 * the debug information, if available).</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class RainBehavior extends ParticleEmitterBehavior {
 *
 *     public RainBehavior(
 *             @BehaviorParam(name = "emitRate",   description = "Particles/s",       min = "1",    max = "500") float emitRate,
 *             @BehaviorParam(name = "windX",      description = "Wind X (px/s²)",    min = "-300", max = "300") float windX,
 *             @BehaviorParam(name = "gravityFact",description = "Gravity factor",    min = "0",    max = "2")   float gravityFactor) {
 *         super(buildConfig(emitRate, windX, gravityFactor));
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface BehaviorParam {

    /** Human-readable label shown in the editor form. */
    String name();

    /** Optional tooltip / description shown next to the field. */
    String description() default "";

    /** Optional minimum value hint (parsed as {@code float} by the editor). */
    String min() default "";

    /** Optional maximum value hint (parsed as {@code float} by the editor). */
    String max() default "";
}
