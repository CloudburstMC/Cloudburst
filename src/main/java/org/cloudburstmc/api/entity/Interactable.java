package org.cloudburstmc.api.entity;

/**
 * @author Adam Matthew
 */
public interface Interactable {

    // Todo: Passive entity?? i18n and boat leaving text
    String getInteractButtonText();

    boolean canDoInteraction();

}
