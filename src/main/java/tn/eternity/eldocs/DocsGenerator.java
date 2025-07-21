package tn.eternity.eldocs;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.util.HashSet;
import java.util.Set;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * DocsGenerator is a tool for generating documentation from Java source code.
 * It parses Java files to extract class, method, and dependency information,
 * and uses this information to generate Markdown documentation and diagrams.
 */
public class DocsGenerator {
    /**
     * Configuration options for DocsGenerator.
     * <ul>
     *   <li>sourceDir: Source directory to scan for Java files.</li>
     *   <li>outputDir: Output directory for generated documentation.</li>
     *   <li>changedOnly: If true, only changed files are processed.</li>
     *   <li>apiUrl: URL for AI documentation generation (optional).</li>
     *   <li>apiKey: API key for AI documentation generation (optional).</li>
     *   <li>useAI: If true, uses AI for documentation enrichment.</li>
     * </ul>
     */
    public static class Config {
        public String sourceDir;
        public String outputDir;
        public boolean changedOnly;
        public String apiUrl;
        public String apiKey;
        public boolean useAI = true;
    }

    // --- Constants for class types ---
    private static final String TYPE_REPOSITORY = "Repository";
    private static final String TYPE_SERVICE = "Service";
    private static final String TYPE_REST_CONTROLLER = "Rest Controller";
    private static final String TYPE_CONTROLLER = "Controller";
    private static final String TYPE_GENERIC = "Generic";

    // --- Constants for prompt templates ---
    private static final String PROMPT_HEADER = "Generate documentation in Markdown format for the following Java class.\n";
    private static final String PROMPT_DEP_TREE = "\nDependencies (tree):\n";
    private static final String PROMPT_DRAW_TREE = "\nDraw a dependency tree showing how this class depends on these objects.\n";
    private static final String PROMPT_REPOSITORY = "\nThis is a Spring Data Repository. Document its purpose, main queries, and usage examples.";
    private static final String PROMPT_SERVICE = "\nThis is a Service class. Document its business logic, main responsibilities, and how it interacts with other layers.";
    private static final String PROMPT_REST_CONTROLLER = "\nThis is a REST Controller. Document its endpoints, request/response models, and example usages.";
    private static final String PROMPT_GENERIC = "\nProvide a general overview, usage, and responsibilities.";
    private static final String PROMPT_DIAGRAMS = "\nInclude diagrams and summary overview where appropriate.";

    // Track package contents for summary/diagram generation
    private static final Map<String, List<String>> packageClassMap = new HashMap<>();

    /**
     * Main entry point for CLI usage.
     * Parses arguments and triggers documentation generation.
     */
    public static void main(String[] args) throws Exception {
        DocsGenerator.Config config = new DocsGenerator.Config();
        // Simple CLI arg parsing
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--sourceDir":
                    config.sourceDir = args[++i];
                    break;
                case "--outputDir":
                    config.outputDir = args[++i];
                    break;
                case "--changedOnly":
                    config.changedOnly = Boolean.parseBoolean(args[++i]);
                    break;
                case "--apiUrl":
                    config.apiUrl = args[++i];
                    break;
                case "--apiKey":
                    config.apiKey = args[++i];
                    break;
            }
        }
        if (config.sourceDir == null) config.sourceDir = "src/main/java";
        if (config.outputDir == null) config.outputDir = "docs";
        if (config.apiUrl == null) config.apiUrl = "http://localhost:8080/v1/chat/completions";
        if (config.apiKey == null) config.apiKey = "";
        new DocsGenerator().generateDocs(config);
    }

    /**
     * Generates documentation for the given configuration.
     * Scans source files, generates Markdown docs, package summaries, and diagrams.
     * @param config DocsGenerator configuration
     * @throws IOException if file operations fail
     * @throws InterruptedException if interrupted
     */
    public void generateDocs(Config config) throws IOException, InterruptedException {
        File sourceDir = new File(config.sourceDir);
        File outputDir = new File(config.outputDir);
        List<File> filesToProcess = config.changedOnly ? getChangedFilesStatic(sourceDir) : getAllJavaFilesStatic(sourceDir);
        for (File file : filesToProcess) {
            processFileStatic(file.toPath(), outputDir.toPath(), config);
        }
        // After processing all files, generate package summaries
        for (Map.Entry<String, List<String>> entry : packageClassMap.entrySet()) {
            String packageName = entry.getKey();
            List<String> classNames = entry.getValue();
            Path packagePath = outputDir.toPath();
            if (!packageName.isEmpty()) {
                String[] packageParts = packageName.split("\\.");
                for (String part : packageParts) {
                    packagePath = packagePath.resolve(part);
                }
            }
            Files.createDirectories(packagePath);
            Path summaryPath = packagePath.resolve("package-summary.md");
            String summary = buildPackageSummaryWithSubpackages(packageName, outputDir.toPath());
            writeIfChangedStatic(summaryPath, summary);
        }
        // Generate global package diagram for the whole project
        Path globalDiagramPath = outputDir.toPath().resolve("project-package-diagram.md");
        String globalDiagram = buildGlobalPackageDiagram();
        writeIfChangedStatic(globalDiagramPath, globalDiagram);
    }

    private static String buildGlobalPackageDiagram() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Project Package Diagram\n\n");
        sb.append("```mermaid\n");
        sb.append("graph TD\n");
        Set<String> allPackages = packageClassMap.keySet();
        for (String pkg : allPackages) {
            String pkgNode = pkg.isEmpty() ? "default" : pkg.replace('.', '_');
            // Link to subpackages
            String prefix = pkg.isEmpty() ? "" : pkg + ".";
            for (String subpkg : allPackages) {
                if (!subpkg.equals(pkg) && subpkg.startsWith(prefix)) {
                    String sub = subpkg.substring(prefix.length());
                    if (!sub.isEmpty() && !sub.contains(".")) {
                        String subNode = subpkg.replace('.', '_');
                        sb.append("    ").append(pkgNode).append(" --> ").append(subNode).append("\n");
                    }
                }
            }
            // Link to classes/interfaces
            List<String> classNames = packageClassMap.getOrDefault(pkg, new ArrayList<>());
            for (String className : classNames) {
                sb.append("    ").append(pkgNode).append(" --> ").append(className).append("\n");
            }
        }
        sb.append("```");
        return sb.toString();
    }

    public static List<File> getChangedFilesStatic(File base) throws IOException {
        List<File> result = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--name-only", "HEAD~1", "HEAD");
        pb.directory(base.getParentFile());
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.endsWith(".java")) {
                File f = new File(base, line);
                if (f.exists()) result.add(f);
            }
        }
        return result;
    }

    public static List<File> getAllJavaFilesStatic(File base) throws IOException {
        List<File> result = new ArrayList<>();
        Files.walk(base.toPath())
            .filter(p -> p.toString().endsWith(".java"))
            .forEach(p -> result.add(p.toFile()));
        return result;
    }

    /**
     * Processes a single Java file and generates its documentation.
     * Places output in the correct package directory and tracks package contents.
     * @param path Path to the Java file
     * @param outputRoot Root output directory
     * @param configObj Configuration object
     */
    public static void processFileStatic(Path path, Path outputRoot, Object configObj) {
        try {
            JavaParser javaParser = new JavaParser();
            ParseResult<CompilationUnit> result = javaParser.parse(path);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                System.err.println("❌ Failed to parse " + path + ":\n");
                result.getProblems().forEach(p -> System.err.println("  → " + p.getMessage()));
                return;
            }
            CompilationUnit cu = result.getResult().get();
            Optional<ClassOrInterfaceDeclaration> clazzOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
            if (clazzOpt.isEmpty()) {
                System.out.println("⚠️ No class/interface found in: " + path);
                return;
            }
            ClassOrInterfaceDeclaration clazz = clazzOpt.get();
            String name = clazz.getNameAsString();
            String doc = clazz.getJavadoc().map(Javadoc::toText).orElse("No description.");
            List<String> methods = extractMethodDocs(clazz);
            String classType = detectClassType(clazz);
            Set<String> dependencies = extractDependencies(clazz);
            boolean useAI = true;
            if (configObj instanceof ElDocsPluginExtension) {
                useAI = ((ElDocsPluginExtension) configObj).useAI;
            } else if (configObj instanceof DocsGenerator.Config) {
                useAI = ((DocsGenerator.Config) configObj).useAI;
            }
            String markdown;
            if (useAI) {
                String prompt = buildPromptStatic(name, doc, methods, classType, dependencies);
                markdown = callChatGPTStatic(prompt, configObj, true);
            } else {
                markdown = buildDocusaurusDoc(name, doc, methods, classType, dependencies);
            }
            // --- Begin package structure logic ---
            String packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
            Path packagePath = outputRoot;
            if (!packageName.isEmpty()) {
                String[] packageParts = packageName.split("\\.");
                for (String part : packageParts) {
                    packagePath = packagePath.resolve(part);
                }
                Files.createDirectories(packagePath);
            }
            Path output = packagePath.resolve(name + ".md");
            writeIfChangedStatic(output, markdown);
            // Track direct children for package summary
            packageClassMap.computeIfAbsent(packageName, k -> new ArrayList<>()).add(name);
        } catch (Exception e) {
            System.err.println("🚨 Error processing " + path + ": " + e.getMessage());
        }
    }

    private static List<String> extractMethodDocs(ClassOrInterfaceDeclaration clazz) {
        List<String> methods = new ArrayList<>();
        for (MethodDeclaration method : clazz.getMethods()) {
            String mdoc = method.getJavadoc().map(Javadoc::toText).orElse("");
            methods.add("- " + method.getNameAsString() + "(): " + mdoc);
        }
        return methods;
    }

    private static String detectClassType(ClassOrInterfaceDeclaration clazz) {
        for (AnnotationExpr annotation : clazz.getAnnotations()) {
            String ann = annotation.getNameAsString();
            if (ann.equals(TYPE_REPOSITORY)) return TYPE_REPOSITORY;
            if (ann.equals(TYPE_SERVICE)) return TYPE_SERVICE;
            if (ann.equals("RestController")) return TYPE_REST_CONTROLLER;
            if (ann.equals(TYPE_CONTROLLER)) return TYPE_CONTROLLER;
        }
        return TYPE_GENERIC;
    }

    private static Set<String> extractDependencies(ClassOrInterfaceDeclaration clazz) {
        Set<String> dependencies = new HashSet<>();
        // Field injection
        for (FieldDeclaration field : clazz.getFields()) {
            boolean isAutowired = field.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals("Autowired"));
            if (isAutowired && field.getElementType() instanceof ClassOrInterfaceType) {
                dependencies.add(((ClassOrInterfaceType) field.getElementType()).getNameAsString());
            }
        }
        // Constructor injection
        clazz.getConstructors().forEach(constructor -> {
            constructor.getParameters().forEach(param -> {
                if (param.getType() instanceof ClassOrInterfaceType) {
                    dependencies.add(((ClassOrInterfaceType) param.getType()).getNameAsString());
                }
            });
        });
        return dependencies;
    }

    public static String buildPromptStatic(String className, String classDoc, List<String> methods, String classType, Set<String> dependencies) {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT_HEADER);
        sb.append("Class: ").append(className).append("\n");
        sb.append("Type: ").append(classType).append("\n");
        sb.append("Description: ").append(classDoc).append("\n");
        sb.append("Methods:\n");
        methods.forEach(m -> sb.append(m).append("\n"));
        if (!dependencies.isEmpty()) {
            sb.append(PROMPT_DEP_TREE);
            for (String dep : dependencies) {
                sb.append("- ").append(dep).append("\n");
            }
            sb.append(PROMPT_DRAW_TREE);
        }
        // Add type-specific instructions
        switch (classType) {
            case TYPE_REPOSITORY:
                sb.append(PROMPT_REPOSITORY);
                break;
            case TYPE_SERVICE:
                sb.append(PROMPT_SERVICE);
                break;
            case TYPE_REST_CONTROLLER:
            case TYPE_CONTROLLER:
                sb.append(PROMPT_REST_CONTROLLER);
                break;
            default:
                sb.append(PROMPT_GENERIC);
        }
        sb.append(PROMPT_DIAGRAMS);
        return sb.toString();
    }

    private static String buildDocusaurusDoc(String className, String classDoc, List<String> methods, String classType, Set<String> dependencies) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("id: ").append(className).append("\n");
        sb.append("title: ").append(className).append("\n");
        sb.append("sidebar_label: ").append(className).append("\n");
        sb.append("---\n\n");
        sb.append("# ").append(className).append("\n\n");
        sb.append("**Type:** ").append(classType).append("\n\n");
        sb.append("## Description\n").append(classDoc).append("\n\n");
        sb.append("## Methods\n");
        for (String m : methods) sb.append(m).append("\n");
        if (!dependencies.isEmpty()) {
            sb.append("\n## Dependencies\n");
            for (String dep : dependencies) sb.append("- ").append(dep).append("\n");
        }
        sb.append("\n---\n");
        return sb.toString();
    }

    private static String buildPackageSummary(String packageName, List<String> classNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Package: ").append(packageName.isEmpty() ? "(default)" : packageName).append("\n\n");
        sb.append("## Classes and Interfaces\n");
        for (String className : classNames) {
            sb.append("- ").append(className).append("\n");
        }
        sb.append("\n## Mermaid Package Diagram\n");
        sb.append("```mermaid\n");
        sb.append("graph TD\n");
        String pkgNode = packageName.isEmpty() ? "default" : packageName.replace('.', '_');
        for (String className : classNames) {
            sb.append("    ").append(pkgNode).append(" --> ").append(className).append("\n");
        }
        sb.append("```\n");
        return sb.toString();
    }

    private static String buildPackageSummaryWithSubpackages(String packageName, Path outputDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Package: ").append(packageName.isEmpty() ? "(default)" : packageName).append("\n\n");
        sb.append("## Classes and Interfaces\n");
        List<String> classNames = packageClassMap.getOrDefault(packageName, new ArrayList<>());
        for (String className : classNames) {
            sb.append("- ").append(className).append("\n");
        }
        // Find subpackages
        sb.append("\n## Subpackages\n");
        String prefix = packageName.isEmpty() ? "" : packageName + ".";
        Set<String> subpackages = new HashSet<>();
        for (String pkg : packageClassMap.keySet()) {
            // Only include immediate subpackages (no nested)
            if (!pkg.equals(packageName) && pkg.startsWith(prefix)) {
                String sub = pkg.substring(prefix.length());
                if (!sub.isEmpty() && !sub.contains(".")) {
                    subpackages.add(pkg);
                }
            }
        }
        for (String subpkg : subpackages) {
            sb.append("- ").append(subpkg).append("\n");
        }
        // Mermaid diagram: show package node, direct classes, and subpackages
        sb.append("\n## Mermaid Package Diagram (with subpackages)\n");
        sb.append("```mermaid\n");
        sb.append("graph TD\n");
        String pkgNode = packageName.isEmpty() ? "default" : packageName.replace('.', '_');
        for (String className : classNames) {
            sb.append("    ").append(pkgNode).append(" --> ").append(className).append("\n");
        }
        for (String subpkg : subpackages) {
            String subNode = subpkg.replace('.', '_');
            sb.append("    ").append(pkgNode).append(" --> ").append(subNode).append("\n");
        }
        sb.append("```");
        return sb.toString();
    }

    /**
     * Calls the AI service to generate documentation, if enabled.
     * @param prompt The prompt to send to the AI
     * @param configObj Configuration object (Gradle or Maven)
     * @param debug If true, prints the prompt and skips the API call
     * @return Markdown documentation or error message
     * @throws IOException if network fails
     * @throws InterruptedException if interrupted
     */
    public static String callChatGPTStatic(String prompt, Object configObj, boolean debug) throws IOException, InterruptedException {
        String apiUrl = null;
        String apiKey = null;
        // Support both Gradle and Maven plugin config
        if (configObj instanceof ElDocsPluginExtension) {
            apiUrl = ((ElDocsPluginExtension) configObj).apiUrl;
            apiKey = ((ElDocsPluginExtension) configObj).apiKey;
        } else if (configObj instanceof DocsGenerator.Config) {
            apiUrl = ((DocsGenerator.Config) configObj).apiUrl;
            apiKey = ((DocsGenerator.Config) configObj).apiKey;
        }
        // Prepare request body for OpenAI-compatible API
        String requestBody = String.format("""
            {
              \"model\": \"gpt-4\",
              \"messages\": [
                {\"role\": \"system\", \"content\": \"You are a helpful technical documentation assistant.\"},
                {\"role\": \"user\", \"content\": \"%s\"}
              ]
            }
            """, prompt.replace("\"", "\\\""));
        if (debug) {
            System.out.println("Request Body: " + requestBody);
            return prompt; // For debugging, return the prompt instead of making a request
        }
        if (apiUrl == null || apiKey == null) return "[ERROR] API config missing.";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String json = response.body();
        int idx = json.indexOf("\"content\":\"");
        if (idx == -1) return "[ERROR] No content returned.";
        // ...parse response as needed...
        return json;
    }

    public static void writeIfChangedStatic(Path output, String markdown) throws IOException {
        // Simple implementation: always write
        Files.writeString(output, markdown);
    }
}
