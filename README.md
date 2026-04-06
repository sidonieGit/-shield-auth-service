
# Shield Auth Service - Identity Provider (IdP)
Ce service est un fournisseur d'identité robuste conçu pour gérer l'authentification centralisée, la génération de jetons JWT asymétriques et la double authentification (MFA).

## 🛠 Technologies & Sécurité
- **Runtime** : Java 21 / Spring Boot 3.4.3
- **Sign. Asymétrique** : RSA-256 (Clé privée pour signature, publique pour validation).
- **MFA** : TOTP (Google Authenticator) + Rotation de jeton.
- **Activation** : Flux d'activation par email via Mailtrap (Statut `active: false` par défaut).

## 🚀 Flux d'Inscription & Activation
1. **Register** : `POST /api/v1/auth/register`. Création du compte + Token d'activation.
2. **Email** : Envoi automatique du lien `https://localhost:4200/auth/activate?token=...`.
3. **Activation** : Le clic sur le lien active le compte et génère un **Auto-Login** (Token RSA renvoyé immédiatement).

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

# 🛡️ Infrastructure PKI (.gitignore)
Les fichiers suivants sont strictement exclus du dépôt :
- `src/main/resources/certs/*.pem` (Clés privées/publiques).
- `.env` (Identifiants Mailtrap et Google).

## 🚀 Configuration Postman
Pour tester les endpoints sécurisés comme /mfa/setup :

* Effectuez un Login pour obtenir l' accessToken.

* Dans l'onglet Authorization de la requête suivante, sélectionnez Bearer Token.

* Collez le jeton pour que le serveur puisse identifier le Principal (l'utilisateur connecté).