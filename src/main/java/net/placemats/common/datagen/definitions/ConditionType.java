package net.placemats.common.datagen.definitions;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.placemats.PlaceMatMain;

@SuppressWarnings("unused")
@Getter
public enum ConditionType {
    IS_PLACEMENT_RANGE(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "is_placement_range")),
    HAS_TRAIT(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "has_trait")),
    IS_ROTTEN(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "is_rotten")),
    IS_PLACEMAT_BLOCK(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "is_placemat_block")),
    IS_ITEM(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "is_item")),
    NOT(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "not"));

    private final ResourceLocation id;

    ConditionType(ResourceLocation id) {
        this.id = id;
    }

    public static final ConditionType[] DEFAULT_HIERARCHY = new ConditionType[] {
        IS_PLACEMENT_RANGE,
        HAS_TRAIT,
        IS_ROTTEN,
        IS_PLACEMAT_BLOCK,
        IS_ITEM,
        NOT
    };

    @Getter private static ConditionType[] hierarchy = DEFAULT_HIERARCHY.clone();

    public static void setHierarchy(ConditionType[] newHierarchy) {
        hierarchy = newHierarchy != null ? newHierarchy.clone() : DEFAULT_HIERARCHY.clone();
    }

    public static void resetHierarchy() {
        hierarchy = DEFAULT_HIERARCHY.clone();
    }

    public int getPriority() {
        for (int i = 0; i < hierarchy.length; i++) {
            if (hierarchy[i] == this) {
                return i;
            }
        }
        return hierarchy.length;
    }

    public static ConditionType fromId(String id) {
        if (id == null) return null;
        if (id.startsWith("place_mats:")) {
            id = id.substring("place_mats:".length());
        }
        return switch (id.toLowerCase()) {
            case "is_placement_range", "isplacementrange", "placement_range", "range" -> IS_PLACEMENT_RANGE;
            case "has_trait", "hastrait", "trait" -> HAS_TRAIT;
            case "is_rotten", "isrotten", "rotten" -> IS_ROTTEN;
            case "is_placemat_block", "isplacematblock", "placemat_block", "block" -> IS_PLACEMAT_BLOCK;
            case "is_item", "isitem", "item" -> IS_ITEM;
            case "not", "not_condition", "notcondition", "inverse" -> NOT;
            default -> null;
        };
    }
}
