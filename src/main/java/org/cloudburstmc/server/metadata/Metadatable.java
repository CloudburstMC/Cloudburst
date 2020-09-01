package org.cloudburstmc.server.metadata;

import org.cloudburstmc.server.plugin.PluginContainer;

import java.util.List;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public interface Metadatable {

    void setMetadata(String metadataKey, MetadataValue newMetadataValue) throws Exception;

    List<MetadataValue> getMetadata(String metadataKey) throws Exception;

    boolean hasMetadata(String metadataKey) throws Exception;

    void removeMetadata(String metadataKey, PluginContainer owningPlugin) throws Exception;
}
