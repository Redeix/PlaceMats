package net.placemats.common.block;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;

/**
 * Api for handling lock types for place mats.
 */
public enum LockType implements StringRepresentable {
    /**
     * Unlocked: No preventions.
     */
    NONE("none", false, false, false, false, false, false),

    /**
     * Locked: prevents interaction, block destruction, explosions, burning, and viewing outlines.
     * Toggled with a key.
     */
    STRICT("strict", true, true, true, true, true, true),

    /**
     * Closed: prevents interaction and outlines but allows destruction.
     * Intended for closed shelves/containers.
     */
    CLOSED("closed", true, true, false, false, false, true),

    /**
     * Inspectable: prevents interaction but allows seeing outlines of placement ranges and placed items.
     */
    INSPECTABLE("inspectable", true, true, false, false, false, false);

    @Getter
    public final String name;
    @Getter
    public final boolean locked;
    @Getter
    public final boolean preventsInteraction;
    @Getter
    public final boolean preventsDestruction;
    @Getter
    public final boolean preventsExplosion;
    @Getter
    public final boolean preventsBurning;
    @Getter
    public final boolean preventsOutlines;

    LockType(String name, boolean locked, boolean preventsInteraction, boolean preventsDestruction,
             boolean preventsExplosion, boolean preventsBurning, boolean preventsOutlines) {
        this.name = name;
        this.locked = locked;
        this.preventsInteraction = preventsInteraction;
        this.preventsDestruction = preventsDestruction;
        this.preventsExplosion = preventsExplosion;
        this.preventsBurning = preventsBurning;
        this.preventsOutlines = preventsOutlines;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
