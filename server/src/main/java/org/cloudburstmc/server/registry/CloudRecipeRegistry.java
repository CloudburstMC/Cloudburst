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
import org.cloudburstmc.api.crafting.*;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.potion.PotionTypes;
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
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.TrimDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.UnlockedRecipesPacket;
import org.cloudburstmc.server.ServerJson;
import org.cloudburstmc.server.crafting.*;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.item.ItemStackPacketCodec;
import org.cloudburstmc.server.item.RecipeItemCodec;
import org.cloudburstmc.server.item.serializer.ItemSerializer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class CloudRecipeRegistry implements RecipeRegistry {

    public static final Comparator<ItemStack> recipeComparator = Comparator
            .comparing((ItemStack i) -> i.isEmpty() ? "" : i.getType().getId().toString())
            .thenComparingInt(i -> i.isEmpty() ? 0 : ItemStackPacketCodec.toNetwork(i).getDamage())
            .thenComparingInt(ItemStack::getCount);

    private static final String VANILLA_RECIPES = "/data/recipes.json";

    private static final String UNLABELED_PREFIX = "minecraft:crafting_recipe_";
    private static final String UNLABELED_POTION_PREFIX = "minecraft:potion_";
    private static final String UNLABELED_CONTAINER_PREFIX = "minecraft:container_";

    private static final int RECIPE_TYPE_SHAPELESS = 0;
    private static final int RECIPE_TYPE_SHAPED = 1;
    private static final int RECIPE_TYPE_COMPLEX = 4;
    private static final int RECIPE_TYPE_SHULKER_BOX = 5;
    private static final int RECIPE_TYPE_SMITHING_TRANSFORM = 8;
    private static final int RECIPE_TYPE_SMITHING_TRIM = 9;

    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {};
    private static final TypeReference<Map<String, Map<String, Object>>> MAP_OF_MAPS = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECTS = new TypeReference<>() {};
    private static final TypeReference<String[]> STRING_ARRAY = new TypeReference<>() {};

    private static final Identifier TRIM_TEMPLATES = Identifier.parse("trim_templates");
    private static final Identifier TRIM_MATERIALS = Identifier.parse("trim_materials");

    private static final CloudRecipeRegistry INSTANCE;

    static {
        INSTANCE = new CloudRecipeRegistry(CloudItemRegistry.get());
    }

    private final CloudItemRegistry itemRegistry;

    private final Map<Identifier, Recipe> recipeMap = new Object2ReferenceOpenHashMap<>();
    private final Int2ReferenceMap<Map<UUID, Identifier>> recipeHashMap = new Int2ReferenceOpenHashMap<>();

    private final Map<UUID, Identifier> uuidIndex = new HashMap<>();

    private final Map<Identifier, List<CloudCookingRecipe>> cookingByBlock = new HashMap<>();
    private final Map<Identifier, List<CloudStonecuttingRecipe>> stonecuttingByInput = new HashMap<>();
    private final Map<Identifier, List<CloudBrewingRecipe>> brewingByBase = new HashMap<>();
    private final Map<Identifier, List<CloudContainerRecipe>> containerByBase = new HashMap<>();

    private final Reference2IntMap<Identifier> netIdMap = new Reference2IntOpenHashMap<>();
    private final Int2ReferenceMap<Identifier> idNetMap = new Int2ReferenceOpenHashMap<>();
    private final AtomicInteger netIdAllocator = new AtomicInteger(1);

    private final List<TrimPattern> trimPatterns = new ArrayList<>();
    private final List<TrimMaterial> trimMaterials = new ArrayList<>();
    private final List<SmithingRecipe> smithingRecipes = new ArrayList<>();

    private boolean closed;
    private CraftingDataPacket cached;
    private TrimDataPacket trimData;
    private UnlockedRecipesPacket unlockedRecipes;

    private CloudRecipeRegistry(CloudItemRegistry registry) {
        this.itemRegistry = registry;
        try (InputStream input = CloudRecipeRegistry.class.getResourceAsStream(VANILLA_RECIPES)) {
            if (input == null) {
                throw new RegistryException("Missing recipe resource: " + VANILLA_RECIPES);
            }
            this.loadRecipes(ServerJson.MAPPER.readTree(input));
        } catch (IOException e) {
            throw new RegistryException("Unable to read recipe resource: " + VANILLA_RECIPES, e);
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

        UUID uuid = recipeUuid(recipe);
        int outputHash = recipeOutputHash(recipe);

        uuidIndex.remove(uuid);

        Map<UUID, Identifier> hashMap = recipeHashMap.get(outputHash);
        if (hashMap != null) {
            hashMap.remove(uuid);
            if (hashMap.isEmpty()) {
                recipeHashMap.remove(outputHash);
            }
        }

        recipeMap.remove(recipe.getId());
        if (recipe instanceof SmithingRecipe smithing) {
            this.smithingRecipes.remove(smithing);
        }

        if (recipe instanceof CloudBrewingRecipe brewing) {
            removeMix(this.brewingByBase, brewing);
        } else if (recipe instanceof CloudContainerRecipe container) {
            removeMix(this.containerByBase, container);
        }

        if (recipe instanceof CloudCookingRecipe furnace) {
            cookingByBlock.computeIfPresent(normalizeFurnaceBlock(furnace.getBlock()), (_, recipes) -> {
                recipes.remove(furnace);
                return recipes.isEmpty() ? null : recipes;
            });
        }

        int netId = netIdMap.removeInt(recipe.getId());
        idNetMap.remove(netId);
    }

    private static <T extends MixRecipe> void removeMix(Map<Identifier, List<T>> byBase, T recipe) {
        byBase.computeIfPresent(recipe.getInput().getType().getId(), (_, recipes) -> {
            recipes.remove(recipe);
            return recipes.isEmpty() ? null : recipes;
        });
    }

    @Override
    public void register(Recipe recipe) throws RegistryException {
        if (recipeMap.containsKey(recipe.getId())) {
            log.debug("Recipe with Identifier §e{}§r already registered! Skipping.", recipe.getId());
            return;
        }

        register0(recipe.getId(), recipeUuid(recipe), recipeOutputHash(recipe), recipe);
        indexRecipe(recipe);
    }

    private static UUID recipeUuid(Recipe recipe) {
        return switch (recipe.getType()) {
            case COMPLEX -> ((CloudComplexRecipe) recipe).uuid();
            case SMITHING_TRANSFORM, SMITHING_TRIM ->
                    UUID.nameUUIDFromBytes(recipe.getId().toString().getBytes(StandardCharsets.UTF_8));
            default -> getInputHash(recipe);
        };
    }

    private static int recipeOutputHash(Recipe recipe) {
        return switch (recipe.getType()) {
            case COMPLEX, SMITHING_TRANSFORM, SMITHING_TRIM -> Objects.hash(System.identityHashCode(recipe.getId()));
            default -> RecipeItemCodec.getItemHash(recipe.getResult());
        };
    }

    private void register0(Identifier id, UUID uuid, int outputHash, Recipe recipe) {
        Map<UUID, Identifier> map = recipeHashMap.computeIfAbsent(outputHash, _ -> new HashMap<>());
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
                CloudCookingRecipe furnace = (CloudCookingRecipe) recipe;
                cookingByBlock.computeIfAbsent(normalizeFurnaceBlock(furnace.getBlock()), _ -> new ArrayList<>()).add(furnace);
            }
            case STONECUTTING -> {
                CloudStonecuttingRecipe sc = (CloudStonecuttingRecipe) recipe;
                stonecuttingByInput.computeIfAbsent(sc.getInputItem().getType().getId(), _ -> new ArrayList<>()).add(sc);
            }
            case POTION -> {
                CloudBrewingRecipe brewing = (CloudBrewingRecipe) recipe;
                brewingByBase.computeIfAbsent(brewing.getInput().getType().getId(), _ -> new ArrayList<>()).add(brewing);
            }
            case POTION_CONTAINER -> {
                CloudContainerRecipe container = (CloudContainerRecipe) recipe;
                containerByBase.computeIfAbsent(container.getInput().getType().getId(), _ -> new ArrayList<>()).add(container);
            }
            case SMITHING_TRANSFORM, SMITHING_TRIM -> smithingRecipes.add((SmithingRecipe) recipe);
            default -> { }
        }
    }

    @Override
    public void loadFromFile(URI file) {
        try (InputStream input = file.toURL().openStream()) {
            this.loadRecipes(ServerJson.MAPPER.readTree(input));
        } catch (IOException e) {
            throw new RegistryException("Unable to read recipes from " + file, e);
        }
    }

    private void loadRecipes(JsonNode json) {
        if (json == null || !json.isObject()) {
            throw new RegistryException("Expected a recipe data object");
        }

        int unlabeled = 0;
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
                case RECIPE_TYPE_SHAPELESS -> parseShapeless(id, entry, block);
                case RECIPE_TYPE_SHULKER_BOX -> parseShulkerBox(id, entry, block);
                case RECIPE_TYPE_SHAPED -> parseShaped(id, entry, block);
                case RECIPE_TYPE_COMPLEX -> parseComplex(id, entry);
                case RECIPE_TYPE_SMITHING_TRANSFORM -> parseSmithingTransform(entry, block);
                case RECIPE_TYPE_SMITHING_TRIM -> parseSmithingTrim(entry, block);
                default -> log.warn("Unsupported recipe type {} for recipe {}", type, id);
            }
        }

        unlabeled = 0;
        for (JsonNode entry : json.get("potionMixes")) {
            Identifier id = Identifier.parse(UNLABELED_POTION_PREFIX + (++unlabeled));
            ItemStack input = ItemStackNbtCodec.deserializeItem(Identifier.parse(entry.get("inputId").asString()), entry.get("inputMeta").shortValue(), 1, NbtMap.EMPTY);
            ItemStack reagent = ItemStackNbtCodec.deserializeItem(Identifier.parse(entry.get("reagentId").asString()), entry.get("reagentMeta").shortValue(), 1, NbtMap.EMPTY);
            ItemStack output = ItemStackNbtCodec.deserializeItem(Identifier.parse(entry.get("outputId").asString()), entry.get("outputMeta").shortValue(), 1, NbtMap.EMPTY);
            this.register(new CloudBrewingRecipe(id, input, reagent, output));
        }

        unlabeled = 0;
        for (JsonNode entry : json.get("containerMixes")) {
            Identifier id = Identifier.parse(UNLABELED_CONTAINER_PREFIX + (++unlabeled));
            ItemStack input = ItemStackNbtCodec.deserializeItem(Identifier.parse(entry.get("inputId").asString()), (short) 0, 1, NbtMap.EMPTY);
            ItemStack reagent = ItemStackNbtCodec.deserializeItem(Identifier.parse(entry.get("reagentId").asString()), (short) 0, 1, NbtMap.EMPTY);
            ItemStack output = ItemStackNbtCodec.deserializeItem(Identifier.parse(entry.get("outputId").asString()), (short) 0, 1, NbtMap.EMPTY);
            this.register(new CloudContainerRecipe(id, input, reagent, output));
        }
    }

    private void parseShapeless(Identifier id, JsonNode entry, Identifier block) {
        if (isCookingStation(block)) {
            parseCooking(id, entry, block);
            return;
        }

        List<ItemStack> outputs = new ArrayList<>();
        for (Map<String, Object> item : toMapList(entry.get("output"))) {
            outputs.add(RecipeItemCodec.fromJson(item));
        }

        List<ItemStack> inputs = new ArrayList<>();
        List<ItemDescriptorWithCount> inputDescriptors = new ArrayList<>();
        for (Map<String, Object> item : toMapList(entry.get("input"))) {
            inputDescriptors.add(RecipeItemCodec.descriptorFromJson(item));
            inputs.add(RecipeItemCodec.fromJson(item));
        }
        inputs.removeIf(ItemStack::isEmpty);
        inputs.sort(recipeComparator);

        if (block != null && block.getName().equals("stonecutter")) {
            ItemStack scInput = inputs.isEmpty() ? ItemStack.EMPTY : inputs.getFirst();
            ItemDescriptorWithCount scDescriptor = inputDescriptors.isEmpty() ? ItemDescriptorWithCount.EMPTY : inputDescriptors.getFirst();
            ItemStack scResult = outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst();
            this.register(new CloudStonecuttingRecipe(id, entry.get("priority").asInt(), scInput, scDescriptor, scResult));
            return;
        }

        CloudShapelessRecipe recipe = new CloudShapelessRecipe(id, entry.get("priority").asInt(), outputs, inputs, inputDescriptors, block, RecipeType.SHAPELESS);
        JsonNode unlockContext = entry.get("unlockContext");
        if (unlockContext != null) {
            recipe.setUnlockContext(RecipeUnlockContext.valueOf(unlockContext.asString()));
        }
        this.register(recipe);
    }

    private void parseShulkerBox(Identifier id, JsonNode entry, Identifier block) {
        List<ItemStack> outputs = new ArrayList<>();
        for (Map<String, Object> item : toMapList(entry.get("output"))) {
            outputs.add(RecipeItemCodec.fromJson(item));
        }

        List<ItemStack> inputs = new ArrayList<>();
        List<ItemDescriptorWithCount> inputDescriptors = new ArrayList<>();
        for (Map<String, Object> item : toMapList(entry.get("input"))) {
            inputDescriptors.add(RecipeItemCodec.descriptorFromJson(item));
            inputs.add(RecipeItemCodec.fromJson(item));
        }
        inputs.removeIf(ItemStack::isEmpty);
        inputs.sort(recipeComparator);

        this.register(new CloudShulkerBoxRecipe(id, entry.get("priority").asInt(), outputs, inputs, inputDescriptors, block));
    }

    private void parseShaped(Identifier id, JsonNode entry, Identifier block) {
        String[] shape = toStringArray(entry.get("shape"));

        List<ItemStack> outputs = new ArrayList<>();
        for (Map<String, Object> item : toMapList(entry.get("output"))) {
            outputs.add(RecipeItemCodec.fromJson(item));
        }
        ItemStack primary = outputs.removeFirst();

        CharObjectMap<ItemStack> ingredients = new CharObjectHashMap<>();
        CharObjectMap<ItemDescriptorWithCount> ingredientDescriptors = new CharObjectHashMap<>();
        for (Map.Entry<String, Map<String, Object>> item : toMapOfMaps(entry.get("input")).entrySet()) {
            char key = item.getKey().charAt(0);
            ingredients.put(key, RecipeItemCodec.fromJson(item.getValue()));
            ingredientDescriptors.put(key, RecipeItemCodec.descriptorFromJson(item.getValue()));
        }

        CloudShapedRecipe recipe = new CloudShapedRecipe(id, entry.get("priority").asInt(), primary, shape, ingredients, ingredientDescriptors, outputs, block);
        JsonNode assumeSymmetry = entry.get("assumeSymmetry");
        if (assumeSymmetry != null) {
            recipe.setAssumeSymmetry(assumeSymmetry.asBoolean());
        }

        JsonNode unlockContext = entry.get("unlockContext");
        if (unlockContext != null) {
            recipe.setUnlockContext(RecipeUnlockContext.valueOf(unlockContext.asString()));
        }

        this.register(recipe);
    }

    private void parseCooking(Identifier id, JsonNode entry, Identifier block) {
        List<Map<String, Object>> outputs = toMapList(entry.get("output"));
        List<Map<String, Object>> inputs = toMapList(entry.get("input"));
        if (inputs.size() != 1 || outputs.size() != 1) {
            throw new IllegalArgumentException("Cooking recipe requires one input and one output: " + id);
        }

        Map<String, Object> inputData = inputs.getFirst();
        ItemStack output = RecipeItemCodec.fromJson(outputs.getFirst());
        int priority = entry.has("priority") ? entry.get("priority").asInt() : 0;
        boolean regularFurnace = Identifiers.FURNACE.equals(block);
        int defaultDuration = defaultCookingTime(block);
        int duration = entry.has("cookingTime") ? entry.get("cookingTime").asInt() : defaultDuration;
        float experience = entry.has("experience") ? (float) entry.get("experience").asDouble()
                : isCampfire(block) ? 0 : VanillaCookingData.experience(output.getType());
        Identifier stationRecipeId = regularFurnace ? id
                : Identifier.from(id.getNamespace(), block.getName() + "/" + id.getName());
        ItemDescriptorWithCount descriptor = RecipeItemCodec.descriptorFromJson(inputData);
        ItemStack input = descriptor.getDescriptor() instanceof ItemTagDescriptor tag
                ? this.itemRegistry.getTag(ItemTagKey.of(tag.getItemTag())).getValues().stream()
                        .min(Comparator.comparing(ItemType::getId))
                        .map(ItemStack::from)
                        .orElse(ItemStack.EMPTY)
                : RecipeItemCodec.fromJson(inputData);
        this.register(new CloudCookingRecipe(
                stationRecipeId,
                output,
                input,
                descriptor,
                block,
                priority,
                duration,
                experience)
        );
    }

    private void parseComplex(Identifier id, JsonNode entry) {
        UUID uuid = UUID.fromString(entry.get("uuid").asString());
        this.register(new CloudComplexRecipe(id, uuid));
    }

    private void parseSmithingTransform(JsonNode entry, Identifier block) {
        Identifier smithingTable = block != null ? block : Identifiers.SMITHING_TABLE;
        JsonNode input = entry.get("input");

        ItemDescriptorWithCount templateDesc = RecipeItemCodec.descriptorFromJson(toMap(input.get("template")));
        ItemDescriptorWithCount baseDesc = RecipeItemCodec.descriptorFromJson(toMap(input.get("base")));
        ItemDescriptorWithCount additionDesc = RecipeItemCodec.descriptorFromJson(toMap(input.get("addition")));

        ItemStack resultItem = RecipeItemCodec.fromJson(toMap(entry.get("output")));
        Identifier id = entry.has("id") ? Identifier.parse(entry.get("id").asString())
                : Identifier.parse("smithing_" + resultItem.getType().getId().getName());
        this.register(new CloudSmithingTransformRecipe(id, templateDesc, baseDesc, additionDesc, resultItem, smithingTable));
    }

    private void parseSmithingTrim(JsonNode entry, Identifier block) {
        Identifier smithingTable = block != null ? block : Identifiers.SMITHING_TABLE;
        JsonNode input = entry.get("input");

        ItemDescriptorWithCount templateDesc = RecipeItemCodec.descriptorFromJson(toMap(input.get("template")));
        ItemDescriptorWithCount baseDesc = RecipeItemCodec.descriptorFromJson(toMap(input.get("base")));
        ItemDescriptorWithCount additionDesc = RecipeItemCodec.descriptorFromJson(toMap(input.get("addition")));

        Identifier id = entry.has("id") ? Identifier.parse(entry.get("id").asString()) : Identifier.parse("smithing_armor_trim");
        this.register(new CloudSmithingTrimRecipe(id, templateDesc, baseDesc, additionDesc, smithingTable));
    }

    public @Nullable SmithingResult findSmithingResult(ItemStack template, ItemStack base, ItemStack addition) {
        if (template.isEmpty() || base.isEmpty() || addition.isEmpty()) {
            return null;
        }

        for (SmithingRecipe recipe : this.smithingRecipes) {
            if (!Identifiers.SMITHING_TABLE.equals(recipe.getBlock())) {
                continue;
            }

            switch (recipe) {
                case CloudSmithingTransformRecipe transform -> {
                    if (this.matchesSmithing(template, transform.getTemplate(), transform.getTemplateDescriptor())
                            && this.matchesSmithing(base, transform.getBase(), transform.getBaseDescriptor())
                            && this.matchesSmithing(addition, transform.getAddition(), transform.getAdditionDescriptor())) {
                        ItemStack output = transform.getResult();
                        if (output.isEmpty()) {
                            continue;
                        }

                        ItemStackBuilder result = base.toBuilder().itemType(output.getType()).amount(output.getCount());
                        output.getDataComponents().keySet().forEach(type -> copySmithingComponent(output, result, type));
                        return new SmithingResult(recipe, result.build(), transform.getTemplateDescriptor().getCount(),
                                transform.getBaseDescriptor().getCount(), transform.getAdditionDescriptor().getCount());
                    }
                }
                case CloudSmithingTrimRecipe trim -> {
                    if (this.matchesSmithing(template, trim.getTemplate(), trim.getTemplateDescriptor())
                            && this.matchesSmithing(base, trim.getBase(), trim.getBaseDescriptor())
                            && this.matchesSmithing(addition, trim.getAddition(), trim.getAdditionDescriptor())) {
                        TrimPattern pattern = this.findTrimPattern(template.getType().getId());
                        TrimMaterial material = this.findTrimMaterial(addition.getType().getId());
                        if (pattern == null || material == null) {
                            continue;
                        }

                        ArmorTrim armorTrim = new ArmorTrim(pattern.patternId(), material.materialId());
                        if (!armorTrim.equals(base.get(ItemDataComponents.ARMOR_TRIM))) {
                            ItemStack result = base.toBuilder().amount(1).setData(ItemDataComponents.ARMOR_TRIM, armorTrim).build();
                            return new SmithingResult(recipe, result, trim.getTemplateDescriptor().getCount(),
                                    trim.getBaseDescriptor().getCount(), trim.getAdditionDescriptor().getCount());
                        }
                    }
                }
                default -> { }
            }
        }

        return null;
    }

    private boolean matchesSmithing(ItemStack stack, RecipeIngredient ingredient, ItemDescriptorWithCount descriptor) {
        if (descriptor.getCount() <= 0 || stack.getCount() < descriptor.getCount()) {
            return false;
        }

        if (ingredient instanceof RecipeIngredient.Tag(Identifier tag)) {
            if (tag.equals(TRIM_TEMPLATES)) {
                return this.findTrimPattern(stack.getType().getId()) != null;
            }

            if (tag.equals(TRIM_MATERIALS)) {
                return this.findTrimMaterial(stack.getType().getId()) != null;
            }

            ItemTagKey key = ItemTagKey.of(tag);
            return this.itemRegistry.getTags().stream().anyMatch(candidate -> candidate.getKey() == key && candidate.isTagged(stack.getType()));
        }

        return ingredient.test(stack) && (!(descriptor.getDescriptor() instanceof DefaultDescriptor exact)
                || exact.getAuxValue() == Short.MAX_VALUE
                || this.itemRegistry.getSerializer(stack.getType()).getAuxValue(stack) == exact.getAuxValue());
    }

    private static <T> void copySmithingComponent(ItemStack source, ItemStackBuilder target, ItemDataComponentType<T> type) {
        target.setData(type, Objects.requireNonNull(source.get(type)));
    }

    private @Nullable TrimPattern findTrimPattern(Identifier item) {
        for (TrimPattern pattern : this.trimPatterns) {
            if (pattern.itemId().equals(item)) {
                return pattern;
            }
        }
        return null;
    }

    private @Nullable TrimMaterial findTrimMaterial(Identifier item) {
        for (TrimMaterial material : this.trimMaterials) {
            if (material.itemId().equals(item)) {
                return material;
            }
        }
        return null;
    }

    public record SmithingResult(SmithingRecipe recipe, ItemStack item, int templateCost, int baseCost, int additionCost) {
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
        if (isCookingStation(craftingBlock)) {
            List<ItemStack> input = flattenGrid(inputMap).stream()
                    .filter(item -> item != null && !item.isEmpty())
                    .toList();
            return input.size() == 1 ? matchCookingRecipe(input.getFirst(), output, craftingBlock) : null;
        }

        int key = RecipeItemCodec.getItemHash(output);
        if (!recipeHashMap.containsKey(key)) {
            return null;
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

    public @Nullable CloudCookingRecipe matchCookingRecipe(ItemStack input, @Nullable Identifier craftingBlock) {
        return this.matchCookingRecipe(input, null, craftingBlock);
    }

    private @Nullable CloudCookingRecipe matchCookingRecipe(
            ItemStack input, @Nullable ItemStack output, @Nullable Identifier craftingBlock) {
        Identifier normalizedBlock = normalizeFurnaceBlock(craftingBlock);
        List<CloudCookingRecipe> candidates = cookingByBlock.get(normalizedBlock);
        if (candidates == null) {
            return null;
        }

        CloudCookingRecipe best = null;
        for (CloudCookingRecipe candidate : candidates) {
            if (best != null && candidate.getPriority() >= best.getPriority()) {
                continue;
            }

            ItemStack result = candidate.getResult();
            if (output != null && (result.getCount() != output.getCount() || !result.isStackableWith(output))) {
                continue;
            }

            if (candidate.matches(input)) {
                best = candidate;
            }
        }

        return best;
    }

    @Override
    public Recipe matchRecipe(ItemStack input, Identifier craftingBlock) {
        if (craftingBlock != null && craftingBlock.getName().equals("stonecutter")) {
            return matchStonecuttingRecipe(input);
        }
        return matchCookingRecipe(input, craftingBlock);
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
        if (ingredient.isEmpty() || basePotion.isEmpty()) {
            return null;
        }

        Identifier baseId = basePotion.getType().getId();

        for (CloudContainerRecipe mix : this.containerByBase.getOrDefault(baseId, List.of())) {
            if (this.matchesBrewingIngredient(mix, ingredient)) {
                return mix;
            }
        }

        for (CloudBrewingRecipe mix : this.brewingByBase.getOrDefault(baseId, List.of())) {
            if (mix.getInput().getOrDefault(ItemDataComponents.POTION_TYPE, PotionTypes.WATER)
                    == basePotion.getOrDefault(ItemDataComponents.POTION_TYPE, PotionTypes.WATER)
                    && this.matchesBrewingIngredient(mix, ingredient)) {
                return mix;
            }
        }

        return null;
    }

    public boolean isBrewingIngredient(ItemStack item) {
        return !item.isEmpty() && (this.brewingByBase.values().stream().flatMap(List::stream)
                .anyMatch(recipe -> this.matchesBrewingIngredient(recipe, item))
                || this.containerByBase.values().stream().flatMap(List::stream)
                .anyMatch(recipe -> this.matchesBrewingIngredient(recipe, item)));
    }

    public ItemStack brewPotion(ItemStack ingredient, ItemStack bottle) {
        if (bottle.getCount() != 1) {
            return bottle;
        }

        MixRecipe recipe = this.matchBrewingRecipe(ingredient, bottle);
        if (recipe == null) {
            return bottle;
        }

        ItemStack output = recipe.getResult();
        ItemStackBuilder result = bottle.toBuilder().itemType(output.getType());
        if (recipe.getType() == RecipeType.POTION) {
            result.setData(ItemDataComponents.POTION_TYPE, output.getOrDefault(ItemDataComponents.POTION_TYPE, PotionTypes.WATER));
        }

        return result.build();
    }

    private boolean matchesBrewingIngredient(MixRecipe recipe, ItemStack item) {
        ItemStack ingredient = recipe.getIngredient();
        if (ingredient.getType() != item.getType() || item.getCount() < ingredient.getCount()) {
            return false;
        }

        ItemSerializer serializer = this.itemRegistry.getSerializer(item.getType());
        return serializer.getAuxValue(ingredient) == serializer.getAuxValue(item);
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
                case CloudCookingRecipe r when r.getInputItem().getType().getId().equals(inputId) -> builder.add(r);
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
    public Collection<Recipe> getRecipesFor(ItemStack result) {
        return recipeMap.values().stream()
                .filter(r -> r.getResult().hasSameDataComponents(result))
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
                    case COOKING -> packet.getShapelessData().add(((CloudCookingRecipe) recipe).toNetwork(entry.getKey(), netId));
                    case COMPLEX ->
                            packet.getMultiData().add(MultiRecipeData.of(((CloudComplexRecipe) recipe).uuid(), netId));
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

    private static boolean shouldSkipPacketRecipe(Identifier id, List<ItemDescriptorWithCount> descriptors) {
        if (descriptors.stream().anyMatch(descriptor -> descriptor != ItemDescriptorWithCount.EMPTY)) {
            return false;
        }
        log.warn("Skipping recipe {} in crafting data: no valid ingredients", id);
        return true;
    }

    private void addShapelessToPacket(CraftingDataPacket packet, CloudShapelessRecipe recipe, UUID uuid, String blockTag, int netId) {
        List<ItemDescriptorWithCount> descriptors = resolveDescriptors(recipe);
        if (shouldSkipPacketRecipe(recipe.getId(), descriptors)) {
            return;
        }

        if (recipe.getType() == RecipeType.SHAPELESS_CHEMISTRY) {
            packet.getShapelessChemistryData().add(ShapelessRecipeData.shapelessChemistry(
                    recipe.getId().toString(),
                    descriptors,
                    RecipeItemCodec.toNetworkRecipe(recipe.getAllResults()),
                    uuid,
                    blockTag,
                    recipe.getPriority(),
                    netId));
        } else {
            packet.getShapelessData().add(ShapelessRecipeData.shapeless(
                    recipe.getId().toString(),
                    descriptors,
                    RecipeItemCodec.toNetworkRecipe(recipe.getAllResults()),
                    uuid,
                    blockTag,
                    recipe.getPriority(),
                    netId,
                    toProtocolRequirement(recipe.getUnlockContext())));
        }
    }

    private void addStonecuttingToPacket(CraftingDataPacket packet, CloudStonecuttingRecipe recipe, UUID uuid, String blockTag, int netId) {
        packet.getShapelessData().add(ShapelessRecipeData.shapeless(
                recipe.getId().toString(),
                RecipeItemCodec.toRecipeDescriptors(recipe.getInputDescriptors()),
                RecipeItemCodec.toNetworkRecipe(List.of(recipe.getResult())),
                uuid,
                blockTag,
                recipe.getPriority(),
                netId,
                RecipeUnlockingRequirement.INVALID));
    }

    private void addShulkerBoxToPacket(CraftingDataPacket packet, CloudShapelessRecipe recipe, UUID uuid, String blockTag, int netId) {
        packet.getShapelessUserData().add(ShapelessRecipeData.shulkerBox(
                recipe.getId().toString(),
                resolveDescriptors(recipe),
                RecipeItemCodec.toNetworkRecipe(recipe.getAllResults()),
                uuid,
                blockTag,
                recipe.getPriority(),
                netId));
    }

    private void addShapedToPacket(CraftingDataPacket packet, CloudShapedRecipe recipe, UUID uuid, String blockTag, int netId) {
        List<ItemDescriptorWithCount> descriptors = resolveDescriptors(recipe);
        if (shouldSkipPacketRecipe(recipe.getId(), descriptors)) {
            return;
        }

        if (recipe.getType() == RecipeType.SHAPED_CHEMISTRY) {
            packet.getShapedChemistryData().add(ShapedRecipeData.shapedChemistry(
                    recipe.getId().toString(),
                    recipe.getWidth(),
                    recipe.getHeight(),
                    descriptors,
                    RecipeItemCodec.toNetworkRecipe(recipe.getAllResults()),
                    uuid,
                    blockTag,
                    recipe.getPriority(),
                    netId,
                    recipe.isAssumeSymmetry()));
        } else {
            packet.getShapedData().add(ShapedRecipeData.shaped(
                    recipe.getId().toString(),
                    recipe.getWidth(),
                    recipe.getHeight(),
                    descriptors,
                    RecipeItemCodec.toNetworkRecipe(recipe.getAllResults()),
                    uuid,
                    blockTag,
                    recipe.getPriority(),
                    netId,
                    recipe.isAssumeSymmetry(),
                    toProtocolRequirement(recipe.getUnlockContext())));
        }
    }

    private void addSmithingTransformToPacket(CraftingDataPacket packet, CloudSmithingTransformRecipe recipe, String blockTag, int netId) {
        packet.getSmithingTransformData().add(SmithingTransformRecipeData.of(
                recipe.getId().toString(),
                RecipeItemCodec.toRecipeDescriptors(List.of(recipe.getTemplateDescriptor())).getFirst(),
                RecipeItemCodec.toRecipeDescriptors(List.of(recipe.getBaseDescriptor())).getFirst(),
                RecipeItemCodec.toRecipeDescriptors(List.of(recipe.getAdditionDescriptor())).getFirst(),
                RecipeItemCodec.toNetworkRecipe(recipe.getResult()),
                blockTag,
                netId));
    }

    private void addSmithingTrimToPacket(CraftingDataPacket packet, CloudSmithingTrimRecipe recipe, String blockTag, int netId) {
        packet.getSmithingTrimData().add(SmithingTrimRecipeData.of(
                recipe.getId().toString(),
                recipe.getBaseDescriptor(),
                recipe.getAdditionDescriptor(),
                recipe.getTemplateDescriptor(),
                blockTag,
                netId));
    }

    private void addBrewingToPacket(CraftingDataPacket packet, CloudBrewingRecipe recipe) {
        ItemData reagentData = RecipeItemCodec.toNetworkRecipe(recipe.getIngredient());
        ItemData inputData = RecipeItemCodec.toNetworkRecipe(recipe.getInput());
        ItemData outputData = RecipeItemCodec.toNetworkRecipe(recipe.getResult());
        packet.getPotionMixData().add(new PotionMixData(
                inputData.getDefinition().getRuntimeId(),
                inputData.getDamage(),
                reagentData.getDefinition().getRuntimeId(),
                reagentData.getDamage(),
                outputData.getDefinition().getRuntimeId(),
                outputData.getDamage()));
    }

    private void addContainerToPacket(CraftingDataPacket packet, CloudContainerRecipe recipe) {
        ItemData reagentData = RecipeItemCodec.toNetworkRecipe(recipe.getIngredient());
        ItemData inputData = RecipeItemCodec.toNetworkRecipe(recipe.getInput());
        ItemData outputData = RecipeItemCodec.toNetworkRecipe(recipe.getResult());
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
        return ServerJson.MAPPER.convertValue(node, LIST_OF_MAPS);
    }

    private static Map<String, Map<String, Object>> toMapOfMaps(JsonNode node) {
        return ServerJson.MAPPER.convertValue(node, MAP_OF_MAPS);
    }

    private static Map<String, Object> toMap(JsonNode node) {
        return ServerJson.MAPPER.convertValue(node, MAP_OF_OBJECTS);
    }

    private static String[] toStringArray(JsonNode node) {
        return ServerJson.MAPPER.convertValue(node, STRING_ARRAY);
    }

    private static List<ItemDescriptorWithCount> resolveDescriptors(CloudShapelessRecipe recipe) {
        List<ItemDescriptorWithCount> raw = recipe.getInputDescriptors();
        if (raw == null) {
            return RecipeItemCodec.toDescriptorsRecipe(recipe.getIngredientList());
        }
        return RecipeItemCodec.toRecipeDescriptors(raw);
    }

    private static List<ItemDescriptorWithCount> resolveDescriptors(CloudShapedRecipe recipe) {
        List<ItemDescriptorWithCount> raw = recipe.getInputDescriptorList();
        if (raw == null) {
            return RecipeItemCodec.toDescriptorsRecipe(recipe.getIngredientList());
        }
        return RecipeItemCodec.toRecipeDescriptors(raw);
    }

    private static UUID getInputHash(Recipe recipe) {
        return UUID.nameUUIDFromBytes(recipe.getId().toString().getBytes());
    }

    private static boolean isCookingStation(@Nullable Identifier block) {
        Identifier normalized = normalizeFurnaceBlock(block);
        return Identifiers.FURNACE.equals(normalized) || Identifiers.BLAST_FURNACE.equals(normalized)
                || Identifiers.SMOKER.equals(normalized) || isCampfire(normalized);
    }

    private static boolean isCampfire(@Nullable Identifier block) {
        return Identifiers.CAMPFIRE.equals(block) || Identifiers.SOUL_CAMPFIRE.equals(block);
    }

    private static int defaultCookingTime(Identifier block) {
        if (isCampfire(block)) {
            return VanillaCookingData.CAMPFIRE_COOK_TICKS;
        }

        return Identifiers.FURNACE.equals(block)
                ? VanillaCookingData.FURNACE_COOK_TICKS
                : VanillaCookingData.FAST_COOK_TICKS;
    }

    private static @Nullable Identifier normalizeFurnaceBlock(@Nullable Identifier block) {
        if (Identifiers.LIT_SMOKER.equals(block)) {
            return Identifiers.SMOKER;
        }

        if (Identifiers.LIT_BLAST_FURNACE.equals(block)) {
            return Identifiers.BLAST_FURNACE;
        }

        if (Identifiers.LIT_FURNACE.equals(block)) {
            return Identifiers.FURNACE;
        }

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
