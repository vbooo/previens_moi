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

## État final de la préparation locale

La 0.2.9 (23) est marquée « Accessible aux testeurs internes » dans Play Console. Le groupe existant Préviens-moi — tests reste sélectionné. Les notes de version sont en français ; le bundle garde sa signature de dépôt existante. Une recommandation non bloquante demande des symboles natifs de débogage ; le fichier ReTrace est bien joint. Les contrôles [GitHub CI](https://github.com/vbooo/previens_moi/actions/runs/46013665971) passent sur le commit `1ee8a79` : Android, serveur/contrat et recherche de secrets.

Les textes français sont enregistrés comme brouillon Store. Le logo approuvé et une image de présentation 1024×500 ont été préparés et importés dans la bibliothèque de composants, sans publication publique. L’adresse publique confirmée par Fabien est `contact@fabienbouvet.fr`. Les pages locales dans `publication/site` reprennent cette adresse et l’identité validée. La revue visuelle des six captures ordinateur/mobile conclut `ship` au périmètre local ; le documenter conclut `preserve`, sans modifier DESIGN.md ni son sidecar. Cela ne valide ni le contenu juridique ni une mise en ligne.

`publication/data-safety-inventory.draft.json` rassemble les traitements réellement identifiés et les déclarations à finaliser. La politique de confidentialité reste explicitement préparatoire : aucune durée de sauvegarde, conservation de boîte mail, garantie de transfert ou donnée d’hébergement non vérifiée n’est présentée comme établie. Aucune page GitHub Pages créée et aucun lien de confidentialité non fonctionnel saisi dans Play. Le parcours externe de suppression est préparé par e-mail, avec vérification de propriété avant effacement ; le libellé natif vérifié est Compte → Effacer mon compte.

La fiche Store complète, les captures finales, les déclarations et le statut professionnel, l’accès de revue Google, le budget/capacité d’exploitation, l’observation de sept jours et les essais téléphone restent des portes distinctes. La préparation ne constitue pas un feu vert de ventes réelles.

Le contact `contact@fabienbouvet.fr` est ensuite confirmé par « Modification publiée » dans les paramètres Store. Logo et visuel 1024×500 sont associés à la fiche et sauvegardés comme brouillon ; la page signale encore l’absence des captures téléphone requises. Le bandeau générique de modifications non enregistrées a conduit à conserver le premier onglet de brouillon ouvert et à modifier les coordonnées dans un second onglet, sans abandonner de saisie. Aucun accès Drive supplémentaire n’a été accepté ; l’onglet OAuth incident a été fermé. Les six captures du site local ont été recapturées à leurs dimensions réelles après correction du libellé natif et du focus : revue finale `ship`, sans correction matérielle restante au périmètre visuel.
