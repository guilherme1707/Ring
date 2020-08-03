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

import br.com.dafiti.ring.model.ManualInput;
import br.com.dafiti.ring.service.ManualInputService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Valdiney V GOMES
 */
@Controller
@RequestMapping("/search")
public class ManualInputSearchController {

    private final ManualInputService manualInputService;

    private final String SEARCH_COOKIE = "a212aa8752164cfa1bde02b00c6af44a";

    @Autowired
    public ManualInputSearchController(ManualInputService manualInputService) {
        this.manualInputService = manualInputService;
    }

    /**
     * Show the search page.
     *
     * @param model Model
     * @param request Request
     * @return Search template
     */
    @GetMapping("/search")
    public String search(
            Model model,
            HttpServletRequest request) {
        List<String> searches = new ArrayList();

        try {
            Cookie[] cookies = request.getCookies();

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(SEARCH_COOKIE)) {
                        searches = new ArrayList<>(Arrays.asList(cookie.getValue().split("#")));
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(ManualInputSearchController.class.getName()).log(Level.SEVERE, "Fail reading cookies", ex);
        }

        model.addAttribute("searches", searches);
        return "search/manualInputSearch";
    }

    /**
     * Search job details.
     *
     * @param response HttpServletResponse
     * @param request HttpServletRequest
     * @param model Model
     * @param search Search expression.
     * @return Job details details fragment
     */
    @PostMapping("/details/")
    public String details(
            HttpServletResponse response,
            HttpServletRequest request,
            Model model,
            @RequestBody String search) {

        List<ManualInput> manualInputs = (List) manualInputService.findByNameContaining(search);
        List<String> searches = new ArrayList();

        if (!manualInputs.isEmpty()) {
            try {
                Cookie[] cookies = request.getCookies();

                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (cookie.getName().equals(SEARCH_COOKIE)) {
                            searches = new ArrayList<>(Arrays.asList(cookie.getValue().split("#")));
                        }
                    }
                }

                if (!searches.contains(search)) {
                    if (searches.size() >= 5) {
                        searches.remove(searches.get(0));
                    }

                    searches.add(search);
                }

                Cookie cookie = new Cookie(SEARCH_COOKIE, String.join("#", searches));
                cookie.setMaxAge(7 * 24 * 60 * 60);
                cookie.setPath("/");
                response.addCookie(cookie);

            } catch (Exception ex) {
                Logger.getLogger(ManualInputSearchController.class.getName()).log(Level.SEVERE, "Fail managing cookies", ex);
            }
        }

        request.getCookies();

        
        model.addAttribute("manualInputList", manualInputs);
        
        return "search/fragmentSearchResult::searchResult";
    }
}