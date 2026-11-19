# Qualification des achats Google Play sans débit réel

Préparation du 17 novembre 2026. **Premier achat fictif qualifié le 19 novembre 2026.** Les essais courants, dont la résiliation jusqu’à expiration et la récupération après réinstallation, sont confirmés par Fabien. Les incidents de paiement et le raccordement RTDN restent à qualifier. L’offre est `previens_moi_plus` / `monthly`, France, 6,99 € TTC par mois, package `fr.previensmoi.app`.

## Conditions avant le premier achat

- Identité serveur autorisée uniquement sur cette application ; clé privée installée dans les secrets Supabase, jamais Android.
- Produit et forfait disponibles pour le test ; application hors production publique. L’activation du forfait ne suffit pas à rendre un achat fictif.
- Compte Google utilisé sur le téléphone ajouté dans **Tests de licence** de Play Console. Une invitation aux tests internes seule ne protège pas contre un paiement réel.
- Version portant exactement `fr.previensmoi.app`, avec les bons paramètres Firebase/Supabase. C’est une application distincte du prototype ; il faut s’y reconnecter.
- Serveur en mode `test_only=true`. La surveillance payante reste désactivée pendant la qualification des achats ; aucun crédit IA/Web ne doit être engagé par ces tests.
- À l’ouverture de la feuille Google, vérifier **l’adresse du compte de test**, la mention d’achat de test et le moyen de paiement **« Test card, always approves »** (ou sa traduction). Si une vraie carte ou un compte non test apparaît : annuler. Ne pas valider un débit réel pour essayer.

Google permet aux comptes de licence de tester une version installée directement, même signée en debug, avec le même package. La piste interne et le bundle signé restent nécessaires pour qualifier aussi la diffusion réelle. [Documentation des comptes et moyens de paiement de test](https://developer.android.com/google/play/billing/test).

## Version locale disponible

`.local/play-test/previens-moi-play-test-0.2.0-14.apk` : version de développement, package Play et signature debug vérifiés. Elle n’a pas été installée sur le téléphone. Le fichier `.local/play-test/previens-moi-play-release-0.2.1-15.aab` est signé avec la clé de dépôt pour préparer la piste interne ; la version précédente 0.2.0 a été refusée par Google pour son minimum SDK 23. La capture de Fabien confirme l’import de la 0.2.1 avec API 24 ; la version est maintenant accessible aux testeurs internes et la liste prévue est sélectionnée. Ne pas tenter une mise à jour entre un APK debug et une version signée autrement sous le même package : Android refuse la signature différente. Le prototype a un autre package et peut rester installé.

Lecture Google du 19 novembre 2026 : forfait `monthly` maintenant `ACTIVE`, mensuel, France à 6,99 € TTC. Fabien confirme avoir installé la version de test depuis Google Play. Le premier achat de licence est maintenant confirmé en base : achat de test, état actif, acquittement Google enregistré et cinq demandes restantes. La surveillance Plus reste désactivée et les plafonds globaux sont conservés. [Contrôle du premier achat](../validation/billing/play-first-license-purchase.json).

Pour éviter qu’un achat de test ne transforme l’essai gratuit d’un compte déjà utilisé, se connecter dans la nouvelle application avec un **compte Préviens-moi réservé aux tests**. Le compte Google Play choisi pour le paiement doit rester celui inscrit comme testeur de licence. Les deux identités jouent des rôles différents.

Au 17 novembre, le serveur accepte uniquement les preuves Google marquées comme achats de test ; les droits Plus peuvent être contrôlés, mais l’exécution de nouvelles surveillances Plus reste fermée pour préserver l’enveloppe. Ce lot ne valide donc pas l’utilisation de deux suivis Plus réels sur le Web.

## Parcours à effectuer et à consigner

| Test | Action | Résultat attendu |
|---|---|---|
| Refus | Choisir la carte de test qui refuse toujours. | Aucun droit Plus ; retour compréhensible et nouvelle tentative possible. |
| Annulation | Fermer la feuille d’achat. | Pas d’achat ni droit créé ; aucun message de réussite. |
| En attente | Choisir la carte lente, approuvée ensuite ; fermer puis rouvrir l’app avant approbation. | Paiement en attente ; pas de droit avant validation Google. |
| Achat approuvé | Utiliser la carte de test toujours approuvée. | Vérification serveur puis Plus actif ; acquittement Google confirmé ; surveillance externe toujours fermée. |
| Restauration | Fermer/redémarrer, puis réinstaller la version et se reconnecter au même compte Préviens-moi. | Même achat retrouvé, sans nouvelle facturation ni remise à zéro des cinq demandes. |
| Autre compte Préviens-moi | Se connecter à un autre compte dans l’app avec le même achat Google présent. | Pas de transfert silencieux du droit ; information explicite. |
| Renouvellement | Attendre le renouvellement accéléré de test, puis actualiser plusieurs fois. | Une seule nouvelle période pour un même ordre Google ; pas de remise à zéro répétée. |
| Résiliation | Résilier le test dans Google Play. | Accès conservé jusqu’à la fin de période puis expiré ; résultats précédents accessibles. |
| Paiement défaillant | Utiliser Play Billing Lab pour grâce, blocage et régularisation. | Grâce conforme à Google ; blocage/expiration sans surveillance ; récupération sans réactiver une pause volontaire. |
| Notifications serveur | Envoyer le test RTDN, contrôler doublons, ordre inversé et rattrapage périodique. | Seul l’état relu auprès de Google fait foi ; aucune notification non authentifiée acceptée. |

Une souscription mensuelle de test se renouvelle en temps accéléré. Ne pas coder ces durées de test dans le produit. Consigner l’heure et l’état réels plutôt qu’attendre un mois. [Tests des abonnements et Play Billing Lab](https://developer.android.com/google/play/billing/test#subs).

## Preuves à garder

Pour chaque cas : version installée, heure, état affiché, état serveur agrégé, résultat attendu/obtenu. Garder les captures privées si elles montrent un e-mail ou une commande. Ne jamais publier de jeton d’achat, secret, clé, session ou données personnelles dans GitHub.

L’absence d’achat réel ne prouve ni la rentabilité ni la disponibilité du moteur de surveillance. La qualification Play, les notifications Android, le fonctionnement Web pendant sept jours et l’ouverture commerciale sont des validations distinctes. Le plafond cumulé de 2 € et les compteurs actuels ne sont pas remis à zéro.

## Observations du premier achat — 19 novembre 2026

Google affichait explicitement la carte de test toujours approuvée et l’absence de facturation. Fabien confirme le mail Google et l’écran « Abonnement actif », puis le maintien de cet état après fermeture depuis les applications récentes et réouverture. La lecture serveur confirme un achat de test actif, acquitté et cinq demandes restantes. Les plafonds globaux restent inchangés et la surveillance Plus demeure fermée. Le bouton de restauration, la réinstallation, les autres moyens de paiement, RTDN et le cycle complet ne sont pas déduits de ce seul résultat.

## Renouvellement et correction d’affichage — 19 novembre 2026

Sur la version 0.2.1 installée par Google Play, Fabien confirme le mail de renouvellement après cinq minutes. Une lecture serveur en consultation seule confirme deux fins de période successives, un achat de test toujours actif et acquitté, et cinq demandes restantes. Les deux périodes n’ont consommé ni création, ni crédit IA/Web. Ce constat valide un renouvellement accéléré observé ; il ne qualifie pas encore la résiliation, le blocage, la réinstallation ou RTDN.

Le bouton de restauration a été utilisé : l’état actif a été conservé, mais le retour visuel était trop discret. Fabien a aussi vu brièvement le bouton d’achat après navigation. Le code pouvait publier un état serveur expiré avant de rapprocher l’achat présent dans Google Play.

**Correction locale 0.2.2 (16), pas encore diffusée :** rapprocher les achats avant d’afficher une offre après un état expiré, masquer l’achat pendant la vérification et empêcher un achat lorsque la vérification a échoué. Le bouton devient « Vérifier mon abonnement » ; une Snackbar Material 3 confirme le résultat visible en bas de l’écran. Un échec de lecture serveur ne peut plus produire une fausse confirmation de réussite. Les droits restent exclusivement accordés par le serveur.

Validation : 81 tests unitaires remote, Lint debug/release sans erreur ni avertissement (14 suggestions de mise à jour de dépendances par variante), deux tests d’écran contrôlés en clair/français et en sombre/anglais avec police à 130 %. Une première capture sombre était masquée par un blocage de Pixel Launcher sur l’émulateur ; elle a été remplacée après arrêt du lanceur, sans modification de l’application. Le bundle et l’APK sont signés avec la clé de dépôt existante et contrôlés avec le package définitif et un minimum API 24. [Preuves et limites](../validation/billing/play-renewal-fix.json).

Bundle à importer dans une **nouvelle version de la piste interne** : `.local/play-test/previens-moi-play-release-0.2.2-16.aab`. Ce fichier local n’est pas dans GitHub. Après diffusion et mise à jour depuis Play, vérifier le retour sur Abonnement autour d’un renouvellement, puis le bouton « Vérifier mon abonnement », sans nouvel achat. Le comportement réel de cette correction n’est pas encore validé sur le téléphone. Aucun appel fournisseur métier payant supplémentaire ; plafond cumulé de 2 € conservé, surveillance Plus et ventes réelles toujours fermées.

## Tests sur la piste interne 0.2.2 — 19 novembre 2026

La capture Play Console confirme la publication de la 0.2.2 (16), accessible aux testeurs internes le 19 novembre à 11 h 13 (Paris). Le téléphone montre le nouveau bouton « Vérifier mon abonnement ». L’ancien achat de test est expiré en base, avec une fin de période à 11 h 08 et une lecture Google enregistrée à 11 h 47 ; le bouton confirme qu’aucun abonnement actif n’est retrouvé.

La carte de test toujours refusée déclenche bien le refus Google. Fabien confirme ensuite la fermeture de la feuille d’achat, le maintien de l’état terminé et l’absence de chargement bloqué. Un nouvel achat fictif approuvé a permis de vérifier la résiliation : Google affiche « Annulé », l’application « Résiliation prise en compte », avec accès jusqu’à 12 h 01. L’expiration après cette résiliation reste à confirmer sur le téléphone ; le cycle complet n’est pas déclaré terminé.

Le retour du bouton de vérification affichait encore « Ton abonnement est bien actif » tant que l’accès restait ouvert après résiliation. Ce message est corrigé **localement, hors de la version 0.2.2 publiée**, en français et anglais : « Résiliation confirmée. Ton accès reste disponible jusqu’à la fin de la période. » Aucun droit d’accès ni quota n’est modifié. Le test d’écran couvre maintenant actif → résilié avec accès → expiré, avec la confirmation adaptée et l’absence de nouvelle offre d’achat avant expiration. 81 tests unitaires remote et Lint passent ; les deux tests d’écran passent en français, en clair et en sombre avec police 130 %. [Bilan des essais et limites](../validation/billing/play-license-manual-tests.json). Aucun appel fournisseur métier payant supplémentaire.

### Confirmation de fin de période et de récupération après réinstallation

Fabien confirme ensuite l’état « Abonnement terminé » après la fin de période de la souscription résiliée, puis le succès du test suivant : nouvel achat fictif approuvé, désinstallation/réinstallation depuis Google Play, reconnexion et récupération de l’abonnement. Sa confirmation ne précise pas si la récupération était automatique ou obtenue par le bouton de vérification ; ce point n’est donc pas déduit. Les parcours courants sont maintenant couverts par les observations utilisateur, sans prétendre qualifier les paiements en attente, la grâce/le blocage, le changement de compte ou RTDN.


## Refus de renouvellement et suspension — 19 novembre 2026

Le testeur confirme un nouveau paiement fictif accepté puis un renouvellement refusé : l’accès est maintenu pendant les cinq minutes de grâce accélérées, puis il est coupé. La lecture directe de l’API Google a confirmé `SUBSCRIPTION_STATE_ON_HOLD`, avec une date de fin d’accès passée et un achat acquitté. Le serveur convertissait à tort tout état dont cette date était passée en `expired`, et la lecture SQL reproduisait cette conversion. Un ancien achat expiré, vérifié plus récemment, pouvait également masquer l’achat récupérable.

La correction conserve les états Google `hold` et `paused` même lorsque l’accès est fermé, privilégie l’abonnement en cours lors de la lecture et de la vérification manuelle, et garde le contrôle de date pour l’ouverture effective des droits. La migration `202611190041` et les fonctions `api`, `worker` et `play-notifications` sont déployées ; la facturation Cloud et les droits IAM sont inchangés. Le mode achats fictifs reste imposé, la surveillance payante reste fermée et les plafonds restent à 300 000 micro-USD / 171 crédits Web.

La version Android 0.2.3 (17), signée et contrôlée localement, distingue le refus avec accès maintenu jusqu’à une date et la suspension à régulariser. Le bouton « Vérifier mon abonnement » reprend l’état connu au lieu de déclarer qu’aucun abonnement n’existe. Aucun nouveau paiement n’est proposé pendant la suspension. La confirmation de résiliation corrigée précédemment est incluse dans cette version.

Les 138 tests serveur, 82 tests unitaires Android et le scénario de base PostgreSQL jetable couvrent notamment grâce → suspension avec date passée → récupération sans remise à zéro des quotas, ainsi que la présence d’anciens achats expirés. Cela ne remplace pas le contrôle sur téléphone après diffusion ni la qualification RTDN, qui reste bloquée par l’absence de facturation Cloud. La récupération d’un paiement refusé sur le téléphone reste à confirmer après mise à jour.


Contrôle natif : les trois parcours passent en français clair à 100 % et sombre à 130 %. Les quatre [captures contrôlées](../validation/billing/payment-ui/) sont sans données utilisateur ; elles ne prouvent pas une récupération de paiement réelle. Lint debug/release : aucune erreur ni avertissement. Le [rapport de version](../validation/billing/payment-recovery-release.json) garde la publication interne et le contrôle sur téléphone à `false` tant qu’ils ne sont pas observés. L’import 0.2.3 est préparé dans Safari mais le sélecteur de fichier ne se valide pas depuis l’automatisation ; intervention manuelle demandée. [Déploiement serveur](../validation/billing/payment-recovery-deployment.json).


La version 0.2.3 (17) est publiée le 19 novembre 2026 à 13 h 42 (Paris, précision minute affichée par Google) dans le canal interne existant : statut « Accessible aux testeurs internes » observé après confirmation. Aucun déploiement en production. Le [contrôle GitHub du correctif](https://github.com/vbooo/previens_moi/actions/runs/44524820232) est réussi. La récupération du paiement sur téléphone reste à contrôler après installation de cette mise à jour ; l’ancien blocage du sélecteur de fichier est résolu par l’intervention du testeur.
