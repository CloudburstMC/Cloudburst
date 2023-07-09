package org.cloudburstmc.server.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.server.form.element.*;
import org.cloudburstmc.server.form.response.CustomFormResponse;
import org.cloudburstmc.server.form.util.FormType;
import org.cloudburstmc.server.form.util.ImageData;
import org.cloudburstmc.server.form.util.ImageType;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
@ToString
public class CustomForm extends Form<CustomFormResponse> {

    @JsonProperty("content")
    private final List<Element> elements;

    private final ImageData icon;

    public CustomForm(
            String title,
            ImageData icon,
            List<Element> elements,
            List<BiConsumer<CloudPlayer, CustomFormResponse>> listeners,
            List<Consumer<CloudPlayer>> closeListeners,
            List<Consumer<CloudPlayer>> errorListeners
    ) {
        super(FormType.CUSTOM, title, listeners, closeListeners, errorListeners);
        this.icon = icon;
        this.elements = elements;
    }

    /**
     * @param index element index
     * @return element at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Element getElement(int index) {
        return elements.get(index);
    }

    @Override
    public void handleResponse(CloudPlayer p, JsonNode node) {
        if (!node.isArray()) {
            error(p);
            log.warn("Received invalid response for CustomForm {}", node);
            return;
        }

        submit(p, new CustomFormResponse(this, node));
    }

    public static class CustomFormBuilder extends FormBuilder<CustomForm, CustomFormBuilder, CustomFormResponse> {

        private final List<Element> elements = new ArrayList<>();

        private ImageData icon = null;

        /**
         * Add a dropdown element
         *
         * @param text    dropdown title
         * @param options dropdown options
         * @return self builder instance
         */
        public CustomFormBuilder dropdown(@NonNull String text, @NonNull String... options) {
            return dropdown(text, Arrays.asList(options));
        }

        /**
         * Add a dropdown element
         *
         * @param text    dropdown title
         * @param options dropdown options
         * @return self builder instance
         */
        public CustomFormBuilder dropdown(@NonNull String text, @NonNull List<String> options) {
            return element(new ElementDropdown(text, options));
        }

        /**
         * Add a dropdown element
         *
         * @param text          dropdown title
         * @param defaultOption default dropdown option index
         * @param options       dropdown options
         * @return self builder instance
         */
        public CustomFormBuilder dropdown(@NonNull String text, int defaultOption, @NonNull String... options) {
            return dropdown(text, defaultOption, Arrays.asList(options));
        }

        /**
         * Add a dropdown element
         *
         * @param text          dropdown title
         * @param defaultOption default dropdown option index
         * @param options       dropdown options
         * @return self builder instance
         */
        public CustomFormBuilder dropdown(@NonNull String text, int defaultOption, @NonNull List<String> options) {
            Preconditions.checkPositionIndex(defaultOption, options.size(), "Default option index out of bounds");
            return element(new ElementDropdown(text, options, defaultOption));
        }

        /**
         * Add an input element
         *
         * @param text input title
         * @return self builder instance
         */
        public CustomFormBuilder input(@NonNull String text) {
            return element(new ElementInput(text));
        }

        /**
         * Add an input element
         *
         * @param text        input title
         * @param placeholder placeholder text
         * @return self builder instance
         */
        public CustomFormBuilder input(@NonNull String text, @NonNull String placeholder) {
            return element(new ElementInput(text, placeholder));
        }

        /**
         * Add an input element
         *
         * @param text        input title
         * @param placeholder placeholder text
         * @param defaultText default input text
         * @return self builder instance
         */
        public CustomFormBuilder input(@NonNull String text, @NonNull String placeholder, @NonNull String defaultText) {
            return element(new ElementInput(text, placeholder, defaultText));
        }

        /**
         * Add a label element
         *
         * @param text label text
         * @return self builder instance
         */
        public CustomFormBuilder label(@NonNull String text) {
            return element(new ElementLabel(text));
        }

        /**
         * Add a slider element
         *
         * @param elementText slider title
         * @return self builder instance
         */
        public CustomFormBuilder slider(@NonNull String elementText) {
            return element(new ElementSlider(elementText));
        }

        /**
         * Add a slider element
         *
         * @param elementText slider title
         * @param minimum     minimal slider value
         * @param maximum     maximal slider value
         * @return self builder instance
         */
        public CustomFormBuilder slider(@NonNull String elementText, float minimum, float maximum) {
            return element(new ElementSlider(elementText, minimum, maximum));
        }

        /**
         * Add a slider element
         *
         * @param elementText slider title
         * @param minimum     minimal slider value
         * @param maximum     maximal slider value
         * @param stepCount   amount of steps in a given range
         * @return self builder instance
         */
        public CustomFormBuilder slider(@NonNull String elementText, float minimum, float maximum, int stepCount) {
            return element(new ElementSlider(elementText, minimum, maximum, stepCount));
        }

        /**
         * Add a slider element
         *
         * @param elementText  slider title
         * @param minimum      minimal slider value
         * @param maximum      maximal slider value
         * @param stepCount    amount of steps in a given range
         * @param defaultValue default slider value
         * @return self builder instance
         */
        public CustomFormBuilder slider(@NonNull String elementText, float minimum, float maximum, int stepCount, float defaultValue) {
            return element(new ElementSlider(elementText, minimum, maximum, stepCount, defaultValue));
        }

        /**
         * Add a step slider element
         *
         * @param elementText step slider title
         * @return self builder instance
         */
        public CustomFormBuilder stepSlider(@NonNull String elementText) {
            return element(new ElementStepSlider(elementText));
        }

        /**
         * Add a step slider element
         *
         * @param elementText step slider title
         * @param stepOptions list of all available steps
         * @return self builder instance
         */
        public CustomFormBuilder stepSlider(@NonNull String elementText, @NonNull String... stepOptions) {
            return stepSlider(elementText, Arrays.asList(stepOptions));
        }

        /**
         * Add a step slider element
         *
         * @param elementText step slider title
         * @param stepOptions list of all available steps
         * @return self builder instance
         */
        public CustomFormBuilder stepSlider(@NonNull String elementText, @NonNull List<String> stepOptions) {
            return element(new ElementStepSlider(elementText, stepOptions));
        }

        /**
         * Add a step slider element
         *
         * @param elementText      step slider title
         * @param defaultStepIndex step slider default option index
         * @param stepOptions      list of all available steps
         * @return self builder instance
         */
        public CustomFormBuilder stepSlider(@NonNull String elementText, int defaultStepIndex, @NonNull String... stepOptions) {
            return stepSlider(elementText, defaultStepIndex, Arrays.asList(stepOptions));
        }

        /**
         * Add a step slider element
         *
         * @param elementText      step slider title
         * @param defaultStepIndex step slider default option index
         * @param stepOptions      list of all available steps
         * @return self builder instance
         */
        public CustomFormBuilder stepSlider(@NonNull String elementText, int defaultStepIndex, @NonNull List<String> stepOptions) {
            return element(new ElementStepSlider(elementText, stepOptions, defaultStepIndex));
        }

        /**
         * Add a toggle element
         *
         * @param elementText toggle title
         * @return self builder instance
         */
        public CustomFormBuilder toggle(@NonNull String elementText) {
            return element(new ElementToggle(elementText));
        }

        /**
         * Add a toggle element
         *
         * @param elementText  toggle title
         * @param defaultValue default toggle value
         * @return self builder instance
         */
        public CustomFormBuilder toggle(@NonNull String elementText, boolean defaultValue) {
            return element(new ElementToggle(elementText, defaultValue));
        }

        /**
         * Add an element
         *
         * @param element an element to be added
         * @return self builder instance
         */
        public CustomFormBuilder element(@NonNull Element element) {
            elements.add(element);
            return this;
        }

        /**
         * Add one or more elements
         *
         * @param element  an element to be added
         * @param elements list of elements to be added
         * @return self builder instance
         */
        public CustomFormBuilder elements(@NonNull Element element, @NonNull Element... elements) {
            this.elements.add(element);
            this.elements.addAll(Arrays.asList(elements));
            return this;
        }

        /**
         * Add list of elements
         *
         * @param elements list of elements to be added
         * @return self builder instance
         */
        public CustomFormBuilder elements(@NonNull Collection<Element> elements) {
            this.elements.addAll(elements);
            return this;
        }

        /**
         * Set an icon of the form
         * The icon is visible only in case of server settings form
         *
         * @param imageType icon image type
         * @param imageData icon image data
         * @return self builder instance
         */
        public CustomFormBuilder icon(@NonNull ImageType imageType, @NonNull String imageData) {
            this.icon = new ImageData(imageType, imageData);
            return this;
        }

        /**
         * Builds a new CustomForm instance using builder values
         *
         * @return CustomForm instance
         */
        @Override
        public CustomForm build() {
            return new CustomForm(title, icon, Collections.unmodifiableList(elements), listeners, closeListeners, errorListeners);
        }

        @Override
        protected CustomFormBuilder self() {
            return this;
        }
    }
}
