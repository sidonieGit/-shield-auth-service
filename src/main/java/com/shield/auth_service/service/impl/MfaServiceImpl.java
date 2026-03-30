package com.shield.auth_service.service.impl;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.shield.auth_service.service.MfaService;
import org.springframework.stereotype.Service;

@Service
public class MfaServiceImpl implements MfaService {

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    @Override
    public String generateSecret() {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    @Override
    public String getQrCodeUrl(String secret, String email) {
        // "ShieldAuth" est le nom qui apparaîtra dans l'app (Google Authenticator)
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL("ShieldAuth", email,
                new GoogleAuthenticatorKey.Builder(secret).build());
    }

    @Override
    public boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }
}