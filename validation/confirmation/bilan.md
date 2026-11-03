# Confirmation allégée — 3 novembre 2026

La version connectée **0.1.8 (9)** conserve seulement le titre dans le résumé de confirmation. L’action d’information à côté du titre ouvre un volet natif Material 3 : critères détaillés, fonctionnement au démarrage et limites. Les deux paragraphes d’explication n’occupent plus le bas de la page. Les réglages, choix de notification, actions fixes, erreurs et consentement explicite à une portée partielle restent directement accessibles.

Le volet utilise ModalBottomSheet officiel, son défilement et sa fermeture native. L’action possède un libellé accessible et une cible native de 48 dp ; une croix permet de fermer le volet. Les informations sont reformulées en français et en anglais. La phrase ambiguë « sans connexion » est remplacée par « pages publiques, accessibles sans compte ». Aucun critère enregistré, quota ou comportement du moteur ne change.

## Vérifications

- 65 tests unitaires remote, 82 demo et 13 parcours natifs remote uniques passent : sept parcours fonctionnels avec la vraie activité et ses repositories HTTP contrôlés, un parcours visuel intégré et cinq tests de composants.
- Le parcours du volet est répété en sombre et à 130 % de texte. Ouverture, croix, Retour Android, recréation et confirmation unique sont vérifiés. Les deux répétitions ne sont pas comptées comme de nouveaux tests uniques.
- Six captures natives API 36 : confirmation fermée et volet ouvert dans les trois présentations. Revue indépendante et verdict après correction de la phrase : **ship**, sans correction matérielle restante dans ce périmètre.
- Compilation des deux APK, Lint sans erreur ni avertissement, frontières Android et diff vérifiés. Les suggestions de mise à jour restent informatives.
- APK cloud construit sans service local ; licence Material Symbols et absence des valeurs littérales de secrets serveur connus contrôlées. Ce contrôle n’est pas un audit de sécurité général.

Le harnais attend l’ouverture réelle du volet avant Retour et fait défiler les champs au texte agrandi. Ses actions de boutons passent par les sémantiques visibles/activées pour éviter une course de coordonnées pendant l’animation du clavier Android ; cela ne modifie pas l’application. Un délai intermittent de soumission a été observé avant cette stabilisation, sans appel d’analyse déclenché. Les exécutions finales passent. Les captures utilisent des fixtures locales, dont des textes concert/PS5 volontairement distincts ; elles ne prouvent pas une offre ou une recherche réelle.

Le système visuel existant est conservé ; seule la recette du volet d’information est ajoutée à DESIGN.md et au sidecar. Composant et icône officiels : [ModalBottomSheet](https://developer.android.com/develop/ui/compose/components/bottom-sheets), [tooltips](https://developer.android.com/develop/ui/compose/components/tooltip), [Material Symbols info](https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/info/materialsymbolsoutlined/info_24px.svg). Le tracé SVG est conservé ; une translation verticale adapte son repère au vector drawable Android, sous la licence Apache 2.0 existante.

Les [preuves détaillées](checks.json) identifient captures, APK et limites des essais. Installer la 0.1.8 par-dessus l’application existante pour conserver ses données. L’essai OnePlus et les étapes de diffusion du plan restent ouverts.

Coût fournisseur supplémentaire : **0 €**. Plafond de **2 €**, quotas et serveur inchangés ; aucun push GitHub.
