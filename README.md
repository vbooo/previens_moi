# Préviens-moi

Projet Android de surveillance personnelle. Nom provisoire. Objectif prioritaire : remise à niveau Android native de Fabien et réalisation défendable en entretien, avec une dépense limitée.

## État du projet — 21 octobre 2026

S1 et S2 livrés localement. Supabase et Firebase dédiés raccordés, connexion par code e-mail OVH et restauration de session vérifiées. Le premier pilote de changements de texte d’une URL publique fonctionne de bout en bout : proposition explicite, état initial silencieux, changement réel sourcé, notification FCM Android et arrêt distant confirmé. Le faux message d’erreur à la déconnexion est corrigé en FR/EN.

**S3 reste ouvert : la surveillance générale prévue pour la V1 n’est pas qualifiée.** Le pilote accepte uniquement `Préviens-moi de tout changement sur https://…` ou `Notify me of any change on https://…`, avec une portée partielle annoncée. Un emplacement et jusqu’à 12 crédits gratuits supplémentaires, contrôles horaires et planificateur cloud actif. Le test de détection utilise une page fictive réellement modifiée ; son second contrôle a été avancé. Pas d’observation S4 de sept jours ni de publication Play. [Bilan et limites](validation/s3/bilan.md) · [Preuves](validation/s3/page-change-pilot.json).

57 tests Deno, 31 intégrations locales, tests unitaires des deux variantes et déconnexion Android FR/EN passent. Aucun nouvel appel IA payant ; borne conservatrice cumulée **1,9414335 € / 2 €**, solde 981 crédits Firecrawl gratuits avant l’essai utilisateur. Aucun abonnement/recharge. Dépôt local, sans remote ni push.

## Documents par sujet

- [Produit et positionnement](docs/01-produit-et-positionnement.md)
- [Concurrence et différenciation](docs/02-concurrence-et-differenciation.md)
- [Périmètre du MVP](docs/03-perimetre-mvp.md)
- [Parcours et écrans](docs/04-parcours-et-ecrans.md)
- [Règles des alertes](docs/05-regles-des-alertes.md)
- [Sources et stratégie de recherche](docs/06-sources-et-strategie-de-recherche.md)
- [Coûts et modèle économique](docs/07-couts-et-modele-economique.md)
- [Architecture et données](docs/08-architecture-et-donnees.md)
- [Qualité et publication](docs/09-qualite-et-publication.md)
- [Plan de validation technique](docs/10-plan-de-validation-technique.md)
- [Service et contrat S3](docs/11-service-et-contrat-s3.md)
- [Alternatives au moteur Yutori — comparaison documentaire](validation/02-alternatives-au-moteur-yutori.md)
- [Protocole comparatif Exa, Firecrawl et Yutori](validation/03-protocole-comparatif-des-moteurs.md)
- [Matrice de faisabilité générale et de coûts](validation/04-matrice-de-faisabilite-et-couts.md)
- [Jeu d'essai transversal v0](validation/05-jeu-essai-transversal-v0.md)
- [Plan de construction du premier moteur](validation/06-plan-premier-moteur.md)
- [Critères et cas d’acceptation du premier moteur](validation/07-acceptation-premier-moteur.md)
- [Fiche d’exécution du moteur](validation/08-fiche-execution-moteur.md)
- [Bilan intermédiaire du moteur](validation/09-bilan-premier-moteur.md)
- [Laboratoire exécutable](engine-lab/README.md)
- [Historique des décisions](docs/decisions/README.md)

## Cadre conservé

France prioritaire, saisie libre, événements précis et suivis généraux, refus honnête des demandes non surveillables. MVVM et Clean Architecture, un seul module Gradle initial, service central pour les secrets et la surveillance. Les capacités et quotas de la V1 publique gratuite doivent être démontrés et chiffrés. La publication exige notamment les contrôles du document 09 et sept jours d'observation. Le retour Google sur la régularisation du compte Play est attendu selon la dernière information de Fabien.

## Livraison actuelle et prochaine étape

La démo S2 reste disponible hors réseau. La variante Android remote utilise le compte e-mail et le pilote textuel explicite ; le service cloud poursuit ses vérifications sans l’application ouverte, dans son enveloppe limitée. Les changements de texte ne prouvent pas la compréhension d’un événement ni la couverture d’un sujet général.

La prochaine étape produit reste la qualification de la surveillance générale du périmètre initial, puis l’observation S4 et la préparation de publication. Les [preuves S3](validation/s3/bilan.md) distinguent les tests locaux, les anciens résultats injectés et la nouvelle source de contrôle réelle. GitHub et la régularisation du compte Play restent à traiter avec Fabien.
