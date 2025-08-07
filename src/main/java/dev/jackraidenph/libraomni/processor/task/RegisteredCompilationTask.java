package dev.jackraidenph.libraomni.processor.task;

enum RegisteredCompilationTask {

    //Must run first
    VALIDATE_ANNOTATIONS(new ValidateAnnotationsTask()),

    //Not a must, but cleaner to run last
    CREATE_METADATA(new CreateMetadataTask());

    public final CompilationTask INSTANCE;

    RegisteredCompilationTask(CompilationTask instance) {
        this.INSTANCE = instance;
    }

    static void init(CompilationTaskProcessor taskProcessor) {
        for (RegisteredCompilationTask tasks : RegisteredCompilationTask.values()) {
            taskProcessor.registerTask(tasks.INSTANCE);
        }
    }
}
