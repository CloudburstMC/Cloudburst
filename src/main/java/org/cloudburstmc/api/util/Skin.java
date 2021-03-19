package org.cloudburstmc.api.util;

import com.google.common.collect.ImmutableList;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ToString(exclude = {"geometryData", "animationData"})
@NoArgsConstructor
@Data
public class Skin {

    private final String fullSkinId = UUID.randomUUID().toString();
    private String skinId;
    private String skinResourcePatch = GEOMETRY_CUSTOM;
    private BufferedImage skinData;
    private final List<SkinAnimation> animations = new ArrayList<>();
    private final List<PersonaPiece> personaPieces = new ArrayList<>();
    private final List<PersonaPieceTint> tintColors = new ArrayList<>();
    private BufferedImage capeData;
    private String geometryData;
    private String animationData;
    private boolean premium;
    private boolean persona;
    private boolean capeOnClassic;
    private String capeId;
    private String skinColor = "#0";
    private String armSize = "wide";
    private boolean trusted = false;

    public static final String GEOMETRY_CUSTOM = convertLegacyGeometryName("geometry.humanoid.custom");
    public static final String GEOMETRY_CUSTOM_SLIM = convertLegacyGeometryName("geometry.humanoid.customSlim");

    private static String convertLegacyGeometryName(String geometryName) {
        return "{\"geometry\" : {\"default\" : \"" + geometryName + "\"}}";
    }

    public boolean isValid() {
        return true; //TODO
    }

    @Data
    public class PersonaPieceTint {
        private final String pieceType;
        private final ImmutableList<String> colors;

        public PersonaPieceTint(String pieceType, List<String> colors) {
            this.pieceType = pieceType;
            this.colors = ImmutableList.copyOf(colors);
        }
    }

    @Data
    public class PersonaPiece {
        private final String id;
        private final String type;
        private final String packId;
        private final boolean isDefault;
        private final String productId;
    }

    @Data
    public class SkinAnimation {
        private final BufferedImage image;
        private final int type;
        private final float frames;
        private final int expression;
    }
}
