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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Valdiney V GOMES
 */
@Controller
@RequestMapping(path = {"/customization"})
public class CustomizationController {

    /**
     * Get the logo image.
     *
     * @return Logo image.
     * @throws IOException
     */
    @RequestMapping(value = "/logo")
    @ResponseBody
    @Cacheable(value = "logo")
    public byte[] getLogo() throws IOException {
        File logo = new File(System.getProperty("user.home") + "/.ring/logo");

        //Identify if the custom logo exists. 
        if (!logo.exists()) {
            //Get the default logo. 
            logo = new ClassPathResource("static/images/ring.png").getFile();
        }

        return Files.readAllBytes(logo.toPath());
    }

    /**
     * Uploads a logo to Ring.
     *
     * @param file MultipartFile
     * @return
     */
    @PostMapping(path = "/uploadFile")
    @CacheEvict(value = "logo", allEntries = true)
    public String uploadLogo(@RequestParam("file") MultipartFile file) {
        File convFile = new File(System.getProperty("user.home") + "/.ring/logo");

        try {
            // If no file is selected, restore default logo.
            if (file.isEmpty()) {
                convFile.delete();
            } else {
                file.transferTo(convFile);
            }
        } catch (IOException | IllegalStateException ex) {
            Logger.getLogger(CustomizationController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "redirect:/configuration/edit";
    }

    /**
     * Shows change logo modal.
     *
     * @param model Model
     * @return Change logo modal
     */
    @GetMapping(path = "/modal/logo")
    public String changeLogoModal(Model model) {
        return "configuration/modalLogo::logo";
    }

}