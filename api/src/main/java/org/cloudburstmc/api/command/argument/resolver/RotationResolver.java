package org.cloudburstmc.api.command.argument.resolver;

/**
 * Resolves an absolute or relative rotation value.
 */
@FunctionalInterface
public interface RotationResolver {

    /**
     * Resolves the parsed value against an origin angle.
     *
     * @param origin the angle used for relative input
     * @return the resolved angle
     */
    float resolve(float origin);
}
