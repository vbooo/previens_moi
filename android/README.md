# Socle Android — Préviens-moi

Application native de démonstration du sprint 1. Données synthétiques, aucune surveillance réelle, aucun appel Exa/Firecrawl/OpenAI, aucune notification. Les deux écrans servent à éprouver les fondations ; le parcours produit complet appartient au sprint 2.

## Ouvrir et compiler

Ouvrir ce dossier `android/` dans Android Studio. Choisir la variante `demoDebug`. Installer la plateforme Android 37.0 et Build Tools 36.0.0 via le SDK Manager, et sélectionner un JDK 17 pour Gradle. Le fichier `local.properties` porte le chemin SDK propre au poste et est exclu de Git.

```sh
./gradlew assembleDemoDebug testDemoDebugUnitTest lintDemoDebug
python3 ../scripts/check-android-boundaries.py --merged
# Avec un émulateur ou téléphone connecté :
./gradlew connectedDemoDebugAndroidTest
```

Sur le poste de Fabien, les outils du projet sont dans `../.local/` : JDK dans `jdk/Contents/Home`, SDK dans `sdk/`, cache Gradle dans `gradle-home/`. Android Studio est installé dans `~/Applications/Android Studio.app`. Pour les commandes hors IDE, définir `JAVA_HOME` vers le JDK et `GRADLE_USER_HOME` vers ce cache. Ces chemins ne constituent pas une dépendance de la CI.

## Versions

AGP 9.4.1, Gradle 9.6.0, Kotlin/compilateur Compose/sérialisation 2.3.21, KSP 2.3.12, Hilt 2.60.1. Compose BOM 2026.09.00, Activity 1.13.0, Lifecycle 2.11.0, Navigation 3 **1.1.0**, coroutines 1.11.0. Catalogue : `gradle/libs.versions.toml`.

Navigation 3 1.2.0 a été écarté à la compilation : son manifeste exige API 24. La version stable 1.1.0 conserve API 23, notre minimum. `compileSdk`/`targetSdk` 37 ; le téléphone virtuel fourni par le profil Android CLI `medium_phone` utilise API 36 ARM64. API 23 et 37 restent à tester avant publication.

AGP fournit Kotlin intégré ; aucun plugin `org.jetbrains.kotlin.android` ajouté. Les plugins Compose et sérialisation alignent le compilateur Kotlin à 2.3.21. Le wrapper vérifie l’empreinte SHA-256 de Gradle. Aucun artefact demo release n’est généré.

## Lire l’architecture

1. L’écran envoie une action `refresh()` au `WatchesViewModel`.
2. Le ViewModel utilise le port `WatchRepository` du domaine.
3. Hilt injecte `DemoWatchRepository`, présent uniquement dans `src/demo`.
4. Le repository émet la liste confirmée ; le ViewModel expose un `StateFlow<WatchesUiState>` immuable.
5. Compose collecte cet état selon le cycle de vie et redessine l’écran. Navigation 3 conserve une pile de clés sérialisables.

Le domaine ne dépend ni d’Android ni de Hilt. Présentation et data dépendent du domaine ; l’assemblage connaît les implémentations. Un seul module Gradle : aucun cas d’usage de simple délégation ajouté pour remplir une couche. Le port S1 est volontairement réduit à lecture/rafraîchissement ; les commandes, versions et contrats complets seront implémentés avec les tests du sprint 2.

## Preuves et limites

Les tests unitaires vérifient double clic, échec sans perte de données et résultat réussi. Le test sur appareil vérifie la navigation, la recréation de l’Activity et le retour système sans perte de la liste. Ce n’est pas encore un test de mort du processus, de toute l’accessibilité ni du backend.

Android Lint traite les avertissements comme erreurs. Les suggestions de mise à jour de versions sont gardées comme informations, avec un catalogue volontairement figé et compatible. Le contrôle Python vérifie les frontières d’import et le manifeste source. L’absence de permission réseau doit aussi être vérifiée sur l’APK livré, car une dépendance peut ajouter une permission.

La CI GitHub Actions est préparée dans `../.github/workflows/android.yml` ; aucun dépôt distant ni run distant n’est présumé. Le laboratoire historique et ses secrets sont exclus de Git. Aucun nouvel appel fournisseur payant n’est nécessaire à ces commandes.
