package org.cloudburstmc.server.metadata;

import org.cloudburstmc.api.plugin.PluginContainer;

import java.lang.ref.WeakReference;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class MetadataValue {

    protected final WeakReference<PluginContainer> owningPlugin;

    protected MetadataValue(PluginContainer owningPlugin) {
        this.owningPlugin = new WeakReference<>(owningPlugin);
    }

    public PluginContainer getOwningPlugin() {
        return this.owningPlugin.get();
    }

    public abstract Object value();

    public abstract void invalidate();

}
