# Nukkit Mappings

Numeric block and item mappings used by the Anvil and LevelDB import decoders.
See [World imports](../README.md) for setup and backup handling.

## Compatibility

Unknown block and item mappings fail import. Older releases and forks are compatible only
when their storage formats and numeric ID assignments match these mappings.

## Mapping Sources

These resources are copied from [Nukkit commit 935d60c](https://github.com/CloudburstMC/Nukkit/tree/935d60c50f3860b434c9c7fdaa30253393ae8724/src/main/resources).

| Local file           | Nukkit source                                    |
| -------------------- | ------------------------------------------------ |
| `block_states.nbt`    | `runtime_block_states.dat` (gzip-compressed NBT)  |
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
