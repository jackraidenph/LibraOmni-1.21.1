package dev.jackraidenph.libraomni.runtime;

import dev.jackraidenph.libraomni.runtime.extension.AutoCreativeModeTabs;
import dev.jackraidenph.libraomni.runtime.extension.AutoRegisters;
import dev.jackraidenph.libraomni.runtime.extension.PropertiesPool;
import dev.jackraidenph.libraomni.runtime.task.*;

public class StaticInit {

    public static void initContextExtensions(ModContextManager modContextManager) {
        modContextManager.registerExtensionFactory(AutoRegisters::new);
        modContextManager.registerExtensionFactory(AutoCreativeModeTabs::new);
        modContextManager.registerExtensionFactory(PropertiesPool::new);
    }

    //---

    private static final RuntimeTask REGISTER_OBJECTS = new RegisterObjectsTask();
    private static final RuntimeTask ADD_TO_CREATIVE_TABS = new AddToCreativeTabsTask();
    private static final RuntimeTask GENERATE_BLOCK_ITEMS = new GenerateBlockItemsTask();
    private static final RuntimeTask GATHER_PROPERTIES = new GatherPropertiesTask();

    public static void initRuntimeTasks(RuntimeTaskProcessor processor) {
        processor.registerTask(REGISTER_OBJECTS);
        processor.registerTask(ADD_TO_CREATIVE_TABS);
        processor.registerTask(GENERATE_BLOCK_ITEMS);
        processor.registerTask(GATHER_PROPERTIES);
    }

    //---
}
