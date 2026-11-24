package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.entity.Shearable;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.event.player.PlayerShearEntityEvent;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class PlayerShearingTest {

    @Test
    void cancelledShearingDoesNotApplyDropsOrDamageTheTool() {
        Fixture fixture = new Fixture();
        ItemStack tool = fixture.held;

        assertTrue(fixture.shear(PlayerShearEntityEvent::setCancelled));

        assertNull(fixture.dropped);
        assertEquals(tool, fixture.held);
    }

    @Test
    void successfulSurvivalShearingUsesEventDropsAndDamagesTheTool() {
        Fixture fixture = new Fixture();
        List<ItemStack> replacementDrops = List.of(ItemStack.from(ItemTypes.DIAMOND));

        assertTrue(fixture.shear(event -> event.setDrops(replacementDrops)));

        assertEquals(replacementDrops, fixture.dropped);
        assertEquals(1, fixture.held.getDamage());
    }

    @Test
    void changingTheHeldItemDuringTheEventAbortsShearing() {
        Fixture fixture = new Fixture();
        ItemStack replacement = ItemStack.from(ItemTypes.DIAMOND);

        assertTrue(fixture.shear(event -> fixture.held = replacement));

        assertNull(fixture.dropped);
        assertEquals(replacement, fixture.held);
    }

    private static class Fixture {

        private ItemStack held;
        private List<ItemStack> dropped;

        Fixture() {
            PersistenceFixtures.ensureInitialized();
            this.held = ItemStack.from(ItemTypes.SHEARS);
        }

        boolean shear(Consumer<PlayerShearEntityEvent> listener) {
            EventManager eventManager = InterfaceProxy.create(EventManager.class, (proxy, method, args) -> {
                listener.accept((PlayerShearEntityEvent) args[0]);
                return null;
            });
            Server server = InterfaceProxy.create(Server.class, Map.of("getEventManager", eventManager));
            Level level = InterfaceProxy.create(Level.class, Map.of("getServer", server));
            PlayerInventoryView inventory = InterfaceProxy.create(PlayerInventoryView.class,
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getSelectedSlot" -> 0;
                        case "getItem" -> this.held;
                        case "setItem" -> {
                            this.held = (ItemStack) args[1];
                            yield null;
                        }
                        default -> throw new AssertionError(method.getName());
                    });
            Player player = InterfaceProxy.create(Player.class,
                    Map.of("getInventory", inventory, "getLevel", level, "isAlive", true));
            Shearable entity = InterfaceProxy.create(Shearable.class,
                    Map.of("getLevel", level, "readyForShearing", true));

            return PlayerShearing.tryShear(entity, player, this.held, List::of, drops -> this.dropped = drops);
        }
    }
}
