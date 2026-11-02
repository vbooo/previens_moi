# Pause, reprise et coût — 2 novembre 2026

**Version connectée 0.1.3, code 4.** Les transitions sont allégées et la préparation d’une reprise devient annulable dès que le serveur a accepté la commande. La synchronisation automatique de 0.1.2 est conservée.

## Changements Android

Pendant une mise en pause ou une reprise normale, le statut porte l’information. Les phrases « L’arrêt du suivi est en cours de confirmation » et « Demande envoyée, nous attendons la confirmation du service » sont retirées, ainsi que leurs ressources traduites. Les explications redondantes de pause/préparation/reprise sont retirées du résumé. Les actions indisponibles pendant l’envoi d’une commande sont masquées plutôt que laissées grisées ; le ViewModel continue de bloquer les commandes concurrentes.

L’apparition fugace de « Vérifier la demande » venait de la fin de l’attente courte du reçu : la commande était encore en cours, mais l’interface présentait déjà une action de récupération avant la relecture automatique suivante. Un reçu normalement en attente ne montre plus ce message ni ce bouton. Une vraie impossibilité de vérifier l’issue, par exemple une erreur de lecture, garde un message court et « Actualiser » ; elle n’est pas présentée comme une réussite.

La reprise attendait auparavant la fin de la préparation avant de libérer les contrôles. Maintenant, un reçu `pending` avec suivi identifié et `remoteState=starting` est accepté en arrière-plan, comme une création. Le journal conserve l’identifiant de cette reprise. Après relecture du suivi courant, « Annuler la préparation » est disponible : il envoie une nouvelle pause avec la version confirmée, remplace la reprise en attente et ne crée aucune nouvelle activation. Une commande dont l’acceptation est inconnue reste protégée contre les répétitions.

## Pourquoi l’attente peut dépasser une minute

Le code serveur réveille le worker chaque minute. La pause attend la confirmation d’arrêt ; une reprise réexécute plusieurs phases de préparation. Le worker traite une phase générale par passage. Une part du délai vient donc de la file et de cette cadence, puis du travail de lecture/analyse. Cela ne signifie pas que l’écran a cessé de se synchroniser.

**Ce lot ne réduit pas cette cadence serveur et ne prétend pas rendre la surveillance instantanée.** Aucun nouveau chronométrage réel sur téléphone ou fournisseur n’a été fait. L’acceptation Android de la reprise et l’annulation deviennent disponibles avant sa fin, sans faire passer prématurément le suivi à « Actif ».

## Ce qui consomme des crédits

- La pause elle-même ne lance aucune recherche Web ni analyse IA. Un appel fournisseur déjà engagé peut néanmoins avoir un coût ; l’annulation ne le rembourse pas.
- Une reprise d’un suivi à politique `stable_v1`, avec sources retenues, les réutilise à son premier passage. Elle n’effectue pas une nouvelle découverte Web. La relecture de ces pages et les phases IA suivantes peuvent néanmoins consommer des crédits.
- Une première préparation sans source ou une modification de demande peut nécessiter une découverte. Les anciens suivis utilisant une autre politique ne sont pas convertis silencieusement par ce correctif.

Le comportement économique proposé est donc déjà celui de nos suivis récents à sources fixes. Un nouveau test appelle la phase réelle de collecte d’une reprise avec une source retenue : uniquement une lecture de cette URL, aucun appel de recherche ni d’IA dans cette phase. Les réponses sont injectées ; aucun crédit réel n’est consommé. Le moteur et la planification serveur n’ont pas changé.

## Vérifications

53 tests unitaires remote et 70 demo passent (tests communs exécutés dans les deux variantes). Neuf parcours natifs remote et cinq demo passent sur l’émulateur API 36 réservé aux tests. Les nouveaux cas contrôlent l’annulation effective d’une reprise acceptée, l’absence des phrases redondantes et du bouton grisé, le blocage des doubles commandes, l’absence de récupération sur attente normale et son maintien sur véritable erreur. La synchronisation sans push ni geste reste vérifiée.

Les six tests ciblés de politique de sources passent sans réseau réel. Compilation, Lint des deux variantes sans erreur/avertissement et frontières Android passent. Trois captures natives de reprise annulable, mise en pause et pause confirmée ont été inspectées. Le contrôle ciblé des secrets serveur locaux dans les APK n’en trouve aucun ; les paramètres publics cloud sont présents.

[Preuves détaillées](checks.json). Journaux, captures et APK restent sous `.local/`, hors Git. Aucun déploiement serveur, changement de quota, donnée utilisateur modifiée ou push GitHub. **Coût fournisseur supplémentaire : 0 € ; plafond cumulé inchangé à 2 €.**

## Essayer sur OnePlus

Mettre à jour avec `remoteDebug`, sans désinstaller. L’APK est conservé sous `.local/builds/operations-2026-11-02/Previens-moi-0.1.3-test.apk`. Le téléphone n’a pas été installé ou testé dans ce lot. Éviter les bascules répétées sur de vrais suivis uniquement pour tester l’interface : une reprise peut commencer à relire les pages avant son annulation.
