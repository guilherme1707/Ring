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

import br.com.dafiti.ring.model.Configuration;
import br.com.dafiti.ring.model.ConfigurationGroup;
import br.com.dafiti.ring.repository.ConfigurationRepository;
import br.com.dafiti.ring.security.PasswordCryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Helio Leal
 */
@Service
public class ConfigurationService {

    private final ConfigurationRepository configurationRepository;
    private final ConfigurationGroupService configurationGroupService;
    private final PasswordCryptor passwordCryptor;

    @Autowired
    public ConfigurationService(
            ConfigurationRepository configurationRepository,
            ConfigurationGroupService configurationGroupService,
            PasswordCryptor passwordCryptor) {

        this.configurationRepository = configurationRepository;
        this.configurationGroupService = configurationGroupService;
        this.passwordCryptor = passwordCryptor;

        this.init();
    }

    public Iterable<Configuration> list() {
        return configurationRepository.findAll();
    }

    public Configuration load(Long id) {
        return configurationRepository.findById(id).get();
    }

    /**
     * Find a parameter value based on the parameter name.
     *
     * @param parameter name of the parameter.
     * @return Configuration object.
     */
    public Configuration findByParameter(String parameter) {
        Configuration configuration = configurationRepository.findByParameter(parameter);

        if (configuration != null) {
            if (configuration.getType().toUpperCase().equals("PASSWORD")) {
                configuration.setValue(passwordCryptor.decrypt(configuration.getValue()));
            }
        }

        return configuration;
    }

    /**
     * Value of a parameter.
     *
     * @param parameter name of the parameter.
     * @return String with the parameter value.
     */
    public String getValue(String parameter) {
        String value = "";
        Configuration configuration = this.findByParameter(parameter);

        if (configuration != null) {
            if (configuration.getValue() != null) {
                value = configuration.getValue();
            }
        }

        return value;
    }

    public void save(Configuration configuration) {
        save(configuration, false);
    }

    public void save(Configuration configuration, boolean add) {
        Configuration config = configurationRepository.findByParameter(configuration.getParameter());

        if (config == null || !add) {
            if (!add) {
                configuration.setId(config.getId());
            }

            if (configuration.getType().toUpperCase().equals("PASSWORD")) {
                configuration.setValue(passwordCryptor.encrypt(configuration.getValue()));
            }

            configurationRepository.save(configuration);
        }
    }

    /**
     * Insert default values into Configuration table.
     */
    private void init() {
        //E-mail configuration.
        ConfigurationGroup emailGroup = new ConfigurationGroup("E-mail");
        this.configurationGroupService.save(emailGroup, true);
        this.save(new Configuration(
                "Host",
                "EMAIL_HOST",
                "smtp.gmail.com",
                "text",
                emailGroup,
                10,
                255,
                "*"), true);

        this.save(new Configuration(
                "Port",
                "EMAIL_PORT",
                "587",
                "number",
                emailGroup,
                0,
                9999,
                "*"), true);

        this.save(new Configuration(
                "Address",
                "EMAIL_ADDRESS",
                "ring.manager@dafiti.com.br",
                "text",
                emailGroup,
                10,
                255,
                "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}$"), true);

        this.save(new Configuration(
                "Password",
                "EMAIL_PASSWORD",
                "",
                "password",
                emailGroup,
                05,
                50,
                "*"), true);


        //Generic configuration.
        ConfigurationGroup others = new ConfigurationGroup("Others");
        this.configurationGroupService.save(others, true);
        this.save(new Configuration(
                "Log retention (in days)",
                "LOG_RETENTION_PERIOD",
                "90",
                "number",
                others,
                15,
                365,
                "*"), true);

    }
}
