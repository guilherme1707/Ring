/*
 * Copyright (c) 2020 Dafiti Group
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package br.com.dafiti.ring.service;

import br.com.dafiti.ring.model.Blueprint;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
//import org.thymeleaf.templateresolver.TemplateResolver;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;

/**
 *
 * @author Guilherme OLIVEIRA
 * @author Valdiney V GOMES
 */
@Service
public class MailService {

    private final ConfigurationService configurationService;

    @Autowired
    public MailService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * Send a mail with a HTML blueprint.
     *
     * @param blueprint blueprint
     */
    @Async
    public void send(Blueprint blueprint) {
        HtmlEmail mail = new HtmlEmail();
        
        String host = configurationService.getValue("EMAIL_HOST");
        int port = Integer.valueOf(configurationService.getValue("EMAIL_PORT"));
        String email = configurationService.getValue("EMAIL_ADDRESS");
        String password = configurationService.getValue("EMAIL_PASSWORD");

        try {
            mail.setHostName(host);
            mail.setSmtpPort(port);
            mail.setAuthenticator(new DefaultAuthenticator(email, password));
            mail.setSSLOnConnect(true);
            mail.addHeader("X-Priority", "1");
            mail.setFrom(email);
            mail.setSubject(blueprint.getSubject());
            mail.addTo(blueprint.getRecipient());
            mail.setHtmlMsg(this.getTemplateHTMLOf(blueprint.getPath(), blueprint.getTemplate(), blueprint.getVariables()));
            mail.send();
        } catch (EmailException ex) {
            Logger.getLogger(MailService.class.getName()).log(Level.SEVERE, "Fail sending e-mail", ex);
        }
    }

    /**
     * Fill in a HTML Template.
     *
     * @param path Path
     * @param template Template
     * @param variables Variables
     * @return Filled HTML Template
     */
    private String getTemplateHTMLOf(String path, String template, HashMap<String, Object> variables) {
        //Define the template resolver. 
        AbstractConfigurableTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(path += !path.endsWith("/") ? '/' : "");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML5");

        //Define the template engine. 
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);

        //Proccess the template. 
        return engine.process(template, new Context(Locale.getDefault(), variables));
    }
    
    /**
     * Verify if configuration of e-mail is ok.
     * 
     * @return
     */
    public boolean isEmailOk() {
    	String host = configurationService.getValue("EMAIL_HOST");
        String port = configurationService.getValue("EMAIL_PORT");
        String user = configurationService.getValue("EMAIL_ADDRESS");
        String password = configurationService.getValue("EMAIL_PASSWORD");
        
        // All parameters need to be full filled.
        if (host.isEmpty() || 
    		port.isEmpty() ||
    		user.isEmpty() || 
    		password.isEmpty()) {

			return false;
        }
        return true;
    }
}
