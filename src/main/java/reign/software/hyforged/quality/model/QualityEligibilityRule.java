package reign.software.hyforged.quality.model;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rule for determining which items are eligible for quality rolling.
 */
public record QualityEligibilityRule(
        @Nonnull String id,
        int priority,
        @Nonnull String description,
        @Nonnull String weightProfileId,
        @Nonnull AppliesTo appliesTo,
        @Nonnull Excludes excludes,
        @Nonnull SourceFilter sourceFilter,
        @Nonnull QualityModifierOverrides modifierOverrides
) {

    public QualityEligibilityRule {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(weightProfileId, "weightProfileId cannot be null");
        Objects.requireNonNull(appliesTo, "appliesTo cannot be null");
        Objects.requireNonNull(excludes, "excludes cannot be null");
        Objects.requireNonNull(sourceFilter, "sourceFilter cannot be null");
        Objects.requireNonNull(modifierOverrides, "modifierOverrides cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        if (weightProfileId.isBlank()) {
            throw new IllegalArgumentException("weightProfileId cannot be blank");
        }
    }

    public boolean matches(@Nonnull QualityRollContext context) {
        Objects.requireNonNull(context, "context cannot be null");

        Set<String> itemCategories = toSet(context.itemCategories());
        Set<String> itemTags = toSet(context.itemTags());
        Set<String> sourceTags = toSet(context.sourceTags());

        if (!appliesTo.matches(itemCategories, itemTags, context.itemId())) {
            return false;
        }
        if (excludes.matches(itemTags, context.itemId())) {
            return false;
        }
        return sourceFilter.matches(sourceTags);
    }

    public record AppliesTo(
            @Nonnull List<String> categories,
            @Nonnull List<String> tags,
            @Nonnull List<String> itemIds
    ) {
        public AppliesTo {
            Objects.requireNonNull(categories, "categories cannot be null");
            Objects.requireNonNull(tags, "tags cannot be null");
            Objects.requireNonNull(itemIds, "itemIds cannot be null");
            categories = List.copyOf(categories);
            tags = List.copyOf(tags);
            itemIds = List.copyOf(itemIds);
        }

        public boolean matches(@Nonnull Set<String> categoriesSet, @Nonnull Set<String> tagsSet, @Nonnull String itemId) {
            boolean categoriesMatch = categories.isEmpty() || containsAny(categoriesSet, categories);
            boolean tagsMatch = tags.isEmpty() || containsAny(tagsSet, tags);
            boolean itemMatch = itemIds.isEmpty() || matchesAnyPattern(itemId, itemIds);
            return categoriesMatch && tagsMatch && itemMatch;
        }
    }

    public record Excludes(
            @Nonnull List<String> tags,
            @Nonnull List<String> itemIds
    ) {
        public Excludes {
            Objects.requireNonNull(tags, "tags cannot be null");
            Objects.requireNonNull(itemIds, "itemIds cannot be null");
            tags = List.copyOf(tags);
            itemIds = List.copyOf(itemIds);
        }

        public boolean matches(@Nonnull Set<String> tagsSet, @Nonnull String itemId) {
            if (!tags.isEmpty() && containsAny(tagsSet, tags)) {
                return true;
            }
            return !itemIds.isEmpty() && matchesAnyPattern(itemId, itemIds);
        }
    }

    public record SourceFilter(
            @Nonnull List<String> sourceTags,
            @Nonnull List<String> excludeSourceTags
    ) {
        public SourceFilter {
            Objects.requireNonNull(sourceTags, "sourceTags cannot be null");
            Objects.requireNonNull(excludeSourceTags, "excludeSourceTags cannot be null");
            sourceTags = List.copyOf(sourceTags);
            excludeSourceTags = List.copyOf(excludeSourceTags);
        }

        public boolean matches(@Nonnull Set<String> sourceTagSet) {
            if (!sourceTags.isEmpty() && !containsAny(sourceTagSet, sourceTags)) {
                return false;
            }
            return excludeSourceTags.isEmpty() || !containsAny(sourceTagSet, excludeSourceTags);
        }
    }

    private static boolean containsAny(@Nonnull Set<String> haystack, @Nonnull List<String> needles) {
        for (String needle : needles) {
            if (needle != null && haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnyPattern(@Nonnull String itemId, @Nonnull List<String> patterns) {
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            if (pattern.equals("*")) {
                return true;
            }
            if (pattern.contains("*")) {
                String regex = Pattern.quote(pattern).replace("*", "\\E.*\\Q");
                if (itemId.matches(regex)) {
                    return true;
                }
            } else if (pattern.equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private static Set<String> toSet(@Nonnull String[] values) {
        Set<String> result = new HashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            result.add(value);
        }
        return result;
    }
}
