# Marge de crédits pour l’essai — 3 novembre 2026

Le refus d’activation ne venait pas des trois suivis affichés. Le service confirmait trois places libres : deux suivis suspendus, un terminé, aucun actif ou en préparation. L’enveloppe partagée de recherche était en revanche saturée : plafond interne 100, consommation comptabilisée 80 et réservations conservatrices 20. Les réservations couvrent des appels anciens dont la consommation n’est pas entièrement confirmée ; elles ne sont pas présentées comme une consommation certaine.

La lecture gratuite du solde Firecrawl indiquait 853 crédits encore disponibles. Dans le cadre de la prolongation d’essai déjà autorisée et du plafond de 2 €, la limite interne est passée de **100 à 150**. Cela donne 50 crédits de marge sans abonnement ni recharge. Le plafond modèle, ses consommations et réservations, les compteurs de recherche, la capacité, les plans et les suivis restent conservés. Aucun suivi suspendu n’a été réactivé par cette opération.

La migration 202611030037 modifie uniquement la borne structurelle du crédit de recherche ; le réglage effectif a été appliqué séparément dans une transaction auditée. Aucun appel OpenAI ou recherche/lecture Firecrawl n’a été déclenché. Seul le solde fournisseur a été lu. Les réservations incertaines n’ont pas été libérées sans preuve ; une pause ou une fin de suivi rend une place disponible, pas les crédits déjà utilisés.

## Vérifications

- Base locale vide et jetable : la migration conserve toutes les valeurs existantes ; l’ajout de marge permet le contrôle d’admission sans remise à zéro ; les bornes de crédit, de dépense modèle et de cohérence comptable restent appliquées. Aucune donnée de la base active n’a été copiée.
- Service actif : capacité et budget verrouillés pendant l’opération, comparaison avant/après, plans et suivis inchangés, crédit consommé/réservé inchangé. Le contrôle préalable à une activation générale passe avec trois places libres et 50 crédits de marge.
- Vérification indépendante en lecture seule après validation de la transaction. Les preuves sont dans [deployment.json](deployment.json), [verification.json](verification.json) et [local-database.json](local-database.json).

Aucune nouvelle version Android n’est nécessaire. L’utilisateur peut réessayer l’activation ; cela ne prouve pas encore la préparation ou la pertinence d’une nouvelle recherche réelle. Les résultats dépendent ensuite des sources et des limites restantes. La marge est finie et partagée entre les comptes, sans renouvellement automatique.

Coût fournisseur supplémentaire de ce diagnostic et déblocage : **0 €**. Plafond de **2 €** et budget OpenAI inchangés. L’ancien message regroupe encore place et crédits ; cette opération corrige le blocage de l’essai, sans modification de l’interface ou du contrat d’erreur.
