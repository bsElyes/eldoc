package tn.eternity.eldocs;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ElDocsPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getExtensions().create("eldocs", ElDocsPluginExtension.class);
    project.getTasks().register("generateDocs", GenerateDocsTask.class);
  }
}
