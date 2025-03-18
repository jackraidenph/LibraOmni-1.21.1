package dev.jackraidenph.libraomni.annotation.compile.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import java.util.Set;

class EmptySetToNullFactory implements TypeAdapterFactory {
    // Used as workaround for https://github.com/google/gson/issues/1028
    private final boolean wasCreatedByJsonAdapter;

    public static final EmptySetToNullFactory INSTANCE = new EmptySetToNullFactory(false);

    private EmptySetToNullFactory(boolean wasCreatedByJsonAdapter) {
        this.wasCreatedByJsonAdapter = wasCreatedByJsonAdapter;
    }

    /**
     * @deprecated
     *      Only intended to be called by Gson when used for {@link JsonAdapter}.
     */
    @Deprecated
    private EmptySetToNullFactory() {
        this(true);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<?> rawType = type.getRawType();
        if (!Set.class.isAssignableFrom(rawType)) {
            return null;
        }

        // Safe; the check above made sure type is List
        @SuppressWarnings("unchecked")
        TypeAdapter<Set<Object>> delegate = (TypeAdapter<Set<Object>>) (wasCreatedByJsonAdapter ?
                gson.getAdapter(type) : gson.getDelegateAdapter(this, type));

        @SuppressWarnings("unchecked")
        TypeAdapter<T> adapter = (TypeAdapter<T>) new TypeAdapter<Set<Object>>() {
            @Override
            public Set<Object> read(JsonReader in) throws IOException {
                return delegate.read(in);
            }

            @Override
            public void write(JsonWriter out, Set<Object> value) throws IOException {
                if (value == null || value.isEmpty()) {
                    // Call the delegate instead of directly writing null in case the delegate has
                    // special null handling
                    delegate.write(out, null);
                } else {
                    delegate.write(out, value);
                }
            }
        };
        return adapter;
    }
}