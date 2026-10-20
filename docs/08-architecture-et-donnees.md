# Architecture et données

Validé par Fabien le 3 octobre 2026, avec MVVM et Clean Architecture comme choix explicites pour Android. Ce document traduit les [parcours](04-parcours-et-ecrans.md), [règles d'alerte](05-regles-des-alertes.md), [méthodes de recherche](06-sources-et-strategie-de-recherche.md) et [limites de coût](07-couts-et-modele-economique.md) validés en responsabilités techniques et en données à conserver. Les décisions figurent dans la [décision 0008](decisions/0008-architecture-et-donnees.md) ; les paramètres encore ouverts sont signalés à la fin. Le socle cloud est désormais précisé pour l’implémentation locale dans l’[ADR 0011](decisions/0011-service-reel-s3.md) ; aucun prestataire IA de production n’est qualifié.

## Principe directeur

Le **service** est la référence pour les surveillances confirmées, leurs versions, leur état, les résultats et les décisions d'envoi. Il continue à travailler lorsque l'application est fermée ou le téléphone éteint. **Android** recueille la demande, présente une proposition compréhensible, transmet les actions et affiche l'état confirmé par le service. Une notification est un moyen de signaler un résultat déjà enregistré ; elle n'est pas le seul endroit où ce résultat existe.

Une vérification périodique lancée par le téléphone ne peut pas porter la promesse du produit : elle dépend de l'appareil, de sa connexion et des contraintes d'exécution Android. Même WorkManager ne garantit pas une heure exacte d'exécution périodique. Un serveur ou un service hébergé de planification est donc nécessaire. [Documentation Android sur les tâches périodiques](https://developer.android.com/develop/background-work/background-tasks/data-transfer-options). Le composant de recherche peut appeler une API, lire un flux ou utiliser un prestataire agentique selon le plan validé ; il n'est pas confondu avec l'application Android. La documentation Android recommande Compose, une couche de données accessible par repositories, des ViewModels et un flux de données unidirectionnel ; elle présente la couche de cas d'usage comme utile lorsque la logique le justifie, pas comme un passage obligé pour chaque écran. [Recommandations officielles Android](https://developer.android.com/topic/architecture/recommendations).

## Frontière des responsabilités retenue

| Élément | Responsabilité dans la V1 | Ne fait pas |
| --- | --- | --- |
| Application Android | Saisie et clarification, affichage de la proposition, identification à l'activation, liste et détail, actions, réglages utiles, permission et ouverture des notifications. | Choisir seule les sources, conserver une clé fournisseur, planifier les contrôles récurrents ou déclarer une action réussie avant réponse du service. |
| API applicative | Authentifier les actions durables, vérifier le propriétaire des ressources, valider les commandes, appliquer quotas et renvoyer des représentations stables à l'application. | Exposer directement des tables ou laisser le client écrire l'état interne d'une surveillance. |
| Orchestrateur de surveillance | Préparer le plan, établir l'état initial, planifier et relancer les contrôles, appliquer les règles de coût et de santé. | Affirmer qu'une absence de réponse de source vaut absence d'événement. |
| Adaptateurs de sources et de services | Interroger API, RSS/Atom, ICS, pages ou prestataires qualifiés ; normaliser les observations avec provenance. | Transformer seuls une observation en notification certaine. |
| Évaluation des résultats | Vérifier critères, nouveauté, preuve, dates, doublons, contradictions et corrections ; produire un résultat explicable. | Déduire la vérité d'une simple phrase générée par un modèle. |
| Stockage et diffusion | Conserver état, résultats et journal minimal ; décider une émission idempotente et solliciter le service push. | Garantir qu'un téléphone affichera instantanément une notification. |

Ces responsabilités vivent dans **un seul service logique déployable**, avec frontières internes bien délimitées. Ses règles de surveillance ne dépendent pas directement d'un fournisseur : des ports décrivent les besoins de lecture de source, d'horloge, de stockage et d'envoi ; des adaptateurs les réalisent pour les services retenus. C'est l'application pragmatique des principes hexagonaux là où les sources externes changent, au sein de l'architecture propre du produit. Microservices, moteur de recherche propriétaire, bus d'événements distribué et agent Web universel ne sont pas nécessaires pour la V1. Employer des services hébergés pour identification, base, planification ou push est envisageable après comparaison coût, fiabilité et possibilité de récupérer les données.

## Parcours de bout en bout

1. Android envoie la demande libre à l'analyse de prévisualisation. Le service limite cet accès public et crée une proposition temporaire : critères candidats, éventuelles questions, méthode de recherche et coût admissible.
2. Après les réponses utiles, le service cherche une méthode récurrente, qualifie les sources, construit l'état initial et renvoie une proposition versionnée. Les critères, limites, sources et cadence sont montrés avant accord.
3. La personne s'identifie seulement lorsqu'elle active durablement. La proposition est rattachée à son compte après vérification qu'elle est toujours valide ; la confirmation crée une surveillance et réserve sa capacité. Une proposition devenue obsolète ou trop chère est réévaluée avant activation.
4. L'ordonnanceur prend les contrôles dus. Chaque exécution reprend la version du plan, note début, fin, succès ou erreur, et enregistre les observations avec source et dates distinctes.
5. L'évaluation produit éventuellement un résultat sourcé, distinct d'une simple observation. Avant émission, elle revérifie état et version de la surveillance. Le résultat est conservé et une intention d'envoi unique est inscrite de manière cohérente avec lui.
6. Un composant d'envoi demande la notification au fournisseur push. Android ouvre l'identifiant du résultat, le recharge depuis le service et gère session expirée, résultat supprimé ou accès interdit.

Les contrôles peuvent être exécutés plus d'une fois après panne ou redémarrage ; la conception ne suppose pas une livraison technique « exactement une fois ». Elle utilise des identifiants stables, des contraintes d'unicité et des commandes idempotentes pour éviter de produire deux résultats ou deux intentions d'envoi pour le même fait et la même version. Une demande de pause ou suppression déjà confirmée empêche toute nouvelle émission même si un contrôle avait commencé. Pour une demande ponctuelle, la décision d'envoi du premier résultat est prise avant, ou dans la même transaction que, la transition vers « terminée », conformément au document 05.

## Architecture Android retenue : MVVM et Clean Architecture

**Kotlin + Jetpack Compose et MVVM** : les composables affichent un état d'écran, envoient les actions au ViewModel et observent son état via Flow/StateFlow. Le ViewModel orchestre les actions et traduit les réponses en états chargement, contenu et erreur ; il ne parle ni HTTP ni base directement. Il ne décide pas non plus qu'une source est fiable ou qu'une alerte doit être émise : ces décisions appartiennent au service. L'injection de dépendances doit permettre de remplacer les accès réseau dans les tests ; Hilt est retenu pour le socle décrit dans la spécification du 20 octobre ci-dessous ; sa version et celle de KSP seront éprouvées par compilation au sprint 1.

**Clean Architecture** est appliquée par une règle de dépendance vérifiable, même avec un seul module Gradle : la présentation dépend du domaine ; le domaine décrit les modèles utiles à l'application, les opérations métier et les interfaces dont il a besoin ; la couche data implémente ces interfaces avec API, authentification et stockage local. Le domaine n'importe ni classes Android, ni client HTTP, ni base de données, ni SDK fournisseur. L'assemblage des implémentations se fait à la frontière de l'application. Les objets de réponse réseau sont convertis en modèles du domaine, puis en état d'écran. Un changement de fournisseur ou de format de réponse ne doit pas obliger à réécrire les composables ou les règles du domaine.

Le domaine contient des cas d'usage pour les parcours qui ont une véritable règle ou coordination : par exemple confirmer une proposition encore valable, appliquer une commande avec sa version attendue, ou interpréter l'état d'une surveillance pour l'affichage. Une simple lecture sans logique peut passer par le contrat du repository sans classe « UseCase » vide. C'est un choix de Clean Architecture proportionné : les dépendances et les responsabilités sont réelles, sans multiplier les couches pour reproduire chaque fonction à l'identique. La [documentation Android](https://developer.android.com/topic/architecture/domain-layer) précise d'ailleurs que sa notion facultative de couche domaine n'est pas strictement identique à toutes les variantes de Clean Architecture ; ici, la règle de dépendance et le domaine indépendant sont un choix explicite du projet.

| Zone de code dans le module Android | Dépend de | Responsabilité |
| --- | --- | --- |
| Présentation, par écran | Domaine | Compose, ViewModel, état d'écran, navigation et messages. |
| Domaine | Kotlin et ses propres contrats | Modèles indépendants, cas d'usage justifiés et interfaces des données nécessaires. |
| Data | Domaine et SDK techniques | Implémentations des repositories, API, stockage local, authentification et conversion des modèles. |
| Assemblage | Toutes les zones | Fournir les implémentations aux contrats sans faire remonter les détails techniques dans le domaine. |

**Un seul module Gradle Android est retenu pour commencer**, organisé par fonctionnalités et responsabilités, avec contrats et tests capables de vérifier ces frontières. Les imports du domaine vers la présentation ou les implémentations data doivent être interdits par revue et contrôle automatisé d'architecture avant publication. Une extraction en modules Gradle sera justifiée par une frontière stable, une réutilisation ou un temps de compilation devenu problématique ; elle n'est pas nécessaire pour prouver le principe de dépendance en entretien. Un catalogue de versions Gradle et des contrôles de qualité peuvent exister sans multiplication artificielle des modules.

Ce choix répond à l'objectif de remise à niveau Android. Dans les annonces Android consultées en octobre 2026, [Theodo Apps](https://www.welcometothejungle.com/en/companies/bam/jobs/developpeur-mobile-android-cdi-paris-theodo-apps_paris) et [Finom](https://app.welcometothejungle.com/jobs/LbI-7KU1) citent explicitement MVVM et Clean Architecture ; ces exemples ne sont pas un recensement du marché. Ils ne donnent aucune raison de remplacer MVVM par « hexagonal » : MVVM décrit la présentation, tandis que les ports et adaptateurs isolent les dépendances externes, surtout utiles dans le moteur côté service.

Le service reste la source de vérité. Pour la V1, Android peut conserver le brouillon en cours et des préférences locales, ainsi qu'un cache de lecture explicitement daté si utile. **La consultation hors ligne complète et la file de modifications hors ligne sont reportées** selon le document 03. Une commande pause, modification ou suppression n'est jamais montrée comme appliquée tant que le service ne l'a pas confirmée. Un cache local n'est pas présenté comme une donnée fraîche ; à la déconnexion, la dernière mise à jour reste visible. Une base Room et le modèle « offline-first » ne sont pas imposés sans ce besoin. DataStore est retenu pour les petites préférences et le brouillon durable dans la spécification du socle ci-dessous ; il ne stocke pas une copie de toute la base serveur.

La navigation depuis une notification porte des identifiants opaques de surveillance et de résultat, sans texte privé dans l'URL. L'application recharge la ressource après authentification. La permission de notification peut être refusée : le résultat reste dans la liste de la surveillance. Les jetons d'instance pour le push sont enregistrés côté service et supprimés lorsqu'ils deviennent invalides ou lors de la suppression du compte. FCM décrit un envoi par un environnement serveur de confiance à un jeton d'instance ; le transport et l'affichage dépendent ensuite de l'appareil. [Architecture serveur FCM](https://firebase.google.com/docs/cloud-messaging/server-environment).

## Opérations applicatives de référence

L'API transporte des objets métier, pas des noms de tables ou des réponses brutes de fournisseurs. Les opérations logiques suivantes sont nécessaires ; les URL et formats exacts seront versionnés lors de l'implémentation :

| Opération | Entrée | Sortie ou effet |
| --- | --- | --- |
| Analyser une demande | Texte, langue, réponses de clarification si présentes. | Proposition temporaire ou question, refus motivé ou erreur transitoire. |
| Lire et confirmer une proposition | Identifiant opaque, version, session si activation. | Critères et plan encore valables ; surveillance créée une seule fois ou nouvelle proposition demandée. |
| Lister et lire les surveillances | Compte authentifié, pagination. | États et dates du service ; détail de la version confirmée. |
| Lire les résultats | Surveillance ou résultat appartenant au compte, pagination. | Faits, explication, sources, dates de publication, observation et envoi. |
| Modifier, suspendre, reprendre, supprimer | Identifiant, version attendue, clé de répétition de commande. | Nouvel état confirmé ou conflit explicite ; une modification suit une nouvelle proposition. |
| Gérer les appareils et le compte | Session, jeton push, demande de suppression des données. | Enregistrement ou retrait des appareils, suppression du compte et des surveillances. |

Les erreurs doivent distinguer au minimum : précision nécessaire, non réalisable, couverture partielle à confirmer, limite d'usage, session absente ou expirée, ressource interdite, version périmée, panne temporaire et source dégradée. Les réponses contiennent un code stable pour l'application et un message localisable ou des paramètres contrôlés ; Android ne déduit pas le sens d'une erreur d'une chaîne fournie par un prestataire.

## Modèle de données logique

| Entité | Données indispensables et relation |
| --- | --- |
| Compte | Identifiant interne, identifiant du fournisseur d'authentification, langue utile au service et dates de création/suppression. Données d'identité limitées au besoin réel. |
| Appareil | Compte, identifiant d'installation, jeton push et date de dernière activité ; plusieurs appareils possibles. |
| Proposition temporaire | Demande initiale, précisions, critères candidats, plan suggéré, date d'expiration et jeton opaque de reprise. Une proposition non activée n'est pas une surveillance. |
| Surveillance | Compte propriétaire, titre, type ponctuel ou continu, état, version confirmée, cadence annoncée, dernière réussite, dernier échec, prochaine vérification si connue. |
| Version de plan | Critères confirmés, exclusions, portée, sources, méthode, état initial, date de confirmation et politique de comparaison. Immuable pour expliquer les anciens résultats. |
| Source | Producteur, accès, droits, portée, fraîcheur, état de santé et date de revue du registre du document 06. Une source partagée ne contient pas les critères privés d'un compte. |
| Contrôle | Surveillance et version, fenêtre ou échéance, tentative, statut, horodatages, coût et code d'erreur ; détails de diagnostic conservés brièvement. |
| Observation | Fait normalisé, source et lien, date de publication si connue, date de l'événement si différente, date d'observation et attributs nécessaires à la comparaison. |
| Résultat | Surveillance et version, identité du fait, décision et justification, sources, dates, éventuel résultat corrigé, date de création. Seul un résultat qualifié est montré comme alerte. |
| Intention d'envoi | Résultat, version, destinataires autorisés, identifiant unique, état d'envoi et essais. Elle permet une reprise sans répéter volontairement la même alerte. |
| Consommation | Compte ou plan selon le besoin, fournisseur, méthode, unités, coût estimé puis rapproché, période et limites appliquées. Pas de texte de demande dans la comptabilité. |

Les dates ont des sens distincts : publication par la source, événement annoncé, première observation, début et fin du contrôle, qualification et demande d'envoi. Ne jamais remplacer une date inconnue par « aujourd'hui ». Conserver les unités et devises d'un seuil, ainsi que la version des critères qui explique chaque résultat. Les identifiants stables de fait et les contraintes d'unicité font partie du modèle, pas seulement d'un filtre dans l'interface.

## Compte, accès et confidentialité

L'analyse de prévisualisation reste accessible sans compte, mais elle est limitée contre les abus avant tout appel coûteux. Une proposition temporaire peut être reprise pendant l'identification grâce à un jeton opaque à durée limitée ; celui-ci ne donne pas accès aux surveillances d'un compte. **Recommandation à valider : connexion par lien ou code reçu par e-mail via un fournisseur d'authentification géré.** Cela évite un mot de passe maison et n'impose pas un compte Google. Le fournisseur, le coût d'envoi, la récupération et l'expérience sur plusieurs appareils seront comparés avant choix.

Toute opération sur une surveillance, un résultat, un appareil ou une demande de suppression vérifie côté service l'identité et la propriété ; le client ne choisit jamais un identifiant de propriétaire fiable. Les tâches internes utilisent une identité de service distincte. Les secrets d'API, les autorisations d'envoi push et les limites globales ne sont pas embarqués dans l'APK. Les données en transit et au repos sont protégées avec les moyens du fournisseur choisi ; les accès administratifs sont limités et journalisés sans recopier systématiquement les demandes privées.

Le texte libre peut contenir des informations personnelles non prévues. L'interface prévient de ne pas saisir de secrets, et le système limite ce qu'il transmet aux prestataires à ce qui est nécessaire à la méthode. Les pages et réponses de sources sont des **données non fiables** : elles ne peuvent ni changer les instructions du service ni déclencher seules une action. Une URL fournie par l'utilisateur est validée et protégée contre l'accès à des ressources internes comme prévu au document 06.

## Conservation et suppression : paramètres de travail validés

La CNIL indique que la durée de conservation des données personnelles se définit selon leur finalité ; il n'existe pas une durée universelle pour cette application. Les durées ci-dessous sont **validées comme paramètres de conception de la V1**, sans constituer une garantie de conformité juridique. Leur mise en œuvre et leur adéquation aux finalités, aux besoins de preuve et à la configuration du fournisseur seront vérifiées avant publication. [CNIL — durées de conservation](https://www.cnil.fr/fr/passer-laction/les-durees-de-conservation-des-donnees).

| Catégorie | Durée retenue pour la conception | Fin ou suppression |
| --- | --- | --- |
| Brouillon local non envoyé | Jusqu'à envoi, effacement par l'utilisateur ou désinstallation. | Supprimable depuis l'interface de création ; jamais transmis tant qu'il n'est pas soumis. |
| Proposition serveur non activée | 24 heures après la dernière analyse. | Expiration automatique du jeton et effacement du texte et du plan temporaire. |
| Surveillance active, plan et état initial | Tant que la personne choisit de la conserver. | Suppression explicite de la surveillance ou du compte. |
| Résultats et explications d'une surveillance active | 90 jours après chaque résultat, sous réserve de ne pas rendre une correction incompréhensible. | Purge automatique ; la durée et son effet sur l'historique sont annoncés. |
| Contrôles détaillés et traces techniques liées au compte | 14 jours, sauf incident ouvert nécessitant un traitement justifié. | Purge ou agrégation sans données identifiantes. |
| Jeton push | Tant que l'appareil est associé et le jeton valable. | Retrait à la déconnexion, invalidation ou suppression du compte. |
| Sauvegardes | Rotation et suppression sous 30 jours au plus, à vérifier avec l'hébergeur choisi avant publication. | Une restauration ne doit pas réactiver un compte ou une surveillance supprimés. |

La suppression d'une surveillance prend effet immédiatement pour l'accès normal et les nouvelles vérifications ou émissions. Une suppression de compte retire ses surveillances, résultats, appareils et propositions associées ; les travaux déjà lancés sont neutralisés avant émission. Les copies de sauvegarde expirent selon leur rotation documentée et l'effacement y est rejoué en cas de restauration. Des métriques agrégées non identifiantes peuvent rester si elles sont réellement anonymes. Le parcours explique à la personne ce qui est supprimé tout de suite et ce qui suit la rotation des sauvegardes. La durée des surveillances terminées mais non supprimées et le traitement des comptes durablement inactifs restent ouverts : les conserver indéfiniment n'est pas une valeur par défaut.

## Exploitation et sécurité de fonctionnement

Les composants exposent des métriques de santé : contrôles dus/réussis/échoués, retard, sources silencieuses, résultats, notifications demandées, coûts et quotas. Les journaux portent des identifiants techniques et codes d'erreur ; les textes de demande, réponses brutes de prestataire et jetons d'accès ne sont pas copiés par défaut. Les sauvegardes, restauration et migrations de schéma doivent être testées avant publication.

Une nouvelle version de l'Android app et une ancienne doivent pouvoir coexister pendant une transition : contrat d'API versionné, migration compatible et capacité à refuser une version devenue incompatible avec un message explicite. Les opérations réessayées sont idempotentes ; les dates sont stockées sans ambiguïté de fuseau puis présentées dans la langue et le fuseau adaptés. Le document 09 fixera les tests automatiques, essais de panne, critères de qualité et conditions de publication.

## Exigences validées

| Référence | Exigence |
| --- | --- |
| ARCH-R01 | Faire tourner les vérifications côté service indépendamment de l'état du téléphone. |
| ARCH-R02 | Garder état confirmé, versions de plan et résultats comme données de référence côté service. |
| ARCH-R03 | Séparer UI Android, accès aux données et règles de surveillance ; garder les secrets côté serveur. |
| ARCH-R04 | Prévisualiser sans compte avec limites d'usage, puis identifier à l'activation durable. |
| ARCH-R05 | Protéger toute ressource et action durable par une vérification de propriété côté service. |
| ARCH-R06 | Rendre commandes et émissions idempotentes, avec contrôle de version et d'état avant notification. |
| ARCH-R07 | Préserver sources, preuves, dates distinctes et version de critères pour chaque résultat. |
| ARCH-R08 | Montrer l'état réel après réponse du service ; ne pas présenter une modification hors ligne comme appliquée. |
| ARCH-R09 | Définir et appliquer des durées par finalité, suppression du compte et comportement des sauvegardes. |
| ARCH-R10 | Mesurer fonctionnement et coût sans exposer inutilement le contenu privé dans les journaux. |
| ARCH-R11 | Isoler les adaptateurs de sources et fournisseurs pour pouvoir les tester et remplacer. |
| ARCH-R12 | Préparer une compatibilité de contrat entre versions Android et service pendant les mises à jour. |
| ARCH-R13 | Appliquer MVVM dans la présentation Android et une règle de dépendance Clean Architecture contrôlable entre présentation, domaine et data. |
| ARCH-R14 | Garder le domaine Android indépendant des SDK et utiliser des cas d'usage lorsqu'ils portent une règle ou une coordination réelle. |

## Scénarios d'acceptation validés

| Référence | Étant donné | Quand | Alors |
| --- | --- | --- | --- |
| ARCH-A01 | Un téléphone est éteint à l'heure d'un contrôle. | La source publie un événement admissible. | Le service vérifie et enregistre le résultat ; le téléphone le retrouve ensuite, même si le push n'a pas été affiché. |
| ARCH-A02 | Une activation reçoit deux fois la même commande après perte réseau. | Le service traite les demandes. | Une seule surveillance est créée et la réponse décrit son état réel. |
| ARCH-A03 | Un contrôle est relancé après panne. | Il retrouve le même fait. | Un seul résultat et une seule intention d'envoi existent pour ce fait et cette version. |
| ARCH-A04 | Une pause a été confirmée pendant un contrôle. | Le contrôle termine après la pause. | Aucune nouvelle intention d'envoi n'est créée. |
| ARCH-A05 | La première nouveauté termine une demande ponctuelle. | Son résultat est enregistré. | L'intention d'envoi légitime est conservée et la surveillance passe à « terminée » sans autre contrôle. |
| ARCH-A06 | Un compte A demande le résultat du compte B. | L'API reçoit la requête avec une session A valide. | Elle refuse sans révéler le contenu du résultat B. |
| ARCH-A07 | Une proposition anonyme expire. | Quelqu'un tente de l'activer. | Le service demande une nouvelle analyse, sans créer de surveillance à partir d'un plan périmé. |
| ARCH-A08 | Android est hors ligne au moment d'une suppression. | La personne agit. | L'interface n'annonce pas la suppression comme effectuée ; elle montre l'échec ou l'attente explicitement. |
| ARCH-A09 | La permission de notification est refusée. | Un résultat est qualifié. | Le résultat reste consultable après synchronisation et l'absence de push est visible. |
| ARCH-A10 | Un compte est supprimé alors qu'une tâche était planifiée. | La tâche démarre ou se termine. | Aucun nouveau résultat ou envoi n'est exposé pour ce compte ; les données suivent la politique d'effacement. |
| ARCH-A11 | Un fournisseur de source est remplacé. | Le même corpus de faits est rejoué. | Les critères et résultats restent explicables ; une baisse de couverture est signalée avant de prétendre poursuivre à l'identique. |
| ARCH-A12 | Une sauvegarde contenant un compte supprimé est restaurée. | La restauration s'achève. | Le compte et ses surveillances ne redeviennent ni accessibles ni actifs. |
| ARCH-A13 | Le service change de format de réponse pour l'état d'une surveillance. | L'adaptateur data est mis à jour. | Les modèles et règles du domaine, ainsi que l'écran Compose, n'importent aucun objet du client HTTP et restent testables indépendamment. |
| ARCH-A14 | Une règle de présentation métier est testée sans émulateur Android. | Le cas d'usage du domaine est exécuté avec un faux repository. | Son résultat est vérifiable sans SDK fournisseur, base ou requête réseau. |

## Arbitrages validés par Fabien

| Référence | Décision | Effet du choix |
| --- | --- | --- |
| ARCH-D01 | Un service central léger, déployé comme un seul ensemble logique, avec base et planification hébergées ; Android n'exécute pas les contrôles récurrents. | Fiabilité hors téléphone ; coût d'hébergement et responsabilité d'exploitation à mesurer. |
| ARCH-D02 | Un seul module Android pour commencer, avec MVVM, Clean Architecture et frontières de dépendances contrôlées ; extraire des modules quand un besoin mesuré apparaît. | Architecture défendable en entretien sans configuration Gradle disproportionnée. |
| ARCH-D03 | Compte par lien ou code e-mail auprès d'un fournisseur géré ; prévisualisation sans compte. | Parcours simple et indépendant d'un compte Google ; coût et fournisseur à comparer. |
| ARCH-D04 | Durées de conception : proposition 24 h, résultats 90 jours, traces détaillées 14 jours, sauvegardes au plus 30 jours. | Effet sur la confidentialité, l'historique consultable et le coût de stockage. |
| ARCH-D05 | Utiliser des ports et adaptateurs pour isoler les sources et prestataires du moteur côté service ; ne pas remplacer MVVM par une étiquette hexagonale sur Android. | Sources remplaçables et responsabilités lisibles ; évaluation selon le comportement réel. |

Les technologies concrètes du backend, du fournisseur d'identification, de la base et du push restent à choisir après comparaison. Le devenir d'une surveillance terminée non supprimée et d'un compte durablement inactif reste à spécifier avant publication. Les durées validées devront être confrontées aux finalités et aux possibilités du fournisseur choisi avant de figer l'implémentation ; toute modification sera expliquée et documentée. Aucun agent de développement ne doit transformer ces points ouverts en choix implicites.

## Spécification du socle Android — 20 octobre 2026

**Statut : choix techniques réversibles pris dans la délégation de Fabien.** Ils concrétisent les principes validés ; la matrice finale et le socle S1 sont maintenant compilés/testés, voir le bilan en fin de document. La reprise du travail ne nécessite pas une approbation de chaque bibliothèque. Les décisions affectant promesse, budget ou accès demeurent soumises à Fabien. Le backlog et les critères de fin sont dans le document 10.

### Outillage et bibliothèques retenus

| Besoin | Choix | Motif et limite |
| --- | --- | --- |
| IDE / environnement | Android Studio stable pour Apple Silicon, SDK Android géré par l'IDE, émulateur ARM64 | Environnement utilisable par Fabien pour coder, déboguer et apprendre ; la ligne de commande sert à reproduire les contrôles. |
| Build | Gradle Kotlin DSL, wrapper vérifié et version catalog | Versions exactes enregistrées dans le projet ; aucune dépendance dynamique `+`. Un module `:app`. |
| Langage / interface | Kotlin, Compose, Material 3, une Activity | Choix déjà validés. Composants Android natifs, modes clair/sombre, edge-to-edge et ressources FR/EN dès le départ. |
| Présentation | ViewModel par écran ou parcours cohérent, état immuable `StateFlow`, coroutines | Actions explicites, état collecté selon le cycle de vie ; aucun Context, client HTTP ou SDK fournisseur dans le ViewModel. |
| Navigation | Navigation 3 stable, clés typées sérialisables, sauvegarde de pile | Projet Compose neuf ; modèle de navigation et gestion des ViewModels testés dès S1. Pas de navigation fondée sur des concaténations de routes. |
| Injection | Hilt avec KSP, injection par constructeur | Objectif pédagogique et plusieurs ViewModels ; assemblage remplaçable en tests. Pas de service locator global. |
| Sérialisation / réseau | kotlinx.serialization ; Retrofit + OkHttp à l'intégration | DTO séparés du domaine, client observable en tests. Aucun réseau ajouté simplement pour la démonstration. |
| Persistance locale | SavedStateHandle pour l'état de parcours ; DataStore pour préférences et brouillon durable | Pas de Room ni de file de commandes hors ligne sans besoin validé. Ne pas utiliser DataStore comme base de résultats serveur. |
| Tests | JUnit, kotlinx-coroutines-test, Turbine pour les séquences de Flow ; Compose UI tests pour les parcours | Horloge, identifiants et dispatchers remplaçables ; pas de temporisations réelles dans les tests unitaires. |
| Qualité | Android Lint, compilation, tests et vérification des frontières | GitHub Actions préparé au sprint 1 ; exécution distante uniquement après choix/connexion du dépôt. Pas d'outil d'orchestration supplémentaire. |

Les [recommandations Android](https://developer.android.com/topic/architecture/recommendations) étayent les repositories, ViewModels et flux unidirectionnels. [Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3) présente une version stable 1.2.0 au 20 octobre. [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) fournit l'injection et son intégration Android. Le choix de navigation précise le principe historique de navigation typée ; il ne change pas les parcours.

**Matrice de build de départ, à vérifier au ticket S1-02 :** la documentation [AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes) indique Gradle 9.6.0, JDK 17 et support API 37. L'ensemble doit être réellement disponible en canal stable et compatible avec l'IDE installé avant sélection. AGP 9 utilise [Kotlin intégré](https://developer.android.com/build/migrate-to-built-in-kotlin) : ne pas recopier une configuration historique ajoutant automatiquement le plugin Kotlin Android. Compilateur Compose et sérialisation doivent être alignés avec Kotlin. Choisir une BOM Compose stable et les versions compatibles de Lifecycle, Activity, Hilt/KSP ; les inscrire au catalogue puis fournir la preuve de résolution/compilation. Les versions d'exemple d'une page ne constituent pas à elles seules une matrice compatible.

Le Gradle 8.13 téléchargé précédemment n'est donc **pas** adopté parce qu'il est déjà sur disque. Aucun nouvel environnement n'a été installé pour rédiger cette spécification. La cible de compilation/publication sera la version stable compatible retenue ; les contraintes Play seront revérifiées avant soumission. Minimum technique de travail : API 23, sous réserve des dépendances ; toute nécessité de relever ce minimum et d'exclure des appareils est signalée avant arbitrage. Les numéros non encore éprouvés sont clos par S1-02, pas inventés dans ce document.

### Organisation des sources

Le projet Gradle sera dans `android/`, distinct de `engine-lab/`. L'application de démonstration porte un identifiant séparé avec suffixe `.demo` ; l'identifiant public définitif et la signature de publication ne sont pas figés par ce lot.

- `app/` : Activity, application, thème, navigation et assemblage Hilt.
- `feature/create/presentation/` : saisie, clarification, proposition et confirmation.
- `feature/watches/presentation/` : liste, détail, commandes et résultats.
- `feature/account/presentation/` : session et identification, d'abord simulées.
- `domain/model/`, `domain/repository/`, `domain/usecase/` : modèles Kotlin, ports et coordinations justifiées.
- `data/demo/`, puis `data/remote/` et `data/local/` : adaptateurs et conversion des données.

Ces chemins sont des packages sous le module `:app`, pas des modules Gradle supplémentaires. Présentation → domaine ; data → domaine ; assemblage → implémentations. Domaine : aucun import Android, Compose, Retrofit, Hilt, SDK fournisseur ou DTO. `Flow`/coroutines sont autorisés comme contrats Kotlin. Les classes de domaine reçoivent leurs dépendances par constructeur ; Hilt les assemble à la frontière plutôt que d'y introduire ses annotations spécifiques.

### Contrat applicatif v0 — indépendant du fournisseur

Ce contrat est la cible du repository simulé et du futur adaptateur distant. Il ne prétend pas définir une API déjà déployée. Les chemins HTTP, authentification et JSON seront spécifiés avant S3-02. Identifiants opaques, instants UTC, pas de texte privé dans les routes. Aucun type Exa/Firecrawl ne traverse cette frontière.

| Modèle | Champs et invariants |
| --- | --- |
| `RequestDraft` | Texte, langue, réponses structurées aux questions. Texte original conservé jusqu'à abandon ou confirmation ; toute correction invalide l'ancienne proposition affichée. |
| `Clarification` | Identifiant de proposition, version, identifiant de question, libellé et options éventuelles. Reprise sans redemander une réponse déjà fournie. |
| `Proposal` | Identifiant, version, expiration serveur, demande reformulée, critères, exclusions, mode ponctuel/continu, portée, méthode, sources connues (éventuellement vides), intervalle annoncé, limites et quota applicable. Absence de source trouvée distincte d'absence de méthode qualifiée. |
| `Watch` | Identifiant, version, proposition confirmée, statut, santé, dernière réussite, dernier échec et prochaine vérification connue. L'état confirmé provient du repository ; le chargement d'une commande reste un état UI séparé. |
| `Finding` | Identifiant, suivi et version du plan, titre, fait, justification, preuves avec lien/citation, publication nullable, observation, éventuelle date d'événement et éventuelle correction d'un résultat précédent. Jamais de date inventée. |
| `UsageLimits` | Capacité disponible, limite par compte, portée de la limite, période, échéance de reprise si connue. Une limite financière ne devient pas une erreur de faisabilité. |
| `CommandReceipt` | Identifiant de commande et état/version confirmés, ou confirmation de suppression. Même identifiant rejoué = même effet ; la simple réception réseau ne prouve pas la réussite métier. |

| Port et opération | Entrées | Résultat |
| --- | --- | --- |
| `ProposalRepository.evaluate` | Brouillon, version attendue si clarification | `Ready(Proposal)`, `NeedsClarification`, `Unsupported(reason)` ou échec typé. |
| `ProposalRepository.get` | Identifiant | Proposition courante, expirée ou introuvable. |
| `WatchRepository.activate` | Proposition + version, identifiant de commande | Suivi confirmé ou échec. Le compte appartient au contexte d'authentification, jamais à un identifiant de propriétaire choisi par le client. |
| `WatchRepository.observeList/observeDetail` | Paramètres de lecture / identifiant | Flow de données confirmées avec état de fraîcheur. Une liste vide réussie n'est pas une erreur. |
| `WatchRepository.refresh` | Identifiant ou liste | Actualisation ; conserver l'ancien contenu explicitement périmé en cas d'échec. |
| `WatchRepository.pause/resume/delete` | Identifiant, version attendue, identifiant de commande | Reçu confirmé ou échec ; pas de mise à jour optimiste irréversible. |
| `WatchRepository.replacePlan` | Suivi/version + nouvelle proposition/version + identifiant de commande | Nouveau plan confirmé, ancienne version conservée pour expliquer ses résultats. |
| `FindingRepository.list/get` | Suivi, curseur opaque éventuel / identifiant | Résultats sourcés ; introuvable/expiré et accès refusé gérés. |
| `SessionRepository` | État de session, démarrage/confirmation d'identification, déconnexion | Session simulée au sprint 2 ; identité gérée au sprint 3. Suppression du compte intégrée avant publication. |

Échecs stables : `Offline`, `TemporaryFailure`, `SessionRequired`, `AccessDenied`, `NotFound`, `ProposalExpired`, `VersionConflict`, `QuotaExceeded`, `Unsupported`. Chaque échec porte des paramètres contrôlés et une possibilité de réessai, jamais un message brut fournisseur destiné à l'écran. Les textes sont localisés à la présentation. L'annulation d'une coroutine est propagée ; elle ne devient pas une fausse erreur serveur.

### États, restauration et concurrence

Statut d'un suivi : `Active`, `Paused`, `Completed`, `SuspendedByCapacity`. Santé séparée : `NotCheckedYet`, `Healthy`, `Degraded`. Un suivi actif sans résultat est légitime ; une panne ne change pas la dernière réussite. Un contrôle non reçu à l'échéance rend la santé incertaine selon le contrat serveur, pas « tout va bien ».

L'écran de création porte un brouillon et un état `Editing / Evaluating / Clarifying / Ready / Confirming / Error`. L'action soumettre est désactivée pendant le traitement. Une réponse tardive à l'ancien brouillon ne remplace jamais la proposition d'un brouillon corrigé. Le ViewModel produit l'état ; la navigation utilise des clés et identifiants stables, pas des objets métier complets. Le bouton retour, une rotation et une restauration de pile ne rejouent pas une activation.

Le brouillon durable est sauvegardé localement via l'adaptateur DataStore. SavedStateHandle conserve les identifiants et petits états de parcours ; aucune collection volumineuse ni secret. Après mort du processus, recharger une proposition ou un suivi depuis le repository avant de confirmer. Si la démonstration en mémoire ne contient plus cet identifiant, afficher « démonstration réinitialisée » et permettre de reprendre ; ne pas inventer de persistance distante.

Une commande reste visible comme en cours jusqu'au reçu. En cas de réponse perdue, réessayer avec le même identifiant, ou relire l'état avant une nouvelle commande ; pas de répétition incontrôlée. Un conflit de version recharge l'état puis laisse la personne confirmer son intention. Ces règles doivent être exercées par le faux service autant que par l'adaptateur réel.

### Démonstration et production séparées

Variante `demo` avec bannière persistante et nom d'application distinct, fixtures synthétiques et fournisseur de session simulé ; aucune clé, permission INTERNET ou appel externe pour ses parcours. Les liens de preuves fictives sont signalés et non ouverts comme de vraies annonces. Des scénarios de démonstration choisissent succès, clarification, absence de résultat, refus, quota et panne : aucune heuristique locale prétendant comprendre une demande arbitraire. Le repository simulé valide les transitions/versions, pas la faisabilité du Web.

La variante `live` n'est activée qu'au sprint 3 avec une configuration explicite. L'assemblage échoue si un adaptateur simulé se retrouve dans une variante publiable. Les tests unitaires/CI n'utilisent aucune clé réelle. Aucun SDK de monitoring ne se trouve dans Android ; WorkManager ne porte pas les contrôles récurrents. FCM et l'identité réels restent des décisions de S3-01, pas des dépendances nécessaires à S1.

### Vérifications attendues

- Contrat : proposition expirée, commande répétée, conflit de version, quota, reprise et suppression ; mêmes tests de contrat réutilisables pour demo et remote.
- ViewModels : traitement en cours, erreur récupérable, contenu conservé, réponse tardive et annulation ; Flow observé avec ordonnanceur de test.
- UI : saisie → proposition → activation simulée → détail ; retour et rotation sans doublon ; pause/suppression échouée non affichée comme réussie.
- Architecture : contrôle d'import du domaine et de l'absence d'accès data direct depuis les ViewModels.
- Build : variante demo sans permission réseau/secrets, APK installé et lancé, sources et rapports rattachés à la même révision.

Aucun test n'est déclaré passé par cette spécification. Le socle aura une première preuve au sprint 1 ; les parcours complets au sprint 2. Room, modularisation avancée, comparaison de modèles, design de marque et paiement restent hors de ces lots.

### Résultat S1 — matrice éprouvée le 20 octobre 2026

AGP 9.4.1 / Gradle 9.6.0 / JDK 17.0.20.1, Kotlin et plugins Compose/sérialisation 2.3.21, KSP 2.3.12, Hilt 2.60.1, BOM Compose 2026.09.00, Activity 1.13.0, Lifecycle 2.11.0, coroutines 1.11.0. API minimum 23 conservée ; compilation/cible 37. Android Studio Rabbit1 2026.2.1.8 Apple Silicon, signature et téléchargement contrôlés.

**Correction de compatibilité : Navigation 3 stable 1.1.0** remplace la proposition 1.2.0 du tableau. La compilation a révélé un minimum API 24 dans 1.2.0 ; 1.1.0 conserve API 23 sans contournement de manifeste. L’identifiant SDK est `platforms/android-37.0`. Le profil CLI `medium_phone` a créé un émulateur API 36 ARM64, démarré et utilisé pour les tests. Cela ne vaut pas une validation sur toutes les versions supportées.

Le code utilise un port `WatchRepository` réduit à lecture/rafraîchissement, deux destinations de démonstration et un seul ViewModel. Le contrat applicatif complet reste une spécification pour S2/S3, pas du code déjà livré. Le domaine n’importe que Kotlin/coroutines et ses propres modèles. Aucun cas d’usage de simple délégation ajouté.

Deux tests unitaires et un test sur appareil passent ; Lint : zéro erreur, zéro avertissement, six suggestions de mise à jour conservées comme informations. Ces suggestions ne bloquent pas le build afin qu’une publication amont ne change pas le verdict d’un code inchangé. Défauts d’icône et de sauvegarde réellement corrigés, aucun baseline ne les masque. APK sans permission INTERNET vérifié, règles de sauvegarde explicites, variante demo release désactivée.

[Guide de reproduction](../android/README.md) · [Bilan et preuve S1](../validation/android-s1/bilan.md). La CI distante, l’authentification, la persistance durable, les commandes métier, le push et le moteur réel ne sont pas validés par ce sprint.

## Contrat d’implémentation S2-01 — préparation du 20 octobre 2026

**Statut : spécification prête à implémenter, aucun test S2 déclaré exécuté.** Précisions techniques dans la délégation existante ; aucune nouvelle promesse produit. Références : contrat v0 ci-dessus, documents 03/04/05, tickets S2 du document 10. Cette section précise le faux service nécessaire aux parcours ; elle ne définit ni un moteur de recherche ni le futur protocole HTTP.

### Frontière et données de test

Le vrai code Android (domain, ViewModels, navigation et écrans) consomme des ports Kotlin. Les adaptateurs `src/demo` simulent les réponses côté service. Aucun adaptateur de fournisseur, parcours de connexion réel ou règle d’interprétation approximative du texte n’est ajouté en S2. Les fixtures sont choisies explicitement dans un contrôle marqué « Scénario de démonstration ». Ce contrôle ne devient pas un catalogue obligatoire du produit.

Utiliser un état en mémoire partagé par les repositories demo pour propositions, session, suivis, résultats et reçus de commande. L’état partagé évite plusieurs vérités incompatibles entre liste et détail. Exposer des snapshots immuables ; un seul mécanisme de sérialisation des mutations (par exemple Mutex) garantit leur atomicité. Ni SQLite ni backend local supplémentaire pour simuler ce contrat.

Injecter une horloge et un générateur d’identifiants en Kotlin, avec doubles déterministes dans les tests. Les échecs sont pilotés par des scénarios et des points de suspension contrôlables, jamais par des tirages aléatoires ou un `sleep`. L’annulation reste une annulation. Un problème inattendu n’est pas transformé silencieusement en succès ou en refus métier.

Les fixtures portent un sujet synthétique d’événement ponctuel et un sujet synthétique de suivi général. Elles réutilisent la même structure et ne prouvent aucune couverture de catégorie. Les sources fictives utilisent un domaine réservé (`example.invalid`) et ne sont pas ouvertes comme preuves réelles. Prix, quotas et cadence de fixture sont des paramètres visibles de démonstration ; ils ne deviennent pas des valeurs d’offre publique. Le contrat expose la limite reçue au lieu de la recopier en constante dans chaque écran.

### Invariants des opérations

| Opération / situation | Comportement exigé de l’adaptateur demo | Effet attendu côté Android |
| --- | --- | --- |
| Évaluer un brouillon | Associer la réponse à la révision du brouillon ; fournir Ready, NeedsClarification, Unsupported ou échec selon le scénario explicitement choisi. | Une réponse tardive d’une ancienne révision est ignorée ; aucune activation automatique. |
| Clarifier / corriger | Conserver les réponses pertinentes ; une correction invalide la proposition précédente côté présentation. Le service vérifie la version attendue. | Afficher la nouvelle proposition avant confirmation ; ne pas réutiliser un ancien accord pour une nouvelle portée. |
| Proposition expirée | `expiresAt <= now` est expiré. Une nouvelle activation échoue avec ProposalExpired, sans suivi créé. | Conserver le brouillon et proposer une nouvelle analyse. |
| Activer sans session | SessionRequired, sans mutation. Le scénario de connexion crée une identité synthétique, sans adresse personnelle demandée. | Prévisualisation toujours accessible ; connexion demo au moment de confirmer, proposition conservée puis revalidée. |
| Activer avec session | Vérifier proposition/version/expiration et capacité dans une même mutation atomique, puis créer le suivi et son reçu. Une même proposition/version déjà activée ne crée pas un second suivi. | Actif seulement après reçu confirmé ; afficher les critères de cette version. |
| Réessayer une commande réussie | Identifiant de commande et contenu immuable conservés. Dans la même session autorisée, rejouer la même commande retourne son reçu sans nouvel effet, même si la proposition a expiré entre-temps. | La perte de réponse ne crée pas un doublon. Réutiliser le même identifiant, pas un nouvel UUID à chaque tentative. |
| Réutiliser un identifiant avec un autre contenu | Refuser sans mutation avec conflit de commande, distinct d’un conflit de version de ressource. Ajouter ce code au contrat technique. | Ne pas réessayer aveuglément ; relire l’état et proposer une nouvelle action explicite. |
| Mauvaise version de suivi | VersionConflict, sans mutation ni incrément. Le service ne force jamais la version demandée sur la version courante. | Recharger, montrer l’état courant, laisser confirmer à nouveau. |
| Pause / reprise | Active → Paused ; Paused → Active seulement après confirmation et vérification de capacité. Incrémenter la version seulement lors d’une mutation. Une répétition de pause déjà effective est sans effet supplémentaire. | Garder l’ancien état pendant l’attente ou l’échec. La reprise montre un nouvel état initial, sans rattrapage automatique d’alertes. |
| Modification du plan | Remplacer la version de plan uniquement après nouvelle proposition confirmée. Les résultats antérieurs gardent leur référence de version. | Ne pas présenter un ancien résultat comme preuve des nouveaux critères. |
| Suppression | Supprimer l’accès normal au suivi et à ses résultats dans la même mutation ; garder un reçu technique sans contenu pour le rejeu dans la session demo. | Retirer le suivi après confirmation ; un lien restauré vers le suivi supprimé affiche l’indisponibilité. |
| Quota / capacité indisponible | Échec typé avec portée, limite et reprise connue ou inconnue ; aucune réservation partielle ni modification du contenu confirmé. | Distinguer limite d’usage et impossibilité technique de répondre à la demande. |
| Source dégradée | Garder le statut de contrôle et la dernière réussite ; actualiser séparément santé et dernier échec. | Ne pas afficher « aucune nouveauté » en conclusion d’une panne. |
| Lecture vide / lecture en échec | Succès avec liste vide distinct d’un échec typé. Une donnée précédente conservée porte un état de fraîcheur explicite. | État vide, panne et ancien contenu restent trois situations différentes. |

Le rejeu d’un reçu suppose toujours une session autorisée ; le cache d’idempotence n’est pas un moyen de contourner l’accès à un autre compte. Les tests d’isolation sur un vrai serveur appartiennent à S3 : le faux service ne prouve pas une sécurité de production. Les contraintes financières atomiques sont exercées avec une capacité de fixture injectée ; le calcul de la capacité commerciale reste au document 07/S3-01.

### États et restauration à brancher aux tickets suivants

- `CreateViewModel` possède le brouillon, sa révision et l’état du parcours ; aucune deuxième copie modifiable dans les composables. Les composants reçoivent état et callbacks.
- Une navigation vers la confirmation porte un identifiant/version, pas une proposition complète figée dans la route. Une réponse du service prévaut sur la copie de présentation.
- Une commande en attente porte son identifiant et ses paramètres ; double clic ignoré. Un abandon de l’attente ne prouve pas l’annulation de la mutation côté service.
- DataStore conserve le brouillon et les réponses, pas une session ou un état de surveillance présenté comme confirmé. Effacer le brouillon après activation confirmée ou abandon explicitement demandé. Aucune synchronisation distante du brouillon.
- Après recréation d’Activity, conserver les petits états de parcours. Après perte du processus demo, les identifiants absents de la mémoire du faux service conduisent à « Démonstration réinitialisée » ; le brouillon local peut être repris. Aucun rejeu automatique d’une activation au simple réaffichage.
- Notification push, permission système et lien reçu de l’extérieur restent à S3-05. S2 affiche honnêtement que les notifications ne sont pas disponibles en démonstration.

### Cas d’acceptation à rendre exécutables

Ces cas sont des attentes, pas des résultats. Leur implémentation sera reliée aux noms des tests dans le bilan de ticket. Les cas de port seront regroupés dans une suite réutilisable : adaptateur demo maintenant, serveur/adaptateur remote lorsque S3 le permettra.

| ID / ticket | Étant donné / action | Résultat vérifiable | Exigence reliée |
| --- | --- | --- | --- |
| S2-C01 / S2-01 | Proposition valide, puis horloge exactement à son expiration ; activation | ProposalExpired ; aucun suivi ni reçu de succès ajouté | ARCH-A07 |
| S2-C02 / S2-01 | Mutation d’activation réussie mais réponse perdue ; rejeu du même identifiant/contenu | Même suivi et même reçu, un seul effet ; testé également après expiration de la proposition | ARCH-A02 |
| S2-C03 / S2-01 | Même identifiant de commande avec paramètres différents | Conflit explicite ; état inchangé | ARCH-R06 |
| S2-C04 / S2-01 | Deux commandes concurrentes pour deux propositions avec une capacité fixture restante de 1 | Une seule activation réussit ; l’autre retourne QuotaExceeded ; compte final égal à 1 | REV-A05, COST-A04 |
| S2-C05 / S2-01 | Nouvelle activation sans session puis avec version périmée | SessionRequired puis VersionConflict selon le cas ; aucune mutation | ARCH-R04, ARCH-R06 |
| S2-C06 / S2-01 | Pause/modification échouée ou en conflit de version | Statut et version précédents conservés | MVP-A12, ARCH-A08 |
| S2-C07 / S2-01 | Dernière réussite connue, puis panne de source, puis réussite sans nouveauté | Panne : réussite inchangée ; succès suivant : réussite actualisée, aucun résultat inventé | MVP-A08, REV-A04 |
| S2-C08 / S2-01 | Suppression confirmée, commande rejouée puis relecture suivi/résultat | Même confirmation de suppression ; relectures indisponibles ; pas de contenu révélé par le reçu | ARCH-R09 |
| S2-C09 / S2-02 | Réponse lente pour brouillon A ; correction en B ; réponse A arrive ensuite | Seule B peut produire la proposition confirmable ; aucun écran de succès tardif pour A | UX-D05, ARCH-R08 |
| S2-C10 / S2-02–03 | Demande de suivi général dans le scénario clarification, puis portée partielle | Clarification accessible ; critères et réduction visibles ; connexion demo seulement après confirmation | MVP-A02, MVP-A07, UX-D02 |
| S2-C11 / S2-03–05 | Activation sans résultat initial, puis reprise de liste/détail | Suivi visible sans faux résultat ; absence initiale de résultat ne devient pas un refus | REV-A01, REV-A06 |
| S2-C12 / S2-04–05 | Pause/reprise et remplacement de critères avec résultat antérieur | Pas de rattrapage automatique ; ancienne version du résultat conservée et expliquée | AL-D09, ARCH-R07 |
| S2-C13 / S2-05 | Résultat dont la date de publication est inconnue | Date inconnue affichée ; observation et événement restent distincts ; preuve fictive signalée | REV-A03, ARCH-R07 |
| S2-C14 / S2-06 | Retour, recréation puis mort du processus demo pendant un parcours | Aucun doublon de commande ; brouillon récupérable ; mémoire serveur perdue expliquée | UX-D05, ARCH-A02 |

### Prêt à démarrer / terminé

**Prêt :** exigences produit référencées, contrats et invariants ci-dessus, cas attendus écrits, dépendances S1 passantes, aucun accès externe ni nouveau budget nécessaire. **Terminé S2-01 :** modèles/ports/adaptateurs codés, cas C01–C08 exécutés, tests S1 toujours passants, absence de réseau conservée, limites et preuve enregistrées. Une description en Markdown ne remplace pas ces tests.

Le premier parcours visible recherché en S2 reste saisie → proposition → connexion demo → suivi confirmé. Les autres états sont ajoutés selon les tickets, sans multiplier les écrans ou bâtir un moteur fictif généraliste. Le bilan de sprint montrera ce parcours et expliquera le rôle de chaque composant pour l’apprentissage Android.


## Exécution S2 — 20 octobre 2026

**S2-01 à S2-06 livrés localement.** Les cas S2-C01 à C14 ci-dessus ont été reliés aux tests et preuves dans le [bilan S2](../validation/android-s2/bilan.md). Les critères restent la référence ; leur couverture ne vaut pas validation d’un moteur réel.

Les ports du domaine sont implémentés par un service demo partagé, avec mutations atomiques, reçus idempotents et horloge injectable. Les ViewModels exposent un état observable ; les composables affichent cet état et transmettent les actions. DataStore conserve le brouillon, SavedStateHandle les petits identifiants de parcours. Une perte du processus efface honnêtement la mémoire du service demo et ne déclenche aucune activation automatique. L’assemblage de cette variante est isolé dans `src/demo`.

Validation : 24 tests unitaires, cinq parcours Compose dans trois configurations, restauration après mort réelle du processus, Lint et contrôle des frontières passants. Aucun réseau métier ni coût fournisseur. Les limites de qualification de production sont détaillées dans le bilan. La prochaine étape technique est S3-01 : ADR du service réel, contrat HTTP, identité, stockage, push et chiffrage avant intégration.


## Préparation S3 — 20 octobre 2026

Le [document 11](11-service-et-contrat-s3.md) et l’[ADR 0011](decisions/0011-service-reel-s3.md) précisent le service réel. Le [contrat OpenAPI](../contracts/application-v1.openapi.json) définit les échanges avant raccordement. Les anciens modèles S2 simplifiés restent la référence de la démo ; les DTO de S3 enrichissent questions, preuves, dates et opérations en attente dans `data/remote`, sans modifier les écrans ou le domaine à ce stade.

Le client HTTP est testé sur serveur local et n’est pas injecté dans la démo. Retrofit 3.0.0 et OkHttp 5.5.0 ont été résolus/compilés ; la permission Internet apportée par la bibliothèque est explicitement retirée du manifeste demo et le binaire est contrôlé. Les exemples contractuels ne sont empaquetés que dans les tests JVM. Les validations de format ne prouvent ni l’identité, ni les transactions serveur, ni la détection réelle.


## Exécution locale S3 — 20 octobre 2026

Variante `remoteDebug` distincte, ports raccordés à l'API et à Supabase Auth, session Keystore, UUIDv7 et reçu pending persistant par identifiant. Le domaine conserve les états de préparation et d'arrêt distant, les preuves et la précision date/instant. PostgreSQL privé/RLS, transactions de commandes, capacité, jobs à baux, inbox HMAC et outbox atomique sont implémentés sous `service/`. Le [bilan S3](../validation/s3/bilan.md) précise les confirmations fournisseur simulées et les validations réelles encore absentes. Aucune modification des promesses, budgets ou conditions de publication.
