# Android — Préviens-moi

Application native de démonstration des sprints 1 et 2. Parcours de création, clarification, proposition, identification fictive, gestion des suivis et consultation des résultats. **Aucun texte n’est interprété par une IA, aucun site surveillé, aucune notification envoyée.** Les scénarios sont choisis explicitement dans le menu « Scénarios ».

## Ouvrir et compiler

Ouvrir le dossier `android/` dans Android Studio et choisir `demoDebug`. Installer la plateforme Android 37.0 et Build Tools 36.0.0 ; sélectionner un JDK 17 pour Gradle. `local.properties` porte le chemin SDK propre au poste, exclu de Git.

```sh
./gradlew assembleDemoDebug testDemoDebugUnitTest lintDemoDebug
python3 ../scripts/check-android-boundaries.py --merged
# Avec un émulateur ou téléphone connecté :
./gradlew connectedDemoDebugAndroidTest
```

Sur le poste de Fabien : outils dans `../.local/`, JDK `jdk/Contents/Home`, SDK `sdk/`, cache Gradle `gradle-home/`. Définir `JAVA_HOME` et `GRADLE_USER_HOME` vers ces chemins hors IDE. Android Studio est dans `~/Applications/Android Studio.app`. Ces chemins ne sont pas une dépendance de la CI. Le lancement de l’IDE est acquis en S1, pas la vérification visuelle de sa synchronisation.

## Essayer le parcours

1. Saisir une demande ; éventuellement activer le suivi dans la durée. « Prévisualiser » affiche une proposition synthétique contenant le texte saisi.
2. Confirmer, utiliser le compte fictif, puis confirmer à nouveau. L’identification ne déclenche aucune activation automatique.
3. Dans le détail, mettre en pause, reprendre ou modifier les critères. Supprimer demande confirmation.
4. Menu « Scénarios » → « Simuler une vérification » : fabriquer un résultat fictif. Le mode ponctuel se termine au premier résultat ; le mode continu reste actif.
5. Tester « Précision nécessaire », « Portée réduite », « Aucun résultat », « Source en panne », « Capacité épuisée », « Réponse perdue après succès » et les autres erreurs. Changer de scénario puis réessayer l’action permet de tester la récupération.

La capacité de 1 et l’intervalle de 24 h sont des paramètres de fixture. Aucun minuteur ne tourne ; ces chiffres ne fixent pas l’offre commerciale. Le brouillon est enregistré par DataStore. Les comptes, propositions, suivis et résultats vivent uniquement dans la mémoire du service demo : un arrêt du processus les efface. L’interface explique leur indisponibilité, sans prétendre qu’ils continuent à surveiller le Web.

## Lire le code dans cet ordre

| Rôle | Fichier depuis ce dossier |
| --- | --- |
| Modèles et erreurs métier, sans Android | `app/src/main/kotlin/fr/previensmoi/domain/model/Watch.kt` |
| Ports du domaine | `app/src/main/kotlin/fr/previensmoi/domain/repository/WatchRepository.kt` |
| Service simulé : commandes, versions, reçus, quota atomique | `app/src/demo/kotlin/fr/previensmoi/data/demo/DemoService.kt` |
| Persistance locale du brouillon | `app/src/main/kotlin/fr/previensmoi/data/local/LocalDraftRepository.kt` |
| État et actions de création | `app/src/main/kotlin/fr/previensmoi/feature/create/presentation/CreateViewModel.kt` |
| Saisie et proposition Compose | `app/src/main/kotlin/fr/previensmoi/feature/create/presentation/CreateScreen.kt` |
| Liste, détail et résultat | `app/src/main/kotlin/fr/previensmoi/feature/watches/presentation/WatchScreens.kt` |
| Actions sur les suivis | `app/src/main/kotlin/fr/previensmoi/feature/watches/presentation/WatchesViewModel.kt` |
| Hilt et assemblage demo | `app/src/demo/kotlin/fr/previensmoi/app/DemoModule.kt` |
| Navigation typée et entrée d’application | `app/src/demo/kotlin/fr/previensmoi/app/MainActivity.kt` |

**Changement de chemin en S2 :** `MainActivity` est maintenant dans `src/demo`, afin que l’assemblage et ses commandes de simulation restent isolés de la future variante réelle.

## Expliquer l’architecture en entretien

« J’utilise MVVM pour les écrans et des dépendances orientées vers le domaine. Une action Compose appelle le ViewModel, qui utilise un port de repository. Hilt fournit l’implémentation. Le repository possède l’état confirmé ; le ViewModel l’expose dans un `StateFlow` immuable, collecté par Compose avec le cycle de vie. La navigation repose sur des clés sérialisables Navigation 3. »

Exemple concret : toucher « Mettre en pause » lance une commande identifiée et versionnée. L’écran montre son traitement, mais ne change pas arbitrairement le statut. Seul l’état confirmé par le service modifie la liste et le détail. En cas de réponse perdue, une nouvelle tentative réutilise la même commande : le reçu évite un deuxième effet. **Ces garanties sont éprouvées sur un double de service, et devront être imposées au serveur réel en S3.**

Le domaine est du Kotlin indépendant d’Android et de Hilt. Présentation et data dépendent du domaine ; l’assemblage connaît les implémentations. Un seul module Gradle suffit à ce stade ; pas de couches ou de cas d’usage qui ne font que transmettre un appel. Le contrôle d’imports protège ces frontières.

`StateFlow` ne survit pas seul à la mort du processus. DataStore conserve le brouillon sur disque ; `SavedStateHandle` conserve de petits identifiants pour restaurer le parcours. Le repository demo, en mémoire, ne survit pas : l’UI invalide alors l’ancienne proposition. Une réponse d’évaluation tardive est rejetée si la demande a changé ou si l’évaluation a été abandonnée.

## Tests et preuves

- Tests de contrat réutilisables via les ports : expiration exacte, idempotence, conflit de commande, dernière place concurrente, session, suppression, source en panne, reprise et version des résultats.
- Tests de ViewModels : état confirmé conservé en erreur, double clic, réponse tardive, consentement explicite, identification sans activation, persistance défaillante et restauration.
- Cinq tests Compose : création, clarification/portée partielle, navigation/recréation, mutations et suppression, résultat daté honnêtement, panne distincte d’absence de résultat.
- Test réel de mort du processus : `../scripts/check-android-process-restoration.py`. Il **efface les données de l’application demo du périphérique ciblé** avant de préparer un brouillon, envoyer l’application en arrière-plan, tuer son processus et relancer la tâche. À réserver à un émulateur de test.

```sh
python3 ../scripts/check-android-process-restoration.py \
  --serial emulator-5554 --adb ../.local/sdk/platform-tools/adb \
  --output ../validation/android-s2
```

Le bilan daté et la matrice d’exécution sont dans [validation/android-s2](../validation/android-s2/bilan.md). Les rapports complets Gradle sont dans `app/build/reports/`, générés localement. Les tests existants servent de non-régression ; ce terme désigne leur réexécution après changement, pas une bibliothèque supplémentaire. Les captures contrôlées ne sont pas des screenshot tests automatiques de comparaison pixel à pixel.

## Versions et limites

AGP 9.4.1, Gradle 9.6.0, Kotlin/Compose compiler/sérialisation 2.3.21, KSP 2.3.12, Hilt 2.60.1. Compose BOM 2026.09.00, Activity 1.13.0, Lifecycle 2.11.0, Navigation 3 **1.1.0**, coroutines 1.11.0, DataStore Preferences **1.2.1**. Catalogue : `gradle/libs.versions.toml`.

Navigation 3 1.2.0 exige API 24 ; la version stable 1.1.0 conserve notre minimum API 23. Compilation/cible 37. API 23 et 37 en exécution restent à tester avant publication. AGP fournit Kotlin intégré ; aucun plugin `org.jetbrains.kotlin.android` supplémentaire. Le wrapper contrôle le SHA-256 de Gradle. Aucun artefact release demo.

Lint traite les avertissements comme erreurs ; six suggestions de mises à jour restent informatives pour préserver la matrice compatible. Le manifeste fusionné et l’APK **demo** doivent rester sans permission INTERNET. Aucun secret dans Android. La CI GitHub Actions compile, teste le domaine/ViewModels et exécute Lint et le contrôle de frontières ; ses tests sur appareil restent locaux à ce stade. **Aucun dépôt distant ni exécution de CI distante n’est prétendu.**

S3-01 a démarré : choix et spécification du service réel, identité, API et coûts ; voir la mise à jour ci-dessous. Surveillance réelle, authentification, accès entre comptes, push, performance, revue complète TalkBack et publication ne sont pas validés par cette démo.


## Contrat réseau S3

Les routes Retrofit et DTO sont assemblés aux repositories dans `remoteDebug`. La démo conserve son service simulé. Les tests de transport contrôlé ne prouvent pas un backend hébergé ; l'intégration locale Supabase/PostgreSQL et ses limites sont documentées séparément.

## Variante connectée S3 (locale, non publiable)

`remoteDebug` est séparée de `demoDebug`. Sans propriétés explicites, son URL reste `.invalid`, aucun projet Firebase n'est initialisé. `python3 scripts/check-local-android.py` construit l'app et ses tests avec seulement le service Supabase local dédié (voir `service/README.md`). La démo retire toujours INTERNET et ne contient pas Firebase/WorkManager.

Session : code e-mail réel local, jetons protégés par Android Keystore dans `noBackupFilesDir`, rotation coordonnée, effacement local même après échec de déconnexion. Les UUID de commandes incertaines sont conservés séparément et relus après recréation ; aucune commande complète avec secret n'est persistée. Une commande perdue avant envoi et absente du serveur après mort du processus reste explicitement incertaine ; son contenu n'est pas reconstruit pour créer une nouvelle activation.

FCM : SDK 25.1.3 et WorkManager 2.11.2 dans remote uniquement, données réduites aux identifiants, relecture authentifiée du résultat et du suivi, permission au choix de la personne, notification stable ouvrant le bon résultat. Projet Firebase absent : transport réel non prouvé. Les propriétés publiques `firebaseAppId`, `firebaseProjectId`, `firebaseApiKey`, `firebaseSenderId` initialisent Firebase uniquement lorsqu'elles sont toutes fournies. Aucune clé de compte de service dans l'APK.

Les tests de parcours demo sont sous `src/androidTestDemo` ; le parcours Supabase local est sous `src/androidTestRemote`. Les tests dépendant du faux service restent sous `src/testDemo`. Le [bilan S3](../validation/s3/bilan.md) sépare contrat, tests réseau contrôlés, PostgreSQL/Auth réels locaux et preuves externes encore absentes.

### Reprise et notifications après le complément S3

Les questions portent leur identifiant, leur caractère obligatoire et leurs choix. Les réponses sont conservées avec le brouillon. Une analyse en attente est consultée par polling borné ; identifiant, requête et secret de prévisualisation sont enregistrés dans `evaluation.enc`, chiffré par Keystore et exclu des sauvegardes, **avant** le POST. Après mort du processus, le même identifiant et le même corps sont repris. Secret expiré : information explicite, aucune nouvelle analyse automatique. Une erreur de stockage empêche le POST. Les explications de refus du service sont affichées.

Les notifications contrôlent aussi l'expiration FCM pendant leur attente WorkManager. Un journal d'identifiants hors sauvegarde empêche une seconde alerte après redémarrage. Il est pris avant affichage : une mort du processus à cette frontière peut perdre ce push, tandis que le résultat reste accessible dans l'application. Ne pas annoncer une livraison exactement une fois.

Validation actuelle : 49 tests unitaires demo, 27 remote (une partie commune), Lint sans erreur/avertissement ; trois parcours remote en FR et EN ; reprise réelle du processus pour commande **et** analyse. Pour les tester, construire avec `scripts/check-local-android.py`. La procédure d'instrumentation directe conserve les données nécessaires aux scripts `check-android-remote-restoration.py` et `check-android-evaluation-restoration.py`.

Raccordement dédié : `scripts/check-cloud-config.py` contrôle `.local/cloud/supabase.env`, `google-services.json` et `firebase-service-account.json` sans réseau ni affichage des valeurs. L'identité serveur reste hors d'Android.
