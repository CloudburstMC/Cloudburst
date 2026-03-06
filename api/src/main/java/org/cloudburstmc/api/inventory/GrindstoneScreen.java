package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.GrindstoneView;

/**
 * Represents the grindstone container screen.
 *
 * <p>The grindstone has two input slots and one output slot:</p>
 * <ul>
 *   <li>{@link GrindstoneView#getInput()}: the primary (left) item input slot</li>
 *   <li>{@link GrindstoneView#getAdditional()}: the secondary (right) item input slot</li>
 *   <li>{@link GrindstoneView#getResult()}: the result output slot</li>
 * </ul>
 */
public interface GrindstoneScreen extends ContainerScreen {

    GrindstoneView getGrindstone();
}
