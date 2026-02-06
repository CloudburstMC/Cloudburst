package org.cloudburstmc.server.utils;

import tools.jackson.databind.JsonNode;
import com.nimbusds.jwt.SignedJWT;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.util.LoginChainData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.server.Bootstrap;

import java.security.PublicKey;
import java.text.ParseException;
import java.util.Objects;
import java.util.UUID;

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

    private final SignedJWT skinData;
    private SerializedSkin serializedSkin;
    private Skin skin;

    private String username;
    private UUID clientUUID;
    private String xuid;
    private String identityPublicKey;
    private PublicKey parsedIdentityPublicKey;
    private boolean xboxAuthed;

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

    public final static int UI_PROFILE_CLASSIC = 0;
    public final static int UI_PROFILE_POCKET = 1;

    private ClientChainData(ChainValidationResult chainResult, SignedJWT skinData) {
        this.skinData = skinData;
        this.xboxAuthed = chainResult.signed();

        try {
            ChainValidationResult.IdentityClaims claims = chainResult.identityClaims();
            ChainValidationResult.IdentityData extraData = claims.extraData;

            this.username = extraData.displayName;
            this.clientUUID = extraData.identity;
            this.xuid = this.xboxAuthed ? extraData.xuid : null;
            this.identityPublicKey = claims.identityPublicKey;
            this.parsedIdentityPublicKey = claims.parsedIdentityPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract identity claims", e);
        }

        decodeSkinData(skinData);
    }

    public static ClientChainData read(LoginPacket pk) {
        try {
            ChainValidationResult result = EncryptionUtils.validatePayload(pk.getAuthPayload());
            SignedJWT skinJwt = parseJwt(pk.getClientJwt());
            return new ClientChainData(result, skinJwt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate login chain", e);
        }
    }

    private static SignedJWT parseJwt(String jwt) {
        try {
            return SignedJWT.parse(jwt);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse JWT", e);
        }
    }

    private JsonNode decodeToken(SignedJWT token) {
        return Bootstrap.JSON_MAPPER.readTree(token.getPayload().toBytes());
    }

    private void decodeSkinData(SignedJWT skinData) {
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
        this.skin = SkinUtils.fromToken(skinToken);
    }

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

    public PublicKey getParsedIdentityPublicKey() {
        return parsedIdentityPublicKey;
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

    @Override
    public boolean isXboxAuthed() {
        return xboxAuthed;
    }

    @Override
    public int getCurrentInputMode() {
        return currentInputMode;
    }

    @Override
    public int getDefaultInputMode() {
        return defaultInputMode;
    }

    @Override
    public int getUIProfile() {
        return UIProfile;
    }

    @Override
    public Skin getSkin() {
        return skin;
    }

    @Override
    public void setSkin(Skin skin) {
        this.skin = skin;
        this.serializedSkin = SkinUtils.toSerialized(skin);
    }

    public void setSkin(SerializedSkin skin) {
        this.serializedSkin = skin;
        this.skin = SkinUtils.fromSerialized(skin);
    }

    public SerializedSkin getSerializedSkin() {
        if (this.serializedSkin == null) {
            this.serializedSkin = SkinUtils.toSerialized(this.skin);
        }
        return this.serializedSkin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        ClientChainData that = (ClientChainData) obj;
        return Objects.equals(this.clientUUID, that.clientUUID) &&
                Objects.equals(this.username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientUUID, username);
    }
}
