package org.cloudburstmc.server.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import io.netty.buffer.ByteBuf;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.util.LoginChainData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.server.Bootstrap;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
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

    private final List<SignedJWT> chainData;
    private final SignedJWT skinData;
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

    private ClientChainData(List<SignedJWT> chain, SignedJWT skinData) {
        this.chainData = chain;
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

//    public static ClientChainData of(byte[] buffer) {
//        ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
//        return new ClientChainData(readString(byteBuf), readString(byteBuf));
//    }

    public static ClientChainData read(LoginPacket pk) {
        return new ClientChainData(pk.getChain().stream().map(ClientChainData::parseJwt).toList(), parseJwt(pk.getExtra()));
    }

    private static SignedJWT parseJwt(String jwt) {
        try {
            return SignedJWT.parse(jwt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Internal
    ///////////////////////////////////////////////////////////////////////////

    private String username;
    private UUID clientUUID;
    private String xuid;

    private static ECPublicKey generateKey(String base64) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
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

    private boolean verify(ECPublicKey key, JWSObject object) throws JOSEException {
        return object.verify(new ECDSAVerifier(key));
    }

    private JsonNode decodeToken(SignedJWT token) {
        try {
            return Bootstrap.JSON_MAPPER.readTree(token.getPayload().toBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid token JSON", e);
        }
    }

    private void decodeChainData(List<SignedJWT> chains) {

        // Validate keys
        try {
            xboxAuthed = verifyChain(chains);
        } catch (Exception e) {
            xboxAuthed = false;
        }

        for (SignedJWT c : chains) {
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

    private boolean verifyChain(List<SignedJWT> chains) throws Exception {
        ECPublicKey lastKey = null;
        boolean mojangKeyVerified = false;
        Iterator<SignedJWT> iterator = chains.iterator();
        while (iterator.hasNext()) {
            SignedJWT jwt = iterator.next();

            URI x5u = jwt.getHeader().getX509CertURL();
            if (x5u == null) {
                return false;
            }

            ECPublicKey expectedKey = generateKey(x5u.toString());
            // First key is self-signed
            if (lastKey == null) {
                lastKey = expectedKey;
            } else if (!lastKey.equals(expectedKey)) {
                return false;
            }

            if (!verify(lastKey, jwt)) {
                return false;
            }

            if (mojangKeyVerified) {
                return !iterator.hasNext();
            }

            if (lastKey.equals(MOJANG_PUBLIC_KEY)) {
                mojangKeyVerified = true;
            }

            Map<String, Object> payload = jwt.getPayload().toJSONObject();
            Object base64key = payload.get("identityPublicKey");
            if (!(base64key instanceof String)) {
                throw new RuntimeException("No key found");
            }
            lastKey = generateKey((String) base64key);
        }
        return mojangKeyVerified;
    }

    @Override
    public Skin getSkin() {
        return skin;
    }

    public SerializedSkin getSerializedSkin() {
        if (this.serializedSkin == null) {
            this.serializedSkin = SkinUtils.toSerialized(this.skin);
        }
        return this.serializedSkin;
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
}
