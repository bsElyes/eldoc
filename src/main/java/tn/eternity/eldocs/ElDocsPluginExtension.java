package tn.eternity.eldocs;

/**
 * Extension class for configuring the eldocs Gradle plugin.
 *
 * <p>This class holds configuration options for the documentation generator, such as source
 * directory, output directory, API endpoint, and API key. These options can be set in the Gradle
 * build script using the 'eldocs' extension.
 */
public class ElDocsPluginExtension {
  /** The source directory containing Java files to document. Default is "src/main/java". */
  public String sourceDir = "src/main/java";

  /** The output directory for generated documentation files. Default is "docs". */
  public String outputDir = "docs";

  /** If true, only changed files will be processed. Default is true. */
  public boolean changedOnly = true;

  /**
   * The API URL for the documentation generation backend (e.g., OpenAI endpoint). Default is
   * "http://localhost:8080/v1/chat/completions".
   */
  public String apiUrl = "http://localhost:8080/v1/chat/completions";

  /** The API key for authentication with the documentation backend. Default is empty string. */
  public String apiKey = "";

  /**
   * If true, use AI to generate Markdown documentation prompts. If false, generate Docusaurus-style
   * Markdown documentation directly. Default is true.
   */
  public boolean useAI = false;
}
