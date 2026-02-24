/**
 * Slot-group view interfaces for the Cloudburst inventory/screen API.
 *
 * <h2>Naming conventions</h2>
 *
 * <h3>{@code Block}-prefixed views</h3>
 * <p>Any interface whose name starts with {@code Block} (e.g. {@link org.cloudburstmc.api.inventory.view.BlockStorageView},
 * {@link org.cloudburstmc.api.inventory.view.BlockFurnaceView}) extends
 * {@link org.cloudburstmc.api.inventory.view.BlockSlotGroup} and therefore provides:</p>
 * <ul>
 *   <li>{@link org.cloudburstmc.api.inventory.view.BlockSlotGroup#getBlock()} — the world block backing this group</li>
 *   <li>{@link org.cloudburstmc.api.inventory.view.BlockSlotGroup#getBlockEntity()} — the block entity (if any)</li>
 * </ul>
 * <p>These views always correspond to a real block and block entity in the server's world.</p>
 *
 * <h3>Unprefixed views</h3>
 * <p>Interfaces without the {@code Block} prefix (e.g. {@link org.cloudburstmc.api.inventory.view.StorageView},
 * {@link org.cloudburstmc.api.inventory.view.HopperView}) may or may not be backed by a block entity.
 * In particular, <em>virtual</em> containers created by plugins via
 * {@link org.cloudburstmc.api.inventory.VirtualStorageScreen} use the same unprefixed view interfaces
 * but have no corresponding block entity in the world.</p>
 *
 * <h2>Common base types</h2>
 * <ul>
 *   <li>{@link org.cloudburstmc.api.inventory.view.SlotGroup} — root interface for all slot groups</li>
 *   <li>{@link org.cloudburstmc.api.inventory.view.BlockSlotGroup} — adds block/block-entity accessors</li>
 *   <li>{@link org.cloudburstmc.api.inventory.view.GridView} — 3×3 grid shared by dispenser, dropper, and crafter</li>
 * </ul>
 */
package org.cloudburstmc.api.inventory.view;
