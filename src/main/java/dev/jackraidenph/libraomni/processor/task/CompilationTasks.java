package dev.jackraidenph.libraomni.processor.task;

class CompilationTasks {

    private static final CompilationTask CREATE_METADATA = new CreateMetadataTask();
    private static final CompilationTask VALIDATE_ANNOTATIONS = new ValidateAnnotationsTask();

    static void init(CompilationTaskProcessor taskProcessor) {
        taskProcessor.registerTask(CREATE_METADATA);
        taskProcessor.registerTask(VALIDATE_ANNOTATIONS);
    }
}
