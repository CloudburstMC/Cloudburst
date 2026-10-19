package org.cloudburstmc.server.player;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.net.InetAddresses;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.ban.BanEntry;

import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public class IpBanList extends CloudBanList<InetAddress, IpBanList.SavedBan> {

    public IpBanList(Path path) {
        super(path, SavedBan[].class, SavedBan::ip);
    }

    @Override
    protected String key(InetAddress target) {
        return InetAddresses.toAddrString(target);
    }

    @Override
    protected SavedBan encode(BanEntry<InetAddress> entry) {
        return new SavedBan(key(entry.target()), entry.created(), entry.source(), entry.expiration(), entry.reason());
    }

    @Override
    protected BanEntry<InetAddress> decode(SavedBan entry) {
        return new BanEntry<>(InetAddresses.forString(entry.ip()), entry.created(), entry.source(), entry.expires(), entry.reason());
    }

    public record SavedBan(String ip, @JsonFormat(shape = JsonFormat.Shape.STRING) Instant created,
                           String source, @Nullable @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expires,
                           String reason) {
        public SavedBan {
            ip = InetAddresses.toAddrString(InetAddresses.forString(ip));
            Objects.requireNonNull(created, "created");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(reason, "reason");
        }
    }
}
