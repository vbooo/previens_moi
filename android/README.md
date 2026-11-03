# Android — Préviens-moi

Client natif Kotlin / Jetpack Compose / Material 3, avec MVVM et dépendances orientées vers un domaine indépendant d’Android. Un seul module Gradle. Les recherches et la surveillance durable tournent dans le service Supabase, pas dans le téléphone.

## Choisir la bonne variante

| Variante | Nom sur le téléphone | Usage |
| --- | --- | --- |
| `remoteDebug` | Préviens-moi | Service réel, connexion par code e-mail, données du compte et notifications FCM. Choix par défaut après import. |
| `demoDebug` | Préviens-moi · Simulation | Service en mémoire, scénarios et résultats fictifs, sans permission INTERNET. Tests hors réseau. |

Ouvrir ce dossier **`android/`** dans Android Studio. Vérifier **View → Tool Windows → Build Variants → app → remoteDebug**, puis lancer ▶ sur le OnePlus ou un émulateur. Studio peut conserver un ancien choix explicite de variante. Les deux identifiants d’application sont différents et les données du simulateur ne sont pas celles du compte réel.

## Paramètres et outils

Le catalogue `gradle/libs.versions.toml` et le wrapper Gradle fixent les versions. Compilation/cible Android 37, minimum API 23 ; installer la plateforme SDK correspondante et Build Tools 36.0.0. Le projet conserve la compatibilité Java 17. La matrice complète API 23/37 reste à qualifier avant publication ; le lot de stabilisation utilise un émulateur API 36.

Sur le poste de Fabien, JDK, SDK et cache Gradle sont sous `../.local/`. Hors IDE :

```sh
export JAVA_HOME="$PWD/../.local/jdk/Contents/Home"
export GRADLE_USER_HOME="$PWD/../.local/gradle-home"
```

Ces chemins sont propres au poste et ne sont pas nécessaires sur un autre environnement. `local.properties` et les préférences JVM générées par Studio doivent rester des configurations locales adaptées au poste.

Les paramètres **publics** du client réel vivent dans `client.properties`, ignoré par Git. Le modèle `client.properties.example` liste les propriétés à renseigner. Sur le poste déjà raccordé, depuis la racine du dépôt :

```sh
python3 scripts/configure-android-client.py
```

Ce script copie seulement les URL, la clé publique Auth et les identifiants Firebase du client. Aucune identité Firebase Admin, clé OpenAI/Firecrawl, clé Supabase de service ou mot de passe ne doit entrer dans les propriétés Gradle ou l’APK. Après configuration, synchroniser Gradle. Les options `-P` explicites restent prioritaires.

Sans configuration publique, la variante connectée compile avec des URL `.invalid`, mais ne peut pas se connecter. `-PlocalService=true` désactive le repli vers `client.properties` et Firebase : il sert aux builds et intégrations isolés, pas à l’essai cloud sur téléphone.

## Compiler et vérifier

Depuis ce dossier :

```sh
./gradlew assembleDemoDebug assembleRemoteDebug \
  assembleDemoDebugAndroidTest assembleRemoteDebugAndroidTest \
  testDemoDebugUnitTest testRemoteDebugUnitTest \
  lintDemoDebug lintRemoteDebug -PlocalService=true
python3 ../scripts/check-android-boundaries.py --merged
```

Pour produire ensuite l’APK réel configuré, sans l’option d’isolation :

```sh
./gradlew assembleRemoteDebug
```

APK : `app/build/outputs/apk/remote/debug/app-remote-debug.apk`. L’installation de mise à jour conserve les données si le package et la signature sont les mêmes ; ne pas désinstaller ni effacer les données pour mettre à jour. Les variantes release restent désactivées : le lot de diffusion/signature est distinct.

Lint garde les avertissements comme erreurs. Les suggestions de nouvelles versions restent informatives ; aucune baseline ni désactivation globale de règle ne masque les défauts. La CI configurée exécute les contrôles des deux variantes, mais aucun dépôt distant ni passage de CI GitHub n’est encore disponible.

## Parcours natifs sans fournisseur

Sur un émulateur **réservé aux tests**, installer les APK app et AndroidTest correspondants. Ne pas lancer toute la suite remote sans filtrage : certaines classes `Cloud*` sont des essais d’intégration externes qui demandent une configuration et une autorisation de coût distinctes.

Les classes hors réseau utiles sont :

- `fr.previensmoi.app.RootJourneyTest` : six parcours complets avec la véritable activité et les clients HTTP contrôlés ; capture intégrée optionnelle avec `-e uiIntegrationCapture light` (ou `dark` / `large`, après réglage système correspondant). Construire l’app et ses tests avec `-PlocalService=true` pour cette classe ; ne jamais utiliser ce build pour les recherches réelles.
- `fr.previensmoi.app.V1UiTest` : création, confirmation, connexion simulée, préparation, limite, pause, reprise échouée, résultat et captures des écrans de production.
- `fr.previensmoi.app.NotificationOpeningTest` : ouverture directe du résultat, chargement lent, connexion, erreurs, notification répétée et retour sans navigation tardive.
- `fr.previensmoi.app.PauseResumeUiTest` : transitions sans phrases redondantes, annulation d’une reprise acceptée et récupération réservée aux véritables erreurs.
- `fr.previensmoi.app.ForegroundSynchronizationTest` : préparation et résultat visibles sans actualisation/push, annulation en arrière-plan et à la déconnexion, reprise immédiate.
- `fr.previensmoi.app.PreparationFlowTest` : geste d’actualisation, conservation de la liste lors d’un échec, masquage des données après déconnexion.
- `fr.previensmoi.app.FoundationsTest` dans demo : scénarios déterministes, modification/suppression, erreurs et recréation d’écran.

Exemple pour l’émulateur de qualification `emulator-5556`, une fois les APK remote installés :

```sh
adb -s emulator-5556 shell am instrument -w -r \
  -e class fr.previensmoi.app.V1UiTest,fr.previensmoi.app.PreparationFlowTest \
  fr.previensmoi.prototype.remote.test/androidx.test.runner.AndroidJUnitRunner
```

Les scripts de restauration de processus effacent les données de leur variante sur le périphérique ciblé ; ils sont réservés à un émulateur jetable et ne doivent pas être lancés sur le OnePlus de Fabien. Ces scripts ne sont pas requalifiés par le lot du 1er novembre. Le contrat local complet, avec Supabase local, est préparé par `scripts/check-local-android.py` ; voir [service/README.md](../service/README.md).

Le doublon de chargement à l’ouverture du détail est corrigé en **0.1.9**, voir [validation/single-progress](../validation/single-progress/bilan.md). La confirmation allégée et son volet natif d’information sont livrés en **0.1.8**, voir [validation/confirmation](../validation/confirmation/bilan.md). La correction précédente de saisie et de doublon pays est livrée en **0.1.7**, voir [validation/draft-copy](../validation/draft-copy/bilan.md). L’interface validée a été intégrée en **0.1.6** ; voir [validation/approved-ui](../validation/approved-ui/bilan.md) pour les captures natives clair/sombre/texte agrandi et les contrôles. Le lot précédent de fiabilisation (0.1.5) est décrit dans [validation/reliability](../validation/reliability/bilan.md), avec six parcours traversant la véritable activité et ses repositories face à un service HTTP local. Le [guide de diagnostic](../docs/diagnostics.md) donne les filtres Logcat et la corrélation serveur. Le précédent correctif d’ouverture des notifications (0.1.4) est décrit dans [validation/notification-opening](../validation/notification-opening/bilan.md). Le précédent correctif pause/reprise (0.1.3) est dans [validation/pause-resume](../validation/pause-resume/bilan.md). La synchronisation de 0.1.2 est décrite dans [validation/foreground-sync](../validation/foreground-sync/bilan.md). Le [bilan de stabilisation](../validation/stabilization/bilan.md) conserve les contrôles du 1er novembre. Les rapports Gradle sont générés sous `app/build/reports/`. Les captures de fixtures ne prouvent ni la qualité de toutes les recherches ni la livraison FCM sur un appareil physique.

## Lire le code pour un entretien

| Rôle | Point d’entrée |
| --- | --- |
| Modèles et erreurs métier | `app/src/main/kotlin/fr/previensmoi/domain/model/Watch.kt` |
| Ports des repositories | `app/src/main/kotlin/fr/previensmoi/domain/repository/WatchRepository.kt` |
| Création et restauration du parcours | `app/src/main/kotlin/fr/previensmoi/feature/create/presentation/CreateViewModel.kt` |
| Actions, attente et actualisation des suivis | `app/src/main/kotlin/fr/previensmoi/feature/watches/presentation/WatchesViewModel.kt` |
| Session et rotation des jetons | `app/src/main/kotlin/fr/previensmoi/data/auth/RemoteSessionRepository.kt` |
| Adaptation API vers le domaine | `app/src/main/kotlin/fr/previensmoi/data/remote/RemoteRepositories.kt` |
| Navigation et assemblage réel | `app/src/remote/kotlin/fr/previensmoi/app/MainActivity.kt`, `RemoteModule.kt` |
| Notification et relecture authentifiée | `app/src/remote/kotlin/fr/previensmoi/data/push/PushWork.kt` |
| Simulateur déterministe | `app/src/demo/kotlin/fr/previensmoi/data/demo/DemoService.kt` |

`WatchSynchronizationEffect` lance `synchronizeWhileVisible` dans `repeatOnLifecycle(STARTED)`, avec la session comme clé. Une préparation est relue environ toutes les 3 secondes après succès, les états stables et erreurs toutes les 30 secondes. Les appels manuels se partagent un verrou avec ces lectures ; quitter le premier plan ou la session annule le travail. Ces lectures ne déclenchent aucune recherche fournisseur. Les pushes relisent aussi les données et un résultat initial ouvre sa fiche plutôt qu’un simple message « suivi prêt ».

Compose déclenche une action du ViewModel. Celui-ci utilise les interfaces du domaine et expose un `StateFlow` immuable, collecté avec le cycle de vie. Hilt fournit les implémentations ; les écrans n’importent pas la couche data. Un contrôle d’imports vérifie ces frontières. Il n’y a pas de cas d’usage qui transmettent simplement un appel pour ajouter une couche.

Exemple : « Mettre en pause » envoie une commande avec un identifiant et une version attendue. L’écran indique le traitement ; le repository ne remplace le statut que par un état confirmé. Une réponse perdue conserve le même identifiant pour la reprise ; un reçu d’échec confirmé libère l’opération et déclenche une actualisation. L’affichage de la liste et du détail partage la même source d’état.

`StateFlow` ne survit pas à la mort du processus. DataStore garde le brouillon ; `SavedStateHandle` garde de petits identifiants. Les jetons Auth et le journal d’évaluation contenant le secret de prévisualisation sont chiffrés par Android Keystore, hors sauvegarde. Les commandes incertaines conservent leur UUID ; après mort du processus, une commande complète perdue n’est pas inventée pour forcer une nouvelle activation.

FCM transporte des identifiants. Le client relit le résultat avec la session et vérifie l’expiration avant affichage. Le journal de notifications limite les doublons ; une mort du processus entre réservation et affichage peut perdre le push, tandis que le résultat reste disponible dans l’app. La livraison « exactement une fois » n’est pas promise.

Les vecteurs officiels Material Symbols sont accompagnés de leur licence inchangée dans `app/src/main/assets/licenses/material_symbols_license.txt`, incluse dans les APK.

## Limites avant publication

Restent : essai de cette version sur le OnePlus, accessibilité complète dont TalkBack, matrice d’appareils/API, performance et build release, confidentialité/effacement externe, sauvegarde indépendante, observation de sept jours dans le budget disponible et situation Play Console. Le benchmark et la modularisation restent différés. Le [plan actif](../docs/10-plan-de-validation-technique.md) porte ces étapes.
