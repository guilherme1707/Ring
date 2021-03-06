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
package br.com.dafiti.ring.service;

import br.com.dafiti.ring.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 *
 * @author guilherme.almeida
 */
@Service
public class TokenService {
    
    private final String encryptKey;
    
    public TokenService(@Value("${ring.encrypt.key}") String encryptKey) {
        this.encryptKey = encryptKey;
    }
    
    /**
     * generate a new token
     * 
     * @param authetication
     * @return
     */
    public String generateToken(Authentication authetication) {
        
        User user = (User) authetication.getPrincipal();
        
        return Jwts.builder()
                .setIssuer("Ring API")
                .setIssuedAt(new Date())
                .setSubject(user.getId().toString())
                .signWith(SignatureAlgorithm.HS256, encryptKey)
                .compact();
    }

    /**
     * extract the token from the request
     * 
     * @param request
     * @return
     */
    public String extractToken(HttpServletRequest request) {
        
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty() || !token.startsWith("Bearer ")) {
            return null;
        }

        return token.substring(7, token.length());
    }
    
    /**
     * return a boolean to indicate if token is valid or not
     * 
     * @param token
     * @return
     */
    public boolean validate(String token) {
        
        try {
            Jwts.parser().setSigningKey(encryptKey).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
        
    }

    /**
     * return the user id setted in token
     * 
     * @param token
     * @return
     */
    public Long getUserId(String token) {
        Claims body = Jwts.parser().setSigningKey(encryptKey).parseClaimsJws(token).getBody();
        return Long.parseLong(body.getSubject());
    }
}
