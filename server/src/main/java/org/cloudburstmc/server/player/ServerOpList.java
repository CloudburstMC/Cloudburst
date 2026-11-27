package org.cloudburstmc.server.player;

import java.nio.file.Path;
import java.util.UUID;

public class ServerOpList extends StoredUserList<ServerOpListEntry> {

    public ServerOpList(Path path) {
        super(path, ServerOpListEntry[].class, (reserved, uuid, name) -> new ServerOpListEntry(uuid, name, reserved.level(), reserved.bypassesPlayerLimit()));
    }

    public int getLevel(UUID uuid) {
        return get(uuid).map(ServerOpListEntry::level).orElse(0);
    }

    public boolean canBypassPlayerLimit(UUID uuid) {
        return get(uuid).map(ServerOpListEntry::bypassesPlayerLimit).orElse(false);
    }
}
