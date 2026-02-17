package reign.software.hyforged.progression.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ArraySchema;
import com.hypixel.hytale.codec.schema.config.IntegerSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import org.bson.BsonArray;
import org.bson.BsonInt64;
import org.bson.BsonType;
import org.bson.BsonValue;

import javax.annotation.Nonnull;

/**
 * A tolerant long[] codec that handles BSON INT32 → INT64 promotion.
 * <p>
 * BSON may store small long values (e.g. 0L) as INT32 instead of INT64.
 * The standard {@code Codec.LONG_ARRAY} fails on these with
 * "Value expected to be of type INT64 is of unexpected type INT32".
 * This codec transparently promotes INT32 values to long.
 */
public final class TolerantLongArrayCodec implements Codec<long[]> {

    public static final TolerantLongArrayCodec INSTANCE = new TolerantLongArrayCodec();
    private static final long[] EMPTY = new long[0];

    private TolerantLongArrayCodec() {}

    @Override
    public long[] decode(@Nonnull BsonValue bsonValue, ExtraInfo extraInfo) {
        BsonArray array = bsonValue.asArray();
        if (array.isEmpty()) {
            return EMPTY;
        }
        long[] result = new long[array.size()];
        for (int i = 0; i < result.length; i++) {
            BsonValue element = array.get(i);
            if (element.getBsonType() == BsonType.INT64) {
                result[i] = element.asInt64().getValue();
            } else if (element.getBsonType() == BsonType.INT32) {
                result[i] = element.asInt32().getValue();
            } else {
                // Fallback: try asNumber
                result[i] = element.asNumber().longValue();
            }
        }
        return result;
    }

    @Nonnull
    @Override
    public BsonValue encode(@Nonnull long[] longs, ExtraInfo extraInfo) {
        BsonArray array = new BsonArray();
        for (long value : longs) {
            array.add(new BsonInt64(value));
        }
        return array;
    }

    @Nonnull
    @Override
    public Schema toSchema(@Nonnull SchemaContext context) {
        ArraySchema s = new ArraySchema();
        s.setItem(new IntegerSchema());
        return s;
    }
}
