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
package br.com.dafiti.ring.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import br.com.dafiti.ring.model.Configuration;
import br.com.dafiti.ring.service.ConfigurationGroupService;
import br.com.dafiti.ring.service.ConfigurationService;

/**
 *
 * @author Helio Leal
 */
@Controller
@RequestMapping(path = "/settings")
public class ConfigurationController {

    private final ConfigurationService configurationService;
    private final ConfigurationGroupService configurationGroupService;

    @Autowired
    public ConfigurationController(ConfigurationService configurationService,
            ConfigurationGroupService configurationGroupService) {

        this.configurationService = configurationService;
        this.configurationGroupService = configurationGroupService;
    }

    /**
     * Save the configuration came by ajax.
     *
     * @param value value
     * @param request request
     * @param parameter Parameter
     * @return
     */
    @PostMapping("/save/{parameter}")
    @ResponseBody
    public boolean save(
            @RequestBody String value,
            HttpServletRequest request,
            @PathVariable(value = "parameter") String parameter) {

        Configuration configuration = this.configurationService.findByParameter(parameter);
        boolean result = true;

        if (configuration != null && !value.isEmpty()) {
            try {
                configuration.setValue(value);
                this.configurationService.save(configuration);
            } catch (Exception ex) {
                result = false;
            }
        } else {
            result = false;
        }

        return result;
    }

    /**
     * Edit configurations.
     *
     * @param model Model
     * @return Configuration edit template.
     */
    @RequestMapping(path = "/edit")
    public String edit(Model model) {
        model.addAttribute("configurationGroup", configurationGroupService.list());
        return "configuration/edit";
    }
}