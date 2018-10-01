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
import java.util.*;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "jinjava", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class JinjavaMojo
        extends AbstractMojo {

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/*.j2", "**/*.jinja2",};

    /**
     * The directory which contains the resources you want scanned for templates.
     */
    @Parameter(defaultValue = "${basedir}/src/main/resources")
    private File resourcesDirectory;

    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
    private File resourcesOutput;

    /**
     * A list of files to include. Can contain ant-style wildcards and double wildcards.
     * The default includes are
     * <code>**&#47;*.j2   **&#47;*.jinja2</code>
     *
     * @since 1.0-alpha-5
     */
    @Parameter
    private String[] includes;

    /**
     * A list of files to exclude. Can contain ant-style wildcards and double wildcards.
     *
     * @since 1.0-alpha-5
     */
    @Parameter
    private String[] excludes;

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

        MavenProject project = new MavenProject(model);
        System.out.println("==>" + project.getDependencies());
        System.out.println("==>" + project.getProperties());
        System.out.println("==>" + project.getResources());
        project.getBuildPlugins();

        List<Map<String, String>> dependencies = new ArrayList<Map<String, String>>();
        for (Dependency dependency : project.getDependencies()) {
            Map<String, String> map = new HashMap<>();
            map.put("groupId", dependency.getGroupId());
            map.put("artifactId", dependency.getArtifactId());
            map.put("type", dependency.getType());
            map.put("version", dependency.getVersion());
            dependencies.add(map);
        }
        context.put("dependencies", dependencies);

        List<Map<String, String>> properties = new ArrayList<Map<String, String>>();
        for (Map.Entry<Object, Object> property : project.getProperties().entrySet()) {
            Map<String, String> map = new HashMap<>();
            map.put(property.getKey().toString(), property.getValue().toString());
            properties.add(map);
        }
        context.put("properties", properties);


        DirectoryScanner scanner = new DirectoryScanner();

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

        List<String> includedFiles = Arrays.asList(scanner.getIncludedFiles());

        for (String resource : includedFiles) {

            try {
                String source = resourcesDirectory + "\\" + resource;
                String target = resourcesDirectory + "\\" + resource.substring(0, resource.lastIndexOf("."));

                FileUtils.fileWrite(target, jinjava.render(FileUtils.fileRead(source), context));

            } catch (IOException e) {
                throw new MojoExecutionException("Error reading source file.", e);
            }
        }
    }
}
