# Finitions utiles des formulaires — 13 novembre 2026

Version connectée **0.1.10 (11)**. Le lot retient uniquement les changements utiles de la proposition Gemini : clavier et identité des résultats dynamiques. Couleurs, composants, textes, espacements et architecture validés sont conservés. GitHub et sa préparation restent différés à la demande de Fabien.

- **Connexion :** la touche Valider du clavier et le bouton utilisent la même action et les mêmes conditions existantes. Une adresse sans `@` ou un code incomplet n’entraîne pas d’appel. Une soumission valide retire le focus et ferme le clavier. Aucun envoi automatique au sixième chiffre.
- **Fenêtre :** la première vérification native en texte agrandi a révélé un déplacement de toute l’activité à l’ouverture du clavier, coupant la barre du haut. `windowSoftInputMode="adjustResize"` dans le manifest corrige ce comportement et complète la gestion `imePadding()` déjà présente. Pas d’ajout de marges ni de correction par valeurs arbitraires.
- **Résultats :** `key(finding.id)` conserve l’identité des lignes lors d’insertion/réorganisation. La liste principale avait déjà ses clés ; aucune clé systématique ajoutée aux sources/options. Aucun gain de performance chiffré prétendu.

## Vérification

**147 tests unitaires** : 65 remote et 82 demo, sans échec ni test ignoré. Compilation des deux variantes, Lint sans erreur/avertissement (13 suggestions informatives par variante) et contrôle des frontières réussis.

**10 parcours natifs distincts** avec la vraie activité, DI, stockage et repositories HTTP : huit régressions existantes, deux nouveaux parcours clavier. Ces deux derniers sont aussi repassés en sombre/texte 130 %, soit 12 exécutions finales. Vérification des champs demande longue, URL, région, e-mail et code au-dessus du clavier, de la visibilité du bouton Retour et de la validation depuis le clavier. Bouton de soumission de la demande atteignable par défilement avec clavier ouvert. Aucun compte réel, e-mail, OpenAI, Firecrawl ou FCM payant sollicité.

Émulateur dédié API 36, téléphone 1080×1920/densité 440, thème clair/texte 100 % et sombre/130 %. Dix captures après correction et une preuve du déplacement avant correction sont conservées dans [la revue](../../.impeccable/review/form-polish-2026-11-13/review.md). Les menus de sélection et le bandeau de taille de police Gboard sont des éléments du système visibles dans certaines captures. Deux passes visuelles bornées, revue indépendante **ship** ; aucun autre changement demandé. Le documenter conserve DESIGN.md et son fichier associé.

APK connecté vérifié, configuration locale de test désactivée, sept valeurs serveur littérales connues absentes du binaire. Ce contrôle ciblé n’est pas un audit général de sécurité. Empreinte et contrôles dans [checks.json](checks.json). L’installation par-dessus la version précédente conserve package/signature/données ; ne pas désinstaller pour mettre à jour.

**Coût supplémentaire : 0 €**. Aucun changement serveur, quota, recherche ou dépendance. Essai du nouveau comportement sur OnePlus encore à faire ; cette vérification ne revendique pas toute la matrice d’appareils/API, TalkBack, paysage ou tablette. Les modifications utilisateur préexistantes de MainActivity, le réglage JDK de Studio et le plan Gemini ne sont pas inclus dans le commit.
