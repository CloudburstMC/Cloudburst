package org.cloudburstmc.server.level.generator.standard;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import net.daporkchop.lib.common.misc.file.PFiles;
import org.cloudburstmc.server.Nukkit;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.utils.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Various helper methods used by the Cloudburst standard generator.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class StandardGeneratorUtils {

    public static InputStream read(@NonNull String category, @NonNull Identifier id) throws IOException {
        String name = String.format("generator/%s/%s/%s.yml", category, id.getNamespace(), id.getName());

        File file = new File(name);
        if (PFiles.checkFileExists(file)) {
            return new BufferedInputStream(new FileInputStream(file));
        }

        InputStream in = null;
        switch (id.getNamespace()) {
            case "minecraft":
            case "cloudburst":
                in = Nukkit.class.getClassLoader().getResourceAsStream(name);
                break;
            default:
                val plugin = Server.getInstance().getPluginManager().getPlugin(id.getNamespace());
                if (plugin.isPresent()) {
                    in = plugin.get().getResource(name);
                }
        }
        if (in == null) {
            throw new FileNotFoundException(name);
        } else {
            return in;
        }
    }

    /**
     * Hashes a {@link String} to a 64-bit value.
     *
     * @param text the text to hash
     * @return the hashed value
     */
    public static long hash(@NonNull String text) {
        UUID uuid = UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8));
        return uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits();
    }
}
