package dev.jackraidenph.libraomni.compilation.task;

enum CompilationTasksInit {

    //Must run first
    VALIDATE_ANNOTATIONS(new ValidateAnnotationsTask()),

    GENERATE_BLOCK_MODELS(new GenerateBlockModelsTask()),
    GENERATE_ITEM_MODELS(new GenerateItemModelsTask()),
    GENERATE_BLOCK_STATES(new GenerateBlockStatesTask()),

    //Not a must, but cleaner to run last
    CREATE_METADATA(new CreateMetadataTask());

    public final CompilationTask INSTANCE;

    CompilationTasksInit(CompilationTask instance) {
        this.INSTANCE = instance;
    }

    static void init(CompilationTaskProcessor taskProcessor) {
        for (CompilationTasksInit tasks : CompilationTasksInit.values()) {
            taskProcessor.registerTask(tasks.INSTANCE);
        }
    }
}
