package org.cloudburstmc.api.pack;

import org.cloudburstmc.api.pack.loader.PackLoader;

public class ResourcePack extends Pack {
    public static final Pack.Factory FACTORY = ResourcePack::new;

    private ResourcePack(PackLoader loader, PackManifest manifest, PackManifest.Module module) {
        super(loader, manifest, module);
    }

    @Override
    public PackType getType() {
        return PackType.RESOURCES;
    }

    @Override
    public String toString() {
        return "Resource" + super.toString();
    }
}
