package cn.nukkit.pack;

import cn.nukkit.pack.loader.PackLoader;
import com.nukkitx.protocol.bedrock.data.ResourcePackType;

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
