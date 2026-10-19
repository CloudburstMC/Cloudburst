package org.cloudburstmc.server.player;

import java.nio.file.Path;
import java.util.UUID;

public class ServerOpList extends StoredUserList<ServerOpListEntry> {

    public ServerOpList(Path path) {
        super(path, ServerOpListEntry[].class);
    }

    public boolean canBypassPlayerLimit(UUID uuid) {
        return get(uuid).map(ServerOpListEntry::bypassesPlayerLimit).orElse(false);
    }
}
