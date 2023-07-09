package org.cloudburstmc.server.form.element;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.server.form.util.ImageData;
import org.cloudburstmc.server.form.util.ImageType;

@ToString
@Getter
public final class ElementButton {

    @JsonIgnore
    private final ElementType elementType = ElementType.BUTTON;

    @JsonProperty("text")
    private final String buttonText;
    @JsonProperty("image")
    private final ImageData imageData;

    public ElementButton(@NonNull String buttonText) {
        Preconditions.checkNotNull(buttonText, "The provided button text can not be null");

        this.buttonText = buttonText;
        this.imageData = new ImageData();
    }

    public ElementButton(@NonNull String buttonText, @Nullable ImageType imageType, @Nullable String imageData) {
        Preconditions.checkNotNull(buttonText, "The provided button text can not be null");

        this.buttonText = buttonText;
        this.imageData = new ImageData(imageType, imageData);
    }
}
