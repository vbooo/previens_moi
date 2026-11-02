# Préviens-moi

Application Android native pour demander à être prévenu d’une information publique : une offre d’emploi, une promotion ou une annonce, par exemple. L’objectif prioritaire est une réalisation propre que Fabien peut comprendre, démontrer et défendre en entretien Android.

## État au 2 novembre 2026

Le socle Android, le service réel et le parcours « comprendre → confirmer → préparer en arrière-plan » sont livrés pour un essai limité. Une première interface Material 3 est intégrée. La stabilisation est terminée. La version connectée **0.1.3** conserve la synchronisation automatique et les notifications de résultat de 0.1.2 ; elle allège les transitions et rend une reprise acceptée annulable. [Le bilan courant](validation/pause-resume/bilan.md) distingue les vérifications locales, les délais serveur et le test restant sur OnePlus.

**La V1 est une version de test connectée ; aucune publication Play ni observation de sept jours n’est revendiquée.** Les anciens bilans datés conservent leurs résultats et limites, mais leurs anciens états « prochaine étape » ne décrivent pas la livraison courante.

## Ce que fait l’application

1. La personne décrit ce qu’elle souhaite suivre et peut proposer un site.
2. OpenAI reformule les critères ou demande une précision. Aucune recherche Web ne précède la confirmation.
3. Après confirmation, le serveur cherche des sources avec Firecrawl, lit les pages sélectionnées et vérifie qu’elles permettent le suivi.
4. L’application présente la préparation, le suivi actif ou un échec explicite. La préparation et les vérifications continuent côté serveur quand l’app est fermée.
5. Pendant que l’app est visible, ses états et résultats sont relus automatiquement ; la mise à jour ne dépend pas du push. Les résultats comportent une source et un passage justificatif. Les alertes utilisent Firebase Cloud Messaging et ouvrent le contenu concerné.
6. La personne peut mettre en pause, reprendre, modifier ou supprimer son suivi et demander l’effacement de son compte.

Pour les nouveaux suivis généraux, la politique actuelle retient jusqu’à trois pages publiques, relues environ toutes les heures ; la recherche de nouvelles sources est renouvelée tous les 24 passages. Les anciens plans conservent leurs paramètres confirmés. La couverture reste partielle : pages protégées, sources non trouvées et informations non prouvables peuvent empêcher un suivi. Les informations déjà disponibles au démarrage sont distinguées des nouveautés ; un suivi ponctuel peut donc se terminer dès son premier résultat initial.

## Architecture

| Partie | Responsabilité |
| --- | --- |
| Android : Kotlin, Jetpack Compose et Material 3 | Écrans, navigation, permissions, état des parcours et accès aux résultats. |
| MVVM et dépendances dirigées vers le domaine | Compose appelle les ViewModels ; ceux-ci utilisent les ports des repositories. Hilt choisit les implémentations. Un seul module Gradle reste volontaire. |
| Supabase : Auth, PostgreSQL et fonctions serveur | Connexion par code e-mail, données privées, commandes identifiées/versionnées, préparation, planification, quotas et historique. |
| OpenAI / Firecrawl | Compréhension et jugement avec références aux passages / recherche et lecture des pages publiques. |
| Firebase Cloud Messaging | Transport des notifications Android ; les résultats sont relus auprès du service authentifié. |

Une opération incertaine conserve son identifiant : réessayer vérifie la même commande et évite un deuxième effet. Le statut affiché vient du service ; un échec confirmé est distinct d’une réponse réseau encore inconnue. Les secrets serveur, la surveillance et les plafonds restent hors de l’APK.

## Lancer l’application

Ouvrir **`android/`** dans Android Studio et sélectionner **`remoteDebug`** pour essayer le service réel. Le nom visible est **Préviens-moi**. La variante **`demoDebug`**, nommée **Préviens-moi · Simulation**, est un simulateur hors réseau avec des scénarios déterministes ; elle sert aux tests.

Les instructions de configuration, compilation et lecture du code sont dans [android/README.md](android/README.md). Les paramètres publics du client passent par `android/client.properties`, ignoré par Git ; voir son modèle `.example`. Les clés serveur et mots de passe ne doivent jamais y figurer.

## Contrôles reproductibles

Depuis `android/`, les vérifications de compilation, tests unitaires et Lint peuvent être exécutées sans configuration cloud :

```sh
./gradlew assembleDemoDebug assembleRemoteDebug \
  testDemoDebugUnitTest testRemoteDebugUnitTest \
  lintDemoDebug lintRemoteDebug -PlocalService=true
```

Depuis la racine :

```sh
python3 scripts/check-android-boundaries.py --merged
cd service
./node_modules/.bin/deno task check
./node_modules/.bin/deno task test
```

`localService=true` isole le build des paramètres cloud locaux ; ce n’est pas l’APK à installer pour tester les vraies recherches. Les tests du service injectent leurs réponses et ne déclenchent aucun appel fournisseur payant. Les parcours natifs utilisent un émulateur de qualification et des fixtures identifiées. Les commandes détaillées sont dans [le guide Android](android/README.md).

La CI Android est configurée pour vérifier les deux variantes. Le dépôt reste local, sans remote GitHub : aucune exécution de CI distante ni aucun push n’est revendiqué.

## Suite du plan

- Tester l’APK stabilisé sur le OnePlus et corriger les problèmes fonctionnels constatés.
- Finaliser l’UI/UX à partir des écrans déjà validés et des retours d’usage.
- Organiser les sept jours d’observation dans une enveloppe compatible avec le budget disponible.
- Préparer les contrôles de diffusion, la confidentialité, la sauvegarde indépendante, la signature et le compte Play, puis la publication.
- Créer le dépôt GitHub quand Fabien le décide ; le benchmark et la modularisation restent différés.

Le plafond des essais reste **2 € cumulés**, sans abonnement ni recharge implicite. Il ne définit pas un budget d’exploitation publique. La compilation release reste désactivée jusqu’au lot de diffusion.

## Documents utiles

- [Plan actif et historique des étapes](docs/10-plan-de-validation-technique.md)
- [Parcours comprendre / confirmer / préparer](docs/12-comprendre-puis-preparer.md)
- [Périmètre du MVP](docs/03-perimetre-mvp.md) et [règles des alertes](docs/05-regles-des-alertes.md)
- [Architecture et données](docs/08-architecture-et-donnees.md)
- [Contrat réseau](contracts/application-v1.openapi.json) et [service](service/README.md)
- [Qualité et publication](docs/09-qualite-et-publication.md)
- [Bilan S3](validation/s3/bilan.md), [bilan du nouveau parcours](validation/workflow/bilan.md), [corrections de préparation](validation/preparation/checks.json)
- [Décisions d’architecture et produit](docs/decisions/README.md)
