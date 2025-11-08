package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.GeneratesDrops;
import dev.jackraidenph.libraomni.common.LootTableData;
import dev.jackraidenph.libraomni.common.LootTableData.CompositeEntry;
import dev.jackraidenph.libraomni.common.LootTableData.EntryData;
import dev.jackraidenph.libraomni.common.LootTableData.PoolData;
import dev.jackraidenph.libraomni.common.LootTableData.SingletonEntry;
import dev.jackraidenph.libraomni.common.StringUtilities;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import dev.jackraidenph.libraomni.compilation.util.ResourceManager;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.*;

public class GenerateDropsTask extends SequentialCompilationTask {
    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        GeneratesDrops generatesDrops = element.getAnnotation(GeneratesDrops.class);
        if (generatesDrops != null) {
            processDropsItself(modId, elementId, generatesDrops, processingContext);
        }
    }

    private void processDropsItself(String modId, String elementId, GeneratesDrops annotation, ProcessingContext processingContext) {
        ResourceManager resourceManager = processingContext.resourceManager();

        String dropsId = annotation.value();
        boolean mustSurviveExplosion = annotation.mustSurviveExplosion();
        boolean limitToMinMax = annotation.useLimits();
        int min = annotation.min();
        int max = annotation.max();
        int fortuneBonus = annotation.fortuneBonus();
        String silkTouchDrop = annotation.silkTouchAlternative();

        String drops = dropsId.isBlank() ? StringUtilities.makeNamespacedId(modId, elementId) : dropsId;

        LootTableData lootTableData = new LootTableData("minecraft:block", StringUtilities.makeNamespacedId(modId, "blocks/" + elementId));
        PoolData poolData = new PoolData();
        EntryData entryData;
        lootTableData.addPool(poolData);

        SingletonEntry regularDrops = SingletonEntry.itemEntry(drops);
        if (min == max && min != 1) {
            regularDrops.functions().add(
                    "minecraft:set_count",
                    Map.of(
                            "add", false,
                            "count", min
                    ));
        } else if (min != max) {
            regularDrops.functions().add(
                    "minecraft:set_count",
                    Map.of(
                            "add", false,
                            "count", Map.of(
                                    "type", "minecraft:uniform",
                                    "max", max,
                                    "min", min
                            ))
            );
        }
        if (mustSurviveExplosion) {
            if (min == max && min == 1) {
                poolData.conditions().add("minecraft:survives_explosion");
            } else {
                regularDrops.functions().add("minecraft:explosion_decay");
            }
        }
        if (limitToMinMax) {
            int maxLimit = annotation.maxLimit() < 0 ? max : annotation.maxLimit();
            int minLimit = annotation.minLimit() < 0 ? min : annotation.minLimit();
            regularDrops.functions().add(
                    "minecraft:limit_count",
                    Map.of(
                            "limit", Map.of(
                                    "max", maxLimit,
                                    "min", minLimit
                            ))
            );
        }
        if (fortuneBonus > 0) {
            regularDrops.functions().add(
                    "minecraft:apply_bonus",
                    Map.of(
                            "enchantment", "minecraft:fortune",
                            "formula", "minecraft:uniform_bonus_count",
                            "parameters", Map.of(
                                    "bonusMultiplier", fortuneBonus
                            ))
            );
        }

        if (silkTouchDrop.isBlank()) {
            entryData = regularDrops;
        } else {
            SingletonEntry silkTouchDrops = SingletonEntry.itemEntry(silkTouchDrop);
            silkTouchDrops.conditions().add(
                    "minecraft:match_tool",
                    Map.of(
                            "predicate", Map.of(
                                    "predicates", Map.of(
                                            "minecraft:enchantments", List.of(
                                                    Map.of(
                                                            "enchantments", "minecraft:silk_touch",
                                                            "levels", Map.of(
                                                                    "min", 1
                                                            )
                                                    )
                                            )
                                    )
                            )
                    ));

            entryData = new CompositeEntry(
                    "minecraft:alternatives",
                    silkTouchDrops,
                    regularDrops
            );
        }
        poolData.addEntry(entryData);

        resourceManager.save(
                ResourceIdentifier.data(modId, "loot_table/blocks", elementId),
                lootTableData
        );
    }

    @Override
    public boolean requireIdAnnotation() {
        return true;
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(GeneratesDrops.class);
    }
}
