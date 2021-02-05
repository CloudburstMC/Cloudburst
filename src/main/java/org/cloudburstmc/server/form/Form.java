package org.cloudburstmc.server.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cloudburstmc.server.form.CustomForm.CustomFormBuilder;
import org.cloudburstmc.server.form.ModalForm.ModalFormBuilder;
import org.cloudburstmc.server.form.SimpleForm.SimpleFormBuilder;
import org.cloudburstmc.server.form.util.FormType;
import org.cloudburstmc.server.player.CloudPlayer;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Getter
@ToString(exclude = {"listeners", "closeListeners", "errorListeners"})
public abstract class Form<R> {

    protected static final Logger log = LogManager.getLogger(Form.class);

    private final FormType type;
    private final String title;

    @JsonIgnore
    private final List<BiConsumer<CloudPlayer, R>> listeners;
    @JsonIgnore
    private final List<Consumer<CloudPlayer>> closeListeners;
    @JsonIgnore
    private final List<Consumer<CloudPlayer>> errorListeners;

    /**
     * Use this method to build a new simple form
     *
     * @return new SimpleFormBuilder instance
     */
    public static SimpleFormBuilder simple() {
        return new SimpleFormBuilder();
    }

    /**
     * Use this method to build a new modal form
     *
     * @return new ModalFormBuilder instance
     */
    public static ModalFormBuilder modal() {
        return new ModalFormBuilder();
    }

    /**
     * Use this method to build a new custom form
     *
     * @return new CustomFormBuilder instance
     */
    public static CustomFormBuilder custom() {
        return new CustomFormBuilder();
    }

    public abstract void handleResponse(CloudPlayer p, JsonNode node);

    public void close(CloudPlayer p) {
        for (Consumer<CloudPlayer> closeListener : closeListeners) {
            closeListener.accept(p);
        }
    }

    public void submit(CloudPlayer p, R response) {
        if (response == null) {
            close(p);
            return;
        }

        for (BiConsumer<CloudPlayer, R> listener : listeners) {
            listener.accept(p, response);
        }
    }

    public void error(CloudPlayer p) {
        for (Consumer<CloudPlayer> errorListener : errorListeners) {
            errorListener.accept(p);
        }
    }

    public static abstract class FormBuilder<F extends Form<R>, T extends FormBuilder<F, T, R>, R> {

        protected String title = "";

        protected final List<BiConsumer<CloudPlayer, R>> listeners = new LinkedList<>();
        protected final List<Consumer<CloudPlayer>> closeListeners = new LinkedList<>();
        protected final List<Consumer<CloudPlayer>> errorListeners = new LinkedList<>();

        /**
         * Set a title of the form
         *
         * @param title form title
         * @return self builder instance
         */
        public T title(@Nonnull String title) {
            this.title = title;
            return self();
        }

        /**
         * Called when the form is successfully submitted
         *
         * @param listener callback function
         * @return builder instance
         */
        public T onSubmit(@Nonnull BiConsumer<CloudPlayer, R> listener) {
            this.listeners.add(listener);
            return self();
        }

        /**
         * Called when the form is closed
         *
         * @param listener callback function
         * @return builder instance
         */
        public T onClose(@Nonnull Consumer<CloudPlayer> listener) {
            this.closeListeners.add(listener);
            return self();
        }

        /**
         * Called when an error occurs during the response processing
         * That could be caused either by a plugin or wrong response (which shouldn't occur in case of vanilla client)
         *
         * @param listener callback function
         * @return builder instance
         */
        public T onError(@Nonnull Consumer<CloudPlayer> listener) {
            this.errorListeners.add(listener);
            return self();
        }

        /**
         * @return a new Form instance of given generic type
         */
        public abstract F build();

        protected abstract T self();
    }
}
