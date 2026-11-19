# Qualification des achats Google Play sans débit réel

Préparation du 17 novembre 2026. **Premier achat fictif qualifié le 19 novembre 2026.** La restauration, les autres moyens de paiement et le cycle complet restent à vérifier. L’offre est `previens_moi_plus` / `monthly`, France, 6,99 € TTC par mois, package `fr.previensmoi.app`.

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
