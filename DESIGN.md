---
name: "Préviens-moi — Android 0.1.6"
description: "Android natif Material 3, graphite et ambre, surfaces neutres chaudes et états sémantiques explicites."
colors:
  light-primary: "#303D34"
  light-on-primary: "#FFFFFF"
  light-primary-container: "#E0E8DD"
  light-on-primary-container: "#26352A"
  light-secondary: "#586456"
  light-on-secondary: "#FFFFFF"
  light-secondary-container: "#E7EBE2"
  light-on-secondary-container: "#303D34"
  light-tertiary: "#765B14"
  light-on-tertiary: "#FFFFFF"
  light-tertiary-container: "#F4D38B"
  light-on-tertiary-container: "#302406"
  light-background: "#F6F6F0"
  light-on-background: "#222820"
  light-surface: "#F6F6F0"
  light-on-surface: "#222820"
  light-surface-container-lowest: "#FFFFFF"
  light-surface-container-low: "#F0F1E9"
  light-surface-container: "#ECEEE4"
  light-surface-container-high: "#E5E8DD"
  light-surface-container-highest: "#DFE3D7"
  light-on-surface-variant: "#5C645A"
  light-outline: "#767E71"
  light-outline-variant: "#D8DED1"
  light-error: "#BA1A1A"
  light-on-error: "#FFFFFF"
  light-error-container: "#FFDAD6"
  light-on-error-container: "#410002"
  light-status-active: "#186544"
  light-status-preparing: "#805600"
  light-status-paused: "#555F52"
  light-status-warning: "#805600"
  light-status-active-container: "#E1F2E7"
  light-status-preparing-container: "#FFF0C9"
  light-status-paused-container: "#E8EDE3"
  light-status-warning-container: "#FFF0C9"
  dark-primary: "#DCE6D8"
  dark-on-primary: "#243022"
  dark-primary-container: "#354132"
  dark-on-primary-container: "#DAE5D6"
  dark-secondary: "#C0CBBA"
  dark-on-secondary: "#2B3627"
  dark-secondary-container: "#364332"
  dark-on-secondary-container: "#DFE9D8"
  dark-tertiary: "#E5C56F"
  dark-on-tertiary: "#3C2E04"
  dark-tertiary-container: "#544319"
  dark-on-tertiary-container: "#FFE4A3"
  dark-background: "#171C17"
  dark-on-background: "#E0E6DB"
  dark-surface: "#171C17"
  dark-on-surface: "#E0E6DB"
  dark-surface-container-lowest: "#111611"
  dark-surface-container-low: "#202620"
  dark-surface-container: "#252C24"
  dark-surface-container-high: "#2C342B"
  dark-surface-container-highest: "#374035"
  dark-on-surface-variant: "#BFC9B9"
  dark-outline: "#899582"
  dark-outline-variant: "#414B3D"
  dark-error: "#FFB4AB"
  dark-on-error: "#690005"
  dark-error-container: "#93000A"
  dark-on-error-container: "#FFDAD6"
  dark-status-active: "#98DBB7"
  dark-status-preparing: "#ECCC83"
  dark-status-paused: "#C3CEC0"
  dark-status-warning: "#ECCC83"
  dark-status-active-container: "#223F2E"
  dark-status-preparing-container: "#43371C"
  dark-status-paused-container: "#343E31"
  dark-status-warning-container: "#43371C"
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
  title-large-semibold:
    fontFamily: "sans-serif"
    fontSize: "22sp"
    fontWeight: 600
    lineHeight: "28sp"
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
  headline-small-semibold:
    fontFamily: "sans-serif"
    fontSize: "24sp"
    fontWeight: 600
    lineHeight: "32sp"
    letterSpacing: "0sp"
rounded:
  extra-small: "4dp"
  small: "8dp"
  medium: "12dp"
  large: "16dp"
  grouped: "20dp"
  extra-large: "28dp"
  full: "50%"
spacing:
  inline-tight: "4dp"
  metadata: "6dp"
  inline: "8dp"
  status-horizontal: "10dp"
  compact: "12dp"
  section: "16dp"
  home-top: "12dp"
  page: "20dp"
  page-bottom: "24dp"
components:
  button-primary-light:
    backgroundColor: "{colors.light-primary}"
    textColor: "{colors.light-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "8dp 24dp"
  button-page-light:
    backgroundColor: "{colors.light-primary}"
    textColor: "{colors.light-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    width: "100%"
    height: "48dp"
  button-confirm-light:
    backgroundColor: "{colors.light-primary}"
    textColor: "{colors.light-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    width: "100%"
    height: "52dp"
  button-tonal-light:
    backgroundColor: "{colors.light-primary-container}"
    textColor: "{colors.light-on-primary-container}"
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
    backgroundColor: "{colors.light-tertiary-container}"
    textColor: "{colors.light-on-tertiary-container}"
    typography: "{typography.label-large}"
    rounded: "{rounded.large}"
    height: "56dp"
  field-outlined-light:
    backgroundColor: "{colors.light-surface-container-lowest}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.body-large}"
    rounded: "{rounded.medium}"
    width: "100%"
  watch-summary-light:
    backgroundColor: "{colors.light-surface-container}"
    textColor: "{colors.light-on-surface}"
    rounded: "{rounded.grouped}"
    padding: "{spacing.section}"
    width: "100%"
  proposal-card-light:
    backgroundColor: "{colors.light-primary-container}"
    textColor: "{colors.light-on-primary-container}"
    typography: "{typography.headline-small-medium}"
    rounded: "{rounded.large}"
    padding: "{spacing.page}"
    width: "100%"
  section-card-light:
    backgroundColor: "{colors.light-surface-container-lowest}"
    textColor: "{colors.light-on-surface}"
    rounded: "{rounded.large}"
    padding: "{spacing.section}"
    width: "100%"
  collection-card-light:
    backgroundColor: "{colors.light-surface-container-lowest}"
    textColor: "{colors.light-on-surface}"
    rounded: "{rounded.large}"
    width: "100%"
  grouped-watch-list-light:
    backgroundColor: "{colors.light-surface-container-lowest}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.title-medium-semibold}"
    rounded: "{rounded.grouped}"
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
    backgroundColor: "{colors.light-status-active-container}"
    textColor: "{colors.light-status-active}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    padding: "7dp 10dp"
  watch-status-preparing-light:
    backgroundColor: "{colors.light-status-preparing-container}"
    textColor: "{colors.light-status-preparing}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    padding: "7dp 10dp"
  watch-status-paused-light:
    backgroundColor: "{colors.light-status-paused-container}"
    textColor: "{colors.light-status-paused}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    padding: "7dp 10dp"
  watch-status-warning-light:
    backgroundColor: "{colors.light-status-warning-container}"
    textColor: "{colors.light-status-warning}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    padding: "7dp 10dp"
  watch-status-compact-light:
    typography: "{typography.label-medium}"
    rounded: "{rounded.small}"
    padding: "5dp 10dp"
  capacity-chip-light:
    backgroundColor: "{colors.light-status-warning-container}"
    textColor: "{colors.light-status-warning}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    height: "48dp"
  top-app-bar-home-light:
    backgroundColor: "{colors.light-background}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.title-large-semibold}"
  top-app-bar-light:
    backgroundColor: "{colors.light-background}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.title-large}"
  list-item-light:
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
  button-page-dark:
    backgroundColor: "{colors.dark-primary}"
    textColor: "{colors.dark-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    width: "100%"
    height: "48dp"
  button-confirm-dark:
    backgroundColor: "{colors.dark-primary}"
    textColor: "{colors.dark-on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.medium}"
    width: "100%"
    height: "52dp"
  button-tonal-dark:
    backgroundColor: "{colors.dark-primary-container}"
    textColor: "{colors.dark-on-primary-container}"
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
    backgroundColor: "{colors.dark-tertiary-container}"
    textColor: "{colors.dark-on-tertiary-container}"
    typography: "{typography.label-large}"
    rounded: "{rounded.large}"
    height: "56dp"
  field-outlined-dark:
    backgroundColor: "{colors.dark-surface-container-lowest}"
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.body-large}"
    rounded: "{rounded.medium}"
    width: "100%"
  watch-summary-dark:
    backgroundColor: "{colors.dark-surface-container}"
    textColor: "{colors.dark-on-surface}"
    rounded: "{rounded.grouped}"
    padding: "{spacing.section}"
    width: "100%"
  proposal-card-dark:
    backgroundColor: "{colors.dark-primary-container}"
    textColor: "{colors.dark-on-primary-container}"
    typography: "{typography.headline-small-medium}"
    rounded: "{rounded.large}"
    padding: "{spacing.page}"
    width: "100%"
  section-card-dark:
    backgroundColor: "{colors.dark-surface-container-lowest}"
    textColor: "{colors.dark-on-surface}"
    rounded: "{rounded.large}"
    padding: "{spacing.section}"
    width: "100%"
  collection-card-dark:
    backgroundColor: "{colors.dark-surface-container-lowest}"
    textColor: "{colors.dark-on-surface}"
    rounded: "{rounded.large}"
    width: "100%"
  grouped-watch-list-dark:
    backgroundColor: "{colors.dark-surface-container-lowest}"
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.title-medium-semibold}"
    rounded: "{rounded.grouped}"
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
    backgroundColor: "{colors.dark-status-active-container}"
    textColor: "{colors.dark-status-active}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    padding: "7dp 10dp"
  watch-status-preparing-dark:
    backgroundColor: "{colors.dark-status-preparing-container}"
    textColor: "{colors.dark-status-preparing}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    padding: "7dp 10dp"
  watch-status-paused-dark:
    backgroundColor: "{colors.dark-status-paused-container}"
    textColor: "{colors.dark-status-paused}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    padding: "7dp 10dp"
  watch-status-warning-dark:
    backgroundColor: "{colors.dark-status-warning-container}"
    textColor: "{colors.dark-status-warning}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    padding: "7dp 10dp"
  watch-status-compact-dark:
    typography: "{typography.label-medium}"
    rounded: "{rounded.small}"
    padding: "5dp 10dp"
  capacity-chip-dark:
    backgroundColor: "{colors.dark-status-warning-container}"
    textColor: "{colors.dark-status-warning}"
    typography: "{typography.label-large}"
    rounded: "{rounded.small}"
    height: "48dp"
  top-app-bar-home-dark:
    backgroundColor: "{colors.dark-background}"
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.title-large-semibold}"
  top-app-bar-dark:
    backgroundColor: "{colors.dark-background}"
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.title-large}"
  list-item-dark:
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.title-medium}"
    width: "100%"
  dialog-dark:
    backgroundColor: "{colors.dark-surface-container-high}"
    textColor: "{colors.dark-on-surface}"
    rounded: "{rounded.extra-large}"
    padding: "24dp"
  confirmation-info-action-light:
    textColor: "{colors.light-on-primary-container}"
    width: "48dp"
    height: "48dp"
  confirmation-info-action-dark:
    textColor: "{colors.dark-on-primary-container}"
    width: "48dp"
    height: "48dp"
  confirmation-info-sheet-light:
    backgroundColor: "{colors.light-surface-container-low}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.body-medium}"
    padding: "0 24dp 24dp"
    width: "100%"
  confirmation-info-sheet-dark:
    backgroundColor: "{colors.dark-surface-container-low}"
    textColor: "{colors.dark-on-surface}"
    typography: "{typography.body-medium}"
    padding: "0 24dp 24dp"
    width: "100%"
  home-empty-light:
    backgroundColor: "{colors.light-primary-container}"
    textColor: "{colors.light-on-primary-container}"
    rounded: "{rounded.large}"
    padding: "24dp"
    width: "100%"
  support-sheet-light:
    backgroundColor: "{colors.light-surface-container-lowest}"
    textColor: "{colors.light-on-surface}"
    padding: "0 24dp 24dp"
    width: "100%"
  support-entry-light:
    backgroundColor: "{colors.light-secondary-container}"
    textColor: "{colors.light-on-secondary-container}"
    rounded: "{rounded.large}"
    height: "56dp"
  support-category-light:
    backgroundColor: "{colors.light-secondary-container}"
    textColor: "{colors.light-on-secondary-container}"
    height: "48dp"
  support-category-selected-light:
    backgroundColor: "{colors.light-primary}"
    textColor: "{colors.light-on-primary}"
    height: "48dp"
  support-field-light:
    backgroundColor: "{colors.light-surface-container-low}"
    textColor: "{colors.light-on-surface}"
    rounded: "{rounded.medium}"
    width: "100%"
  home-empty-dark:
    backgroundColor: "{colors.dark-primary-container}"
    textColor: "{colors.dark-on-primary-container}"
    rounded: "{rounded.large}"
    padding: "24dp"
    width: "100%"
  support-sheet-dark:
    backgroundColor: "{colors.dark-surface-container-lowest}"
    textColor: "{colors.dark-on-surface}"
    padding: "0 24dp 24dp"
    width: "100%"
  support-entry-dark:
    backgroundColor: "{colors.dark-secondary-container}"
    textColor: "{colors.dark-on-secondary-container}"
    rounded: "{rounded.large}"
    height: "56dp"
  support-category-dark:
    backgroundColor: "{colors.dark-secondary-container}"
    textColor: "{colors.dark-on-secondary-container}"
    height: "48dp"
  support-category-selected-dark:
    backgroundColor: "{colors.dark-primary}"
    textColor: "{colors.dark-on-primary}"
    height: "48dp"
  support-field-dark:
    backgroundColor: "{colors.dark-surface-container-low}"
    textColor: "{colors.dark-on-surface}"
    rounded: "{rounded.medium}"
    width: "100%"
---

# Design System: Préviens-moi

## Overview

**Creative North Star: "Material 3 natif, graphite et ambre"**

Préviens-moi adopte les composants conventionnels Jetpack Compose Material 3, la police système Android et une palette graphite légèrement végétale, sur des surfaces neutres chaudes. Le FAB ambre distingue la création ; les états restent séparés des actions. La sobriété et les composants officiels choisis par Fabien gouvernent cette première version.

Cette extraction documente le système intégré dans la production Android 0.1.6, après approbation des maquettes le 2 novembre 2026. Les couleurs viennent de PreviensTheme.kt, les compositions des composants Kotlin main et remote. La typographie et les formes héritées ont été vérifiées dans les sources de Material 3 résolu (1.4.0) ; aucune nouvelle police, durée ou élévation applicative n’est introduite. Les unités restent en dp et sp ; les aperçus HTML du sidecar sont consultatifs et ne remplacent pas Android natif.

La confirmation reçoit une extension locale en Android 0.1.8 : les informations facultatives passent dans une feuille modale Material 3 ouverte à la demande. Cette recette prolonge le système accepté sans modifier sa palette, sa typographie ou ses formes ; les composants et tokens antérieurs restent la référence.

**Key Characteristics:**

- Composants officiels Material 3 et typographie système.
- Actions graphite, création ambre et statuts sémantiques indépendants.
- Liste groupée, titre de détail hors du résumé et surfaces calmes.
- Dates adaptatives, défilement et actions fixes lisibles avec le texte agrandi.

La référence visuelle courante est `.impeccable/review/approved-ui-2026-11-02/` : 28 captures Android natives téléphone API 36 (clair, sombre et texte à 130 %), comparées aux neuf références individuelles de `.impeccable/review/proposal-2026-11-02/`. La revue finale a conclu SHIP sans correction matérielle. Cette couverture ne vaut pas exercice TalkBack, mesure de performance matérielle ni qualification tablette, réseau ou moteur externe. Les sources Kotlin restent l’autorité des tokens. `PRODUCT.md` conserve un contexte historique de démonstration ; ses contraintes fonctionnelles et celles de la documentation produit ne sont pas modifiées ici.

## Colors

Les préfixes `light-` et `dark-` du frontmatter décrivent les deux schémas statiques. Le thème suit Android ; Dynamic Color n’est pas activé. Les valeurs absentes de PreviensTheme restent héritées de Material et ne sont pas inventées ici.

### Primary

Le **graphite végétal** porte les boutons remplis, les catégories de support sélectionnées et les actions textuelles. `primary-container` et `on-primary-container` portent la pause ou l’annulation de préparation, le choix radio sélectionné et le résumé de proposition, le panneau d’accueil vide et les surfaces d’en-tête ou de réception du support. Ces rôles expriment l’action, sans reprendre la couleur d’un statut.

### Secondary

Les **neutres feuillus** secondaires servent au bouton tonal du compte, à la connexion dans la barre d’accueil, à l’accès au support et aux catégories de support non sélectionnées. La pause possède une surcharge explicite en `primary-container` ; elle n’utilise pas le conteneur secondaire par défaut.

### Tertiary

L’**ambre doux** du conteneur tertiaire porte le seul FAB étendu « Nouveau suivi ». La préparation et l’attention ont leurs propres tokens sémantiques : ne pas les déduire du FAB.

### Neutral

Le **papier chaud** clair et le **graphite sombre** structurent les fonds. `background` et `surface` coïncident dans chaque schéma. Le résumé de détail utilise `surface-container` ; sections, collections, champs et pied fixe utilisent `surface-container-lowest` (blanc en clair). `on-surface` porte le texte principal ; `on-surface-variant` les métadonnées. Séparateurs et contours utilisent respectivement `outline-variant` et le rôle natif du composant.

### Status and error

| Tokens | Usage réel |
| --- | --- |
| `status-active` / `status-active-container` | Vert : Actif confirmé et Terminé, avec coche. |
| `status-preparing` / `status-preparing-container` | Ambre : préparation et opérations en attente, avec horloge. |
| `status-paused` / `status-paused-container` | Neutre : pause, avec symbole pause. |
| `status-warning` / `status-warning-container` | Ambre : capacité, arrêt en cours ou actif non confirmé. |
| `error` / `error-container` et leurs `on-` | Rouge : préparation échouée, erreurs et suppression. |

Les fonds de statut sont des couleurs explicites du thème, pas une opacité calculée depuis le texte. WatchStatusLabel donne priorité à une opération en attente, puis à l’échec, puis à l’arrêt ou à l’actif non vérifié (prochaine vérification absente ou santé non saine), avant le statut nominal. La capacité donne accès à une explication via AssistChip. FailureText garde les attentes normales en texte secondaire et les erreurs en rouge, avec annonce polie.

**The Semantic Color Rule.** Employer MaterialTheme.colorScheme et LocalStatus ; conserver libellé et icône avec la couleur. Le vert indique Actif ou Terminé, l’ambre la préparation ou l’attention, le neutre la pause et le rouge l’échec.

## Typography

**Display Font:** police sans-serif système Android via Material 3 ; aucun rôle Display n’est utilisé dans les écrans inspectés.

**Body Font:** la même famille système, sans police embarquée ou décorative ni famille mono dédiée.

**Character:** hiérarchie native et mesurée. Le frontmatter extrait les rôles utilisés de la bibliothèque résolue et les variantes de graisse réellement observées ; il ne définit pas un thème typographique personnalisé. Conserver les unités sp et le retour à la ligne.

| Rôle | Usage observé |
| --- | --- |
| `headlineMedium` | Certains états de connexion. |
| `headlineSmall` | Question de création, détail, résultat, clarification et proposition ; SemiBold pour les titres principaux, l’accueil vide et le support, Medium pour la proposition. |
| `titleLarge` | Compte et barres de contexte compactes ; SemiBold pour la barre d’accueil. |
| `titleMedium` | Titres de section et suivis d’accueil, avec emphase SemiBold. |
| `bodyLarge` | Champs et fait principal d’un résultat. |
| `bodyMedium` | Métadonnées et explications ; Medium pour certaines valeurs de réglage. |
| `bodySmall` | Labels de dates du résumé et note d’essai. |
| `labelLarge` | Actions, statuts normaux, compteur et initiales du compte. |
| `labelMedium` | Statuts compacts et labels de réglage. |

Les titres longs de détail et résultat utilisent `LineBreak.Heading`. Les titres de suivis dans la liste acceptent deux lignes puis une ellipse ; les titres de barre gardent une ligne avec ellipse. Aucun ratio typographique supplémentaire n’est imposé.

**The Material Type Roles Rule.** Réutiliser MaterialTheme.typography et les emphases observées ; laisser Android appliquer son échelle de texte sans taille fixe par écran.

## Layout

Page centre une colonne défilante de largeur maximale (720 dp), avec marges horizontales (20 dp), retrait supérieur (12 dp), retrait inférieur (24 dp) et espacement de section (16 dp). Son pied reste hors du défilement avec marges horizontales (20 dp), retraits supérieur (12 dp) et inférieur (8 dp). Les hauteurs de composants du frontmatter représentent les minima observés ou natifs : ne pas transformer `heightIn(min=…)` en hauteur fixe.

L’accueil conserve LazyColumn avec marges latérales (20 dp), retrait supérieur (12 dp), réserve inférieure pour le FAB (112 dp) et aucun espace entre lignes. Les ListItem sont groupés sur une surface continue ; seuls les coins extérieurs du premier et dernier item sont arrondis (20 dp). Les lignes utilisent un retrait local horizontal (4 dp) et vertical (12 dp), puis leurs insets Material ; le séparateur est rentré (20 dp). Statut et dernier contrôle sont côte à côte seulement lorsque la largeur interne atteint (352 dp), fontScale ne dépasse pas (1,15) et que le suivi actif est sain sans commande non confirmée ; sinon ils s’empilent.

Le détail place son titre hors d’un résumé neutre arrondi (20 dp), avec retrait interne (16 dp) et espacement (12 dp). CheckTiming partage l’espace en deux colonnes égales séparées de (16 dp), puis empile les dates au-delà de fontScale (1,15), avec espacement (12 dp). MetaRow et le réglage des notifications s’empilent également au-delà de ce seuil. Les dates compactes gardent l’année pour une observation d’une autre année.

SectionCard et CollectionCard placent leur titre au-dessus de la surface, séparé de (10 dp). SectionCard applique un retrait interne (16 dp) et un espacement (12 dp). CollectionCard laisse les ListItem gérer leurs insets, avec séparateurs directs et sans retrait doublé. Les sources gardent un minimum (56 dp). La confirmation conserve résumé, comportement après résultat puis réglages ; les choix radio ont un minimum (52 dp), les lignes de réglage (56 dp).

Scaffold applique ses insets avant le contenu avec imePadding ; l’activité active l’affichage bord à bord. Les icônes de barres système suivent le thème. Les icônes communes mesurent (24 dp) ; cette taille n’est pas leur cible tactile. Conserver les contrôles natifs et une cible interactive minimale de (48 × 48 dp). Le plafond de largeur ne constitue pas une qualification tablette et aucun breakpoint tablette n’est établi.

## Elevation & Depth

La profondeur repose sur les surfaces tonales et les composants Material. Les cartes remplies n’ajoutent aucune ombre de marque. Le pied de Page utilise une élévation tonale explicite (2 dp) ; le FAB garde l’élévation native de repos (6 dp), vérifiée dans la bibliothèque. Menus et dialogues conservent leurs défauts natifs.

**The Native Elevation Rule.** Conserver la profondeur des surfaces et composants Material ; aucune ombre décorative sur les cartes ou listes.

Aucun token de durée, d’easing ou d’ombre applicative supplémentaire n’est défini. Ripple, focus, états pressé/désactivé et transitions restent ceux des composants Android. Le sidecar ne revendique aucune équivalence de ces comportements dans le navigateur.

## Shapes

Les formes héritées sont celles de Material 3 : `small` pour les statuts contenus, `medium` pour les champs, boutons de page, choix radio et reprise, `large` pour sections, collections, proposition et FAB, `extra-large` pour dialogues, `full` pour les boutons ordinaires et textuels. `grouped` est la forme locale (20 dp) du résumé de détail et des coins extérieurs de la liste groupée. `extra-small` est observé sur les barres du squelette. Le thème ne surcharge pas Shapes.

## Components

### Buttons

Button en graphite porte analyse, connexion, confirmation, ouverture de source et reprise ; la forme des boutons de page est `medium`. FilledTonalButton en `primary-container` porte pause ou annulation de préparation. Ces actions ont un minimum (48 dp), sauf confirmation (52 dp). Le bouton de confirmation et l’ouverture de source sont dans le pied fixe de Page, suivi de l’action textuelle de révision pour la confirmation. TextButton porte les actions légères ; les confirmations de suppression utilisent `error`. Conserver les états natifs et laisser les labels revenir à la ligne.

Le seul FAB est l’ExtendedFloatingActionButton ambre de l’accueil, avec symbole plus et « Nouveau suivi ». Son affichage attend le chargement du compte et du brouillon ; il apparaît hors connexion, avec une liste non vide ou en cas d’échec de chargement. L’accueil connecté vide propose la création dans son panneau, avec Button primaire de forme `medium` et minimum (52 dp), sans doubler le FAB. L’accès au support reste un FilledTonalButton secondaire de forme `large`, minimum (56 dp), avec symbole `info` et espace icône/texte (8 dp), ancré en bas à gauche (16 dp). Aucun OutlinedButton n’est utilisé dans les sources inspectées.

### Inputs and choices

OutlinedTextField utilise `surface-container-lowest` à l’état normal et focalisé, avec forme `medium` (12 dp). La demande accepte de trois à six lignes ; zone, site, email et code sont monolignes. Labels, supportingText du site, isError, clavier et focus restent natifs. ModeChoice place un RadioButton sans clic propre dans une ligne selectable de rôle radio ; l’état sélectionné utilise `primary-container` / `on-primary-container`. Les clarifications réemploient les radios natives. FailureText annonce poliment ses messages.

### Navigation

WatchAppBar utilise TopAppBar compact sur toutes les routes, sur `background`. Le titre d’accueil emploie `titleLarge` SemiBold ; les autres titres gardent `titleLarge` normal. La barre d’accueil contient actualisation et compte ; le détail contient retour, actualisation et menu d’actions. Le bouton tonal du compte affiche les initiales dérivées du véritable email : jusqu’à deux segments de sa partie locale séparés par point, tiret bas ou tiret, avec « ? » si vide. Les initiales d’une maquette ne sont jamais figées en production. Hors connexion, un FilledTonalButton de connexion le remplace. Le retour conserve la pile NavDisplay et le retour système ; aucune destination inférieure n’est ajoutée.

### Cards and collections

Le résumé neutre du détail réunit statut, dates, explication utile et action disponible ; le titre principal reste au-dessus. La proposition conserve son conteneur primaire. Les résultats et sources sont des collections sur `surface-container-lowest`, les critères une SectionCard sur ce même rôle. Les ListItem transparents montrent la surface de leur collection ; résultats portent un chevron, sources une ouverture externe. Réutiliser sourceDomain pour les domaines.

### Home empty state

Le panneau vide commence dans le flux normal de l’accueil, sans grand retrait supérieur ajouté. Il emploie `primary-container` / `on-primary-container`, forme `large`, retraits (24 dp) et espacement interne (12 dp). Le titre utilise `headlineSmall` SemiBold avec `LineBreak.Heading` ; le texte utilise `bodyMedium`. Le bouton primaire pleine largeur ajoute un retrait supérieur (8 dp), garde une hauteur minimale (52 dp) et laisse son libellé grandir. Hors connexion, cette même recette conduit à la connexion.

### Support form

SupportSheet réemploie ModalBottomSheet avec `skipPartiallyExpanded = true`, les insets natifs, imePadding et une colonne défilante plafonnée (720 dp). Son fond est `surface-container-lowest`, ses retraits horizontaux et inférieur (24 dp), son espacement (16 dp). L’en-tête associe une icône `info` dans un carré primaire tonal de forme `medium` (48 dp), un titre `headlineSmall` SemiBold et la fermeture native ; l’espace d’en-tête est (12 dp).

Les trois catégories utilisent FilterChip natif dans FlowRow, avec espaces horizontaux et verticaux (8 dp) et minimum tactile (48 dp). Le fond non sélectionné est `secondary-container` / `on-secondary-container` ; la sélection utilise `primary` / `on-primary` et une icône de confirmation. Les OutlinedTextField ont une forme `medium`, `surface-container-low` au repos et `surface-container-lowest` au focus. Le message accepte quatre à sept lignes ; le champ email reste monoligne. Labels, texte d’aide, compteur et focus restent natifs.

L’envoi et la fermeture après réception utilisent Button primaire pleine largeur de forme `medium`, minimum (52 dp), avec états désactivé et occupé natifs. L’envoi montre le symbole `send` ou un indicateur de progression. L’erreur conserve une surface `error-container` / `on-error-container` de forme `medium`, avec icône et annonce polie ; la réception utilise une surface `primary-container` / `on-primary-container` de forme `large`, avec icône de confirmation, retraits (20 dp), espacement (12 dp) et annonce polie. Ces recettes restent locales au support et ne modifient pas les autres champs ou statuts.

### Confirmation information

La carte de proposition affiche le titre seul, sans sous-titre répété, et une action d’information à droite. Le titre garde `headlineSmall` Medium et son retour à la ligne ; la carte garde `primary-container` / `on-primary-container`, sa forme `large` et son retrait (20 dp). L’action est un IconButton natif à cible (48 × 48 dp), avec le symbole officiel Material Symbols Outlined `info` (24 dp) et un libellé accessible localisé (« Informations sur ce suivi » en français). Le symbole réutilise le SVG Google sous Apache 2.0, sans police d’icônes embarquée.

L’action ouvre `ConfirmationInfoSheet`, un ModalBottomSheet Material 3 avec `skipPartiallyExpanded = true`. Surface, coins supérieurs `extra-large`, poignée, voile, animation et comportement de retour restent natifs. Le conteneur hérite de `surface-container-low` / `on-surface` ; aucun nouveau token de couleur, d’ombre ou de mouvement n’est introduit. Son contenu est une colonne défilante, avec retraits horizontaux et inférieur (24 dp) et espacement (16 dp). Le `padding` du frontmatter décrit ces retraits de contenu ; il n’ajoute pas un retrait supérieur uniforme. La feuille garde les insets système Material.

Le titre « À propos de ce suivi » utilise `titleLarge`, avec une action fermer native à cible (48 × 48 dp), symbole `close` et libellé localisé. La fermeture est disponible par ce bouton, le retour Android ou un appui hors de la feuille. Les titres de section réemploient SectionTitle (`titleMedium` SemiBold) et le texte `bodyMedium`, sans hauteur fixe ni troncature. Le titre et le bouton de fermeture font partie du contenu défilant.

Les sections sont « Ta demande en détail » uniquement si `criteriaForDisplay(proposal)` fournit des critères, « Au démarrage » selon la méthode de suivi, puis « Ce que couvre le suivi ». Pour `web_search`, le texte explique l’affichage des résultats déjà présents dès que le suivi est prêt ; pour les autres méthodes, il explique la première lecture comme point de départ des changements. Le texte de couverture précise « Nous consultons des pages publiques, accessibles sans compte. », puis l’absence de couverture de tous les sites et le refus ou la suspension possibles à la limite d’utilisation du service. Les ressources anglaises et françaises restent localisées ; la formulation « sans compte » décrit l’accès aux pages consultées.

Cette feuille contient le contexte facultatif. Le consentement à une couverture partielle et FailureText restent directement dans la page, avec les réglages, les choix après résultat et le pied fixe de confirmation. Ne pas masquer une décision requise ou une erreur d’admission derrière l’action d’information.

### Watch status and chip

WatchStatusLabel associe icône, libellé et couple sémantique. Accueil et résumé utilisent des statuts contenus : format compact (labelMedium, icône 16 dp, retrait vertical 5 dp) dans la liste ; format normal (labelLarge, icône 18 dp, retrait vertical 7 dp) dans le résumé. Le retrait horizontal est (10 dp), l’espace icône/texte (6 dp). Un statut contenu informatif n’est pas un bouton. La suspension par capacité utilise un AssistChip sans contour, minimum (48 dp), ouvrant un AlertDialog avec explication.

### Dialogs and loading

Suppression, confidentialité et capacité réemploient AlertDialog. La suppression conserve annulation et confirmation rouge. Une ligne LinearProgressIndicator apparaît sous la barre lors du chargement initial ou de l’actualisation de l’accueil, ainsi que de l’actualisation du détail. Le chargement initial sans liste conserve le squelette groupé de trois lignes et son libellé accessible. Busy est un LinearProgressIndicator pleine largeur ; l’analyse de demande et l’actualisation locale peuvent conserver un CircularProgressIndicator compact. Un échec de premier chargement montre son erreur et sa reprise ; un échec d’actualisation conserve l’indication de contenu ancien.

## Do's and Don'ts

### Do:

- **Do** utiliser les rôles clair/sombre du thème et les couples de statut explicites.
- **Do** garder le titre du suivi hors du résumé neutre et les titres de section hors de leur surface.
- **Do** conserver les marges de page de 20 dp et les insets système et clavier.
- **Do** empiler les dates et métadonnées lorsque fontScale dépasse 1,15 et laisser les composants grandir.
- **Do** garder les composants Material, leurs cibles tactiles natives d’au moins 48 × 48 dp et leurs sémantiques.
- **Do** préserver les vérités produit : état non confirmé, préparation, contenu ancien et erreurs restent explicites.

### Don't:

- **Don’t** réintroduire l’ancienne identité cobalt ni le violet par défaut dans les écrans courants.
- **Don’t** utiliser le vert pour une préparation, une opération en attente ou un état actif non confirmé.
- **Don’t** figer les initiales du compte à celles d’une maquette ni coder un hexadécimal dans un écran.
- **Don’t** ajouter une police décorative, des ombres de marque ou une direction fantaisiste.
- **Don’t** empiler les cartes ou doubler les retraits des ListItem dans les collections.
- **Don’t** déduire une qualification tablette, matérielle, réseau ou fournisseur des captures téléphone ou des aperçus HTML.


### Identité de lancement V1

Le logo est le P avec une cloche en réserve, sans point jaune. Il reprend `light-primary` (`#303D34`) sur `light-background` (`#F6F6F0`) ; la palette des écrans ne change pas. Les ressources de lancement adaptatives séparent fond et symbole et fournissent un calque monochrome, teinté par le système lorsque l’utilisateur active les icônes thématiques. Le même symbole blanc transparent sert aux petites icônes de notification. Sources et export Google Play : [assets/branding](assets/branding/README.md).
