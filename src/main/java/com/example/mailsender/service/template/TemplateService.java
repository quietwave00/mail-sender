package com.example.mailsender.service.template;

import com.example.mailsender.dto.Template;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Service
public class TemplateService
{
    private Template template;

    public void setTemplate(String subject, String body, MultipartFile templateFile) throws IOException {
        this.template = new Template(subject, body, templateFile);
    }

    public Template getTemplate() {
        return template;
    }

    public Boolean hasFile() {
        return Objects.nonNull(template.getTemplateFileData());
    }

    public byte[] getFile() {
        return template.getTemplateFileData();
    }
}
