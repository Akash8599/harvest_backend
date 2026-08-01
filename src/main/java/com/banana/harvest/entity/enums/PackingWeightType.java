package com.banana.harvest.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PackingWeightType {
    KG_13,
    KG_13_5,
    KG_7,
    KG_16;

    @JsonCreator
    public static PackingWeightType fromValue(String value) {
        if (value == null) {
            return null;
        }
        switch (value.trim().toUpperCase()) {
            case "13":
            case "KG_13":
                return KG_13;
            case "13.5":
            case "13_5":
            case "KG_13_5":
                return KG_13_5;
            case "7":
            case "KG_7":
                return KG_7;
            case "16":
            case "KG_16":
                return KG_16;
            default:
                throw new IllegalArgumentException(
                        "Unknown packing weight type '" + value + "'. Allowed values are 13, 13.5, 7, 16 or KG_* equivalents.");
        }
    }
}
