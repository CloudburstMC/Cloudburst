package org.cloudburstmc.server.crafting;

import com.google.common.collect.ImmutableList;
import io.netty.util.collection.CharObjectHashMap;
import io.netty.util.collection.CharObjectMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.crafting.RecipeIngredient;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.crafting.RecipeUnlockContext;
import org.cloudburstmc.api.crafting.ShapedRecipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.*;
import org.cloudburstmc.server.utils.Utils;

import java.util.*;

public class CloudShapedRecipe implements ShapedRecipe {

    private final Identifier recipeId;
    private final ItemStack primaryResult;
    private final ImmutableList<ItemStack> extraResults;
    private final CharObjectHashMap<ItemStack> ingredients = new CharObjectHashMap<>();
    private final CharObjectHashMap<ItemDescriptorWithCount> ingredientDescriptors = new CharObjectHashMap<>();
    private final String[] shape;
    private final int priority;
    private final Identifier block;
    private boolean assumeSymmetry;
    private RecipeUnlockContext unlockContext = RecipeUnlockContext.NONE;

    public CloudShapedRecipe(Identifier recipeId, int priority, ItemStack primaryResult, String[] shape, CharObjectMap<ItemStack> ingredients, List<ItemStack> extraResults, Identifier block) {
        this(recipeId, priority, primaryResult, shape, ingredients, null, extraResults, block);
    }

    public CloudShapedRecipe(Identifier recipeId, int priority, ItemStack primaryResult, String[] shape, CharObjectMap<ItemStack> ingredients, CharObjectMap<ItemDescriptorWithCount> descriptors, List<ItemStack> extraResults, Identifier block) {
        this.recipeId = recipeId;
        this.priority = priority;
        int rowCount = shape.length;
        if (rowCount > 3 || rowCount <= 0) {
            throw new RuntimeException("Shaped recipes may only have 1, 2 or 3 rows, not " + rowCount);
        }

        int columnCount = shape[0].length();
        if (columnCount > 3 || columnCount <= 0) {
            throw new RuntimeException("Shaped recipes may only have 1, 2 or 3 columns, not " + columnCount);
        }

        for (String row : shape) {
            if (row.length() != columnCount) {
                throw new RuntimeException("Shaped recipe rows must all have the same length (expected " + columnCount + ", got " + row.length() + ")");
            }

            for (int x = 0; x < columnCount; ++x) {
                char c = row.charAt(x);

                if (c != ' ' && !ingredients.containsKey(c)) {
                    throw new RuntimeException("No item specified for symbol '" + c + "'");
                }
            }
        }

        this.primaryResult = primaryResult;
        this.extraResults = ImmutableList.copyOf(extraResults);
        this.block = block;
        this.shape = shape;
        this.assumeSymmetry = true;

        for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
            this.setIngredient(entry.getKey(), entry.getValue());
        }

        if (descriptors != null) {
            for (Map.Entry<Character, ItemDescriptorWithCount> entry : descriptors.entrySet()) {
                this.ingredientDescriptors.put(entry.getKey().charValue(), entry.getValue());
            }
        }
    }

    /**
     * Converts a protocol descriptor to a {@link RecipeIngredient}.
     * Returns {@code null} for invalid/empty descriptors.
     */
    @Nullable
    static RecipeIngredient descriptorToIngredient(ItemDescriptorWithCount descriptor) {
        return switch (descriptor.getDescriptor()) {
            case DefaultDescriptor d -> new RecipeIngredient.Exact(Identifier.parse(d.getItemId().getIdentifier()));
            case ItemTagDescriptor t -> new RecipeIngredient.Tag(Identifier.parse(t.getItemTag()));
            case DeferredDescriptor d -> new RecipeIngredient.Exact(Identifier.parse(d.getFullName()));
            case ComplexAliasDescriptor c -> new RecipeIngredient.Tag(Identifier.parse(c.getName()));
            case MolangDescriptor m -> new RecipeIngredient.Tag(Identifier.parse(m.getTagExpression()));
            default -> null;
        };
    }

    @Override
    public int getWidth() {
        return this.shape[0].length();
    }

    @Override
    public int getHeight() {
        return this.shape.length;
    }

    @Override
    public ItemStack getResult() {
        return this.primaryResult;
    }

    @Override
    public Identifier getId() {
        return this.recipeId;
    }

    public CloudShapedRecipe setIngredient(String key, ItemStack item) {
        return this.setIngredient(key.charAt(0), item);
    }

    public CloudShapedRecipe setIngredient(char key, ItemStack item) {
        if (String.join("", this.shape).indexOf(key) < 0) {
            throw new RuntimeException("Symbol does not appear in the shape: " + key);
        }

        this.ingredients.put(key, item);
        return this;
    }

    @Override
    public List<ItemStack> getIngredientList() {
        List<ItemStack> items = new ArrayList<>();
        for (int y = 0, y2 = getHeight(); y < y2; ++y) {
            for (int x = 0, x2 = getWidth(); x < x2; ++x) {
                ItemStack ingredient = getIngredient(x, y);
                if (!ingredient.isEmpty()) {
                    items.add(ingredient);
                }
            }
        }
        return items;
    }

    public List<ItemDescriptorWithCount> getInputDescriptorList() {
        if (ingredientDescriptors.isEmpty()) {
            return null;
        }
        List<ItemDescriptorWithCount> descriptors = new ArrayList<>();
        for (int y = 0, y2 = getHeight(); y < y2; ++y) {
            for (int x = 0, x2 = getWidth(); x < x2; ++x) {
                char c = this.shape[y].charAt(x);
                ItemDescriptorWithCount desc = this.ingredientDescriptors.get(c);
                descriptors.add(Objects.requireNonNullElse(desc, ItemDescriptorWithCount.EMPTY));
            }
        }
        return descriptors;
    }

    Map<Integer, Map<Integer, ItemStack>> getIngredientMap() {
        Map<Integer, Map<Integer, ItemStack>> ingredientMap = new LinkedHashMap<>();

        for (int y = 0, y2 = getHeight(); y < y2; ++y) {
            Map<Integer, ItemStack> m = new LinkedHashMap<>();

            for (int x = 0, x2 = getWidth(); x < x2; ++x) {
                m.put(x, getIngredient(x, y));
            }

            ingredientMap.put(y, m);
        }

        return ingredientMap;
    }

    @Override
    public ItemStack getIngredient(int x, int y) {
        ItemStack item = this.ingredients.get(this.shape[y].charAt(x));

        return item != null ? item : ItemStack.EMPTY;
    }

    @Override
    public RecipeIngredient getIngredientChoice(int x, int y) {
        char c = this.shape[y].charAt(x);
        if (c == ' ') return null;
        ItemDescriptorWithCount descriptor = this.ingredientDescriptors.get(c);
        if (descriptor == null) return null;
        return descriptorToIngredient(descriptor);
    }

    @Override
    public String[] getShape() {
        return shape;
    }

    @Override
    public boolean isAssumeSymmetry() {
        return this.assumeSymmetry;
    }

    public void setAssumeSymmetry(boolean assumeSymmetry) {
        this.assumeSymmetry = assumeSymmetry;
    }

    @Override
    public RecipeType getType() {
        return RecipeType.SHAPED;
    }

    @Override
    public List<ItemStack> getExtraResults() {
        return extraResults;
    }

    @Override
    public List<ItemStack> getAllResults() {
        List<ItemStack> list = new ArrayList<>();
        list.add(this.primaryResult);
        list.addAll(this.extraResults);
        return list;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public RecipeUnlockContext getUnlockContext() {
        return this.unlockContext;
    }

    public void setUnlockContext(RecipeUnlockContext context) {
        this.unlockContext = context;
    }

    public boolean matchItems(ItemStack[][] input, ItemStack[][] output) {
        if (!matchInputMap(Utils.clone2dArray(input))) {

            ItemStack[][] reverse = Utils.clone2dArray(input);

            for (int y = 0; y < reverse.length; y++) {
                reverse[y] = Utils.reverseArray(reverse[y], false);
            }

            if (!matchInputMap(reverse)) {
                return false;
            }
        }

        List<ItemStack> haveItems = new ArrayList<>();
        for (ItemStack[] items : output) {
            haveItems.addAll(Arrays.asList(items));
        }

        List<ItemStack> needItems = this.getExtraResults();

        for (ItemStack haveItem : new ArrayList<>(haveItems)) {
            if (haveItem.isEmpty()) {
                haveItems.remove(haveItem);
                continue;
            }

            for (ItemStack needItem : new ArrayList<>(needItems)) {
                if (needItem.equals(haveItem) && needItem.getCount() == haveItem.getCount()) {
                    haveItems.remove(haveItem);
                    needItems.remove(needItem);
                    break;
                }
            }
        }

        return haveItems.isEmpty() && needItems.isEmpty();
    }

    private boolean matchInputMap(ItemStack[][] input) {
        Map<Integer, Map<Integer, ItemStack>> map = this.getIngredientMap();

        for (int y = 0, y2 = this.getHeight(); y < y2; ++y) {
            for (int x = 0, x2 = this.getWidth(); x < x2; ++x) {
                ItemStack given = input[y][x];
                ItemStack required = map.get(y).get(x);

                if (!required.equals(given) || required.getCount() != given.getCount()) {
                    return false;
                }

                input[y][x] = null;
            }
        }

        for (ItemStack[] items : input) {
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public Identifier getBlock() {
        return block;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ");

        ingredients.forEach((character, item) -> joiner.add(item.toString()));
        return joiner.toString();
    }

    @Override
    public boolean requiresCraftingTable() {
        return this.getHeight() > 2 || this.getWidth() > 2;
    }
}
