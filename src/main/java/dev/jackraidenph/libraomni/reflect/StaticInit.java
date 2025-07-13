package dev.jackraidenph.libraomni.reflect;

public class StaticInit {

    public static void initContextExtensions(ModContextManager modContextManager) {
        modContextManager.addExtension(AutoRegisters::new);
        modContextManager.addExtension(AutoCreativeModeTabs::new);
    }

    //---

    private static final RuntimeTask REGISTER_OBJECTS = new RegisterObjectsTask();
    private static final RuntimeTask ADD_TO_CREATIVE_TABS = new AddToCreativeTabsTask();

    public static void initRuntimeTasks(RuntimeTaskProcessor processor) {
        processor.registerTask(REGISTER_OBJECTS);
        processor.registerTask(ADD_TO_CREATIVE_TABS);
    }

    //---
}
