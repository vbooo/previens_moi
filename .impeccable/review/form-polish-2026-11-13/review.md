# Revue des formulaires — 13 novembre 2026

Périmètre : finitions utiles seulement, interface native Material 3 existante conservée. Dix captures après correction : e-mail, code, demande, site et région, chacune en clair 100 % et sombre 130 %. `before-keyboard-pan.png` est une preuve AVANT le réglage, pas le rendu final. Capture native API36 / 1080×1920 / densité440, serveur HTTP local contrôlé.

Deux rondes auteur : première détection du déplacement de fenêtre en texte agrandi ; seconde confirmation après `adjustResize`. Aucun détecteur Web applicable.

Revue indépendante fraîche : **ship**.
- persistence : handlers bouton/IME gardés, retrait du focus et fermeture du clavier sur validation valide, clé stable des résultats présents dans le code.
- fidelity : les dix captures inspectées séparément, style/copie/espacement conservés ; barres du haut visibles et champs atteignables. Sélection et notices Gboard appartiennent au système.
- ceiling : deux rondes achevées, pas de nouveau cycle de finition justifié.
- material_fixes : aucun ; ni reconstruction ni recapture nécessaire.
- keep : comportement natif, réglage `adjustResize`, défilement et clés utiles ; préserver palette et composants.

Documenter : **préserver DESIGN.md et .impeccable/design.json**. Pas de nouveau token, composant ou recette visuelle ; les règles existantes d’insets et de saisie suffisent. Aucun changement normatif ou réparation de dérive.
