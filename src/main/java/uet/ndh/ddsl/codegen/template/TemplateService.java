package uet.ndh.ddsl.codegen.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Service for processing FreeMarker templates to generate Java code.
 */
@Service
@Slf4j
public class TemplateService {

    private final Configuration freemarkerConfig;

    public TemplateService() {
        this.freemarkerConfig = createConfiguration();
        log.info("FreeMarker template service initialized");
    }

    /**
     * Process a template with the given data model.
     *
     * @param templateName The name of the template file (without .ftl extension)
     * @param dataModel    The data model to pass to the template
     * @return Generated code as string
     */
    public String processTemplate(String templateName, Map<String, Object> dataModel) {
        try {
            Template template = freemarkerConfig.getTemplate(templateName + ".ftl");
            StringWriter output = new StringWriter();
            template.process(dataModel, output);
            return output.toString();
        } catch (IOException e) {
            log.error("Failed to load template: {}", templateName, e);
            throw new TemplateProcessingException("Failed to load template: " + templateName, e);
        } catch (TemplateException e) {
            log.error("Failed to process template: {} with data model keys: {}", templateName, dataModel.keySet(), e);
            throw new TemplateProcessingException("Failed to process template: " + templateName, e);
        }
    }

    /**
     * Create and configure FreeMarker configuration.
     */
    private Configuration createConfiguration() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_34);

        try {
            // Set template loading location - templates will be in classpath:/templates/
            config.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "templates");

            // Verify templates directory exists
            ClassPathResource templatesDir = new ClassPathResource("templates");
            if (!templatesDir.exists()) {
                log.warn("Templates directory not found at classpath:templates");
            } else {
                log.info("Found templates directory at: {}", templatesDir.getURI());
            }
        } catch (Exception e) {
            log.warn("Could not verify templates directory, proceeding with default configuration", e);
        }

        // Set default encoding
        config.setDefaultEncoding("UTF-8");

        // Set template exception handler
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        // Don't log exceptions inside FreeMarker that we gonna handle
        config.setLogTemplateExceptions(false);

        // Wrap unchecked exceptions thrown during template processing
        config.setWrapUncheckedExceptions(true);

        // Don't fall back to higher scoped variables when reading a null loop variable
        config.setFallbackOnNullLoopVariable(false);

        // Set number format to prevent scientific notation
        config.setNumberFormat("0.######");

        return config;
    }

    /**
     * Exception thrown when template processing fails.
     */
    public static class TemplateProcessingException extends RuntimeException {
        public TemplateProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
