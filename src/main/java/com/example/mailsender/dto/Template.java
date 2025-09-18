package com.example.mailsender.dto;

import lombok.Getter;
import org.jsoup.nodes.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class Template {

    private String subject;
    private String body;
    private byte[] templateFileData;
    private String templateFileName;

    public Template(String subject, String body, MultipartFile templateFile)  {
        this.subject = subject;

        try {
            this.body = cleanTemplateBodyWithJSoup(body);
            if (templateFile != null && !templateFile.isEmpty()) {
                this.templateFileData = templateFile.getBytes();
                this.templateFileName = templateFile.getOriginalFilename();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private String cleanTemplateBodyWithJSoup(String htmlBody) {
        if (htmlBody == null) return null;

        try {
            Document doc = org.jsoup.Jsoup.parseBodyFragment(htmlBody);
            org.jsoup.select.Elements variableSpans = doc.select("span[data-variable]");

            for (org.jsoup.nodes.Element span : variableSpans) {
                String variableName = span.attr("data-variable");
                span.replaceWith(new org.jsoup.nodes.TextNode("[[" + variableName + "]]"));
            }
            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            return doc.body().html();
        } catch (Exception e) {
            e.printStackTrace();
            return htmlBody;
        }
    }
}