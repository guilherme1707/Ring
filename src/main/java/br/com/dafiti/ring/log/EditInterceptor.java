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
package br.com.dafiti.ring.log;

import br.com.dafiti.ring.model.EventLog;
import br.com.dafiti.ring.option.Event;
import br.com.dafiti.ring.service.EventLogService;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Valdiney V GOMES
 */
@Component
public class EditInterceptor implements HandlerInterceptor {

    @Autowired
    private EventLogService eventLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) {
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object object, ModelAndView model) {
        try {
            boolean partial = false;
            String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

            for (String paramenter : request.getParameterMap().keySet()) {
                partial = paramenter.startsWith("partial_");

                if (partial) {
                    break;
                }
            }

            if (!partial) {
                String id = request.getParameter("id");

                EventLog eventLog = new EventLog();
                eventLog.setUsername(request.getRemoteUser());
                eventLog.setDate(new Date());

                if (!id.isEmpty()) {
                    eventLog.setTypeID(Long.parseLong(id));
                    eventLog.setEvent(Event.EDIT);
                } else {
                    eventLog.setEvent(Event.ADD);
                }

                String name = request.getParameter("name");

                if (name == null) {
                    name = request.getParameter("username");
                }

                eventLog.setTypeName(name);


                eventLogService.save(eventLog);
            }
        } catch (Exception exception) {
            Logger.getLogger(BuildInterceptor.class.getName()).log(Level.SEVERE, "Fail recording build log!", exception);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object object, Exception ex) {
    }
}
