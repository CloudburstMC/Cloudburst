package org.cloudburstmc.api.container;

import lombok.Getter;
import org.cloudburstmc.api.container.screen.ContainerScreen;
import org.cloudburstmc.api.util.Identifier;

import java.util.Objects;

@Getter
public class ContainerScreenType<T extends ContainerScreen> {

    private final Identifier identifier;
    private final Class<T> screenClass;

    private ContainerScreenType(Identifier identifier, Class<T> screenClass) {
        this.identifier = identifier;
        this.screenClass = screenClass;
    }

    public static <T extends ContainerScreen> ContainerScreenType from(Identifier identifier, Class<T> screenClass) {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(screenClass, "screenClass");
        if (!ContainerScreen.class.isAssignableFrom(screenClass)) {
            throw new IllegalArgumentException("screenClass must be a subclass of ContainerScreen");
        }
        return new ContainerScreenType(identifier, screenClass);
    }
}
