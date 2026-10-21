# Nukkit Migration Data

Copied unchanged from [Nukkit commit 935d60c](https://github.com/CloudburstMC/Nukkit/tree/935d60c50f3860b434c9c7fdaa30253393ae8724/src/main/resources).

| Local file           | Nukkit source                                    |
| -------------------- | ------------------------------------------------ |
| `block_states.nbt`   | `runtime_block_states.dat` (gzip-compressed NBT) |
| `item_ids.json`      | `legacy_item_ids.json`                           |
| `item_variants.json` | `item_mappings.json`                             |

Mappings for the Nukkit Anvil and LevelDB importers.

## Importing

Stop the source server and place the world in Cloudburst's levels directory.
Loading a Nukkit world automatically converts it before opening it for play.
Conversion retains the original as `.<level-name>.nukkit-backup` beside the world.
Keep that backup until the converted world has been checked.

The LevelDB importer handles one dimension at a time (`0` Overworld, `1` Nether,
`2` End), selected by the level configuration. The backup retains all source dimensions.
New terrain uses the generator configured for that level.

If conversion is interrupted after the original directory is moved, loading fails
instead of creating a new world. Restore the backup to the original name before retrying.

Unknown references fail import. Older releases and forks are compatible only
when their storage formats and numeric ID assignments match these mappings.

## Updating

Review these resources when supporting another Nukkit version.

1. Compare the source files, palette mapping code and storage serializers against
   the pinned commit.
2. Check ID and metadata changes. Keep separate version-specific mappings if an
   existing assignment changed meaning. Do not substitute current Bedrock ID tables.
3. Verify conversions of block variants, item durability, inventories and pending ticks.
4. Copy the reviewed resources and update the source link to the exact commit.
