# Abonnement Google Play : configuration et qualification

Point au 19 novembre : achat fictif approuvé, refus, fermeture de la feuille d’achat, renouvellement observé, résiliation jusqu’à expiration et récupération après réinstallation confirmés pendant les essais sur téléphone. Les cas de paiement en attente/défaillant et le raccordement RTDN restent ouverts ; les ventes réelles et la surveillance Plus restent fermées. [État des essais](../validation/billing/play-license-manual-tests.json).

## État

Implémentation Android et serveur de Préviens-moi Plus, sans ouverture des ventes. La vérification des achats de licence est ouverte depuis le 17 novembre : `private.billing_policy` vaut `enabled=true`, `test_only=true`, `monitoring_enabled=false`. Les achats réels restent refusés par le serveur et aucune surveillance Plus supplémentaire n’est ouverte. Aucun achat réel, aucune hausse du budget global. Le [bilan de validation](../validation/billing/bilan.md) distingue les tests contrôlés de la qualification Google à effectuer.

## Catalogue unique

| Champ | Valeur |
|---|---|
| Package Play définitif | `fr.previensmoi.app` |
| Produit abonnement | `previens_moi_plus` |
| Nom | Préviens-moi Plus |
| Forfait de base | `monthly` |
| Type | Renouvellement automatique, chaque mois |
| France | 6,99 € TTC/mois |
| Droits | 2 suivis actifs, 3 sources/suivi, contrôle quotidien, 5 créations/changements de sujet par période |

Le prix affiché par le bouton vient de `ProductDetails` Google, pas d’un prix codé en dur. Une référence à 6,99 € peut être montrée lorsque l’achat est indisponible, mais le bouton reste désactivé. Aucun essai Google supplémentaire : les sept jours gratuits du service ne demandent aucune carte et ne se transforment pas en abonnement. Si l’utilisateur achète Plus pendant son essai, la période payante commence immédiatement ; le paiement n’est pas reporté à la fin des sept jours.

## Parcours

Depuis Compte → Abonnement (ou un suivi dont l’accès est terminé), l’utilisateur consulte l’offre. Google affiche sa feuille d’achat. Android transmet le jeton au serveur avec la session Préviens-moi. **Seule la preuve récupérée auprès de Google ouvre les droits**, jamais un booléen Android. Un paiement en attente ne donne pas accès. La même preuve est liée à un seul compte ; les achats se restaurent au retour au premier plan ou avec Restaurer mes achats.

Le serveur enregistre l’achat avant de l’acquitter chez Google. Une réponse perdue se récupère en reconsultant les achats ; elle ne crée pas de nouvelle période. Le numéro d’ordre Google distingue le renouvellement d’une simple relecture. Un ordre déjà traité ne réinitialise pas les cinq demandes. Le droit annulé reste valable jusqu’à sa date de fin. Blocage de paiement, pause ou expiration suspendent les vérifications, sans supprimer les résultats. Une reprise après régularisation conserve les sources ; une pause volontaire n’est pas réactivée automatiquement.

Les notifications RTDN ne sont pas une preuve d’achat : signature Google OIDC, audience et identité du compte de diffusion sont contrôlées, puis le serveur relit l’état courant dans l’API Google. Le réveil périodique rapproche aussi les achats connus. Un état Google trop ancien empêche de nouvelles dépenses, sans toucher aux essais gratuits. Les jetons d’achat restent dans les tables privées, jamais dans les logs ou l’APK. La suppression d’un compte efface les jetons ; une empreinte non transférable empêche leur réutilisation sur un autre compte.

## Configuration manuelle restante

1. **Catalogue créé le 17 novembre puis activé par Fabien le 19 novembre 2026** : produit `previens_moi_plus`, forfait `monthly`, France, 6,99 € TTC/mois. Le profil de paiement reste à vérifier si Google le demande. Garder l’application hors production. Le téléchargement est **sans frais** ; le paiement concerne l’abonnement dans l’application.
2. **Firebase raccordé le 17 novembre 2026** : application « Préviens-moi Play Store » enregistrée sous `fr.previensmoi.app` dans le projet existant, sur le compte Firebase de Fabien. Configuration téléchargée et validée, puis client Play généré avec `python3 scripts/configure-android-client.py --play`. Les fichiers `.local/cloud/play-google-services.json` et `android/play-client.properties` restent ignorés par Git. L’ancienne application et sa configuration sont conservées. Compilation optimisée, bundle non signé, Lint et inspection binaire passent avec la vraie configuration publique ; cela ne prouve pas encore la réception d’une notification sur cette version. [Contrôles](../validation/billing/firebase-play-configuration.json).
3. **Identité serveur raccordée le 17 novembre 2026** : compte `previens-moi-play-billing` créé sans rôle Cloud global, API Google Play activée après accord de Fabien, accès Play limité à `fr.previensmoi.app` (lecture des informations de l’app/qualité, données financières et gestion des commandes/abonnements). Aucune autorisation globale Play, de publication ou de modification de catalogue. Clé privée `.local/cloud/play-service-account.json`, installée dans `PLAY_SERVICE_ACCOUNT_JSON` sur Supabase avec empreinte vérifiée. La lecture Google du catalogue réussit et confirme produit, forfait, cadence mensuelle et prix français ; cela ne valide pas encore un achat. [Contrôles serveur](../validation/billing/play-server-configuration.json), [lecture du catalogue](../validation/billing/play-catalogue-access.json).
4. Pour RTDN, définir le topic Pub/Sub, accorder à Google Play le droit d’y publier, puis un push authentifié vers la fonction `play-notifications`. Définir `PLAY_RTDN_AUDIENCE` exactement comme l’audience du push et `PLAY_RTDN_EMAIL` comme l’identité autorisée. Ne pas activer un nouveau forfait cloud payant pour ce lot.
5. **Clé de dépôt créée avec accord de Fabien le 17 novembre 2026**, dans `.local/play-signing/`. APK optimisé et bundle signés, signatures comparées au certificat de dépôt, Lint et inspection binaire réussis. Copies privées et mapping R8 conservés dans `.local/play-test/`. Le premier import de la version 0.2.0 (14) a été refusé : la protection automatique Play exige `minSdk >= 24`, contre 23 dans ce bundle. La version 0.2.1 (15), minimum Android 7/API 24, est reconstruite et signée avec la même clé ; 78 tests remote et Lint release réussis. Sa capture confirme maintenant l’import de la version 15 et son minimum API 24. Fabien a retiré l’ancien fichier et publié la version en test interne le 17 novembre à 21:24 (Europe/Paris) ; sa capture montre « Accessible aux testeurs internes ». La clé de dépôt n’est ni celle du debug ni la future clé de signature d’application gérée par Google. [Contrôles de signature](../validation/billing/play-signed-release.json).
6. Ajouter les comptes de test dans **Tests de licence**, puis sur la piste interne. Au 17 novembre, l’adresse du compte Google du téléphone est connue et conservée uniquement dans `.local/cloud/play-test-settings.json`. Après difficulté de contrôle de Safari, Fabien a créé puis sélectionné « Préviens-moi — tests ». Sa capture du 17 novembre confirme la liste cochée avec un utilisateur et aucun changement en attente ; la capture et l’adresse restent privées. `RESPOND_NORMALLY` a été relu précédemment. La configuration de licence est confirmée ; la capture suivante confirme aussi cette liste seule sélectionnée sur la piste interne, avec un utilisateur et le bouton Enregistrer désactivé. Le 19 novembre, le premier achat Google de test est confirmé côté serveur : actif, acquitté, cinq demandes restantes ; la restauration et le cycle complet restent à qualifier. Une simple invitation sur une piste de test ne garantit pas un paiement fictif : vérifier la présence du moyen de paiement de test avant toute confirmation.
7. **Mode serveur de test ouvert le 17 novembre** : `enabled=true`, `test_only=true`, `monitoring_enabled=false`. Les plafonds globaux (300 000 micro-USD et 171 crédits Web) sont conservés ; l’empreinte des essais, suivis et capacités existants est inchangée dans la transaction. Aucun achat connu au moment de cette activation. [Configuration contrôlée](../validation/billing/play-test-mode.json). Qualifier les scénarios ci-dessous avant toute vente ; ce réglage ne rend pas à lui seul les achats Google gratuits.

Le prototype reste `fr.previensmoi.prototype.remote` pour conserver ses données et l’observation actuelle. Le package définitif est un autre espace Android ; ses sessions ne migrent pas automatiquement. Construire la variante Play avec `-PplayApplicationId=fr.previensmoi.app`. Le build de publication refuse une configuration Firebase dont le package ne correspond pas.

## Préparation locale de la signature

La signature est optionnelle et isolée du prototype : `-PplayApplicationId=fr.previensmoi.app -PplaySigning=true`. Elle attend `.local/play-signing/signing.properties`, avec `storeFile`, `storePassword`, `keyAlias` et `keyPassword`. Ce fichier est privé, ignoré par Git. `storeFile` se résout depuis `android/`. La signature est refusée sans le package définitif, avec le service local ou si les réglages/clé manquent. Les compilations habituelles et la CI restent non signées.

Au 17 novembre, la configuration Gradle passe, le refus sans package Play est vérifié et la version signée est contrôlée localement. La génération est dans `scripts/prepare-play-signing.py` : usage unique, refus si le dossier existe, aucun secret affiché. Ne jamais recréer ou remplacer la clé pour lancer une simple compilation. Aucun achat réel ni surveillance payante n’est ouvert ; `enabled=true`, `test_only=true`, `monitoring_enabled=false` ouvrent uniquement la vérification des achats de licence.

L’[inventaire des fichiers et sauvegardes](15-fichiers-locaux-et-sauvegardes.md) distingue les éléments existants des emplacements prévus. Le [protocole d’achats fictifs](16-tests-achats-google-play.md) donne les prérequis et résultats attendus ; le premier achat de licence est validé, les autres parcours Google restent à exécuter.

## Tests Google restant à faire

Avec les comptes de licence et sans débit réel : achat approuvé, refusé, en attente puis approuvé ; restauration après redémarrage/réinstallation ; changement de compte Préviens-moi ; résiliation, renouvellement accéléré, grâce/blocage de paiement, expiration et régularisation ; RTDN valide/dupliquée/désordonnée et rattrapage d’un message perdu ; acquittement serveur. Confirmer aussi le prix localisé, les liens de gestion/résiliation et l’absence de données sensibles dans les logs.

Les tests unitaires/SQL natifs contrôlés ne remplacent pas cette qualification. Ouvrir le paiement réel exige ensuite une décision d’exploitation distincte (budget, capacité, informations contractuelles et confidentialité, gestion des demandes clients), puis une publication explicite.

## Références

- [Intégration Billing Android](https://developer.android.com/google/play/billing/integrate)
- [Sécurité et vérification serveur](https://developer.android.com/google/play/billing/security)
- [Créer les abonnements Play Console](https://support.google.com/googleplay/android-developer/answer/140504?hl=fr)
- [Configurer l’API Google Play](https://developers.google.com/android-publisher/getting_started)
- [Cycle des abonnements](https://developer.android.com/google/play/billing/lifecycle/subscriptions)

## Évolutions commerciales après la V1

Demande de Fabien du 19 novembre 2026 : prévoir une formule annuelle dans une prochaine mise à jour. Étudier aussi une formule à vie, sans activation ni engagement commercial à ce stade : son prix, ses quotas et la prise en charge du coût récurrent des surveillances doivent être décidés avant implémentation. La V1 conserve uniquement `monthly` à 6,99 € TTC/mois en France.

### Correctif local après le premier renouvellement de test

Le renouvellement accéléré a été observé le 19 novembre 2026. La version 0.2.2 (16), signée et disponible localement, corrige l’offre d’achat transitoire pendant la vérification et rend le retour du bouton « Vérifier mon abonnement » visible. Elle doit encore être diffusée en test interne et contrôlée sur le téléphone. [Qualification, artefact et limites](16-tests-achats-google-play.md#renouvellement-et-correction-daffichage--19-novembre-2026). Aucun changement de prix, quota, plafond financier ou ouverture commerciale.
