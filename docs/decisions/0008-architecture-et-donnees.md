# Architecture et données de Préviens-moi

Date : 3 octobre 2026. Statut : validé par Fabien dans la conversation, après revue du document 08 et ajout de l'objectif Clean Architecture + MVVM pour Android.

## Contexte

Les vérifications doivent continuer quand l'application n'est pas ouverte. Les demandes, états, résultats et coûts doivent rester cohérents malgré pannes et réessais. Fabien veut aussi une architecture Android propre, compréhensible et défendable en entretien. Le choix entre Clean Architecture et architecture hexagonale ne doit pas reposer sur un effet de mode non démontré.

## Options examinées

Exécuter les contrôles sur le téléphone ou côté service ; créer plusieurs microservices ou un service central léger ; démarrer avec plusieurs modules Gradle ou un seul module dont les dépendances sont contrôlées ; imposer un compte avant toute saisie ou seulement à l'activation ; gérer des mots de passe ou utiliser une identification e-mail opérée par un fournisseur ; choisir MVVM/Clean Architecture ou remplacer ce vocabulaire par « hexagonal ».

## Décision

Le [document 08](../08-architecture-et-donnees.md) est validé. Un service central détient les surveillances confirmées, leurs plans versionnés, les résultats et l'exécution récurrente. L'application Android présente et commande ces données sans contenir les secrets des fournisseurs ni simuler une action non confirmée.

Android utilise Kotlin, Compose et MVVM pour la présentation, avec une règle de dépendance Clean Architecture : présentation vers domaine, interfaces du domaine implémentées dans data, domaine indépendant d'Android et des fournisseurs. Les cas d'usage portent une vraie règle ou coordination plutôt que chaque appel trivial. Un seul module Gradle suffit au départ, avec frontières et tests d'architecture ; la modularisation suit un besoin réel. Le service isole ses sources et fournisseurs par ports et adaptateurs, ce qui reprend le principe hexagonal sans le substituer à MVVM.

La prévisualisation fonctionne sans compte ; la première activation durable demande un compte via lien ou code e-mail fourni par un service d'authentification géré, à choisir. Les durées de conception sont 24 heures pour une proposition non activée, 90 jours pour les résultats, 14 jours pour les traces détaillées et 30 jours au plus pour les sauvegardes. Leur adéquation et leur faisabilité seront vérifiées avant publication.

## Conséquences

Les exigences ARCH-R01 à ARCH-R14 et les scénarios ARCH-A01 à ARCH-A14 guident la conception et les tests. L'API, les fournisseurs, le budget d'exploitation, les quotas et les cadences restent à concrétiser. La durée de conservation d'une surveillance terminée non supprimée et la règle des comptes durablement inactifs restent ouvertes ; aucune conservation illimitée n'est choisie par défaut.

## Révision

Réexaminer le découpage Gradle si les dépendances deviennent difficiles à contrôler, si une fonctionnalité doit être réutilisée ou si le temps de compilation le justifie. Revoir les durées seulement sur une finalité documentée ou une contrainte vérifiée. Tout changement de fournisseur doit préserver les critères confirmés et les résultats explicables.

## Précision technique du 20 octobre 2026

Statut : choix d'implémentation réversibles pris dans la délégation de Fabien, et non sélection personnelle par lui de chaque bibliothèque. Le document 08 contient le contrat applicatif v0 et les critères de vérification.

Retenus pour le socle : Android Studio stable, Compose/Material 3, ViewModels/StateFlow, Navigation 3 typée, Hilt/KSP, kotlinx.serialization, DataStore pour préférences/brouillon ; Retrofit/OkHttp à l'intégration réseau. Navigation 3 est choisie pour une application Compose neuve, plutôt qu'introduire des routes textuelles ou une navigation maison. Hilt sert les plusieurs ViewModels et l'objectif d'apprentissage. Un seul module, domaine sans dépendance Android, variantes demo/live séparées. Pas de Room, d'abonnement utilisateur ou d'ordonnancement réseau sur le téléphone.

La disponibilité stable et la compatibilité exacte des versions seront prouvées au ticket S1-02 avant toute déclaration de build réussi. Le Gradle précédemment téléchargé ne prescrit pas la matrice. La démo ne prétend pas traiter une demande libre : scénarios synthétiques explicitement identifiés. Aucun fournisseur serveur, tarif ou identifiant public définitif n'est décidé par cette précision.
