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
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Mojo(name = "jinjava", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class JinjavaMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.scriptSourceDirectory}")
    private String templateFolder;

    @Parameter
    private Properties templates;

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
            System.out.println(property.getKey().toString() + "==>>" + property.getValue().toString());
            properties.add(map);
        }
        context.put("properties", properties);


        for (Object key : templates.keySet()) {
            try {
                String source = (templateFolder + "/" + key.toString()).replace("\\", "/");
                String target = (templates.get(key).toString()).replace("\\", "/");

                String sourceContent = FileUtils.fileRead(source);
                String targetContent = jinjava.render(sourceContent, context);

                PrintWriter out = new PrintWriter(target);
                out.print(targetContent);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
