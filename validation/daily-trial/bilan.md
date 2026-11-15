# Essai gratuit quotidien — 15 novembre 2026

Version Android 0.1.11 (code 12). Règle déployée et activée ; aucun essai réel lancé pour la qualification.

## Comportement livré

- Un suivi actif par compte, jusqu’à trois sources, contrôle toutes les 24 heures pendant sept jours consécutifs depuis la première activation confirmée.
- Plafond du compte : 30 crédits Web et 0,18 USD de réservations modèle après confirmation. Compréhension préalable financée par le budget global, hors engagement du compte. Aucun compteur renouvelé par pause, édition ou changement de suivi.
- Réservation atomique de l’enveloppe future du compte dans le plafond global ; tentatives inconnues conservées en réserve. Essai arrêté à expiration ou insuffisance, résultats conservés, reprise impossible après échéance. Une reprise saine utilise les sources/mémoire existantes sans préparation payante.
- Les anciens plans gardent leur cadence. Nouveaux champs API optionnels, ancien statut compatible ; le client affiche « Essai terminé » et distingue place occupée et essai expiré.
- Fin gratuite sans paiement automatique. Google Play Billing est une recommandation pour un futur lot, aucun SDK ou tarif ajouté.

## Contrôles utiles

153 tests unitaires Android (85 demo, 68 remote), Lint debug des deux variantes et frontières passent. 125 tests Deno et contrôle des types, 37 exemples du contrat (23 positifs, 14 négatifs), trois tests du calcul d’observation passent. Les tests partagés comptent dans chaque variante Android.

La [base locale jetable](local-database.json) valide migration depuis le schéma historique, activation rejouée, durée non renouvelée, limite active, réserve protégée d’un autre compte et des analyses anonymes, règlement idempotent, échéance pendant un travail en vol, quota, concurrence et effacement du registre personnel sans libérer les charges inconnues. Pas de données réelles copiées ni fournisseur appelé.

Six parcours natifs uniques API 36 : offre avant confirmation, essai expiré, connexion/création/détail, pause/reprise, notification à froid, pays non répété. Les deux nouveaux parcours sont repassés en sombre avec police à 130 % ; captures inspectées. Le statut expiré prévaut même sur un ancien statut de pause. Le contexte est un serveur HTTP local contrôlé, pas le Web réel.

APK et AAB remoteRelease optimisés compilés avec configuration client réelle, Lint release et [inspection du binaire](release-checks.json) réussis. Artefacts locaux ignorés, version de diffusion non signée. Le contrôle statique ne prouve ni l’affichage d’une notification ni sept jours de fonctionnement.

Sur le cloud, les quatre fonctions sont déployées. L’activation et l’expiration synthétiques ont été vérifiées dans une transaction annulée avant l’ouverture de la règle. Le [contrôle HTTP réel](http-checks.json) confirme 403 free_trial_ended avant interprétation, santé 200 et refus du worker non autorisé. Identité synthétique supprimée, compteurs inchangés, aucun e-mail envoyé. Le premier essai du script HTTP lisait à tort une réponse Auth imbriquée ; le script a été corrigé pour la réponse REST directe et les identités synthétiques nettoyées.

## Dépenses et suite

[Déploiement](deployment.json) : un suivi par compte, plafond modèle général de 0,30 USD et 171 crédits Web cumulés ; utilisé et réservé antérieurs inchangés, aucun suivi réactivé. Enveloppe modèle qualification + runtime ≤ 0,390130 USD sous l’autorisation historique de 0,40 USD. Borne cumulée conservatrice 0,99587625 € sous les 2 € ; pas de facture rapprochée ni budget mensuel public autorisé. Aucun nouvel appel payant dans ce lot.

À installer : APK remoteDebug 0.1.11. L’observation commence à la prochaine activation réelle ; sept jours n’ont pas encore été observés. Restent la qualification physique release, les contrôles de publication/confidentialité et la signature. Protocole : [release et observation](../../docs/13-release-et-observation.md).
