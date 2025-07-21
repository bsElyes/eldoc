package tn.eternity.eldocs;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Maven Mojo for generating documentation using DocsGenerator.
 * <p>
 * Usage example in pom.xml:
 * <pre>
 * <plugin>
 *   <groupId>tn.eternity.eldocs</groupId>
 *   <artifactId>elDoc-maven-plugin</artifactId>
 *   <version>VERSION</version>
 *   <configuration>
 *     <sourceDir>src/main/java</sourceDir>
 *     <outputDir>docs</outputDir>
 *     <changedOnly>false</changedOnly>
 *     <apiUrl>http://localhost:8080/v1/chatgpt</apiUrl>
 *     <apiKey>YOUR_API_KEY</apiKey>
 *   </configuration>
 * </plugin>
 * </pre>
 * <p>
 * Parameters:
 * <ul>
 *   <li>sourceDir: Source directory to scan for Java files.</li>
 *   <li>outputDir: Output directory for generated documentation.</li>
 *   <li>changedOnly: If true, only changed files are processed.</li>
 *   <li>apiUrl: URL for AI documentation generation (optional).</li>
 *   <li>apiKey: API key for AI documentation generation (optional).</li>
 * </ul>
 */
@Mojo(name = "generate-docs", defaultPhase = LifecyclePhase.PACKAGE)
public class ElDocsMojo extends AbstractMojo {

  /** Source directory to scan for Java files. */
  @Parameter(property = "eldocs.sourceDir", defaultValue = "src/main/java")
  private String sourceDir;

  /** Output directory for generated documentation. */
  @Parameter(property = "eldocs.outputDir", defaultValue = "docs")
  private String outputDir;

  /**
   * If true, only changed files are processed.
   * This is useful for incremental builds or CI pipelines.
   */
  @Parameter(property = "eldocs.changedOnly", defaultValue = "false")
  private boolean changedOnly;

  /**
   * URL for AI documentation generation (optional).
   * If set, documentation can be enriched using an external AI service.
   */
  @Parameter(property = "eldocs.apiUrl", defaultValue = "http://localhost:8080/v1/chat/completions")
  private String apiUrl;

  /**
   * API key for AI documentation generation (optional).
   * Required if using an external AI service for documentation enrichment.
   */
  @Parameter(property = "eldocs.apiKey", defaultValue = "")
  private String apiKey;

  /**
   * Executes the documentation generation process using DocsGenerator.
   * Throws MojoExecutionException if generation fails.
   */
  @Override
  public void execute() throws MojoExecutionException {
    DocsGenerator.Config config = new DocsGenerator.Config();
    config.sourceDir = sourceDir;
    config.outputDir = outputDir;
    config.changedOnly = changedOnly;
    config.apiUrl = apiUrl;
    config.apiKey = apiKey;
    try {
      new DocsGenerator().generateDocs(config);
    } catch (Exception e) {
      getLog().error("Error during documentation generation", e);
      throw new MojoExecutionException("Failed to generate docs", e);
    }
  }
}
