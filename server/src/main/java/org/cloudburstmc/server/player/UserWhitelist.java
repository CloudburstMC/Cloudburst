package org.cloudburstmc.server.player;

import java.nio.file.Path;

public class UserWhitelist extends StoredUserList<UserWhitelistEntry> {

    public UserWhitelist(Path path) {
        super(path, UserWhitelistEntry[].class, (reserved, uuid, name) -> new UserWhitelistEntry(uuid, name));
    }
}
