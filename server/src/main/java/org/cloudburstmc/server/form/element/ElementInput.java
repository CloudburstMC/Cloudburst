package org.cloudburstmc.server.form.element;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@ToString
@Getter
public final class ElementInput extends Element {

    private String placeholder;
    @JsonProperty("default")
    private String defaultText;

    public ElementInput(@NonNull String elementText) {
        super(ElementType.INPUT, elementText);
    }

    public ElementInput(@NonNull String elementText, @Nullable String placeholder) {
        super(ElementType.INPUT, elementText);
        this.placeholder = placeholder;
    }

    public ElementInput(@NonNull String elementText, @Nullable String placeholder, @Nullable String defaultText) {
        super(ElementType.INPUT, elementText);
        this.placeholder = placeholder;
        this.defaultText = defaultText;
    }
}
