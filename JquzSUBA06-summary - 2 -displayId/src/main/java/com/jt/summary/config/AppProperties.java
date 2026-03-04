package com.jt.summary.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    // MOD: bind app.casetype-rules
    private Map<String, CaseTypeRule> casetypeRules = Collections.emptyMap();

    @Data
    public static class CaseTypeRule {
        // MOD: default requirement
        private boolean defaultRequireCategory2 = true;

        // MOD: override by category1_displayid
        private Map<String, Boolean> category1Overrides = Collections.emptyMap();

        // MOD: optional mapping list (for validation/whitelist)
        private List<CategoryMapping> category1Mappings = List.of();
    }

    @Data
    public static class CategoryMapping {
        private String name;
        private String displayId;
    }
}
