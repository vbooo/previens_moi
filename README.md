# Préviens-moi

Projet Android de surveillance personnelle. Nom provisoire. Objectif prioritaire : remise à niveau Android native de Fabien et réalisation défendable en entretien, avec une dépense limitée.

## État du projet — 20 octobre 2026

Documents produit 01 à 09 validés. **Revue MVP et coûts livrée : [périmètre](docs/03-perimetre-mvp.md), [budget](docs/07-couts-et-modele-economique.md). **Sprint 1 livré localement : APK demo compilé et lancé, 2 tests unitaires + 1 test émulateur réussis, Lint sans erreur/avertissement. Prochain ticket S2-01**, selon le [plan 10](docs/10-plan-de-validation-technique.md).** Exa D0 et Firecrawl F0 ont montré des limites de format et de récence ; Firecrawl F1 passe les quatre cas d’une page fictive connue. **F2 : 13 tests locaux passent** pour notre journal, ses doublons, pauses et preuves. **F3 : quatre messages Firecrawl signés reçus, vérifiés localement et rapprochés du journal sur deux passages réels.** Récepteur public de production et push Android non validés ; socle Android dans [android/](android/README.md). Ne pas confondre passages manuels des monitors, rejeux locaux et surveillance autonome dans la durée.

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

## Prochaine étape — sprint 2, parcours produit simulé

**S1-01 à S1-06 terminés localement**, preuves dans le [bilan du sprint 1](validation/android-s1/bilan.md). Android Studio est installé et lancé ; le contrôle visuel de sa fenêtre n’a pas abouti via l’outil. SDK/Gradle/compilation, émulateur et application sont vérifiés en ligne de commande. Aucun projet ouvert/synchronisé dans l’IDE n’est prétendu par ce bilan.

Le [guide Android](android/README.md) explique comment ouvrir le projet, compiler, tester et lire l’architecture. La [capture en français](validation/android-s1/demo-fr.png) montre l’écran minimal. Le dépôt Git est local ; aucun dépôt distant, CI distante, abonnement ou backend créé.

Prochain ticket : **S2-01 du plan 10**. Construire le parcours simulé prévu dans les documents 03/04/08, avec scénarios explicites et tests d’acceptation. Le moteur réel vient en S3 ; les interfaces actuelles ne prétendent pas comprendre une demande libre ni surveiller le Web. Les [spécifications](docs/08-architecture-et-donnees.md) et le [backlog](docs/10-plan-de-validation-technique.md) restent les références. Aucun nouvel arbitrage n’est nécessaire pour commencer S2.
