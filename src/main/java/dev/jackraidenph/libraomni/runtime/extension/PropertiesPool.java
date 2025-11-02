package dev.jackraidenph.libraomni.runtime.extension;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.runtime.BlockPropertiesByName;
import dev.jackraidenph.libraomni.annotation.runtime.BlockPropertiesCopy;
import dev.jackraidenph.libraomni.annotation.runtime.ItemPropertiesByName;
import dev.jackraidenph.libraomni.annotation.runtime.ItemPropertiesCopy;
import dev.jackraidenph.libraomni.data.proxy.ProxyAnnotatedElement;
import dev.jackraidenph.libraomni.runtime.ModContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.HashMap;
import java.util.Map;

public class PropertiesPool extends AbstractModContextExtension {

    private final Map<String, BlockBehaviour.Properties> blockProperties = new HashMap<>();
    private final Map<String, Item.Properties> itemProperties = new HashMap<>();

    public PropertiesPool(ModContext modContext) {
        super(modContext);
    }

    public void addItemProperties(String id, Item.Properties props) {
        if (id != null && !id.isBlank()) {
            LibraOmni.LOGGER.info("Registered item properties [{}]", id);
            this.itemProperties.put(id, props);
        }
    }

    public void addBlockProperties(String id, BlockBehaviour.Properties props) {
        if (id != null && !id.isBlank()) {
            LibraOmni.LOGGER.info("Registered block properties [{}]", id);
            this.blockProperties.put(id, props);
        }
    }

    public Item.Properties getItemProperties(String id) {
        Item.Properties properties = itemProperties.get(id);
        if (properties == null) {
            LibraOmni.LOGGER.warn("Failed to get Item.Properties with id [{}], returning empty", id);
            return new Properties();
        }
        return properties;
    }

    public BlockBehaviour.Properties getBlockProperties(String id) {
        BlockBehaviour.Properties properties = blockProperties.get(id);
        if (properties == null) {
            LibraOmni.LOGGER.warn("Failed to get BlockBehaviour.Properties with id [{}], returning empty", id);
            return BlockBehaviour.Properties.of();
        }
        return properties;
    }

    public boolean hasItemProperties(String id) {
        return itemProperties.containsKey(id);
    }

    public boolean hasBlockProperties(String id) {
        return blockProperties.containsKey(id);
    }

    public boolean hasProperties(String id) {
        return hasItemProperties(id) || hasBlockProperties(id);
    }

    public static class Util {
        public static BlockBehaviour.Properties getBlockPropertiesForElement(ProxyAnnotatedElement e, ModContext context) {
            BlockPropertiesByName propertiesByName = e.getAnnotation(BlockPropertiesByName.class);
            BlockPropertiesCopy copyFrom = e.getAnnotation(BlockPropertiesCopy.class);
            if (propertiesByName != null) {
                if (copyFrom != null) {
                    throw new IllegalStateException("@BlockPropertiesByName clashes with @BlockPropertiesCopy on element [%s]".formatted(e));
                }
                return getBlockProperties(propertiesByName.value(), context);
            } else if (copyFrom != null) {
                return copyBlockProperties(
                        copyFrom.namespace().isBlank() ? "minecraft" : copyFrom.namespace(),
                        copyFrom.value()
                );
            }

            LibraOmni.LOGGER.warn("Failed to get BlockBehaviour.Properties for element [{}], returning defaults", e);
            return BlockBehaviour.Properties.of();
        }

        public static Item.Properties getItemPropertiesForElement(ProxyAnnotatedElement e, ModContext context) {
            ItemPropertiesByName propertiesByName = e.getAnnotation(ItemPropertiesByName.class);
            ItemPropertiesCopy copyFrom = e.getAnnotation(ItemPropertiesCopy.class);
            if (propertiesByName != null) {
                if (copyFrom != null) {
                    throw new IllegalStateException("@ItemPropertiesByName clashes with @ItemPropertiesCopy on element [%s]".formatted(e));
                }
                return getItemProperties(propertiesByName.value(), context);
            } else if (copyFrom != null) {
                return copyItemProperties(
                        copyFrom.namespace().isBlank() ? "minecraft" : copyFrom.namespace(),
                        copyFrom.value()
                );
            }

            LibraOmni.LOGGER.warn("Failed to get Item.Properties for element [{}}], returning defaults", e);
            return new Properties();
        }

        public static BlockBehaviour.Properties getBlockProperties(String id, ModContext modContext) {
            return modContext.getExtension(PropertiesPool.class).getBlockProperties(id);
        }

        public static BlockBehaviour.Properties copyBlockProperties(String nameSpace, String path) {
            return BlockBehaviour.Properties.ofFullCopy(BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(nameSpace, path)));
        }

        public static Item.Properties getItemProperties(String id, ModContext modContext) {
            return modContext.getExtension(PropertiesPool.class).getItemProperties(id);
        }

        public static Item.Properties copyItemProperties(String nameSpace, String path) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(nameSpace, path));
            Item.Properties properties = new Properties();
            properties.craftingRemainingItem = item.craftingRemainingItem;
            properties.requiredFeatures = item.requiredFeatures();
            properties.canRepair = item.canRepair;
            for (TypedDataComponent<?> component : item.components()) {
                properties.component(homogenizeType(component).getKey(), homogenizeType(component).getValue());
            }
            return properties;
        }

        private static <T> Map.Entry<DataComponentType<T>, T> homogenizeType(TypedDataComponent<?> component) {
            //Used solely in 1 occasion when type safety is guaranteed
            //noinspection unchecked
            return Map.entry((DataComponentType<T>) component.type(), (T) component.value());
        }
    }
}
