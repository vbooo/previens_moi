# ADR 0011 — service réel du premier pilote

Date : 20 octobre 2026. **Décision technique pour l'implémentation locale ; déploiement et exploitation non autorisés par ce document.** Fabien a autorisé le démarrage de S3. Ce choix réversible concrétise ARCH-D01/D03/D05 ; il ne modifie ni la saisie libre, ni la prévisualisation sans compte, ni le plafond d'essai de 2 € cumulés.

## Problème et choix

S2 fournit des ports Android et un service simulé. S3 doit substituer un service durable, isoler les comptes, recevoir les résultats et envoyer des notifications sans dépendre du téléphone. Les limites du moteur restent celles des essais F0–F3 : choisir un hébergeur ne qualifie pas la compréhension libre ou la découverte.

Retenir **Supabase : PostgreSQL, Auth par code e-mail, Edge Functions TypeScript/Deno et planification PostgreSQL**, puis **Firebase Cloud Messaging (FCM), projet Spark distinct, pour le transport push**. Android garde Kotlin, MVVM/Clean, Retrofit/OkHttp et DTO séparés. Aucun SDK de base de données dans les écrans. Migrations SQL et fonctions restent dans ce dépôt sous `service/` lors de S3-02. Les versions exactes des outils seront verrouillées et compilées à ce ticket ; aucun runtime backend n'est encore installé par cette décision.

Le code par e-mail applique le choix d'identification déjà validé. Les e-mails servent à la connexion, **pas à envoyer les alertes du produit**. Prévoir Resend gratuit comme transport de ces codes via le mécanisme d'envoi d'Auth. Un domaine d'expédition vérifié est nécessaire pour cette voie ; aucun domaine disponible n'est présumé et aucun achat n'est lancé. La configuration réelle reste ouverte. Un serveur mail local de test suffit pour commencer.

## Comparaison bornée

| Option | Intérêt | Limite déterminante / conclusion |
| --- | --- | --- |
| Supabase + FCM | Transactions SQL pour quotas, reçus et résultats ; identité et fonctions gérées ; développement local ; FCM natif Android | Envoi e-mail à configurer, sauvegarde du gratuit à organiser, pause possible d'un projet gratuit inactif. **Retenu pour l'intégration testée**, pas SLA de production. |
| Firebase Auth/Firestore/Functions/FCM | Ensemble Google intégré et clients Android natifs | Functions nécessite Blaze ; facturation variable et garde-fous à traiter dès l'essai. Les spend caps récents ne sont pas des plafonds stricts. Pas retenu pour notre première étape sans facturation. |
| Cloudflare Workers/D1 + fournisseur d'identité + FCM | Fonctions peu coûteuses, hébergement géré | Identité à assembler séparément, autre modèle de transactions et portabilité à qualifier. Aucun gain démontré pour ajouter ce travail maintenant. |
| Ktor + PostgreSQL sur serveur à administrer | Backend Kotlin et contrôle fin | Hébergement, mises à jour, identité, sauvegardes et exploitation à assembler ; éloigne du livrable Android. Reporté. |

Ce choix ne vaut pas interdiction d'une migration : ports métier, contrat HTTP et PostgreSQL réduisent le couplage. Auth, fonctions et déploiement restent spécifiques au prestataire ; une migration ne serait pas gratuite.

## Responsabilités et limites

Le fournisseur gère infrastructure, moteur SQL et identité. **Nous écrivons** autorisations métier, transactions, idempotence, jobs durables, limites financières, rapprochement fournisseur, suppression et intention d'envoi. Pas d'authentification maison, pas de navigateur automatisé ni de boucle IA générale.

Les fonctions reçoivent des tâches courtes. Les travaux réseau longs sont découpés et persistés ; une promesse JavaScript en arrière-plan ne remplace pas une file durable. PostgreSQL sert à la file initiale et à ses verrous, sans ajouter Redis ou un orchestrateur commercial.

Firecrawl reste le candidat de surveillance de page connue étayé par F1–F3, **pas un moteur universel adopté**. Exa reste une piste de découverte. Toute méthode est désactivée tant que ses critères S3-03 et son budget ne passent pas. L'offre publique, les quotas et la cadence proposés restent à arbitrer avant ouverture.

## Conséquences et conditions de passage

Le [contrat S3](../11-service-et-contrat-s3.md), l'[OpenAPI](../../contracts/application-v1.openapi.json) et les cas d'acceptation précèdent l'implémentation. L'intégration locale peut avancer sans compte distant. Avant connexion réelle : projet Supabase dédié gratuit, projet Firebase Spark, accès nécessaires et mécanisme de codes e-mail configuré. Avant activation récurrente : capacité financée, arrêt distant démontré, seuils de qualité atteints. Avant publication : sauvegarde/restauration, observation, données et exigences Play de S4.

Aucun passage automatique à Supabase Pro, Firebase Blaze, Firecrawl payant ou Resend payant. Si la voie gratuite ne satisfait pas les conditions, présenter le prix ou le compromis à Fabien ; ne pas changer silencieusement le mode de connexion ni publier une surveillance peu fiable.

## Références officielles consultées le 20 octobre 2026

- [Supabase tarifs](https://supabase.com/pricing) : gratuit avec quotas, pause après une semaine d'inactivité, pas de sauvegardes automatiques ; Pro à partir de 25 USD/mois.
- [Auth e-mail](https://supabase.com/docs/guides/auth/auth-email-passwordless) et [transport SMTP](https://supabase.com/docs/guides/auth/auth-smtp) : codes gérés ; transport de démonstration restreint aux adresses d'équipe, deux messages/heure au moment de la lecture.
- [Limites des fonctions](https://supabase.com/docs/guides/functions/limits), [planification](https://supabase.com/docs/guides/functions/schedule-functions), [sauvegardes](https://supabase.com/docs/guides/platform/backups).
- [Firebase tarifs](https://firebase.google.com/pricing), [spend caps](https://firebase.google.com/docs/projects/billing/spend-caps) : FCM sans coût de transport ; arrêt des services concernés potentiellement retardé, dépassement facturé.
- [Cloudflare Workers tarifs](https://developers.cloudflare.com/workers/platform/pricing/).
- [Resend tarifs](https://resend.com/pricing), [vérification du domaine](https://resend.com/docs/dashboard/domains/introduction).
