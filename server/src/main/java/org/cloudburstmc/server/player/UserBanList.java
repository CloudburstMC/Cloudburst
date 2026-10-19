package org.cloudburstmc.server.player;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.ban.BanEntry;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UserBanList extends CloudBanList<UUID, UserBanList.SavedBan> {

    private final Function<UUID, String> names;

    public UserBanList(Path path, Function<UUID, String> names) {
        super(path, SavedBan[].class, entry -> entry.uuid().toString());
        this.names = Objects.requireNonNull(names, "names");
    }

    public Optional<String> findName(UUID uuid) {
        return storedEntries().stream().filter(entry -> entry.uuid().equals(uuid))
                .map(SavedBan::name).filter(Objects::nonNull).findFirst();
    }

    public Set<UUID> findIdentities(String name) {
        return storedEntries().stream()
                .filter(entry -> entry.name() != null && entry.name().equalsIgnoreCase(name))
                .map(SavedBan::uuid).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected String key(UUID target) {
        return target.toString();
    }

    @Override
    protected SavedBan encode(BanEntry<UUID> entry) {
        String name = names.apply(entry.target());
        if (name == null) {
            name = findName(entry.target()).orElse(null);
        }

        return new SavedBan(entry.target(), name, entry.created(), entry.source(), entry.expiration(), entry.reason());
    }

    @Override
    protected BanEntry<UUID> decode(SavedBan entry) {
        return new BanEntry<>(entry.uuid(), entry.created(), entry.source(), entry.expires(), entry.reason());
    }

    public record SavedBan(UUID uuid, @Nullable String name,
                           @JsonFormat(shape = JsonFormat.Shape.STRING) Instant created,
                           String source, @Nullable @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expires,
                           String reason) {
        public SavedBan {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(created, "created");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(reason, "reason");
        }
    }
}
