# Préparer la diffusion et l’observation

État du 15 novembre 2026 : contrôles release effectués, observation **préparée mais non démarrée**, aucune ouverture publique du service ni dépense nouvelle autorisée par ce document. Le plafond reste de **2 € cumulés pour les essais**, pas par compte et pas par mois.

## Version optimisée

`remoteRelease` est désormais disponible : R8 et réduction des ressources activés, application non débogable, sauvegardes désactivées et réseau HTTPS uniquement, y compris pour localhost. Les exceptions HTTP de l’émulateur sont limitées à `src/remoteDebug`. `demoRelease` reste désactivée.

La tâche `validateRemoteReleaseConfiguration` refuse le mode de service local, les adresses HTTP ou `.invalid`, une configuration Firebase incomplète et une clé Supabase non publique. Les bibliothèques apportent leurs règles de conservation R8 ; aucune règle globale gardant tout le code n’a été ajoutée.

Avec `android/client.properties` correctement renseigné, depuis `android/` :

```sh
./gradlew assembleRemoteRelease bundleRemoteRelease lintRemoteRelease
python3 ../scripts/check-android-release.py
```

L’APK et l’AAB restent **non signés**. La copie installée sur l’émulateur de qualification a été signée uniquement avec la clé debug existante ; ce n’est pas une identité de production. Ne pas distribuer cette copie comme une version Play. Conserver le mapping R8 avec chaque véritable version publiée.

La CI construit aussi l’APK et le bundle optimisés avec des paramètres publics synthétiques, vérifie quatre refus de configuration et inspecte le binaire :

```sh
python3 scripts/check-android-release.py --build-fixture
```

Les paramètres `.example.test` ne contactent aucun serveur. La CI ne publie pas cet APK release et ne reçoit aucun secret. Un succès CI prouve la construction, pas le fonctionnement d’un service réel.

Sources : [activation de R8](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization), [préparation release](https://developer.android.com/studio/publish/preparing), [tests et mapping R8](https://developer.android.com/topic/performance/app-optimization/troubleshoot-the-optimization).

## Règle appliquée et enveloppe au 15 novembre

Version **0.1.11** : un suivi actif par compte, trois sources publiques maximum et contrôle toutes les 24 heures. Sept jours à partir de la première activation confirmée, sans renouvellement lors d’une pause, reprise, modification ou suppression du suivi. L’essai peut s’arrêter plus tôt si son enveloppe est épuisée ; les résultats restent disponibles. Les anciens suivis gardent leurs paramètres et n’ont pas été réactivés.

| Compteur partagé après activation de la règle | Plafond | Consommé | Réservé incertain | Disponible hors essais engagés |
| --- | ---: | ---: | ---: | ---: |
| IA générale, micro-USD | 300 000 | 80 929 | 0 | 219 071 |
| Recherche/lecture générale, crédits Web | 171 | 112 | 29 | 30 |

À ce relevé aucun essai n’est commencé, aucun budget de compte engagé. Le premier essai réserve atomiquement 180 000 micro-USD et 30 crédits Web dans cette enveloppe globale. Une autre admission ne peut pas dépenser cette somme engagée. Les parties inconnues d’un appel restent réservées ; les compteurs ne sont pas remis à zéro. Aucun abonnement ou recharge. Borne cumulée conservatrice inchangée : 0,99587625 € sous 2 €, sans prétendre à un rapprochement de facture. [Transaction et versions déployées](../validation/daily-trial/deployment.json).

Dimensionnement reproductible :

```sh
python3 scripts/observe-service.py plan --days 7 --interval-hours 24 --sources 3
```

Le scénario conservateur compte jusqu’à sept contrôles, préparation et compréhension comprises : 26 crédits Web et 0,20 USD de réservations IA, dont 0,02 USD de compréhension avant confirmation. L’enveloppe après confirmation est de 0,18 USD. Ces bornes incluent le contrôle de bord à sept jours, qui est refusé dès expiration ; elles ne promettent pas sept alertes. Les modifications répétées et incidents ne sont pas compris dans cette prévision simple et consomment le même plafond. Les sources restent fixes sur cette période : le renouvellement historique tous les 24 passages ne s’applique pas avant la fin des sept jours quotidiens.

## Protocole d’observation préparé

Le jour 0 sera la date effective de lancement, pas celle de ce document. Avant départ : exécuter `python3 scripts/check-model-access.py` (lecture gratuite des métadonnées, pas une génération), vérifier la correspondance de la clé déployée, puis consigner version exacte de l’app/service, cadence confirmée, sources retenues, résultat attendu et enveloppe sont consignés. Les essais contrôlés existants restent une preuve séparée de la qualité des sources réelles.

| Moment | Vérification | Preuve attendue |
| --- | --- | --- |
| J0 | Installer la version qualifiée sur OnePlus, activer un suivi continu admissible ; noter les sources et le résultat initial. | Activation confirmée, information déjà présente distinguée du nouveau, aucune attente cachée. |
| J1–J7 | Laisser l’application fermée et relever chaque jour les contrôles dus et exécutés. | Dernière réussite, retards, pannes ou quota distingués ; coûts et réservations conservés. |
| Pendant l’essai | Comparer chaque notification au contenu exact de sa source. | Sujet et lieu corrects, opportunité identifiable, aucun même fait renotifié. La même URL peut légitimement apporter un autre fait. |
| Cas contrôlés | Éprouver ajout, répétition identique, erreur de source puis retour à la normale ; réutiliser les tests locaux avant tout essai externe. | Une seule alerte utile ; panne visible ; pas de fausse réussite. Une injection locale ne prouve pas le Web réel. |
| Un parcours OnePlus | Permission de notification refusée, retour depuis une notification, perte de réseau, pause/reprise. | Résultat accessible dans l’app, état distant confirmé, pas de commande répétée accidentelle. |
| J7 ou arrêt anticipé | Comparer attendus, résultats, doublons, échéances, incidents et dépenses. | Bilan honnête. Sept jours silencieux, un quota épuisé ou une capture de cron ne constituent pas sept jours de surveillance réussie. |

On arrête les nouvelles opérations payantes si la réserve ne peut être garantie. Un quota atteint doit rester visible ; ne pas effacer les comptes ou réservations pour prolonger artificiellement la durée. Aucun robot ou tâche récurrente supplémentaire n’est installé par ce lot.

Pour les relevés, avec Python et `service/requirements-test.txt` installés :

```sh
python3 scripts/observe-service.py snapshot
```

Le script sélectionne des **compteurs agrégés**, dans une transaction PostgreSQL en lecture seule. Aucun titre, texte de demande, e-mail, identifiant utilisateur, URL de source, jeton ou secret n’est exporté. Les rapports horodatés restent dans `.local/observation/`, ignorés par Git et accessibles au seul compte du Mac. Ne jamais lancer un script historique de qualification ou un worker pour faire un simple relevé.

Le succès cron prouve le réveil de la base, pas une notification affichée. Les réponses pg_net sont une courte fenêtre à l’échelle du projet ; un envoi FCM accepté ne prouve ni affichage sur le téléphone ni pertinence du résultat. Le script n’annonce jamais automatiquement sept jours validés.

## Paiement différé

L’essai ne se transforme pas en abonnement : aucun débit automatique, prix ni offre payante configurés. Le futur paiement Android pourra passer par Google Play Billing avec contrôle des droits sur le serveur ; il constitue un lot distinct à définir, pas une condition pour tester cette V1 gratuite. Voir le [cadre économique courant](07-couts-et-modele-economique.md).

## Portes restant ouvertes avant publication

- Installation et parcours connecté de la version optimisée sur le OnePlus ; matrice des API prises en charge, TalkBack et contrôle final des permissions. Le smoke test release local sur API 36 n’en tient pas lieu.
- Cadence/quota d’observation configurés ; restent sept jours réellement exécutés et analysés. Budget d’exploitation publique distinct à fixer.
- Identifiant définitif avant première publication (actuel : `fr.previensmoi.prototype.remote`), identité de signature sécurisée, fiche Store et situation du compte Play.
- Politique publique de confidentialité, voie externe de suppression et sauvegarde indépendante. Ne pas déclarer ces éléments terminés par le seul contrôle de l’APK.
- Modèle actuel : `gpt-4.1-mini-2025-04-14`. La [documentation officielle OpenAI](https://developers.openai.com/api/docs/deprecations) annonce son retrait le **14 janvier 2027**. Prévoir une migration qualifiée avant cette date ; aucun changement de modèle ou essai payant n’est fait ici. La date ne bloque pas un essai de sept jours lancé en novembre.
