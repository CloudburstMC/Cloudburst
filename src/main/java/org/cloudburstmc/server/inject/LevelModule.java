package org.cloudburstmc.server.inject;

import com.google.inject.AbstractModule;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cloudburstmc.server.level.LevelData;
import org.cloudburstmc.server.level.provider.LevelProvider;

@Value
@EqualsAndHashCode(callSuper = false)
public class LevelModule extends AbstractModule {
    String id;
    LevelProvider provider;
    LevelData data;

    @Override
    protected void configure() {
        this.bind(String.class).toInstance(this.id);
        this.bind(LevelProvider.class).toInstance(this.provider);
        this.bind(LevelData.class).toInstance(this.data);
    }
}
