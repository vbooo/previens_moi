# Services de surveillance — audit actualisé

> **Décision courante de Fabien : Yutori abandonné.** Inscription impossible selon son retour et coût jugé élevé. Aucun essai exécuté. Les comparaisons documentaires ci-dessous restent historiques ; aucune demande de clé ni lancement Y1.


> **Observation complétée le 12 octobre 2026 :** deux passages Exa Monitor produisent un format différent du schéma demandé, malgré sa conservation distante ; webhook signé fonctionnel, suivi en pause. Le même schéma fonctionne avec Search, qui retrouve 9 concerts cohérents avec le calendrier officiel mais affirme 4 dates de publication contredites par les pages et 1 non prouvée. La confiance élevée n'est pas une preuve de nouveauté. [Bilan audité](../engine-lab/reports/monitor-result-20261012.json). Firecrawl F0 exécuté : deux passages, 7 crédits, résultats exploitables mais récence non fiabilisée ; suivi en pause. [Bilan F0](../engine-lab/reports/firecrawl-result-20261012.json). Aucun prestataire de production retenu.


Vérifié le **12 octobre 2026**. Recommandation d'essai, **aucun fournisseur de production adopté**. Les recherches Search, l'extraction ponctuelle et l'essai IA du 9 octobre sont décrits dans le [bilan](09-bilan-premier-moteur.md) ; **au moment de cet audit initial, aucun Monitor n’avait encore été essayé ; les observations D0/F0 ci-dessus le remplacent pour les capacités effectivement testées**. La revue du 3 octobre est conservée plus bas comme historique, avec ses recommandations et tarifs datés.

> **F1, 12 octobre :** Firecrawl réussit les quatre cas de changement d'une page connue synthétique, pour 6 crédits. Ce résultat ne corrige pas les limites de découverte F0. Yutori a ensuite été retiré des essais par Fabien ; aucune clé demandée. [Bilan F1](../engine-lab/reports/firecrawl-controlled-result-20261012.json).

## Conclusion exploitable

Il existe des services qui font déjà de la surveillance récurrente. Nous ne devons pas reconstruire recherche, scraping, jugement et ordonnanceur par défaut. Cela ne supprime ni les limites de couverture du Web ni notre responsabilité sur la promesse, les preuves, les états utilisateur, les doublons et les dépenses. Une fonctionnalité documentée reste à essayer.

**Recommandation initiale avant essais, dépassée par les observations ci-dessus :** conserver **Exa Monitors comme premier candidat pour la découverte D**, pour son coût affiché et son déclenchement manuel documenté. Réserver **Firecrawl Monitor à l'essai P sur page connue**, puis à une comparaison D si elle résout une lacune observée. Yutori est un témoin éventuel, pas le moteur gratuit par défaut. Cette recommandation maintient l'ordre du 3 octobre et sépare les mécanismes ; elle n'engage aucune dépense ni intégration aujourd'hui.

## Capacités et responsabilités

| Service | Documenté par le fournisseur | Limite / travail qui reste chez nous |
| --- | --- | --- |
| [Exa Monitors](https://exa.ai/docs/monitors/quickstart) | Recherches récurrentes, résultats nouveaux, synthèse ou JSON avec références, historique et webhook signé. Mode manuel sans `trigger`. Cadence minimale 1 h, retard possible de 30 min ; un chevauchement annule le passage précédent. | Évaluer requête, preuve et nouveauté métier. Ne pas assimiler découverte et comparaison d'une page existante. Mesurer réception, reprise, arrêt et facturation. |
| [Firecrawl Monitor](https://docs.firecrawl.dev/features/monitoring) | Cibles page, site ou recherche ; comparaison, jugement optionnel, historique et webhooks. Cadence minimale 5 min. | Valider les conditions métier et l'état du suivi. Crédit épuisé : passage sauté ; le monitor conserve sa planification et reprend si des crédits arrivent. La suspension explicite reste nécessaire. |
| [Firecrawl recherche récurrente](https://docs.firecrawl.dev/features/monitoring-web-scale) | Requêtes de recherche + objectif de jugement, déduplication par URL. | Le jugement ne trouve pas une source absente des résultats. Plusieurs URLs peuvent désigner le même fait ; une URL déjà connue qui change relève de la voie P. |
| [Yutori Scouting](https://yutori.com/scouting) | Recherche récurrente depuis une consigne, résultats et webhook ; cadence minimale annoncée 30 min. | Couverture, pertinence, preuve, gestion des incidents et coût à mesurer. La page commerciale ne constitue pas un résultat d'essai. |

Pour tous : notre service conserve les critères confirmés, versions, comptes, quotas, état, déduplication métier et notifications Android. La signature d'un webhook prouve son origine, pas la vérité du contenu. Un indicateur de confiance fournisseur n'est pas une probabilité de fiabilité validée sur notre produit. Les données structurées/API/RSS restent une voie distincte lorsqu'elles couvrent effectivement la demande.

## Coût récurrent, même sans alerte

Hypothèse de comparaison : une surveillance, un passage quotidien, 30 jours ; hors taxes/change, backend, stockage, admission et traitements complémentaires. **Ce sont des calculs à partir des tarifs, pas des consommations observées ni une prévision d'audience.**

| Configuration | 1 suivi / mois | 30 suivis / mois | Source et limites |
| --- | --- | --- | --- |
| Exa, requête Monitor de base, au plus 10 résultats | 0,45 USD | 13,50 USD | [15 USD / 1 000 requêtes](https://exa.ai/pricing). Résumés de pages et options supplémentaires à comptabiliser séparément ; vérifier le débit réel. |
| Firecrawl, une page simple + juge optionnel, au plus un jugement par passage | 30 à 60 crédits | 900 à 1 800 crédits | [1 crédit par lecture + 1 par page changée jugée](https://docs.firecrawl.dev/features/monitoring). Sans nouveau changement, la lecture consomme encore. |
| Firecrawl Search, une requête, au plus 10 résultats jugés par passage | 60 à 360 crédits | 1 800 à 10 800 crédits | [2 crédits / 10 résultats + 1 par résultat jugé](https://docs.firecrawl.dev/features/monitoring-web-scale). Hors options et requêtes additionnelles. |
| Yutori, un Scout | 10,50 USD | 315 USD | [0,35 USD par passage](https://yutori.com/pricing). |

[Firecrawl](https://www.firecrawl.dev/pricing) annonce 1 000 crédits mensuels gratuits ; Hobby coûte 19 USD au mois pour 5 000 crédits, ou 16 USD/mois avec facturation annuelle. Les recharges automatiques des offres payantes se font par 5 USD : elles ne sont pas autorisées ici. [Exa](https://exa.ai/pricing) annonce 10 USD gratuits mensuels, en plus du bonus d'accueil. Le solde du compte et l'éligibilité restent à vérifier avant essai ; ces gratuités ne financent pas une offre utilisateur illimitée.

**Incohérence à résoudre avant extraction structurée Firecrawl :** la page tarifaire annonce 7 crédits par page/contrôle pour l'extraction déterministe Monitor ; la documentation générale renvoie aux suppléments du scrape. Ne pas convertir cette incertitude en tarif garanti. Le premier essai P peut utiliser le mode Markdown simple, avec compteur réel, sans prétendre valider le mode JSON.

## Inconnues et conditions de passage

| Point | État documentaire / prochaine preuve | Bloque quoi ? |
| --- | --- | --- |
| Accès et facturation | Vérifier comptes, crédits, clé locale, options et absence de recharge ; ne pas redemander une clé déjà disponible. | Création d'un essai réel. |
| Arrêt des dépenses distantes | Exa documente le mode manuel ; Firecrawl expose création, mise à jour, exécution et suppression. La [création Firecrawl](https://docs.firecrawl.dev/api-reference/endpoint/monitor-create) demande une planification ; ne pas supposer un mode manuel seul ou l'exécution d'un monitor en pause. Préparer arrêt et contrôle final avant activation. | Lancement de chaque monitor ; un compteur local seul ne suffit pas. |
| Réception et incidents | Préparer endpoint HTTPS, signature, persistance avant accusé de réception, rejeu et récupération de l'historique. Vérifier les modalités exactes des reprises du fournisseur choisi. | Déclaration de chaîne complète fonctionnelle. |
| Couverture et précision | Aucun résultat Monitor mesuré. Attendus et source de référence indépendants, cas positifs, bruit, ancienneté, correction et panne à figer dans le protocole. | Promesse MVP et choix de production. |
| Usage et données | [Conditions Exa](https://exa.ai/terms), [Firecrawl](https://www.firecrawl.dev/terms-of-service) et [Yutori](https://yutori.com/tos) identifiées ; aucun avis juridique ou accord de sous-traitance conclu par cette revue. Vérifier obligations de réutilisation des contenus, données transmises, conservation, suppression et sous-traitants pour la configuration retenue. | Données personnelles et production. Les essais préparatoires utilisent des demandes synthétiques et sources publiques autorisées. |
| Coût total | Mesurer contrôles vides, incidents et traitements additionnels, pas uniquement tokens IA ou alertes émises. | Quotas et budget mensuel à valider à l'étape 3. |

La première lecture documentaire est terminée ; ces inconnues sont des points de contrôle explicites, pas des capacités supposées. Le [plan courant](../docs/10-plan-de-validation-technique.md) gouverne la suite. Le plafond autorisé reste 2 € cumulés ; aucun nouvel appel facturable ni abonnement n'est lancé par cet audit. Le montant historique de 50 € ci-dessous n'est pas une autorisation.

---

# Revue historique du 3 octobre 2026

Date de vérification : 3 octobre 2026. Statut : **comparaison de capacités annoncées et de prix publics ; aucun service essayé ni choisi**. Cette note répond à la question du [plan de validation](../docs/10-plan-de-validation-technique.md) : quel moteur peut accepter des demandes libres, chercher des nouveautés dans la durée et rester soutenable pour une V1 Android gratuite ? Les prix en dollars ci-dessous ne comprennent ni taxes, ni change, ni service central, ni notifications, ni analyse complémentaire. Les promesses des fournisseurs ne constituent pas des mesures de fiabilité.

## Comparer la même unité de travail

L'unité de référence est **une demande active vérifiée une fois par jour pendant 30 jours**, même si aucune nouveauté n'apparaît. Distinguer : création et clarification, découverte des sources, exécution des recherches, extraction du contenu, appréciation de la pertinence, déduplication, planification, notification et reprise après incident. Un appel de recherche seul n'inclut pas toute cette chaîne.

| Option | Ce qui est réellement fourni | Prix public pour 30 contrôles quotidiens, sous hypothèses indiquées | Travail restant chez nous / incertitude principale |
| --- | --- | --- | --- |
| [Yutori Scouting API](https://docs.yutori.com/) | Consigne libre ; agent de recherche récurrent ; cadence, résultats et webhook. | [0,35 $ par exécution](https://docs.yutori.com/pricing), soit **10,50 $** pour 30 passages. | Vérifier couverture et précision réelles ; notre service gère compte, limites, preuve, état et notifications. Prix élevé par demande gratuite. |
| [Exa Monitors](https://exa.ai/docs/monitors/quickstart) | Recherche récurrente à partir d'une requête, filtrage temporel, résultats déjà vus écartés, sortie textuelle ou structurée avec références et webhook. Intervalle minimal d'une heure ; une exécution peut avoir jusqu'à 30 min de retard. | [15 $ / 1 000 requêtes](https://exa.ai/pricing), soit **0,45 $** pour 30 exécutions de base, si une requête facturable correspond à chaque passage ; vérifier la facture et les options qui ajoutent des coûts. | Transformer la demande en requête de recherche juste, établir ce que les premiers résultats couvrent, contrôler pertinence et preuves. Une recherche récurrente n'est pas automatiquement équivalente au travail multi-agent de Yutori. **Premier candidat à essayer**. |
| [Firecrawl Monitor, cible `search`](https://docs.firecrawl.dev/features/monitoring-web-scale) | Requêtes de recherche planifiées, fenêtre de récence, déduplication par URL, juge des résultats, webhook. Peut aussi surveiller des pages ou sites connus. | [2 crédits par 10 résultats recherchés et par contrôle, plus 1 crédit par résultat jugé](https://docs.firecrawl.dev/features/monitoring-web-scale). Une requête, jusqu'à 10 résultats et 30 passages donnent **au moins 60 crédits de recherche**, avant résultats jugés et autres cibles ; [1 000 crédits gratuits par mois](https://www.firecrawl.dev/pricing). | Produire nous-mêmes les mots-clés : la consigne donnée au juge ne génère pas les requêtes. Coût variable selon nombre de requêtes et résultats ; déduplication par URL, donc plusieurs articles sur un fait peuvent produire plusieurs alertes. **Second candidat à essayer**. |
| [Recherche Web OpenAI + notre ordonnanceur](https://developers.openai.com/api/docs/guides/tools-web-search) | Recherche actuelle avec citations ; aucun suivi récurrent ni comparaison historique fournis par cet appel seul. | [10 $ / 1 000 appels à l'outil, plus tokens du modèle et du contenu](https://developers.openai.com/api/docs/pricing), soit **0,30 $ de frais d'outil** pour 30 appels si chaque contrôle n'en utilise qu'un. Ce n'est **pas** un coût complet. | Nous devons construire planification, historique, fraîcheur, déduplication, contrôle des sources, erreurs et estimation des tokens. Option de référence si les moteurs gérés échouent ou pour qualifier un cas difficile. |
| [Brave Search API + notre ordonnanceur](https://brave.com/search/api/) | Résultats de recherche Web, sans surveillance récurrente ni jugement de pertinence pour notre demande. | [5 $ / 1 000 requêtes et 5 $ de crédit mensuel annoncé](https://brave.com/search/api/), soit **0,15 $** de requêtes pour 30 passages avant extraction, IA et service central. | Même construction chez nous que pour une recherche brute. Utile comme base de coût et de couverture ; pas une alternative complète prête à brancher. Les [conditions du crédit gratuit](https://api-dashboard.search.brave.com/documentation/resources/help-feedback) peuvent demander des coordonnées de paiement. |
| Sources directes : API, RSS/Atom, calendriers, pages publiques | Observation ciblée d'une source identifiée, souvent sans prix d'appel ; parfois meilleure fraîcheur et preuve. | Variable selon source et fréquence ; aucun coût universel honnête. | Trouver et qualifier la source, maintenir les adaptateurs et surveiller les pannes. Voie d'optimisation quand une source identifiable couvre réellement la demande, sans imposer de catégories d'entrée. Voir [stratégie validée](../docs/06-sources-et-strategie-de-recherche.md). |

## Résultat de l'audit documentaire

**Exa Monitors change l'ordre des essais.** C'est le candidat le plus proche de Yutori sur le cœur récurrent (recherche planifiée + nouveautés + webhook) à un tarif affiché bien inférieur. Firecrawl est le second candidat géré, notamment si une demande peut être transformée en requêtes précises ou si une page connue doit être suivie. Yutori reste témoin de comparaison sur la capacité plus large d'un agent multi-étapes. Les solutions OpenAI/Brave et les sources directes ne sont pas des services de surveillance complets ; elles servent de secours, de référence ou d'optimisation après mesure.

Le prix ne prouve pas que ces options trouvent les mêmes faits. Exa et Firecrawl partent d'une recherche qui doit faire remonter le bon résultat ; une source absente de l'index, un événement seulement visible sur une page existante ou une requête mal formulée peuvent faire échouer la détection. Firecrawl distingue explicitement le `goal` qui juge et les `queries` qui retrouvent ; un objectif parfait ne compense pas une recherche trop étroite. [Documentation Firecrawl](https://docs.firecrawl.dev/features/monitoring-web-scale). Exa documente un historique de passages et des références, mais leur taux de couverture réel sur nos demandes reste inconnu. [Documentation Exa](https://exa.ai/docs/monitors/quickstart).

Les crédits gratuits servent à commencer une vérification, **pas à promettre une offre gratuite illimitée** : Exa annonce [10 $ mensuels sans moyen de paiement](https://exa.ai/pricing), Firecrawl [1 000 crédits mensuels](https://www.firecrawl.dev/pricing), Brave [5 $ mensuels de crédit](https://brave.com/search/api/) avec inscription et moyen de paiement à examiner. Toute utilisation doit respecter les conditions d'accès et les données envoyées aux fournisseurs. Aucun compte ni clé n'a été créé dans cette revue.

## Prochain essai proposé, sans exemples de sujets arbitraires

1. Constituer avec Fabien un **même corpus transversal de demandes réelles ou représentatives**, en variant forme de l'alerte, langue, pays, lieu, source connue ou inconnue, exigence de délai et faisabilité. Pour chaque demande : formulation exacte, fait attendu, faux positifs à éviter et preuves vérifiables. Les sujets sont choisis pour tester la capacité, pas pour fixer un catalogue commercial.
2. Fixer les critères **avant** les appels : pertinence des notifications, événements de référence manqués dans la portée annoncée, délai, reprise d'erreur et coût complet. Garder des cas contrôlés distincts des cas réels.
3. Essayer d'abord **Exa Monitors**, puis **Firecrawl Monitor**, sur les mêmes demandes et à la même cadence. Inclure les coûts de formulation des requêtes et de validation des résultats. Comparer Yutori sur les cas où les deux premiers échouent et où son coût pourrait acheter une meilleure couverture. Une source directe peut être testée pour un cas seulement si elle est réellement pertinente.
4. Décider après mesure : moteur principal, éventuel secours, types de demandes à limiter ou refuser, coût par surveillance active et quotas de la V1. Aucun moteur n'est adopté par cette note.

Le plafond préparatoire du [document 07](../docs/07-couts-et-modele-economique.md) est de **50 € au total pour une campagne facturable de 30 jours**. Il ne constitue pas une autorisation d'achat ; une fiche d'essai chiffrée sera établie avant tout appel payant. Le volet documentaire et la préparation du corpus n'engagent aucune dépense.
