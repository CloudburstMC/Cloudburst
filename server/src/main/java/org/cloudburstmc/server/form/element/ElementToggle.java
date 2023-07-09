package org.cloudburstmc.server.form.element;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;

@ToString
public final class ElementToggle extends Element {

    @JsonProperty("default")
    private boolean defaultValue;

    public ElementToggle(@NonNull String elementText) {
        super(ElementType.TOGGLE, elementText);
    }

    public ElementToggle(@NonNull String elementText, boolean defaultValue) {
        super(ElementType.TOGGLE, elementText);
        this.defaultValue = defaultValue;
    }

    public boolean getDefaultValue() {
        return this.defaultValue;
    }
}
