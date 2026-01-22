package reign.software.hyforged.affix.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reign.software.hyforged.stats.modifier.HyforgedModifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HyforgedItemData} record.
 */
@DisplayName("HyforgedItemData")
class HyforgedItemDataTest {
    
    private static RolledAffix createAffix(String id, String type) {
        Map<String, RolledAffix.RolledStat> stats = new HashMap<>();
        stats.put("hyforged:health", new RolledAffix.RolledStat(50, HyforgedModifier.StackType.FLAT));
        return new RolledAffix(id, type, 1, stats);
    }
    
    @Nested
    @DisplayName("Construction")
    class Construction {
        
        @Test
        @DisplayName("should create with valid parameters")
        void shouldCreateWithValidParameters() {
            RolledAffix affix = createAffix("sturdy", "prefix");
            HyforgedItemData data = new HyforgedItemData(1, List.of(affix));
            
            assertEquals(1, data.schemaVersion());
            assertEquals(1, data.affixes().size());
            assertEquals(affix, data.affixes().get(0));
        }
        
        @Test
        @DisplayName("should reject null affixes list")
        void shouldRejectNullAffixes() {
            assertThrows(NullPointerException.class, () -> 
                new HyforgedItemData(1, null)
            );
        }
        
        @Test
        @DisplayName("should create immutable copy of affixes")
        void shouldCreateImmutableCopyOfAffixes() {
            List<RolledAffix> mutableList = new ArrayList<>();
            mutableList.add(createAffix("sturdy", "prefix"));
            
            HyforgedItemData data = new HyforgedItemData(1, mutableList);
            mutableList.add(createAffix("sharp", "suffix"));
            
            // Data should still have only 1 affix
            assertEquals(1, data.affixes().size());
        }
        
        @Test
        @DisplayName("affixes list should be immutable")
        void affixesListShouldBeImmutable() {
            HyforgedItemData data = HyforgedItemData.create(
                    List.of(createAffix("sturdy", "prefix"))
            );
            
            assertThrows(UnsupportedOperationException.class, () ->
                data.affixes().add(createAffix("sharp", "suffix"))
            );
        }
    }
    
    @Nested
    @DisplayName("Constants")
    class Constants {
        
        @Test
        @DisplayName("EMPTY should have no affixes")
        void emptyShouldHaveNoAffixes() {
            assertTrue(HyforgedItemData.EMPTY.affixes().isEmpty());
        }
        
        @Test
        @DisplayName("EMPTY should have current schema version")
        void emptyShouldHaveCurrentSchemaVersion() {
            assertEquals(
                    HyforgedItemData.CURRENT_SCHEMA_VERSION,
                    HyforgedItemData.EMPTY.schemaVersion()
            );
        }
        
        @Test
        @DisplayName("METADATA_KEY should be 'Hyforged'")
        void metadataKeyShouldBeHyforged() {
            assertEquals("Hyforged", HyforgedItemData.METADATA_KEY);
        }
    }
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {
        
        @Test
        @DisplayName("create() should use current schema version")
        void createShouldUseCurrentSchemaVersion() {
            HyforgedItemData data = HyforgedItemData.create(Collections.emptyList());
            assertEquals(HyforgedItemData.CURRENT_SCHEMA_VERSION, data.schemaVersion());
        }
        
        @Test
        @DisplayName("of(RolledAffix) should create single-affix data")
        void ofSingleShouldCreateSingleAffixData() {
            RolledAffix affix = createAffix("sturdy", "prefix");
            HyforgedItemData data = HyforgedItemData.of(affix);
            
            assertEquals(1, data.affixes().size());
            assertEquals(affix, data.affixes().get(0));
        }
        
        @Test
        @DisplayName("of(RolledAffix...) should create multi-affix data")
        void ofVarargsShouldCreateMultiAffixData() {
            RolledAffix a1 = createAffix("sturdy", "prefix");
            RolledAffix a2 = createAffix("sharp", "suffix");
            
            HyforgedItemData data = HyforgedItemData.of(a1, a2);
            
            assertEquals(2, data.affixes().size());
        }
    }
    
    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {
        
        @Test
        @DisplayName("hasAffixes() should return false for empty")
        void hasAffixesShouldReturnFalseForEmpty() {
            assertFalse(HyforgedItemData.EMPTY.hasAffixes());
        }
        
        @Test
        @DisplayName("hasAffixes() should return true when affixes exist")
        void hasAffixesShouldReturnTrueWhenAffixesExist() {
            HyforgedItemData data = HyforgedItemData.of(createAffix("sturdy", "prefix"));
            assertTrue(data.hasAffixes());
        }
        
        @Test
        @DisplayName("affixCount() should return correct count")
        void affixCountShouldReturnCorrectCount() {
            HyforgedItemData data = HyforgedItemData.of(
                    createAffix("sturdy", "prefix"),
                    createAffix("sharp", "suffix"),
                    createAffix("mighty", "prefix")
            );
            assertEquals(3, data.affixCount());
        }
        
        @Test
        @DisplayName("getAffixesByType() should filter by type")
        void getAffixesByTypeShouldFilterByType() {
            HyforgedItemData data = HyforgedItemData.of(
                    createAffix("sturdy", "prefix"),
                    createAffix("sharp", "suffix"),
                    createAffix("mighty", "prefix")
            );
            
            List<RolledAffix> prefixes = data.getAffixesByType("prefix");
            assertEquals(2, prefixes.size());
            assertTrue(prefixes.stream().allMatch(a -> a.type().equals("prefix")));
        }
        
        @Test
        @DisplayName("countByType() should count affixes of type")
        void countByTypeShouldCountAffixesOfType() {
            HyforgedItemData data = HyforgedItemData.of(
                    createAffix("sturdy", "prefix"),
                    createAffix("sharp", "suffix"),
                    createAffix("mighty", "prefix")
            );
            
            assertEquals(2, data.countByType("prefix"));
            assertEquals(1, data.countByType("suffix"));
            assertEquals(0, data.countByType("forged"));
        }
        
        @Test
        @DisplayName("hasAffix() should detect affix by ID")
        void hasAffixShouldDetectAffixById() {
            HyforgedItemData data = HyforgedItemData.of(
                    createAffix("sturdy", "prefix"),
                    createAffix("sharp", "suffix")
            );
            
            assertTrue(data.hasAffix("sturdy"));
            assertTrue(data.hasAffix("sharp"));
            assertFalse(data.hasAffix("mighty"));
        }
    }
    
    @Nested
    @DisplayName("Mutation Methods (return new instance)")
    class MutationMethods {
        
        @Test
        @DisplayName("withAffixes() should replace all affixes")
        void withAffixesShouldReplaceAllAffixes() {
            HyforgedItemData original = HyforgedItemData.of(createAffix("sturdy", "prefix"));
            List<RolledAffix> newAffixes = List.of(
                    createAffix("sharp", "suffix"),
                    createAffix("mighty", "prefix")
            );
            
            HyforgedItemData updated = original.withAffixes(newAffixes);
            
            assertEquals(2, updated.affixes().size());
            assertFalse(updated.hasAffix("sturdy"));
            assertTrue(updated.hasAffix("sharp"));
        }
        
        @Test
        @DisplayName("withAffixes() should preserve schema version")
        void withAffixesShouldPreserveSchemaVersion() {
            HyforgedItemData original = new HyforgedItemData(5, List.of());
            HyforgedItemData updated = original.withAffixes(List.of(createAffix("a", "prefix")));
            assertEquals(5, updated.schemaVersion());
        }
        
        @Test
        @DisplayName("withAffix() should append affix")
        void withAffixShouldAppendAffix() {
            HyforgedItemData original = HyforgedItemData.of(createAffix("sturdy", "prefix"));
            HyforgedItemData updated = original.withAffix(createAffix("sharp", "suffix"));
            
            assertEquals(2, updated.affixes().size());
            assertTrue(updated.hasAffix("sturdy"));
            assertTrue(updated.hasAffix("sharp"));
        }
        
        @Test
        @DisplayName("withoutType() should remove affixes of type")
        void withoutTypeShouldRemoveAffixesOfType() {
            HyforgedItemData data = HyforgedItemData.of(
                    createAffix("sturdy", "prefix"),
                    createAffix("sharp", "suffix"),
                    createAffix("mighty", "prefix")
            );
            
            HyforgedItemData result = data.withoutType("prefix");
            
            assertEquals(1, result.affixes().size());
            assertTrue(result.hasAffix("sharp"));
            assertFalse(result.hasAffix("sturdy"));
            assertFalse(result.hasAffix("mighty"));
        }
        
        @Test
        @DisplayName("withoutAffix() should remove specific affix")
        void withoutAffixShouldRemoveSpecificAffix() {
            HyforgedItemData data = HyforgedItemData.of(
                    createAffix("sturdy", "prefix"),
                    createAffix("sharp", "suffix")
            );
            
            HyforgedItemData result = data.withoutAffix("sturdy");
            
            assertEquals(1, result.affixes().size());
            assertFalse(result.hasAffix("sturdy"));
            assertTrue(result.hasAffix("sharp"));
        }
        
        @Test
        @DisplayName("original should not be modified")
        void originalShouldNotBeModified() {
            HyforgedItemData original = HyforgedItemData.of(createAffix("sturdy", "prefix"));
            original.withAffix(createAffix("sharp", "suffix"));
            
            // Original should still have only 1 affix
            assertEquals(1, original.affixes().size());
        }
    }
    
    @Nested
    @DisplayName("Schema Migration")
    class SchemaMigration {
        
        @Test
        @DisplayName("needsMigration() returns false for current version")
        void needsMigrationReturnsFalseForCurrentVersion() {
            HyforgedItemData data = new HyforgedItemData(
                    HyforgedItemData.CURRENT_SCHEMA_VERSION,
                    List.of()
            );
            assertFalse(data.needsMigration());
        }
        
        @Test
        @DisplayName("needsMigration() returns true for old version")
        void needsMigrationReturnsTrueForOldVersion() {
            HyforgedItemData data = new HyforgedItemData(0, List.of());
            assertTrue(data.needsMigration());
        }
    }
    
    @Nested
    @DisplayName("Asset Conversion")
    class AssetConversion {
        
        @Test
        @DisplayName("toAsset() should create HyforgedItemDataAsset")
        void toAssetShouldCreateAsset() {
            RolledAffix affix = createAffix("sturdy", "prefix");
            HyforgedItemData data = HyforgedItemData.of(affix);
            
            HyforgedItemData.HyforgedItemDataAsset asset = data.toAsset();
            
            assertEquals(HyforgedItemData.CURRENT_SCHEMA_VERSION, asset.schemaVersion);
            assertEquals(1, asset.affixes.length);
            assertEquals("sturdy", asset.affixes[0].affixId);
        }
        
        @Test
        @DisplayName("round-trip conversion should preserve data")
        void roundTripShouldPreserveData() {
            HyforgedItemData original = HyforgedItemData.of(
                    createAffix("sturdy", "prefix"),
                    createAffix("sharp", "suffix")
            );
            
            HyforgedItemData.HyforgedItemDataAsset asset = original.toAsset();
            HyforgedItemData restored = asset.toItemData();
            
            assertEquals(original.schemaVersion(), restored.schemaVersion());
            assertEquals(original.affixes().size(), restored.affixes().size());
            assertEquals(original.affixes().get(0).affixId(), restored.affixes().get(0).affixId());
            assertEquals(original.affixes().get(1).affixId(), restored.affixes().get(1).affixId());
        }
        
        @Test
        @DisplayName("empty affixes should round-trip correctly")
        void emptyAffixesShouldRoundTrip() {
            HyforgedItemData original = HyforgedItemData.EMPTY;
            
            HyforgedItemData.HyforgedItemDataAsset asset = original.toAsset();
            HyforgedItemData restored = asset.toItemData();
            
            assertFalse(restored.hasAffixes());
        }
    }
}
