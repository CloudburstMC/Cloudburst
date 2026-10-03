package org.cloudburstmc.server.event;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.event.player.PlayerAsyncPreLoginEvent;
import org.cloudburstmc.api.event.player.PlayerConnectionValidateLoginEvent;
import org.cloudburstmc.api.event.player.PlayerLoginResult;
import org.cloudburstmc.api.player.PlayerClientInfo;
import org.cloudburstmc.api.player.PlayerProfile;
import org.cloudburstmc.api.player.skin.Skin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class PlayerConnectionValidateLoginEventTest {

    private static final PlayerProfile PROFILE = (PlayerProfile) Proxy.newProxyInstance(
            PlayerProfile.class.getClassLoader(), new Class<?>[]{PlayerProfile.class, PlayerClientInfo.class},
            (proxy, method, arguments) -> {
                throw new UnsupportedOperationException(method.getName());
            });

    @Test
    void exposesTheConnectingIdentityWithoutAPlayerEntity() {
        PlayerConnectionValidateLoginEvent event = newEvent(PlayerLoginResult.ALLOWED, Component.empty());
        assertSame(PROFILE, event.getProfile());
        assertSame(PROFILE, event.getClientInfo());
        assertEquals(new InetSocketAddress("127.0.0.1", 12345), event.getAddress());
        assertFalse(event.isAsynchronous());
    }

    @Test
    void preservesTheServersDenialReasonAndMessage() {
        Component message = Component.text("Denied");
        PlayerConnectionValidateLoginEvent event = newEvent(PlayerLoginResult.KICK_BANNED, message);
        assertEquals(PlayerLoginResult.KICK_BANNED, event.getLoginResult());
        assertEquals(message, event.kickMessage());
    }

    @Test
    void allowingAdmissionClearsTheDenialMessage() {
        PlayerConnectionValidateLoginEvent event = newEvent(PlayerLoginResult.KICK_FULL, Component.text("Full"));
        event.allow();
        assertEquals(PlayerLoginResult.ALLOWED, event.getLoginResult());
        assertEquals(Component.empty(), event.kickMessage());
    }

    @Test
    void replacesTheDecisionAndMessageTogether() {
        PlayerConnectionValidateLoginEvent event = newEvent(PlayerLoginResult.ALLOWED, Component.empty());
        Component message = Component.text("Private server");
        event.disallow(PlayerLoginResult.KICK_WHITELIST, message);
        assertEquals(PlayerLoginResult.KICK_WHITELIST, event.getLoginResult());
        assertEquals(message, event.kickMessage());
    }

    @Test
    void rejectsAnAllowedResultAsADenialWithoutChangingTheDecision() {
        PlayerConnectionValidateLoginEvent event = newEvent(PlayerLoginResult.KICK_FULL, Component.text("Full"));
        assertThrows(IllegalArgumentException.class, () -> event.disallow(PlayerLoginResult.ALLOWED, Component.empty()));
        assertEquals(PlayerLoginResult.KICK_FULL, event.getLoginResult());
        assertEquals(Component.text("Full"), event.kickMessage());
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void rejectsNullDenialsWithoutChangingTheDecision() {
        PlayerConnectionValidateLoginEvent event = newEvent(PlayerLoginResult.ALLOWED, Component.empty());
        assertThrows(NullPointerException.class, () -> event.disallow(null, Component.text("Denied")));
        assertThrows(NullPointerException.class, () -> event.disallow(PlayerLoginResult.KICK_OTHER, null));
        assertEquals(PlayerLoginResult.ALLOWED, event.getLoginResult());
        assertEquals(Component.empty(), event.kickMessage());
    }

    @Test
    void preLoginUsesAnAsynchronousContractAndAllowsReplacingTheSkin() {
        Skin skin = new Skin();
        PlayerAsyncPreLoginEvent event = new PlayerAsyncPreLoginEvent(PROFILE, (PlayerClientInfo) PROFILE,
                new InetSocketAddress("127.0.0.1", 12345), skin);
        assertTrue(event.isAsynchronous());
        Skin replacement = new Skin();
        event.setSkin(replacement);
        assertSame(replacement, event.getSkin());
    }

    @Test
    void preLoginAllowClearsAPreviousDenial() {
        PlayerAsyncPreLoginEvent event = new PlayerAsyncPreLoginEvent(PROFILE, (PlayerClientInfo) PROFILE,
                new InetSocketAddress("127.0.0.1", 12345), new Skin());
        event.disallow(PlayerLoginResult.KICK_OTHER, Component.text("Denied"));
        event.allow();
        assertEquals(PlayerLoginResult.ALLOWED, event.getLoginResult());
        assertEquals(Component.empty(), event.kickMessage());
    }

    private static PlayerConnectionValidateLoginEvent newEvent(PlayerLoginResult result, Component message) {
        return new PlayerConnectionValidateLoginEvent(PROFILE, (PlayerClientInfo) PROFILE,
                new InetSocketAddress("127.0.0.1", 12345), result, message);
    }
}
