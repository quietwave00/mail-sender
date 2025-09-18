package com.example.mailsender.service.template;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TemplateProcessor {

    /**
     * 템플릿 변수를 실제 값으로 치환
     */
    public static String processTemplate(String templateBody, Map<String, String> variables) {
        if (templateBody == null || variables == null) {
            return templateBody;
        }

        String processedBody = templateBody;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "[[" + entry.getKey() + "]]";
            String value = entry.getValue() != null ? entry.getValue() : "";
            processedBody = processedBody.replace(placeholder, value);
        }
        return processedBody;
    }

    /**
     * 템플릿에서 사용된 변수 목록 추출
     */
    public static java.util.List<String> extractTemplateVariables(String templateBody) {
        if (templateBody == null) return java.util.Collections.emptyList();

        Pattern variablePattern = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
        Matcher matcher = variablePattern.matcher(templateBody);

        return matcher.results()
                .map(result -> result.group(1))
                .distinct()
                .collect(Collectors.toList());
    }
}