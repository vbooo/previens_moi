# Service S3 — raccordement local et qualification

Supabase CLI **2.121.0**, Deno **2.5.4**, dépendances verrouillées, PostgreSQL 17. Projet local isolé `previens-moi-s3` : API **56321**, base **56322**, Mailpit **56324**. Les conteneurs Doklear restent inchangés. Les 26 migrations ont été installées depuis une base de test vide.

## Vérifier localement

```sh
npm ci --prefix service
service/node_modules/.bin/supabase start --workdir service
service/node_modules/.bin/supabase migration up --local --workdir service
service/node_modules/.bin/supabase status --workdir service --output json > .local/s3-local-status.json
service/node_modules/.bin/supabase functions serve --workdir service --env-file .local/s3-function.env
.local/contracts-venv/bin/python service/tests/integration.py
service/node_modules/.bin/deno task --config service/deno.json check
service/node_modules/.bin/deno task --config service/deno.json test
```

Les sorties `start/status` contiennent des clés locales : les conserver dans `.local/`. Le fichier de fonctions contient un secret HMAC de test `PROVIDER_WEBHOOK_SECRET` et un `WORKER_SECRET` distinct. Aucun secret fournisseur n'est requis pour les tests locaux. Les codes e-mail arrivent uniquement dans Mailpit, expiration dix minutes, renvoi limité à une minute.

**La suite d'intégration réinitialise les fixtures métier locales et refuse toute autre cible.** Elle ouvre temporairement des qualifications de fixture puis les referme ; ses confirmations fournisseur ne prouvent pas un appel Firecrawl. `db reset --local --workdir service` détruit cette seule base de test. Ne jamais utiliser ces procédures sur des données utilisateur.

## Fonctions et protections

`public/v1` fournit les prévisualisations, évaluations et propositions. `api/v1` exige un utilisateur Auth valide et vérifie la propriété. Les commandes sont idempotentes, versionnées et limitées dans le temps ; les créations restent en préparation avant confirmation de l'état initial. RLS et droits SQL empêchent de contourner les validations Edge.

`webhook` vérifie la signature HMAC des octets bruts et la taille avant stockage. L'inbox durable est rapprochée du passage lu par l'API fournisseur authentifiée : monitor, passage, source, statut et résumé doivent correspondre. Une valeur `meaningful=true` ne devient jamais automatiquement un résultat.

`worker` traite des étapes persistées. L'exécution de l'admission, du fournisseur et de FCM est désactivée par défaut. Le budget réel et les qualifications restent fermés indépendamment de la présence de clés.

- Admission : réservation avant appel OpenAI, sortie structurée, questions identifiées, contrôle des incohérences et coût mesuré. Une réponse perdue garde sa réservation, sans second appel aveugle. Les critères compris ne prouvent pas la capacité à surveiller.
- Fournisseur : création avec identité de job durable ; rapprochement après réponse perdue ; monitor en pause au repos ; lancement explicite réservé ; attente d'une fin signée et rapprochement API ; état initial silencieux ; nouvelle réservation par passage récurrent. Pause et remplacement changent la génération, une réponse tardive conserve l'identité distante nécessaire au nettoyage. Crash final : état inconnu/dégradé et capacité gelée.
- **Le jugement sémantique général n'est pas qualifié.** Le chemin actuel sait confirmer un état initial et un passage inchangé. Un changement sans jugement qualifié produit une panne visible, jamais une fausse affirmation ni un silence annoncé comme réussite. Ce n'est pas encore un moteur général d'alertes.
- Envoi : FCM HTTP v1 avec identifiants seuls, TTL d'un jour, acceptation distincte de livraison. Prise d'émission atomique, conservée même après suppression du résultat ; l'arrêt attend les émissions engagées ou leur expiration. Android relit résultat et suivi authentifiés, contrôle compte, version, permission, expiration et doublon durable.
- Effacement : accès fermé immédiatement ; arrêt et suppression fournisseur confirmés avant purge des plans, appareils, résultats et commandes ; suppression Auth via l'API Admin ; reprise après interruption. Une borne de coût non rapprochée reste sous forme minimale sans plan ni propriétaire. Le marqueur d'effacement est rejoué après restauration, y compris si une ancienne identité Auth réapparaît.

## Planification et secrets

Deux jobs PostgreSQL existent : rétention horaire et réveil du worker chaque minute. Ce dernier est **inactif** tant que `private.worker_schedule.enabled=false`. Son activation exige une URL dédiée et un secret Vault nommé `previens-moi-worker` identique au secret serveur. La file `pg_net` ne reçoit aucun secret durable : uniquement une signature à usage unique valable 90 secondes. Les sorties du worker ne contiennent pas de texte de demande ni d'identité.

Variables serveur préparées :

| Variable | Usage |
| --- | --- |
| `WORKER_SECRET` | Appel administratif direct et signature des réveils planifiés |
| `PROVIDER_WEBHOOK_SECRET` | Vérification du webhook Firecrawl |
| `OPENAI_API_KEY` | Interprétation ; `ADMISSION_EXECUTION_ENABLED=true` **et** budget/qualification SQL requis |
| `FIRECRAWL_API_KEY` | Adaptateur ; `PROVIDER_EXECUTION_ENABLED=true` **et** méthode/capacité SQL requises |
| `PUBLIC_WEBHOOK_URL` | `https://<ref>.supabase.co/functions/v1/webhook` ; indispensable pour les essais de réception réelle |
| `FIREBASE_SERVICE_ACCOUNT` | JSON serveur uniquement ; `FCM_EXECUTION_ENABLED=true` requis |

Ne jamais placer une identité serveur dans Android. Les données publiques Firebase viennent de `google-services.json`. `scripts/check-cloud-config.py` contrôle les fichiers de `.local/cloud/` sans appel réseau et sans afficher leurs valeurs. L'option `--build` transmet seulement les données publiques à Android ; elle ne déploie rien.

## Preuves et limites

Voir le [bilan S3](../validation/s3/bilan.md). A1 historique : **7/9**, conservé. Le [correctif du 21 octobre](../validation/s3/correction-admission.md) passe **9/9** sur les mêmes cas avec 50 régressions locales ; la qualification générale reste ouverte. Le transport fournisseur, ses reprises et la récurrence sont testés avec réseau injecté et PostgreSQL réel, sans nouvelle surveillance externe. Les anciens suivis Firecrawl listés sont toujours en pause, solde 985 crédits recontrôlé.

`scripts/check-local-backup-restore.py` prouve une reconstruction depuis un fichier hors du volume Docker et le rejeu d'un effacement postérieur au snapshot. Ce test local ne prouve ni une sauvegarde indépendante hébergée, ni une récupération de clés, ni la restauration du cloud.

Il manque les projets Supabase et Firebase dédiés pour éprouver réception publique, codes externes et FCM réel. Les deux défauts A1, la découverte générale, le jugement des changements et la déduplication sémantique restent des conditions de qualité à satisfaire ; les accès seuls ne les rendent pas acquis. Aucune observation S4 commencée.

Sources officielles consultées : [planifier Edge](https://supabase.com/docs/guides/functions/schedule-functions), [visibilité des en-têtes pg_net](https://supabase.com/docs/guides/troubleshooting/database-roles-can-read-request-headers-queued-by-pg_net-ad6357), [API Firecrawl](https://github.com/firecrawl/firecrawl-docs/blob/main/api-reference/v2-openapi.json), [FCM HTTP v1](https://firebase.google.com/docs/cloud-messaging/send/v1-api), [tarifs OpenAI](https://developers.openai.com/api/docs/pricing).
