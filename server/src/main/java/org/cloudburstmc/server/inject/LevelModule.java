package org.cloudburstmc.server.inject;

import com.google.inject.AbstractModule;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.provider.LevelProvider;

@Value
@EqualsAndHashCode(callSuper = false)
public class LevelModule extends AbstractModule {
    String id;
    LevelProvider provider;
    CloudLevelData data;

    @Override
    protected void configure() {
        this.bind(String.class).toInstance(this.id);
        this.bind(LevelProvider.class).toInstance(this.provider);
        this.bind(CloudLevelData.class).toInstance(this.data);
    }
}
