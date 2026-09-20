# Code generation

The `codegen` module builds Java catalogs from vanilla data. Generated source files are written directly into the API module and are committed with the input data that produced them.

Run the generator from the repository root:

```shell
./gradlew :codegen:generateVanillaData
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Generated catalogs

`VanillaDataGen` runs the biome and item generators.

| Generator      | Output                                                                           |
| -------------- | -------------------------------------------------------------------------------- |
| `BiomeDataGen` | `BiomeTypes`, `BiomeBehaviorComponentTypes`, and `BiomeAppearanceComponentTypes` |
| `ItemDataGen`  | `ItemTypes` and `ItemDefinitionComponents`                                       |

Do not edit these generated classes by hand. Update their inputs and run the generator again.

## Inputs

Runtime data is read from the Data submodule at `server/src/main/resources/data`.

Initialize that submodule from the repository root when the files are missing:

```shell
git submodule update --init server/src/main/resources/data
```

| Input                             | Used for                                              |
| --------------------------------- | ----------------------------------------------------- |
| `stripped_biome_definitions.json` | Built-in biome identifiers                            |
| `runtime_item_states.json`        | Built-in item identifiers                             |
| `item_mappings.json`              | Item aliases that must not become separate item types |

Small component catalogs are stored in `codegen/src/main/resources`.

| Input                              | Used for                                |
| ---------------------------------- | --------------------------------------- |
| `biome_behavior_components.json`   | Server-side biome component identifiers |
| `biome_appearance_components.json` | Client-side biome component identifiers |
| `item_definition_components.json`  | Item-definition component identifiers   |

Each component catalog contains a `format_version` for its source version and a `components` array of fully qualified identifiers. Keep the array sorted and do not include experimental components unless Cloudburst supports the experiment.

## Updating vanilla data

1. Update the Data submodule with captures for the target Minecraft version.
2. Download and extract the matching release from [Mojang's Bedrock samples](https://aka.ms/resourcepacktemplate). The `min` archive is sufficient because the generator only needs text files.
3. Update the component catalogs from the sources below.
4. Run `:codegen:generateVanillaData`.
5. Review the generated source changes together with the input changes.

Use these vanilla sources when updating component catalogs:

- Collect biome behavior components from the `components` objects in `behavior_pack/biomes/*.biome.json`.
- Collect biome appearance components from the `components` objects in `resource_pack/biomes/*.client_biome.json`. Older templates may instead define them in `resource_pack/biomes_client.json`.
- Collect item-definition components from the property names in `metadata/json_schemas/server/item/<version>/Item Components.json`.

The extracted samples are reference sources. Do not copy the packs or schemas into this repository. Store only the normalized component identifiers required by the generator.

## Validation

Generation fails when identifiers are duplicated, use an unsupported namespace, or map to the same Java constant. After generation, review removals carefully because a missing input can remove a public API constant.

Commit generated outputs and their updated inputs together so the source of every catalog change remains clear.
