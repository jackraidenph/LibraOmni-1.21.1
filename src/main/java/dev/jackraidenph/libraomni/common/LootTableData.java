package dev.jackraidenph.libraomni.common;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;

public final class LootTableData {
    @SerializedName("type")
    private String type = "generic";
    @SerializedName("functions")
    private ArbitraryOptions functions;
    @SerializedName("pools")
    private List<PoolData> pools;
    @SerializedName("random_sequence")
    private String randomSequence;

    public LootTableData(String randomSequence) {
        this.randomSequence = randomSequence;
    }

    public LootTableData(String type, String randomSequence) {
        this.type = type;
        this.randomSequence = randomSequence;
    }

    public ArbitraryOptions getFunctions() {
        if (functions == null) {
            functions = new ArbitraryOptions("function");
        }
        return functions;
    }

    public LootTableData addPool(PoolData data) {
        if (pools == null) {
            pools = new ArrayList<>();
        }
        pools.add(data);
        return this;
    }

    public static class PoolData {
        @SerializedName("rolls")
        private Object rolls = 1.0f;
        @SerializedName("bonus_rolls")
        private Object bonusRolls = 0.0f;
        @SerializedName("conditions")
        private ArbitraryOptions conditions;
        @SerializedName("entries")
        private List<EntryData> entries;

        /**
         * Default rolls value - 1
         * </p>
         * Default bonus_rolls value - 0.0f
         */
        public PoolData() {
        }

        public PoolData(Object rolls, Object bonusRolls) {
            this.rolls = rolls;
            this.bonusRolls = bonusRolls;
        }

        public ArbitraryOptions conditions() {
            if (conditions == null) {
                conditions = new ArbitraryOptions("condition");
            }
            return conditions;
        }

        public PoolData addEntry(EntryData entryData) {
            if (entries == null) {
                entries = new ArrayList<>();
            }
            entries.add(entryData);
            return this;
        }
    }

    public static abstract class EntryData {
        @SerializedName("type")
        private String type;
        @SerializedName("conditions")
        private ArbitraryOptions conditions;

        public EntryData(String type) {
            this.type = type;
        }

        public ArbitraryOptions conditions() {
            if (conditions == null) {
                conditions = new ArbitraryOptions("condition");
            }
            return conditions;
        }
    }

    public static class SingletonEntry extends EntryData {
        @SerializedName("functions")
        private ArbitraryOptions functions;
        @SerializedName("weight")
        private Object weight;
        @SerializedName("quality")
        private Object quality;

        @Nullable
        @SerializedName("name")
        private final String name;

        private SingletonEntry(String type, int weight, int quality, String name) {
            super(type);
            this.weight = weight;
            this.quality = quality;
            this.name = name;
        }

        private SingletonEntry(String type, String name) {
            super(type);
            this.name = name;
        }

        public ArbitraryOptions functions() {
            if (functions == null) {
                functions = new ArbitraryOptions("function");
            }
            return functions;
        }

        public CompositeEntry wrap(String type) {
            CompositeEntry compositeEntry = new CompositeEntry(type);
            compositeEntry.addChild(this);
            return compositeEntry;
        }

        public static SingletonEntry itemEntry(int weight, int quality, String item) {
            return new SingletonEntry("minecraft:item", weight, quality, item);
        }

        public static SingletonEntry itemEntry(String item) {
            return new SingletonEntry("minecraft:item", item);
        }

        public static SingletonEntry lootTableEntry(int weight, int quality, String lootTable) {
            return new SingletonEntry("minecraft:loot_table", weight, quality, lootTable);
        }

        public static SingletonEntry lootTableEntry(String lootTable) {
            return new SingletonEntry("minecraft:loot_table", lootTable);
        }

        public static SingletonEntry dynamicEntry(int weight, int quality, String dynamic) {
            return new SingletonEntry("minecraft:dynamic", weight, quality, dynamic);
        }

        public static SingletonEntry dynamicEntry(String dynamic) {
            return new SingletonEntry("minecraft:dynamic", dynamic);
        }
    }

    public static class TagEntry extends SingletonEntry {
        @SerializedName("expand")
        private boolean expand;

        public TagEntry(int weight, int quality, String name, boolean expand) {
            super("minecraft:tag", weight, quality, name);
            this.expand = expand;
        }

        public TagEntry(String name, boolean expand) {
            super("minecraft:tag", 1, 0, name);
            this.expand = expand;
        }
    }

    public static class CompositeEntry extends EntryData {
        @SerializedName("children")
        private final List<EntryData> children = new ArrayList<>();

        public CompositeEntry(String type, EntryData... children) {
            super(type);
            this.children.addAll(List.of(children));
        }

        public void addChild(EntryData entryData) {
            this.children.add(entryData);
        }
    }

    public static class ArbitraryOptions {
        private final List<Map<?, ?>> options = new ArrayList<>();
        private final String type;

        public ArbitraryOptions(String type) {
            this.type = type;
        }

        public ArbitraryOptions add(String name, Map<?, ?> params) {
            var opts = new HashMap<>();
            opts.put(type, name);
            if (params != null) {
                opts.putAll(params);
            }
            options.add(opts);
            return this;
        }

        public ArbitraryOptions add(String name) {
            return add(name, null);
        }

        public static class ArbitraryOptionsSerializer implements JsonSerializer<ArbitraryOptions> {
            public JsonElement serialize(ArbitraryOptions src, Type member, JsonSerializationContext context) {
                return context.serialize(src.options);
            }
        }
    }
}
