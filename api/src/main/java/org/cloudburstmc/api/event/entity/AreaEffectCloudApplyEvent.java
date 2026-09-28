package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.misc.AreaEffectCloud;
import org.cloudburstmc.api.event.Cancellable;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Called before an area effect cloud applies its effects.
 */
public class AreaEffectCloudApplyEvent extends EntityEvent implements Cancellable {
    private final List<Living> affectedEntities;

    /**
     * Creates an area effect cloud application event.
     *
     * @param areaEffectCloud  the applying cloud
     * @param affectedEntities the mutable list of entities selected for this application
     */
    public AreaEffectCloudApplyEvent(AreaEffectCloud areaEffectCloud, List<Living> affectedEntities) {
        this.entity = requireNonNull(areaEffectCloud, "areaEffectCloud");
        this.affectedEntities = new ArrayList<>(requireNonNull(affectedEntities, "affectedEntities"));
        checkArgument(!this.affectedEntities.contains(null), "affectedEntities cannot contain null");
    }

    @Override
    public AreaEffectCloud getEntity() {
        return (AreaEffectCloud) this.entity;
    }

    /**
     * Returns the mutable list of entities selected for this application.
     * The cloud may expire while processing this list when its radius or duration
     * changes on use.
     *
     * @return affected entities
     */
    public List<Living> getAffectedEntities() {
        return this.affectedEntities;
    }
}
