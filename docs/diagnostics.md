# Lire les diagnostics de Préviens-moi

## Sur ton OnePlus avec Android Studio

1. Branche le téléphone, ouvre le projet `android` et utilise la variante `remoteDebug`.
2. Ouvre **View → Tool Windows → Logcat**, puis sélectionne le téléphone.
3. Pour les traces de l'application, saisis `package:mine tag:PreviensMoi` dans le filtre.
4. Reproduis l'action. Note l'heure et, s'il existe, le `requestId` ; cela permet de retrouver la même requête sur le serveur.

Pour chercher aussi les plantages et erreurs générales de ton application, utilise `package:mine (tag:PreviensMoi | level:ERROR)`. Logcat peut afficher la pile d'appels d'un crash. Source officielle : [Android — View logs with Logcat](https://developer.android.com/studio/debug/logcat).

Exemple de trace applicative :

```text
event=Http result=Rejected durationMs=340 status=503 requestId=019a05be-8000-7000-8000-000000000099
```

Cela signifie : réponse du service reçue après 340 ms, mais indisponibilité HTTP 503. `NetworkFailure` signifie qu'aucune réponse HTTP exploitable n'a été reçue ; cela ne prouve pas à lui seul que le téléphone est hors connexion. `InvalidResponse` signale une réponse illisible pour le client. `Command`, `Finding` et `DraftReset` repèrent le début de ces actions ; leurs appels HTTP suivants donnent les statuts et durées. `Session` ne contient que le résultat et la durée, jamais le code ni l'adresse.

Les traces `PreviensMoi` sont activées uniquement dans les builds debug. Elles sont locales au téléphone et ne sont pas envoyées à un service de collecte. Les identifiants de requête sont des UUID validés ; le texte des demandes, les e-mails, mots de passe, codes, jetons et URL n'y sont pas inscrits.

## Côté serveur

Dans le projet Supabase, ouvre les fonctions puis leurs logs, ou la vue **Logs** en filtrant sur **Edge Function**. Les traces console sont dans `function_logs`, les invocations dans `function_edge_logs` : [documentation officielle Supabase](https://supabase.com/docs/guides/observability/logs?product=functions).

Les quatre fonctions ont une trace JSON `event: http_request`, avec `requestId`, méthode, catégorie fixe (`watches`, `commands`, etc.), statut et durée. Cherche l'UUID copié depuis Logcat. Cet UUID est aussi renvoyé dans l'en-tête `X-Request-Id` et dans le corps d'une erreur. Une reprise automatique après un refus d'authentification reçoit un nouvel UUID HTTP.

Ces traces décrivent une requête, pas toutes les étapes d'une préparation. Un HTTP 200 ne garantit pas qu'une page a permis de trouver une offre : il faut aussi consulter le statut enregistré du suivi. Les réveils du worker et les commandes asynchrones restent des requêtes séparées ; nous n'avons pas ajouté de traçage distribué complet.

## Quand cela ne suffit pas

Pour un blocage de l'interface, le débogueur et ses points d'arrêt permettent de suivre les ViewModels ; pour un problème de consommation mémoire ou de fluidité, les outils de profilage Android seront utilisés sur un cas reproductible. Aucun SDK Crashlytics/Sentry ni nouvel abonnement n'a été installé dans ce lot. Une collecte de crashs avant publication sera une décision explicite, avec sa politique de confidentialité.

En cas de problème, fournir l'action effectuée, l'heure, le modèle de téléphone, la version de l'app et le `requestId` suffit souvent pour commencer. Ne partager aucun code e-mail ni jeton ; les journaux généraux du système ou d'un fournisseur peuvent contenir d'autres informations que nos nouvelles traces.

## Avant une observation réelle : vérifier l’accès au modèle

```sh
python3 scripts/check-model-access.py
```

Ce contrôle lit la clé serveur locale et effectue uniquement un GET de métadonnées modèle : aucune génération payante, recharge ou modification du service. Il distingue notamment une clé expirée, un refus d’accès et une panne de connexion sans afficher de clé ni de réponse fournisseur brute. Un succès ne prouve pas le solde facturable ni les droits Responses. Vérifier aussi que l’empreinte de la clé déployée correspond à celle du fichier local après une rotation.

Dans les logs des fonctions `public` et `worker`, `event: model_rejected`, `phase: interpret`, `status: 401` indiquent que l’accès au modèle a été refusé avant la compréhension. Cette trace ne contient ni demande utilisateur, clé ni corps d’erreur du fournisseur. Un refus 401 explicite règle la tentative à zéro ; une réponse perdue reste couverte par sa réserve. Les anciennes réservations inconnues ne sont pas effacées rétroactivement.
