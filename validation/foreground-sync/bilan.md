# Synchronisation et premier résultat — 2 novembre 2026

**Correction Android livrée dans la version connectée 0.1.2, code 3.** Le scénario signalé est reproduit et passe sur un émulateur réservé aux tests. Le OnePlus n’était pas connecté ; son application doit encore être mise à jour.

## Pourquoi l’écran restait en préparation

Le serveur avait terminé, mais l’écran conservait les dernières données téléchargées. Compose observait déjà les `StateFlow` du ViewModel ; il manquait la relecture automatique de l’état serveur pendant que l’application restait visible. La correction précédente de pause/reprise réconciliait une commande en attente, sans suivre toute préparation asynchrone après modification d’une demande.

`WatchSynchronizationEffect` lie maintenant cette lecture à la session et au cycle de vie. À l’ouverture ou au retour au premier plan, l’app lit immédiatement les suivis et résultats. Pendant la préparation et les transitions, elle les relit environ toutes les trois secondes après une lecture réussie ; sinon, environ toutes les trente secondes. Une nouvelle préparation réveille la lecture sans attendre l’intervalle long. Une lecture lente ou hors réseau conserve le contenu et attend avant de réessayer. Les lectures manuelles et automatiques ne se superposent pas dans le ViewModel.

Passer en arrière-plan, se déconnecter ou changer de compte annule cette boucle. Les commandes conservent leur identifiant de réconciliation ; la boucle ne crée aucune nouvelle activation. Ce sont des lectures de notre API, pas de nouvelles recherches OpenAI/Firecrawl. Les résultats et suivis continuent d’être affichés à partir de la même source d’état.

À réception d’un push, le worker recharge également les données authentifiées avant de présenter une notification, même si les notifications sont désactivées. L’écran ne dépend donc plus du délai de livraison du push. La livraison FCM et l’exécution WorkManager peuvent arriver après l’état serveur ; aucun délai maximal sur téléphone n’est promis.

## Pourquoi la notification disait seulement « suivi prêt »

Le push de fin de préparation utilisait toujours le modèle générique, y compris lorsqu’une information avait été trouvée pendant la première lecture et avait terminé un suivi ponctuel.

Le client recherche maintenant le résultat initial correspondant exactement au suivi, au plan et à sa version courante. S’il existe, la notification affiche « Trouvé pour toi : [titre] » et le résumé du résultat ; elle ouvre directement sa fiche. Elle utilise le même identifiant de résultat que la notification habituelle pour limiter les doublons. Sans résultat initial, le message « suivi prêt » reste approprié. Un ancien résultat ou une préparation échouée ne sont pas annoncés comme une réussite.

Aucun changement serveur, migration, augmentation de quota ou redémarrage des demandes réelles n’a été nécessaire.

## Textes simplifiés

« Déjà disponible au démarrage » devient **« Trouvé dès la première recherche »** dans la liste et la fiche. Cela signifie que l’information existait déjà sur la page au premier passage ; cela ne prétend pas qu’elle vient d’être publiée. En mode « arrêter au premier résultat », elle peut donc terminer le suivi immédiatement.

La section « Résultats » devient « Informations trouvées ». La phrase finale « Vérifié à cette date. Consulte le site pour connaître les conditions actuelles. » est supprimée, ainsi que sa ressource traduite. La date de vérification et le lien vers le site sont conservés. Le moteur n’a pas été transformé en extracteur d’annonces individuelles : un résultat peut encore résumer une page listant plusieurs offres.

## Vérifications et limites

- 52 tests unitaires remote et 69 demo passent ; les tests communs sont exécutés dans les deux variantes. Dix nouvelles régressions couvrent la synchronisation, son annulation, le recul hors réseau et le choix du résultat initial.
- Huit parcours natifs passent en français : passage automatique préparation → terminé dans le détail et la liste, ouverture du résultat, arrêt des lectures en arrière-plan et à la déconnexion, reprise immédiate, puis régressions existantes de création et commandes. Aucun clic d’actualisation ou push n’est injecté pour provoquer la mise à jour testée.
- Une capture native du résultat en anglais, thème sombre et texte à 130 % passe également. Les trois captures inspectées restent des fixtures, pas de nouvelles offres réelles.
- Les deux variantes compilent, Lint passe sans erreur ni avertissement et les frontières Android passent. Aucune baseline ne masque les problèmes. Les suggestions de versions restent informatives.
- Les paramètres publics cloud sont présents dans l’APK connecté ; aucun des secrets serveur locaux recherchés littéralement n’est trouvé. Le test est ciblé, pas un audit de sécurité complet.

Les [preuves détaillées](checks.json) contiennent les empreintes des APK, captures et journaux locaux. Les tests du service ne sont pas relancés ici : le backend n’a pas changé. Les essais réels précédents ne sont pas présentés comme une nouvelle qualification FCM ou HelloWork sur téléphone.

**Coût fournisseur supplémentaire : 0 €.** Le plafond cumulé de 2 € reste inchangé. Aucun push GitHub.

## Essai sur OnePlus

Mettre à jour avec `remoteDebug` dans Android Studio, sans désinstaller l’application. L’APK est aussi conservé sous `.local/builds/sync-fix-2026-11-02/Previens-moi-0.1.2-test.apk`. Les suivis déjà terminés restent terminés ; ne pas lancer une nouvelle recherche uniquement pour vérifier les libellés.

Pour un prochain suivi créé dans le budget disponible : laisser le détail ouvert, vérifier le changement automatique, puis revenir à la liste. Le push annonçant le premier résultat doit ouvrir sa fiche et afficher son résumé. Cette vérification physique reste à faire ; elle n’est pas incluse dans les résultats émulateur ci-dessus.
