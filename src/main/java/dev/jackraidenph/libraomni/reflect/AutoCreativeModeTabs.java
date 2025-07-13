package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.LibraOmni;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.*;

public class AutoCreativeModeTabs extends AbstractModContextExtension implements LifecycleSetup {
    private final Map<ResourceLocation, List<ItemLike>> itemsInTab = new HashMap<>();

    public AutoCreativeModeTabs(ModContext modContext) {
        super(modContext);
    }

    private static ResourceLocation fromId(String modId, String id) {
        ResourceLocation resourceLocation = Optional
                .ofNullable(ResourceLocation.tryBuild(modId, id))
                .orElseThrow(IllegalArgumentException::new);

        if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(resourceLocation)) {
            throw new IllegalArgumentException("Tab [%s] does not exist".formatted(resourceLocation));
        }

        return resourceLocation;
    }

    public void add(String modId, String tabName, ItemLike item) {
        this.itemsInTab.computeIfAbsent(fromId(modId, tabName), k -> new ArrayList<>()).add(item);
    }

    public List<ItemLike> getItems(ResourceLocation tabResourceLocation) {
        List<ItemLike> added = itemsInTab.get(tabResourceLocation);
        if (added == null) {
            return List.of();
        }

        return Collections.unmodifiableList(added);
    }

    @Override
    public String toString() {
        return new StringJoiner(",", "{", "}").add(getContext().modId()).add(itemsInTab.toString()).toString();
    }

    @Override
    public void listenToBus(IEventBus eventBus) {
        eventBus.addListener(this::populateTabs);
    }

    @SubscribeEvent
    public void populateTabs(BuildCreativeModeTabContentsEvent event) {
        List<ItemLike> items = getItems(event.getTabKey().location());
        if (items != null && !items.isEmpty()) {
            for (ItemLike itemLike : items) {
                event.accept(itemLike);
                LibraOmni.LOGGER.info("Added [{}] to [{}]", itemLike, event.getTabKey());
            }
        }
    }
}