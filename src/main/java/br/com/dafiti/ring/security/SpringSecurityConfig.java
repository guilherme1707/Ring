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
package br.com.dafiti.ring.security;

import br.com.dafiti.ring.rest.TokenAuthentication;
import br.com.dafiti.ring.service.TokenService;
import br.com.dafiti.ring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 *
 * @author Valdiney V GOMES
 * @author Guilherme Oliveira Fonseca de Almeida
 *
 * https://stackoverflow.com/questions/48904238/spring-boot-authenticating-both-a-stateless-rest-api-and-a-stateful-login-web
 */
@Configuration
@EnableWebSecurity
@EnableScheduling
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserService userDetailService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final SessionRegistry sessionRegistry;
    private final AuthenticationSuccessHandler loginSuccessHandler;
    private final TokenService tokenService;
    private final boolean anonymousEnabled;

    @Autowired
    public SpringSecurityConfig(UserService userDetailService,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            SessionRegistry sessionRegistry,
            AuthenticationSuccessHandler loginSuccessHandler,
            TokenService tokenService,
            @Value("${ring.anonymous.access:true}") boolean anonymousEnabled) {

        this.userDetailService = userDetailService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.sessionRegistry = sessionRegistry;
        this.loginSuccessHandler = loginSuccessHandler;
        this.tokenService = tokenService;
        this.anonymousEnabled = anonymousEnabled;

    }

    /* START API CONFIGURATION */
    @Configuration
    @Order(1)
    public class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        /*@Override
        public void configure(WebSecurity web) throws Exception {
            web
                    .ignoring()
                    .antMatchers(
                            "/observer",
                            "/webjars/**",
                            "/css/**",
                            "/js/**",
                            "/images/**",
                            "/customization/**");
        }*/

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http.sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .addFilterBefore(new TokenAuthentication(tokenService, userDetailService), UsernamePasswordAuthenticationFilter.class);

            http.antMatcher("/api/**")
                    .authorizeRequests()
                    .antMatchers("/api/get").authenticated()
                    .antMatchers("/api/auth").permitAll()
                    .and()
                    .csrf().disable();

        }

    }

    /* END API CONFIGURATION */

 /* START APLICATION CONFIGURATION */
    @Configuration
    @Order(2)
    public class AplicationWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Override
        public void configure(WebSecurity web) throws Exception {
            web
                    .ignoring()
                    .antMatchers(
                            "/observer",
                            "/webjars/**",
                            "/css/**",
                            "/js/**",
                            "/images/**",
                            "/customization/**",
                            "/**.html",
                            "/v2/api-docs",
                            "/configuration/**",
                            "/swagger-resources/**");
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .maximumSessions(1)
                    .sessionRegistry(sessionRegistry);

            //Identify if anonymous access is enabled.
            if (anonymousEnabled) {
                http
                        .authorizeRequests()
                        .antMatchers("/",
                                "/**/home",
                                //"/**/list",
                                //"/**/view/**",
                                //"/**/detail/**",
                                //"/**/search/**",
                                //"/**/log/**",
                                //"/flow/**",
                                //"/propagation/**",
                                "/**/user/confirmation/**",
                                "/**/alter/",
                                "/error/**"
                        //"/user/edit/**",
                        //"/build/history/**"
                        )
                        .permitAll();
            }

            http.antMatcher("/**/**/**/**")
                    .authorizeRequests()
                    .antMatchers(
                            "/manual-input/create",
                            "/manual-input/delete/**",
                            "/manual-input/save/**",
                            "/manual-input/edit/**").access("hasRole('ADMIN') || hasRole('LORD')")
                    .antMatchers(
                            "/**/create",
                            "/**/edit/**",
                            "/**/save/**",
                            "/**/delete/**").access("hasRole('LORD')")
                    .antMatchers(
                            "/**/**/view/**",
                            "/**/**/list/**",
                            "/**/**/uploadfile/**",
                            "/log/import/**").access("hasRole('USER') || hasRole('ADMIN') || hasRole('LORD')")
                    .anyRequest().authenticated()
                    .and()
                    .formLogin().loginPage("/login").permitAll().defaultSuccessUrl("/home").successHandler(loginSuccessHandler)
                    .and()
                    .logout().logoutUrl("/logout").permitAll().logoutSuccessUrl("/home")
                    .and()
                    .requestCache()
                    .and()
                    .exceptionHandling().accessDeniedPage("/403");

        }

    }

    /* END APLICATION CONFIGURATION */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailService)
                .passwordEncoder(bCryptPasswordEncoder);
    }

    @Override
    @Bean
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager(); //To change body of generated methods, choose Tools | Templates.
    }

}
