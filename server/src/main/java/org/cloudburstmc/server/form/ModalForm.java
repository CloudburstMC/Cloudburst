package org.cloudburstmc.server.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.server.form.util.FormType;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
@ToString
public class ModalForm extends Form<Boolean> {

    @JsonProperty("button1")
    private final String trueValue;
    @JsonProperty("button2")
    private final String falseValue;
    private final String content;

    public ModalForm(
            String title,
            String content,
            String trueValue,
            String falseValue,
            List<BiConsumer<CloudPlayer, Boolean>> listeners,
            List<Consumer<CloudPlayer>> closeListeners,
            List<Consumer<CloudPlayer>> errorListeners
    ) {
        super(FormType.MODAL, title, listeners, closeListeners, errorListeners);
        this.content = content;
        this.trueValue = trueValue;
        this.falseValue = falseValue;
    }

    @Override
    public void handleResponse(CloudPlayer p, JsonNode node) {
        if (!node.isBoolean()) {
            error(p);
            log.warn("Received invalid response for ModalForm {}", node);
            return;
        }

        submit(p, node.booleanValue());
    }

    public static class ModalFormBuilder extends FormBuilder<ModalForm, ModalFormBuilder, Boolean> {

        private String content = "";
        private String trueValue = "true";
        private String falseValue = "false";

        /**
         * Set the form text content
         *
         * @param content form text content
         * @return self builder instance
         */
        public ModalFormBuilder content(@NonNull String content) {
            this.content = content;
            return this;
        }

        /**
         * Set a displayed value for true boolean value
         *
         * @param value string value for {@link Boolean#TRUE}
         * @return self builder instance
         */
        public ModalFormBuilder trueValue(@NonNull String value) {
            this.trueValue = value;
            return this;
        }

        /**
         * Set a displayed value for false boolean value
         *
         * @param value string value for {@link Boolean#FALSE}
         * @return self builder instance
         */
        public ModalFormBuilder falseValue(@NonNull String value) {
            this.falseValue = value;
            return this;
        }

        /**
         * Builds a new ModalForm instance using builder values
         *
         * @return ModalForm instance
         */
        @Override
        public ModalForm build() {
            return new ModalForm(title, content, trueValue, falseValue, listeners, closeListeners, errorListeners);
        }

        @Override
        protected ModalFormBuilder self() {
            return this;
        }
    }
}
