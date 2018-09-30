package org.corsaircode;


import com.hubspot.jinjava.Jinjava;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "jinjava", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class MyMojo
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
     * My Map.
     */
    @Parameter
    private Map context;

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

            Jinjava jinjava = new Jinjava();

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
