package org.cloudburstmc.server.crafting;

import com.google.common.collect.ImmutableList;
import io.netty.util.collection.CharObjectHashMap;
import io.netty.util.collection.CharObjectMap;
import org.cloudburstmc.api.inventory.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Utils;

import java.util.*;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ShapedRecipe implements CraftingRecipe {

    private final Identifier recipeId;
    private final ItemStack primaryResult;
    private final ImmutableList<ItemStack> extraResults;
    private final CharObjectHashMap<ItemStack> ingredients = new CharObjectHashMap<>();
    private final String[] shape;
    private final int priority;
    private final Identifier block;

    /**
     * Constructs a ShapedRecipe instance.
     *
     * @param primaryResult    Primary result of the recipe
     * @param shape<br>        Array of 1, 2, or 3 strings representing the rows of the recipe.
     *                         This accepts an array of 1, 2 or 3 strings. Each string should be of the same length and must be at most 3
     *                         characters long. Each character represents a unique type of ingredient. Spaces are interpreted as air.
     * @param ingredients<br>  Char =&gt; Item map of items to be set into the shape.
     *                         This accepts an array of Items, indexed by character. Every unique character (except space) in the shape
     *                         array MUST have a corresponding item in this list. Space character is automatically treated as air.
     * @param extraResults<br> List of additional result items to leave in the crafting grid afterwards. Used for things like cake recipe
     *                         empty buckets.
     *                         <p>
     *                         Note: Recipes **do not** need to be square. Do NOT add padding for empty rows/columns.
     */
    public ShapedRecipe(Identifier recipeId, int priority, ItemStack primaryResult, String[] shape,
                        CharObjectMap<ItemStack> ingredients, List<ItemStack> extraResults, Identifier block) {
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

        for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
            this.setIngredient(entry.getKey(), entry.getValue());
        }
    }

    public int getWidth() {
        return this.shape[0].length();
    }

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

    public ShapedRecipe setIngredient(String key, ItemStack item) {
        return this.setIngredient(key.charAt(0), item);
    }

    public ShapedRecipe setIngredient(char key, ItemStack item) {
        if (String.join("", this.shape).indexOf(key) < 0) {
            throw new RuntimeException("Symbol does not appear in the shape: " + key);
        }

        this.ingredients.put(key, item);
        return this;
    }

    public List<ItemStack> getIngredientList() {
        List<ItemStack> items = new ArrayList<>();
        for (int y = 0, y2 = getHeight(); y < y2; ++y) {
            for (int x = 0, x2 = getWidth(); x < x2; ++x) {
                if (getIngredient(x, y) != null)
                    items.add(getIngredient(x, y));
            }
        }
        return items;
    }

    public Map<Integer, Map<Integer, ItemStack>> getIngredientMap() {
        Map<Integer, Map<Integer, ItemStack>> ingredients = new LinkedHashMap<>();

        for (int y = 0, y2 = getHeight(); y < y2; ++y) {
            Map<Integer, ItemStack> m = new LinkedHashMap<>();

            for (int x = 0, x2 = getWidth(); x < x2; ++x) {
                m.put(x, getIngredient(x, y));
            }

            ingredients.put(y, m);
        }

        return ingredients;
    }

    public ItemStack getIngredient(int x, int y) {
        ItemStack item = this.ingredients.get(this.shape[y].charAt(x));

        return item != null ? item : CloudItemRegistry.AIR;
    }

    public String[] getShape() {
        return shape;
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

        //and then, finally, check that the output items are good:
        List<ItemStack> haveItems = new ArrayList<>();
        for (ItemStack[] items : output) {
            haveItems.addAll(Arrays.asList(items));
        }

        List<ItemStack> needItems = this.getExtraResults();

        for (ItemStack haveItem : new ArrayList<>(haveItems)) {
            if (haveItem.isNull()) {
                haveItems.remove(haveItem);
                continue;
            }

            for (ItemStack needItem : new ArrayList<>(needItems)) {
                if (needItem.equals(haveItem) && needItem.getAmount() == haveItem.getAmount()) {
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

        //match the given items to the requested items
        for (int y = 0, y2 = this.getHeight(); y < y2; ++y) {
            for (int x = 0, x2 = this.getWidth(); x < x2; ++x) {
                ItemStack given = input[y][x];
                ItemStack required = map.get(y).get(x);

                if (given == null || !required.equals(given) || required.getAmount() != given.getAmount()) {
                    return false;
                }

                input[y][x] = null;
            }
        }

        //check if there are any items left in the grid outside of the recipe
        for (ItemStack[] items : input) {
            for (ItemStack item : items) {
                if (item != null && !item.isNull()) {
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