package com.jt.summary.service.impl;

import com.jt.summary.config.AppProperties;
import java.util.HashSet;
//import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Category2RuleService {

    private final AppProperties props;

    public boolean needCategory2(String caseType, String category1DisplayId) {
        // MOD: safe default
        if (isBlank(caseType) || isBlank(category1DisplayId)) {
            return true;
        }

        AppProperties.CaseTypeRule rule = props.getCasetypeRules().get(caseType);
        if (rule == null) {
            // MOD: unknown caseType => safe side require
            return true;
        }

        // MOD: optional whitelist validation (if mappings present)
        if (rule.getCategory1Mappings() != null && !rule.getCategory1Mappings().isEmpty()) {
            Set<String> allowed = new HashSet<>();
            rule.getCategory1Mappings().forEach(m -> {
                if (m != null && !isBlank(m.getDisplayId()))
                    allowed.add(m.getDisplayId());
            });
            if (!allowed.isEmpty() && !allowed.contains(category1DisplayId)) {
                // MOD: unknown category1_displayid for this caseType => safe side require
                return true;
            }
        }

        Boolean override = rule.getCategory1Overrides() == null
                ? null
                : rule.getCategory1Overrides().get(category1DisplayId);

        return override != null ? override : rule.isDefaultRequireCategory2();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
