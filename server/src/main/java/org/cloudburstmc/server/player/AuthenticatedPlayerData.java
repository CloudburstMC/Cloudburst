package org.cloudburstmc.server.player;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.player.*;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthType;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.utils.SkinUtils;
import tools.jackson.databind.JsonNode;

import java.security.PublicKey;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Getter
public class AuthenticatedPlayerData implements PlayerProfile, PlayerClientInfo {

    private final String name;
    private final UUID uniqueId;
    private final @Nullable String minecraftId;
    private final @Nullable String xuid;
    private final PublicKey parsedIdentityPublicKey;
    private final boolean authenticated;
    private final long clientId;
    private final @Nullable String serverAddress;
    private final @Nullable String deviceModel;
    private final DevicePlatform devicePlatform;
    private final @Nullable String gameVersion;
    private final int guiScale;
    private final Locale locale;
    private final InputMode currentInputMode;
    private final InputMode defaultInputMode;
    private final UiProfile uiProfile;
    private SerializedSkin serializedSkin;
    private Skin skin;

    private AuthenticatedPlayerData(ChainValidationResult chainResult, String clientJwt, boolean authenticated) {
        this.authenticated = authenticated;
        try {
            ChainValidationResult.IdentityClaims claims = chainResult.identityClaims();
            ChainValidationResult.IdentityData extraData = claims.extraData;

            this.name = Objects.requireNonNull(extraData.displayName, "Missing display name");
            this.uniqueId = resolveUniqueId(chainResult, extraData.xuid);
            this.minecraftId = extraData.minecraftId;
            this.xuid = this.authenticated ? extraData.xuid : null;
            this.parsedIdentityPublicKey = claims.parsedIdentityPublicKey();

            byte[] verifiedSkinData = EncryptionUtils.verifyClientData(clientJwt, this.parsedIdentityPublicKey);
            if (verifiedSkinData == null) {
                throw new IllegalStateException("Client data signature does not match the identity key");
            }

            JsonNode skinToken = Objects.requireNonNull(Bootstrap.JSON_MAPPER.readTree(verifiedSkinData), "Client data is empty");
            DecodedClientData clientData = DecodedClientData.read(skinToken);

            this.clientId = clientData.clientId();
            this.serverAddress = clientData.serverAddress();
            this.deviceModel = clientData.deviceModel();
            this.devicePlatform = clientData.devicePlatform();
            this.gameVersion = clientData.gameVersion();
            this.guiScale = clientData.guiScale();
            this.locale = clientData.locale();
            this.currentInputMode = clientData.currentInputMode();
            this.defaultInputMode = clientData.defaultInputMode();
            this.uiProfile = clientData.uiProfile();

            this.skin = SkinUtils.fromToken(skinToken);
            this.skin.setGeometryDataEngineVersion(this.gameVersion);
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate client identity data", e);
        }
    }

    public static AuthenticatedPlayerData read(LoginPacket pk) {
        try {
            AuthType authType = pk.getAuthPayload().getAuthType();
            if (authType == AuthType.GUEST) {
                throw new IllegalStateException("Guest authentication is not supported");
            }

            ChainValidationResult result = EncryptionUtils.validatePayload(pk.getAuthPayload());
            return new AuthenticatedPlayerData(result, pk.getClientJwt(), authType == AuthType.FULL);
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate login chain", e);
        }
    }

    private static Locale parseLocale(@Nullable String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return Locale.US;
        }

        Locale locale = Locale.forLanguageTag(languageCode.replace('_', '-'));
        return locale.getLanguage().isBlank() ? Locale.US : locale;
    }

    private static UUID resolveUniqueId(ChainValidationResult result, @Nullable String xuid) {
        Map<String, Object> claims = result.rawIdentityClaims();
        Object legacyUniqueId = claims.get("leguuid");
        if (legacyUniqueId == null && claims.get("extraData") instanceof Map<?, ?> extraData) {
            legacyUniqueId = extraData.get("identity");
        }

        if (legacyUniqueId instanceof String value && !value.isBlank()) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Player identity is not a valid UUID", exception);
            }
        }

        if (xuid == null || xuid.isBlank()) {
            throw new IllegalStateException("Player identity is missing both a legacy UUID and XUID");
        }

        return UUID.nameUUIDFromBytes(("pocket-auth-1-xuid:" + xuid).getBytes(UTF_8));
    }

    private static <T> T enumValue(T[] values, int id, T fallback) {
        return id >= 0 && id < values.length ? values[id] : fallback;
    }

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
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        AuthenticatedPlayerData that = (AuthenticatedPlayerData) obj;
        return Objects.equals(this.uniqueId, that.uniqueId) && Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, name);
    }

    private record DecodedClientData(long clientId, @Nullable String serverAddress, @Nullable String deviceModel,
                                     DevicePlatform devicePlatform, @Nullable String gameVersion, int guiScale,
                                     Locale locale, InputMode currentInputMode, InputMode defaultInputMode,
                                     UiProfile uiProfile) {

        private static DecodedClientData read(JsonNode token) {
            return new DecodedClientData(
                    longValue(token.get("ClientRandomId")),
                    stringValue(token, "ServerAddress"),
                    stringValue(token, "DeviceModel"),
                    enumValue(DevicePlatform.values(), intValue(token, "DeviceOS", -1), DevicePlatform.UNKNOWN),
                    stringValue(token, "GameVersion"),
                    intValue(token, "GuiScale", 0),
                    parseLocale(stringValue(token, "LanguageCode")),
                    enumValue(InputMode.values(), intValue(token, "CurrentInputMode", -1), InputMode.UNKNOWN),
                    enumValue(InputMode.values(), intValue(token, "DefaultInputMode", -1), InputMode.UNKNOWN),
                    switch (intValue(token, "UIProfile", -1)) {
                        case 0 -> UiProfile.CLASSIC;
                        case 1 -> UiProfile.POCKET;
                        default -> UiProfile.UNKNOWN;
                    });
        }

        private static @Nullable String stringValue(JsonNode token, String name) {
            JsonNode value = token.get(name);
            return value == null ? null : value.stringValue();
        }

        private static int intValue(JsonNode token, String name, int fallback) {
            JsonNode value = token.get(name);
            return value == null ? fallback : value.intValue();
        }

        private static long longValue(@Nullable JsonNode value) {
            return value == null ? 0 : value.longValue();
        }
    }
}
