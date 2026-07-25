package dev.jackraidenph.libraomni.compilation.util;

import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;
import dev.jackraidenph.libraomni.compilation.util.JsonMergeHelper.JsonMergeConflictPolicy;
import dev.jackraidenph.libraomni.exception.AlreadyInitializedException;

import javax.annotation.processing.ProcessingEnvironment;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public final class ResourceConfig {

    private static final Map<String, String> DEFAULT_CONFIG_OPTIONS = Map.of(
            "**.json", JsonMergeConflictPolicy.PREFER_NEW.name(),
            "regex:.*(?<!\\.json)$", JsonMergeConflictPolicy.OVERWRITE.name()
    );

    private static final Map<PathMatcher, JsonMergeConflictPolicy> DEFAULT_CONFIG = parseOptionsMapToConfig(DEFAULT_CONFIG_OPTIONS);
    private final Map<PathMatcher, JsonMergeConflictPolicy> CONFIG = new LinkedHashMap<>();

    private final Set<Path> RESOURCE_SET_DIRS = new HashSet<>();

    private boolean INITIALIZED = false;

    public Map<PathMatcher, JsonMergeConflictPolicy> getConfig() {
        return Collections.unmodifiableMap(CONFIG);
    }

    public Map<PathMatcher, JsonMergeConflictPolicy> getDefaultConfig() {
        return Collections.unmodifiableMap(DEFAULT_CONFIG);
    }

    public Set<Path> getResourceSetDirs() {
        return Collections.unmodifiableSet(RESOURCE_SET_DIRS);
    }

    public void init(ProcessingEnvironment processingEnv) {
        if (INITIALIZED) {
            throw new AlreadyInitializedException();
        }
        gatherResourceDirs(processingEnv);
        gatherConfig(processingEnv);
        INITIALIZED = true;
    }

    private void gatherResourceDirs(ProcessingEnvironment processingEnv) {
        String resourcesPaths = processingEnv.getOptions().get(AnnotationProcessorConstants.RESOURCE_LOCATIONS_OPTION);
        if (resourcesPaths != null) {
            for (String str : resourcesPaths.split(";")) {
                RESOURCE_SET_DIRS.add(Path.of(str));
            }
        }
    }

    private void gatherConfig(ProcessingEnvironment processingEnv) {
        String config = processingEnv.getOptions().get(AnnotationProcessorConstants.CONFIG_OPTION);
        Map<String, String> userConfig = parseConfigString(config);
        if (!userConfig.isEmpty()) {
            CONFIG.putAll(parseOptionsMapToConfig(userConfig));
            processingEnv.getMessager().printNote("Found resource config: %s, backup values: %s".formatted(userConfig, DEFAULT_CONFIG_OPTIONS));
        } else {
            processingEnv.getMessager().printNote("Resource config not found, assuming default values %s".formatted(DEFAULT_CONFIG_OPTIONS));
        }
    }

    private static PathMatcher globMatcher(String pattern) {
        try {
            return FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        } catch (PatternSyntaxException e) {
            throw new RuntimeException("Pattern [%s] is invalid".formatted(pattern), e);
        }
    }

    private static PathMatcher getMatcher(String pattern) {
        if (pattern.startsWith("regex:")) {
            return FileSystems.getDefault().getPathMatcher(pattern);
        } else {
            return globMatcher(pattern);
        }
    }

    private static Map<PathMatcher, JsonMergeConflictPolicy> parseOptionsMapToConfig(Map<String, String> map) {
        return map.entrySet().stream().collect(Collectors.toMap(e -> getMatcher(e.getKey()), e -> parsePolicy(e.getValue())));
    }

    private static JsonMergeConflictPolicy parsePolicy(String policy) {
        try {
            return JsonMergeConflictPolicy.valueOf(policy);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("[%s] policy is unknown, use any of [%s]".formatted(policy, Arrays.asList(JsonMergeConflictPolicy.values())), e);
        }
    }

    private static Map<String, String> parseConfigString(String str) {
        str = str.replaceAll("[{}\\s]", "");

        if (str.isBlank()) {
            return Map.of();
        }

        String[] pairs = str.split(",");
        Map<String, String> map = new HashMap<>();
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            map.put(kv[0], kv[1]);
        }
        return map;
    }

    public JsonMergeConflictPolicy getConflictPolicy(ResourceIdentifier resourceIdentifier) {
        JsonMergeConflictPolicy policy = getConflictPolicy(resourceIdentifier, this.getConfig());
        if (policy == null) {
            policy = getConflictPolicy(resourceIdentifier, this.getDefaultConfig());
        }
        return policy;
    }

    private JsonMergeConflictPolicy getConflictPolicy(ResourceIdentifier resourceIdentifier, Map<PathMatcher, JsonMergeConflictPolicy> conf) {
        Path path;
        String resourcePath = resourceIdentifier.getFilePath();
        try {
            path = Path.of(resourcePath);
        } catch (InvalidPathException e) {
            throw new RuntimeException("Not a path [%s]".formatted(resourcePath));
        }

        for (Entry<PathMatcher, JsonMergeConflictPolicy> e : conf.entrySet()) {
            PathMatcher globMatcher = e.getKey();
            if (globMatcher.matches(path)) {
                return e.getValue();
            }
        }

        return null;
    }

}
