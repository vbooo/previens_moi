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

## Relevé de départ, sans recherche payante

Le 15 novembre, aucun suivi actif ou en préparation n’a été trouvé : un suivi terminé et trois suspendus par la capacité. Le planificateur se réveille ; les réponses HTTP sont majoritairement 200, avec une 520 isolée dans la fenêtre de six heures. Cela ne prouve pas de vérifications utiles puisque tous les suivis sont arrêtés.

| Compteur partagé actuel | Plafond | Consommé | Réservé | Disponible |
| --- | ---: | ---: | ---: | ---: |
| IA générale, micro-USD | 100 000 | 80 929 | 0 | 19 071 |
| Recherche/lecture générale, crédits Web | 150 | 112 | 29 | 9 |
| Ancien pilote page connue, unités | 14 | 13 | 0 | 1 |

Les 29 crédits réservés restent couverts par prudence : ils concernent des réponses non réconciliées, pas une consommation certaine. Aucun compteur n’a été remis à zéro ou plafond augmenté. Le solde fournisseur Firecrawl relu par GET est de 1 000 crédits ; il est distinct de notre enveloppe interne de 150. Aucune recherche, lecture de page ou requête IA n’a été lancée pour ce relevé.

La marge IA ne permet même plus la prochaine réservation de 20 000 micro-USD. Réactiver un suivi ne suffit donc pas à débloquer l’observation. Les montants consommés sont des estimations de notre registre ; ils ne remplacent pas une facture fournisseur rapprochée.

## Dimensionnement de sept jours

Le calcul reproductible est disponible sans réseau :

```sh
python3 scripts/observe-service.py plan --days 7 --interval-hours 24 --sources 3
```

Pour **un nouveau suivi général continu**, trois sources maximum :

| Cadence | Contrôles sur sept jours | Crédits Web réservables, préparation incluse | Réserve IA maximale |
| --- | ---: | ---: | ---: |
| Quotidienne, proposée | 7 | 26 | 0,20 USD |
| Horaire, règle actuelle des nouveaux plans | 168 | 544 | 3,42 USD |

Ces montants sont des bornes conservatrices, pas une facture : le worker ne rappelle pas l’IA quand le texte est inchangé, mais exige la réserve avant de le découvrir. Les modifications de demande et reprises supplémentaires ne sont pas comprises. Le calcul horaire inclut les recherches de renouvellement tous les 24 passages.

**La cadence quotidienne stable n’est pas encore implémentée.** Le calcul permet de proposer une règle ; il ne ralentit aucun plan existant. Avec l’état courant, il faudrait au minimum 26 crédits Web et 0,20 USD supplémentaires de capacité réservée pour ce seul suivi ; notre marge actuelle ne suffit pas. Le démarrage doit d’abord disposer d’une règle confirmée et d’une enveloppe bornée compatible avec les 2 € cumulés. Aucun essai horaire de sept jours n’est lancé par défaut.

## Protocole d’observation préparé

Le jour 0 sera la date effective de lancement, pas celle de ce document. Avant départ : version exacte de l’app/service, cadence confirmée, sources retenues, résultat attendu et enveloppe sont consignés. Les essais contrôlés existants restent une preuve séparée de la qualité des sources réelles.

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

## Offre gratuite proposée, non appliquée

Pour une première ouverture : **un suivi actif, jusqu’à trois sources, une vérification par jour et sept jours d’essai**, avec 30 crédits Web par compte non renouvelés automatiquement. Garder un plafond IA par compte **et surtout une enveloppe globale**, avec refus d’admission avant les appels si elle est saturée. La protection globale évite qu’une multiplication d’inscriptions épuise les crédits. Une limite d’analyse de demandes et de reprises est également nécessaire avant ouverture ; elle n’existe pas comme nouvelle offre dans ce lot.

Ne pas présenter les crédits techniques dans l’interface : afficher la durée et la fréquence réelles. Cette proposition n’est pas un budget mensuel approuvé, ni une modification de la règle actuelle (trois places par compte et trois globalement). Les suivis actuels ne sont pas ralentis et aucun crédit utilisateur n’a été distribué.

Le fournisseur annonce désormais [1 000 crédits gratuits par mois](https://www.firecrawl.dev/pricing), mais notre enveloppe interne ne se renouvelle pas pour autant automatiquement. Les coûts IA observés sur quelques cas ne garantissent pas le coût de toute demande.

## Portes restant ouvertes avant publication

- Installation et parcours connecté de la version optimisée sur le OnePlus ; matrice des API prises en charge, TalkBack et contrôle final des permissions. Le smoke test release local sur API 36 n’en tient pas lieu.
- Cadence/quota d’observation réellement configurés, puis sept jours exécutés et analysés. Budget d’exploitation publique distinct à fixer.
- Identifiant définitif avant première publication (actuel : `fr.previensmoi.prototype.remote`), identité de signature sécurisée, fiche Store et situation du compte Play.
- Politique publique de confidentialité, voie externe de suppression et sauvegarde indépendante. Ne pas déclarer ces éléments terminés par le seul contrôle de l’APK.
- Modèle actuel : `gpt-4.1-mini-2025-04-14`. La [documentation officielle OpenAI](https://developers.openai.com/api/docs/deprecations) annonce son retrait le **14 janvier 2027**. Prévoir une migration qualifiée avant cette date ; aucun changement de modèle ou essai payant n’est fait ici. La date ne bloque pas un essai de sept jours lancé en novembre.
