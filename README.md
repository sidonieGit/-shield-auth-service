
# Shield Auth Service - Identity Provider (IdP)
Ce service est un fournisseur d'identité robuste conçu pour gérer l'authentification centralisée, la génération de jetons JWT asymétriques et la double authentification (MFA).

## 🛠 Technologies
* Runtime : Java 21 / Spring Boot 3.4.3
    
* Sécurité : RSA-256 Asymmetric JWT (OAuth2 Resource Server)
    
* MFA : TOTP via Google Authenticator
    
* Base de données : MongoDB 7.0

## 🔐 Flux d'Authentification
Le service suit un processus rigoureux pour garantir la sécurité des accès :

* Inscription (Register) : POST /api/v1/auth/register

* Crée l'utilisateur en base de données.

    Note : Le corps de la requête (JSON) doit contenir le champ fullName (et non name) pour passer la validation @NotBlank.

* Configuration MFA (MFA Setup) : POST /api/v1/auth/mfa/setup

    Condition : Nécessite une authentification via Bearer Token.

* Génère une clé secrète et renvoie l'URL otpauth:// pour le QR Code.

* Connexion (Login) : POST /api/v1/auth/login

Si le MFA est activé pour l'utilisateur, la réponse renvoie mfaRequired: true.

* Un jeton temporaire est fourni pour l'étape de vérification.

* Vérification (Verification) : POST /api/v1/auth/mfa/verify

L'utilisateur envoie le code à 6 chiffres généré par son application mobile (Google Authenticator, Microsoft Authenticator, etc.).

En cas de succès, le service délivre le JWT final.

## 🛡 Sécurité des Clés (Infrastructure PKI)
Le service utilise le chiffrement asymétrique RSA pour signer les jetons.

* Emplacement : Les fichiers private.pem et public.pem doivent résider dans src/main/resources/certs.

* Génération : Utilisez OpenSSL pour générer vos paires de clés.

* Recommandation critique : Ne jamais commiter la clé privée (private.pem) sur un dépôt public. Utilisez des variables d'environnement ou un gestionnaire de secrets (comme AWS Secrets Manager) en production.

## 🚀 Configuration Postman
Pour tester les endpoints sécurisés comme /mfa/setup :

* Effectuez un Login pour obtenir l' accessToken.

* Dans l'onglet Authorization de la requête suivante, sélectionnez Bearer Token.

* Collez le jeton pour que le serveur puisse identifier le Principal (l'utilisateur connecté).