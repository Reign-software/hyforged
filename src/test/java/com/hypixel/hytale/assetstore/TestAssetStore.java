package com.hypixel.hytale.assetstore;

import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.event.IAsyncEvent;
import com.hypixel.hytale.event.IBaseEvent;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.event.IEventDispatcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class TestAssetStore<K, T extends JsonAssetWithMap<K, M>, M extends AssetMap<K, T>> extends AssetStore<K, T, M> {

    private static final IEventBus EVENT_BUS = new NoOpEventBus();

    private TestAssetStore(Builder<K, T, M> builder) {
        super(builder);
    }

    @Nonnull
    @Override
    protected IEventBus getEventBus() {
        return EVENT_BUS;
    }

    @Override
    public void addFileMonitor(@Nonnull String packKey, Path assetsPath) {
        // No-op for tests.
    }

    @Override
    public void removeFileMonitor(Path path) {
        // No-op for tests.
    }

    @Override
    protected void handleRemoveOrUpdate(
        @Nullable Set<K> toBeRemoved,
        @Nullable Map<K, T> toBeUpdated,
        @Nonnull AssetUpdateQuery query
    ) {
        // No-op for tests.
    }

    @Nonnull
    public static <K, T extends JsonAssetWithMap<K, M>, M extends AssetMap<K, T>> Builder<K, T, M> builder(
        Class<K> kClass,
        Class<T> tClass,
        M assetMap
    ) {
        return new Builder<>(kClass, tClass, assetMap);
    }

    public static class Builder<K, T extends JsonAssetWithMap<K, M>, M extends AssetMap<K, T>>
        extends AssetStore.Builder<K, T, M, Builder<K, T, M>> {

        public Builder(Class<K> kClass, Class<T> tClass, M assetMap) {
            super(kClass, tClass, assetMap);
        }

        @Override
        public TestAssetStore<K, T, M> build() {
            return new TestAssetStore<>(this);
        }
    }

    private static final class NoOpEventBus implements IEventBus {
        private static final IEventDispatcher<IBaseEvent<?>, Object> NO_OP_DISPATCHER = new IEventDispatcher<>() {
            @Override
            public boolean hasListener() {
                return false;
            }

            @Override
            public Object dispatch(IBaseEvent<?> event) {
                return null;
            }
        };

        private static final IEventDispatcher<IAsyncEvent<?>, CompletableFuture<IAsyncEvent<?>>> NO_OP_ASYNC_DISPATCHER =
                new IEventDispatcher<>() {
            @Override
            public boolean hasListener() {
                return false;
            }

            @Override
            public CompletableFuture<IAsyncEvent<?>> dispatch(IAsyncEvent<?> event) {
                return CompletableFuture.completedFuture(null);
            }
        };

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public <KeyType, EventType extends IEvent<KeyType>> IEventDispatcher<EventType, EventType> dispatchFor(
                @Nonnull Class<? super EventType> eventClass,
                @Nullable KeyType key
        ) {
            return (IEventDispatcher<EventType, EventType>) (IEventDispatcher) NO_OP_DISPATCHER;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public <KeyType, EventType extends IAsyncEvent<KeyType>> IEventDispatcher<EventType, CompletableFuture<EventType>> dispatchForAsync(
                @Nonnull Class<? super EventType> eventClass,
                @Nullable KeyType key
        ) {
            return (IEventDispatcher<EventType, CompletableFuture<EventType>>) (IEventDispatcher) NO_OP_ASYNC_DISPATCHER;
        }

        @Override
        public <EventType extends IBaseEvent<Void>> EventRegistration<Void, EventType> register(
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <EventType extends IBaseEvent<Void>> EventRegistration<Void, EventType> register(
                @Nonnull EventPriority priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <EventType extends IBaseEvent<Void>> EventRegistration<Void, EventType> register(
                short priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> register(
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull KeyType key,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> register(
                @Nonnull EventPriority priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull KeyType key,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> register(
                short priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull KeyType key,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <EventType extends IAsyncEvent<Void>> EventRegistration<Void, EventType> registerAsync(
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <EventType extends IAsyncEvent<Void>> EventRegistration<Void, EventType> registerAsync(
                @Nonnull EventPriority priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <EventType extends IAsyncEvent<Void>> EventRegistration<Void, EventType> registerAsync(
                short priority,
                Class<? super EventType> eventClass,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IAsyncEvent<KeyType>> EventRegistration<KeyType, EventType> registerAsync(
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull KeyType key,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IAsyncEvent<KeyType>> EventRegistration<KeyType, EventType> registerAsync(
                @Nonnull EventPriority priority,
                Class<? super EventType> eventClass,
                @Nonnull KeyType key,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IAsyncEvent<KeyType>> EventRegistration<KeyType, EventType> registerAsync(
                short priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull KeyType key,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> registerGlobal(
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> registerGlobal(
                @Nonnull EventPriority priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> registerGlobal(
                short priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IAsyncEvent<KeyType>> EventRegistration<KeyType, EventType> registerAsyncGlobal(
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IAsyncEvent<KeyType>> EventRegistration<KeyType, EventType> registerAsyncGlobal(
                @Nonnull EventPriority priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IAsyncEvent<KeyType>> EventRegistration<KeyType, EventType> registerAsyncGlobal(
                short priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> registerUnhandled(
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> registerUnhandled(
                @Nonnull EventPriority priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> registerUnhandled(
                short priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Consumer<EventType> consumer
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IAsyncEvent<KeyType>> EventRegistration<KeyType, EventType> registerAsyncUnhandled(
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IAsyncEvent<KeyType>> EventRegistration<KeyType, EventType> registerAsyncUnhandled(
                @Nonnull EventPriority priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }

        @Override
        public <KeyType, EventType extends IAsyncEvent<KeyType>> EventRegistration<KeyType, EventType> registerAsyncUnhandled(
                short priority,
                @Nonnull Class<? super EventType> eventClass,
                @Nonnull Function<CompletableFuture<EventType>, CompletableFuture<EventType>> function
        ) {
            return null;
        }
    }
}
