package org.cloudburstmc.server.registry;

import com.google.common.collect.ImmutableList;
import io.netty.util.collection.CharObjectHashMap;
import io.netty.util.collection.CharObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.crafting.MixRecipe;
import org.cloudburstmc.api.crafting.Recipe;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.crafting.RecipeUnlockContext;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.registry.RecipeRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.ContainerMixData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.RecipeUnlockingRequirement;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.TrimDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.UnlockedRecipesPacket;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.crafting.*;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.utils.Utils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.cloudburstmc.api.block.BlockIds.LIT_BLAST_FURNACE;

@Log4j2
public class CloudRecipeRegistry implements RecipeRegistry {

    public static final Comparator<ItemStack> recipeComparator = Comparator
            .comparing((ItemStack i) -> i.isEmpty() || i.getType() == null ? "" : i.getType().getId().toString())
            .thenComparingInt(i -> i.isEmpty() || i.getType() == null ? 0 : ItemUtils.toNetwork(i).getDamage())
            .thenComparingInt(ItemStack::getCount);

    private static final String UNLABELED_PREFIX = "minecraft:crafting_recipe_";
    private static final String UNLABELED_POTION_PREFIX = "minecraft:potion_";
    private static final String UNLABELED_CONTAINER_PREFIX = "minecraft:container_";

    private static final int RECIPE_TYPE_SHAPELESS = 0;
    private static final int RECIPE_TYPE_SHAPED = 1;
    private static final int RECIPE_TYPE_COOKING = 3;
    private static final int RECIPE_TYPE_COMPLEX = 4;
    private static final int RECIPE_TYPE_SHULKER_BOX = 5;
    private static final int RECIPE_TYPE_SMITHING_TRANSFORM = 8;
    private static final int RECIPE_TYPE_SMITHING_TRIM = 9;

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Map<String, Object>>> MAP_OF_MAPS = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECTS = new TypeReference<>() {
    };
    private static final TypeReference<String[]> STRING_ARRAY = new TypeReference<>() {
    };

    private static final Map<Identifier, SmithingUpgrade> SMITHING_UPGRADE_BY_BASE;

    static {
        List<SmithingUpgrade> all = List.of(
                SmithingUpgrades.AXE, SmithingUpgrades.BOOTS, SmithingUpgrades.CHESTPLATE,
                SmithingUpgrades.HELMET, SmithingUpgrades.HOE, SmithingUpgrades.HORSE_ARMOR,
                SmithingUpgrades.LEGGINGS, SmithingUpgrades.NAUTILUS_ARMOR, SmithingUpgrades.PICKAXE,
                SmithingUpgrades.SHOVEL, SmithingUpgrades.SPEAR, SmithingUpgrades.SWORD
        );
        SMITHING_UPGRADE_BY_BASE = Map.copyOf(
                all.stream().collect(Collectors.toMap(SmithingUpgrade::base, u -> u))
        );
    }

    private static final CloudRecipeRegistry INSTANCE;

    static {
        INSTANCE = new CloudRecipeRegistry(CloudItemRegistry.get());
    }

    private final CloudItemRegistry itemRegistry;

    private final Map<Identifier, Recipe> recipeMap = new Object2ReferenceOpenHashMap<>();
    private final Int2ReferenceMap<Map<UUID, Identifier>> recipeHashMap = new Int2ReferenceOpenHashMap<>();

    private final Map<UUID, Identifier> uuidIndex = new HashMap<>();

    private final Map<Identifier, List<CloudFurnaceRecipe>> furnaceByBlock = new HashMap<>();
    private final Map<Identifier, List<CloudStonecuttingRecipe>> stonecuttingByInput = new HashMap<>();
    private final Map<Identifier, List<CloudBrewingRecipe>> brewingByBase = new HashMap<>();
    private final Map<Identifier, List<CloudContainerRecipe>> containerByBase = new HashMap<>();

    private final Reference2IntMap<Identifier> netIdMap = new Reference2IntOpenHashMap<>();
    private final Int2ReferenceMap<Identifier> idNetMap = new Int2ReferenceOpenHashMap<>();
    private final AtomicInteger netIdAllocator = new AtomicInteger(1);

    private final List<TrimPattern> trimPatterns = new ArrayList<>();
    private final List<TrimMaterial> trimMaterials = new ArrayList<>();
    private final List<SmithingUpgrade> smithingUpgrades = new ArrayList<>();

    private boolean closed;
    private CraftingDataPacket cached;
    private TrimDataPacket trimData;
    private UnlockedRecipesPacket unlockedRecipes;

    public CloudRecipeRegistry(ItemRegistry registry) {
        this.itemRegistry = (CloudItemRegistry) registry;
        try {
            loadFromFile(Thread.currentThread().getContextClassLoader().getResource("data/recipes.json").toURI());
        } catch (URISyntaxException | NullPointerException e) {
            throw new RegistryException("Unable to load data/recipes.json", e);
        }
        registerVanillaTrimData();
    }

    public static CloudRecipeRegistry get() {
        return INSTANCE;
    }

    @Override
    public void close() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("Registry already closed!");
        }
        this.closed = true;
        rebuildPacket();
        log.info("Loaded §a{}§r recipes.", recipeMap.size());
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

        UUID uuid = getInputHash(recipe);
        int outputHash = ItemUtils.getItemHash(recipe.getResult());

        uuidIndex.remove(uuid);

        Map<UUID, Identifier> hashMap = recipeHashMap.get(outputHash);
        if (hashMap != null) {
            if (hashMap.size() <= 1) {
                recipeHashMap.remove(outputHash);
            } else {
                hashMap.remove(uuid);
            }
        }

        recipeMap.remove(recipe.getId());
        int netId = netIdMap.removeInt(recipe.getId());
        idNetMap.remove(netId);
    }

    @Override
    public void register(Recipe recipe) throws RegistryException {
        if (recipeMap.containsKey(recipe.getId())) {
            log.debug("Recipe with Identifier §e{}§r already registered! Skipping.", recipe.getId());
            return;
        }

        int outputHash;
        UUID uuid = switch (recipe.getType()) {
            case COMPLEX -> {
                outputHash = Objects.hash(System.identityHashCode(recipe.getId()));
                yield ((CloudComplexRecipe) recipe).uuid();
            }
            case SMITHING_TRANSFORM, SMITHING_TRIM -> {
                outputHash = Objects.hash(System.identityHashCode(recipe.getId()));
                yield UUID.nameUUIDFromBytes(recipe.getId().toString().getBytes());
            }
            default -> {
                outputHash = ItemUtils.getItemHash(recipe.getResult());
                yield getInputHash(recipe);
            }
        };

        register0(recipe.getId(), uuid, outputHash, recipe);
        indexRecipe(recipe);
    }

    private void register0(Identifier id, UUID uuid, int outputHash, Recipe recipe) {
        Map<UUID, Identifier> map = recipeHashMap.computeIfAbsent(outputHash, x -> new HashMap<>());
        map.put(uuid, id);
        uuidIndex.put(uuid, id);
        this.recipeMap.put(id, recipe);
        int netId = this.netIdAllocator.getAndIncrement();
        this.netIdMap.putIfAbsent(id, netId);
        this.idNetMap.putIfAbsent(netId, id);
    }

    private void indexRecipe(Recipe recipe) {
        switch (recipe.getType()) {
            case COOKING -> {
                CloudFurnaceRecipe furnace = (CloudFurnaceRecipe) recipe;
                furnaceByBlock.computeIfAbsent(furnace.getBlock(), k -> new ArrayList<>()).add(furnace);
            }
            case STONECUTTING -> {
                CloudStonecuttingRecipe sc = (CloudStonecuttingRecipe) recipe;
                stonecuttingByInput.computeIfAbsent(sc.getInputItem().getType().getId(), k -> new ArrayList<>()).add(sc);
            }
            case POTION -> {
                CloudBrewingRecipe brewing = (CloudBrewingRecipe) recipe;
                brewingByBase.computeIfAbsent(brewing.getInput().getType().getId(), k -> new ArrayList<>()).add(brewing);
            }
            case POTION_CONTAINER -> {
                CloudContainerRecipe container = (CloudContainerRecipe) recipe;
                containerByBase.computeIfAbsent(container.getInput().getType().getId(), k -> new ArrayList<>()).add(container);
            }
            default -> { /* shapeless/shaped/smithing/complex need no additional index */ }
        }
    }

    @Override
    public void loadFromFile(URI file) {
        JsonNode json;
        int unlabeled = 0;

        try {
            json = Bootstrap.JSON_MAPPER.readTree(file.toURL().openStream());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read JSON File to load recipes", e);
        }

        if (json == null) {
            log.warn("Unable to load JSON file: {}", file);
            return;
        }

        for (JsonNode entry : json.get("recipes")) {
            Identifier id = entry.has("id")
                    ? Identifier.parse(entry.get("id").asString())
                    : Identifier.parse(UNLABELED_PREFIX + (++unlabeled));

            Identifier block = null;
            if (entry.has("block")) {
                String raw = entry.get("block").asString();
                if (raw != null) {
                    block = Identifier.parse(raw);
                }
            }

            int type = entry.get("type").asInt();
            switch (type) {
                case RECIPE_TYPE_SHAPELESS -> parseShapeless(id, entry, block, false);
                case RECIPE_TYPE_SHULKER_BOX -> parseShulkerBox(id, entry, block);
                case RECIPE_TYPE_SHAPED -> parseShaped(id, entry, block);
                case RECIPE_TYPE_COOKING -> parseCooking(id, entry, block);
                case RECIPE_TYPE_COMPLEX -> parseComplex(id, entry);
                case RECIPE_TYPE_SMITHING_TRANSFORM -> parseSmithingTransform(id, entry, block);
                case RECIPE_TYPE_SMITHING_TRIM -> parseSmithingTrim(entry, block);
                default -> log.warn("Unsupported recipe type {} for recipe {}", type, id);
            }
        }

        unlabeled = 0;
        for (JsonNode entry : json.get("potionMixes")) {
            Identifier id = Identifier.parse(UNLABELED_POTION_PREFIX + (++unlabeled));
            ItemStack input = ItemUtils.deserializeItem(Identifier.parse(entry.get("inputId").asString()), entry.get("inputMeta").shortValue(), 1, NbtMap.EMPTY);
            ItemStack reagent = ItemUtils.deserializeItem(Identifier.parse(entry.get("reagentId").asString()), entry.get("reagentMeta").shortValue(), 1, NbtMap.EMPTY);
            ItemStack output = ItemUtils.deserializeItem(Identifier.parse(entry.get("outputId").asString()), entry.get("outputMeta").shortValue(), 1, NbtMap.EMPTY);
            this.register(new CloudBrewingRecipe(id, input, reagent, output));
        }

        unlabeled = 0;
        for (JsonNode entry : json.get("containerMixes")) {
            Identifier id = Identifier.parse(UNLABELED_CONTAINER_PREFIX + (++unlabeled));
            ItemStack input = ItemUtils.deserializeItem(Identifier.parse(entry.get("inputId").asString()), (short) 0, 1, NbtMap.EMPTY);
            ItemStack reagent = ItemUtils.deserializeItem(Identifier.parse(entry.get("reagentId").asString()), (short) 0, 1, NbtMap.EMPTY);
            ItemStack output = ItemUtils.deserializeItem(Identifier.parse(entry.get("outputId").asString()), (short) 0, 1, NbtMap.EMPTY);
            this.register(new CloudContainerRecipe(id, input, reagent, output));
        }
    }

    private void parseShapeless(Identifier id, JsonNode entry, Identifier block, boolean isChemistry) {
        List<ItemStack> outputs = new ArrayList<>();
        for (Map<String, Object> item : toMapList(entry.get("output"))) {
            outputs.add(ItemUtils.fromJson(item));
        }

        List<ItemStack> inputs = new ArrayList<>();
        List<ItemDescriptorWithCount> inputDescriptors = new ArrayList<>();
        for (Map<String, Object> item : toMapList(entry.get("input"))) {
            inputDescriptors.add(ItemUtils.descriptorFromJson(item));
            inputs.add(ItemUtils.fromJson(item));
        }
        inputs.removeIf(ItemStack::isEmpty);
        inputs.sort(recipeComparator);

        RecipeType type = isChemistry ? RecipeType.SHAPELESS_CHEMISTRY : RecipeType.SHAPELESS;

        if (!isChemistry && block != null && block.getName().equals("stonecutter")) {
            ItemStack scInput = inputs.isEmpty() ? ItemStack.EMPTY : inputs.getFirst();
            ItemDescriptorWithCount scDescriptor = inputDescriptors.isEmpty() ? ItemDescriptorWithCount.EMPTY : inputDescriptors.getFirst();
            ItemStack scResult = outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst();
            this.register(new CloudStonecuttingRecipe(id, entry.get("priority").asInt(), scInput, scDescriptor, scResult));
            return;
        }

        CloudShapelessRecipe recipe = new CloudShapelessRecipe(id, entry.get("priority").asInt(), outputs, inputs, inputDescriptors, block, type);
        if (entry.has("unlockContext")) {
            recipe.setUnlockContext(RecipeUnlockContext.valueOf(entry.get("unlockContext").asString()));
        }
        this.register(recipe);
    }

    private void parseShulkerBox(Identifier id, JsonNode entry, Identifier block) {
        List<ItemStack> outputs = new ArrayList<>();
        for (Map<String, Object> item : toMapList(entry.get("output"))) {
            outputs.add(ItemUtils.fromJson(item));
        }

        List<ItemStack> inputs = new ArrayList<>();
        List<ItemDescriptorWithCount> inputDescriptors = new ArrayList<>();
        for (Map<String, Object> item : toMapList(entry.get("input"))) {
            inputDescriptors.add(ItemUtils.descriptorFromJson(item));
            inputs.add(ItemUtils.fromJson(item));
        }
        inputs.removeIf(ItemStack::isEmpty);
        inputs.sort(recipeComparator);

        this.register(new CloudShulkerBoxRecipe(id, entry.get("priority").asInt(), outputs, inputs, inputDescriptors, block));
    }

    private void parseShaped(Identifier id, JsonNode entry, Identifier block) {
        String[] shape = toStringArray(entry.get("shape"));

        List<ItemStack> outputs = new ArrayList<>();
        for (Map<String, Object> item : toMapList(entry.get("output"))) {
            outputs.add(ItemUtils.fromJson(item));
        }
        ItemStack primary = outputs.removeFirst();

        CharObjectMap<ItemStack> ingredients = new CharObjectHashMap<>();
        CharObjectMap<ItemDescriptorWithCount> ingredientDescs = new CharObjectHashMap<>();
        for (Map.Entry<String, Map<String, Object>> item : toMapOfMaps(entry.get("input")).entrySet()) {
            char key = item.getKey().charAt(0);
            ingredients.put(key, ItemUtils.fromJson(item.getValue()));
            ingredientDescs.put(key, ItemUtils.descriptorFromJson(item.getValue()));
        }

        CloudShapedRecipe recipe = new CloudShapedRecipe(id, entry.get("priority").asInt(), primary, shape, ingredients, ingredientDescs, outputs, block);
        if (entry.has("assumeSymetry")) {
            recipe.setAssumeSymmetry(entry.get("assumeSymetry").asBoolean());
        }

        if (entry.has("unlockContext")) {
            recipe.setUnlockContext(RecipeUnlockContext.valueOf(entry.get("unlockContext").asString()));
        }

        this.register(recipe);
    }

    private void parseCooking(Identifier id, JsonNode entry, Identifier block) {
        if (block == null) {
            return;
        }

        Map<String, Object> outputData = toMap(entry.get("output"));
        Map<String, Object> inputData = toMap(entry.get("input"));

        int rawInputDamage = Utils.toInt(inputData.getOrDefault("damage", 0));
        int wireDamage = rawInputDamage == -1 ? 32767 : rawInputDamage;

        int priority = entry.has("priority") ? entry.get("priority").asInt() : 0;
        this.register(new CloudFurnaceRecipe(
                id,
                ItemUtils.fromJson(outputData),
                ItemUtils.fromJson(inputData),
                ItemUtils.descriptorFromJson(inputData),
                block,
                priority,
                wireDamage));
    }

    private void parseComplex(Identifier id, JsonNode entry) {
        UUID uuid = UUID.fromString(entry.get("uuid").asString());
        this.register(new CloudComplexRecipe(id, uuid));
    }

    private void parseSmithingTransform(Identifier id, JsonNode entry, Identifier block) {
        Identifier smithingTable = block != null ? block : Identifier.parse("smithing_table");
        JsonNode input = entry.get("input");

        ItemDescriptorWithCount templateDesc = ItemUtils.descriptorFromJson(toMap(input.get("template")));
        ItemDescriptorWithCount baseDesc = ItemUtils.descriptorFromJson(toMap(input.get("base")));
        ItemDescriptorWithCount additionDesc = ItemUtils.descriptorFromJson(toMap(input.get("addition")));

        Map<String, Object> baseData = toMap(input.get("base"));
        Identifier baseId = baseData.containsKey("id")
                ? Identifier.parse(baseData.get("id").toString())
                : itemRegistry.fromLegacy(Utils.toInt(baseData.get("itemId")), 0);

        SmithingUpgrade upgrade = baseId != null ? SMITHING_UPGRADE_BY_BASE.get(baseId) : null;
        if (upgrade == null) {
            log.warn("No SmithingUpgrade found for base item {} — skipping smithing transform recipe.", baseId);
            return;
        }

        registerSmithingUpgrade(upgrade);
        ItemStack resultItem = ItemUtils.fromJson(toMap(entry.get("output")));
        this.register(new CloudSmithingTransformRecipe(upgrade.id(), templateDesc, baseDesc, additionDesc, resultItem, smithingTable));
    }

    private void parseSmithingTrim(JsonNode entry, Identifier block) {
        Identifier smithingTable = block != null ? block : Identifier.parse("smithing_table");
        JsonNode input = entry.get("input");

        ItemDescriptorWithCount templateDesc = ItemUtils.descriptorFromJson(toMap(input.get("template")));
        ItemDescriptorWithCount baseDesc = ItemUtils.descriptorFromJson(toMap(input.get("base")));
        ItemDescriptorWithCount additionDesc = ItemUtils.descriptorFromJson(toMap(input.get("addition")));

        this.register(new CloudSmithingTrimRecipe(Identifier.parse("minecraft:smithing_armor_trim"), templateDesc, baseDesc, additionDesc, smithingTable));
    }

    public int getRecipeNetId(Identifier id) {
        return netIdMap.getOrDefault(id, -1);
    }

    public int getRecipeNetId(Recipe recipe) {
        return getRecipeNetId(recipe.getId());
    }

    public Recipe getRecipeFromNetId(int netId) {
        return this.recipeMap.get(idNetMap.get(netId));
    }

    @Nullable
    @Override
    public Recipe getRecipe(Identifier identifier) {
        return this.recipeMap.get(identifier);
    }

    @Nullable
    @Override
    public Recipe getRecipe(UUID uuid) {
        Identifier id = uuidIndex.get(uuid);
        return id != null ? recipeMap.get(id) : null;
    }

    @Override
    public Recipe matchRecipe(ItemStack[][] inputMap, ItemStack output, ItemStack[][] extraOutputMap, Identifier craftingBlock) {
        int key = ItemUtils.getItemHash(output);
        if (!recipeHashMap.containsKey(key)) {
            return null;
        }

        if (craftingBlock != null) {
            if (craftingBlock == Identifiers.FURNACE
                    || craftingBlock == Identifiers.LIT_FURNACE
                    || craftingBlock == Identifiers.BLAST_FURNACE
                    || craftingBlock == Identifiers.LIT_BLAST_FURNACE) {
                List<ItemStack> flat = flattenGrid(inputMap);
                return matchFurnaceRecipe(flat.getFirst(), output, craftingBlock);
            }

            if (craftingBlock == Identifiers.BREWING_STAND) {
                List<ItemStack> flat = flattenGrid(inputMap);
                return matchBrewingRecipe(flat.getFirst(), output);
            }
        }

        Map<UUID, Identifier> map = recipeHashMap.get(key);
        for (Identifier recipeId : map.values()) {
            Recipe candidate = recipeMap.get(recipeId);
            if (candidate instanceof CloudShapedRecipe shaped && shaped.matchItems(inputMap.clone(), extraOutputMap.clone())) {
                if (shaped.requiresCraftingTable() && craftingBlock != shaped.getBlock()) {
                    continue;
                }
                return shaped;
            }
            if (candidate instanceof CloudShapelessRecipe shapeless && shapeless.matchItems(inputMap.clone(), extraOutputMap.clone())) {
                if (shapeless.requiresCraftingTable() && craftingBlock != shapeless.getBlock()) {
                    continue;
                }
                return shapeless;
            }
        }

        return null;
    }

    public CloudFurnaceRecipe matchFurnaceRecipe(ItemStack input, ItemStack output, Identifier craftingBlock) {
        Identifier normalizedBlock = normalizeFurnaceBlock(craftingBlock);
        List<CloudFurnaceRecipe> candidates = furnaceByBlock.get(normalizedBlock);
        if (candidates == null) {
            return null;
        }

        for (CloudFurnaceRecipe furnace : candidates) {
            if (furnace.getResult().equals(output) && furnace.getInputItem().equals(input)) {
                return furnace;
            }
        }

        return null;
    }

    public CloudFurnaceRecipe matchFurnaceRecipe(ItemStack input, Identifier craftingBlock) {
        Identifier normalizedBlock = normalizeFurnaceBlock(craftingBlock);
        List<CloudFurnaceRecipe> candidates = furnaceByBlock.get(normalizedBlock);
        if (candidates == null) {
            return null;
        }

        for (CloudFurnaceRecipe furnace : candidates) {
            if (furnace.getInputItem().equals(input)) {
                return furnace;
            }
        }

        return null;
    }

    @Override
    public Recipe matchRecipe(ItemStack input, Identifier craftingBlock) {
        if (craftingBlock != null && craftingBlock.getName().equals("stonecutter")) {
            return matchStonecuttingRecipe(input);
        }
        return matchFurnaceRecipe(input, craftingBlock);
    }

    public CloudStonecuttingRecipe matchStonecuttingRecipe(ItemStack input) {
        List<CloudStonecuttingRecipe> candidates = stonecuttingByInput.get(input.getType().getId());
        if (candidates == null) {
            return null;
        }

        for (CloudStonecuttingRecipe sc : candidates) {
            if (sc.getInputItem().equals(input)) {
                return sc;
            }
        }

        return null;
    }

    @Override
    public MixRecipe matchBrewingRecipe(ItemStack ingredient, ItemStack basePotion) {
        Identifier baseId = basePotion.getType().getId();

        List<CloudBrewingRecipe> brewing = brewingByBase.get(baseId);
        if (brewing != null) {
            for (CloudBrewingRecipe mix : brewing) {
                if (mix.getInput().equals(basePotion) && mix.getIngredient().equals(ingredient)) {
                    return mix;
                }
            }
        }

        List<CloudContainerRecipe> containers = containerByBase.get(baseId);
        if (containers != null) {
            for (CloudContainerRecipe mix : containers) {
                if (mix.getInput().equals(basePotion) && mix.getIngredient().equals(ingredient)) {
                    return mix;
                }
            }
        }

        return null;
    }

    @Override
    public Collection<Recipe> getRecipes() {
        return ImmutableList.copyOf(this.recipeMap.values());
    }

    @Override
    public Collection<Recipe> getRecipes(RecipeType type) {
        ImmutableList.Builder<Recipe> builder = ImmutableList.builder();
        for (Recipe recipe : this.recipeMap.values()) {
            if (recipe.getType() == type) {
                builder.add(recipe);
            }
        }
        return builder.build();
    }

    @Override
    public Collection<Recipe> getRecipesByStation(Identifier craftingBlock) {
        ImmutableList.Builder<Recipe> builder = ImmutableList.builder();
        for (Recipe recipe : this.recipeMap.values()) {
            if (craftingBlock.equals(recipe.getBlock())) {
                builder.add(recipe);
            }
        }
        return builder.build();
    }

    @Override
    public Collection<Recipe> getRecipesForInput(ItemStack input, Identifier craftingBlock) {
        ImmutableList.Builder<Recipe> builder = ImmutableList.builder();
        Identifier inputId = input.getType().getId();

        for (Recipe recipe : this.recipeMap.values()) {
            if (!craftingBlock.equals(recipe.getBlock())) {
                continue;
            }

            switch (recipe) {
                case CloudFurnaceRecipe r when r.getInputItem().getType().getId().equals(inputId) -> builder.add(r);
                case CloudStonecuttingRecipe r when r.getInputItem().getType().getId().equals(inputId) ->
                        builder.add(r);
                case CloudShapelessRecipe r -> {
                    for (ItemStack ingredient : r.getIngredientList()) {
                        if (ingredient.getType().getId().equals(inputId)) {
                            builder.add(r);
                            break;
                        }
                    }
                }
                case CloudShapedRecipe r -> {
                    for (ItemStack ingredient : r.getIngredientList()) {
                        if (ingredient.getType().getId().equals(inputId)) {
                            builder.add(r);
                            break;
                        }
                    }
                }
                default -> {
                }
            }
        }
        return builder.build();
    }

    public CraftingDataPacket getNetworkData() {
        if (cached == null) {
            rebuildPacket();
        }
        return cached;
    }

    public TrimDataPacket getTrimData() {
        if (trimData == null) {
            rebuildPacket();
        }
        return trimData;
    }

    public UnlockedRecipesPacket getUnlockedRecipesPacket() {
        if (unlockedRecipes == null) {
            rebuildPacket();
        }
        return unlockedRecipes;
    }

    @Override
    public void registerTrimPattern(TrimPattern pattern) {
        trimPatterns.add(pattern);
        trimData = null;
    }

    @Override
    public void registerTrimMaterial(TrimMaterial material) {
        trimMaterials.add(material);
        trimData = null;
    }

    @Override
    public Collection<TrimPattern> getTrimPatterns() {
        return ImmutableList.copyOf(trimPatterns);
    }

    @Override
    public Collection<TrimMaterial> getTrimMaterials() {
        return ImmutableList.copyOf(trimMaterials);
    }

    @Override
    public void registerSmithingUpgrade(SmithingUpgrade upgrade) {
        smithingUpgrades.add(upgrade);
    }

    @Override
    public Collection<SmithingUpgrade> getSmithingUpgrades() {
        return ImmutableList.copyOf(smithingUpgrades);
    }

    @Override
    public Collection<Recipe> getRecipesFor(ItemStack result) {
        return recipeMap.values().stream()
                .filter(r -> r.getResult().isSimilarMetadata(result))
                .collect(ImmutableList.toImmutableList());
    }

    private void registerVanillaTrimData() {
        trimPatterns.add(TrimPatterns.WARD);
        trimPatterns.add(TrimPatterns.SENTRY);
        trimPatterns.add(TrimPatterns.SNOUT);
        trimPatterns.add(TrimPatterns.DUNE);
        trimPatterns.add(TrimPatterns.SPIRE);
        trimPatterns.add(TrimPatterns.TIDE);
        trimPatterns.add(TrimPatterns.WILD);
        trimPatterns.add(TrimPatterns.RIB);
        trimPatterns.add(TrimPatterns.COAST);
        trimPatterns.add(TrimPatterns.SHAPER);
        trimPatterns.add(TrimPatterns.EYE);
        trimPatterns.add(TrimPatterns.VEX);
        trimPatterns.add(TrimPatterns.SILENCE);
        trimPatterns.add(TrimPatterns.WAYFINDER);
        trimPatterns.add(TrimPatterns.RAISER);
        trimPatterns.add(TrimPatterns.HOST);
        trimPatterns.add(TrimPatterns.FLOW);
        trimPatterns.add(TrimPatterns.BOLT);

        trimMaterials.add(TrimMaterials.QUARTZ);
        trimMaterials.add(TrimMaterials.IRON);
        trimMaterials.add(TrimMaterials.NETHERITE);
        trimMaterials.add(TrimMaterials.REDSTONE);
        trimMaterials.add(TrimMaterials.COPPER);
        trimMaterials.add(TrimMaterials.GOLD);
        trimMaterials.add(TrimMaterials.EMERALD);
        trimMaterials.add(TrimMaterials.DIAMOND);
        trimMaterials.add(TrimMaterials.LAPIS);
        trimMaterials.add(TrimMaterials.AMETHYST);
        trimMaterials.add(TrimMaterials.RESIN);
    }

    private void rebuildPacket() {
        CraftingDataPacket packet = new CraftingDataPacket();
        packet.setCleanRecipes(true);

        for (Map<UUID, Identifier> bucket : recipeHashMap.values()) {
            for (Map.Entry<UUID, Identifier> entry : bucket.entrySet()) {
                Recipe recipe = recipeMap.get(entry.getValue());

                int netId = netIdMap.getOrDefault(recipe.getId(), 0);
                if (netId < 1 && recipe.getType() != RecipeType.POTION && recipe.getType() != RecipeType.POTION_CONTAINER) {
                    log.warn("Recipe {} has no valid net ID assigned — skipping from packet.", recipe.getId());
                    continue;
                }

                String blockTag = recipe.getBlock() != null ? recipe.getBlock().getName() : "";
                switch (recipe.getType()) {
                    case SHAPELESS, SHAPELESS_CHEMISTRY ->
                            addShapelessToPacket(packet, (CloudShapelessRecipe) recipe, entry.getKey(), blockTag, netId);
                    case STONECUTTING ->
                            addStonecuttingToPacket(packet, (CloudStonecuttingRecipe) recipe, entry.getKey(), blockTag, netId);
                    case SHULKER_BOX ->
                            addShulkerBoxToPacket(packet, (CloudShapelessRecipe) recipe, entry.getKey(), blockTag, netId);
                    case SHAPED, SHAPED_CHEMISTRY ->
                            addShapedToPacket(packet, (CloudShapedRecipe) recipe, entry.getKey(), blockTag, netId);
                    case COOKING -> packet.getCraftingData().add(((CloudFurnaceRecipe) recipe).toNetwork());
                    case COMPLEX ->
                            packet.getCraftingData().add(MultiRecipeData.of(((CloudComplexRecipe) recipe).uuid(), netId));
                    case SMITHING_TRANSFORM ->
                            addSmithingTransformToPacket(packet, (CloudSmithingTransformRecipe) recipe, blockTag, netId);
                    case SMITHING_TRIM ->
                            addSmithingTrimToPacket(packet, (CloudSmithingTrimRecipe) recipe, blockTag, netId);
                    case POTION -> addBrewingToPacket(packet, (CloudBrewingRecipe) recipe);
                    case POTION_CONTAINER -> addContainerToPacket(packet, (CloudContainerRecipe) recipe);
                    default -> {
                    }
                }
            }
        }

        this.cached = packet;
        this.trimData = buildTrimDataPacket();
        this.unlockedRecipes = buildUnlockedRecipesPacket();
    }

    private void addShapelessToPacket(CraftingDataPacket packet, CloudShapelessRecipe recipe, UUID uuid, String blockTag, int netId) {
        List<ItemDescriptorWithCount> descriptors = resolveDescriptors(recipe);
        if (descriptors.isEmpty() || descriptors.stream().allMatch(d -> d == ItemDescriptorWithCount.EMPTY)) {
            log.warn("Recipe {} has all-empty ingredient descriptors — skipping from packet.", recipe.getId());
            return;
        }

        if (recipe.getType() == RecipeType.SHAPELESS_CHEMISTRY) {
            packet.getCraftingData().add(ShapelessRecipeData.shapelessChemistry(
                    recipe.getId().toString(),
                    descriptors,
                    ItemUtils.toNetworkRecipe(recipe.getAllResults()),
                    uuid,
                    blockTag,
                    recipe.getPriority(),
                    netId));
        } else {
            packet.getCraftingData().add(ShapelessRecipeData.shapeless(
                    recipe.getId().toString(),
                    descriptors,
                    ItemUtils.toNetworkRecipe(recipe.getAllResults()),
                    uuid,
                    blockTag,
                    recipe.getPriority(),
                    netId,
                    toProtocolRequirement(recipe.getUnlockContext())));
        }
    }

    private void addStonecuttingToPacket(CraftingDataPacket packet, CloudStonecuttingRecipe recipe, UUID uuid, String blockTag, int netId) {
        packet.getCraftingData().add(ShapelessRecipeData.shapeless(
                recipe.getId().toString(),
                ItemUtils.toRecipeDescriptors(recipe.getInputDescriptors()),
                ItemUtils.toNetworkRecipe(List.of(recipe.getResult())),
                uuid,
                blockTag,
                recipe.getPriority(),
                netId,
                RecipeUnlockingRequirement.INVALID));
    }

    private void addShulkerBoxToPacket(CraftingDataPacket packet, CloudShapelessRecipe recipe, UUID uuid, String blockTag, int netId) {
        packet.getCraftingData().add(ShapelessRecipeData.shulkerBox(
                recipe.getId().toString(),
                resolveDescriptors(recipe),
                ItemUtils.toNetworkRecipe(recipe.getAllResults()),
                uuid,
                blockTag,
                recipe.getPriority(),
                netId));
    }

    private void addShapedToPacket(CraftingDataPacket packet, CloudShapedRecipe recipe, UUID uuid, String blockTag, int netId) {
        List<ItemDescriptorWithCount> descriptors = resolveDescriptors(recipe);
        if (descriptors.isEmpty() || descriptors.stream().allMatch(d -> d == ItemDescriptorWithCount.EMPTY)) {
            log.warn("Recipe {} has all-empty ingredient descriptors — skipping from packet.", recipe.getId());
            return;
        }

        if (recipe.getType() == RecipeType.SHAPED_CHEMISTRY) {
            packet.getCraftingData().add(ShapedRecipeData.shapedChemistry(
                    recipe.getId().toString(),
                    recipe.getWidth(),
                    recipe.getHeight(),
                    descriptors,
                    ItemUtils.toNetworkRecipe(recipe.getAllResults()),
                    uuid,
                    blockTag,
                    recipe.getPriority(),
                    netId,
                    recipe.isAssumeSymmetry()));
        } else {
            packet.getCraftingData().add(ShapedRecipeData.shaped(
                    recipe.getId().toString(),
                    recipe.getWidth(),
                    recipe.getHeight(),
                    descriptors,
                    ItemUtils.toNetworkRecipe(recipe.getAllResults()),
                    uuid,
                    blockTag,
                    recipe.getPriority(),
                    netId,
                    recipe.isAssumeSymmetry(),
                    toProtocolRequirement(recipe.getUnlockContext())));
        }
    }

    private void addSmithingTransformToPacket(CraftingDataPacket packet, CloudSmithingTransformRecipe recipe, String blockTag, int netId) {
        packet.getCraftingData().add(SmithingTransformRecipeData.of(
                recipe.getId().toString(),
                ItemUtils.toRecipeDescriptors(List.of(recipe.getTemplateDescriptor())).getFirst(),
                ItemUtils.toRecipeDescriptors(List.of(recipe.getBaseDescriptor())).getFirst(),
                ItemUtils.toRecipeDescriptors(List.of(recipe.getAdditionDescriptor())).getFirst(),
                ItemUtils.toNetworkRecipe(recipe.getResult()),
                blockTag,
                netId));
    }

    private void addSmithingTrimToPacket(CraftingDataPacket packet, CloudSmithingTrimRecipe recipe, String blockTag, int netId) {
        packet.getCraftingData().add(SmithingTrimRecipeData.of(
                recipe.getId().toString(),
                recipe.getBaseDescriptor(),
                recipe.getAdditionDescriptor(),
                recipe.getTemplateDescriptor(),
                blockTag,
                netId));
    }

    private void addBrewingToPacket(CraftingDataPacket packet, CloudBrewingRecipe recipe) {
        ItemData reagentData = ItemUtils.toNetworkRecipe(recipe.getIngredient());
        ItemData inputData = ItemUtils.toNetworkRecipe(recipe.getInput());
        ItemData outputData = ItemUtils.toNetworkRecipe(recipe.getResult());
        packet.getPotionMixData().add(new PotionMixData(
                inputData.getDefinition().getRuntimeId(),
                inputData.getDamage(),
                reagentData.getDefinition().getRuntimeId(),
                reagentData.getDamage(),
                outputData.getDefinition().getRuntimeId(),
                outputData.getDamage()));
    }

    private void addContainerToPacket(CraftingDataPacket packet, CloudContainerRecipe recipe) {
        ItemData reagentData = ItemUtils.toNetworkRecipe(recipe.getIngredient());
        ItemData inputData = ItemUtils.toNetworkRecipe(recipe.getInput());
        ItemData outputData = ItemUtils.toNetworkRecipe(recipe.getResult());
        packet.getContainerMixData().add(new ContainerMixData(
                inputData.getDefinition().getRuntimeId(),
                reagentData.getDefinition().getRuntimeId(),
                outputData.getDefinition().getRuntimeId()));
    }

    private TrimDataPacket buildTrimDataPacket() {
        TrimDataPacket packet = new TrimDataPacket();
        for (TrimPattern p : trimPatterns) {
            packet.getPatterns().add(new org.cloudburstmc.protocol.bedrock.data.TrimPattern(p.itemId().toString(), p.patternId()));
        }
        for (TrimMaterial m : trimMaterials) {
            packet.getMaterials().add(new org.cloudburstmc.protocol.bedrock.data.TrimMaterial(m.materialId(), m.color().legacyCode(), m.itemId().toString()));
        }
        return packet;
    }

    private UnlockedRecipesPacket buildUnlockedRecipesPacket() {
        UnlockedRecipesPacket packet = new UnlockedRecipesPacket();
        packet.setAction(UnlockedRecipesPacket.ActionType.INITIALLY_UNLOCKED);

        for (Recipe recipe : recipeMap.values()) {
            if (recipe.getType() != RecipeType.POTION && recipe.getType() != RecipeType.POTION_CONTAINER) {
                packet.getUnlockedRecipes().add(recipe.getId().toString());
            }
        }

        return packet;
    }

    private static List<Map<String, Object>> toMapList(JsonNode node) {
        return Bootstrap.JSON_MAPPER.convertValue(node, LIST_OF_MAPS);
    }

    private static Map<String, Map<String, Object>> toMapOfMaps(JsonNode node) {
        return Bootstrap.JSON_MAPPER.convertValue(node, MAP_OF_MAPS);
    }

    private static Map<String, Object> toMap(JsonNode node) {
        return Bootstrap.JSON_MAPPER.convertValue(node, MAP_OF_OBJECTS);
    }

    private static String[] toStringArray(JsonNode node) {
        return Bootstrap.JSON_MAPPER.convertValue(node, STRING_ARRAY);
    }

    private static List<ItemDescriptorWithCount> resolveDescriptors(CloudShapelessRecipe recipe) {
        List<ItemDescriptorWithCount> raw = recipe.getInputDescriptors();
        if (raw == null) {
            return ItemUtils.toDescriptorsRecipe(recipe.getIngredientList());
        }
        return ItemUtils.toRecipeDescriptors(raw);
    }

    private static List<ItemDescriptorWithCount> resolveDescriptors(CloudShapedRecipe recipe) {
        List<ItemDescriptorWithCount> raw = recipe.getInputDescriptorList();
        if (raw == null) {
            return ItemUtils.toDescriptorsRecipe(recipe.getIngredientList());
        }
        return ItemUtils.toRecipeDescriptors(raw);
    }

    private static UUID getInputHash(Recipe recipe) {
        return UUID.nameUUIDFromBytes(recipe.getId().toString().getBytes());
    }

    private static Identifier normalizeFurnaceBlock(Identifier block) {
        if (block == BlockIds.LIT_SMOKER) return BlockIds.SMOKER;
        if (block == LIT_BLAST_FURNACE) return BlockIds.BLAST_FURNACE;
        if (block == BlockIds.LIT_FURNACE) return BlockIds.FURNACE;
        return block;
    }

    private static List<ItemStack> flattenGrid(ItemStack[][] grid) {
        List<ItemStack> flat = new ArrayList<>();
        for (ItemStack[] row : grid) {
            flat.addAll(Arrays.asList(row));
        }
        return flat;
    }

    private static RecipeUnlockingRequirement toProtocolRequirement(RecipeUnlockContext context) {
        RecipeUnlockingRequirement.UnlockingContext protocolContext = RecipeUnlockingRequirement.UnlockingContext.valueOf(context.name());
        return new RecipeUnlockingRequirement(protocolContext);
    }
}
