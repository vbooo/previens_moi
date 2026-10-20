# Sources et stratégie de recherche de Préviens-moi

Date : 2 octobre 2026. Statut : validé par Fabien dans la conversation, après revue du document 06.

## Contexte

Une demande en langage naturel peut être comprise et trouver une réponse ponctuelle sans qu’il existe une méthode fiable pour détecter ses nouveautés dans la durée. Préviens-moi doit tenir compte de la couverture, des droits d’usage, de la fraîcheur, des coûts répétés et des échecs des sources avant d’annoncer une surveillance active.

## Options examinées

Lancer une recherche Web ou un agent IA à chaque vérification ; privilégier systématiquement le coût nul ; ou qualifier la méthode adaptée à chaque demande, en examinant d’abord les sources structurées et ciblées lorsqu’elles satisfont la promesse. Accepter toutes les demandes trouvant une page aujourd’hui ou réduire honnêtement celles sans méthode récurrente. Choisir un fournisseur dès le cadrage ou mesurer plusieurs approches sur un même corpus.

## Décision

Le [document 06](../06-sources-et-strategie-de-recherche.md) est validé. Les API, données ouvertes, flux, calendriers, notifications de producteurs et pages publiques sont évalués lorsqu’ils sont pertinents. Qualité, droit d’usage et fraîcheur passent avant la simple gratuité. Une recherche Web ou un service agentique est utilisé lorsque sa valeur est démontrée ; aucun modèle, fournisseur ou protocole MCP n’est supposé couvrir tous les besoins.

Une demande sans méthode de vérification récurrente exploitable est refusée ou proposée avec une portée réduite explicitement acceptée, même si une recherche ponctuelle a trouvé des pages. Un essai comparatif mesurera une méthode ciblée et une solution tierce sur les mêmes cas, après fixation du plafond d’essai dans le document 07. Cette décision n’autorise aucune dépense.

## Conséquences

Le registre de sources, les plans de surveillance, les exigences SRC-R01 à SRC-R12 et les scénarios SRC-A01 à SRC-A12 guident l’implémentation et les essais. Les fournisseurs candidats, quotas, cadences, seuils de qualité et budget restent ouverts. Les conditions externes vérifiées le 2 octobre 2026 devront être contrôlées de nouveau avant intégration.

## Révision

Réviser la hiérarchie si les essais montrent qu’une méthode tierce offre une meilleure qualité et un coût complet plus faible sur un ensemble de besoins, ou que des sources structurées pourtant disponibles ne satisfont pas la fraîcheur requise. Toute modification de la couverture annoncée reste explicite pour la personne.
