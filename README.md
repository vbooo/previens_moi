# Préviens-moi

Projet Android de surveillance personnelle. Nom provisoire. Objectif prioritaire : remise à niveau Android native de Fabien et réalisation défendable en entretien, avec une dépense limitée.

## État du projet — 20 octobre 2026

Documents produit 01 à 09 validés. **Revue MVP et coûts livrée : [périmètre](docs/03-perimetre-mvp.md), [budget](docs/07-couts-et-modele-economique.md). Sprints 1 et 2 livrés localement : parcours demo complet, 24 tests unitaires et 5 parcours Android sur trois configurations réussis, restauration après mort du processus vérifiée, Lint sans erreur/avertissement. [Bilan S2](validation/android-s2/bilan.md). S3 démarré : [spécification du service](docs/11-service-et-contrat-s3.md), contrat et client HTTP préparés, selon le [plan 10](docs/10-plan-de-validation-technique.md).** Exa D0 et Firecrawl F0 ont montré des limites de format et de récence ; Firecrawl F1 passe les quatre cas d’une page fictive connue. **F2 : 13 tests locaux passent** pour notre journal, ses doublons, pauses et preuves. **F3 : quatre messages Firecrawl signés reçus, vérifiés localement et rapprochés du journal sur deux passages réels.** Récepteur public de production et push Android non validés ; socle Android dans [android/](android/README.md). Ne pas confondre passages manuels des monitors, rejeux locaux et surveillance autonome dans la durée.

Plafond autorisé : **2 € cumulés**. Estimation historique IA : 0,0476092 USD ; diminution Exa affichée : 0,02 USD arrondie ; Firecrawl F0+F1+F3 : 15 crédits gratuits, solde final mesuré 985. F2 : aucun appel fournisseur et aucune dépense. F3 clos : suivi en pause, aucune exécution courante, boîte temporaire supprimée. Aucun abonnement ni recharge ; les 50 € du cadrage initial ne sont pas une autorisation de dépense.

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

**S2-01 à S2-06 terminés localement**, preuves dans le [bilan du sprint 2](validation/android-s2/bilan.md). Création, clarification, proposition, compte fictif, gestion des suivis et résultats sourcés fictifs sont démontrables. Seul le brouillon est conservé durablement ; aucun moteur réel ni notification push n’est livré. S2 n’a consommé aucun crédit fournisseur.

Le [guide Android](android/README.md) explique comment ouvrir le projet, compiler, tester et lire l’architecture. Android Studio 2026.2 est installé ; son assistant initial reste à terminer si affiché. SDK/Gradle/compilation et émulateurs sont vérifiés en ligne de commande. Aucun projet ouvert/synchronisé dans l’IDE n’est prétendu par ce bilan. Le dépôt Git reste local, sans push ni CI distante exécutée.

**S3-01 en cours :** choix techniques et contrat préparés, coût complet d’ouverture encore conditionnel. Le [bilan de démarrage S3](validation/s3-01/bilan.md) distingue les preuves locales et le travail restant. Prochain lot local : migrations PostgreSQL, API, Auth et isolation des comptes (S3-02), avant raccordement distant. Les [spécifications](docs/08-architecture-et-donnees.md) et le [backlog](docs/10-plan-de-validation-technique.md) restent les références. Toute dépense nouvelle ou réduction de promesse nécessite un arbitrage explicite.
