package org.cloudburstmc.api.locale;

import lombok.extern.slf4j.Slf4j;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Slf4j
public class TextContainer implements Cloneable {
    protected String text;

    public TextContainer(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.getText();
    }

    @Override
    public TextContainer clone() {
        try {
            return (TextContainer) super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("TextContainer Clone Error", e);
        }
        return null;
    }
}
