# Accès complet pour la revue et qualification — proposition non appliquée

## État vérifié le 27 novembre 2026

- Version 0.2.11 (26) diffusée en test interne, contrôles locaux et CI réussis.
- Compte évaluateur utilisateur ordinaire créé dans le projet Supabase existant, après accord explicite. Pas de privilège d’administration, pas d’accès Plus offert actuellement.
- Google demande un accès complet aux fonctions payantes, sans achat ni recours à un essai gratuit. Le formulaire ne doit pas certifier un accès complet avant sa mise en place.
- L’observation de sept jours consécutifs n’est pas démontrée.
- Plafond d’essais autorisé : 2 EUR cumulés. Modèle général : limite 320000 micro-USD, consommé 96732, réservé incertain 20000, engagé 0. Web : limite 171 crédits, consommé 116, réservé incertain 39, engagé 0 ; reste admissible 16 crédits.
- Solde fournisseur Firecrawl lu sans recherche ni achat : 988 crédits, plan de 1000, période du 12 novembre au 12 décembre 2026. Aucun forfait payant à souscrire pour cette proposition.

## Mécanisme proposé

Ajouter un droit Plus offert explicite, administré côté serveur, séparé des preuves d’achat Google. Le bénéficiaire reste un utilisateur ordinaire et ne peut consulter que ses propres données. Aucune adresse codée en dur dans Android, aucun faux achat, aucun mot de passe spécial ou désactivation de vérification. L’interface doit indiquer que l’accès est offert ; elle ne doit pas annoncer un prélèvement ou une résiliation Google pour ce droit.

Conserver les limites normales Plus : deux suivis actifs, trois sources maximum, cadence quotidienne, cinq créations/changements de sujet par période et limites techniques d’analyse existantes. Attribuer un seul droit, au compte préparé pour la revue. Isoler une enveloppe finie pour sa première période de contrôle ; aucune dépense non bornée ni renouvellement de budget silencieux. Révoquer ce droit ou supprimer le compte doit couper les opérations protégées ; le cache Android ne crée aucun droit serveur.

Prévoir une observation distincte de sept jours, avec un suivi réel, ses sources et ses attendus consignés. Les relevés quotidiens n’exécutent pas d’appels fournisseurs supplémentaires. Ne pas qualifier sept jours avant leur exécution.

## Plafonds proposés, après accord uniquement

| Élément | Valeur proposée |
| --- | --- |
| Enveloppe globale d’essais | 6 EUR cumulés, remplaçant le plafond de 2 EUR ; ni 6 EUR par mois ni 6 EUR par utilisateur |
| Plafond modèle général | 2900000 micro-USD ; compteurs utilisés/réservés/engagés préservés |
| Plafond Web général | 435 crédits gratuits ; compteurs utilisés/réservés/engagés préservés |
| Affectation Web proposée | 250 crédits pour la première période Plus offerte et 30 pour l’observation ; 116 utilisés et 39 réservés restent intacts |
| Borne modèle proposée | 2 USD pour les contrôles Plus, jusqu’à 25 analyses à 0,02 USD, puis 0,20 USD pour préparation et observation ; toutes restent sous le plafond global |
| Capacité d’essais | Au plus trois suivis simultanés partagés : deux pour l’accès Plus, un pour l’observation |
| Ventes réelles | Fermées ; aucune ouverture générale de surveillance Plus par modification du drapeau global |
| Forfait fournisseur, recharge ou carte | Aucun nouveau forfait ni recharge ; Firecrawl utilise les crédits gratuits disponibles |

Calcul conservateur reprenant le dossier du 20 novembre : 1,03337625 EUR + (2,90 - 0,32) USD × 1,5 × 1,25 = **5,87087625 EUR**, sous 6 EUR. Il s’agit d’une borne interne prudente, pas d’une facture rapprochée. Le plafond ne couvre pas la facturation indépendante Google Cloud déjà expliquée ; aucune ressource Cloud supplémentaire n’est créée par cette proposition.

## Mise en place et contrôles à réaliser après accord

1. Implémentation locale et tests : droit offert séparé des achats Google, accès à ses propres données, quotas, concurrence, révocation, suppression et absence d’escalade depuis un client.
2. Vérification que la capacité normale Plus est accessible au bénéficiaire sans ouvrir les ventes ni les achats de test des autres comptes.
3. Précontrôle des soldes et compteurs puis migration ciblée ; aucune remise à zéro ou libération d’un coût incertain.
4. Attribution du seul droit explicitement autorisé, puis vérification de bout en bout de l’accès normal et de son affichage.
5. Renseigner les identifiants dédiés dans le champ privé Google ; terminer la cible 18+ et la validation de Sécurité des données.
6. Démarrer l’observation avec sources/attendus et date effective J0 ; rapport de sortie avant toute ouverture publique payante.

Cette proposition ne résout pas à elle seule les vérifications contractuelles fournisseurs et l’exploitation des sauvegardes avant production. Aucun droit offert ni plafond n’est changé par la rédaction de ce document.
