# Création : limites d’essai et accès à Plus — 0.2.6 (20)

## Comportement

- Un essai terminé ou un abonnement requis propose « Voir l’offre Plus » au stade de saisie, de clarification et de confirmation. Le bouton ouvre l’écran Abonnement, sans achat automatique.
- La limite de suivis actifs propose aussi Plus si la proposition identifie explicitement l’essai gratuit de sept jours. Les limites d’un compte Plus ne proposent pas un second abonnement.
- Un blocage global ne propose aucun paiement : « Le service a atteint sa limite d’utilisation. Réessaie plus tard. » La demande reste saisie. Ce message n’identifie plus à tort la limite personnelle de l’essai.
- La confirmation garde son erreur visible dans le pied fixe, près de l’action correspondante. Bouton Material 3 primaire de 52 dp minimum, forme medium ; identité existante conservée.
- Aucun changement serveur, quota, plafond de dépense ou droit d’abonnement. Le blocage global constaté pendant le diagnostic reste distinct de ce correctif d’interface.

## Validation

84 tests unitaires remote et 87 demo réussis ; trois tests d’interface sur deux configurations : français clair, anglais sombre avec texte à 130 %. Vérification du callback d’abonnement aux trois étapes, des exclusions quota global/limites Plus et de la conservation du texte après refus. Aucun achat, nouvelle recherche ou envoi de mail déclenché par ces tests.

Lint remote debug/release et demo debug sans erreur ni avertissement. Correction de la dernière CI : l’icône Envoyer, utilisée seulement par l’assistance connectée, est rangée dans les ressources remote au lieu de main ; aucune suppression générale de contrôle Lint.

Revue indépendante : **ship**, limitée à cette extension du parcours sur téléphone portrait. Six captures contrôlées, fixtures synthétiques utilisant CreateScreen de production. Pas de qualification tablette, paysage, TalkBack ou parcours d’achat réel. Documentation de design conservée : la recette de bouton et de pied fixe existait déjà.

Fichier signé : `.local/play-test/previens-moi-play-release-0.2.6-20.aab`. Contrôles statiques APK et signature AAB réussis. **Pas encore téléversé sur Google Play** ; le téléphone nécessite donc une mise à jour via le canal interne.

Coût fournisseur supplémentaire : **0 €**. Plafond de 2 € inchangé. Les changements préexistants d’assistance serveur et d’imports MainActivity sont laissés hors de ce lot.
