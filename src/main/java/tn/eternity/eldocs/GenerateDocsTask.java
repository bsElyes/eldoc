package tn.eternity.eldocs;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateDocsTask extends DefaultTask {

  private Logger getLog() {
    return LoggerFactory.getLogger(GenerateDocsTask.class);
  }

  @TaskAction
  public void generateDocs() throws IOException, InterruptedException {
    ElDocsPluginExtension config =
        getProject().getExtensions().findByType(ElDocsPluginExtension.class);
    if (config == null) throw new IllegalStateException("elDocs extension not configured");

    DocsGenerator.Config dgConfig = new DocsGenerator.Config();
    dgConfig.sourceDir = config.sourceDir;
    dgConfig.outputDir = config.outputDir;
    dgConfig.changedOnly = config.changedOnly;
    // Add other config fields as needed

    new DocsGenerator().generateDocs(dgConfig);
  }
}
