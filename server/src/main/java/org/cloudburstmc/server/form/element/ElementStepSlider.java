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
public final class ElementStepSlider extends Element {

    private final List<String> steps = new ArrayList<>();
    @JsonProperty("default")
    private int defaultStepIndex = 0;

    public ElementStepSlider(@NonNull String elementText) {
        super(ElementType.STEP_SLIDER, elementText);
    }

    public ElementStepSlider(@NonNull String elementText, @NonNull List<String> steps) {
        super(ElementType.STEP_SLIDER, elementText);
        Preconditions.checkNotNull(steps, "The provided step options can not be null");

        this.steps.addAll(steps);
    }

    public ElementStepSlider(@NonNull String elementText, @NonNull List<String> steps, int defaultStepIndex) {
        super(ElementType.STEP_SLIDER, elementText);
        Preconditions.checkNotNull(steps, "The provided step options can not be null");

        this.steps.addAll(steps);
        this.defaultStepIndex = defaultStepIndex;
    }

    public String getStep(int index) {
        return steps.get(index);
    }

    @RequiredArgsConstructor
    @Getter
    @ToString
    public static class Response {

        private final int index;
        private final String option;
    }
}
