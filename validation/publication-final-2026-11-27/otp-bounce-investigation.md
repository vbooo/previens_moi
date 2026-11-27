# Demandes répétées de code vers un domaine d’exemple

Contrôle du 27 novembre 2026, heures Europe/Paris.

- Trois requêtes réseau `POST /auth/v1/otp`, statut 200, à 18:20:20, 18:24:35 et 18:26:54 ; elles correspondent aux trois nouveaux retours SMTP signalés.
- Le détail Auth du dernier appel indique `user_confirmation_requested` pour `test@example.com` à 18:26:56. L’identité préexiste au 17 novembre ; aucune connexion réussie enregistrée pour elle au contrôle après 18:31.
- Le client du dernier appel est `okhttp/5.5.0`, également utilisé par l’application Android. L’adresse source observée dans le journal Auth appartient aux plages publiques officielles Google (`goog.json`), et pas à celles de Google Cloud (`cloud.json`). L’adresse n’est pas conservée ici.
- Un test automatique Google après dépôt de l’application est une explication fortement compatible avec ces indices. Aucun identifiant de test Google n’a été obtenu : attribution probable, non certaine. L’origine du texte « Great app! » n’est pas établie par ces seuls journaux.
- La création du compte évaluateur n’a pas demandé de code ni envoyé d’e-mail. La CI utilise des fixtures locales ; les tests natifs exécutés ici n’ont pas demandé de code réel.
- L’application locale a été arrêtée sur `emulator-5554` pendant le diagnostic ; le téléphone physique n’a pas été manipulé.

## Correction préparée

`RemoteSessionRepository.requestCode` refuse les domaines de documentation `example.com`, `example.net`, `example.org` et leurs sous-domaines avant toute requête réseau. Le flux de connexion par mot de passe reste distinct.

Un test avec MockWebServer vérifie zéro requête pour ces adresses, puis une requête normale simulée pour un domaine réel. Exécution ciblée de `RemoteRepositoryTest` réussie. Aucun e-mail n’est envoyé par ce test.

Cette correction a ensuite été diffusée en test interne dans la version 0.2.11 (26), accessible aux testeurs le 27 novembre à 18 h 49. Elle ne bloque pas les appels provenant d’un ancien binaire ou directement de l’API. Aucun réglage global SMTP/Auth n’a été désactivé et aucun compte réel n’a été supprimé ou modifié.
