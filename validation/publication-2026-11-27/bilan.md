# Préparation de publication — 27 novembre 2026

La correction serveur de la version Android 0.2.9 (23) est déployée : migration 46, API v24, worker v44, play-notifications v8. Les contrôles de droits SQL et les rejets HTTP non authentifiés passent. Les achats réels restent refusés (`testOnly=true`) et la surveillance Plus reste désactivée. Aucun appel fournisseur payant n’a été lancé pour cette préparation. Les compteurs et plafonds sont inchangés avant/après déploiement.

La sauvegarde du code serveur en ligne a été comparée aux sources locales avant mise à jour. L’identité des mails Mesures sur photos/Préviens-moi est préservée. Les huit tests ciblés de vérification d’abonnement et d’identité d’assistance passent. Le rapport [d’audit du 21 novembre](../billing/audit-2026-11-21.md) reste la preuve historique des 151 tests serveur, 190 tests unitaires Android, vérifications natives, lint et signatures du bundle. Son absence de déploiement correspond à sa date ; le présent rapport établit le déploiement ultérieur.

## Tests téléphone finaux

1. Mettre à jour vers 0.2.9 (23) depuis le canal interne. Ouvrir en ligne, avec le même compte et un abonnement de test actif, carte toujours approuvée.
2. Avant l’échéance connue : noter la date, activer le mode avion, fermer complètement l’app puis la rouvrir. L’abonnement doit rester reconnu comme dernier état connu hors connexion ; aucune nouvelle souscription proposée. Le cache n’étend pas la période au-delà de la date déjà vérifiée.
3. Rétablir le réseau et vérifier l’abonnement : retour à l’état serveur courant, sans doublon d’achat. Puis conserver l’écran ouvert lors d’une fin de période/révocation de test : l’affichage doit se synchroniser. Les autorisations serveur restent contrôlées indépendamment du cache Android.
4. Contrôle rapide accueil → suivi → résultat/notification, puis assistance. Le retour d’assistance ne doit contenir aucune donnée personnelle inutile ; son envoi utilise le quota partagé existant.

## Portes de production encore ouvertes

- Aucun suivi actif lors du contrôle. Quatre contrôles isolés sur les quatorze derniers jours ne constituent pas sept jours continus d’observation. Ne pas déclarer cette qualification terminée.
- Budget de recherche : 171 crédits, dont 116 utilisés et 39 réservés, soit 16 disponibles. Le budget IA est de 320000 microUSD, dont 94313 utilisés et 20000 réservés. Une nouvelle réservation d’essai de sept jours ne rentre pas dans la marge Web restante. Ne pas augmenter, remettre à zéro ou libérer une réservation sans justification/autorisation. Aucun basculement vers ventes réelles avec ces seules limites de test.
- Préparer les liens publics de confidentialité et de suppression, les déclarations de données, les éléments Store et l’accès de revue Google compatible avec le code e-mail. Vérifier les exigences effectivement affichées pour ce compte historique ; ne pas lui attribuer automatiquement l’obligation de douze testeurs des nouveaux comptes personnels.
- Valider capacité/budget d’exploitation avant activation de la surveillance Plus. Les tests d’achat fictifs réussis ne prouvent pas que la capacité de surveillance payante est ouverte.
- Français pour le lancement envisagé ; ne pas promettre une qualification complète des sources et résultats en anglais.

Le bundle signé porte l’empreinte SHA-256 `30e2d69b9621f6fb34efb8b415cc06492baa32bf7d958b9807fa4f2e46262aa7`. Les fichiers de signature, identifiants et snapshots serveur restent dans `.local/` hors Git.
