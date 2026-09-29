# Code generation

The `codegen` module generates API catalogs from vanilla data. It writes Java source files to `api/src/main/java`.

Run the generator from the repository root:

```shell
./gradlew :codegen:generateVanillaData
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Generated catalogs

The command runs all four generators:

| Generator         | Output                                                                           |
| ----------------- | -------------------------------------------------------------------------------- |
| `BiomeDataGen`    | `BiomeTypes`, `BiomeBehaviorComponentTypes`, and `BiomeAppearanceComponentTypes` |
| `ItemDataGen`     | `ItemTypes` and `ItemDefinitionComponents`                                       |
| `ParticleDataGen` | `ParticleTypes` and `ParticleEmitterTypes`                                       |
| `SoundDataGen`    | `SoundTypes`                                                                     |

Do not edit these generated classes by hand. Update their inputs and run the generator again.

## Source layout

Under `org.cloudburstmc.codegen`, the `biome`, `item`, `particle`, and `sound` packages contain the generators. Shared helpers are in `support`, and `VanillaDataGen` is the entry point.

Input catalogs under `codegen/src/main/resources` use matching domain folders.

## Inputs

Runtime data is read from the Data submodule at `server/src/main/resources/data`.

Initialize that submodule from the repository root when the files are missing:

```shell
git submodule update --init server/src/main/resources/data
```

| Input                             | Used for                                       |
| --------------------------------- | ---------------------------------------------- |
| `stripped_biome_definitions.json` | Built-in biome identifiers                     |
| `runtime_item_states.json`        | Built-in item identifiers                      |
| `item_mappings.json`              | Item aliases excluded from separate item types |

The following identifier catalogs are stored in `codegen/src/main/resources`:

| Input                                    | Used for                                |
| ---------------------------------------- | --------------------------------------- |
| `biome/biome_behavior_components.json`   | Server-side biome component identifiers |
| `biome/biome_appearance_components.json` | Client-side biome component identifiers |
| `item/item_definition_components.json`   | Item-definition component identifiers   |
| `particle/particle_emitters.json`        | Named particle emitter identifiers      |
| `sound/sound_types.json`                 | Named sound identifiers                 |

Each catalog records its source version in `format_version`. Component catalogs have a sorted `components` array of fully qualified identifiers. The emitter catalog has a sorted `emitters` array of fully qualified identifiers. The sound catalog has a sorted `sounds` array of names as they appear in `sound_definitions.json`, including dots and underscores.

The particle generator reads `ParticleType` from the configured Protocol dependency and excludes `UNDEFINED`.

Resource-pack particle emitters belong in `ParticleEmitterTypes`, not `ParticleTypes`. Custom emitters can be referenced with `ParticleEmitterType.of` without adding them to the generated catalog.

## Updating vanilla data

1. Update the Data submodule with captures for the target Minecraft version.
2. Download and extract the matching release from [Mojang's Bedrock samples](https://github.com/Mojang/bedrock-samples/releases). The `min` archive includes the required JSON files. [The resource-pack template link](https://aka.ms/resourcepacktemplate) also leads to these releases.
3. Update the identifier catalogs using the sources below.
4. Update the configured Protocol dependency when its particle catalog changes.
5. Run `:codegen:generateVanillaData`.
6. Review the generated source changes together with the input changes.

Paths below are relative to the extracted samples:

- Collect biome behavior components from the `components` objects in `behavior_pack/biomes/*.biome.json`.
- Collect biome appearance components from the `components` objects in `resource_pack/biomes/*.client_biome.json`.
- Collect item-definition components from the property names in `metadata/json_schemas/server/item/<version>/Item Components.json`.
- Collect sound names from the keys of `sound_definitions` in `resource_pack/sounds/sound_definitions.json`.
- Collect emitter identifiers from `particle_effect.description.identifier` in JSON files under `resource_pack/particles`.

Store the collected identifiers in the catalogs above, not copies of the full packs or schemas.

## Validation

Generation rejects duplicate identifiers, unsupported namespaces, and Java constant-name collisions. Review generated removals carefully because an incomplete input can remove public API constants.

Commit generated outputs and their updated inputs together.
