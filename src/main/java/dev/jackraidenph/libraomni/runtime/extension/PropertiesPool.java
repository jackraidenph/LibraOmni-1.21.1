package dev.jackraidenph.libraomni.runtime.extension;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.runtime.ModContext;
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
}
