# Identité V1 — Préviens-moi

Logo validé le 20 novembre 2026 : P avec une cloche en réserve, sans point de notification. Couleurs de l’application : vert graphite `#303D34` et blanc cassé `#F6F6F0`. Aucun dégradé ni texture.

- `logo.svg` : original vectoriel et cadrage de référence (72 × 72).
- `logo-monochrome.svg` : silhouette blanche avec fond transparent.
- `play-store-512.png` : export sRGB, RGBA 32 bits, 512 × 512, fond opaque plein carré, sans ombre ni coins arrondis. Destiné à la fiche Google Play ; sa présence dans Git ne le publie pas.

Android utilise des ressources vectorielles natives issues du même tracé : premier plan adaptatif sur 108 dp, fond séparé, silhouette monochrome pour les icônes thématiques, repli vectoriel pour API 24–25 et petite icône de notification blanche transparente. Le symbole entier reste dans le cercle central de sécurité de 66 dp ; le système choisit le masque et les couleurs de l’icône thématique. L’icône de notification ne reprend pas le fond carré.

Références : [icônes adaptatives Android](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [spécifications Google Play](https://developer.android.com/distribute/google-play/resources/icon-design-specifications).
