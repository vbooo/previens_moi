# Accueil et assistance — 0.2.5 (19)

## Changement

- Barre de titre compacte ; retrait de liste de 12 dp et suppression des marges hautes de 64/72 dp des états vides.
- Accueil vide sur une surface sauge avec action remplie ; une fois connecté, « Nouveau suivi » est accessible dans le contenu. Le FAB ambre reste l’action de création de la liste non vide et de l’accueil déconnecté.
- Aide : accès tonal avec icône, en-tête identifiable, choix du sujet clairement sélectionné, champs différenciés, bouton primaire Envoyer de 52 dp minimum. États hors ligne, envoi et réception conservés.
- Palette et composants natifs Material 3 conservés. Aucun changement au moteur, aux quotas, aux achats ou à l’envoi serveur.

## Vérification

84 tests unitaires remote et 87 demo réussis. Lint debug/release sans erreur ni avertissement. Trois tests natifs sur deux configurations (français clair, anglais sombre à 130 %), plus une régression de confidentialité/connexion déconnectée : 7 exécutions réussies. Clavier français réel et accès à Envoyer après défilement vérifiés sur l’émulateur. Contraste des nouvelles paires texte/fond : minimum 6,13:1.

Revue visuelle indépendante : **ship**, à la portée de cette retouche native. Les 16 captures contrôlées sont des captures de téléphone en portrait ; les états connectés vide/liste sont des fixtures synthétiques utilisant les composants de production. Aucun appareil physique, tablette, paysage ou TalkBack n’est qualifié par ce lot.

Le fichier signé `.local/play-test/previens-moi-play-release-0.2.5-19.aab` est prêt ; il reste à l’importer dans le canal de test interne puis à mettre à jour l’app du téléphone. Aucun téléversement Google Play n’a été effectué ici. Les rapports JSON fournissent les contrôles et empreintes.

Aucun appel IA/Firecrawl et aucun envoi de mail supplémentaire : **0 € de coût fournisseur supplémentaire** pour cette retouche.

Références : [boutons Compose Material 3](https://developer.android.com/develop/ui/compose/components/button), [barres d’application](https://developer.android.com/develop/ui/compose/components/app-bars).
