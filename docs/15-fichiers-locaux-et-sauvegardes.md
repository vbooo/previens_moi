# Fichiers locaux importants et sauvegardes

État au 17 novembre 2026. Les chemins ci-dessous partent de la racine du projet, `previens-moi`. Ce document donne les emplacements, jamais les valeurs des secrets.

## À conserver et à protéger

| Emplacement | Rôle | Précaution |
|---|---|---|
| `.local/cloud/supabase.env` | Accès au serveur Supabase et à sa base de données, dont des droits privilégiés. | Privé ; ne pas transmettre ni inclure dans une capture. |
| `.local/cloud/general-secrets.env` | Accès aux services IA et Web utilisés par le moteur général. | Privé ; des clés peuvent permettre des dépenses. |
| `.local/cloud/firebase-service-account.json` | Identité du serveur qui envoie les notifications Android. | Clé privée ; jamais dans l’application ni GitHub. |
| `.local/cloud/server.env`, `.local/cloud/provider.env`, `.local/cloud/smtp-settings.json` | Fichiers de configuration utilisés pendant l’installation et les tests, pouvant contenir des secrets ou des copies. | Traiter comme privés ; ne pas supposer qu’ils sont tous la source courante de la configuration déployée. |
| `.local/cloud/google-services.json` | Configuration Firebase publique du prototype Android. | À conserver pour reconstruire le prototype. Ce n’est pas une clé privée de serveur. |
| `.local/cloud/play-google-services.json` | Configuration Firebase publique de `fr.previensmoi.app`, la version Play Store. | À conserver ; distincte de celle du prototype. |
| `android/client.properties`, `android/play-client.properties` | Paramètres publics Android générés depuis les fichiers précédents. | Régénérables avec `scripts/configure-android-client.py`, et `--play` pour la version Play. Aucun secret serveur ne doit y entrer. |
| `.local/cloud-backups/` | Copies chiffrées de données applicatives et d’authentification Supabase. | Les copies trouvées datent du 22 octobre 2026 ; elles ne constituent pas une sauvegarde actuelle ni indépendante du Mac. |
| `.local/backup-keys/` | Clés permettant de déchiffrer ces copies. | Indispensables pour restaurer. Prévoir une conservation sécurisée séparée des archives. |

Les identités Play suivantes ont été créées le 17 novembre 2026, avec autorisation explicite. Leur conservation locale ne constitue pas encore une sauvegarde indépendante :

- `.local/cloud/play-service-account.json` : clé privée du serveur qui vérifie les achats Google Play. Copie serveur installée et vérifiée dans le secret Supabase `PLAY_SERVICE_ACCOUNT_JSON`.
- `.local/play-signing/upload-keystore.p12` et `signing.properties` : clé de dépôt Play et son mot de passe. Les deux sont nécessaires pour signer les versions suivantes. Les certificats `.der` et `.pem` associés sont publics.

Une copie de téléchargement de la clé serveur Play se trouve aussi dans `~/Downloads/previens-moi-32653-a2615156ce06.json`. Elle n’a pas été supprimée automatiquement. La copie rangée et utilisée est `.local/cloud/play-service-account.json`.

Le compte Google choisi pour les achats fictifs est consigné dans `.local/cloud/play-test-settings.json`, privé ; il peut différer du compte Firebase et du compte de connexion dans l’application. La sélection de sa liste de testeurs de licence a été confirmée par capture le 17 novembre 2026.

Les artefacts de test se trouvent dans `.local/play-test/` : APK debug pour les comptes de licence, APK optimisé et AAB signés avec la clé de dépôt, ainsi que `mapping-0.2.0-14.txt`. Ce sont des fichiers locaux ; aucun téléversement Play n’est prouvé par leur présence.

## Ce que GitHub contient

Le code, les migrations de base de données, les scripts de vérification, la documentation et des comptes rendus sans secrets. Le dossier `.local/`, les paramètres clients propres à ce Mac, les clés et les résultats de compilation sont exclus de Git. Vérifier les fichiers réellement ajoutés avant chaque publication ; une règle d’exclusion ne retire pas un fichier déjà suivi.

Les réglages déployés dans Supabase et Google ne sont pas tous reconstruits par un simple clone Git. Il faut aussi retrouver les comptes Google/Firebase/Play, les accès Supabase, les clés privées nécessaires et les réglages externes documentés dans [le raccordement Play](14-abonnement-google-play.md).

## Ce qu’il faut sauvegarder hors de ce Mac

1. Les clés et secrets encore utilisés, dans un coffre chiffré ou un gestionnaire de mots de passe avec pièces jointes. Ne jamais les envoyer dans une conversation, par e-mail en clair ou dans GitHub.
2. La clé de dépôt Play **et son mot de passe**, dès leur création. Une perte peut imposer une procédure de réinitialisation auprès de Google ; ne pas compter dessus comme sauvegarde.
3. Une sauvegarde récente de la base, avec sa clé de déchiffrement conservée séparément. Les copies actuelles sur le même Mac ne protègent pas contre sa perte.

Aucune sauvegarde indépendante supplémentaire ni synchronisation vers un autre service n’a été créée par ce document. Les fichiers `.local/cloud/*session*.json`, codes e-mail et journaux de diagnostic peuvent contenir des données de test privées ; ils ne remplacent pas une sauvegarde et ne sont pas destinés au dépôt public.

Pour chaque véritable version distribuée, archiver aussi le fichier AAB exact, son numéro de version, son empreinte et le mapping R8 correspondant. Les fichiers dans `android/app/build/` peuvent être effacés lors d’un nettoyage.

## Assistance (19 novembre 2026)

`.local/cloud/support-secrets.env` contient le réglage SMTP encodé et le sel de hachage réseau de l’assistance ; `.local/cloud/support-settings.json` conserve le destinataire. Ces fichiers restent privés et hors Git. Les secrets déployés correspondants sont `SUPPORT_MAIL_CONFIG_B64` et `SUPPORT_IP_SALT`. Voir [le fonctionnement et la conservation des retours](17-assistance.md).
