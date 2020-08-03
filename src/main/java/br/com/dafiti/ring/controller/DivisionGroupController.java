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

import br.com.dafiti.ring.model.DivisionGroup;
import br.com.dafiti.ring.service.DivisionGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author guilherme.almeida
 */
@Controller
@RequestMapping("/group")
public class DivisionGroupController {
    
    private final DivisionGroupService divisionGroupService;
    
    @Autowired
    public DivisionGroupController(DivisionGroupService divisionGroupService) {
        this.divisionGroupService = divisionGroupService;
    }
    
    
    /**
     * creates a new group
     * 
     * @param model
     * @return
     */
    @GetMapping(path = "/create")
    public String createGroup(Model model) {
        
        model.addAttribute("group", new DivisionGroup());
        
        return "group/edit";
    }
    
    /**
     * list all groups
     * 
     * @param model
     * @return
     */
    @GetMapping(path = "/list")
    public String listGroups(Model model) {
        
        model.addAttribute("groups", divisionGroupService.findAll());
        
        return "group/list";
    }
    
    /**
     * open a group in view mode
     * 
     * @param model
     * @param divisionGroup
     * @return
     */
    @GetMapping(path = "/view/{id}")
    public String viewGroup(Model model,
            @PathVariable(value = "id") DivisionGroup divisionGroup) {
        
        model.addAttribute("group", divisionGroup);
        return "group/view";
    }
    
    /**
     * open a group in edit mode
     * 
     * @param model
     * @param divisionGroup
     * @return
     */
    @GetMapping(path = "/edit/{id}")
    public String editGroup(Model model,
            @PathVariable(value = "id") DivisionGroup divisionGroup) {
        
        model.addAttribute("group", divisionGroup);
        return "group/edit";
    }
    
    /**
     * saves the group object
     * 
     * @param model
     * @param manualInputUserGroup
     * @return
     */
    @PostMapping(path = "/save")
    public String saveGroup(Model model,
            @ModelAttribute DivisionGroup divisionGroup) {
        
        DivisionGroup newGroup = divisionGroupService.save(divisionGroup);
        
        return "redirect:/group/view/" + newGroup.getId();
    }
    
}
