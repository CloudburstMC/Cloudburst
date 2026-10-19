package org.cloudburstmc.server.player;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.persistence.StoredJsonList;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Caches observed player identities without inventing UUIDs or performing network lookups.
 */
@Log4j2
public class UserProfileCache implements AutoCloseable {

    private final StoredJsonList<UUID, CachedProfile> storage;
    private final int savedProfileLimit;
    private final boolean saveOnStopOnly;
    private final Clock clock;
    private final ExecutorService writer;

    private final Index index;
    private List<CachedProfile> pendingSave;
    private CompletableFuture<Void> saveCompletion = CompletableFuture.completedFuture(null);
    private RuntimeException saveFailure;
    private boolean saveScheduled;
    private boolean closed;

    public UserProfileCache(Path path, int savedProfileLimit, boolean saveOnStopOnly) {
        this(path, savedProfileLimit, saveOnStopOnly, Clock.systemDefaultZone());
    }

    UserProfileCache(Path path, int savedProfileLimit, boolean saveOnStopOnly, Clock clock) {
        if (savedProfileLimit < 0) {
            throw new IllegalArgumentException("Saved profile limit must not be negative");
        }

        this.storage = new StoredJsonList<>(path, CachedProfile[].class, CachedProfile::uuid);
        this.savedProfileLimit = savedProfileLimit;
        this.saveOnStopOnly = saveOnStopOnly;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.index = createIndex(storage.entries().reversed());
        this.writer = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("Cloudburst User Cache Writer").factory());
    }

    public synchronized Optional<CachedProfile> find(UUID uuid) {
        CachedProfile profile = index.profiles().get(Objects.requireNonNull(uuid, "uuid"));
        if (profile == null) {
            return Optional.empty();
        }

        if (!clock.instant().isBefore(profile.expiresOn())) {
            index.profiles().remove(uuid);
            index.names().remove(profile.name().toLowerCase(Locale.ROOT));

            if (!closed && !saveOnStopOnly) {
                requestSave();
            }

            return Optional.empty();
        }

        return Optional.of(profile);
    }

    public synchronized Optional<UUID> find(String name) {
        UUID uuid = index.names().get(Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT));
        return uuid == null ? Optional.empty() : find(uuid).map(CachedProfile::uuid);
    }

    public synchronized void add(UUID uuid, String name) {
        requireOpen();
        CachedProfile profile = new CachedProfile(uuid, name, ZonedDateTime.now(clock).plusMonths(1).toInstant());
        CachedProfile previous = index.profiles().remove(uuid);
        if (previous != null) {
            index.names().remove(previous.name().toLowerCase(Locale.ROOT), uuid);
        }

        UUID previousIdentity = index.names().put(name.toLowerCase(Locale.ROOT), uuid);
        if (previousIdentity != null && !previousIdentity.equals(uuid)) {
            index.profiles().remove(previousIdentity);
        }

        index.profiles().put(uuid, profile);
        if (!saveOnStopOnly) {
            requestSave();
        }
    }

    /**
     * Saves the current recent-profile snapshot and waits for its write to finish.
     */
    public void flush() {
        CompletableFuture<Void> completion;
        synchronized (this) {
            requireOpen();
            requestSave();
            completion = saveCompletion;
        }

        completion.join();
        checkSaveFailure();
    }

    /**
     * Drains pending writes and saves the final access order before releasing the writer.
     */
    @Override
    public void close() {
        CompletableFuture<Void> completion;
        synchronized (this) {
            if (closed) {
                return;
            }

            closed = true;
            requestSave();
            completion = saveCompletion;
        }

        try {
            completion.join();
            checkSaveFailure();
        } finally {
            writer.close();
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("User profile cache is closed");
        }
    }

    private void requestSave() {
        Instant now = clock.instant();
        pendingSave = new ArrayList<>(index.profiles().values()).reversed().stream()
                .filter(entry -> now.isBefore(entry.expiresOn()))
                .limit(savedProfileLimit).toList();

        if (!saveScheduled) {
            saveScheduled = true;
            saveCompletion = CompletableFuture.runAsync(this::writePending, writer);
        }
    }

    private void writePending() {
        while (true) {
            List<CachedProfile> snapshot;
            synchronized (this) {
                snapshot = pendingSave;
                pendingSave = null;
                if (snapshot == null) {
                    saveScheduled = false;
                    return;
                }
            }

            try {
                storage.replace(snapshot);
                synchronized (this) {
                    saveFailure = null;
                }
            } catch (RuntimeException failure) {
                synchronized (this) {
                    saveFailure = failure;
                }

                log.error("Unable to save user profile cache", failure);
            }
        }
    }

    private synchronized void checkSaveFailure() {
        if (saveFailure != null) {
            throw new IllegalStateException("Unable to flush user profile cache", saveFailure);
        }
    }

    private static Index createIndex(List<CachedProfile> entries) {
        Map<UUID, CachedProfile> profiles = new LinkedHashMap<>(entries.size(), 0.75f, true);
        Map<String, UUID> names = new HashMap<>();
        for (CachedProfile entry : entries) {
            String key = entry.name().toLowerCase(Locale.ROOT);
            if (names.putIfAbsent(key, entry.uuid()) != null) {
                throw new IllegalArgumentException("Duplicate player name in identity cache: " + entry.name());
            }

            profiles.put(entry.uuid(), entry);
        }

        return new Index(profiles, names);
    }

    private record Index(Map<UUID, CachedProfile> profiles, Map<String, UUID> names) {
    }

    public record CachedProfile(UUID uuid, String name, @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresOn) {
        public CachedProfile {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("Player name must not be blank");
            }

            Objects.requireNonNull(expiresOn, "expiresOn");
        }
    }
}
