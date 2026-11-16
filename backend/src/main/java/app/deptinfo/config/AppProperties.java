package app.deptinfo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed configuration, bound from the "app" section of application.yml / environment. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String dataDir, String seedDir, String adminApiKey) {
}
