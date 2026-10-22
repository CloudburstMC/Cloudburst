# Nukkit Migration

Imports Nukkit Anvil and LevelDB worlds.

## Importing

1. Stop Nukkit and Cloudburst.
2. Copy the Nukkit world into Cloudburst's configured levels directory.
3. Configure that directory name in `config/cloudburst-global.yml` with the intended
   dimension and generator.
4. Start Cloudburst. The world is converted automatically before loading.

The original is retained as `.<level-name>.nukkit-backup` beside the converted world.
Keep it until the converted world has been checked. Existing backups are never overwritten.

LevelDB imports only the configured dimension. Other dimensions remain in the backup.
New terrain uses the configured generator.

If startup reports an interrupted conversion, stop Cloudburst. Move any incomplete
converted world aside and restore the backup to the original directory name.
Before retrying, move any remaining backup out of the levels directory rather than deleting it.

## Compatibility

Unknown block and item mappings fail import. Older releases and forks are compatible only
when their storage formats and numeric ID assignments match these mappings.

Anvil chunks containing nonempty `ExtraData` are not supported and fail conversion.

## Mapping Sources

These resources are copied from [Nukkit commit 935d60c](https://github.com/CloudburstMC/Nukkit/tree/935d60c50f3860b434c9c7fdaa30253393ae8724/src/main/resources).

| Local file           | Nukkit source                                    |
| -------------------- | ------------------------------------------------ |
| `block_states.nbt`   | `runtime_block_states.dat` (gzip-compressed NBT) |
| `item_ids.json`      | `legacy_item_ids.json`                           |
| `item_variants.json` | `item_mappings.json`                             |

`block_save_ids.json` is derived from `block/Blocks.java` and `block/BlockID.java`
under `src/main/java/cn/nukkit` at the same commit.

## Updating

When supporting another Nukkit version:

1. Compare its mapping resources, block registrations and storage serializers
   with the pinned commit.
2. Check numeric IDs and metadata meanings. Changed assignments need version-specific
   mappings, not replacement with current Bedrock ID tables.
3. Verify block variants, item durability, inventories and scheduled ticks.
4. Update the mappings and source link to the reviewed commit.
