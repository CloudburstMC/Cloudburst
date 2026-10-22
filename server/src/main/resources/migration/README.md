# World Imports

Cloudburst detects Anvil regions and LevelDB databases before opening a world.
Imports run automatically. No separate command is required.

## Importing

1. Stop the source server and Cloudburst.
2. Copy the world into Cloudburst's configured worlds directory.
3. Configure its directory name, dimension and generator in `config/cloudburst-global.yml`.
4. Start Cloudburst and check the converted world before removing the backup.

The original is retained as `.<world-name>.import-backup` beside the converted world.
Existing backups are never overwritten. A failure before publication leaves the original
directory untouched. If publication cannot restore the original directory layout after
a failed move, the original remains in the backup.

For a combined Bedrock world, configure its Overworld as `default-level` in `server.properties`.
Cloudburst imports its dimensions together before loading any worlds. The configured Nether
and End receive their saved chunks from the same database. The Nether and End may each have
at most one enabled configured destination. Existing destination directories must be moved aside first.

Dimensions without saved chunks use the source seed and the configured generator for
new terrain. Disabled or unconfigured dimensions remain in the complete source backup.
Other LevelDB imports convert only the configured dimension.

If startup reports an interrupted conversion, stop Cloudburst. Move any incomplete
converted worlds aside and restore the reported backup to the original directory name.
Move any remaining backup out of the worlds directory before retrying.

## Encodings

Anvil imports support Nukkit's byte-array and packed block encodings, including compatible forks.
Java Edition Anvil worlds are not supported.

LevelDB imports distinguish named Bedrock data from Nukkit's numeric mappings.
Generator names do not select an encoding. Malformed states and unsupported encodings fail import.

When metadata cannot identify a LevelDB encoding, set `import-profile` to `bedrock` or
`nukkit` under that world's configuration. Only select a profile after checking the source
format. Selecting `bedrock` does not enable legacy numeric Bedrock mappings.

See [Nukkit mappings](nukkit/README.md) for the sources and update process for numeric IDs.

## Unknown Blocks

Bedrock blocks with unregistered names become `minecraft:unknown`, with one warning per name.
These replacements do not retain the original block state in the converted world.
Fix the source world and import it again to restore those blocks. The complete original
remains in the backup.
