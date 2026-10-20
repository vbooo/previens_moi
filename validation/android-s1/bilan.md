# Sprint 1 — fondations Android

**Livré localement le 20 octobre 2026.** Prochain ticket : S2-01 (modèles, erreurs, repository demo et tests de contrat). Aucun arbitrage produit supplémentaire requis pour ce travail.

## Ce qui fonctionne

- Android Studio Rabbit1 installé dans `~/Applications/Android Studio.app` et processus lancé. Contrôle visuel de sa fenêtre impossible après expiration de l’outil ; aucune synchronisation de projet dans l’IDE n’est déclarée vérifiée.
- Plateforme de compilation 37.0, JDK 17, wrapper Gradle 9.6.0 contrôlé ; émulateur `medium_phone` API 36 ARM64 démarré.
- APK demo installé et ouvert en français. Deux destinations typées, Hilt, ViewModel/StateFlow, repository simulé, ressources FR/EN et thème clair/sombre.
- Compilation réussie ; **2 tests unitaires + 1 test sur émulateur, zéro échec**. Le test sur appareil couvre chargement, détail, recréation d’Activity et retour système avec conservation de la liste.
- Lint : **zéro erreur, zéro avertissement**. Six indications de versions plus récentes restent visibles dans le rapport ; elles sont informatives pour préserver la matrice compatible retenue.
- Contrôle des imports du domaine/de la présentation et du manifeste fusionné réussi. Inspection des permissions de l’APK final : **aucun INTERNET**. Pas de clé fournisseur dans le code Android, pas de variante release demo.
- Git local initialisé, outils, `.env`, laboratoire et preuves brutes exclus. Workflow CI écrit mais **aucun run distant**, aucun remote/push.

## Traçabilité

Révision des sources testées : `c4c15ac5959fb493ccd9aed383e8494d62da6f61`.

APK : `android/app/build/outputs/apk/demo/debug/app-demo-debug.apk` (généré, non suivi dans Git).

SHA-256 : `15f15f104585da9cc1f80e3d1dc8f629497daa172146cb10a73c6ca39df277ea`.

[Résultats structurés](resultats.json) · [Capture FR](demo-fr.png) · [Guide développeur](../../android/README.md) · [Plan](../../docs/10-plan-de-validation-technique.md).

Les rapports complets sont dans `android/app/build/reports/` ; le journal local est `.local/s1-build.log`. Les chemins SDK et caches sont exclus de Git. L’empreinte identifie cet APK précis ; une reconstruction peut produire un binaire différent avec une autre clé de debug.

## Corrections et décisions vérifiées

L’identifiant SDK était `android-37.0`, pas `android-37`. Navigation 3 1.2.0 demandait API 24 : version stable 1.1.0 retenue pour maintenir API 23. Aucun contournement `overrideLibrary`. Icône provisoire compatible API 23 et règles explicites de sauvegarde ajoutées après Lint. API de test Compose v2 utilisée pour éviter l’ancienne API dépréciée.

L’installation des outils est indépendante du budget fournisseur : **zéro nouvel appel Exa/Firecrawl/OpenAI, aucun abonnement souscrit**.

## Limites et prochaine action

Ce sprint prouve les fondations Android, pas la fiabilité d’une alerte réelle. Le contrat S1 ne contient que lecture/rafraîchissement : les commandes idempotentes, expirations, quotas et conflits sont l’objet de **S2-01**, avec des scénarios déterministes. Ensuite viendront saisie → clarification → proposition → activation simulée, puis gestion des suivis et résultats fictifs.

API 23/37 en exécution, mort du processus, persistance durable, grand texte/tablettes, accessibilité complète, serveur, authentification et notifications ne sont pas déclarés validés. Aucun SDK de recherche ni travail de fond réseau n’a été ajouté à Android. Le moteur réel reste au sprint 3 et la publication au sprint 4.
