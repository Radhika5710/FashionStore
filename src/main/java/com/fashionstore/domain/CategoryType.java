package com.fashionstore.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum CategoryType {
    MEN(1, "men", "Men", "mens", "menswear", "menwear"),
    WOMEN(2, "women", "Women", "womens", "womenswear", "ladies"),
    FOOTWEAR(3, "footwear", "Footwear", "shoes", "shoe", "sneakers", "boots"),
    ACCESSORIES(4, "accessories", "Accessories", "accessory", "bags", "belts", "jewelry");

    private final int seedId;
    private final String slug;
    private final String displayName;
    private final String[] aliases;

    CategoryType(int seedId, String slug, String displayName, String... aliases) {
        this.seedId = seedId;
        this.slug = slug;
        this.displayName = displayName;
        this.aliases = aliases;
    }

    public int getSeedId() {
        return seedId;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean matches(String value) {
        String key = normalize(value);
        return normalize(slug).equals(key)
                || normalize(displayName).equals(key)
                || Arrays.stream(aliases).anyMatch(alias -> normalize(alias).equals(key));
    }

    public static Optional<CategoryType> fromId(Integer categoryId) {
        if (categoryId == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.seedId == categoryId)
                .findFirst();
    }

    public static Optional<CategoryType> fromName(String value) {
        return Arrays.stream(values())
                .filter(type -> type.matches(value))
                .findFirst();
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("’", "")
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", "");
    }
}
