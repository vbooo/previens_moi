---
name: "Préviens-moi — Android V1"
description: "Système visuel Android natif Material 3, palette encre et cobalt et hiérarchie par surfaces tonales."
colors:
  light-primary: "#324C85"
  light-on-primary: "#FFFFFF"
  light-primary-container: "#DCE4FF"
  light-on-primary-container: "#0B1B3C"
  light-secondary: "#555F75"
  light-on-secondary: "#FFFFFF"
  light-secondary-container: "#E0E5F1"
  light-on-secondary-container: "#182035"
  light-tertiary: "#665D50"
  light-on-tertiary: "#FFFFFF"
  light-tertiary-container: "#EFE1CE"
  light-on-tertiary-container: "#241B10"
  light-background: "#F5F6FA"
  light-on-background: "#1B1E27"
  light-surface: "#F5F6FA"
  light-on-surface: "#1B1E27"
  light-surface-container-lowest: "#FFFFFF"
  light-surface-container-low: "#EDF0F7"
  light-surface-container: "#E6EAF3"
  light-surface-container-high: "#DFE4EF"
  light-surface-container-highest: "#D9DFEC"
  light-on-surface-variant: "#464B59"
  light-outline: "#747986"
  light-outline-variant: "#C5CAD6"
  light-error: "#BA1A1A"
  light-on-error: "#FFFFFF"
  light-error-container: "#FFDAD6"
  light-on-error-container: "#410002"
  dark-primary: "#B2C5FF"
  dark-on-primary: "#002A64"
  dark-primary-container: "#1D3D72"
  dark-on-primary-container: "#DCE4FF"
  dark-secondary: "#BCC6DD"
  dark-on-secondary: "#263047"
  dark-secondary-container: "#3D475E"
  dark-on-secondary-container: "#E0E5F1"
  dark-tertiary: "#D2C5B4"
  dark-on-tertiary: "#382F22"
  dark-tertiary-container: "#504636"
  dark-on-tertiary-container: "#EFE1CE"
  dark-background: "#11141B"
  dark-on-background: "#E2E5EF"
  dark-surface: "#11141B"
  dark-on-surface: "#E2E5EF"
  dark-surface-container-lowest: "#0C0F16"
  dark-surface-container-low: "#1A1E27"
  dark-surface-container: "#1F232D"
  dark-surface-container-high: "#292E38"
  dark-surface-container-highest: "#343944"
  dark-on-surface-variant: "#C5CAD6"
  dark-outline: "#8E93A1"
  dark-outline-variant: "#444956"
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
  body-small:
    fontFamily: "sans-serif"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "16sp"
    letterSpacing: "0.4sp"
  label-medium:
    fontFamily: "sans-serif"
    fontSize: "12sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.5sp"
  headline-small-medium:
    fontFamily: "sans-serif"
    fontSize: "24sp"
    fontWeight: 500
    lineHeight: "32sp"
    letterSpacing: "0sp"
  title-medium-semibold:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "24sp"
    letterSpacing: "0.2sp"
  body-medium-medium:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.2sp"
rounded:
  extra-small: "4dp"
  small: "8dp"
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
  summary: "20dp"
components:
  button-primary-light:
    backgroundColor: "{colors.light-primary}"
    textColor: "{colors.light-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 24dp"
  button-confirm-light:
    backgroundColor: "{colors.light-primary}"
    textColor: "{colors.light-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.large}"
    width: "100%"
    height: "52dp"
  button-tonal-light:
    backgroundColor: "{colors.light-secondary-container}"
    textColor: "{colors.light-on-secondary-container}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    width: "100%"
    height: "48dp"
  button-text-light:
    textColor: "{colors.light-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 12dp"
  fab-extended-light:
    backgroundColor: "{colors.light-primary-container}"
    textColor: "{colors.light-on-primary-container}"
    typography: "{typography.label-large}"
    rounded: "{rounded.large}"
    height: "56dp"
  field-outlined-light:
    textColor: "{colors.light-on-surface}"
    typography: "{typography.body-large}"
    rounded: "{rounded.extra-small}"
    width: "100%"
  watch-summary-light:
    backgroundColor: "{colors.light-surface-container}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.headline-small-medium}"
    rounded: "{rounded.large}"
    padding: "{spacing.summary}"
    width: "100%"
  proposal-card-light:
    backgroundColor: "{colors.light-primary-container}"
    textColor: "{colors.light-on-primary-container}"
    typography: "{typography.headline-small-medium}"
    rounded: "{rounded.large}"
    padding: "{spacing.summary}"
    width: "100%"
  section-card-light:
    backgroundColor: "{colors.light-surface-container-lowest}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.title-medium-semibold}"
    rounded: "{rounded.large}"
    padding: "{spacing.section}"
    width: "100%"
  collection-card-light:
    backgroundColor: "{colors.light-surface-container-lowest}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.title-medium-semibold}"
    rounded: "{rounded.large}"
    width: "100%"
  mode-choice-selected-light:
    backgroundColor: "{colors.light-primary-container}"
    textColor: "{colors.light-on-primary-container}"
    typography: "{typography.body-medium}"
    rounded: "{rounded.medium}"
    padding: "4dp 8dp"
    width: "100%"
    height: "52dp"
  watch-status-active-light:
    textColor: "{colors.light-status-active}"
    typography: "{typography.label-large}"
  watch-status-preparing-light:
    textColor: "{colors.light-status-preparing}"
    typography: "{typography.label-large}"
  watch-status-contained-light:
    textColor: "{colors.light-status-active}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    padding: "8dp 12dp"
  capacity-chip-light:
    textColor: "{colors.light-status-warning}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    height: "48dp"
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
  button-primary-dark:
    backgroundColor: "{colors.dark-primary}"
    textColor: "{colors.dark-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 24dp"
  button-confirm-dark:
    backgroundColor: "{colors.dark-primary}"
    textColor: "{colors.dark-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.large}"
    width: "100%"
    height: "52dp"
  button-tonal-dark:
    backgroundColor: "{colors.dark-secondary-container}"
    textColor: "{colors.dark-on-secondary-container}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    width: "100%"
    height: "48dp"
  button-text-dark:
    textColor: "{colors.dark-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 12dp"
  fab-extended-dark:
    backgroundColor: "{colors.dark-primary-container}"
    textColor: "{colors.dark-on-primary-container}"
    typography: "{typography.label-large}"
    rounded: "{rounded.large}"
    height: "56dp"
  field-outlined-dark:
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.body-large}"
    rounded: "{rounded.extra-small}"
    width: "100%"
  watch-summary-dark:
    backgroundColor: "{colors.dark-surface-container}"
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.headline-small-medium}"
    rounded: "{rounded.large}"
    padding: "{spacing.summary}"
    width: "100%"
  proposal-card-dark:
    backgroundColor: "{colors.dark-primary-container}"
    textColor: "{colors.dark-on-primary-container}"
    typography: "{typography.headline-small-medium}"
    rounded: "{rounded.large}"
    padding: "{spacing.summary}"
    width: "100%"
  section-card-dark:
    backgroundColor: "{colors.dark-surface-container-lowest}"
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.title-medium-semibold}"
    rounded: "{rounded.large}"
    padding: "{spacing.section}"
    width: "100%"
  collection-card-dark:
    backgroundColor: "{colors.dark-surface-container-lowest}"
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.title-medium-semibold}"
    rounded: "{rounded.large}"
    width: "100%"
  mode-choice-selected-dark:
    backgroundColor: "{colors.dark-primary-container}"
    textColor: "{colors.dark-on-primary-container}"
    typography: "{typography.body-medium}"
    rounded: "{rounded.medium}"
    padding: "4dp 8dp"
    width: "100%"
    height: "52dp"
  watch-status-active-dark:
    textColor: "{colors.dark-status-active}"
    typography: "{typography.label-large}"
  watch-status-preparing-dark:
    textColor: "{colors.dark-status-preparing}"
    typography: "{typography.label-large}"
  watch-status-contained-dark:
    textColor: "{colors.dark-status-active}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    padding: "8dp 12dp"
  capacity-chip-dark:
    textColor: "{colors.dark-status-warning}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    height: "48dp"
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

**Creative North Star: "Material 3 natif, encre et cobalt"**

Préviens-moi adopte les composants natifs Jetpack Compose Material 3, la typographie système Android et une palette sobre choisie pour remplacer le violet par défaut. La palette encre et cobalt et les surfaces tonales structurent la lecture : demande, état et prochaine action forment un résumé identifiable, suivi de sections lisibles. Les comportements restent ceux des composants natifs.

Cette documentation décrit la révision téléphone actuellement implémentée, après autorisation de réviser la palette et les écrans de détail et de confirmation. Les valeurs du frontmatter sont normatives pour cette extraction : les couleurs proviennent de PreviensTheme.kt ; les rôles typographiques et les formes sont hérités de Material 3 (version résolue 1.4.0, BOM 2026.09.00), sans surcharge du thème ; les emphases de graisse locales sont décrites ci-dessous. Les unités Android restent en dp et sp : ne pas copier ces valeurs comme des pixels fixes dans Compose. Les aperçus HTML du sidecar sont des traductions de consultation et ne remplacent ni les composants natifs ni leurs comportements.

**Key Characteristics:**

- Composants Material 3 et typographie système.
- Actions en encre et cobalt, états sémantiques séparés.
- Résumés tonals, sections claires et profondeur native mesurée.
- Pages lisibles, défilantes, compatibles avec le texte agrandi.

La source visuelle courante est `.impeccable/review/v2/` : 16 captures de fixtures Android, dont les variantes sombres et le texte agrandi. Les sources Kotlin gouvernent cette extraction. Les fixtures ne qualifient ni réseau ni service ; les aperçus HTML ne démontrent aucune parité APK/web. Les écrans de production sont ceux de la variante `remote`. `PRODUCT.md` conserve un contexte historique de démonstration et n’a pas été réécrit pendant cette extraction ; il ne définit pas les couleurs de cette V1. Cette documentation ne modifie aucune promesse produit.

## Colors

La palette associe une encre cobalt aux actions, des neutres légèrement froids aux surfaces et des couleurs distinctes aux états. Les préfixes `light-` et `dark-` identifient les deux schémas statiques du frontmatter. `PreviensTheme` suit le thème système ; Dynamic Color n’est pas activé.

### Primary

L’**encre cobalt** porte les boutons remplis, liens et actions textuelles. Les couples `primary` / `on-primary` et `primary-container` / `on-primary-container` gouvernent respectivement les actions remplies et le FAB étendu, le résumé de proposition et le choix de mode sélectionné. Le résumé du détail utilise une surface neutre tonale. Ne pas substituer les couleurs de statut à ces rôles.

### Secondary

L’**ardoise bleutée** est le jeu secondaire Material. FilledTonalButton utilise secondaryContainer / onSecondaryContainer pour la pause ou l’annulation de préparation, comme action subordonnée au résumé.

### Tertiary

Le **grège chaud** est le jeu tertiaire déclaré ; il ne crée pas une famille d’actions personnalisées. Les statuts de préparation possèdent leurs propres tokens `status-preparing` : ne pas les déduire d’un rôle tertiaire ni en changer selon l’écran.

### Neutral

Les rôles `background` et `surface` sont identiques dans chaque thème. `on-surface` porte le texte principal ; `on-surface-variant` porte les métadonnées et explications secondaires. Les niveaux `surface-container-lowest` à `surface-container-highest` constituent la gamme de surfaces. Le résumé du détail emploie `surface-container`, les sections et le pied fixe `surface-container-lowest`. Les séparateurs explicites utilisent `outline-variant` ; les champs utilisent le contour natif `outline`.

### Status and error

| Rôle | Usage réel |
| --- | --- |
| `status-active` — vert | Actif et Terminé, avec coche cerclée. |
| `status-preparing` — bleu | En préparation, avec horloge. |
| `status-paused` — gris ardoise | En pause, avec symbole pause. |
| `status-warning` — ambre | Limite de capacité, arrêt en cours, ou suivi actif dont la prochaine vérification ou la santé ne sont pas confirmées. |
| `error` — rouge | Échec de préparation, erreurs et actions de suppression. |

`WatchStatusLabel` donne la priorité à l’échec de préparation, puis à l’arrêt en cours ou à un état actif non vérifié, avant le statut nominal. Une préparation échouée n’est donc pas affichée comme une simple pause. `FailureText` garde les opérations en attente en texte secondaire ; ses autres erreurs utilisent `error`. Dans le résumé, le fond de statut est dérivé de sa couleur à 10 % d’opacité, sans nouveau rôle de palette. La suspension par capacité devient un AssistChip ouvrant une explication.

**The Semantic Color Rule.** Employer MaterialTheme.colorScheme et les couleurs de statut du thème ; choisir la variante claire ou sombre par le thème, jamais par un hexadécimal ajouté dans un écran.

**The Redundant Status Rule.** Conserver le libellé et l’icône de WatchStatusLabel avec sa couleur. Le vert est réservé à Actif ou Terminé ; la préparation utilise le bleu.

Les rôles non surchargés dans `PreviensTheme.kt` restent ceux de la bibliothèque. Ne pas inventer de valeurs locales pour eux ; extraire et vérifier le composant concerné avant d’étendre cette documentation.

## Typography

**Display Font:** sans-serif système Android, via Material 3. Aucun rôle Display n’est utilisé dans les écrans V1 inspectés.

**Body Font:** la même famille système. Aucune police embarquée, police décorative ni famille mono dédiée.

**Character:** hiérarchie native, claire et stable. Les titres donnent le contexte, le corps explique, les labels identifient les actions et les états. Le frontmatter contient neuf rôles natifs utilisés et trois variantes de graisse locales observées : headlineSmall Medium, titleMedium SemiBold et bodyMedium Medium. Ces emphases ne modifient pas le thème typographique.

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
| `labelMedium` | Libellés des dates du résumé et réglages de confirmation. |
| `bodySmall` | Note d’essai de la confirmation. |

Les rôles supplémentaires employés à l’intérieur d’un champ ou d’un dialogue restent gérés par Material 3. Les valeurs chiffrées du frontmatter ont été vérifiées dans les tokens de la bibliothèque résolue ; aucun ratio typographique personnalisé n’est imposé.

**The Material Type Roles Rule.** Utiliser les rôles MaterialTheme.typography existants et laisser Android appliquer l’échelle système. Les chiffres extraits ne sont pas une invitation à créer des tailles par écran.

## Layout

`Page` organise une colonne défilante avec un espacement de section, des marges horizontales de page, un retrait supérieur compact et un retrait inférieur de page. Le contenu et son pied fixe sont centrés dans une largeur maximale (720 dp). Ce plafond réel ne qualifie pas une interface tablette.

Le pied fixe de confirmation ou de résultat utilise `surfaceContainerLowest`, une élévation tonale (2 dp), les marges horizontales de page et des retraits supérieur compact et inférieur inline. La confirmation utilise un bouton pleine largeur, de hauteur minimale (52 dp), suivi de l’action textuelle de révision ; l’ouverture de source conserve son minimum (48 dp). Les hauteurs de boutons et puces du frontmatter sont des minima : laisser le texte agrandi augmenter le composant.

L’accueil conserve une LazyColumn : marges horizontales de page, retrait supérieur compact, espacement inline et réserve inférieure pour le FAB (112 dp). Ses suivis restent des ListItem avec séparateur. Dans le détail, SectionCard applique un retrait de section et un espacement compact. CollectionCard réserve les retraits à son titre ; les ListItem possèdent leurs insets natifs, sans second retrait ni espacement de colonne autour des lignes. Les séparateurs se placent directement entre les lignes.

Les résumés de détail et de proposition utilisent un retrait de résumé et la forme `large`. CheckTiming présente deux colonnes égales, un espacement de section et un libellé au-dessus de chaque valeur. Cette structure reste en place au texte agrandi ; les valeurs peuvent revenir à la ligne. MetaRow, utilisé ailleurs, empile libellé et valeur au-delà d’une échelle de police (1,15) ; il conserve deux colonnes à l’échelle normale et un retrait vertical local (6 dp).

La confirmation présente le résumé, le comportement après résultat, puis les réglages de zone, site, fréquence et sources. Les choix sont des lignes sélectionnables avec radio, hauteur minimale (52 dp) et libellé multilignes. Les réglages associent un label au-dessus de la valeur, un minimum (56 dp), des séparateurs et un chevron seulement quand la ligne est modifiable. Cette composition décrit la confirmation actuelle sans imposer son ordre aux autres écrans.

Le Scaffold fournit les insets appliqués au contenu avec imePadding ; l’activité active l’affichage bord à bord. Les icônes des barres système suivent le thème. Les icônes communes mesurent (24 dp), ce qui ne définit pas leur cible tactile : conserver les contrôles natifs. Aucun breakpoint de tablette ni navigation rail n’est établi par les captures téléphone.

## Elevation & Depth

La profondeur vient des surfaces tonales et des composants natifs. Les Card sont les cartes remplies Material, sans ombre personnalisée. Le résumé du détail est plus tonal que les sections ; la proposition emploie le conteneur primaire. Le pied fixe est une Surface avec élévation tonale, sans ombre dessinée par l’application. Le FAB étendu conserve son élévation native de repos (6 dp), les menus et dialogues leur profondeur native.

**The Native Elevation Rule.** Conserver les surfaces et élévations Material ; ne pas ajouter d’ombres décoratives aux cartes ou listes.

Aucun token d’ombre ou de mouvement applicatif n’est défini. Indicateurs, ripple, états pressé/focus et transitions de navigation restent natifs. Les ombres des aperçus HTML sont des approximations de consultation.

## Shapes

Les formes sont héritées de Material 3 : `extra-small` pour les champs contour, `small` pour le statut contenu et l’AssistChip, `medium` pour les choix de mode et le bouton tonal, `large` pour les résumés, sections, collections et confirmation, `extra-large` pour les dialogues. Le FAB utilise `large` ; les autres boutons remplis et textuels gardent `full`. Les valeurs du frontmatter rendent l’extraction inspectable, sans créer un second thème.

Les suivis d’accueil restent des lignes ; résultats et sources sont réunis dans des cartes de section. Aucun dégradé décoratif, illustration, texture ou traitement translucide n’est implémenté.

## Components

### Buttons

Button porte l’action principale et la reprise. La confirmation garde sa forme `large` et sa hauteur minimale ; la reprise utilise `medium`. FilledTonalButton porte la pause et l’annulation de préparation avec les couleurs secondaires natives et la forme `medium`. TextButton sert aux actions légères ; la suppression utilise `error`. Aucun OutlinedButton n’est actuellement utilisé dans les sources main/remote inspectées.

Les espacements du frontmatter des boutons ordinaires correspondent aux défauts sans icône. Les états désactivé, pressé, focus et survol restent natifs. Les icônes communes sont les vecteurs officiels Material Symbols conservés sans changement de tracé ; la licence Apache 2.0 est dans `android/app/src/main/res/raw/material_symbols_license.txt`.

### Fields and choices

OutlinedTextField prend toute la largeur. La demande accepte de trois à six lignes ; zone, site, email et code sont monolignes. Le site préféré utilise supportingText et isError ; focus et erreur restent natifs.

ModeChoice place un RadioButton dans une ligne selectable avec rôle radio : la ligne gère le clic, le contrôle garde onClick = null. Le choix sélectionné utilise primaryContainer / onPrimaryContainer ; l’autre garde la surface de section. Les clarifications réemploient des radios en lignes sélectionnables. Conserver labels et sémantiques accessibles. FailureText annonce les erreurs dans une région polie.

### Navigation

La production utilise TopAppBar sur background, le retour natif sur les routes secondaires et un menu d’actions sur le détail. Le titre de barre reste sur une ligne avec ellipse. L’accueil propose compte, actualisation et FAB étendu « Nouveau suivi ». Le retour passe par NavDisplay et sa pile ; aucun menu inférieur n’est implémenté.

### Cards and collections

Le résumé de détail associe titre, statut contenu, explication utile, CheckTiming et action disponible dans une Card(surfaceContainer). La proposition utilise Card(primaryContainer, onPrimaryContainer). SectionCard et CollectionCard utilisent surfaceContainerLowest ; leurs titres sont en titleMedium avec emphase SemiBold.

SectionCard applique les retraits et l’espacement au contenu libre. CollectionCard laisse chaque ListItem gérer ses insets : les lignes ont un fond transparent pour conserver la surface de carte, et les séparateurs restent sans espacement ajouté. Les résultats portent un chevron interne ; les sources portent l’icône d’ouverture externe en primaire. Réutiliser sourceDomain pour afficher le domaine. Une source cliquable conserve sa hauteur minimale (56 dp).

SectionTitle, SecondaryText et MetaRow restent les primitives de hiérarchie des autres contenus. Les métadonnées demeurent du texte lisible.

### Watch status and chip

WatchStatusLabel réunit icône, label et couleur sémantique : vert pour Actif/Terminé, bleu pour la préparation, ardoise pour la pause, ambre pour la limite ou l’incertitude et rouge pour l’échec de préparation. Sur l’accueil il reste une ligne transparente informative. Dans le résumé, Surface(shapes.small) ajoute le fond dérivé à 10 % et un retrait horizontal compact et vertical inline.

La suspension par capacité seule utilise un AssistChip natif interactif, sans contour, de hauteur minimale (48 dp), avec chevron et ouverture d’un AlertDialog. Les autres statuts contenus restent informatifs.

### Dialogs, loading and fixed actions

Suppression, confidentialité et explication de capacité utilisent AlertDialog. La suppression distingue annulation et action destructrice rouge. Busy associe l’indicateur (24 dp, trait de 2 dp) au libellé annoncé poliment ; l’évaluation peut employer l’indicateur compact (18 dp, trait de 2 dp). Confirmation et ouverture de source passent par le pied de Page, au-dessus des barres système.

## Do's and Don'ts

### Do:

- **Do** utiliser les rôles de thème et les couleurs sémantiques pour chaque état.
- **Do** conserver texte et icône sur les statuts, y compris en mode sombre.
- **Do** réutiliser Page, SectionCard, CollectionCard, MetaRow et SecondaryText pour les nouveaux écrans.
- **Do** vérifier le défilement, le clavier, les barres système et la lisibilité avec une échelle de texte de 1,3.
- **Do** conserver les composants, cibles tactiles et interactions natives Material 3.
- **Do** laisser les ListItem gérer leurs insets dans CollectionCard, avec des séparateurs sans espacement ajouté.

### Don't:

- **Don’t** réintroduire la palette canard rejetée ou le violet par défaut pour les actions, ni utiliser le vert pour la préparation.
- **Don’t** remplacer la typographie système par une police de marque ou ajouter une direction fantaisiste.
- **Don’t** utiliser uniquement une couleur pour annoncer un état, une limite ou une erreur.
- **Don’t** ajouter des ombres décoratives ou empiler les retraits autour des lignes natives.
- **Don’t** présenter les anciens écrans de démonstration ou les captures tablette historiques comme la référence de cette V1 téléphone.

- **Don’t** déduire une parité APK/web ou une qualification tablette des aperçus HTML et fixtures téléphone.
