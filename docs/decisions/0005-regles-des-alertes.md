# Règles des alertes de Préviens-moi

Date : 2 octobre 2026. Statut : validé par Fabien dans la conversation, après revue du document 05.

## Contexte

Le périmètre et les parcours du MVP sont validés. Une alerte doit représenter une nouveauté pertinente, étayée et compréhensible, sans masquer les pannes ni répéter le même événement. Il fallait aussi définir la fin d’une demande ponctuelle, la reprise après une pause volontaire et le périmètre des notifications de la V1.

## Options examinées

Laisser une surveillance ponctuelle active après son premier résultat ou la terminer ; notifier les événements intervenus pendant une pause volontaire ou repartir d’un nouvel état initial ; ajouter dès la V1 des heures calmes et un résumé périodique ou se concentrer sur les notifications de nouveautés vérifiées.

## Décision

Le [document 05](../05-regles-des-alertes.md) est validé comme contrat fonctionnel, sous réserve des paramètres explicitement ouverts en fin de document. Une surveillance annoncée comme ponctuelle se termine après son premier résultat confirmé. Son envoi est décidé avant, ou atomiquement avec, cette transition ; aucune correction ultérieure n’est garantie après l’arrêt. Une intention de suivi continu est traitée comme telle et confirmée avant activation.

Après une pause volontaire, le service établit un nouvel état initial. Il montre les informations alors connues sans promettre un historique exhaustif ni envoyer automatiquement des alertes de rattrapage pour la période de pause. Une panne subie suit une règle distincte : un événement détecté avec retard peut encore être signalé s’il est prouvé et utile.

La V1 n’ajoute pas d’heures calmes réglables ni de résumé périodique autonome. Les notifications de nouveauté correspondent à des résultats vérifiés ; les réglages de notification du téléphone restent utilisables.

## Conséquences

Les exigences AL-R01 à AL-R17 et les scénarios AL-A01 à AL-A19 guident les essais du document 09. Les seuils de notification en cas de panne, la cadence, les coûts et le réarmement d’une condition à seuil restent ouverts jusqu’aux essais des documents 06 et 07. Cette validation ne choisit aucun fournisseur, fréquence chiffrée ou tarif.

## Révision

Réexaminer ces règles si les essais montrent qu’un événement ponctuel doit conserver un suivi de corrections, que l’absence de rattrapage après pause crée une incompréhension importante, ou que les notifications sans heures calmes nuisent à l’utilité du service. Toute nouvelle promesse doit être explicite avant activation.
