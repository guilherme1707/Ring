/*
 * Copyright (c) 2018 Dafiti Group
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
package br.com.dafiti.ring;

import br.com.dafiti.ring.model.User;
import br.com.dafiti.ring.service.ConfigurationService;
import br.com.dafiti.ring.service.DivisionGroupService;
import br.com.dafiti.ring.service.RoleService;
import br.com.dafiti.ring.service.UserService;
import java.util.Date;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 *
 * @author Valdiney V GOMES
 */
@Component
public class Setup implements ApplicationListener<ContextRefreshedEvent> {

    private final UserService userService;
    private final RoleService roleService;
    private final ConfigurationService configurationService;
    private final DivisionGroupService divisionGroupService;

    boolean setup = false;

    @Autowired
    public Setup(
            ConfigurationService configurationService,
            UserService userService,
            RoleService roleService,
            DivisionGroupService divisionGroupService) {
        
        this.userService = userService;
        this.roleService = roleService;
        this.configurationService = configurationService;
        this.divisionGroupService = divisionGroupService;
    }

    /**
     * Application setup.
     *
     * @param e ContextRefreshedEvent
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent e) {
        int logRetention = Integer.valueOf(
                configurationService.findByParameter("LOG_RETENTION_PERIOD").getValue());

        if (!this.setup) {
            //Setup the admin role. 
            roleService.createRoleIfNotExists("USER");
            roleService.createRoleIfNotExists("ADMIN");
            roleService.createRoleIfNotExists("LORD");
            divisionGroupService.createIfNotExists("DEFAULT");

            //Setup the super user.
            if (userService.findByUsername("ring.manager") == null) {
                User user = new User();
                user.setEmail(this.configurationService.findByParameter("EMAIL_ADDRESS").getValue());
                user.setFirstName("ring");
                user.setLastName("Manager");
                user.setUsername("ring.manager");
                user.setPassword("rmanager");
                user.addRole(roleService.findByName("LORD"));
                user.setDivisionGroup(divisionGroupService.findByName("DEFAULT"));

                userService.save(user);
            }

            this.setup = true;
        }
    }

    /**
     * Calculate the expiration date.
     *
     * @param days Days gone.
     * @return Date
     */
    private Date expiration(int days) {
        return new DateTime(new Date()).minusDays(days).toDate();
    }
}
