package uet.ndh.ddsl.parser;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.model.DomainModel;
import uet.ndh.ddsl.parser.yaml.YamlToDomainModelConverter;
import uet.ndh.ddsl.parser.yaml.LocationTracker;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Main entry point for parsing YAML DSL files into domain model AST.
 */
@Service
public class YamlParser {

    private final YamlToDomainModelConverter converter;

    public YamlParser() {
        this.converter = new YamlToDomainModelConverter();
    }

    /**
     * Parse a YAML file into a domain model.
     * @param filePath Path to the YAML file
     * @return Parsed domain model
     * @throws IOException if file cannot be read
     * @throws ParseException if YAML is malformed or invalid
     */
    public DomainModel parseFile(String filePath) throws IOException, ParseException {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            return parseStream(inputStream, filePath);
        }
    }

    /**
     * Parse a YAML input stream into a domain model.
     * @param inputStream Input stream containing YAML
     * @param sourceName Name of the source (for error reporting)
     * @return Parsed domain model
     * @throws ParseException if YAML is malformed or invalid
     */
    public DomainModel parseStream(InputStream inputStream, String sourceName) throws ParseException, IOException {
        try {
            // Read the entire content first for location tracking
            String content = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            // Create location tracker
            LocationTracker locationTracker = new LocationTracker(content, sourceName);

            // Parse with SnakeYAML
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(loaderOptions);

            Map<String, Object> yamlData = yaml.load(content);
            if (yamlData == null) {
                throw new ParseException("Empty YAML file", new SourceLocation(1, 1, sourceName));
            }

            return converter.convert(yamlData, locationTracker);
        } catch (Exception e) {
            if (e instanceof ParseException) {
                throw e;
            }
            throw new ParseException("Failed to parse YAML: " + e.getMessage(),
                new SourceLocation(1, 1, sourceName), e);
        }
    }
}
