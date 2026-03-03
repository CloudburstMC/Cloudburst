package org.cloudburstmc.codegen;

import com.palantir.javapoet.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockDataGen {

	private static final int TYPE_CHUNK = 150;
	private static final String SERVER_PACKAGE = "org.cloudburstmc.server.block";
	private static final String API_BLOCK_PACKAGE = "org.cloudburstmc.api.block";
	private static final String REGISTRY_PACKAGE = "org.cloudburstmc.api.registry";
	private static final String COMPONENT_PACKAGE = "org.cloudburstmc.api.util.component";

	private static final float DEF_HARDNESS = 0f;
	private static final float DEF_RESISTANCE = 0f;
	private static final float DEF_FRICTION = 0.6f;
	private static final float DEF_TRANSLUCENCY = 0f;
	private static final float DEF_THICKNESS = 0f;
	private static final int DEF_BURN_ODDS = 0;
	private static final int DEF_FLAME_ODDS = 0;
	private static final int DEF_LIGHT_DAMPENING = 0;
	private static final int DEF_LIGHT_EMISSION = 0;
	private static final boolean DEF_SOLID = false;
	private static final boolean DEF_REQUIRES_CORRECT_TOOL = false;
	private static final boolean DEF_REPLACEABLE = false;
	private static final String DEF_MAP_COLOR = "#00000000";

	private static final int PROP_CHUNK = 200;
	private static final ClassName TYPE_PROPERTIES = ClassName.get(SERVER_PACKAGE, "BlockPropertyData", "TypeProperties");
	private static final ClassName STATE_SHAPES = ClassName.get(SERVER_PACKAGE, "BlockPropertyData", "StateShapes");
	private static final ClassName LONG2OBJECT_MAP = ClassName.get("it.unimi.dsi.fastutil.longs", "Long2ObjectOpenHashMap");

	private static final double PROP_DEF_HARDNESS = 1.0;
	private static final double PROP_DEF_EXPLOSION_RESISTANCE = 0.0;
	private static final double PROP_DEF_FRICTION = 0.6;
	private static final double PROP_DEF_THICKNESS = 0.0;
	private static final double PROP_DEF_TRANSLUCENCY = 0.0;
	private static final int PROP_DEF_LIGHT_EMISSION = 0;
	private static final int PROP_DEF_LIGHT_DAMPENING = 15;
	private static final int PROP_DEF_BURN_ODDS = 0;
	private static final int PROP_DEF_FLAME_ODDS = 0;
	private static final boolean PROP_DEF_IS_SOLID = true;
	private static final boolean PROP_DEF_REQUIRES_TOOL = false;
	private static final boolean PROP_DEF_CAN_CONTAIN_LIQUID = false;
	private static final String PROP_DEF_TINT_METHOD = "None";
	private static final String PROP_DEF_LIQUID_REACTION = "BLOCKING";

	private static final Comparator<String> NATURAL_ORDER = (left, right) -> {
		int leftIdx = 0, rightIdx = 0;
		while (leftIdx < left.length() && rightIdx < right.length()) {
			char leftChar = left.charAt(leftIdx), rightChar = right.charAt(rightIdx);
			if (Character.isDigit(leftChar) && Character.isDigit(rightChar)) {
				int leftNumStart = leftIdx, rightNumStart = rightIdx;
				while (leftIdx < left.length() && Character.isDigit(left.charAt(leftIdx))) leftIdx++;
				while (rightIdx < right.length() && Character.isDigit(right.charAt(rightIdx))) rightIdx++;
				long leftNum = Long.parseLong(left.substring(leftNumStart, leftIdx));
				long rightNum = Long.parseLong(right.substring(rightNumStart, rightIdx));
				if (leftNum != rightNum) return Long.compare(leftNum, rightNum);
			} else {
				if (leftChar != rightChar) return Character.compare(leftChar, rightChar);
				leftIdx++;
				rightIdx++;
			}
		}
		return Integer.compare(left.length() - leftIdx, right.length() - rightIdx);
	};

	public static void main(String[] args) throws IOException {
		Path projectRoot = resolveProjectRoot();
		Path inputJson = projectRoot.resolve("server/src/main/resources/data/block_properties.json");
		Path blockIdsJava = projectRoot.resolve("api/src/main/java/org/cloudburstmc/api/block/BlockIds.java");
		Path blockTypesJava = projectRoot.resolve("api/src/main/java/org/cloudburstmc/api/block/BlockTypes.java");

		Path outputDir = projectRoot.resolve("server/build/generated/sources/blockData/java");

		if (!Files.exists(inputJson)) {
			throw new IOException("Input not found: " + inputJson);
		}

		Files.createDirectories(outputDir);

		List<Map<String, Object>> root = new ObjectMapper().readValue(inputJson.toFile(), new TypeReference<>() {
		});

		generateBlockTypeData(root, blockIdsJava, blockTypesJava, outputDir);
		generateBlockPropertyData(root, outputDir);
	}

	private static void generateBlockTypeData(List<Map<String, Object>> root, Path blockIdsJava, Path blockTypesJava, Path outputDir) throws IOException {
		Map<String, String> wireToConst = buildWireToConstMap(blockIdsJava, blockTypesJava);
		Map<String, TypeProps> byName = new LinkedHashMap<>();

		for (Map<String, Object> node : root) {
			String name = stringVal(node, "name");
			if (name == null) continue;
			if (byName.containsKey(name)) continue;

			String wireName = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
			String constName = wireToConst.getOrDefault(wireName, wireName.toUpperCase(Locale.ROOT));

			byName.put(name, new TypeProps(
					constName,
					floatVal(node, "hardness", 1.0f),
					floatVal(node, "explosionResistance", 0.0f),
					floatVal(node, "friction", 0.6f),
					floatVal(node, "translucency", 0.0f),
					floatVal(node, "thickness", 0.0f),
					intVal(node, "burnOdds", 0),
					intVal(node, "flameOdds", 0),
					intVal(node, "lightDampening", 15),
					intVal(node, "lightEmission", 0),
					boolVal(node, "isSolid", true),
					boolVal(node, "requiresCorrectToolForDrops", false),
					stringVal(node, "mapColor"),
					stringVal(node, "liquidReactionOnTouch")
			));
		}

		List<TypeProps> sorted = new ArrayList<>(byName.values());
		sorted.sort(Comparator.comparing(TypeProps::constantName, NATURAL_ORDER));
		sorted.removeIf(BlockDataGen::isAllDefault);

		List<List<TypeProps>> chunks = chunked(sorted, TYPE_CHUNK);

		ClassName blockCompsName = ClassName.get(API_BLOCK_PACKAGE, "BlockComponents");
		ClassName blockTypeName = ClassName.get(API_BLOCK_PACKAGE, "BlockType");
		ClassName blockTypesName = ClassName.get(API_BLOCK_PACKAGE, "BlockTypes");
		ClassName registryName = ClassName.get(REGISTRY_PACKAGE, "BlockRegistry");
		ClassName componentMapName = ClassName.get(COMPONENT_PACKAGE, "ComponentMap");

		ParameterSpec registryParam = ParameterSpec.builder(registryName, "registry").build();

		List<MethodSpec> chunkMethods = new ArrayList<>();
		for (int i = 0; i < chunks.size(); i++) {
			CodeBlock.Builder body = CodeBlock.builder();
			body.addStatement("$T blockType", blockTypeName);
			body.addStatement("$T componentMap", componentMapName);

			for (TypeProps props : chunks.get(i)) {
				body.addStatement("blockType = $T.$L", blockTypesName, props.constantName());
				body.addStatement("componentMap = registry.getComponents(blockType)");
				body.beginControlFlow("if (componentMap != null)");
				emitTypeSets(body, blockCompsName, props);
				body.endControlFlow();
			}

			chunkMethods.add(MethodSpec.methodBuilder("apply" + i)
					.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
					.addParameter(registryParam)
					.addCode(body.build())
					.build());
		}

		CodeBlock.Builder applyBody = CodeBlock.builder();
		for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
			applyBody.addStatement("apply$L(registry)", chunkIndex);
		}

		MethodSpec applyAll = MethodSpec.methodBuilder("applyAll")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addParameter(registryParam)
				.addCode(applyBody.build())
				.build();

		TypeSpec.Builder classSpec = TypeSpec.classBuilder("BlockTypeData")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addJavadoc("Generated by {@code BlockDataGen}, do not edit by hand.\n")
				.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
				.addMethod(applyAll);

		for (MethodSpec chunkMethod : chunkMethods) {
			classSpec.addMethod(chunkMethod);
		}

		JavaFile.builder(SERVER_PACKAGE, classSpec.build())
				.skipJavaLangImports(true)
				.indent("\t")
				.build()
				.writeTo(outputDir);
	}

	private static void emitTypeSets(CodeBlock.Builder body, ClassName blockComponents, TypeProps props) {
		if (props.hardness() != DEF_HARDNESS)
			body.addStatement("componentMap.set($T.HARDNESS, () -> $Lf)", blockComponents, fmtf(props.hardness()));
		if (props.resistance() != DEF_RESISTANCE)
			body.addStatement("componentMap.set($T.RESISTANCE, () -> $Lf)", blockComponents, fmtf(props.resistance()));
		if (props.friction() != DEF_FRICTION)
			body.addStatement("componentMap.set($T.FRICTION, () -> $Lf)", blockComponents, fmtf(props.friction()));
		if (props.translucency() != DEF_TRANSLUCENCY)
			body.addStatement("componentMap.set($T.TRANSLUCENCY, () -> $Lf)", blockComponents, fmtf(props.translucency()));
		if (props.thickness() != DEF_THICKNESS)
			body.addStatement("componentMap.set($T.THICKNESS, () -> $Lf)", blockComponents, fmtf(props.thickness()));
		if (props.burnOdds() != DEF_BURN_ODDS)
			body.addStatement("componentMap.set($T.BURN_ODDS, () -> $L)", blockComponents, props.burnOdds());
		if (props.flameOdds() != DEF_FLAME_ODDS)
			body.addStatement("componentMap.set($T.FLAME_ODDS, () -> $L)", blockComponents, props.flameOdds());
		if (props.lightDampening() != DEF_LIGHT_DAMPENING)
			body.addStatement("componentMap.set($T.LIGHT_DAMPENING, () -> $L)", blockComponents, props.lightDampening());
		if (props.lightEmission() != DEF_LIGHT_EMISSION)
			body.addStatement("componentMap.set($T.LIGHT_EMISSION, () -> $L)", blockComponents, props.lightEmission());
		if (props.solid() != DEF_SOLID)
			body.addStatement("componentMap.set($T.SOLID, () -> $L)", blockComponents, props.solid());
		if (props.requiresCorrectTool() != DEF_REQUIRES_CORRECT_TOOL)
			body.addStatement("componentMap.set($T.REQUIRES_CORRECT_TOOL, () -> $L)", blockComponents, props.requiresCorrectTool());
		if ("POPPED".equals(props.liquidReactionOnTouch()) != DEF_REPLACEABLE)
			body.addStatement("componentMap.set($T.REPLACEABLE, () -> $L)", blockComponents, "POPPED".equals(props.liquidReactionOnTouch()));
		String mapColor = props.mapColor() != null ? props.mapColor() : DEF_MAP_COLOR;
		if (!mapColor.equals(DEF_MAP_COLOR))
			body.addStatement("componentMap.set($T.MAP_COLOR, () -> $S)", blockComponents, mapColor);
	}

	private static boolean isAllDefault(TypeProps props) {
		return props.hardness() == DEF_HARDNESS
				&& props.resistance() == DEF_RESISTANCE
				&& props.friction() == DEF_FRICTION
				&& props.translucency() == DEF_TRANSLUCENCY
				&& props.thickness() == DEF_THICKNESS
				&& props.burnOdds() == DEF_BURN_ODDS
				&& props.flameOdds() == DEF_FLAME_ODDS
				&& props.lightDampening() == DEF_LIGHT_DAMPENING
				&& props.lightEmission() == DEF_LIGHT_EMISSION
				&& props.solid() == DEF_SOLID
				&& props.requiresCorrectTool() == DEF_REQUIRES_CORRECT_TOOL
				&& !"POPPED".equals(props.liquidReactionOnTouch())
				&& (props.mapColor() == null || props.mapColor().equals(DEF_MAP_COLOR));
	}

	private static void generateBlockPropertyData(List<Map<String, Object>> root, Path outputDir) throws IOException {
		Map<String, PropTypeProps> byName = new LinkedHashMap<>();
		Map<Long, StateShapes> byHash = new LinkedHashMap<>();

		for (Map<String, Object> node : root) {
			String name = stringVal(node, "name");
			if (name == null) continue;

			Long hash = longVal(node, "blockStateHash");
			if (hash == null) continue;

			byName.put(name, new PropTypeProps(name,
					doubleVal(node, "hardness", PROP_DEF_HARDNESS),
					doubleVal(node, "explosionResistance", PROP_DEF_EXPLOSION_RESISTANCE),
					doubleVal(node, "friction", PROP_DEF_FRICTION),
					doubleVal(node, "thickness", PROP_DEF_THICKNESS),
					doubleVal(node, "translucency", PROP_DEF_TRANSLUCENCY),
					intVal(node, "lightEmission", PROP_DEF_LIGHT_EMISSION),
					intVal(node, "lightDampening", PROP_DEF_LIGHT_DAMPENING),
					intVal(node, "burnOdds", PROP_DEF_BURN_ODDS),
					intVal(node, "flameOdds", PROP_DEF_FLAME_ODDS),
					boolVal(node, "isSolid", PROP_DEF_IS_SOLID),
					boolVal(node, "requiresCorrectToolForDrops", PROP_DEF_REQUIRES_TOOL),
					boolVal(node, "canContainLiquidSource", PROP_DEF_CAN_CONTAIN_LIQUID),
					stringVal(node, "mapColor"),
					stringVal(node, "tintMethod"),
					stringVal(node, "liquidReactionOnTouch")));

			List<List<Double>> boxes = new ArrayList<>();
			if (node.get("collisionShape") instanceof List<?> collisionList) {
				for (Object box : collisionList) {
					if (box instanceof List<?> boxCoords && boxCoords.size() >= 6) {
						List<Double> coords = new ArrayList<>(6);
						for (int coordIdx = 0; coordIdx < 6; coordIdx++)
							coords.add(((Number) boxCoords.get(coordIdx)).doubleValue());
						boxes.add(coords);
					}
				}
			}

			List<Double> outline = null;
			if (node.get("outlineShape") instanceof List<?> outlineList && outlineList.size() >= 6) {
				outline = new ArrayList<>(6);
				for (int coordIdx = 0; coordIdx < 6; coordIdx++)
					outline.add(((Number) outlineList.get(coordIdx)).doubleValue());
			}

			byHash.put(hash, new StateShapes(hash, boxes, outline));
		}

		TypeSpec typePropertiesClass = buildTypePropertiesClass();
		TypeSpec stateShapesRecord = TypeSpec.recordBuilder("StateShapes")
				.addModifiers(Modifier.PUBLIC)
				.recordConstructor(MethodSpec.constructorBuilder()
						.addParameter(TypeName.get(float[].class), "collisionBoxes")
						.addParameter(TypeName.get(float[].class), "outlineShape")
						.build())
				.addMethod(MethodSpec.methodBuilder("hasNoCollision")
						.addModifiers(Modifier.PUBLIC)
						.returns(TypeName.BOOLEAN)
						.addStatement("return collisionBoxes.length == 0")
						.build())
				.addMethod(MethodSpec.methodBuilder("collisionBoxCount")
						.addModifiers(Modifier.PUBLIC)
						.returns(TypeName.INT)
						.addStatement("return collisionBoxes.length / 6")
						.build())
				.build();

		TypeName mapLongSS = ParameterizedTypeName.get(LONG2OBJECT_MAP, STATE_SHAPES);

		List<List<StateShapes>> hashChunks = chunked(new ArrayList<>(byHash.values()), PROP_CHUNK);
		List<MethodSpec> fillMethods = new ArrayList<>();

		for (int chunkIndex = 0; chunkIndex < hashChunks.size(); chunkIndex++) {
			CodeBlock.Builder body = CodeBlock.builder();
			for (StateShapes stateShapes : hashChunks.get(chunkIndex)) {
				body.addStatement("m.put($LL, $L)", stateShapes.hash(), shapesLiteral(stateShapes));
			}
			fillMethods.add(MethodSpec.methodBuilder("fillByHash" + chunkIndex)
					.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
					.addParameter(mapLongSS, "m")
					.addCode(body.build())
					.build());
		}

		CodeBlock.Builder staticInit = CodeBlock.builder();
		staticInit.addStatement("$T<$T> byHash = new $T<>($L)", LONG2OBJECT_MAP, STATE_SHAPES, LONG2OBJECT_MAP, initialCapacity(byHash.size()));
		for (int i = 0; i < hashChunks.size(); i++) {
			staticInit.addStatement("fillByHash$L(byHash)", i);
		}
		staticInit.addStatement("BY_STATE_HASH = byHash");

		TypeSpec.Builder classBuilder = TypeSpec.classBuilder("BlockPropertyData")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addJavadoc("Generated by {@code BlockDataGen}, do not edit by hand.\n")
				.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
				.addType(typePropertiesClass)
				.addType(stateShapesRecord)
				.addField(FieldSpec.builder(mapLongSS, "BY_STATE_HASH",
						Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).build())
				.addStaticBlock(staticInit.build());

		for (MethodSpec fillMethod : fillMethods) {
			classBuilder.addMethod(fillMethod);
		}

		JavaFile.builder(SERVER_PACKAGE, classBuilder.build())
				.skipJavaLangImports(true)
				.indent("\t")
				.build()
				.writeTo(outputDir);
	}

	private static TypeSpec buildTypePropertiesClass() {
		List<FieldSpec> fields = List.of(
				field(TypeName.FLOAT, "hardness"),
				field(TypeName.FLOAT, "explosionResistance"),
				field(TypeName.FLOAT, "friction"),
				field(TypeName.FLOAT, "thickness"),
				field(TypeName.FLOAT, "translucency"),
				field(TypeName.INT, "lightEmission"),
				field(TypeName.INT, "lightDampening"),
				field(TypeName.INT, "burnOdds"),
				field(TypeName.INT, "flameOdds"),
				field(TypeName.BOOLEAN, "isSolid"),
				field(TypeName.BOOLEAN, "requiresCorrectToolForDrops"),
				field(TypeName.BOOLEAN, "canContainLiquidSource"),
				field(ClassName.get(String.class), "mapColor"),
				field(ClassName.get(String.class), "tintMethod"),
				field(ClassName.get(String.class), "liquidReactionOnTouch")
		);

		MethodSpec.Builder ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE);
		CodeBlock.Builder ctorBody = CodeBlock.builder();
		for (FieldSpec field : fields) {
			ctor.addParameter(field.type(), field.name());
			ctorBody.addStatement("this.$N = $N", field.name(), field.name());
		}

		List<MethodSpec> accessors = new ArrayList<>();
		for (FieldSpec field : fields) {
			accessors.add(MethodSpec.methodBuilder(field.name())
					.addModifiers(Modifier.PUBLIC)
					.returns(field.type())
					.addStatement("return this.$N", field.name())
					.build());
		}

		TypeSpec builderClass = buildBuilderClass();
		TypeSpec.Builder typePropsSpec = TypeSpec.classBuilder("TypeProperties")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
				.addMethod(ctor.addCode(ctorBody.build()).build());

		for (FieldSpec field : fields) {
			typePropsSpec.addField(field);
		}

		for (MethodSpec accessor : accessors) {
			typePropsSpec.addMethod(accessor);
		}

		typePropsSpec.addType(builderClass);
		return typePropsSpec.build();
	}

	private static TypeSpec buildBuilderClass() {
		TypeSpec.Builder builder = TypeSpec.classBuilder("Builder").addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

		builder.addField(FieldSpec.builder(TypeName.FLOAT, "hardness", Modifier.PRIVATE).initializer("$Lf", fmtd(PROP_DEF_HARDNESS)).build());
		builder.addField(FieldSpec.builder(TypeName.FLOAT, "explosionResistance", Modifier.PRIVATE).initializer("$Lf", fmtd(PROP_DEF_EXPLOSION_RESISTANCE)).build());
		builder.addField(FieldSpec.builder(TypeName.FLOAT, "friction", Modifier.PRIVATE).initializer("$Lf", fmtd(PROP_DEF_FRICTION)).build());
		builder.addField(FieldSpec.builder(TypeName.FLOAT, "thickness", Modifier.PRIVATE).initializer("$Lf", fmtd(PROP_DEF_THICKNESS)).build());
		builder.addField(FieldSpec.builder(TypeName.FLOAT, "translucency", Modifier.PRIVATE).initializer("$Lf", fmtd(PROP_DEF_TRANSLUCENCY)).build());
		builder.addField(FieldSpec.builder(TypeName.INT, "lightEmission", Modifier.PRIVATE).initializer("$L", PROP_DEF_LIGHT_EMISSION).build());
		builder.addField(FieldSpec.builder(TypeName.INT, "lightDampening", Modifier.PRIVATE).initializer("$L", PROP_DEF_LIGHT_DAMPENING).build());
		builder.addField(FieldSpec.builder(TypeName.INT, "burnOdds", Modifier.PRIVATE).initializer("$L", PROP_DEF_BURN_ODDS).build());
		builder.addField(FieldSpec.builder(TypeName.INT, "flameOdds", Modifier.PRIVATE).initializer("$L", PROP_DEF_FLAME_ODDS).build());
		builder.addField(FieldSpec.builder(TypeName.BOOLEAN, "isSolid", Modifier.PRIVATE).initializer("$L", PROP_DEF_IS_SOLID).build());
		builder.addField(FieldSpec.builder(TypeName.BOOLEAN, "requiresCorrectToolForDrops", Modifier.PRIVATE).initializer("$L", PROP_DEF_REQUIRES_TOOL).build());
		builder.addField(FieldSpec.builder(TypeName.BOOLEAN, "canContainLiquidSource", Modifier.PRIVATE).initializer("$L", PROP_DEF_CAN_CONTAIN_LIQUID).build());
		builder.addField(FieldSpec.builder(ClassName.get(String.class), "mapColor", Modifier.PRIVATE).build());
		builder.addField(FieldSpec.builder(ClassName.get(String.class), "tintMethod", Modifier.PRIVATE).initializer("$S", PROP_DEF_TINT_METHOD).build());
		builder.addField(FieldSpec.builder(ClassName.get(String.class), "liquidReactionOnTouch", Modifier.PRIVATE).initializer("$S", PROP_DEF_LIQUID_REACTION).build());

		ClassName builderName = ClassName.get("", "Builder");

		for (String name : List.of("hardness", "explosionResistance", "friction", "thickness", "translucency"))
			builder.addMethod(setter(builderName, name, TypeName.FLOAT));
		for (String name : List.of("lightEmission", "lightDampening", "burnOdds", "flameOdds"))
			builder.addMethod(setter(builderName, name, TypeName.INT));
		for (String name : List.of("mapColor", "tintMethod", "liquidReactionOnTouch"))
			builder.addMethod(setter(builderName, name, ClassName.get(String.class)));

		builder.addMethod(MethodSpec.methodBuilder("notSolid")
				.addModifiers(Modifier.PUBLIC).returns(builderName)
				.addStatement("this.isSolid = false").addStatement("return this").build());
		builder.addMethod(MethodSpec.methodBuilder("requiresCorrectToolForDrops")
				.addModifiers(Modifier.PUBLIC).returns(builderName)
				.addStatement("this.requiresCorrectToolForDrops = true").addStatement("return this").build());
		builder.addMethod(MethodSpec.methodBuilder("canContainLiquidSource")
				.addModifiers(Modifier.PUBLIC).returns(builderName)
				.addStatement("this.canContainLiquidSource = true").addStatement("return this").build());

		builder.addMethod(MethodSpec.methodBuilder("build")
				.addModifiers(Modifier.PUBLIC)
				.returns(TYPE_PROPERTIES)
				.addStatement("return new $T(hardness, explosionResistance, friction, thickness, translucency," +
						" lightEmission, lightDampening, burnOdds, flameOdds," +
						" isSolid, requiresCorrectToolForDrops, canContainLiquidSource," +
						" mapColor, tintMethod, liquidReactionOnTouch)", TYPE_PROPERTIES)
				.build());

		return builder.build();
	}

	private static FieldSpec field(TypeName type, String name) {
		return FieldSpec.builder(type, name, Modifier.PRIVATE, Modifier.FINAL).build();
	}

	private static MethodSpec setter(ClassName builderName, String name, TypeName type) {
		return MethodSpec.methodBuilder(name)
				.addModifiers(Modifier.PUBLIC).returns(builderName)
				.addParameter(type, name)
				.addStatement("this.$N = $N", name, name)
				.addStatement("return this")
				.build();
	}

	private static String shapesLiteral(StateShapes shapes) {
		List<Double> flat = new ArrayList<>();
		for (List<Double> box : shapes.collisionBoxes()) flat.addAll(box);
		String boxes = flat.isEmpty() ? "new float[0]"
				: "new float[]{" + joinFloats(flat) + "}";
		String outline = shapes.outlineShape() == null ? "null"
				: "new float[]{" + joinFloats(shapes.outlineShape()) + "}";
		return "new StateShapes(" + boxes + ", " + outline + ")";
	}

	private static String joinFloats(List<Double> values) {
		StringBuilder sb = new StringBuilder();
		for (int idx = 0; idx < values.size(); idx++) {
			if (idx > 0) sb.append(',');
			sb.append(fmtd(values.get(idx))).append('f');
		}
		return sb.toString();
	}

	private static Map<String, String> buildWireToConstMap(Path blockIdsJava, Path blockTypesJava) throws IOException {
		String idsSource = Files.readString(blockIdsJava);
		String typesSource = Files.readString(blockTypesJava);

		Map<String, String> wireToIdsConst = new HashMap<>();
		Pattern idsPattern = Pattern.compile(
				"public\\s+static\\s+final\\s+Identifier\\s+(\\w+)\\s*=\\s*Identifier\\.parse\\(\"([^\"]+)\"\\)");
		Matcher idsMatcher = idsPattern.matcher(idsSource);
		while (idsMatcher.find()) wireToIdsConst.put(idsMatcher.group(2), idsMatcher.group(1));

		Map<String, String> idsConstToTypesConst = new HashMap<>();
		Pattern typesPattern = Pattern.compile(
				"public\\s+static\\s+final\\s+BlockType\\s+(\\w+)\\s*=\\s*BlockType\\.of\\(BlockIds\\.(\\w+)");
		Matcher typesMatcher = typesPattern.matcher(typesSource);
		while (typesMatcher.find()) idsConstToTypesConst.put(typesMatcher.group(2), typesMatcher.group(1));

		Map<String, String> wireToTypesConst = new HashMap<>();
		for (Map.Entry<String, String> entry : wireToIdsConst.entrySet()) {
			String typesConst = idsConstToTypesConst.get(entry.getValue());
			if (typesConst != null) wireToTypesConst.put(entry.getKey(), typesConst);
		}
		return wireToTypesConst;
	}

	private static String fmtf(float f) {
		if (f == 0.0f) return "0";
		if (f == 1.0f) return "1";
		return String.format("%.6f", f).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	private static String fmtd(double d) {
		if (d == 0.0) return "0";
		if (d == 1.0) return "1";
		return String.format("%.6f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	private static int initialCapacity(int size) {
		return (size * 4 / 3) + 1;
	}

	private static <T> List<List<T>> chunked(List<T> list, int chunkSize) {
		List<List<T>> result = new ArrayList<>();
		for (int offset = 0; offset < list.size(); offset += chunkSize)
			result.add(list.subList(offset, Math.min(offset + chunkSize, list.size())));
		return result;
	}

	private static Path resolveProjectRoot() {
		Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		while (dir != null) {
			if (Files.exists(dir.resolve("settings.gradle.kts"))) return dir;
			dir = dir.getParent();
		}
		throw new IllegalStateException("Could not locate project root (no settings.gradle.kts found)");
	}

	private static String stringVal(Map<String, Object> node, String key) {
		Object value = node.get(key);
		return value != null ? value.toString() : null;
	}

	private static Long longVal(Map<String, Object> node, String key) {
		Object value = node.get(key);
		return value instanceof Number number ? number.longValue() : null;
	}

	private static float floatVal(Map<String, Object> node, String key, float defaultValue) {
		Object value = node.get(key);
		return value instanceof Number number ? number.floatValue() : defaultValue;
	}

	private static double doubleVal(Map<String, Object> node, String key, double defaultValue) {
		Object value = node.get(key);
		return value instanceof Number number ? number.doubleValue() : defaultValue;
	}

	private static int intVal(Map<String, Object> node, String key, int defaultValue) {
		Object value = node.get(key);
		return value instanceof Number number ? number.intValue() : defaultValue;
	}

	private static boolean boolVal(Map<String, Object> node, String key, boolean defaultValue) {
		Object value = node.get(key);
		return value instanceof Boolean bool ? bool : defaultValue;
	}

	private record TypeProps(
			String constantName,
			float hardness, float resistance, float friction,
			float translucency, float thickness,
			int burnOdds, int flameOdds, int lightDampening,
			int lightEmission,
			boolean solid, boolean requiresCorrectTool,
			String mapColor, String liquidReactionOnTouch
	) {
	}

	private record PropTypeProps(
			String name,
			double hardness, double explosionResistance, double friction,
			double thickness, double translucency,
			int lightEmission, int lightDampening,
			int burnOdds, int flameOdds,
			boolean isSolid, boolean requiresCorrectToolForDrops, boolean canContainLiquidSource,
			String mapColor, String tintMethod, String liquidReactionOnTouch
	) {
	}

	private record StateShapes(
			long hash,
			List<List<Double>> collisionBoxes,
			List<Double> outlineShape
	) {
	}
}
