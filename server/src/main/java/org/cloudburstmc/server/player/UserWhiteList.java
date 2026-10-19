package org.cloudburstmc.server.player;

import java.nio.file.Path;

public class UserWhiteList extends StoredUserList<UserWhiteListEntry> {

    public UserWhiteList(Path path) {
        super(path, UserWhiteListEntry[].class);
    }
}
