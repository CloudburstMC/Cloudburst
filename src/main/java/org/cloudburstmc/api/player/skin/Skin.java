package org.cloudburstmc.api.player.skin;

import lombok.Data;
import lombok.ToString;
import org.cloudburstmc.api.player.skin.data.ImageData;
import org.cloudburstmc.api.player.skin.data.PersonaPiece;
import org.cloudburstmc.api.player.skin.data.PersonaPieceTint;
import org.cloudburstmc.api.player.skin.data.SkinAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ToString(exclude = {"geometryData", "animationData"})
@Data
public class Skin {

    private final String fullSkinId;
    private String skinId;
    private String playFabId;
    private String skinResourcePatch = GEOMETRY_CUSTOM;
    private ImageData skinData;
    private List<SkinAnimation> animations;
    private final List<PersonaPiece> personaPieces = new ArrayList<>();
    private final List<PersonaPieceTint> tintColors = new ArrayList<>();
    private ImageData capeData;
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

    public Skin(String fullSkinId) {
        this.fullSkinId = fullSkinId;
    }

    public Skin() {
        this(UUID.randomUUID().toString());
    }

    public boolean isValid() {
        return skinId != null && !skinId.trim().isEmpty() &&
                skinData != null && skinData.getWidth() >= 64 && skinData.getHeight() >= 32;
        //TODO geometry validation
    }
}
