package dev.jackraidenph.libraomni.reflect;

import dev.jackraidenph.libraomni.reflect.extension.AutoRegisters;
import dev.jackraidenph.libraomni.reflect.task.AddToCreativeTabsTask;
import dev.jackraidenph.libraomni.reflect.extension.AutoCreativeModeTabs;
import dev.jackraidenph.libraomni.reflect.task.GenerateBlockItemsTask;
import dev.jackraidenph.libraomni.reflect.task.RegisterObjectsTask;
import dev.jackraidenph.libraomni.reflect.task.RuntimeTask;

public class StaticInit {

    public static void initContextExtensions(ModContextManager modContextManager) {
        modContextManager.registerExtensionFactory(AutoRegisters::new);
        modContextManager.registerExtensionFactory(AutoCreativeModeTabs::new);
    }

    //---

    private static final RuntimeTask REGISTER_OBJECTS = new RegisterObjectsTask();
    private static final RuntimeTask ADD_TO_CREATIVE_TABS = new AddToCreativeTabsTask();
    private static final RuntimeTask GENERATE_BLOCK_ITEMS = new GenerateBlockItemsTask();

    public static void initRuntimeTasks(RuntimeTaskProcessor processor) {
        processor.registerTask(REGISTER_OBJECTS);
        processor.registerTask(ADD_TO_CREATIVE_TABS);
        processor.registerTask(GENERATE_BLOCK_ITEMS);
    }

    //---
}
