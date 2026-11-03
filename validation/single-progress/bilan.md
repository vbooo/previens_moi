# Un seul indicateur horizontal — 3 novembre 2026

La version connectée **0.1.9 (10)** retire le doublon signalé à l’ouverture d’un détail depuis une notification. La barre supérieure apparaissait pendant que le contenu vide affichait lui aussi sa barre. Désormais, la barre supérieure du détail n’apparaît que lorsque le compte est chargé, la session présente, le propriétaire correspondant et le suivi demandé disponible. Le chargement initial garde un seul indicateur dans le contenu ; l’actualisation d’un détail déjà affiché garde une seule ligne dans la barre supérieure et conserve les données visibles.

Aucun texte, couleur, composant ou comportement métier n’est remplacé. Le petit indicateur circulaire du bouton d’actualisation existant reste inchangé ; le correctif concerne les deux barres horizontales. Les marqueurs de test ne changent pas le rendu. Les nettoyages d’imports déjà présents dans MainActivity sont conservés à part, sans les intégrer au commit du correctif.

## Vérifications

- 65 tests unitaires remote et 82 demo passent.
- Neuf parcours natifs avec la véritable activité et les repositories HTTP contrôlés passent : huit fonctionnels et un visuel intégré. Le nouveau test maintient une réponse lente après notification, vérifie un seul indicateur horizontal, libère les données, relance une actualisation puis vérifie la conservation du détail et la disparition de l’indicateur à la fin. L’ouverture directe d’un résultat depuis notification passe également.
- Le scénario ciblé est repassé en sombre. Quatre captures natives API 36 sont inspectées et revues indépendamment : **ship**, aucune correction matérielle restante dans le périmètre.
- APK des deux variantes compilés ; Lint sans erreur ni avertissement, frontières Android et diff vérifiés. Les suggestions de mise à jour restent informatives.
- APK connecté reconstruit sans service local, licence Material Symbols et absence des valeurs littérales de secrets serveur connus contrôlées. Ce contrôle n’est pas un audit de sécurité général.

Les [preuves détaillées](checks.json) identifient les captures et APK. Les données des captures sont des fixtures ; aucun push réel, appel fournisseur ou essai OnePlus n’est revendiqué. La vérification ne renouvelle pas la qualification tablette, texte agrandi ou matrice complète des versions Android.

Installer la 0.1.9 par-dessus l’ancienne version, sans désinstallation, puis ouvrir une notification de suivi. Coût fournisseur supplémentaire : **0 €** ; plafond de **2 €**, serveur et quotas inchangés. Aucun push GitHub.
