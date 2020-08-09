package org.cloudburstmc.server.entity;

/**
 * @author Adam Matthew
 */
public interface EntityInteractable {

    // Todo: Passive entity?? i18n and boat leaving text
    String getInteractButtonText();

    boolean canDoInteraction();

}
