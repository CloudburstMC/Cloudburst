package org.cloudburstmc.server.pack;

import com.nukkitx.protocol.bedrock.data.ResourcePackType;
import org.cloudburstmc.server.pack.loader.PackLoader;

public class ResourcePack extends Pack {
    public static final Pack.Factory FACTORY = ResourcePack::new;

    private ResourcePack(PackLoader loader, PackManifest manifest, PackManifest.Module module) {
        super(loader, manifest, module);
    }

    @Override
    public ResourcePackType getType() {
        return ResourcePackType.RESOURCE;
    }

    @Override
    public String toString() {
        return "Resource" + super.toString();
    }
}
