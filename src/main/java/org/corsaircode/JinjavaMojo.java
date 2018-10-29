package org.corsaircode;


import com.google.common.collect.Maps;
import com.hubspot.jinjava.Jinjava;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(name = "jinjava", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class JinjavaMojo extends AbstractMojo {

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/*.j2"};

    /**
     * The directory which contains the resources you want scanned for jinja template files.
     */
    @Parameter(defaultValue = "${project.build.scriptSourceDirectory}")
    private File resourcesDirectory;

    /**
     * The directory where to place the processed jinja template files
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/jinja")
    private File resourcesOutput;

    /**
     * A list of files to include. Can contain ant-style wildcards and double wildcards.
     * The default includes are
     * <code>**&#47;*.j2</code>
     */
    @Parameter
    private String[] includes;

    /**
     * A list of files to exclude. Can contain ant-style wildcards and double wildcards.
     */
    @Parameter
    private String[] excludes;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException {

        Jinjava jinjava = new Jinjava();

        Map<String, Object> context = Maps.newHashMap();

        Model model = null;
        FileReader reader = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try {
            reader = new FileReader(new File("pom.xml"));
            model = mavenreader.read(reader);
            model.setPomFile(new File("pom.xml"));
        } catch (Exception ex) {
        }

        MavenProject mavenProject = new MavenProject(model);

        Map<String, String> details = new HashMap<String, String>();
        details.put("groupId", mavenProject.getModel().getGroupId());
        details.put("artifactId", mavenProject.getModel().getArtifactId());
        details.put("description", mavenProject.getModel().getDescription());
        details.put("name", mavenProject.getModel().getName());
        context.put("project", details);

        List<Map<String, String>> dependencies = new ArrayList<Map<String, String>>();
        for (Dependency dependency : mavenProject.getDependencies()) {
            Map<String, String> map = new HashMap<>();
            map.put("groupId", dependency.getGroupId());
            map.put("artifactId", dependency.getArtifactId());
            map.put("type", dependency.getType());
            map.put("version", dependency.getVersion());
            dependencies.add(map);
        }
        context.put("dependencies", dependencies);

        Map<String, String> properties = new HashMap<String, String>();
        for (Map.Entry<Object, Object> property : mavenProject.getProperties().entrySet()) {
            properties.put(property.getKey().toString(), property.getValue().toString());
        }
        context.put("properties", properties);

        FileUtils.mkdir(resourcesOutput.getPath());

        DirectoryScanner scanner = new DirectoryScanner();

        if( resourcesDirectory.isDirectory()) {

            scanner.setBasedir(resourcesDirectory);
            if (includes != null && includes.length != 0) {
                scanner.setIncludes(includes);
            } else {
                scanner.setIncludes(DEFAULT_INCLUDES);
            }

            if (excludes != null && excludes.length != 0) {
                scanner.setExcludes(excludes);
            }

            scanner.addDefaultExcludes();
            scanner.scan();

            for (String key : scanner.getIncludedFiles()) {
                File file = new File(resourcesOutput + "\\" + key.substring(0, key.lastIndexOf(".")));
                try {
                    String sourceContent = FileUtils.fileRead(resourcesDirectory + "\\" + key);
                    String targetContent = jinjava.render(sourceContent, context);

                    file.getParentFile().mkdirs();

                    PrintWriter out = new PrintWriter(file);
                    out.print(targetContent);
                    out.close();

                } catch (IOException e) {
                    throw new MojoExecutionException("Unable to process jinja template file", e);
                }
            }

            try {
                File output = new File(project.getBuild().getOutputDirectory());
                File target = new File(output.getParentFile() + "/" + project.getBuild().getFinalName());
                FileUtils.copyDirectoryStructure(resourcesOutput, target);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to copy generated resources to build output folder", e);
            }
        }
    }
}
