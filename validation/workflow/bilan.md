# Sprint 4 — comprendre puis préparer

23 octobre 2026. **Parcours fonctionnel terminé pour l’essai borné.** La grosse passe UI/UX reste à concevoir ensemble. Le benchmark exhaustif est différé.

## Ce que l’application permet maintenant

Tu crées un suivi et écris ta demande. L’IA affiche ce qu’elle a compris ou demande une précision. Elle ne fait aucune recherche Web à cette étape. Tu confirmes les critères, puis tu peux quitter l’application : les sources sont recherchées et vérifiées sur le serveur. Une notification indique si le suivi est prêt ou si la préparation a échoué ; le suivi reste consultable dans la liste.

L’accueil présente les suivis et leur état, un bouton « Nouveau suivi » et une actualisation accessible par bouton ou geste. Le compte affiche « Connecté ». Le détail d’un suivi prêt présente les sources réellement consultées. L’annulation d’une préparation est possible, y compris après redémarrage.

**Délai mesuré : 4,85 secondes pour une compréhension réelle.** Aucun appel Web ni crédit de recherche n’a précédé sa confirmation. Il s’agit d’une mesure unique, pas d’une garantie de performance. La préparation prend davantage de temps mais ne bloque plus la confirmation ou la fermeture de l’application.

## Ce qui a été vérifié

- 90 tests du service, contrôle de types et 34 exemples de contrat.
- Intégration transactionnelle dans une base locale jetable : aucune recherche avant confirmation, référence initiale silencieuse, activation seulement après succès, échec explicite, message tardif bloqué après annulation et données de notification retirées avec le terminal.
- 52 tests unitaires Android demo / 30 remote, avec une partie commune ; ne pas additionner comme des cas tous distincts. Compilation des APK et frontières Android vérifiées.
- Parcours natif avec données de contrôle en français clair et en anglais sombre avec grande police, bouton et geste d’actualisation.
- Un essai cloud réel : demande libre → confirmation en attente → recherche/lecture Firecrawl → vérification et référence initiale → suivi actif sans faux résultat → FCM réellement reçu → toucher de la notification Android ouvrant le suivi actif. Le processus Android était arrêté en arrière-plan avant la préparation. Les critères confirmés ont été conservés.

Le premier contrôle de relecture cloud a exposé un HTTP 503 : l’API authentifiée embarquait encore l’ancien contrat. Elle a été redéployée avec le contrat partagé mis à jour, sans refaire la collecte déjà consommée. Les contrôles suivants ont réussi. Une assertion d’écran a aussi dû défiler pour voir le lien source ; aucun résultat de recherche n’a été fabriqué.

Les comptes, terminaux, suivis et page publics synthétiques ont été supprimés par les circuits ordinaires ; les compteurs de coûts sont conservés. L’ancienne URL test de Fabien reste enregistrée mais suspendue, car l’enveloppe du pilote est épuisée ; son objet fournisseur est supprimé après vérification qu’aucune lecture n’avait démarré. Aucun nouvel essai de cette lecture incertaine.

## Limites et suite

Le moteur conserve les limites de S3 : sources publiques, couverture partielle, une page lue par passage général, contrôles quotidiens et quota gratuit fini. Ce sprint valide le nouveau parcours et son transport, pas toutes les demandes possibles ni une observation de sept jours. Ne pas promettre une surveillance exhaustive ou immédiate du Web.

Le prochain travail à cadrer ensemble est le contenu des écrans, leur hiérarchie et le design. Restent ensuite les vérifications de diffusion et de confidentialité, l’observation dans la durée avec budget explicite, le déblocage Play Console et la publication. Aucun nouveau fournisseur ni benchmark ajouté automatiquement.

**Budget :** 270 appels modèle cumulés connus, estimation totale connue de 0,184755 USD. Borne prudente tenant compte de toute l’enveloppe déjà autorisée, change et taxes : **0,99587625 € / 2 € cumulés**. Aucune réserve inconnue ; 907 crédits Firecrawl gratuits constatés, dont 9 encore ouverts dans l’enveloppe générale. Aucun abonnement, recharge ou changement de plafond. Ce n’est pas une facture rapprochée. GitHub reste différé ; sauvegarde dans Git local.

Preuves : [chaîne cloud](cloud-chain.json), [budget](budget.json), [base jetable](local-database.json), [contrôles locaux](local-checks.json), [réception Android](screens/cloud-ready-notification.png), [ouverture depuis la notification](screens/cloud-notification-opened-watch.png), [source consultée](screens/cloud-ready-sources.png). Architecture expliquée simplement dans [le parcours S4](../../docs/12-comprendre-puis-preparer.md).
