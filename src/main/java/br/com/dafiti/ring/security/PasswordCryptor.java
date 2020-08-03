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

import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generic functions to encrypt and decrypt passwords, always based to
 * ring.encrypt.key
 *
 * @author Helio Leal
 */
@Component
public class PasswordCryptor {

    private final String encryptKey;

    @Autowired
    public PasswordCryptor(
            @Value("${ring.encrypt.key}") String encryptKey) {

        this.encryptKey = encryptKey;
    }

    /**
     * Encrypt a password
     *
     * @param password Password
     * @return Encrypted password
     */
    public String encrypt(String password) {
        String encryptedPassword = "";
        StrongTextEncryptor encryptor = new StrongTextEncryptor();
        encryptor.setPassword(encryptKey);

        try {
            encryptedPassword = encryptor.encrypt(password);
        } catch (Exception ex) {
        }

        return encryptedPassword;
    }

    /**
     * Decrypt a password
     *
     * @param password Password
     * @return Decrypted password
     */
    public String decrypt(String password) {
        String decryptedPassword = "";

        StrongTextEncryptor decryptor = new StrongTextEncryptor();
        decryptor.setPassword(encryptKey);

        try {
            decryptedPassword = decryptor.decrypt(password);
        } catch (Exception ex) {
        } finally {
            if (decryptedPassword != null && !decryptedPassword.isEmpty()) {
                password = decryptedPassword;
            }
        }

        return password;
    }
}
