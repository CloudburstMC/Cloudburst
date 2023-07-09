package org.cloudburstmc.server.form.element;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;

@ToString
@Getter
public final class ElementDropdown extends Element {

    private final List<String> options = new ArrayList<>();
    @JsonProperty("default")
    private int defaultOptionIndex = 0;

    public ElementDropdown(@NonNull String elementText) {
        super(ElementType.DROPDOWN, elementText);
    }

    public ElementDropdown(@NonNull String elementText, @NonNull List<String> options) {
        super(ElementType.DROPDOWN, elementText);

        Preconditions.checkNotNull(options, "The provided dropdown options can not be null");
        this.options.addAll(options);
    }

    public ElementDropdown(@NonNull String elementText, @NonNull List<String> options, int defaultOptionIndex) {
        super(ElementType.DROPDOWN, elementText);

        Preconditions.checkNotNull(options, "The provided dropdown options can not be null");
        Preconditions.checkElementIndex(defaultOptionIndex, options.size(), "Default option index");

        this.options.addAll(options);
        this.defaultOptionIndex = defaultOptionIndex;
    }

    public String getDropdownOption(int index) {
        return this.options.get(index);
    }

    @RequiredArgsConstructor
    @Getter
    @ToString
    public static class Response {

        private final int index;
        private final String option;
    }
}
