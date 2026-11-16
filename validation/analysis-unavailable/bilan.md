# Erreur d’analyse et relance — 16 novembre 2026

Version 0.1.12. Le refus intervenait avant la proposition et avant toute activation : aucun nouvel essai de sept jours n’avait démarré. Un GET gratuit des métadonnées modèle refusait la clé locale avec 401 / expired_secret_key ; son empreinte correspondait à celle de la clé déployée. La vérification de préparation précédente n’avait pas couvert cet accès.

Fabien a remplacé la clé dans le fichier privé ignoré par Git. GET modèle 200 ensuite, seule la clé OpenAI installée dans le secret distant, empreinte contrôlée sans exposer de valeur. Ni abonnement ni recharge, aucune génération supplémentaire pour ces contrôles.

## Corrections

Le formulaire dit désormais : « Impossible d’analyser ta demande pour le moment. Réessaie plus tard ; ta saisie est conservée. » Le message générique des actions ne promet plus abstraitement l’absence de doublon. Les mécanismes d’idempotence des confirmations restent inchangés.

Une analyse dont l’échec est confirmé est retirée du journal de reprise, sans effacer le texte. Une nouvelle tentative explicite obtient un nouvel identifiant. Si elle retrouve l’échec terminal conservé par un ancien client, elle le renouvelle une seule fois dans la même action. Une réponse HTTP incertaine garde le même identifiant et n’entraîne pas de nouvelle analyse automatique. Ces cas sont testés séparément, y compris après recréation du repository.

Le serveur journalise seulement event=model_rejected, la phase et le statut HTTP, sans clé, demande ou corps fournisseur. Un refus 401 explicite des futures tentatives règle le coût modèle à zéro ; une réponse perdue ou un rejet 503 conserve sa réserve inconnue. Les réservations historiques ne sont pas remboursées sur la seule base du diagnostic actuel. Les fonctions public et worker sont redéployées.

`scripts/check-model-access.py` complète les précontrôles par un GET uniquement. Son succès ne prouve pas le solde facturable, les droits Responses ni la pertinence du moteur ; vérifier également la correspondance avec le secret déployé après une rotation.

## Validation et limites

157 tests unitaires Android (70 remote, 87 demo), Lint des deux variantes et frontières réussis ; 127 tests service et contrôle des types réussis. Trois parcours natifs API 36 réussis : erreur d’analyse/reprise, connexion/création/détail et pause/reprise. Nouveau message capturé et inspecté en sombre avec police à 130 %. Les services HTTP du parcours sont contrôlés, pas des recherches Web réelles. [Résultats](checks.json).

La version remoteDebug connectée au cloud est préparée pour le téléphone, sans clé fournisseur embarquée. Aucun suivi réel n’a été activé par le correctif et aucun nouveau résultat IA/Web n’est revendiqué. Le plafond de 2 € et les règles d’essai ne changent pas. Installer 0.1.12, retrouver la demande conservée puis utiliser Me prévenir et confirmer le suivi continu pour commencer l’observation.
