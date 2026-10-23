---
name: "Préviens-moi — Android V1"
description: "Système visuel Android natif, sobre et clair, extrait de l’interface Material 3 livrée."
colors:
  light-primary: "#006A6A"
  light-on-primary: "#FFFFFF"
  light-primary-container: "#9CF1EF"
  light-on-primary-container: "#002020"
  light-secondary: "#4A6363"
  light-on-secondary: "#FFFFFF"
  light-secondary-container: "#CCE8E7"
  light-on-secondary-container: "#051F1F"
  light-tertiary: "#46617A"
  light-on-tertiary: "#FFFFFF"
  light-tertiary-container: "#CEE5FF"
  light-on-tertiary-container: "#001D32"
  light-background: "#F8FAFA"
  light-on-background: "#191C1D"
  light-surface: "#F8FAFA"
  light-on-surface: "#191C1D"
  light-surface-container-lowest: "#FFFFFF"
  light-surface-container-low: "#F0F4F4"
  light-surface-container: "#EAEFEF"
  light-surface-container-high: "#E4E9E9"
  light-surface-container-highest: "#DEE3E3"
  light-on-surface-variant: "#3F4949"
  light-outline: "#6F7979"
  light-outline-variant: "#BEC9C8"
  light-error: "#BA1A1A"
  light-on-error: "#FFFFFF"
  light-error-container: "#FFDAD6"
  light-on-error-container: "#410002"
  dark-primary: "#80D5D3"
  dark-on-primary: "#003737"
  dark-primary-container: "#005050"
  dark-on-primary-container: "#9CF1EF"
  dark-secondary: "#B0CCCB"
  dark-on-secondary: "#1C3534"
  dark-secondary-container: "#334B4B"
  dark-on-secondary-container: "#CCE8E7"
  dark-tertiary: "#AECAE6"
  dark-on-tertiary: "#173349"
  dark-tertiary-container: "#2E4A61"
  dark-on-tertiary-container: "#CEE5FF"
  dark-background: "#101718"
  dark-on-background: "#DEE4E3"
  dark-surface: "#101718"
  dark-on-surface: "#DEE4E3"
  dark-surface-container-lowest: "#0B1212"
  dark-surface-container-low: "#192020"
  dark-surface-container: "#1D2424"
  dark-surface-container-high: "#272E2E"
  dark-surface-container-highest: "#323939"
  dark-on-surface-variant: "#BEC9C8"
  dark-outline: "#899392"
  dark-outline-variant: "#3F4949"
  dark-error: "#FFB4AB"
  dark-on-error: "#690005"
  dark-error-container: "#93000A"
  dark-on-error-container: "#FFDAD6"
  light-status-active: "#146C43"
  light-status-preparing: "#245E9C"
  light-status-paused: "#536168"
  light-status-warning: "#805600"
  dark-status-active: "#8FD5AD"
  dark-status-preparing: "#A8CCFF"
  dark-status-paused: "#BCC8CD"
  dark-status-warning: "#F4CC7A"
typography:
  headline-medium:
    fontFamily: "sans-serif"
    fontSize: "28sp"
    fontWeight: 400
    lineHeight: "36sp"
    letterSpacing: "0sp"
  headline-small:
    fontFamily: "sans-serif"
    fontSize: "24sp"
    fontWeight: 400
    lineHeight: "32sp"
    letterSpacing: "0sp"
  title-large:
    fontFamily: "sans-serif"
    fontSize: "22sp"
    fontWeight: 400
    lineHeight: "28sp"
    letterSpacing: "0sp"
  title-medium:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
    letterSpacing: "0.2sp"
  body-large:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "0.5sp"
  body-medium:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
    letterSpacing: "0.2sp"
  label-large:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.1sp"
rounded:
  extra-small: "4dp"
  medium: "12dp"
  large: "16dp"
  extra-large: "28dp"
  full: "50%"
spacing:
  inline-tight: "4dp"
  inline: "8dp"
  compact: "12dp"
  section: "16dp"
  page: "24dp"
components:
  button-primary-light:
    backgroundColor: "{colors.light-primary}"
    textColor: "{colors.light-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 24dp"
  button-primary-dark:
    backgroundColor: "{colors.dark-primary}"
    textColor: "{colors.dark-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 24dp"
  button-outlined-light:
    textColor: "{colors.light-on-surface-variant}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 24dp"
  button-outlined-dark:
    textColor: "{colors.dark-on-surface-variant}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 24dp"
  button-text-light:
    textColor: "{colors.light-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 12dp"
  button-text-dark:
    textColor: "{colors.dark-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 12dp"
  fab-extended-light:
    backgroundColor: "{colors.light-primary-container}"
    textColor: "{colors.light-on-primary-container}"
    typography: "{typography.label-large}"
    rounded: "{rounded.large}"
    height: "56dp"
  fab-extended-dark:
    backgroundColor: "{colors.dark-primary-container}"
    textColor: "{colors.dark-on-primary-container}"
    typography: "{typography.label-large}"
    rounded: "{rounded.large}"
    height: "56dp"
  field-outlined-light:
    textColor: "{colors.light-on-surface}"
    typography: "{typography.body-large}"
    rounded: "{rounded.extra-small}"
    width: "100%"
  field-outlined-dark:
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.body-large}"
    rounded: "{rounded.extra-small}"
    width: "100%"
  notification-surface-light:
    backgroundColor: "{colors.light-surface-container-low}"
    textColor: "{colors.light-on-surface}"
    rounded: "{rounded.medium}"
    padding: "16dp"
  notification-surface-dark:
    backgroundColor: "{colors.dark-surface-container-low}"
    textColor: "{colors.dark-on-surface}"
    rounded: "{rounded.medium}"
    padding: "16dp"
  watch-status-active-light:
    textColor: "{colors.light-status-active}"
    typography: "{typography.label-large}"
  watch-status-preparing-light:
    textColor: "{colors.light-status-preparing}"
    typography: "{typography.label-large}"
  watch-status-active-dark:
    textColor: "{colors.dark-status-active}"
    typography: "{typography.label-large}"
  watch-status-preparing-dark:
    textColor: "{colors.dark-status-preparing}"
    typography: "{typography.label-large}"
  top-app-bar-light:
    backgroundColor: "{colors.light-background}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.title-large}"
  list-item-light:
    backgroundColor: "{colors.light-surface}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.title-medium}"
    width: "100%"
  dialog-light:
    backgroundColor: "{colors.light-surface-container-high}"
    textColor: "{colors.light-on-surface}"
    rounded: "{rounded.extra-large}"
    padding: "24dp"
  top-app-bar-dark:
    backgroundColor: "{colors.dark-background}"
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.title-large}"
  list-item-dark:
    backgroundColor: "{colors.dark-surface}"
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.title-medium}"
    width: "100%"
  dialog-dark:
    backgroundColor: "{colors.dark-surface-container-high}"
    textColor: "{colors.dark-on-surface}"
    rounded: "{rounded.extra-large}"
    padding: "24dp"
---

# Design System: Préviens-moi

## Overview

**Creative North Star: "Material 3 natif, sobre et clair"**

Préviens-moi adopte les composants natifs Jetpack Compose Material 3, la typographie système Android et une palette sobre choisie pour remplacer le violet par défaut. La priorité visuelle est la lecture de la demande, de son état et de la prochaine action. La couleur sert les actions et la compréhension des états ; les surfaces restent neutres.

Cette documentation décrit la V1 téléphone actuellement implémentée. Les valeurs du frontmatter sont normatives pour cette extraction : les couleurs proviennent de PreviensTheme.kt ; les rôles typographiques et les formes sont hérités de Material 3 (version résolue 1.4.0, BOM 2026.09.00), sans surcharge locale. Les unités Android restent en dp et sp : ne pas copier ces valeurs comme des pixels fixes dans Compose. Les aperçus HTML du sidecar sont des traductions de consultation et ne remplacent ni les composants natifs ni leurs comportements.

**Key Characteristics:**

- Composants Material 3 et typographie système.
- Actions en bleu canard, états sémantiques séparés.
- Surfaces neutres et profondeur native mesurée.
- Pages lisibles, défilantes, compatibles avec le texte agrandi.

La source visuelle courante est `.impeccable/review/v1/` : 22 captures natives, dont les variantes sombres et le texte agrandi. Les écrans de production sont ceux de la variante `remote`. `PRODUCT.md` conserve un contexte historique de démonstration et n’a pas été réécrit pendant cette extraction ; il ne définit pas les couleurs de cette V1. Cette documentation ne modifie aucune promesse produit.

## Colors

La palette associe un bleu canard mesuré aux actions, des neutres légèrement froids aux surfaces et des couleurs distinctes aux états. Les préfixes `light-` et `dark-` identifient les deux schémas statiques du frontmatter. `PreviensTheme` suit le thème système ; Dynamic Color n’est pas activé.

### Primary

Le **bleu canard** porte les boutons remplis, liens et actions textuelles. Les couples `primary` / `on-primary` et `primary-container` / `on-primary-container` gouvernent respectivement les actions remplies et le FAB étendu. Ne pas substituer les couleurs de statut à ces rôles.

### Secondary

Le **gris sarcelle** est le jeu secondaire déclaré dans le schéma Material. Ses rôles restent disponibles pour les composants natifs ; la V1 ne crée pas une seconde famille d’actions personnalisées.

### Tertiary

Le **bleu ardoise** est le jeu tertiaire déclaré. Les statuts de préparation possèdent leurs propres tokens `status-preparing` : ne pas les déduire d’un rôle tertiaire ni en changer selon l’écran.

### Neutral

Les rôles `background` et `surface` sont identiques dans chaque thème. `on-surface` porte le texte principal ; `on-surface-variant` porte les métadonnées et explications secondaires. Les niveaux `surface-container-lowest` à `surface-container-highest` constituent la gamme de surfaces. Les séparateurs explicites et contours de boutons utilisent `outline-variant` ; les champs utilisent le contour natif `outline`.

### Status and error

| Rôle | Usage réel |
| --- | --- |
| `status-active` — vert | Actif et Terminé, avec coche cerclée. |
| `status-preparing` — bleu | En préparation, avec horloge. |
| `status-paused` — gris ardoise | En pause, avec symbole pause. |
| `status-warning` — ambre | Limite de capacité, arrêt en cours, ou suivi actif dont la prochaine vérification ou la santé ne sont pas confirmées. |
| `error` — rouge | Échec de préparation, erreurs et actions de suppression. |

`WatchStatusLabel` donne la priorité à l’échec de préparation, puis à l’arrêt en cours ou à un état actif non vérifié, avant le statut nominal. Une préparation échouée n’est donc pas affichée comme une simple pause. `FailureText` garde les opérations en attente en texte secondaire ; ses autres erreurs utilisent `error`.

**The Semantic Color Rule.** Employer MaterialTheme.colorScheme et les couleurs de statut du thème ; choisir la variante claire ou sombre par le thème, jamais par un hexadécimal ajouté dans un écran.

**The Redundant Status Rule.** Conserver le libellé et l’icône de WatchStatusLabel avec sa couleur. Le vert est réservé à Actif ou Terminé ; la préparation utilise le bleu.

Les rôles non surchargés dans `PreviensTheme.kt` restent ceux de la bibliothèque. Ne pas inventer de valeurs locales pour eux ; extraire et vérifier le composant concerné avant d’étendre cette documentation.

## Typography

**Display Font:** sans-serif système Android, via Material 3. Aucun rôle Display n’est utilisé dans les écrans V1 inspectés.

**Body Font:** la même famille système. Aucune police embarquée, police décorative ni famille mono dédiée.

**Character:** hiérarchie native, claire et stable. Les titres donnent le contexte, le corps explique, les labels identifient les actions et les états. Le frontmatter contient uniquement les sept rôles réutilisés explicitement par les écrans et composants de présentation.

### Hierarchy

| Rôle Material | Usage observé |
| --- | --- |
| `headlineMedium` | Question de création, état vide, connexion et titre du résultat. |
| `headlineSmall` | Titre du suivi, proposition et question de clarification. |
| `titleLarge` | Titre de compte ; rôle natif de la barre supérieure. |
| `titleMedium` | Titres de section et titres de la liste de suivis. |
| `bodyLarge` | Texte courant natif et fait principal d’un résultat. |
| `bodyMedium` | SecondaryText et métadonnées. |
| `labelLarge` | WatchStatusLabel et actions natives. |

Les rôles supplémentaires employés à l’intérieur d’un champ ou d’un dialogue restent gérés par Material 3. Les valeurs chiffrées du frontmatter ont été vérifiées dans les tokens de la bibliothèque résolue ; aucun ratio typographique personnalisé n’est imposé.

**The Material Type Roles Rule.** Utiliser les rôles MaterialTheme.typography existants et laisser Android appliquer l’échelle système. Les chiffres extraits ne sont pas une invitation à créer des tailles par écran.

## Layout

`Page` organise une colonne défilante avec un espacement vertical de section, des marges horizontales de page, un retrait supérieur compact et un retrait inférieur de page. Le contenu et son pied fixe sont centrés dans une largeur maximale (720 dp). Ce plafond est une valeur réelle du composant commun ; il ne constitue pas une qualification tablette.

Le pied de page de confirmation ou de résultat reste séparé de la zone défilante, sur `surface`. Il reprend les marges horizontales de page, avec un retrait supérieur compact et inférieur inline. Les boutons de confirmation et d’ouverture de source prennent toute la largeur et ont une hauteur minimale explicite (48 dp). La page reste défilante quand le texte grandit.

La liste d’accueil est une `LazyColumn` : marges horizontales de page, retrait supérieur compact, espacement inline, et réserve inférieure pour le FAB (112 dp). Les entrées emploient `ListItem` et un séparateur, sans grille de cartes.

`MetaRow` affiche deux colonnes égales et un espacement de section à l’échelle normale. Au-delà d’une échelle de police (1,15), le libellé passe au-dessus de la valeur avec un espacement inline-tight. Les deux variantes ont un retrait vertical local (6 dp). Les choix radio occupent une ligne cliquable pleine largeur, d’une hauteur minimale (48 dp), avec espacement compact ; leur libellé peut revenir à la ligne.

Le `Scaffold` fournit les insets ; le contenu applique ces insets et `imePadding`. L’activité active l’affichage bord à bord. Le thème adapte la clarté des icônes de statut et de navigation à son mode. Les icônes communes sont dimensionnées (24 dp), avec un espacement inline près d’un libellé de statut. Une icône de cette taille ne définit pas la taille de sa cible tactile : conserver les `IconButton` natifs.

La V1 documentée cible le téléphone. Aucune grille, aucun breakpoint de tablette et aucune navigation rail ne sont établis par les captures V1.

## Elevation & Depth

Les pages, listes et sections reposent sur des surfaces plates et des séparateurs discrets. Le conteneur d’activation des notifications emploie une surface tonale basse, sans ombre personnalisée. La profondeur du FAB étendu, des menus et des dialogues reste celle des composants Material 3. Le FAB utilise l’élévation native de repos (6 dp), élevée au survol par la bibliothèque ; cela ne justifie pas de recréer des ombres CSS dans Compose.

**The Native Elevation Rule.** Conserver les élévations natives des composants ; ne pas ajouter d’ombres décoratives aux listes ou aux sections.

Aucun token d’ombre ou de mouvement propre à l’application n’est défini. Les indicateurs de progression, le retour tactile, les états pressé/focus et les transitions de navigation restent natifs. Le sidecar ne prescrit pas de nouvelle animation.

## Shapes

Les formes sont héritées du thème Material 3. Les actions remplies, les boutons contour et les actions textuelles ont des extrémités entièrement arrondies (`full`). Les champs contour utilisent `extra-small` ; le panneau de notifications emploie `medium` ; le FAB étendu utilise `large` ; les dialogues utilisent `extra-large`. Ces valeurs sont enregistrées dans le frontmatter pour rendre l’extraction inspectable, sans créer un deuxième thème de formes.

Les listes ne sont pas enveloppées dans des cartes. Le conteneur tonal de notifications reste une exception fonctionnelle identifiée. Aucun dégradé décoratif, illustration, texture ni traitement translucide n’est présent dans cette V1.

## Components

### Buttons

Les actions utilisent `Button`, `OutlinedButton` ou `TextButton`, sans composant dessiné à la main. Le bouton rempli utilise le couple primaire ; le bouton contour conserve le texte `onSurfaceVariant` et le contour `outlineVariant` de Material 3 résolu ; l’action textuelle utilise `primary`. Les suppressions sont des actions textuelles en `error`.

Les espacements du frontmatter correspondent aux boutons natifs sans icône. Les états désactivé, pressé, focus et survol restent ceux de la bibliothèque. Ne pas reproduire les styles interactifs d’un aperçu HTML dans le code Android. Les icônes ajoutées par les écrans utilisent AppIcon avec un espacement inline.

### Fields and choices

`OutlinedTextField` occupe toute la largeur. Le champ de demande accepte de trois à six lignes ; les champs zone, site, email et code sont sur une ligne. Le site préféré affiche son texte d’aide ou son erreur via `supportingText` et `isError`. Les états de focus et d’erreur du contour restent natifs.

Les choix de mode et de clarification utilisent `RadioButton` dans une ligne `selectable` avec rôle radio ; le clic appartient à la ligne, avec `onClick = null` sur le contrôle. Conserver les labels visibles et les sémantiques accessibles. Les erreurs dynamiques utilisent une région d’annonce polie via `FailureText`.

### Navigation

La variante de production emploie une `TopAppBar` sur le fond de page, une action retour native sur les routes secondaires et un menu d’actions sur le détail. Les titres de la barre sont limités à une ligne avec ellipse. L’accueil présente l’accès au compte et l’actualisation dans la barre supérieure, ainsi qu’un seul FAB étendu « Nouveau suivi ». Le retour passe par le `NavDisplay` et la pile de navigation ; aucun menu inférieur n’est implémenté dans cette V1.

### Lists and metadata

Les suivis et les résultats utilisent `ListItem`. Le titre de suivi est limité à deux lignes avec ellipse ; l’état garde son texte et son icône ; les métadonnées utilisent `SecondaryText`. Les chevrons indiquent une navigation interne. Les liens de source utilisent une ligne cliquable, une icône d’ouverture externe en primaire et une hauteur minimale (56 dp). Réutiliser `sourceDomain` pour le nom de domaine affiché.

`SectionTitle`, `SecondaryText` et `MetaRow` définissent la hiérarchie récurrente. Garder les informations secondaires en texte lisible, sans les transformer en micro-badges.

### Watch status

`WatchStatusLabel` est la signature fonctionnelle du système : icône (24 dp), espacement inline, label Material et couleur sémantique. Actif et Terminé partagent le vert ; En préparation reste bleu ; En pause reste ardoise ; limites et états incertains restent ambre ; l’échec de préparation reste rouge. Il s’agit d’une ligne informative, pas d’une puce interactive.

### Containers and dialogs

Le rappel de notifications utilise `Surface(surfaceContainerLow, shapes.medium)` et un retrait de section. Les dialogues de suppression et de confidentialité sont des `AlertDialog` natifs. Les suppressions présentent une annulation distincte et une action destructrice en rouge ; la confidentialité conserve son action de fermeture. La V1 n’introduit aucune carte générique personnalisée.

### Loading and fixed actions

`Busy` associe un indicateur circulaire (24 dp, trait de 2 dp) à un libellé annoncé poliment. Le bouton d’évaluation peut afficher un indicateur compact (18 dp, trait de 2 dp) avec espacement compact. Les actions de confirmation et d’ouverture de source utilisent le pied de `Page` ; les autres contenus restent défilants.

## Do's and Don'ts

### Do:

- **Do** utiliser les rôles de thème et les couleurs sémantiques pour chaque état.
- **Do** conserver texte et icône sur les statuts, y compris en mode sombre.
- **Do** réutiliser Page, MetaRow, SectionTitle et SecondaryText pour les nouveaux écrans.
- **Do** vérifier le défilement, le clavier, les barres système et la lisibilité avec une échelle de texte de 1,3.
- **Do** conserver les composants, cibles tactiles et interactions natives Material 3.

### Don't:

- **Don’t** réintroduire le violet par défaut pour les actions ni utiliser le vert pour la préparation.
- **Don’t** remplacer la typographie système par une police de marque ou ajouter une direction fantaisiste.
- **Don’t** utiliser uniquement une couleur pour annoncer un état, une limite ou une erreur.
- **Don’t** transformer les lignes de suivi en cartes surélevées sans décision explicite.
- **Don’t** présenter les anciens écrans de démonstration ou les captures tablette historiques comme la référence de cette V1 téléphone.
