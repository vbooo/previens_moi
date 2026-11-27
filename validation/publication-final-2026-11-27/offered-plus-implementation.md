# Accès offert et enveloppe autorisés

Fabien a explicitement autorisé le 27 novembre 2026 l’accès Plus offert au seul compte évaluateur `contact@fabienbouvet.fr`, le plafond de **6 EUR cumulés**, le démarrage des essais et la poursuite de la préparation Play. Cet accord remplace le plafond d’essais antérieur de 2 EUR ; il ne fixe pas le budget d’exploitation publique.

## Droit et affichage

Le compte reste un utilisateur ordinaire, authentifié par le parcours de mot de passe existant. Une ligne privée `plus_grants`, administrable uniquement par l’opérateur, attribue Plus ; aucun achat, jeton Google ni rôle administrateur n’est fabriqué. Le client ne reçoit que son état et ses quotas. L’interface FR/EN indique l’accès offert, sans prix, renouvellement automatique ou résiliation Google. Le contrôle manuel interroge le serveur pour ce droit même si Google Play est indisponible. Le cache hors ligne reste lié au compte et borné par la période vérifiée.

Les quotas restent ceux de Plus : deux suivis simultanés, cadence quotidienne, trois sources au maximum, cinq créations/changements et 25 analyses par période. Les périodes de quota sont de trente jours ; elles ne renouvellent jamais le budget financier global. Révocation et suppression ferment les opérations protégées.

## Plafond et fermeture des ventes

La migration 47 élargit uniquement les contraintes structurelles ; l’activation contrôlée séparée a porté les limites modèle à 2900000 micro-USD et Web à 435 crédits gratuits. Les compteurs consommés, réservés et engagés sont conservés. La capacité partagée reste trois suivis. `billing_policy.test_only=true` et `monitoring_enabled=false` sont inchangés : seul le bénéficiaire explicite peut utiliser la surveillance Plus offerte. Aucun forfait fournisseur ni recharge. La borne prudente est 5,87087625 EUR ; ce n’est pas une facture rapprochée et cela ne plafonne pas Google Cloud.

Le déploiement API conserve la vérification JWT de plateforme activée pour `api`, ainsi que la vérification utilisateur Supabase dans le code. Le point public conserve sa configuration publique antérieure et les jetons de prévisualisation. Aucun affaiblissement d’authentification.

## Contrôles exécutés

- Base jetable locale : achats Google existants, droit offert, accès réservé à son compte, quotas et rejeu, consommation par le moteur normal, conservation des coûts incertains, renouvellement de quota sans remise à zéro financière, révocation et effacement.
- Régression distincte de l’essai gratuit : concurrence, réservation des sept jours, expiration et conservation des charges inconnues. Le clone exclut uniquement la planification `pg_cron` de la rétention d’assistance, absente de cette base jetable.
- 107 tests Android, types serveur et 151 tests serveur contrôlés ; contrat OpenAPI et 45 cas valides/invalides.
- Présentation native vérifiée en français clair, puis anglais sombre à texte agrandi ; aucun bouton d’achat ou gestion Google pour l’accès offert.
- Release optimisée 0.2.12 (27), Lint, treize contrôles binaires, signatures APK et AAB avec le certificat existant. Identifiants et clés restent hors du dépôt.

Les preuves cloud de l’attribution et des limites sont distinctes des tests locaux. L’observation réelle et les déclarations Play sont consignées séparément ; ce rapport ne certifie pas sept jours exécutés ni une publication publique.
