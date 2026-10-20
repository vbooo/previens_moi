# Parcours et écrans de Préviens-moi

Date : 2 octobre 2026. Statut : validé par Fabien dans la conversation, après revue du document 04.

## Contexte

Le périmètre du MVP est validé dans le document 03. Il faut traduire la promesse d’alerte en parcours simples, honnêtes sur les limites et vérifiables par des scénarios d’acceptation, sans imposer un catalogue de catégories ni une inscription avant la prévisualisation.

## Options examinées

Afficher de nouveau un onboarding après la première surveillance ou garder la question de départ visible ; créer une boîte globale de toutes les alertes ou consulter les résultats dans chaque surveillance ; demander systématiquement une adresse de page Web ou ne la proposer qu’en secours si la recherche automatique échoue.

## Décision

Le [document 04](../04-parcours-et-ecrans.md) est validé. Après la première surveillance, la question « De quoi veux-tu être prévenu ? » reste visible, et les surveillances sont immédiatement accessibles. La V1 ne comporte pas de boîte globale des alertes : les résultats sont consultables dans chaque surveillance et depuis les notifications.

Si la recherche automatique ne trouve pas de source exploitable, l’application peut proposer à la personne de coller l’adresse d’une page Web publique qu’elle connaît. C’est une option de secours, absente de l’accueil principal. Le service qualifie ensuite cette page avant de proposer une activation ; l’adresse fournie ne prouve ni la fiabilité de la source ni la faisabilité du suivi.

## Conséquences

Les surfaces, états, exigences UX-R01 à UX-R15 et scénarios UX-A01 à UX-A17 du document 04 forment le contrat de parcours à préciser dans les maquettes et les tests. La première version reste centrée sur une demande libre et la transparence de la surveillance. Les règles de détection, les techniques d’accès aux sources, la cadence, le fournisseur de compte et les durées de conservation restent à décider dans leurs documents respectifs.

## Révision

Réexaminer l’accueil ou l’absence de boîte globale si des essais montrent que les personnes ne trouvent pas leurs surveillances ou leurs résultats. Réexaminer la saisie de page Web si elle n’aide pas à résoudre des demandes réelles ou si son coût de qualification est disproportionné.
