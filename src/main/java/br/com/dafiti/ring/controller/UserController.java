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

import java.security.Principal;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import br.com.dafiti.ring.exception.Message;
import br.com.dafiti.ring.model.Blueprint;
import br.com.dafiti.ring.model.User;
import br.com.dafiti.ring.service.DivisionGroupService;
import br.com.dafiti.ring.service.MailService;
import br.com.dafiti.ring.service.RoleService;
import br.com.dafiti.ring.service.UserService;

/**
 *
 * @author Guilherme Almeida
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final RoleService roleService;
    private final MailService mailService;
    private final DivisionGroupService divisionGroupService;

    @Autowired
    public UserController(
            UserService userService,
            RoleService roleService,
            MailService mailService,
            SessionRegistry sessionRegistry,
            DivisionGroupService divisionGroupService) {

        this.userService = userService;
        this.roleService = roleService;
        this.mailService = mailService;
        this.divisionGroupService = divisionGroupService;
    }

    /**
     * List all users.
     *
     * @param model Model
     * @return User list
     */
    @GetMapping(path = "/list")
    public String list(Model model) {
        model.addAttribute("users", userService.list());
        model.addAttribute("roles", roleService.list());
        model.addAttribute("logged", userService.listLoggedIn());
        return "user/list";
    }

    /**
     * Add an user.
     *
     * @param model Model
     * @return User edit
     */
    @GetMapping(path = "/add")
    public String add(Model model) {
        User user = new User();
        model.addAttribute("user", user);
        model.addAttribute("roles", roleService.list());
        model.addAttribute("groups", divisionGroupService.findAll());
        return "user/edit";
    }

    /**
     * Edit an user.
     *
     * @param model Model
     * @param user User
     * @return User edit
     */
    @GetMapping(path = "/edit/{id}")
    public String edit(
            Model model,
            @PathVariable(value = "id") User user) {

        model.addAttribute("user", user);
        model.addAttribute("roles", roleService.list());
        model.addAttribute("groups", divisionGroupService.findAll());
        return "user/edit";
    }

    /**
     * Save an user.
     *
     * @param redirectAttributes
     * @param user User
     * @param bindingResult BindingResult
     * @param model Model
     * @return User list.
     */
    @PostMapping(path = "/save")
    public String save(
            RedirectAttributes redirectAttributes,
            @Valid @ModelAttribute User user,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            boolean error = true;

            if (bindingResult
                    .getFieldError()
                    .getField()
                    .equals("password")) {
                error = (user.getId() != null);
            }

            if (error) {
                model.addAttribute("user", user);
                model.addAttribute("roles", roleService.list());
                model.addAttribute("groups", divisionGroupService.findAll());

                return "user/edit";
            }
        }

        try {
            if (userService.save(user) == null) {
                model.addAttribute("errorMessage", "User couldn't be saved, verify if e-mail configuration are correct!");
            }
        } catch (Exception ex) {
            model.addAttribute("user", user);
            model.addAttribute("roles", roleService.list());
            model.addAttribute("groups", divisionGroupService.findAll());
            model.addAttribute("errorMessage", new Message().getErrorMessage(ex));

            return "user/edit";
        } finally {
            model.addAttribute("users", userService.list());
        }

        return "redirect:/user/list";
    }

    /**
     * Delete an user.
     *
     * @param redirectAttributes
     * @param model
     * @param id ID
     * @return User success.
     */
    @GetMapping("/delete/{id}")
    public String delete(
            RedirectAttributes redirectAttributes,
            Model model,
            @PathVariable(value = "id") Long id) {

        try {
            userService.delete(id);
        } catch (Exception ex) {
            if (ex.getClass() == DataIntegrityViolationException.class) {
                redirectAttributes.addFlashAttribute("error", "This user is being used. Remove the dependencies before deleting the user or disable it!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Fail deleting user because of " + ex.getMessage());
            }
        }

        return "redirect:/user/success";
    }

    /**
     * Delete an user.
     *
     * @param redirectAttributes
     * @param nextApprover
     * @param bindingResult
     * @param oldApprover
     * @return User list template.
     */
    @PostMapping("/delete/{id}")
    public String delete(
            RedirectAttributes redirectAttributes,
            @Valid @ModelAttribute("user") User nextApprover,
            BindingResult bindingResult,
            @PathVariable(value = "id") User oldApprover) {
        

        try {
            userService.delete(oldApprover.getId());
            redirectAttributes.addFlashAttribute("successMessage", "User successful deleted!");
        } catch (Exception ex) {
            if (ex.getClass() == DataIntegrityViolationException.class) {
                redirectAttributes.addFlashAttribute("errorMessage", "This user is being used. Remove the dependencies before deleting the user or disable it!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Fail deleting user because of " + ex.getMessage());
            }
        }

        return "redirect:/user/list";
    }

    /**
     * Render the user edit template.
     *
     * @param model Model
     * @param principal Principal
     * @return Render user edit template
     */
    @GetMapping("/password")
    public String changePassword(
            Model model,
            Principal principal) {

        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        return "user/changePassword";
    }

    /**
     * Recovery confirmation.
     *
     * @param model
     * @param username
     * @return
     */
    @PostMapping("/confirmation/{username}")
    public String recoveryConfirmation(
            Model model,
            @PathVariable(value = "username") String username) {

        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        username = username.replace("_", ".");
        User user = userService.findByUsername(username);

        if (user == null) {
            model.addAttribute("user", new User());
            return "redirect:/login";
        }

        if ((name == null || name.equals("null") || name.equals("anonymousUser"))) {
            if (userService.setResetCode(user.getUsername())) {
                Blueprint blueprint = new Blueprint(user.getEmail(), "Ring Password Recovery", "userPasswordRecover");
                blueprint.addVariable("code", user.getResetCode());

                mailService.send(blueprint);
            }
        }

        User modelUser = new User();
        modelUser.setId(user.getId());
        model.addAttribute("user", modelUser);

        if ((name == null || name.equals("null") || name.equals("anonymousUser"))) {
            return "user/confirmation";
        }

        return "user/edit";
    }

    /**
     * Indentify confirmation code sent by e-mail and redirect to change password.
     * 
     * @param userConfirmation
     * @param bindingResult
     * @param model
     * @return
     */
    @PostMapping("/confirmation")
    public String confirmCode(
            User userConfirmation,
            BindingResult bindingResult,
            Model model) {

        User user = userService.load(userConfirmation.getId());

        if (!user.getResetCode().equals(userConfirmation.getResetCode())) {
            bindingResult.rejectValue("resetCode", "error.resetCode", "This is not the correct code. Please enter with the code sent to your email!");
            userConfirmation.setResetCode("");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userConfirmation);
            return "user/confirmation";
        }

        model.addAttribute("user", user);
        return "user/changePassword";
    }

    /**
     *
     * @param model
     * @param user
     * @param bindingResult
     * @return
     */
    @PostMapping("/alter")
    public String confirmAlteration(
            Model model,
            User user,
            BindingResult bindingResult) {

        if (user.getPassword().equals(user.getConfirmPassword())) {
            userService.save(user);
            return "redirect:/user/list";
        }

        bindingResult.rejectValue("confirmPassword", "confirmPassword", "the password does not match!");
        user.setConfirmPassword("");
        model.addAttribute("user", user);
        return "user/changePassword";
    }

    /**
     * function used to return a text of success instead of a template view (to
     * be used with ajax)
     *
     * @param model
     * @return
     */
    @GetMapping("/success")
    @ResponseBody
    public String successMessage(Model model) {
        Map att = model.asMap();
        if (!att.isEmpty()) {
            if (att.keySet().contains("error")) {
                return "error: " + att.get("error");
            }
        }
        return "ok";
    }

    /**
     * change the approver of jobs that have as approver an user that is
     * supposed to be disbled and then disable the user.
     *
     * @param redirectAttributes
     * @param nextApprover
     * @param bindingResult
     * @param oldApprover
     * @return
     */
    @PostMapping("/disable/{id}")
    public String disable(
            RedirectAttributes redirectAttributes,
            @Valid @ModelAttribute("user") User nextApprover,
            BindingResult bindingResult,
            @PathVariable(value = "id") User oldApprover) {
        
        oldApprover.setEnabled(false);
        userService.save(oldApprover);
        redirectAttributes.addFlashAttribute("successMessage", "User successfully disabled!");

        return "redirect:/user/list";
    }

    /**
     * Reset user password.
     *
     * @param model Model
     * @param userId Use ID
     * @return Empty in case of fail, otherwise the reseted password.
     */
    @RequestMapping(value = "/resetpassword/{userId}")
    @ResponseBody
    public String resetPassword(
            Model model,
            @PathVariable(value = "userId") Long userId) {

        return this.userService.resetPassword(userId);
    }
}
