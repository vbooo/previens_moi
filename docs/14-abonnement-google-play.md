# Abonnement Google Play : configuration et qualification

## État

Implémentation Android et serveur de Préviens-moi Plus, sans ouverture des ventes. La politique `private.billing_policy` reste `enabled=false`, `test_only=true`, `monitoring_enabled=false`. Aucun achat réel, aucune hausse du budget global. Le [bilan de validation](../validation/billing/bilan.md) distingue les tests contrôlés de la qualification Google à effectuer.

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

1. Play Console : créer le produit et son forfait ci-dessus, configurer le profil de paiement si demandé. Garder l’application hors production. Le téléchargement est **sans frais** ; le paiement concerne l’abonnement dans l’application.
2. Firebase : ajouter **une nouvelle application Android** au même projet pour `fr.previensmoi.app`. Conserver celle du prototype. Placer sa configuration publique dans `.local/cloud/play-google-services.json`, puis lancer `python3 scripts/configure-android-client.py --play`. Cela écrit `android/play-client.properties` ignoré par Git, sans modifier la configuration du téléphone actuel.
3. Créer une identité serveur Google dédiée à Play Developer API. L’autoriser dans Play Console pour cette application, avec consultation des commandes/données financières et gestion des abonnements nécessaire à la vérification/acquittement. La clé privée reste hors Git/Android. Installer le secret serveur `PLAY_SERVICE_ACCOUNT_JSON`. Aucun rôle propriétaire global n’est nécessaire.
4. Pour RTDN, définir le topic Pub/Sub, accorder à Google Play le droit d’y publier, puis un push authentifié vers la fonction `play-notifications`. Définir `PLAY_RTDN_AUDIENCE` exactement comme l’audience du push et `PLAY_RTDN_EMAIL` comme l’identité autorisée. Ne pas activer un nouveau forfait cloud payant pour ce lot.
5. Préparer une clé de dépôt et un bundle **signé** pour le package définitif, sans clé ni mot de passe dans Git. La clé d’upload n’est pas la clé de debug. Configuration client réelle et signature manquent encore ; un bundle de CI avec valeurs fictives ne doit jamais être envoyé sur Play.
6. Ajouter les comptes de test dans **Tests de licence**, puis sur la piste interne. Une simple invitation sur une piste de test ne garantit pas un paiement fictif : vérifier la présence du moyen de paiement de test avant toute confirmation.
7. Seulement dans cet environnement de test : ouvrir la vérification avec `test_only=true`, sans ouvrir de surveillance supplémentaire tant que l’enveloppe actuelle ne la finance pas. Qualifier les scénarios ci-dessous avant toute vente.

Le prototype reste `fr.previensmoi.prototype.remote` pour conserver ses données et l’observation actuelle. Le package définitif est un autre espace Android ; ses sessions ne migrent pas automatiquement. Construire la variante Play avec `-PplayApplicationId=fr.previensmoi.app`. Le build de publication refuse une configuration Firebase dont le package ne correspond pas.

## Tests Google restant à faire

Avec les comptes de licence et sans débit réel : achat approuvé, refusé, en attente puis approuvé ; restauration après redémarrage/réinstallation ; changement de compte Préviens-moi ; résiliation, renouvellement accéléré, grâce/blocage de paiement, expiration et régularisation ; RTDN valide/dupliquée/désordonnée et rattrapage d’un message perdu ; acquittement serveur. Confirmer aussi le prix localisé, les liens de gestion/résiliation et l’absence de données sensibles dans les logs.

Les tests unitaires/SQL natifs contrôlés ne remplacent pas cette qualification. Ouvrir le paiement réel exige ensuite une décision d’exploitation distincte (budget, capacité, informations contractuelles et confidentialité, gestion des demandes clients), puis une publication explicite.

## Références

- [Intégration Billing Android](https://developer.android.com/google/play/billing/integrate)
- [Sécurité et vérification serveur](https://developer.android.com/google/play/billing/security)
- [Créer les abonnements Play Console](https://support.google.com/googleplay/android-developer/answer/140504?hl=fr)
- [Configurer l’API Google Play](https://developers.google.com/android-publisher/getting_started)
- [Cycle des abonnements](https://developer.android.com/google/play/billing/lifecycle/subscriptions)
