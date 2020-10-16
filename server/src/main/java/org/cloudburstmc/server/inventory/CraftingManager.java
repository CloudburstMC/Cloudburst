package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Config;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.Utils;

import javax.inject.Singleton;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

import static org.cloudburstmc.server.block.BlockIds.LIT_BLAST_FURNACE;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Log4j2
@Singleton
public class CraftingManager {

    public final Collection<Recipe> recipes = new ArrayDeque<>();

    public static final Comparator<CloudItemStack> recipeComparator = Comparator.comparing(CloudItemStack::getId)
            .thenComparingInt(i -> i.getNetworkData().getDamage()).thenComparingInt(ItemStack::getAmount);
    protected final Map<Integer, Map<UUID, ShapedRecipe>> shapedRecipes = new Int2ObjectOpenHashMap<>();

    public final Map<Integer, FurnaceRecipe> furnaceRecipes = new Int2ObjectOpenHashMap<>();

    public final Map<Integer, BrewingRecipe> brewingRecipes = new Int2ObjectOpenHashMap<>();
    public final Map<Integer, ContainerRecipe> containerRecipes = new Int2ObjectOpenHashMap<>();

    private int RECIPE_COUNT = 0;
    protected final Map<Integer, Map<UUID, ShapelessRecipe>> shapelessRecipes = new Int2ObjectOpenHashMap<>();
    private CraftingDataPacket packet = null;

    public CraftingManager() {
        InputStream recipesStream = Server.class.getClassLoader().getResourceAsStream("recipes.json");
        if (recipesStream == null) {
            throw new AssertionError("Unable to find recipes.json");
        }

        Config recipesConfig = new Config(Config.JSON);
        recipesConfig.load(recipesStream);
//        this.loadRecipes(recipesConfig); //TODO: load recipes

        String path = Server.getInstance().getDataPath() + "custom_recipes.json";
        File filePath = new File(path);

        if (filePath.exists()) {
            Config customRecipes = new Config(filePath, Config.JSON);
//            this.loadRecipes(customRecipes); //TODO: load recipes
        }
        this.rebuildPacket();

        log.info("Loaded " + this.recipes.size() + " recipes.");
    }

    private static int getFullItemHash(CloudItemStack item) {
        return Objects.hash(System.identityHashCode(item.getId()), item.getNetworkData().getDamage(), item.getCount());
    }

    private static UUID getMultiItemHash(Collection<CloudItemStack> items) {
        ByteBuffer buffer = ByteBuffer.allocate(items.size() * 8);
        for (CloudItemStack item : items) {
            buffer.putInt(getFullItemHash(item));
        }
        return UUID.nameUUIDFromBytes(buffer.array());
    }

    public void rebuildPacket() {
        rebuildPacket(true);
    }

    private static int getItemHash(CloudItemStack item) {
        return getItemHash(item.getId(), item.getNetworkData().getDamage());
    }

    public Collection<Recipe> getRecipes() {
        return recipes;
    }

    public Map<Integer, FurnaceRecipe> getFurnaceRecipes() {
        return furnaceRecipes;
    }

    public FurnaceRecipe matchFurnaceRecipe(ItemStack input, Identifier craftingBlock) {
        CloudItemStack nativeInput = (CloudItemStack) input;
        if (craftingBlock == BlockIds.LIT_SMOKER) {
            craftingBlock = BlockIds.SMOKER;
        } else if (craftingBlock == LIT_BLAST_FURNACE) {
            craftingBlock = BlockIds.BLAST_FURNACE;
        } else if (craftingBlock == BlockIds.LIT_FURNACE) {
            craftingBlock = BlockIds.FURNACE;
        }
        FurnaceRecipe recipe = this.furnaceRecipes.get(Objects.hash(getItemHash(nativeInput), craftingBlock.toString()));
        if (recipe == null)
            recipe = this.furnaceRecipes.get(Objects.hash(getItemHash(nativeInput.getId(), 0), craftingBlock.toString()));
        return recipe;
    }

    public void rebuildPacket(boolean cleanRecipes) {
        CraftingDataPacket packet = new CraftingDataPacket();
        packet.setCleanRecipes(cleanRecipes);

        int recipeId = 1;

        for (Recipe recipe : this.getRecipes()) {
            packet.getCraftingData().add(recipe.toNetwork(recipeId++));
        }
        for (Recipe recipe : this.getFurnaceRecipes().values()) {
            packet.getCraftingData().add(recipe.toNetwork(recipeId++));
        }

        for (BrewingRecipe recipe : brewingRecipes.values()) {
            packet.getPotionMixData().add(recipe.toData());
        }
        for (ContainerRecipe recipe : containerRecipes.values()) {
            packet.getContainerMixData().add(recipe.toData());
        }

        this.packet = packet;
    }

    private static int getItemHash(Identifier id, int meta) {
        return Objects.hash(System.identityHashCode(id), meta);
    }

    public void registerFurnaceRecipe(FurnaceRecipe recipe) {
        CloudItemStack input = (CloudItemStack) recipe.getInput();
        Identifier block = recipe.getBlock();
        this.furnaceRecipes.put(Objects.hash(getItemHash(input), block.toString()), recipe);
    }

    public void sendRecipesTo(Player player) {
        if (packet == null) {
            rebuildPacket();
        }
        player.sendPacket(packet);
    }

//    @SuppressWarnings("unchecked")
//    private void loadRecipes(Config config) { //TODO: load recipes
//        List<Map> recipes = config.getMapList("recipes");
//        log.info("Loading recipes...");
//        for (Map<String, Object> recipe : recipes) {
//            try {
//                String blockId = (String) recipe.get("block");
//                Identifier block = blockId == null ? null : Identifier.fromString(blockId);
//                switch (Utils.toInt(recipe.get("type"))) {
//                    case 0:
//                        // TODO: handle multiple result items
//                        List<Map> outputs = ((List<Map>) recipe.get("output"));
//                        if (outputs.size() > 1) {
//                            continue;
//                        }
//                        Map<String, Object> first = outputs.get(0);
//                        List<ItemStack> sorted = new ArrayList<>();
//                        for (Map<String, Object> ingredient : ((List<Map>) recipe.get("input"))) {
//                            sorted.add(ItemStack.fromJson(ingredient));
//                        }
//                        // Bake sorted list
//                        sorted.sort(recipeComparator);
//
//                        String recipeId = (String) recipe.get("id");
//                        int priority = Utils.toInt(recipe.get("priority"));
//
//                        ShapelessRecipe result = new ShapelessRecipe(recipeId, priority, ItemStack.fromJson(first), sorted, block);
//
//                        this.registerRecipe(result);
//                        break;
//                    case 1:
//                        outputs = (List<Map>) recipe.get("output");
//
//                        first = outputs.remove(0);
//                        String[] shape = ((List<String>) recipe.get("shape")).toArray(new String[0]);
//                        CharObjectMap<ItemStack> ingredients = new CharObjectHashMap<>();
//                        List<ItemStack> extraResults = new ArrayList<>();
//
//                        Map<String, Map<String, Object>> input = (Map) recipe.get("input");
//                        for (Map.Entry<String, Map<String, Object>> ingredientEntry : input.entrySet()) {
//                            char ingredientChar = ingredientEntry.getKey().charAt(0);
//                            ItemStack ingredient = ItemStack.fromJson(ingredientEntry.getValue());
//
//                            ingredients.put(ingredientChar, ingredient);
//                        }
//
//                        for (Map<String, Object> data : outputs) {
//                            extraResults.add(ItemStack.fromJson(data));
//                        }
//
//                        recipeId = (String) recipe.get("id");
//                        priority = Utils.toInt(recipe.get("priority"));
//
//                        this.registerRecipe(new ShapedRecipe(recipeId, priority, ItemStack.fromJson(first), shape, ingredients, extraResults, block));
//                        break;
//                    case 2:
//                    case 3:
//                        Map<String, Object> resultMap = (Map) recipe.get("output");
//                        ItemStack resultItem = ItemStack.fromJson(resultMap);
//                        ItemStack inputItem;
//                        Map<String, Object> inputMap = (Map) recipe.get("input");
//                        inputItem = ItemStack.fromJson(inputMap);
//
//                        this.registerRecipe(new FurnaceRecipe(resultItem, inputItem, block));
//                        break;
//                    default:
//                        break;
//                }
//            } catch (RegistryException e) {
//                // ignore non-implemented items
//            } catch (Exception e) {
//                log.error("Exception during registering recipe: " + recipe, e);
//            }
//        }
//
//        // Load brewing recipes
//        List<Map> potionMixes = config.getMapList("potionMixes");
//
//        for (Map potionMix : potionMixes) {
//            int fromPotionId = ((Number) potionMix.get("inputId")).intValue(); // gson returns doubles...
//            int fromPotionMeta = ((Number) potionMix.get("inputMeta")).intValue();
//            int ingredient = ((Number) potionMix.get("reagentId")).intValue();
//            int ingredientMeta = ((Number) potionMix.get("reagentMeta")).intValue();
//            int toPotionId = ((Number) potionMix.get("outputId")).intValue();
//            int toPotionMeta = ((Number) potionMix.get("outputMeta")).intValue();
//
//            try {
//                registerBrewingRecipe(new BrewingRecipe(ItemStack.get(fromPotionId, fromPotionMeta), ItemStack.get(ingredient, ingredientMeta), ItemStack.get(toPotionId, toPotionMeta)));
//            } catch (RegistryException e) {
//                // ignore
//            }
//        }
//
//        List<Map> containerMixes = config.getMapList("containerMixes");
//
//        for (Map containerMix : containerMixes) {
//            int fromItemId = ((Number) containerMix.get("inputId")).intValue();
//            int ingredient = ((Number) containerMix.get("reagentId")).intValue();
//            int toItemId = ((Number) containerMix.get("outputId")).intValue();
//
//            try {
//                registerContainerRecipe(new ContainerRecipe(ItemStack.get(fromItemId), ItemStack.get(ingredient), ItemStack.get(toItemId)));
//            } catch (RegistryException e) {
//                // ignore
//            }
//        }
//    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerShapedRecipe(ShapedRecipe recipe) {
        int resultHash = getItemHash((CloudItemStack) recipe.getResult());
        Map<UUID, ShapedRecipe> map = shapedRecipes.computeIfAbsent(resultHash, k -> new HashMap<>());
        map.put(getMultiItemHash((List) recipe.getIngredientList()), recipe);
    }

    private ItemStack[][] cloneItemMap(ItemStack[][] map) {
        ItemStack[][] newMap = new ItemStack[map.length][];
        for (int i = 0; i < newMap.length; i++) {
            ItemStack[] old = map[i];
            ItemStack[] n = new ItemStack[old.length];

            System.arraycopy(old, 0, n, 0, n.length);
            newMap[i] = n;
        }

        for (ItemStack[] row : newMap) {
            System.arraycopy(row, 0, row, 0, row.length);
        }
        return newMap;
    }

    public void registerRecipe(Recipe recipe) {
        if (recipe instanceof CraftingRecipe) {
            CloudItemStack result = (CloudItemStack) recipe.getResult();

            UUID id = Utils.dataToUUID(String.valueOf(++RECIPE_COUNT), String.valueOf(result.getId()),
                    String.valueOf(result.getNetworkData().getDamage()),
                    String.valueOf(result.getAmount()),
                    String.valueOf(result.getDataTag()));

            ((CraftingRecipe) recipe).setId(id);
            this.recipes.add(recipe);
        }

        recipe.registerToCraftingManager(this);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerShapelessRecipe(ShapelessRecipe recipe) {
        List list = recipe.getIngredientList();
        list.sort(recipeComparator);

        UUID hash = getMultiItemHash(list);

        int resultHash = getItemHash((CloudItemStack) recipe.getResult());
        Map<UUID, ShapelessRecipe> map = shapelessRecipes.computeIfAbsent(resultHash, k -> new HashMap<>());

        map.put(hash, recipe);
    }

    private static int getPotionHash(Identifier ingredientId, int potionType) {
        int id = CloudItemRegistry.get().getRuntimeId(ingredientId);
        return (id << 6) | potionType;
    }

    private static int getContainerHash(Identifier ingredientId, int containerId) {
        int id = CloudItemRegistry.get().getRuntimeId(ingredientId);
        return (id << 9) | containerId;
    }

    public void registerBrewingRecipe(BrewingRecipe recipe) {
        CloudItemStack input = (CloudItemStack) recipe.getIngredient();
        CloudItemStack potion = (CloudItemStack) recipe.getInput();

        this.brewingRecipes.put(getPotionHash(input.getId(), potion.getNetworkData().getDamage()), recipe);
    }

    public void registerContainerRecipe(ContainerRecipe recipe) {
        CloudItemStack input = (CloudItemStack) recipe.getIngredient();
        CloudItemStack potion = (CloudItemStack) recipe.getInput();

        this.containerRecipes.put(getContainerHash(input.getId(), potion.getNetworkData().getDamage()), recipe);
    }

    public BrewingRecipe matchBrewingRecipe(ItemStack input, ItemStack potion) {
        CloudItemStack nPotion = (CloudItemStack) potion;
        Identifier id = nPotion.getId();
        if (id == ItemIds.POTION || id == ItemIds.SPLASH_POTION || id == ItemIds.LINGERING_POTION) {
            return this.brewingRecipes.get(getPotionHash(((CloudItemStack) input).getId(), nPotion.getNetworkData().getDamage()));
        }

        return null;
    }

    public ContainerRecipe matchContainerRecipe(ItemStack input, ItemStack potion) {
        return this.containerRecipes.get(getContainerHash(
                ((CloudItemStack) input).getId(),
                CloudItemRegistry.get().getRuntimeId(((CloudItemStack) potion).getId())
        ));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public CraftingRecipe matchRecipe(ItemStack[][] inputMap, ItemStack primaryOutput, ItemStack[][] extraOutputMap) {
        //TODO: try to match special recipes before anything else (first they need to be implemented!)

        int outputHash = getItemHash((CloudItemStack) primaryOutput);
        if (this.shapedRecipes.containsKey(outputHash)) {
            List itemCol = new ArrayList<>();
            for (ItemStack[] items : inputMap) itemCol.addAll(Arrays.asList(items));
            UUID inputHash = getMultiItemHash(itemCol);

            Map<UUID, ShapedRecipe> recipeMap = shapedRecipes.get(outputHash);

            if (recipeMap != null) {
                ShapedRecipe recipe = recipeMap.get(inputHash);

                if (recipe != null && recipe.matchItems(this.cloneItemMap(inputMap), this.cloneItemMap(extraOutputMap))) { //matched a recipe by hash
                    return recipe;
                }

                for (ShapedRecipe shapedRecipe : recipeMap.values()) {
                    if (shapedRecipe.matchItems(this.cloneItemMap(inputMap), this.cloneItemMap(extraOutputMap))) {
                        return shapedRecipe;
                    }
                }
            }
        }

        if (shapelessRecipes.containsKey(outputHash)) {
            List list = new ArrayList<>();
            for (ItemStack[] a : inputMap) {
                list.addAll(Arrays.asList(a));
            }
            list.sort(recipeComparator);

            UUID inputHash = getMultiItemHash(list);

            Map<UUID, ShapelessRecipe> recipes = shapelessRecipes.get(outputHash);

            if (recipes == null) {
                return null;
            }

            ShapelessRecipe recipe = recipes.get(inputHash);

            if (recipe != null && recipe.matchItems(this.cloneItemMap(inputMap), this.cloneItemMap(extraOutputMap))) {
                return recipe;
            }

            for (ShapelessRecipe shapelessRecipe : recipes.values()) {
                if (shapelessRecipe.matchItems(this.cloneItemMap(inputMap), this.cloneItemMap(extraOutputMap))) {
                    return shapelessRecipe;
                }
            }
        }

        return null;
    }

    public static class Entry {
        final int resultItemId;
        final int resultMeta;
        final int ingredientItemId;
        final int ingredientMeta;
        final String recipeShape;
        final int resultAmount;

        public Entry(int resultItemId, int resultMeta, int ingredientItemId, int ingredientMeta, String recipeShape, int resultAmount) {
            this.resultItemId = resultItemId;
            this.resultMeta = resultMeta;
            this.ingredientItemId = ingredientItemId;
            this.ingredientMeta = ingredientMeta;
            this.recipeShape = recipeShape;
            this.resultAmount = resultAmount;
        }
    }
}
