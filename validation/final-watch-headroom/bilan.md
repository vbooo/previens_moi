# Dernier suivi de test — marge de compréhension

Le 20 novembre 2026, la liste du compte d’essai est vide mais son financement reste réservé jusqu’à la fin des sept jours. La compréhension préalable, exécutée avant la confirmation, puise dans la marge globale et ne pouvait plus réserver ses 20 000 micro-USD, malgré les crédits financés de l’essai. Aucune place de suivi n’était occupée.

## Ajustement demandé

Migration `202611200045_final_test_model_headroom.sql` : plafond structurel modèle de 300 000 à 320 000 micro-USD. Après précontrôle, seule la valeur d’exécution `model_limit` passe aussi de 300 000 à 320 000. La marge supplémentaire est **0,02 USD**, dans le plafond cumulé autorisé de **2 EUR**.

Toutes les autres valeurs du budget sont préservées : consommé, inconnu/réservé, engagé, crédits Web. Les réservations inconnues ne sont pas remboursées. Aucun quota individuel, date d’essai ou droit d’abonnement n’est modifié. Aucun suivi n’est créé ou relancé automatiquement et les ventes publiques restent fermées.

Le calcul conservateur historique de 0,99587625 EUR est majoré de 0,02 USD avec les mêmes coefficients de change/taxes de prudence (1,5 × 1,25) : **1,03337625 EUR**, sous 2 EUR. C’est une enveloppe de contrôle, pas une facture rapprochée.

## Contrôles

- Base locale jetable : migration sans changement de compteurs ; admission de compréhension rétablie ; plafonds et contraintes d’engagement maintenus ; quatre réservations concurrentes ne peuvent pas dépasser la capacité disponible. Aucun export de données cloud.
- Précontrôle cloud : seule la migration 45 était en attente ; place disponible, aucun travail prêt/en cours, essai encore ouvert environ 75 heures, 21 crédits Web et 172 651 micro-USD encore financés pour ce compte.
- Solde fournisseur Firecrawl : 992 crédits disponibles, lecture seule ; aucune recherche/scrape ni recharge.
- Clé modèle : accès en lecture seule au modèle existant validé ; aucun appel de génération.
- Après changement : contrôle transactionnel de la possibilité de réserver 20 000 micro-USD, des valeurs préservées et des dates/financements d’essai inchangés.

Les réservations financées suffisent au démarrage du dernier suivi et à ses passages quotidiens pendant le reste de cet essai (enveloppe prudente de 20 crédits Web et 100 000 micro-USD pour préparation, état initial et trois passages). Il ne s’agit pas d’un nouvel essai de sept jours. Un échec de source, un coût inconnu ou plusieurs modifications supplémentaires peuvent encore consommer cette marge ; une réussite réelle de bout en bout reste à observer sur le téléphone.

Aucun nouvel achat, aucun appel payant déclenché pendant ce correctif. Aucun changement Android et aucune réinstallation requise. Le compte doit être celui de l’essai déjà financé ; une autre identité ne récupère pas cette réserve.
