# Service S3 — environnement local isolé

Supabase CLI **2.121.0**, Deno de contrôle **2.5.4** et dépendances npm verrouillées. PostgreSQL 17 ; runtime Edge fourni par le CLI. Aucun environnement cloud déployé. Projet local `previens-moi-s3`, ports API **56321**, PostgreSQL **56322**, mail de test **56324**. Les conteneurs Doklear ne sont ni arrêtés ni modifiés.

## Démarrer et vérifier

Depuis la racine du dépôt, Docker Desktop démarré :

```sh
npm ci --prefix service
service/node_modules/.bin/supabase start --workdir service
service/node_modules/.bin/supabase migration up --workdir service
service/node_modules/.bin/supabase status --workdir service --output json > .local/s3-local-status.json
service/node_modules/.bin/supabase functions serve --workdir service --env-file .local/s3-function.env
```

Les sorties `start/status` contiennent des clés locales : ne pas les verser dans Git ou un rapport public. Le fichier ignoré `.local/s3-function.env` contient un `PROVIDER_WEBHOOK_SECRET` aléatoire de test ; ne pas réutiliser la clé du laboratoire F3. `WORKER_SECRET` est une identité serveur distincte. Le mail arrive **uniquement** dans Mailpit local. Le code expire après dix minutes, renvoi limité à une minute. Le transport public reste à configurer séparément.

```sh
.local/contracts-venv/bin/python -m pip install -r service/requirements-test.txt
.local/contracts-venv/bin/python service/tests/integration.py
service/node_modules/.bin/deno task --config service/deno.json check
service/node_modules/.bin/deno task --config service/deno.json test
python3 scripts/check-local-android.py
```

**Le test d'intégration tronque les fixtures métier de cette seule base locale.** Il refuse une URL de service différente de `127.0.0.1:56321`. Les méthodes/capacités ouvertes pendant les tests sont des paramètres de fixture et sont désactivées en fin de suite. Aucun compte ni fournisseur public n'est appelé. Pour reconstituer la base à neuf : `supabase db reset --local --workdir service`, exclusivement sur ce projet de test. Ne jamais utiliser cette procédure sur des données utilisateur.

Les fonctions sont accessibles sous `/functions/v1/public/v1/...` et `/functions/v1/api/v1/...`. L'API JWT vérifie aussi l'utilisateur auprès d'Auth. Les RPC privées de métier valident la propriété et les commandes même lorsqu'un client tente de contourner Edge. Tables internes hors du schéma Data API ; RLS et droits explicites. Pagination opaque ordonnée `(created_at,id)`.

## Fonctionnement livré

- Prévisualisation anonyme bornée globalement et par session ; secrets hachés ; réponse honnête `unqualified_method` tant que le moteur ne passe pas sa qualification.
- Auth Supabase par code e-mail réel local, avec boîte mail ; comptes isolés, JWT falsifié et compte fermé refusés.
- Commandes UUIDv7, reçus idempotents, versions attendues, attribution unique des propositions. Pending distinct d'active ; aucune activation réelle dans l'installation par défaut.
- Transactions de pause/reprise/remplacement/suppression ; historique de plans immuable ; ancienne exécution arrêtée avant remplacement ; état initial sans résultat artificiel. Les confirmations fournisseur sont des ports internes, exercés par les fixtures SQL, **pas un adaptateur Firecrawl réel**.
- Jobs durables, baux exclusifs et trois essais au maximum. Une création dont la réponse est perdue devient ambiguë et n'est pas recréée aveuglément. Échec d'arrêt : état inconnu/dégradé et capacité gelée.
- Réservation atomique d'unités avant un appel ; dépassement de la réservation gèle l'exécution. Les unités de fixture ne constituent ni une mesure de facture ni un tarif de production.
- Webhook HMAC sur octets bruts avant stockage, taille bornée, format F3 et déduplication. Inbox seule : un `meaningful=true` signé ne crée pas de résultat. Le rapprochement fournisseur et la qualification restent à brancher.
- Résultat, fait métier et intentions d'envoi atomiques ; première alerte ponctuelle conservée lorsque le suivi termine. Contrôle compte/génération/appareil avant émission.
- Worker FCM HTTP v1 séparé et transport testé sans Google. `FCM_EXECUTION_ENABLED` absent/false : aucun envoi, aucune prise de bail. Pour envoyer, il faudrait une configuration Firebase dédiée valide **et** les preuves manquantes du bilan. `FIREBASE_SERVICE_ACCOUNT` est uniquement côté serveur ; ne jamais l'intégrer à Android.
- Purge horaire locale des propositions, résultats et traces selon les durées prévues ; registre d'effacement et fonction de rejeu. Le test de rejeu d'un marqueur ne prouve pas une sauvegarde indépendante ni une purge complète d'Auth.

## Limites qui bloquent la clôture de S3

Voir `validation/s3/bilan.md`. Il manque un moteur d'admission et un adaptateur de surveillance qualifiés, leur récurrence et arrêt réellement éprouvés, un projet Firebase et un vrai push, les accès cloud pour l'environnement dédié, la délivrabilité publique des codes et la chaîne complète. La purge complète d'identité/compte et la restauration d'une sauvegarde indépendante restent à terminer et prouver. Une petite fenêtre entre autorisation d'envoi et HTTP externe doit être qualifiée dans le test concurrent de pause ; le test d'arrêt avant la dernière autorisation ne prouve pas toutes les courses possibles. Aucun cron fournisseur ou FCM actif. Aucun test de sept jours lancé.

## Sources de mise en œuvre

Consultées le 20 octobre 2026 : [développement local Supabase](https://supabase.com/docs/guides/local-development), [validation utilisateur Auth](https://supabase.com/docs/reference/javascript/auth-getuser), [réception FCM Android](https://firebase.google.com/docs/cloud-messaging/android/receive-messages), [versions Firebase](https://firebase.google.com/support/release-notes/android), [versions AndroidX](https://developer.android.com/jetpack/androidx/versions). FCM **25.1.3** garde minSDK23 ; la version 26 exige minSDK24 et n'a pas été retenue. WorkManager **2.11.2** est isolé dans la variante remote. Le reste des versions Android est conservé.
