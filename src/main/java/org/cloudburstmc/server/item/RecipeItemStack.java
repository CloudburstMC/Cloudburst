package org.cloudburstmc.server.item;

import lombok.Getter;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentType;
import org.cloudburstmc.server.item.behavior.ItemBehavior;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@Immutable
public class RecipeItemStack extends CloudItemStack {

    private final ItemStack item;
    private final boolean hasMeta;

    public RecipeItemStack(CloudItemStack item, boolean hasMeta) {
        super(item.getId(), item.getType());
        this.item = item;
        this.hasMeta = hasMeta;
    }

    public boolean hasTag() {
        return false;
    }

    @Override
    public ItemType getType() {
        return item.getType();
    }

    @Override
    public int getAmount() {
        return item.getAmount();
    }

    @Override
    public boolean isNull() {
        return item.isNull();
    }

    @Override
    public String getName() {
        return item.getName();
    }

    @Override
    public boolean hasName() {
        return item.hasName();
    }

    @Override
    public List<String> getLore() {
        return item.getLore();
    }

    @Override
    public boolean hasEnchantments() {
        return item.hasEnchantments();
    }

    @Override
    public Map<EnchantmentType, EnchantmentInstance> getEnchantments() {
        return item.getEnchantments();
    }

    @Override
    public EnchantmentInstance getEnchantment(EnchantmentType enchantment) {
        return item.getEnchantment(enchantment);
    }

    @Override
    public Collection<Identifier> getCanDestroy() {
        return item.getCanDestroy();
    }

    @Override
    public boolean canDestroy(BlockState state) {
        return item.canDestroy(state);
    }

    @Override
    public Collection<Identifier> getCanPlaceOn() {
        return item.getCanPlaceOn();
    }

    @Override
    public boolean canPlaceOn(BlockState state) {
        return item.canPlaceOn(state);
    }

    @Override
    public <T> T getMetadata(Class<T> metadataClass) {
        return item.getMetadata(metadataClass);
    }

    @Override
    public <T> T getMetadata(Class<T> metadataClass, T defaultValue) {
        return item.getMetadata(metadataClass, defaultValue);
    }

    @Override
    public ItemStackBuilder toBuilder() {
        return item.toBuilder();
    }

    @Override
    public ItemBehavior getBehavior() {
        return item.getBehavior();
    }

    @Override
    public boolean isMergeable(@Nonnull ItemStack itemStack) {
        return item.isMergeable(itemStack);
    }

    @Override
    public boolean equals(@Nullable ItemStack item) {
        return this.item.equals(item);
    }

    @Override
    public boolean isFull() {
        return item.isFull();
    }

    @Override
    public boolean equals(@Nullable ItemStack other, boolean checkAmount) {
        return item.equals(other, checkAmount);
    }

    @Override
    public boolean equals(@Nullable ItemStack other, boolean checkAmount, boolean checkData) {
        return item.equals(other, checkAmount, checkData);
    }

    @Override
    public ItemStack decrementAmount() {
        return item.decrementAmount();
    }

    @Override
    public ItemStack decrementAmount(int amount) {
        return item.decrementAmount(amount);
    }

    @Override
    public ItemStack incrementAmount() {
        return item.incrementAmount();
    }

    @Override
    public ItemStack incrementAmount(int amount) {
        return item.incrementAmount(amount);
    }

    @Override
    public ItemStack withAmount(int amount) {
        return item.withAmount(amount);
    }

    @Override
    public ItemStack withEnchantment(EnchantmentInstance enchantment) {
        return item.withEnchantment(enchantment);
    }

    @Override
    public ItemStack withData(Object data) {
        return item.withData(data);
    }

    @Override
    public ItemStack withData(Class<?> metadataClass, Object data) {
        return item.withData(metadataClass, data);
    }

    @Override
    public BlockState getBlockState() {
        return item.getBlockState();
    }
}
