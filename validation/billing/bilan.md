# Abonnement Plus — 16 novembre 2026

Version Android 0.2.0 (14). Offre validée par Fabien : 6,99 €/mois, deux suivis actifs, trois sources maximum par suivi, vérification quotidienne, cinq créations/changements de sujet par période. Les sept jours gratuits restent sans engagement. [Configuration et étapes Google restantes](../../docs/14-abonnement-google-play.md).

## Livraison

Écran d’abonnement Material 3 en français/anglais, achat via Google Play Billing 9.1.0, restauration au démarrage/retour au premier plan et manuelle, lien de gestion/résiliation, messages de paiement en attente/annulé/indisponible, avertissement de suppression de compte. Prix du bouton récupéré auprès de Google. Le package définitif `fr.previensmoi.app` est distinct du prototype pour préserver le téléphone et l’observation en cours.

Le serveur vérifie l’achat dans Google Play Developer API, le lie à un compte, persiste les droits avant acquittement et rapproche les renouvellements sans réinitialiser deux fois les quotas. RTDN signé par Google et rattrapage périodique des achats connus. L’état Android ne peut pas accorder de droits. Les quotas sont appliqués sur le serveur en complément du plafond global. Le passage essai → Plus conserve les sources, le travail déjà en préparation et les réservations financières en cours. Un changement de compte ne permet pas de confirmer une proposition payante avec un compte gratuit.

Les migrations 39 et 40 et cinq fonctions ont été déployées avec **commerce désactivé, achats réels refusés et surveillance payante désactivée**. Les plans, les essais et leurs compteurs sont restés identiques pendant chaque migration. Plafonds globaux conservés : 300 000 micro-USD IA et 171 crédits Web cumulés. Aucun achat, recharge ou abonnement fournisseur. Aucun appel métier payant supplémentaire pour qualifier ce lot ; la surveillance préexistante reste soumise à ses limites habituelles. Le plafond cumulé autorisé reste 2 €.

## Vérifications réalisées

- **165 tests unitaires Android** (78 remote, 87 demo ; une partie commune est exécutée dans les deux variantes), compilation des deux variantes et Lint sans erreur/avertissement bloquant.
- **134 tests serveur**, contrôle de types Deno, dont achats en attente, annulation, expiration, mauvais produit/compte, preuve avant acquittement et reprise d’un acquittement interrompu ; signature, audience et identité du push Google avec clés de test locales.
- **45 exemples de contrat API** positifs/négatifs, contrat Edge identique, frontières Android et absence de réseau dans la démo.
- **Base PostgreSQL locale jetable** contenant uniquement des identités et résultats fictifs injectés : deux suivis, cinq créations, rejeu de commande, reprise sans nouvelle recherche, renouvellement, réponses désordonnées, suspension/régularisation, effacement des jetons, non-transfert d’achat et passage à Plus en cours de préparation. Aucun fournisseur appelé. La régression complète de l’essai gratuit passe aussi.
- **Émulateur API 36** : écran d’abonnement en clair, sombre et police 130 %, achat indisponible désactivé, offre, abonnement actif, état en attente, restauration et erreur ; 14 parcours de la vraie activité avec HTTP contrôlé, dont Compte → Abonnement, connexion, pause/reprise, notification et états de chargement. Le parcours optionnel de capture n’est pas compté comme exécuté.
- **Version optimisée avec package Play définitif et paramètres publics fictifs** : R8, politiques réseau/sauvegarde, absence de données privées, SDK de démo et secrets. Artefact non signé, inutilisable pour publier ou tester un achat Google réel.

## Limites et suite

**Aucun achat avec compte de licence Google n’a encore été réalisé.** Mise à jour du 17 novembre : identité serveur autorisée sur cette seule application, nouvelle application Firebase raccordée, clé de dépôt et bundle signé contrôlés, compte de licence confirmé. La vérification serveur est ouverte uniquement aux achats de test ; la surveillance Plus reste fermée et les plafonds sont inchangés. Le forfait est encore en brouillon, la diffusion interne et RTDN restent à terminer. Tester ensuite le cycle complet avec les moyens de paiement de test Google, sans carte réellement débitée. Les tests contrôlés ci-dessus ne remplacent pas cette qualification.

La vente réelle reste fermée. Le petit budget actuel protège le pilote personnel et ne peut pas financer une offre publique ; exploitation, capacité, informations commerciales/confidentialité et publication demandent une décision séparée. Les mesures de sept jours restent en cours de qualification, sans conclusion de fiabilité opérationnelle par simple compilation.

## Preuves

[Contrôles](checks.json) · [Base jetable](local-database.json) · [Régression gratuit](free-trial-regression.json) · [Déploiement fermé](deployment.json) · [Binaire de contrôle](release-fixture.json).
