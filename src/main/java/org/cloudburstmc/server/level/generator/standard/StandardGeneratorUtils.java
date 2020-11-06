package org.cloudburstmc.server.level.generator.standard;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.common.util.PorkUtil;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.CloudServer;
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
                in = Bootstrap.class.getClassLoader().getResourceAsStream(name);
                break;
            default:
                val plugin = CloudServer.getInstance().getPluginManager().getPlugin(id.getNamespace());
                if (plugin.isPresent()) {
                    in = plugin.get().getPlugin().getClass().getClassLoader().getResourceAsStream(name);
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
