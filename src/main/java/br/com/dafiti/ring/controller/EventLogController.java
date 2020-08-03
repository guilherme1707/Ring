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

import br.com.dafiti.ring.service.EventLogService;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Valdiney V GOMES
 */
@Controller
@RequestMapping(path = "/log")
public class EventLogController {

    private final EventLogService eventLogService;

    @Autowired
    public EventLogController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    /**
     * List event log.
     *
     * @param model
     * @return
     * @throws java.text.ParseException
     */
    @GetMapping(path = {"/list", "/list/filter"})
    public String listServer(Model model) throws ParseException {
        this.modelDefault(model);
        
        return "eventlog/list";
    }
    
    /**
     * List log with date filter.
     *
     * @param dateFrom
     * @param dateTo
     * @param model
     * @return
     */
    @PostMapping(path = "/list/filter")
    public String filter(
            @RequestParam("dateFrom") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date dateFrom,
            @RequestParam("dateTo") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date dateTo,
            Model model) {

        this.modelDefault(model, dateFrom, dateTo);
        return "eventlog/list";
    }    
    
    /**
     * Model default attribute.
     *
     * @param model Model
     */
    private void modelDefault(Model model) throws ParseException {
        
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        String from = sdfDate.format(new Date()) + " 00:00:00";
        String to = sdfDate.format(new Date()) + " 23:59:59";
        
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateFrom = sdfDateTime.parse(from);
        Date dateTo = sdfDateTime.parse(to);

        this.modelDefault(model, dateFrom, dateTo);
    }

    /**
     * Model default attribute.
     *
     * @param model Model
     * @param dateFrom start Date
     * @param dateTo end date
     */
    private void modelDefault(Model model, Date dateFrom, Date dateTo) {
        model.addAttribute("events", eventLogService.listDateBetween(dateFrom, dateTo));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        model.addAttribute("dateFrom", simpleDateFormat.format(dateFrom));
        model.addAttribute("dateTo", simpleDateFormat.format(dateTo));
    }    
}
