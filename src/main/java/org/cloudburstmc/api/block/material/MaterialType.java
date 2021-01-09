package org.cloudburstmc.api.block.material;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnegative;
import javax.annotation.concurrent.Immutable;


@Immutable
public final class MaterialType {
    private final float translucency;
    private final boolean neverBuildable;
    private final boolean liquid;
    private final boolean solid;
    private final boolean superHot;
    private final boolean alwaysDestroyable;
    private final boolean replaceable;
    private final boolean flammable;
    private final boolean blockingPrecipitation;
    private final boolean blockingMotion;

    private MaterialType(float translucency, boolean neverBuildable, boolean liquid, boolean solid,
                         boolean superHot, boolean alwaysDestroyable, boolean replaceable, boolean flammable,
                         boolean blockingPrecipitation, boolean blockingMotion) {
        this.translucency = translucency;
        this.neverBuildable = neverBuildable;
        this.liquid = liquid;
        this.solid = solid;
        this.superHot = superHot;
        this.alwaysDestroyable = alwaysDestroyable;
        this.replaceable = replaceable;
        this.flammable = flammable;
        this.blockingPrecipitation = blockingPrecipitation;
        this.blockingMotion = blockingMotion;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nonnegative
    public float getTranslucency() {
        return translucency;
    }

    public boolean isNeverBuildable() {
        return neverBuildable;
    }

    public boolean isLiquid() {
        return liquid;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isSuperHot() {
        return superHot;
    }

    public boolean isAlwaysDestroyable() {
        return alwaysDestroyable;
    }

    public boolean isReplaceable() {
        return replaceable;
    }

    public boolean isFlammable() {
        return flammable;
    }

    public boolean isBlockingPrecipitation() {
        return blockingPrecipitation;
    }

    public boolean isBlockingMotion() {
        return blockingMotion;
    }

    public boolean isSolidBlocking() {
        return neverBuildable || blockingMotion;
    }

    public static class Builder {
        private float translucency;
        private boolean neverBuildable;
        private boolean liquid;
        private boolean solid;
        private boolean superHot;
        private boolean alwaysDestroyable;
        private boolean replaceable;
        private boolean flammable;
        private boolean blockingPrecipitation;
        private boolean blockingMotion;

        public Builder translucency(@Nonnegative float translucency) {
            Preconditions.checkArgument(translucency >= 0 && translucency <= 1, "Translucency must be between 0 and 1");
            this.translucency = translucency;
            return this;
        }

        public Builder neverBuildable() {
            this.neverBuildable = true;
            return this;
        }

        public Builder liquid() {
            this.liquid = true;
            this.solid = false;
            return this;
        }

        public Builder solid() {
            this.solid = true;
            this.liquid = false;
            return this;
        }

        public Builder superHot() {
            this.superHot = true;
            return this;
        }

        public Builder alwaysDestroyable() {
            this.alwaysDestroyable = true;
            return this;
        }

        public Builder replaceable() {
            this.replaceable = true;
            return this;
        }

        public Builder flammable() {
            this.flammable = true;
            return this;
        }

        public Builder blockingPrecipitation() {
            this.blockingPrecipitation = true;
            return this;
        }

        public Builder blockingMotion() {
            this.blockingMotion = true;
            return this;
        }

        public MaterialType build() {
            return new MaterialType(translucency, neverBuildable, liquid, solid, superHot, alwaysDestroyable,
                    replaceable, flammable, blockingPrecipitation, blockingMotion);
        }
    }
}
