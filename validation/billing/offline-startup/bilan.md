# Abonnement conservé au redémarrage hors connexion

Contrôle du 21 novembre 2026, version 0.2.8 (22). Correction locale, **pas encore importée dans Google Play**.

## Résultat

L'ancien code ne conservait l'état de l'abonnement qu'en mémoire. Une fermeture du processus suivie d'un redémarrage sans réseau empêchait de retrouver cet état, sans annuler l'achat Google ni les droits serveur.

Le repository conserve maintenant le dernier état validé par le serveur dans un fichier chiffré par Android Keystore, exclu des sauvegardes. Le cache est lié au compte Préviens-moi. L'écran affiche « Hors connexion · Dernier état connu. » et ne propose pas un nouvel achat. Une date de renouvellement n'est pas présentée comme une certitude hors ligne : la date d'accès déjà confirmée est affichée.

Un achat nouveau ne peut jamais être validé par ce cache. L'accès actif ne dépasse pas la date de validité confirmée ; un retour de l'horloge avant la dernière vérification invalide le cache. Déconnexion/suppression du compte : effacement local. Retour au premier plan ou vérification manuelle : la réponse serveur fraîche remplace le cache. Le contrôle des droits, des plafonds et des suivis côté serveur reste inchangé.

## Vérification

- 96 tests unitaires remote et 87 demo : aucune erreur (une partie des tests est commune aux variantes).
- Régression transport réel : HTTP local devenu inaccessible après recréation du repository ; abonnement actif récupéré depuis le cache.
- Session dont le jeton a expiré sans réseau, compte différent, date limite, recul d'horloge, grâce/résiliation, blocage/expiration après reconnexion, refus serveur, cache absent/illisible et échec d'écriture couverts.
- 4 parcours Compose d'abonnement sur émulateur API 36, dont l'affichage hors connexion sans achat ni faux retour « vérification réussie ».
- Stockage Android natif : relecture par une nouvelle instance, absence de données en clair et rejet d'un fichier altéré.
- Deux exécutions d'instrumentation séparées par un arrêt forcé du processus : état écrit puis relu avec succès depuis le stockage chiffré. Données entièrement synthétiques.
- Lint remoteDebug, demoDebug et remoteRelease : zéro erreur et zéro avertissement ; seules les suggestions de mises à jour des dépendances épinglées restent informatives.
- APK optimisé et AAB signés : inspection de publication réussie, signature comparée au certificat public de dépôt existant. Aucun mot de passe affiché.
- Aucun appel à OpenAI/Firecrawl, aucun achat réel, aucun changement de plafond ou de politique serveur.

Preuves : [contrôles](checks.json), [UI native](native-ui.json), [arrêt et relance du processus](native-process.json).

## Reste à vérifier sur téléphone

Après diffusion de 0.2.8 en test interne : ouvrir Abonnement une fois avec Internet et un abonnement actif, couper Wi-Fi **et** données mobiles, fermer complètement puis rouvrir. Le statut et la date d'accès confirmée doivent rester affichés, avec l'indication hors connexion. Rétablir la connexion et vérifier l'abonnement. Pour les abonnements de licence à renouvellement accéléré, effectuer le redémarrage avant la fin de la période de cinq minutes confirmée : le cache n'invente pas le renouvellement suivant.

Le serveur continue les surveillances selon ses propres droits même si le téléphone est hors ligne. Les actions distantes de l'application requièrent toujours Internet. Ce correctif ne transforme pas le service en application entièrement hors ligne.
