package alien4cloud.paas.cloudify3.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

import alien4cloud.utils.FileUtil;

/**
 * A velocity Util class
 */
// TODO This class must be in common module between the cloudify 2 and 3
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VelocityUtil {

    private static final Properties VELOCITY_PROPS;

    static {
        VELOCITY_PROPS = new Properties();
        VELOCITY_PROPS.put(RuntimeConstants.RESOURCE_LOADER, "file");
        VELOCITY_PROPS.put("file.resource.loader.class", FileResourceLoader.class.getName());
        VELOCITY_PROPS.put(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute.class.getName());
        VELOCITY_PROPS.put(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "false");
    }

    /**
     * Generate from the given velocity template to the given output path
     *
     * @param velocityTemplateResource template file
     * @param outputFilePath generated file destination
     * @param properties properties to inject in the template
     * @throws java.io.IOException when the resource was not found
     */
    public static void generate(Path velocityTemplateResource, Path outputFilePath, Map<String, ? extends Object> properties) throws IOException {
        FileUtil.touch(outputFilePath);
        generate(velocityTemplateResource, new FileWriter(outputFilePath.toFile()), properties);
    }

    public static void generate(Path velocityTemplateResource, Writer outputWriter, Map<String, ?> properties) throws IOException {
        VelocityEngine ve = new VelocityEngine();
        File templateFile = velocityTemplateResource.toFile();
        // reading template file
        ve.setProperty("file.resource.loader.path", templateFile.getParent());
        // initialize engine
        ve.init(VELOCITY_PROPS);

        Template template = ve.getTemplate(templateFile.getName(), "UTF-8");
        VelocityContext context = new VelocityContext();

        for (Entry<String, ?> contextEntry : properties.entrySet()) {
            context.put(contextEntry.getKey(), contextEntry.getValue());
        }

        try {
            template.merge(context, outputWriter);
        } finally {
            outputWriter.close();
        }
    }
}