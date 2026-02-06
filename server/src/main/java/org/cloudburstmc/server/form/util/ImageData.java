package org.cloudburstmc.server.form.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

@JsonSerialize(using = ImageData.ImageDataSerializer.class)
@ToString
public final class ImageData {

    @JsonProperty("type")
    private final ImageType imageType;
    @JsonProperty("data")
    private final String imageData;

    public ImageData() {
        this.imageData = null;
        this.imageType = null;
    }

    public ImageData(@Nullable ImageType imageType, @Nullable String imageData) {
        this.imageType = imageType;
        this.imageData = imageData;
    }

    @Nullable
    public ImageType getImageType() {
        return this.imageType;
    }

    @Nullable
    public String getImageData() {
        return this.imageData;
    }

    static final class ImageDataSerializer extends ValueSerializer<ImageData> {

        @Override
        public void serialize(ImageData imageData, JsonGenerator jsonGenerator, SerializationContext serializationContext) throws JacksonException {
            if (imageData.getImageData() == null || imageData.getImageData().isEmpty() || imageData.getImageType() == null) {
                jsonGenerator.writeNull();
                return;
            }

            jsonGenerator.writeStartObject();
            jsonGenerator.writePOJOProperty("type", imageData.getImageType());
            jsonGenerator.writeStringProperty("data", imageData.getImageData());
            jsonGenerator.writeEndObject();
        }
    }

}
