# Coûts et modèle économique

Validé par Fabien le 2 octobre 2026, avec publication de la V1 sur le Play Store et premiers essais prévus surtout par Fabien, sa femme et éventuellement un ami. Les montants d'exploitation, quotas et cadences restent à mesurer avant publication. **Ce document n'autorise aucune dépense, ne choisit aucun fournisseur et ne fixe aucun tarif public.** Les décisions sont consignées dans la [décision 0007](decisions/0007-couts-et-modele-economique.md). Il complète la [stratégie de recherche validée](06-sources-et-strategie-de-recherche.md) et les [règles d'alerte](05-regles-des-alertes.md).

## Règle en vigueur depuis le 15 novembre 2026

Fabien valide et fait appliquer un essai gratuit : **un suivi actif par compte, jusqu’à trois sources, contrôle quotidien pendant sept jours consécutifs à partir de la première activation confirmée**. Les anciens plans ne changent pas sans nouvelle confirmation. Les paragraphes datés suivants sont historiques.

La période appartient au compte, pas au suivi : pause, reprise, remplacement ou suppression du suivi ne la renouvellent pas. Les modifications partagent la même enveloppe finie. Plafond de 30 crédits Web et 180 000 micro-USD d’IA après confirmation ; la compréhension préalable utilise le budget global. Une panne ou une réserve insuffisante peut arrêter le suivi avant sept jours : aucune garantie de résultat ni de couverture exhaustive. Les résultats acquis restent accessibles.

Le serveur réserve cette enveloppe au moment de l’admission, avant de laisser un autre compte ou une analyse anonyme la consommer. Le pilote dispose actuellement de 0,30 USD de plafond IA général et 171 crédits Web cumulés, compteurs existants conservés. Cela finance prudemment **un nouvel essai complet à la fois au départ**, pas dix essais simultanés. La borne cumulée conservatrice reste 0,99587625 € sous les 2 € autorisés ; ce n’est pas une facture rapprochée. Aucun achat ni renouvellement automatique. [Preuve du déploiement](../validation/daily-trial/deployment.json).

**Paiement : la V1 reste gratuite.** À expiration, arrêt du suivi et consultation des résultats, sans prélèvement et sans abonnement automatique. Aucun prix, offre payante, SDK de paiement ou contrat de prestataire n’est configuré. Pour une future offre Android payante, le chemin recommandé est [Google Play Billing](https://developer.android.com/google/play/billing), avec vérification des achats côté serveur. Le tarif, les droits achetés, la restauration et les annulations devront être définis avant ce lot. Aucun intermédiaire supplémentaire n’est nécessaire à ce stade. Cette recommandation ne vaut pas lancement d’un chantier de paiement ni budget public approuvé.

## Révision économique du 20 octobre 2026

**Décision de conduite : priorité à l'application Android ; pas de chantier de monétisation maintenant.** Le cadre reste une V1 gratuite et bornée. Le paiement futur n'est ni acquis ni nécessaire pour commencer Android. Aucun achat, recharge ou appel fournisseur effectué pour cette révision. Le plafond autorisé des essais demeure **2 € cumulés** ; les 50 € plus bas sont un historique de préparation, pas le budget en vigueur.

### Hypothèses du calcul

Tarifs publics revérifiés le 20 octobre : Firecrawl affiche 1 000 crédits gratuits par mois et Hobby à 19 USD par mois en paiement mensuel pour 5 000 crédits. L'affichage 16 USD suppose un paiement annuel. [Tarifs officiels](https://www.firecrawl.dev/pricing).

Le scénario étudié utilise une seule requête par contrôle, dix résultats au maximum, sans autre cible. Recherche : 2 crédits pour dix résultats ; jugement : 1 crédit par résultat jugé. Les résultats déjà vus peuvent ne pas être rejugés. Plusieurs requêtes augmentent la consommation avant déduplication. [Documentation de recherche récurrente](https://docs.firecrawl.dev/features/monitoring-web-scale).

**Calcul de sensibilité, pas mesure de production :** pour 30 contrôles, une recherche facturée 2 crédits à chaque passage donne 60 crédits ; avec dix nouveaux candidats jugés à chaque passage, 360 crédits. Zéro notification ne signifie pas zéro candidat jugé. Ces bornes décrivent cette configuration : moins de résultats peut coûter moins ; reprises, changement de règles, lectures complémentaires ou plusieurs requêtes coûtent plus.

### Consommation selon les suivis actifs

| Suivis actifs, contrôlés une fois par jour | Recherche seule dans le scénario (crédits / 30 jours) | Recherche + dix nouveaux jugements par passage | Réservation indicative : scénario coûteux + 20 % |
| --- | ---: | ---: | ---: |
| 1 | 60 | 360 | 432 |
| 2 | 120 | 720 | 864 |
| 5 | 300 | 1 800 | 2 160 |
| 10 | 600 | 3 600 | 4 320 |
| 50 | 3 000 | 18 000 | 21 600 |

Calcul reproductible : N × 30 × (2 + J), J = 0 ou 10 ; réserve × 1,20. La réserve de 20 % est un choix de planification proposé, **pas une garantie de couverture des incidents**. Les créations et analyses abandonnées doivent avoir leur propre quota ; elles ne peuvent consommer sans limite cette réserve.

Interprétation : deux suivis quotidiens coûtant autant que le scénario haut réserveraient 864 des 1 000 crédits gratuits. Dix suivis réserveraient 4 320 crédits : le seul poste Firecrawl pourrait entrer dans le forfait mensuel de 19 USD, sous ces hypothèses. Ce n'est pas 19 USD pour toute l'application. À cinquante suivis, ce forfait ne suffit pas ; aucun dépassement automatique ou achat n'est proposé ici. L'accès effectif à Monitor, les limites du compte, le solde et le renouvellement sont à contrôler avant lancement.

### Budget complet : ne pas confondre plafond et estimation

| Poste | Connu aujourd'hui | Traitement avant publication |
| --- | --- | --- |
| Recherche récurrente | Sensibilité chiffrée ci-dessus ; Firecrawl candidat, pas choix définitif de production | Compteurs réels et arrêt distant testés ; aucune recharge automatique. |
| Compréhension initiale et corrections | Ancienne campagne IA de 83 appels estimée à 0,0476092 USD, avec échecs de qualité | Ne pas extrapoler ce prix au produit ; mesurer le parcours retenu et limiter les propositions, y compris abandonnées. |
| Service, identité, base et journaux | Stack et facture non arrêtées | Vérifier quotas gratuits, coûts fixes, éventuelle carte et moyens d'arrêt au choix technique. |
| Livraison des notifications | Transport et exploitation à confirmer avec la stack | Inclure les fonctions serveur et le stockage ; pas seulement le push. |
| Publication, taxes et change | Distincts des crédits et des essais | Vérifier au déploiement et compte Play ; pas de conversion fictive USD/EUR. |

**Recommandation de plafond pour le petit pilote : 10 € par mois maximum pour l'exploitation totale**, hors éventuels frais fixes Play et temps de développement. Il s'agit d'une enveloppe proposée à Fabien, **non autorisée et non démontrée suffisante**. Avec Firecrawl gratuit et deux suivis au scénario haut, elle doit couvrir les autres postes et une marge. Si la stack réelle ne tient pas dans cette enveloppe, présenter le coût exact avant engagement. Aucun abonnement à 19 USD ne peut être considéré inclus automatiquement dans ce plafond.

Tant que cette enveloppe et la facture prévisionnelle complète ne sont pas validées, pas de lancement de service payant. Le travail Android local et simulé continue sans cet arbitrage. Ne pas présenter cette configuration comme économiquement viable pour un grand public nombreux : c'est un petit pilote, accessible publiquement mais à capacité limitée.

### Contrôles à implémenter et vérifier

- Une réservation atomique empêche deux activations de prendre la dernière capacité. Le nombre d'installations ne sert jamais d'estimation du nombre de suivis.
- Les analyses avant connexion ont une limite serveur également ; le quota par compte seul ne protège pas cet accès.
- Avant un appel, réserver son coût maximal ; limiter concurrence et réessais. Les dépenses déjà engagées comptent.
- Pour un monitor planifié chez le fournisseur, le compteur local ne suffit pas : surveiller la consommation, garder une réserve pour les passages en vol, demander la pause distante et vérifier son effet. Si cette borne ne peut être garantie, ne pas activer le plan.
- Geler d'abord les nouvelles activations ; afficher toute suspension des suivis existants. Jamais « rien de nouveau » lorsqu'un contrôle n'a pas eu lieu.
- Rapprocher les compteurs et la facture ; ne pas augmenter la capacité sur une seule lecture silencieuse moins chère.

Acceptation : simuler deux activations concurrentes, un échec de pause distante, un coût supérieur à l'estimation, des prévisualisations abandonnées, et un dépassement avec exécution déjà en vol. Aucun test payant supplémentaire n'est autorisé par cette liste.

### Ce qui reste à arbitrer

Avant publication : plafond d'exploitation de 10 € proposé, cadence quotidienne et quotas proposés dans le document 03, capacité réelle une fois la stack chiffrée. La volonté de payer sera observée après usage utile ; ni essai payant ni abonnement utilisateur ne sont des préalables au développement. La baisse de coût ne suffit pas à qualifier une méthode qui manque les informations promises.

## Objectif

Tenir chaque promesse de surveillance à un coût soutenable et connu avant activation. Une vérification sans nouveauté consomme tout de même des ressources : le coût pertinent est celui d'un mois de suivi à la cadence et dans la portée annoncées, pas seulement celui d'une notification. La priorité du premier pilote est de mesurer qualité, coût et usage ; aucune offre gratuite illimitée ou rentabilité future n'est présumée.

## Trois enveloppes distinctes

| Enveloppe | Contenu | Décision nécessaire |
| --- | --- | --- |
| Construction et publication | Temps de conception et développement, appareils, outils, assets et éventuels frais de publication. | Inventaire des frais fixes dans le document 09 ; temps de Fabien suivi séparément des factures. |
| Essais techniques | Corpus, prototypes limités, appels à des services pour comparer méthodes, sources et cadences. | Plafond total, durée et services autorisés avant tout appel payant. |
| Exploitation après publication | Vérifications répétées pour de vraies personnes, hébergement, base, analyse, notifications, supervision et support. | Plafond mensuel, capacité maximale et procédure d'arrêt avant mise en ligne. |

Une API ou un flux sans prix d'accès peut avoir des quotas, des droits restrictifs et un coût de maintenance. Un service payant peut être préférable pour un besoin précis si sa qualité et son coût total battent une extraction fragile. La méthode est choisie par demande selon le document 06.

## Modèle par surveillance active

Pour chaque surveillance et chaque période de 30 jours, mesurer séparément :

| Poste | Éléments à compter |
| --- | --- |
| Création | Compréhension de la phrase, précisions, découverte, qualification, état initial, prévisualisation et reprises. Les propositions abandonnées comptent. |
| Contrôles | Nombre de vérifications réellement lancées, y compris celles sans nouveauté, multiplié par le coût complet de la méthode. |
| Qualification | Extraction, comparaison, déduplication, appels IA éventuels et vérification des preuves. |
| Incidents | Échecs facturés, réessais, changement de format, recherche de source de secours et intervention humaine. |
| Infrastructure | Part de planification, réseau, stockage, base, sauvegardes, secrets, journaux et supervision. |
| Livraison | Préparation du résultat, push, diagnostic d'échec et conservation de l'état. |

Le coût total d'une surveillance sur la période est la somme de ces six postes. Séparer le coût initial, éventuellement amorti, du coût mensuel récurrent ; rapporter les coûts fixes au **nombre réel** de surveillances actives. Les demandes ponctuelles qui se terminent après un résultat et les suivis généraux de longue durée doivent être analysés séparément.

Si plusieurs personnes suivent une même source publique, une collecte commune peut réduire les coûts lorsque droits, fraîcheur et confidentialité le permettent. Garder séparés le coût de collecte partagé et celui de la comparaison propre à chaque compte. Ne pas supposer qu'un prestataire facturé par tâche permet cette mutualisation.

## Ordres de grandeur vérifiés les 2 et 3 octobre 2026

Ces prix en dollars américains sont des **exemples de sensibilité**, hors conversion, taxes, serveur et évolution des tarifs. Ils ne démontrent pas que les méthodes ont la même couverture ni la même qualité. Vérifier à nouveau prix et conditions avant un essai.

| Méthode illustrative | Prix affiché | Hypothèse de 30 contrôles quotidiens |
| --- | --- | --- |
| Yutori Scouting API | 0,35 $ par exécution de Scout. | Une exécution par jour pendant 30 jours coûte **10,50 $ pour une seule surveillance**, avant nos autres postes. [Tarification Yutori](https://docs.yutori.com/pricing). |
| Exa Monitors | 15 $ pour 1 000 requêtes de surveillance, au prix de base affiché. | Sous l'hypothèse d'une requête facturable par passage, 30 contrôles coûtent **0,45 $**, avant options, analyse complémentaire et notre service. [Tarification Exa](https://exa.ai/pricing), [fonctionnement](https://exa.ai/docs/monitors/quickstart). |
| Firecrawl Monitor, recherche Web | 2 crédits par 10 résultats recherchés et par contrôle, plus 1 crédit par résultat jugé ; 1 000 crédits gratuits par mois annoncés. | Une requête ramenant jusqu'à 10 résultats pendant 30 contrôles représente **60 crédits de recherche dans ce scénario**, avant jugements et autres cibles. Le coût en dollars dépend du forfait et du volume effectif. [Fonctionnement](https://docs.firecrawl.dev/features/monitoring-web-scale), [tarifs](https://www.firecrawl.dev/pricing). |
| OpenAI Web search | 10 $ pour 1 000 appels à l'outil, **plus** les tokens de contenu recherché et les tokens du modèle aux tarifs applicables. | Si une vérification ne fait qu'un appel, 30 appels produisent **0,30 $ de frais d'outil**, plus tous les autres coûts. Le nombre d'appels réellement nécessaire et la qualité restent à mesurer. [Tarification OpenAI](https://developers.openai.com/api/docs/pricing). |
| Firebase Cloud Messaging | FCM est indiqué sans coût sur sa grille de prix. | L'envoi push peut ne pas ajouter de prix de transport, mais le service de surveillance, le serveur et le stockage restent à financer. Ce n'est pas un choix d'architecture. [Tarification Firebase](https://firebase.google.com/pricing). |

Au tarif Yutori ci-dessus, **si chaque surveillance a son Scout exécuté chaque jour**, 10 surveillances coûtent 105 $ sur 30 jours, 100 coûtent 1 050 $, 1 000 coûtent 10 500 $, avant nos autres dépenses. Ces volumes sont des scénarios, pas des prévisions. Exa affiche un coût de base beaucoup plus faible pour une recherche récurrente, mais les deux services n'offrent pas nécessairement la même profondeur ni la même couverture. Extrapoler les 0,30 $ d'OpenAI serait trompeur si un contrôle nécessite plusieurs recherches, des tokens importants, des reprises ou un traitement humain. Les méthodes doivent être évaluées sur les mêmes demandes et résultats attendus.

## Mesurer avant les quotas et les cadences

Chaque essai relève, par demande et par exécution : méthode et version, source, durée, unités facturées, devise, coût estimé et facturé, statut, candidats, résultats retenus, erreurs et réessais. Les contenus privés ne sont pas nécessaires dans les journaux financiers. Rapprocher l'usage interne des tableaux de bord ou factures fournisseurs ; une estimation applicative ne constitue pas un plafond de facture.

Le corpus du document 06 couvre au minimum une demande ponctuelle, un suivi général, une source structurée ou un flux, un cas demandant une recherche large, une demande irréalisable et une source défaillante. Relever pour chacun : coût par demande soumise, par activation, par surveillance active sur 30 jours et par **résultat pertinent** ; coût des contrôles sans nouveauté ; faux positifs, événements de référence manqués et délai. Un coût faible qui manque les faits promis n'est pas un succès. Les seuils chiffrés de qualité, délai et coût seront inscrits au document 09 avant la comparaison des méthodes, sur le corpus identifié ; les cadences retenues pour l'exploitation seront ajustées seulement après mesures et avant publication.

## Admission et protection du budget

Avant activation, le service estime le coût mensuel du plan à partir de la méthode, de sa cadence, du nombre de contrôles et d'une réserve pour les reprises. Il le compare à la capacité disponible. Si cette capacité est insuffisante, il propose une portée ou cadence réduite à confirmer, met en attente seulement si cette fonction existe réellement, ou refuse. Aucun suivi fictivement actif.

Le pilote requiert des limites **côté service** : propositions coûteuses par compte et période, nombre de surveillances actives, cadence maximale selon la méthode, plafond global et par fournisseur, nombre de réessais et concurrence. Leurs valeurs doivent provenir des mesures et être affichées avant activation. Les refus de quota surviennent avant un nouvel appel coûteux et expliquent la limite.

Suivre séparément prévision, consommation mesurée et facture. Alerter l'exploitant avant le plafond ; geler d'abord les nouvelles activations ou les opérations non indispensables. Si les vérifications existantes doivent cesser, l'état devient visible comme suspension ou dégradation, avec dernière réussite et effet sur les notifications, conformément au document 05. Le service ne prétend plus être à jour. Après reprise, il réévalue l'état initial et signale la période non couverte sans inventer les événements manqués.

Un compteur interne ne garantit pas un plafond strict : certaines tâches sont déjà lancées et la facturation peut arriver en retard. Prévoir une réserve, limiter la concurrence, utiliser les limites natives du fournisseur si disponibles et tester l'arrêt sur un dépassement simulé. Si le risque de dépassement ne peut être borné raisonnablement, réduire la capacité du pilote ou écarter la méthode.

## Offre initiale sur le Play Store et modèle futur

La V1 a vocation à être **publiée sur le Play Store et accessible sans invitation**, gratuitement, avec nombre de surveillances et cadences limités et visibles. Fabien prévoit de la tester lui-même avec sa femme et éventuellement un ami ; ce petit groupe initial est une prévision, **pas une restriction d'accès**. La publication peut attirer davantage de personnes : le service doit donc limiter côté serveur les activations et les dépenses dès le premier jour. Si sa capacité est atteinte, il explique pourquoi une nouvelle surveillance ne peut pas être activée, sans prétendre que la demande est suivie. Aucun moyen de paiement utilisateur n'est requis pour cette V1. Les étapes exactes de publication et de test imposées par Google seront vérifiées au document 09.

Après mesures, les options sont une offre gratuite bornée si son coût est soutenable, une option payante si la valeur mesurée couvre l'ensemble des coûts, ou une spécialisation sur les besoins viables. Publicité et affiliation restent hors MVP. Un prix éventuel intégrera aussi hébergement, paiement, taxes, support, acquisition, incidents et marge de sécurité, puis les règles de paiement Play Store seront vérifiées avant lancement payant.

## Enveloppe proposée pour les premiers essais

**Phase 0 retenue : 0 € de services payants.** Inventorier les sources accessibles et tester sur données contrôlées ou offres réellement gratuites, dans le respect de leurs conditions. Construire le corpus et une référence de qualité avant les essais tarifés.

**Phase 1 retenue comme plafond de préparation : 50 € au total sur 30 jours d'essais techniques facturables**, tous fournisseurs externes variables confondus, hors frais fixes de publication et temps de développement. Ce plafond validé par Fabien concerne **uniquement cette première campagne d'essais techniques payants** : ce n'est ni le budget total du projet, ni le coût mensuel du service publié, ni une autorisation de lancer des appels payants. Il peut être révisé avant un essai si une fiche de coût montre qu'il ne permettrait pas une comparaison utile. Convertir les prix en dollars avec taxes et marge de change avant de réserver les exécutions. Chaque essai payant nécessite encore le choix explicite de son fournisseur et de sa fiche d'essai.

Avant la phase 1, une fiche d'essai indique fournisseur et tarifs revérifiés, corpus, maximum d'appels ou exécutions, coût maximal calculé dans la devise du fournisseur, marge pour erreurs, critère d'arrêt et rapprochement avec la facture. Le budget mensuel du service publié sera fixé séparément à partir des mesures, jamais déduit automatiquement des 50 €.

## Exigences proposées

| Référence | Exigence |
| --- | --- |
| COST-R01 | Mesurer coût complet de création et fonctionnement, y compris vérifications sans nouveauté et incidents. |
| COST-R02 | Instrumenter les postes séparément et rapprocher les consommations des factures. |
| COST-R03 | N'activer que les plans dont méthode et cadence entrent dans la capacité réservée. |
| COST-R04 | Appliquer côté service des quotas et plafonds affichés avant activation. |
| COST-R05 | Rendre visible toute interruption due au budget avec dernière réussite et effet sur la couverture. |
| COST-R06 | Comparer ou remplacer un fournisseur sans modifier silencieusement la promesse. |
| COST-R07 | Séparer les enveloppes d'essai et d'exploitation ; tester la procédure d'arrêt avant publication. |
| COST-R08 | N'engager aucune dépense ni promesse gratuite illimitée par seule validation documentaire. |
| COST-R09 | Rendre la V1 gratuite et accessible sur le Play Store sans invitation, dans les limites de capacité annoncées et appliquées côté service. |

## Scénarios d'acceptation proposés

| Référence | Situation | Résultat attendu |
| --- | --- | --- |
| COST-A01 | Un flux ne publie rien pendant 30 contrôles. | Les 30 contrôles, leurs éventuels appels IA et l'infrastructure figurent au coût. |
| COST-A02 | Une personne prévisualise trois demandes puis abandonne. | Le coût de ces analyses n'est pas attribué uniquement aux activations réussies. |
| COST-A03 | Deux personnes suivent la même source publique. | Collecte partageable si autorisée, mais comparaisons et données personnelles isolées. |
| COST-A04 | Une méthode excède la capacité restante. | Activation refusée ou nouvelle proposition à confirmer ; aucun suivi fictivement actif. |
| COST-A05 | Un fournisseur échoue puis est rappelé. | Appels facturables et réessais comptés ; pas de boucle de dépense. |
| COST-A06 | Le plafond impose l'arrêt des contrôles. | État et dernière réussite visibles ; jamais « rien de nouveau » par défaut. |
| COST-A07 | Le compteur interne diffère de la facture. | Écart détecté et expliqué avant élargissement du pilote. |
| COST-A08 | Deux méthodes sont comparées sur un même corpus. | Coût complet, manqués, faux positifs et délai déterminent le choix, pas le seul prix de l'appel. |
| COST-A09 | Un compte atteint sa limite de propositions. | Limite expliquée avant tout autre traitement coûteux. |
| COST-A10 | Une personne qui ne connaît pas Fabien installe l'application publiée. | Elle peut parcourir et demander une activation dans les mêmes conditions que les premiers testeurs, sous réserve des limites annoncées ; aucun code d'invitation n'est exigé. |
| COST-A11 | Plus de personnes que prévu installent l'application. | Les nouvelles activations restent bornées par la capacité du service ; les personnes concernées voient une explication honnête, et le plafond de dépenses n'est pas fondé sur l'hypothèse de trois utilisateurs. |

## Arbitrages validés par Fabien

| Référence | Décision | Conséquence |
| --- | --- | --- |
| COST-D01 | Phase 0 sans service payant, puis plafond **total de 50 € pour 30 jours** pour le premier banc d'essai facturable. | Cadre des comparaisons ; chaque essai payant et son fournisseur seront définis avant dépense. |
| COST-D02 | V1 gratuite, publiée sur le Play Store et accessible sans invitation ; éventuel modèle commercial décidé sur les mesures. | Quelques proches testeront probablement en premier, sans être les seuls utilisateurs autorisés. |
| COST-D03 | Fixer quotas, cadence et plafond mensuel du service publié après mesure, avant mise en ligne. | Capacité et promesse de chaque surveillance ; aucune valeur arbitraire codée. |
| COST-D04 | Bloquer d'abord les nouvelles activations ; si l'existant doit s'arrêter, afficher la suspension et requalifier avant reprise. | Comportement visible et testable, sans fausse impression de fraîcheur. |

Les montants d'exploitation, quotas, cadence et choix de fournisseurs restent ouverts. La validation de ce document n'autorise ni achat, ni appel facturable, ni offre payante.


## Mise à jour S3-01 — 20 octobre 2026

La [spécification du service](11-service-et-contrat-s3.md#budget-de-préparation-et-ouverture-du-pilote) chiffre les postes connus et distingue ceux qui restent inconnus. Supabase gratuit et FCM permettent de préparer l’intégration sans abonnement. Domaine d’expédition e-mail, sauvegarde indépendante, analyse qualifiée et consommation réelle restent à préciser avant de conclure sur une facture complète. [Scénarios reproductibles](../validation/s3-01/cost-scenarios.json). Aucune dépense fournisseur nouvelle, aucune réactivation de monitor ni hausse du plafond historique.


## Prévision de l’essai S3 à la clôture du 22 octobre

L’essai utilise les projets Supabase Free et Firebase Spark existants, la boîte OVH existante et un stockage chiffré local existant : aucun engagement d’achat supplémentaire. Analyse/recherche/lecture sont comptées même sans alerte. Dans le moteur général, admission : au plus deux réservations modèle de 0,02 USD chacune et une collecte de 3 crédits gratuits ; état initial : au plus 0,02 USD + 3 crédits ; contrôle : au plus 0,02 USD + 3 crédits (zéro modèle si texte inchangé). Ce sont des bornes de réservation prudentes, pas les prix mesurés par alerte. Arrêt avant appel lorsque la réservation manque ; aucune recharge automatique.

Le service général garde au maximum 0,10 USD de modèle, dont 0,014462 USD déjà imputés, à l’intérieur de l’enveloppe commune de 0,40 USD. Quota gratuit : 51 unités au total, 39 imputées, 12 restantes à l’ouverture. Cela permet au plus quatre collectes supplémentaires, par exemple une admission, un état initial et deux contrôles ; ce n’est pas sept jours garantis ni un forfait mensuel. Le pilote textuel garde son propre compteur existant, sans remise à zéro, dans le même solde gratuit fournisseur.

Usage modèle connu cumulé : 0,182842 USD estimés ; borne conservatrice incluant toutes les réserves encore autorisées, ancien poste d’autres coûts, change plafond 1,5 et taxes 1,25 : 0,99587625 EUR / plafond utilisateur inchangé 2 EUR. Solde fournisseur rapproché : 915 crédits gratuits. [Registre final](../validation/s3/general-budget-final.json). Pas de facture rapprochée prétendue.

Une exploitation publique demande un scénario de volume, une identité d’envoi dédiée si nécessaire, une destination de sauvegarde indépendante et une nouvelle prévision incluant hébergement, e-mail, domaine, sauvegarde, sources/analyse et taxes. Aucun budget mensuel n’est autorisé ; cette décision appartient à la préparation S4, sans achat implicite pour terminer S3.

## Extension de l’essai autorisée le 24 octobre 2026

Fabien a demandé de relever modestement les limites pour poursuivre ses tests. Le service autorise maintenant **trois suivis actifs par compte et trois au total**, tous comptes réunis. L’enveloppe gratuite Firecrawl du moteur général passe de 51 à **100 crédits cumulés**, dont 45 déjà imputés et 55 encore disponibles au moment de l’extension. Le solde fournisseur contrôlé était de 904 crédits : aucun achat ni abonnement. Les crédits du pilote historique sur URL restent inchangés.

Le plafond modèle du moteur général reste à **0,10 USD**, dans l’enveloppe commune déjà autorisée. Aucun compteur utilisé ou réservé n’est remis à zéro. Le plafond utilisateur reste **2 € cumulés pour le service**, pas 2 € par compte. La borne conservatrice historique reste 0,99587625 € ; ce n’est pas une facture rapprochée. Cette extension ne garantit pas un nombre de jours de surveillance. Les réservations avant chaque appel peuvent refuser un nouveau traitement avant épuisement exact du solde. [Preuve des limites avant/après](../validation/test-capacity/deployment.json).
