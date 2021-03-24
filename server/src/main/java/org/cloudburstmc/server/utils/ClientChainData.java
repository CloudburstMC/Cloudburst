package org.cloudburstmc.server.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nukkitx.protocol.bedrock.data.skin.AnimatedTextureType;
import com.nukkitx.protocol.bedrock.data.skin.AnimationData;
import com.nukkitx.protocol.bedrock.data.skin.AnimationExpressionType;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minidev.json.JSONObject;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.player.skin.data.ImageData;
import org.cloudburstmc.api.player.skin.data.PersonaPiece;
import org.cloudburstmc.api.player.skin.data.PersonaPieceTint;
import org.cloudburstmc.api.player.skin.data.SkinAnimation;
import org.cloudburstmc.api.util.LoginChainData;
import org.cloudburstmc.server.Bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * ClientChainData is a container of chain data sent from clients.
 * <p>
 * Device information such as client UUID, xuid and serverAddress, can be
 * read from instances of this object.
 * <p>
 * To get chain data, you can use player.getLoginChainData() or read(loginPacket)
 * <p>
 * ===============
 * author: boybook
 * Nukkit Project
 * ===============
 */
public final class ClientChainData implements LoginChainData {
    private static final String MOJANG_PUBLIC_KEY_BASE64 =
            "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V";
    private static final PublicKey MOJANG_PUBLIC_KEY;

    private static final TypeReference<Map<String, List<String>>> MAP_TYPE_REFERENCE =
            new TypeReference<Map<String, List<String>>>() {
            };

    static {
        try {
            MOJANG_PUBLIC_KEY = generateKey(MOJANG_PUBLIC_KEY_BASE64);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private final String chainData;
    private final String skinData;
    private SerializedSkin serializedSkin;
    private Skin skin;

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public UUID getClientUUID() {
        return clientUUID;
    }

    @Override
    public String getIdentityPublicKey() {
        return identityPublicKey;
    }

    @Override
    public long getClientId() {
        return clientId;
    }

    @Override
    public String getServerAddress() {
        return serverAddress;
    }

    @Override
    public String getDeviceModel() {
        return deviceModel;
    }

    @Override
    public int getDeviceOS() {
        return deviceOS;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public String getGameVersion() {
        return gameVersion;
    }

    @Override
    public int getGuiScale() {
        return guiScale;
    }

    @Override
    public String getLanguageCode() {
        return languageCode;
    }

    @Override
    public String getXUID() {
        return xuid;
    }

    private boolean xboxAuthed;

    @Override
    public int getCurrentInputMode() {
        return currentInputMode;
    }

    @Override
    public int getDefaultInputMode() {
        return defaultInputMode;
    }

    private ClientChainData(String chainData, String skinData) {
        this.chainData = chainData;
        this.skinData = skinData;
        decodeChainData(chainData);
        decodeSkinData(skinData);
    }

    public final static int UI_PROFILE_CLASSIC = 0;
    public final static int UI_PROFILE_POCKET = 1;

    @Override
    public int getUIProfile() {
        return UIProfile;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Override
    ///////////////////////////////////////////////////////////////////////////

    public static ClientChainData of(byte[] buffer) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
        return new ClientChainData(readString(byteBuf), readString(byteBuf));
    }

    public static ClientChainData read(LoginPacket pk) {
        return new ClientChainData(pk.getChainData().toString(), pk.getSkinData().toString());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Internal
    ///////////////////////////////////////////////////////////////////////////

    private String username;
    private UUID clientUUID;
    private String xuid;

    private static PublicKey generateKey(String base64) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }

    private String identityPublicKey;

    private long clientId;
    private String serverAddress;
    private String deviceModel;
    private int deviceOS;
    private String deviceId;
    private String gameVersion;
    private int guiScale;
    private String languageCode;
    private int currentInputMode;
    private int defaultInputMode;

    private int UIProfile;

    private static String readString(ByteBuf buffer) {
        int length = buffer.readIntLE();
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.US_ASCII); // base 64 encoded.
    }

    private static Skin getSkin(JsonNode skinToken) {
        Skin newSkin = new Skin();

        if (skinToken.has("SkinId")) {
            newSkin.setSkinId(skinToken.get("SkinId").textValue());
        }
        if (skinToken.has("PlayFabId")) {
            newSkin.setPlayFabId(skinToken.get("PlayFabId").textValue());
        }
        if (skinToken.has("CapeId")) {
            newSkin.setCapeId(skinToken.get("CapeId").textValue());
        }

        newSkin.setSkinData(getImage(skinToken, "Skin"));
        newSkin.setCapeData(getImage(skinToken, "Cape"));
        if (skinToken.has("PremiumSkin")) {
            newSkin.setPremium(skinToken.get("PremiumSkin").booleanValue());
        }
        if (skinToken.has("PersonaSkin")) {
            newSkin.setPersona(skinToken.get("PersonaSkin").booleanValue());
            if (skinToken.has("PersonaPieces")) {
                for (JsonNode piece : skinToken.get("PersonaPieces")) {
                    newSkin.getPersonaPieces().add(new PersonaPiece(
                            piece.get("PieceId").textValue(),
                            piece.get("PieceType").textValue(),
                            piece.get("PackId").textValue(),
                            piece.get("IsDefault").booleanValue(),
                            piece.get("ProductId").textValue()
                    ));
                }
            }
            if (skinToken.has("PieceTintColors")) {
                for (JsonNode node : skinToken.get("PieceTintColors")) {
                    List<String> colors = new ArrayList<>();
                    for (JsonNode color : node.get("Colors")) {
                        colors.add(color.textValue());
                    }
                    newSkin.getTintColors().add(new PersonaPieceTint(
                            node.get("PieceType").textValue(),
                            colors
                    ));
                }
            }
        }
        if (skinToken.has("CapeOnClassicSkin")) {
            newSkin.setCapeOnClassic(skinToken.get("CapeOnClassicSkin").booleanValue());
        }

        if (skinToken.has("SkinResourcePatch")) {
            newSkin.setSkinResourcePatch(new String(Base64.getDecoder().decode(skinToken.get("SkinResourcePatch").textValue()), StandardCharsets.UTF_8));
        }

        if (skinToken.has("SkinGeometryData")) {
            newSkin.setGeometryData(new String(Base64.getDecoder().decode(skinToken.get("SkinGeometryData").textValue()), StandardCharsets.UTF_8));
        }

        if (skinToken.has("SkinAnimationData")) {
            newSkin.setAnimationData(new String(Base64.getDecoder().decode(skinToken.get("SkinAnimationData").textValue()), StandardCharsets.UTF_8));
        }

        if (skinToken.has("AnimatedImageData")) {
            List<SkinAnimation> animations = new ArrayList<>();
            JsonNode array = skinToken.get("AnimatedImageData");
            for (JsonNode element : array) {
                animations.add(getAnimation(element));
            }
            newSkin.setAnimations(animations);
        }
        return newSkin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        ClientChainData that = (ClientChainData) obj;
        return Objects.equals(this.skinData, that.skinData) && Objects.equals(this.chainData, that.chainData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skinData, chainData);
    }

    @Override
    public boolean isXboxAuthed() {
        return xboxAuthed;
    }

    private static boolean verify(PublicKey key, JWSObject object) throws JOSEException {
        JWSVerifier verifier = new DefaultJWSVerifierFactory().createJWSVerifier(object.getHeader(), key);
        return object.verify(verifier);
    }

    private JsonNode decodeToken(String token) {
        String[] base = token.split("\\.");
        if (base.length < 2) throw new IllegalArgumentException("Invalid token length");
        String json = new String(Base64.getDecoder().decode(base[1]), StandardCharsets.UTF_8);
        //Server.getInstance().getLogger().debug(json);
        try {
            return Bootstrap.JSON_MAPPER.readTree(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid token JSON", e);
        }
    }

    private void decodeChainData(String chainData) {
        Map<String, List<String>> map;
        try {
            map = Bootstrap.JSON_MAPPER.readValue(chainData, MAP_TYPE_REFERENCE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON", e);
        }
        if (map.isEmpty() || !map.containsKey("chain") || map.get("chain").isEmpty()) return;
        List<String> chains = map.get("chain");

        // Validate keys
        try {
            xboxAuthed = verifyChain(chains);
        } catch (Exception e) {
            xboxAuthed = false;
        }

        for (String c : chains) {
            JsonNode chainMap = decodeToken(c);
            if (chainMap == null) continue;
            if (chainMap.has("extraData")) {
                JsonNode extra = chainMap.get("extraData");
                if (extra.has("displayName")) this.username = extra.get("displayName").textValue();
                if (extra.has("identity")) this.clientUUID = UUID.fromString(extra.get("identity").textValue());
                if (extra.has("XUID")) this.xuid = extra.get("XUID").textValue();
            }
            if (chainMap.has("identityPublicKey"))
                this.identityPublicKey = chainMap.get("identityPublicKey").textValue();
        }

        if (!xboxAuthed) {
            xuid = null;
        }
    }

    private boolean verifyChain(List<String> chains) throws Exception {

        PublicKey lastKey = null;
        boolean mojangKeyVerified = false;
        for (String chain : chains) {
            JWSObject jws = JWSObject.parse(chain);

            if (!mojangKeyVerified) {
                // First chain should be signed using Mojang's private key. We'd be in big trouble if it leaked...
                mojangKeyVerified = verify(MOJANG_PUBLIC_KEY, jws);
            }

            if (lastKey != null) {
                if (!verify(lastKey, jws)) {
                    throw new JOSEException("Unable to verify key in chain.");
                }
            }

            JSONObject payload = jws.getPayload().toJSONObject();
            String base64key = payload.getAsString("identityPublicKey");
            if (base64key == null) {
                throw new RuntimeException("No key found");
            }
            lastKey = generateKey(base64key);
        }
        return mojangKeyVerified;
    }

    private static SkinAnimation getAnimation(JsonNode element) {
        float frames = element.get("Frames").floatValue();
        int type = element.get("Type").intValue();
        byte[] data = Base64.getDecoder().decode(element.get("Image").textValue());
        int width = element.get("ImageWidth").intValue();
        int height = element.get("ImageHeight").intValue();
        int expression = 0;
        if (element.hasNonNull("ExpressionType")) {
            expression = element.get("ExpressionType").intValue();
        }
        return new SkinAnimation(new ImageData(width, height, data), type, frames, expression);
    }

    private static ImageData getImage(JsonNode token, String name) {
        if (token.has(name + "Data")) {
            byte[] skinImage = Base64.getDecoder().decode(token.get(name + "Data").textValue());
            if (token.has(name + "ImageHeight") && token.has(name + "ImageWidth")) {
                int width = token.get(name + "ImageWidth").intValue();
                int height = token.get(name + "ImageHeight").intValue();
                return new ImageData(width, height, skinImage);
            } else {
                switch (skinImage.length / 4) {
                    case 2048:
                        return new ImageData(64, 32, skinImage);
                    case 4096:
                        return new ImageData(64, 64, skinImage);
                    case 8192:
                        return new ImageData(128, 64, skinImage);
                    case 16384:
                        return new ImageData(128, 128, skinImage);
                    default:
                        return ImageData.EMPTY;
                }
            }
        }
        return ImageData.EMPTY;
    }

    @Override
    public Skin getSkin() {
        return skin;
    }

    public SerializedSkin getSerializedSkin() {
        if (this.serializedSkin == null) {
            List<AnimationData> animations = new ArrayList<>();
            for (SkinAnimation a : this.skin.getAnimations()) {
                animations.add(new AnimationData(
                        com.nukkitx.protocol.bedrock.data.skin.ImageData.of(a.getImage().getWidth(), a.getImage().getHeight(), a.getImage().getImage()),
                        AnimatedTextureType.values()[a.getType()],
                        a.getFrames(),
                        AnimationExpressionType.values()[a.getExpression()]
                ));
            }
            this.serializedSkin = SerializedSkin.of(this.skin.getSkinId(),
                    this.skin.getPlayFabId(),
                    this.skin.getSkinResourcePatch(),
                    com.nukkitx.protocol.bedrock.data.skin.ImageData.of(this.skin.getSkinData().getWidth(), this.skin.getSkinData().getWidth(), this.skin.getSkinData().getImage()),
                    animations,
                    com.nukkitx.protocol.bedrock.data.skin.ImageData.of(this.skin.getCapeData().getWidth(), this.skin.getCapeData().getWidth(), this.skin.getCapeData().getImage()),
                    this.skin.getGeometryData(),
                    this.skin.getAnimationData(),
                    this.skin.isPremium(),
                    this.skin.isPersona(),
                    this.skin.isCapeOnClassic(),
                    this.skin.getCapeId(),
                    this.skin.getFullSkinId());
        }
        return this.serializedSkin;
    }

    private void decodeSkinData(String skinData) {
        JsonNode skinToken = decodeToken(skinData);
        if (skinToken == null) return;
        if (skinToken.has("ClientRandomId")) this.clientId = skinToken.get("ClientRandomId").longValue();
        if (skinToken.has("ServerAddress")) this.serverAddress = skinToken.get("ServerAddress").textValue();
        if (skinToken.has("DeviceModel")) this.deviceModel = skinToken.get("DeviceModel").textValue();
        if (skinToken.has("DeviceOS")) this.deviceOS = skinToken.get("DeviceOS").intValue();
        if (skinToken.has("DeviceId")) this.deviceId = skinToken.get("DeviceId").textValue();
        if (skinToken.has("GameVersion")) this.gameVersion = skinToken.get("GameVersion").textValue();
        if (skinToken.has("GuiScale")) this.guiScale = skinToken.get("GuiScale").intValue();
        if (skinToken.has("LanguageCode")) this.languageCode = skinToken.get("LanguageCode").textValue();
        if (skinToken.has("CurrentInputMode")) this.currentInputMode = skinToken.get("CurrentInputMode").intValue();
        if (skinToken.has("DefaultInputMode")) this.defaultInputMode = skinToken.get("DefaultInputMode").intValue();
        if (skinToken.has("UIProfile")) this.UIProfile = skinToken.get("UIProfile").intValue();
        this.skin = getSkin(skinToken);
    }
}
