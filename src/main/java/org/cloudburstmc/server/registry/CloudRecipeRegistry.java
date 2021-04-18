package org.cloudburstmc.server.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerMixData;
import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.data.inventory.PotionMixData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import io.netty.util.collection.CharObjectHashMap;
import io.netty.util.collection.CharObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.extern.log4j.Log4j2;
import lombok.var;
import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.inventory.Recipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.registry.RecipeRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.crafting.*;
import org.cloudburstmc.server.inventory.ContainerRecipe;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.RecipeItemStack;
import org.cloudburstmc.server.utils.TextFormat;
import org.cloudburstmc.server.utils.Utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.cloudburstmc.api.block.BlockIds.LIT_BLAST_FURNACE;

@Log4j2
public class CloudRecipeRegistry implements RecipeRegistry {
    private static final String UNLABELED_PREFIX = "minecraft:crafting_recipe_";
    private static final String UNLABELED_POTION_PREFIX = "minecraft:potion_";
    private static final String UNLABELED_CONTAINER_PREFIX = "minecraft:container_";

    private static final CloudRecipeRegistry INSTANCE;

    static {
        INSTANCE = new CloudRecipeRegistry(CloudItemRegistry.get()); // forces item registry to init first
    }

    public static final Comparator<ItemStack> recipeComparator = Comparator.comparing((ItemStack i) -> i.getType().getId())
            .thenComparingInt(i -> ((CloudItemStack) i).getNetworkData().getDamage()).thenComparingInt(ItemStack::getAmount);

    private final ItemRegistry itemRegistry;
    private final Map<Identifier, Recipe> recipeMap = new Object2ReferenceOpenHashMap<>();
    private final Int2ReferenceMap<Map<UUID, Identifier>> recipeHashMap = new Int2ReferenceOpenHashMap<>();

    private boolean closed;
    private CraftingDataPacket cached;

    public static CloudRecipeRegistry get() {
        return INSTANCE;
    }

    public CloudRecipeRegistry(ItemRegistry registry) {
        this.itemRegistry = registry;
        try {
            loadFromFile(Paths.get(Thread.currentThread().getContextClassLoader().getResource("data/recipes.json").toURI()));
        } catch (URISyntaxException e) {
            throw new RegistryException("Unable to load recipes.json", e);
        }

    }

    @Override
    public void close() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("Registry already closed!");
        }
        this.closed = true;
        log.info("Loaded {}{}{} recipies.", TextFormat.GREEN, recipeMap.size(), TextFormat.RESET);
        this.rebuildPacket();
    }

    @Override
    public void unregister(Identifier id) {
        unregister(recipeMap.get(id));
    }


    @Override
    public void unregister(Recipe recipe) {
        if (this.closed) {
            throw new RegistryException("Unable to unregister recipes after registry closes");
        }
        int outputHash = ItemUtils.getItemHash((CloudItemStack) recipe.getResult());
        UUID id = getInputHash(recipe);
        var hashMap = recipeHashMap.get(outputHash);
        if (hashMap.size() <= 1) {
            recipeHashMap.remove(outputHash);
        } else {
            hashMap.remove(id);
        }

        recipeMap.remove(recipe.getId());


    }

    @Override
    public void register(Recipe recipe) throws RegistryException {
        if (recipeMap.containsKey(recipe.getId())) {
            log.warn("Recipe with Identifier {} already registered! Skipping.", recipe.getId());
        }

        int outputHash = ItemUtils.getItemHash((CloudItemStack) recipe.getResult());
        UUID id = getInputHash(recipe);
        register0(recipe.getId(), id, outputHash, recipe);

    }

    private void register0(Identifier id, UUID uuid, int outputHash, Recipe recipe) {
        Map<UUID, Identifier> map = recipeHashMap.computeIfAbsent(outputHash, (x) -> new HashMap<>());
        map.put(uuid, id);
        this.recipeMap.put(id, recipe);
    }

    public void loadFromFile(Path file) {
        log.info("Loading recipes...");
        JsonNode json;
        int unlabeled = 0;

        try {
            json = Bootstrap.JSON_MAPPER.readTree(file.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read JSON File to load recipes",e);
        }

        if(json == null) {
            log.warn("Unable to load JSON file: {}", file);
            return;
        }

        //Load Recipes
        for (JsonNode recipe : json.get("recipes")) {
            Identifier id;
            if (recipe.has("id")) {
                id = Identifier.fromString(recipe.get("id").asText());
            } else {
                id = Identifier.fromString(UNLABELED_PREFIX + (++unlabeled));
            }

            switch (recipe.get("type").asInt()) {
                case 0: // Shapeless
                    String craftingBlock = recipe.get("block").asText();
                    Identifier block = craftingBlock == null ? null : Identifier.fromString(craftingBlock);

                    List<ItemStack> outputs = new ArrayList<>();
                    for(Map<String, Object> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("output"), new TypeReference<List<Map<String, Object>>>(){})) {
                        outputs.add(createRecipeItem(item));
                    }

                    List<ItemStack> inputs = new ArrayList<>();
                    for(Map<String, Object> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("input"), new TypeReference<List<Map<String, Object>>>(){})) {
                        inputs.add(createRecipeItem(item));
                    }
                    inputs.sort(recipeComparator);

                    this.register(new ShapelessRecipe(id, recipe.get("priority").asInt(),outputs,inputs,block));
                    break;
                case 1: //Shaped
                    String[] shape = Bootstrap.JSON_MAPPER.convertValue(recipe.get("shape"), new TypeReference<String[]>() {});
                    craftingBlock = recipe.get("block").asText();
                    block = craftingBlock == null ? null : Identifier.fromString(craftingBlock);

                    outputs = new ArrayList<>();
                    for(Map<String, Object> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("output"), new TypeReference<List<Map<String, Object>>>(){})) {
                        outputs.add(createRecipeItem(item));
                    }
                    ItemStack primary = outputs.remove(0);

                    CharObjectMap<ItemStack> ingredients = new CharObjectHashMap<>();
                    for(Map.Entry<String, Map<String, Object>> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("input"), new TypeReference<Map<String, Map<String, Object>>>(){}).entrySet()) {
                        ingredients.put(item.getKey().charAt(0), createRecipeItem(item.getValue()));

                    }
                    this.register(new ShapedRecipe(id, recipe.get("priority").asInt(), primary, shape, ingredients, outputs, block));
                    break;
                case 2:
                case 3: //furnace
                    Map<String, Object> outputData = Bootstrap.JSON_MAPPER.convertValue(recipe.get("output"), new TypeReference<Map<String, Object>>() {});
                    Map<String, Object> inputData = Bootstrap.JSON_MAPPER.convertValue(recipe.get("input"), new TypeReference<Map<String, Object>>() {});
                    craftingBlock = recipe.get("block").asText();
                    block = craftingBlock == null ? null : Identifier.fromString(craftingBlock);

                    this.register(new FurnaceRecipe(id,createRecipeItem(outputData),createRecipeItem(inputData),block));
                default:
                    log.debug("Skipping recipie with unimplemented type");
                    break; // unsupported type
            }
        }

        //Load Potions
        unlabeled = 0;
        for( JsonNode recipe : json.get("potionMixes")) {
            ItemStack input = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("inputId").asText()),recipe.get("inputMeta").shortValue(),1, NbtMap.EMPTY);
            ItemStack reagent = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("reagentId").asText()),recipe.get("reagentMeta").shortValue(),1, NbtMap.EMPTY);
            ItemStack output = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("outputId").asText()),recipe.get("outputMeta").shortValue(),1, NbtMap.EMPTY);

            Identifier id = Identifier.fromString(UNLABELED_POTION_PREFIX+ (++unlabeled));
            this.register(new BrewingRecipe(id, input,reagent,output));
        }

        //Load Container Mixes
        unlabeled = 0;
        for( JsonNode recipe : json.get("containerMixes")) {
            ItemStack input = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("inputId").asText()), (short) 0,1, NbtMap.EMPTY);
            ItemStack reagent = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("reagentId").asText()), (short) 0,1, NbtMap.EMPTY);
            ItemStack output = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("outputId").asText()), (short) 0,1, NbtMap.EMPTY);

            Identifier id = Identifier.fromString(UNLABELED_CONTAINER_PREFIX+ (++unlabeled));
            this.register(new ContainerRecipe(id, input, reagent, output));
        }
    }

    private RecipeItemStack createRecipeItem(Map<String, Object> json) {
        int damage = Utils.toInt(json.getOrDefault("damage", 0));
        if ((damage & 0x7FFF) == 0x7FFF) damage = -1;
        return new RecipeItemStack(ItemUtils.fromJson(json), damage != -1);
    }

    @Override
    public Recipe getRecipe(Identifier identifier) {
        return this.recipeMap.get(identifier);
    }

    @Override
    public Recipe matchRecipe(ItemStack[][] inputMap, ItemStack output, ItemStack[][] extraOutputMap, Identifier craftingBlock) {
        int key = ItemUtils.getItemHash((CloudItemStack) output);
        if( !recipeHashMap.containsKey(key)) {
            return null;
        }

        Map<UUID, Identifier> map = recipeHashMap.get(key);
        List<ItemStack> inputs = new ArrayList<>();
        for(ItemStack[] row : inputMap) inputs.addAll(Arrays.asList(row));

        if(craftingBlock != null) {
            if(craftingBlock == Identifiers.FURNACE || craftingBlock == Identifiers.LIT_FURNACE
                    || craftingBlock == Identifiers.BLAST_FURNACE || craftingBlock == Identifiers.LIT_BLAST_FURNACE )
                return matchFurnaceRecipe(inputs.get(0), output, craftingBlock);
            if(craftingBlock == Identifiers.BREWING_STAND)
                return matchBrewingRecipe(inputs.get(0), output);
        }

        //Shaped recipies don't have inputs sorted
        UUID id = ItemUtils.getMultiItemHash(inputs);
        if(map.containsKey(id)) {
            CraftingRecipe recipe = (CraftingRecipe) recipeMap.get(map.get(id));
            if(recipe.matchItems(inputMap.clone(), extraOutputMap.clone() )) {
                if(recipe.requiresCraftingTable() && craftingBlock != recipe.getBlock()) return null;
                return recipe;
            }
        }

        //Shapeless needs inputs sorted before hash
        inputs.sort(recipeComparator);
        id = ItemUtils.getMultiItemHash(inputs);
        if(map.containsKey(id)) {
            CraftingRecipe recipe = (CraftingRecipe) recipeMap.get(map.get(id));
            if(recipe.matchItems(inputMap.clone(), extraOutputMap.clone())) {
                if(recipe.requiresCraftingTable() && craftingBlock != recipe.getBlock()) return null;
                return recipe;
            }
        }
        return null;
    }

    public FurnaceRecipe matchFurnaceRecipe(ItemStack input, ItemStack output, Identifier craftingBlock) {
        int hash = ItemUtils.getItemHash((CloudItemStack) output);

        if (craftingBlock == BlockIds.LIT_SMOKER) {
            craftingBlock = BlockIds.SMOKER;
        } else if (craftingBlock == LIT_BLAST_FURNACE) {
            craftingBlock = BlockIds.BLAST_FURNACE;
        } else if (craftingBlock == BlockIds.LIT_FURNACE) {
            craftingBlock = BlockIds.FURNACE;
        }

        if(!recipeHashMap.containsKey(hash)) return null;

        Map<UUID, Identifier> map = recipeHashMap.get(hash);
        UUID id = ItemUtils.getMultiItemHash(Arrays.asList(input, output));

        Recipe recipe = recipeMap.get(map.get(id));
        if(recipe != null && recipe.getBlock() == craftingBlock
                     && recipe.getResult().equals(output) && ((FurnaceRecipe)recipe).getInput().equals(input))
            return (FurnaceRecipe) recipe;

        return null;
    }

    public FurnaceRecipe matchFurnaceRecipe(ItemStack input, Identifier craftingBlock) {
        if (craftingBlock == BlockIds.LIT_SMOKER) {
            craftingBlock = BlockIds.SMOKER;
        } else if (craftingBlock == LIT_BLAST_FURNACE) {
            craftingBlock = BlockIds.BLAST_FURNACE;
        } else if (craftingBlock == BlockIds.LIT_FURNACE) {
            craftingBlock = BlockIds.FURNACE;
        }

        for(Recipe recipe : this.recipeMap.values()) {
            if(recipe.getBlock() != craftingBlock)
                continue;

            if(recipe instanceof FurnaceRecipe && ((FurnaceRecipe) recipe).getInput().equals(input)) {
                return (FurnaceRecipe) recipe;
            }
        }
        return null;
    }

    public MixRecipe matchBrewingRecipe(ItemStack input, ItemStack potion) {
        int hash = ItemUtils.getItemHash((CloudItemStack) potion);
        if(!recipeHashMap.containsKey(hash)) return null;

        Map<UUID, Identifier> map = recipeHashMap.get(hash);
        UUID id = ItemUtils.getMultiItemHash(Arrays.asList(input, potion));

        Recipe recipe = recipeMap.get(map.get(id));
        if(recipe != null && recipe.getResult().equals(potion) && ((MixRecipe)recipe).getInput().equals(input))
            return (MixRecipe) recipe;

        return null;
    }

    @Override
    public Collection<Recipe> getRecipes() {
        return ImmutableList.copyOf(this.recipeMap.values());
    }

    public CraftingDataPacket getNetworkData() {
        if(cached != null) {
            return cached;
        }
        rebuildPacket();
        return cached;
    }

    private void rebuildPacket() {
        CraftingDataPacket packet = new CraftingDataPacket();
        packet.setCleanRecipes(true);
        int recipeId = 0;

        for( Map<UUID,Identifier> map : recipeHashMap.values()) {
            for (Map.Entry<UUID, Identifier> entry : map.entrySet()) {
                Recipe recipe = recipeMap.get(entry.getValue());
                switch (recipe.getType()) {
                    case SHAPELESS:
                        packet.getCraftingData().add(CraftingData.fromShapeless(
                                recipe.getId().toString(),
                                ItemUtils.toNetwork(((ShapelessRecipe) recipe).getIngredientList()),
                                ItemUtils.toNetwork(((ShapelessRecipe) recipe).getAllResults()),
                                entry.getKey(),
                                recipe.getBlock().getName(),
                                ((ShapelessRecipe) recipe).getPriority(),
                                ++recipeId));
                        break;
                    case SHAPED:
                        packet.getCraftingData().add(CraftingData.fromShaped(
                                recipe.getId().toString(),
                                ((ShapedRecipe) recipe).getWidth(),
                                ((ShapedRecipe) recipe).getHeight(),
                                ItemUtils.toNetwork(((ShapedRecipe) recipe).getIngredientList()),
                                ItemUtils.toNetwork(((ShapedRecipe) recipe).getAllResults()),
                                entry.getKey(),
                                recipe.getBlock().getName(),
                                ((ShapedRecipe) recipe).getPriority(),
                                ++recipeId));
                        break;
                    case FURNACE:
                        assert recipe instanceof FurnaceRecipe;
                        ItemData inputData = ((CloudItemStack) ((FurnaceRecipe) recipe).getInput()).getNetworkData();
                        ItemData outputData = ((CloudItemStack) recipe.getResult()).getNetworkData();

                        packet.getCraftingData().add(CraftingData.fromFurnace(
                                inputData.getId(),
                                outputData,
                                recipe.getBlock().getName(),
                                ++recipeId));
                        break;
                    case FURNACE_DATA:
                        assert recipe instanceof FurnaceRecipe;
                        inputData = ((CloudItemStack) ((FurnaceRecipe) recipe).getInput()).getNetworkData();
                        outputData = ((CloudItemStack) recipe.getResult()).getNetworkData();

                        packet.getCraftingData().add(CraftingData.fromFurnaceData(
                                ((CloudItemStack) ((FurnaceRecipe) recipe).getInput()).getNetworkData().getId(),
                                inputData.getDamage(),
                                outputData,
                                recipe.getBlock().getName(),
                                ++recipeId));
                        break;
                    case POTION:
                        assert recipe instanceof BrewingRecipe;
                        ItemData reagentData = ((CloudItemStack) ((BrewingRecipe) recipe).getIngredient()).getNetworkData();
                        inputData = ((CloudItemStack) ((BrewingRecipe) recipe).getInput()).getNetworkData();
                        outputData = ((CloudItemStack) recipe.getResult()).getNetworkData();

                        packet.getPotionMixData().add(new PotionMixData(
                                inputData.getId(),
                                inputData.getDamage(),
                                reagentData.getId(),
                                reagentData.getDamage(),
                                outputData.getId(),
                                outputData.getDamage()));
                        break;
                    case CONTAINER:
                        assert recipe instanceof ContainerRecipe;
                        reagentData = ((CloudItemStack) ((ContainerRecipe) recipe).getIngredient()).getNetworkData();
                        inputData = ((CloudItemStack) ((ContainerRecipe) recipe).getInput()).getNetworkData();
                        outputData = ((CloudItemStack) recipe.getResult()).getNetworkData();

                        packet.getContainerMixData().add(new ContainerMixData(
                                inputData.getId(),
                                reagentData.getId(),
                                outputData.getId()));
                        break;
                    default:
                        break;
                }
            }
        }

        this.cached = packet;
    }

    private UUID getInputHash(Recipe recipe) {
        switch (recipe.getType()) {
            case SHAPED:
            case SHAPED_CHEMISTRY:
                return ItemUtils.getMultiItemHash(((ShapedRecipe) recipe).getIngredientList());
            case SHAPELESS:
            case SHAPELESS_CHEMISTRY:
                List<ItemStack> list = ((ShapelessRecipe) recipe).getIngredientList();
                list.sort(recipeComparator);
                return ItemUtils.getMultiItemHash(list);
            case FURNACE:
            case FURNACE_DATA:
                ItemStack input = ((FurnaceRecipe) recipe).getInput();
                return ItemUtils.getMultiItemHash(Arrays.asList(input, recipe.getResult()));
            case POTION:
            case CONTAINER:
                return ItemUtils.getMultiItemHash(Arrays.asList(((MixRecipe) recipe).getInput(), ((MixRecipe) recipe).getIngredient()));
        }
        return null;
    }
}
