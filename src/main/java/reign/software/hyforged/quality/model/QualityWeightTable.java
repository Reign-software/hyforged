package reign.software.hyforged.quality.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Precomputed cumulative weight table for quality rolling.
 */
public final class QualityWeightTable {

    private final String[] qualities;
    private final int[] cumulative;
    private final int totalWeight;

    private QualityWeightTable(String[] qualities, int[] cumulative, int totalWeight) {
        this.qualities = qualities;
        this.cumulative = cumulative;
        this.totalWeight = totalWeight;
    }

    @Nullable
    public String roll(@Nonnull Random random) {
        if (totalWeight <= 0 || qualities.length == 0) {
            return null;
        }
        int value = random.nextInt(totalWeight);
        for (int i = 0; i < cumulative.length; i++) {
            if (value < cumulative[i]) {
                return qualities[i];
            }
        }
        return qualities[qualities.length - 1];
    }

    public int totalWeight() {
        return totalWeight;
    }

    @Nonnull
    public static QualityWeightTable fromWeights(@Nonnull Map<String, Integer> weights) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(weights.entrySet());
        entries.removeIf(entry -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() <= 0);
        entries.sort(Comparator.comparing(Map.Entry::getKey));

        String[] qualities = new String[entries.size()];
        int[] cumulative = new int[entries.size()];
        int running = 0;

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            running += entry.getValue();
            qualities[i] = entry.getKey();
            cumulative[i] = running;
        }

        return new QualityWeightTable(qualities, cumulative, running);
    }
}
