# Sprint 3 — bilan local du 20 octobre 2026

**S3 reste ouvert.** Le socle Supabase/PostgreSQL/Auth local et la variante Android connectée sont implémentés et testés. Cela ne constitue pas une application de surveillance réelle terminée. Aucun service cloud, fournisseur récurrent ou push FCM réel n'a été qualifié ou activé. Aucune observation de sept jours commencée.

Implémentation enregistrée dans le commit local `c658b4f`. Les preuves et statuts sont enregistrés dans le commit documentaire suivant.

Travail réalisé dans le dépôt existant `/Users/fabienbouvet/Documents/Emploi/Mobile/Android/previens-moi`, depuis `4772eb3`, sur `main`, sans remote, sans réinitialiser les modifications du projet. Les conteneurs Doklear sont restés actifs et inchangés. Environnement local distinct `previens-moi-s3`, ports 56321/56322/56324.

## État des six tâches

| Tâche | Livré et prouvé localement | Ce qui empêche la clôture |
| --- | --- | --- |
| S3-01 | Choix Supabase/FCM conservé, outillage verrouillé, contrat versionné cohérent, exécution externe fermée et budget repris. | Facture complète d'ouverture encore conditionnelle : domaine d'expédition si nécessaire, sauvegarde indépendante et coût du moteur qualifié. Aucun achat à demander maintenant. |
| S3-02 | Sept migrations appliquées, PostgreSQL/RLS réels, API Edge, codes e-mail dans la boîte locale, accès A/B, sessions Android Keystore et variante remote branchée aux ports. | Environnement cloud dédié et transport public de codes non configurés ; adaptation complète des questions identifiées, polling d'évaluations et de certains détails de méthode/correction à terminer avant raccordement d'une admission réelle. |
| S3-03 | Critères conservés avant nouveaux appels, refus explicite d'une méthode non qualifiée, aucune activation mensongère. Rejeu des régressions existantes sans réseau. | Admission libre, découverte, pertinence, récence et déduplication sémantique non qualifiées. Adaptateur fournisseur réel et récurrence bornée encore à implémenter/éprouver. Les résultats F0–F3 ne sont pas réinterprétés comme une réussite générale. |
| S3-04 | HMAC avant stockage, inbox durable, transactions résultat/outbox, quotas concurrents, baux et reprises bornées, pause/reprise/remplacement/suppression avec générations et arrêt explicite. | Arrêt fournisseur et arrêt des coûts non prouvés réellement ; confirmations testées via fixtures SQL. Rapprochement API fournisseur absent. Fenêtre autorisation/envoi HTTP concurrent à qualifier. Fermeture des accès et rejeu du marqueur prouvés ; purge complète d'identité/compte et sauvegarde indépendante non prouvées. |
| S3-05 | FCM/WorkManager Android isolés dans remote ; worker HTTP v1 serveur, identifiants seuls, relecture authentifiée du résultat et du suivi, permission et navigation préparées. | Fabien a confirmé l'absence de projets dédiés. Aucun projet Firebase configuré ni notification réellement reçue. Refus de permission, retards, session expirée au clic et résultat supprimé doivent encore être qualifiés par les parcours de livraison réelle. |
| S3-06 | État de préparation explicite, coûts et preuves séparés, distinctions silence/panne/quota testées localement. | Chaîne réelle et références attendues manquantes ; observation non lancée. Les sept jours et la publication restent S4. |

Le travail restant n'est donc **pas seulement une question d'accès**. Il comprend aussi l'admission et l'adaptateur fournisseur qualifiés, les courses d'envoi, la purge complète et les derniers raccordements de contrat.

## Ce qui fonctionne réellement

Une personne peut installer la variante locale, recevoir un vrai code Supabase dans Mailpit, se connecter et retrouver sa session chiffrée après recréation et arrêt du processus. Le mail reste dans la boîte de test ; rien n'est envoyé à une adresse externe. L'e-mail sert à la connexion ; les futures alertes passeront par les notifications Android.

Le serveur refuse un JWT altéré et les accès croisés. Les commandes ont des UUIDv7, un reçu idempotent et une version attendue ; une ancienne réussite ne confirme pas une autre action. Une préparation n'est pas présentée comme active. La pause bloque les nouvelles intentions, conserve la place jusqu'à l'arrêt confirmé et expose `stopping/unknown`. Reprise et remplacement attendent un nouvel état initial. La première intention légitime d'un suivi ponctuel n'est pas annulée par son passage à terminé.

Les créations dont la réponse est perdue restent ambiguës, sans seconde création aveugle. Un arrêt définitivement en échec gèle les nouvelles activations. Une sous-estimation de coût gèle aussi l'exécution et garde la réservation à rapprocher. Ces règles sont testées avec des unités de fixture, pas avec une facture fournisseur.

La variante demo reste sans INTERNET et sans Firebase/WorkManager. Les écrans connectés signalent le service de test et l'absence de surveillance réelle. Les sources, critères et preuves sont enrichis ; une date sans heure n'est pas transformée en instant fictif.

## Vérifications exécutées

- **17 scénarios d'intégration** sur PostgreSQL, Supabase Auth et Edge HTTP réels locaux : code invalide/expiré, JWT altéré, propriété A/B, RLS, quota concurrent, reçus rejoués après expiration, reprise après bail, résultats/outbox, arrêt, remplacement, effacement, RPC direct et pagination. [Rapport](local-integration.json).
- **43 tests unitaires demo et 22 remote**, avec une partie commune : transport HTTP, DTO, ViewModels, OTP/session, rotation coordonnée, déconnexion, commande pending après recréation, réessai authentifié avec le même corps et refus persistant après rafraîchissement. Ces tests HTTP contrôlés ne sont pas une preuve du backend réel ; la ligne précédente couvre celui-ci localement.
- **5 parcours Compose demo** et **1 parcours connecté exécuté en FR et EN** sur téléphone émulé API36, sombre et police 1,3 : code réellement reçu, connexion, recréation, fichier chiffré et refus honnête de surveillance. Des premiers passages ont échoué : course sur rappel identique du champ, attente après déconnexion et actions pendant la fermeture du clavier. Correctif métier et préparation explicite du clavier ajoutés ; les délais/critères n'ont pas été élargis pour faire réussir la démo.
- **Arrêt réel du processus Android** avec une commande pending injectée localement : même identifiant, session relue depuis Keystore, suivi en préparation et aucune commande supplémentaire. Ce n'est pas une activation fournisseur. [Rapport](android-process-restoration.json).
- **6 tests TypeScript du transport FCM** : désactivation sans appel, arrêt avant autorisation, identifiants seuls et TTL borné, acceptation distincte de livraison, réponse perdue sans double POST, erreurs permanentes/temporaires. Aucun appel Google réel.
- **75 tests hors ligne du laboratoire existant**, sans nouvel essai fournisseur. [Portée](lab-regression.json).
- Contrat **15 opérations, 23 schémas, 34 exemples** (21 positifs, 13 négatifs), snapshot Edge identique ; contrôle des frontières ; compilation des deux variantes ; Lint sans erreur ni avertissement. Les indications de versions plus récentes sont informatives ; pas de mise à jour gratuite des dépendances existantes.

Les captures sous `android-phone/` proviennent de l'émulateur. Ce passage ne remplace pas les contrôles API23, tablette, téléphone physique, TalkBack, qualité de livraison ni observation S4. Android Studio n'est pas annoncé ouvert/synchronisé par ce lot.

## Budget et état final

**Dépense fournisseur supplémentaire : 0 €. Aucun nouvel appel OpenAI, Exa ou Firecrawl.** Plafond maintenu à **2 € cumulés** ; pas d'abonnement, recharge ou passage payant. Historique inchangé : OpenAI estimé 0,0476092 USD ; Exa affiché 0,02 USD arrondis ; 15 crédits gratuits Firecrawl consommés, dernier solde connu 985 le 15 octobre. Ce solde n'a pas été recontrôlé puisqu'aucun nouvel usage n'a été lancé. [Registre](costs.json).

À la fin, les surveillances et commandes de fixture sont retirées ; capacité réelle zéro et toutes les méthodes désactivées. FCM non configuré et désactivé. Aucun cron fournisseur, aucun push, aucune publication et aucun push Git. Seule la purge locale de rétention est planifiée. [État contrôlé](local-safety.json).

## Reprise concrète

1. Qualifier l'admission et le moteur avec les cas A1/REV et références indépendantes, sans changer les seuils. Avant tout nouvel appel, vérifier les soldes et réserver une borne dans le reliquat historique de 2 € ; pas de nouvelle réserve ouverte par ce bilan.
2. Terminer le raccordement fournisseur, son rapprochement authentifié, la récurrence/coût bornés et la preuve d'arrêt réel. Achever le mapping des questions/pending et la purge complète du compte.
3. Au moment des preuves externes, créer/configurer les projets **gratuits dédiés** Supabase (région UE) et Firebase Spark, puis fournir la configuration dans les fichiers locaux appropriés. Aucun secret à coller dans la conversation. Ne pas acheter de domaine prématurément ; la délivrabilité publique des codes reste une condition distincte.
4. Prouver FCM et la chaîne avec les cas de permission, session expirée, suppression, doublons et panne. Seulement alors déclencher l'observation S4 et ses références attendues.

Guide reproductible : [service](../../service/README.md), [Android](../../android/README.md). Le [plan 10](../../docs/10-plan-de-validation-technique.md) reste ouvert conformément à ces preuves.
