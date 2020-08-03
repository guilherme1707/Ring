/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.dafiti.ring.rest;

import br.com.dafiti.ring.model.User;
import br.com.dafiti.ring.service.TokenService;
import br.com.dafiti.ring.service.UserService;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 *
 * @author guilherme.almeida
 */
public class TokenAuthentication extends OncePerRequestFilter {
    
    private final TokenService tokenService;
    private final UserService userService;
    
    public TokenAuthentication(TokenService tokenService,
            UserService userService) {
        this.tokenService = tokenService;
        this.userService = userService;
    }
    

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        String token = tokenService.extractToken(request);
        boolean isValid = tokenService.validate(token);
        // authenticate user
        if(isValid) {
            Long userId = tokenService.getUserId(token);
            User user = userService.findById(userId);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
    
}
