# Sources et stratégie de recherche

Validé par Fabien le 2 octobre 2026. Ce document décrit comment Préviens-moi peut trouver des sources, qualifier une demande et effectuer des vérifications répétées à coût soutenable. Il complète le [périmètre du MVP](03-perimetre-mvp.md), les [parcours](04-parcours-et-ecrans.md) et les [règles d’alerte](05-regles-des-alertes.md) validés. Les choix de méthode sont consignés dans la [décision 0006](decisions/0006-sources-et-recherche.md). Aucun fournisseur ni budget n’est encore choisi. Les faits sur les services et standards externes ont été vérifiés le 2 octobre 2026 ; leurs tarifs, conditions et capacités devront être revérifiés avant un essai ou une intégration.

## La décision à prendre pour chaque demande

Le produit reçoit une phrase libre, mais il ne peut promettre une surveillance que si une **méthode de vérification récurrente** est identifiée pour les critères confirmés. La recherche initiale et les vérifications suivantes sont deux problèmes distincts : une recherche Web peut trouver une annonce aujourd’hui sans savoir observer demain toutes les annonces pertinentes. Une réponse convaincante d’un modèle n’est ni une preuve de couverture ni un plan de surveillance.

Une décision de faisabilité doit répondre aux questions suivantes :

| Référence | Question | Preuve attendue avant activation |
| --- | --- | --- |
| SRC-Q01 | Quel fait observable satisfera la demande ? | Critères, exclusions, zone et période reformulés puis confirmés. |
| SRC-Q02 | Où ce fait pourrait-il apparaître ? | Une ou plusieurs sources identifiées avec leur portée réelle, sans prétendre couvrir tout le Web. |
| SRC-Q03 | Comment consulter ces sources dans la durée ? | Format, accès, fréquence possible, droits de réutilisation et mécanisme de remplacement évalués. |
| SRC-Q04 | Comment distinguer l’ancien du nouveau ? | État initial, identifiants ou attributs stables, dates et stratégie de déduplication. |
| SRC-Q05 | Quel délai et quel coût faut-il annoncer ? | Mesures ou hypothèses explicites pour la méthode envisagée, y compris vérifications sans résultat. |
| SRC-Q06 | Comment saura-t-on que la méthode se dégrade ? | Contrôles de fraîcheur, succès, erreurs, disparition de source et requalification. |

Les issues possibles sont : **réalisable selon une portée annoncée**, **partiellement réalisable avec accord explicite**, **précision nécessaire**, **non prise en charge aujourd’hui** ou **analyse temporairement impossible**. « Aucune information trouvée » ne veut pas dire « aucun événement n’existe » ; « source inaccessible aujourd’hui » ne veut pas dire « demande intrinsèquement impossible ». L’application ne crée pas une surveillance active sur la seule base d’une recherche ponctuelle réussie.

## Hiérarchie pratique des méthodes

Cette hiérarchie est une **séquence d’examen**, pas une obligation de choisir la première technique disponible. On retient la méthode qui satisfait les critères de couverture, de fraîcheur, de preuve et de droits, puis on compare son coût total. Une source gratuite mais trop tardive ou instable ne passe pas les mêmes critères qu’une source payante adaptée.

| Méthode à examiner | Ce qu’elle apporte | Limite à vérifier | Usage recommandé |
| --- | --- | --- | --- |
| API ou jeu de données structuré du producteur | Champs stables, identifiants, dates et filtres parfois explicites. | Champ réellement couvert, fraîcheur, licence, quotas, disponibilité, historique et accès commercial. | Premier choix si elle répond au fait à détecter. |
| Abonnement de changement ou webhook proposé par le producteur | Signalement d’un événement sans interroger sans cesse la source. | Existence réelle, autorisation, signature ou authentification, pertes d’événements et mécanisme de reprise. | À préférer au sondage répété lorsque le producteur l’offre et que la livraison est vérifiable. |
| Flux RSS ou Atom du producteur | Publication d’entrées nouvelles, titres, liens et parfois dates/identifiants. | Le flux peut être partiel, tardif ou ne pas contenir le fait précis ; la licence n’est pas implicite. | Surveillance d’annonces ou de publications d’une source identifiée. |
| Calendrier public iCalendar/ICS | Événements, dates, lieu et modifications possibles. | Exposition effective d’un calendrier public, stabilité des identifiants, annulations, fuseaux horaires. | Cas où la condition porte sur une date ou un événement publié par son organisateur. |
| Page publique du producteur | Information de première main sans API connue. | Accès automatisé, stabilité du contenu, changements cosmétiques, charge et conditions du site. | Complément ou solution ciblée si le contrôle est permis et fiable. |
| API de recherche Web | Découverte de nouvelles sources et de pages non connues d’avance. | Index incomplet ou retardé, coût par recherche, source citée non nécessairement suffisante. | Exploration initiale, réévaluation ou suivi large lorsque les sources ciblées ne suffisent pas. |
| Service tiers de surveillance récurrente | Planification, recherche et collecte déjà opérées par le prestataire. | Prix par exécution, latence, qualité, preuve, contrôle de la couverture, dépendance et traitement des données. | Candidat de prototype ou de solution pour besoins difficiles, après comparaison. |
| Navigateur automatisé | Accès à certaines pages publiques rendues par JavaScript. | Coût, fragilité, règles du site, charge et sécurité ; ne contourne pas un espace privé. | Dernier recours ciblé, pas moteur universel par défaut. |

RSS 2.0 et Atom sont des formats de syndication documentés ; iCalendar définit un format d’événements. Leur existence ne garantit ni que le producteur publie un flux, ni que le flux soit complet pour la demande. [Spécification RSS 2.0](https://www.rssboard.org/rss-specification), [RFC Atom 4287](https://www.rfc-editor.org/info/rfc4287/), [RFC iCalendar 5545](https://www.rfc-editor.org/info/rfc5545/).

Un sitemap ou des métadonnées structurées d’une page peuvent aider à **découvrir** des URL ou à extraire un fait, mais ne constituent pas automatiquement une preuve de date de publication ni un flux d’alerte complet. Une newsletter publique peut fournir un indice ; sa réception et sa réutilisation doivent être examinées selon ses conditions et sa fiabilité. Ces pistes sont évaluées comme sources candidates, sans ajouter un connecteur par format avant un cas réel.

En France, le catalogue et l’API de data.gouv.fr sont des pistes de découverte de jeux de données, pas une API universelle des événements de la vie quotidienne. Chaque jeu possède ses propres ressources, dates et droits ; la licence associée au jeu est déterminante pour sa réutilisation. [Documentation des jeux de données data.gouv.fr](https://guides.data.gouv.fr/api-de-data.gouv.fr/reference/datasets), [guide officiel sur les conditions de réutilisation](https://guides.data.gouv.fr/guides/reutiliser-des-donnees/introduction-a-lopen-data/comprendre-les-conditions-dutilisation-des-donnees-en-open-data).

Pour les sources HTTP qui le permettent, les requêtes conditionnelles et un cache évitent de télécharger inutilement un contenu inchangé. Elles réduisent la charge et parfois le coût, sans démontrer qu’un événement absent de la réponse n’existe pas ailleurs. [RFC HTTP 9110](https://www.rfc-editor.org/rfc/rfc9110.html).

## Ce que font — et ne font pas — l’IA et MCP

L’IA peut aider à comprendre une phrase libre, repérer des entités, proposer des questions de clarification, explorer des sources, extraire un fait d’un contenu difficile et comparer le sens de deux annonces. Elle peut aussi produire une fausse correspondance, manquer une page, confondre date de publication et date d’événement, ou attribuer trop de certitude à un résumé. Les règles d’alerte du document 05 exigent donc des liens et des faits vérifiables derrière le résultat ; un modèle ne se certifie pas lui-même.

Une API de recherche Web existe chez OpenAI via l’outil `web_search` de la Responses API, avec citations et liste de sources consultées. Son usage est facturé ; un appel peut rechercher, ouvrir ou examiner des pages, mais il ne garantit pas d’avoir surveillé l’ensemble du Web ni de détecter une nouveauté au délai souhaité. Les citations provenant de cette recherche doivent être visibles et cliquables dans l’interface qui les présente. [Documentation officielle OpenAI Web search](https://developers.openai.com/api/docs/guides/tools-web-search).

Yutori documente une **Scouting API** qui accepte une demande en langage naturel, crée une tâche récurrente, prévoit un intervalle et permet de récupérer des mises à jour. Au 2 octobre 2026, sa page de prix affiche **0,35 $ par exécution** de Scout ; une exécution quotidienne sur 30 jours représenterait 10,50 $ pour une seule surveillance avant nos autres coûts. Ce chiffre illustre pourquoi l’exécution systématique d’un agent pour chaque surveillance doit être testée économiquement ; ce n’est pas un choix de fournisseur ni un prix garanti dans le temps. [Création d’un Scout](https://docs.yutori.com/reference/scouts-create), [mises à jour](https://docs.yutori.com/reference/scouts-updates), [tarification Yutori](https://docs.yutori.com/pricing).

MCP est un protocole d’exposition d’outils et de ressources à un modèle, pas un moteur qui découvre automatiquement une source légitime pour n’importe quel besoin. Un connecteur MCP pourrait appeler une API existante, sans rendre cette API plus complète ni résoudre à lui seul la planification, les doublons, les droits ou la qualité. Le MVP peut très bien interroger directement une API ou un flux sans MCP ; on l’ajoutera seulement si un besoin d’intégration concret le justifie. [Spécification MCP, outils et ressources](https://modelcontextprotocol.io/specification/draft/server/index).

## Plan de surveillance réutilisable

Après clarification et avant confirmation par la personne, le service prépare un plan versionné. Il sépare la **formulation de l’utilisateur** de sa **méthode d’observation** pour pouvoir changer de source sans réécrire silencieusement la demande.

| Élément du plan | Contenu attendu |
| --- | --- |
| Critères confirmables | Sujet, identifiants utiles, lieu, période, condition, exclusions et type ponctuel ou continu. |
| Couverture promise | Quelles sources et quels faits sont couverts, quelles parties de la demande restent hors portée. |
| Sources candidates | Identité du producteur, URL ou identifiant, méthode d’accès, rôle primaire ou secondaire. |
| Méthode d’extraction | Champs structurés ou extraction contrôlée, format, version et limites connues. |
| État initial | Valeurs et événements déjà connus, avec dates et niveau d’incertitude. |
| Comparaison | Clés d’événement, règle de changement, seuil et preuve selon le document 05. |
| Exécution | Cadence annoncée, prochaine vérification si connue, coût estimé, plafond et comportement en échec. |
| Santé et remplacement | Contrôles de fraîcheur et disponibilité, source de secours possible, date de dernière réévaluation. |

Le plan passe par **proposé**, **confirmé**, **actif**, **dégradé**, **à requalifier** ou **non pris en charge** selon les règles des documents 03 à 05. Ces états logiques n’imposent pas un écran de plus. Si une source essentielle disparaît ou cesse de publier, on ne transforme pas son silence en « aucun événement » : le plan est requalifié et la perte de couverture devient visible.

Le plan ne doit pas contenir de secret fournisseur accessible depuis l’application Android. Les identifiants API, les limites globales et les tâches périodiques sont gérés côté service. Les responsabilités techniques détaillées seront décrites au document 08.

## Registre de sources et conditions d’admission

Chaque source utilisée dans un plan doit avoir une fiche actualisable. Le registre évite de redécouvrir la même source et rend visible ce qui a été vérifié, ce qui n’est qu’une hypothèse et quand les conditions ont changé.

| Champ du registre | Ce qu’il faut relever |
| --- | --- |
| Identité et responsabilité | Producteur, domaine, page officielle et personne ou service responsable lorsqu’identifiable. |
| Accès et format | API, RSS/Atom, ICS, HTML, recherche ou fournisseur tiers ; authentification et contraintes d’accès. |
| Droits | Licence du jeu de données, conditions du site ou de l’API, attribution requise, limites de redistribution et date de revue. |
| Portée | Sujet, géographie, langue, période et types de changements réellement observables. |
| Fraîcheur | Cadence annoncée par la source, délai réellement observé et éventuels trous de publication. |
| Structure | Identifiants, dates, unités, variantes, liens canoniques et changements de schéma. |
| Fiabilité | Succès et erreurs observés, stabilité, doublons, contradictions et historique d’incidents. |
| Coût et limites | Prix, quotas, volume de données, calcul du coût des vérifications sans nouveauté. |
| Exploitation | Fréquence de contrôle autorisée, cache, limite de débit, mécanisme de reprise et source de secours. |

**Conditions bloquantes proposées** : accès interdit ou non autorisé pour l’usage envisagé ; absence d’un fait permettant de vérifier la condition ; fraîcheur incompatible avec le délai annoncé ; impossibilité de différencier un échec d’une absence de nouveauté ; preuve insuffisante ; coût incompatible avec le plafond du pilote. Une source peut être excellente pour découvrir une piste et insuffisante pour confirmer une alerte.

Une page publiquement visible n’est pas automatiquement libre à aspirer ou republier. Le protocole `robots.txt` permet à un site d’exprimer des règles pour les robots ; il ne constitue pas à lui seul une autorisation d’accès ou de réutilisation. Les conditions du site, de l’API et de la donnée doivent être examinées séparément. [RFC Robots Exclusion Protocol 9309](https://www.rfc-editor.org/rfc/rfc9309.html), [guide data.gouv.fr sur la réutilisation](https://guides.data.gouv.fr/guides/guide-juridique/reutilisateurs-de-donnees/respecter-les-conditions-de-reutilisation).

## Parcours de recherche et de vérification retenu

1. **Comprendre la demande** : construire les critères candidats et demander uniquement les précisions indispensables, conformément au document 04.
2. **Découvrir** : chercher d’abord sources officielles, API et flux publiés par les producteurs pertinents. Une recherche Web ou un service agentique peut élargir la découverte, sans devenir automatiquement l’exécution récurrente.
3. **Qualifier les sources** : vérifier format, droit d’usage, portée, fraîcheur, stabilité, coût et capacité de dédupliquer. Enregistrer le résultat et les inconnues dans le registre.
4. **Établir la référence** : relever l’état initial et vérifier qu’il suffit à détecter un changement dans la portée annoncée. Si ce n’est pas possible, réduire honnêtement la portée ou refuser l’activation.
5. **Proposer** : présenter critères, sources trouvées, cadence soutenable, limites et éventuelle couverture partielle avant accord de la personne.
6. **Exécuter** : interroger seulement les sources du plan, normaliser les candidats, comparer à l’état initial et aux résultats déjà envoyés, puis appliquer les règles du document 05.
7. **Observer la méthode** : mesurer succès, retard, sources silencieuses, changements de schéma et coût ; requalifier le plan si nécessaire.

Ce circuit évite de payer une nouvelle recherche exploratoire complète à chaque vérification quand une source déjà qualifiée suffit. Il n’interdit pas une recherche périodique plus large pour les suivis généraux si les essais montrent qu’elle est nécessaire et économiquement viable. Les appels réseau et IA doivent pouvoir être instrumentés séparément pour comprendre les coûts et les manqués.

### Adresse publique indiquée en secours par l’utilisateur

Si la découverte automatique échoue, le parcours validé au document 04 permet de coller l’adresse d’une page publique. Cette adresse ne saute aucune étape de qualification. Le service vérifie l’URL, refuse les espaces privés ou les authentifications tierces, examine les droits et détermine si la condition recherchée y est réellement observable dans la durée. Une page qui contient seulement une mention générale du sujet ne suffit pas.

Comme cette adresse entre dans un service côté serveur, sa lecture doit être protégée contre l’accès à des ressources internes : schémas réseau autorisés, résolution et redirections contrôlées, adresses privées ou locales bloquées, taille et durée de réponse limitées. Le détail de sécurité est à concevoir au document 08 et à tester au document 09. Une page inaccessible n’est pas contournée par un navigateur connecté au compte personnel de l’utilisateur dans le MVP.

## Coût, mutualisation et remplacement

Le coût d’une surveillance ne se limite pas à l’appel qui trouve une nouveauté. Il inclut la clarification et la découverte initiales, les **vérifications sans résultat**, les réessais, la recherche de remplacement, l’analyse sémantique, le stockage, l’envoi et l’observabilité. Une source gratuite peut demander du calcul, du nettoyage et de la maintenance ; un fournisseur payant peut être raisonnable pour une demande rare si son coût complet est inférieur à une extraction fragile faite maison. Le chiffrage et le plafond du pilote relèvent du document 07.

Lorsque plusieurs surveillances utilisent la même donnée publique, le service peut examiner un téléchargement ou un cache commun afin de réduire la charge et le coût. Les critères privés des personnes restent séparés et ne doivent pas être transmis à d’autres utilisateurs ou au producteur de la source sans raison. Les limites de cache, l’attribution et les droits de redistribution restent attachés à la source. Une mutualisation n’est adoptée qu’après comparaison de son effet réel sur fraîcheur, coût et confidentialité.

Une source, une API de recherche ou un prestataire agentique doit pouvoir être remplacé sans changer la promesse confirmée à l’utilisateur sans réévaluation. Le changement de méthode passe par le même corpus de tests, avec mesure des événements manqués et des fausses alertes. Si le remplacement réduit la couverture ou allonge le délai, l’état du plan et l’interface le signalent avant de continuer comme si rien n’avait changé.

## Protocole de comparaison avant choix fournisseur

Le corpus concret sera constitué **après l'audit du moteur généraliste**, à l'étape 1 du [plan de validation révisé](10-plan-de-validation-technique.md). Les situations COMP-T01 à COMP-T06 du [document 02](02-concurrence-et-differenciation.md) fournissent des dimensions de couverture — événement précis, suivi général, source officielle, doublon, clarification et refus honnête — mais leurs sujets illustratifs ne sont pas des cas présélectionnés. Le corpus ajoutera langues, pays, source inconnue, seuil, source tardive, page fournie, contradiction, panne, absence de date et source supprimée. Aucun exemple n'est une catégorie imposée dans l'application.

Pour chaque demande, fixer **avant le test** la condition attendue, les événements de référence connus ou simulés, le lieu et la période, la preuve acceptable et la cadence visée. Une page de test contrôlée peut provoquer un changement daté ; les événements réels exigent une preuve de leur disponibilité dans la source. Un modèle ne peut pas être seul juge de ses propres résultats. Les évaluations multilingues doivent distinguer compréhension de la demande et couverture effective des sources dans le pays concerné.

| Mesure | Question à résoudre |
| --- | --- |
| Qualification | La méthode accepte-t-elle seulement les demandes qu’elle sait réellement suivre ? |
| Couverture | Quels événements de référence de la portée annoncée sont vus ou manqués ? |
| Précision | Quels résultats ou notifications ne correspondent pas aux critères confirmés ? |
| Délai | Temps entre disponibilité dans la source de référence, détection et envoi ; pas seulement durée d’un appel API. |
| Doublons et corrections | Même fait repris, fait distinct et correction matérielle sont-ils distingués ? |
| Résilience | Source en panne, vide, modifiée ou supprimée : l’état reste-t-il honnête et la reprise correcte ? |
| Coût complet | Prix de la découverte, des exécutions sans nouveauté, des erreurs, des réessais et de la supervision. |
| Droits et données | Usage envisagé permis, attribution correcte et données personnelles minimisées. |

Comparer sur le même corpus les moteurs récurrents **Exa Monitors et Firecrawl Monitor** ; ajouter Yutori sur les demandes où une recherche plus profonde pourrait justifier son coût. Examiner une source ciblée gratuite ou peu coûteuse **quand elle existe réellement pour le cas**. La recherche Web OpenAI ou Brave peut servir de référence technique sans être assimilée à un service de surveillance complet. La [revue documentaire du 3 octobre](../validation/02-alternatives-au-moteur-yutori.md) justifie cet ordre d'essai, mais ne sélectionne aucun fournisseur. Aucun abonnement ou déploiement n’est engagé par ce document. Les seuils de succès et le budget d’essai doivent être fixés avant de conclure, dans les documents 07 et 09.

## Exigences traçables validées à ce stade

| Référence | Exigence |
| --- | --- |
| SRC-R01 | Qualifier une méthode récurrente avant d’annoncer une surveillance active. |
| SRC-R02 | Évaluer systématiquement API, données structurées, flux RSS/Atom, calendriers et pages officielles lorsqu’ils sont pertinents, avant une recherche agentique récurrente coûteuse. |
| SRC-R03 | Conserver un registre des sources avec portée, fraîcheur, droits, coût, état de santé et date de revue. |
| SRC-R04 | Séparer découverte, état initial, exécution récurrente, qualification des candidats et émission. |
| SRC-R05 | Documenter couverture, limites et sources trouvées dans une proposition compréhensible. |
| SRC-R06 | Distinguer absence de nouveauté, source silencieuse, échec, couverture partielle et demande non réalisable. |
| SRC-R07 | Employer l’IA quand elle apporte une valeur mesurée ; ne pas traiter sa réponse comme source ou preuve suffisante. |
| SRC-R08 | Examiner droits d’usage, attribution, accès automatisé et conditions de chaque source avant utilisation durable. |
| SRC-R09 | Requalifier un plan lorsque la source ou le fournisseur change, se dégrade ou disparaît. |
| SRC-R10 | Mesurer qualité, délai et coût complet sur un corpus préétabli avant de choisir un fournisseur ou une cadence. |
| SRC-R11 | Qualifier et sécuriser toute adresse Web fournie par l’utilisateur avant accès serveur et activation. |
| SRC-R12 | Garder les secrets fournisseur côté service et permettre le remplacement d’une méthode sans altération cachée des critères confirmés. |

## Scénarios d’acceptation validés à ce stade

| Référence | Étant donné | Quand | Alors | Exigences |
| --- | --- | --- | --- | --- |
| SRC-A01 | Une API structurée du producteur couvre la condition avec un rythme compatible. | Le plan est construit. | Son usage est évalué avant une recherche Web agentique récurrente ; le choix final documente qualité, droits et coût. | SRC-R02, SRC-R03 |
| SRC-A02 | Un flux RSS annonce une nouvelle page, mais pas le fait exact demandé. | La source est qualifiée. | Le flux peut servir à découvrir la page, sans être déclaré preuve suffisante de la condition. | SRC-R01, SRC-R05 |
| SRC-A03 | Un calendrier donne une date mais aucun statut de billetterie. | La demande porte sur l’ouverture des ventes. | Le calendrier ne suffit pas à rendre cette demande réalisable. | SRC-R01, SRC-R05 |
| SRC-A04 | Une recherche Web trouve une annonce aujourd’hui, sans méthode de contrôle futur identifiée. | La personne veut activer la surveillance. | L’application demande une portée différente ou refuse l’activation opérationnelle. | SRC-R01, SRC-R04 |
| SRC-A05 | Une source ne répond plus pendant plusieurs vérifications. | Le plan est exécuté. | La perte de couverture est visible et une requalification est engagée ; l’absence de réponse n’est pas « rien de nouveau ». | SRC-R06, SRC-R09 |
| SRC-A06 | Une page officielle affiche une nouvelle date après avoir changé uniquement sa mise en page. | Le plan l’observe. | Seul le fait pertinent est candidat à l’alerte ; la mise en page ne suffit pas. | SRC-R04, SRC-R07 |
| SRC-A07 | Une personne fournit une URL pointant vers un réseau local ou y redirigeant. | Le service tente de la qualifier. | L’accès est bloqué avant tout téléchargement de la ressource interne ; aucune surveillance n’est activée. | SRC-R11 |
| SRC-A08 | Une page publique fournie par la personne nécessite en réalité une connexion pour voir la condition. | Elle est évaluée. | La limite est expliquée ; l’application ne présente pas la page comme surveillable dans le MVP. | SRC-R01, SRC-R11 |
| SRC-A09 | Deux surveillances utilisent le même flux public. | Le service examine une mutualisation. | Les données de source peuvent être partagées si les droits le permettent ; les critères privés et résultats des comptes restent isolés. | SRC-R03, SRC-R08 |
| SRC-A10 | Un fournisseur retourne un résultat plausible sans source vérifiable. | Le candidat est qualifié. | Aucun fait n’est notifié comme confirmé sur cette seule base. | SRC-R07 |
| SRC-A11 | Un changement de fournisseur réduit la zone couverte. | Le plan est requalifié. | La couverture réduite est signalée et les critères confirmés ne sont pas modifiés silencieusement. | SRC-R09, SRC-R12 |
| SRC-A12 | Un flux est gratuit mais met les annonces à disposition trop tard pour la cadence affichée. | Les sources sont comparées. | Le flux est écarté pour cette promesse ou la cadence annoncée est revue avant activation. | SRC-R03, SRC-R10 |

## Arbitrages validés et paramètres restant à décider

| Référence | Décision validée | Conséquence |
| --- | --- | --- |
| SRC-D01 | Examiner d’abord les sources ciblées et structurées capables de tenir la promesse ; recourir à la recherche Web ou à un agent tiers lorsque nécessaire, après contrôle de qualité et des droits. | Aucune technique universelle ni fournisseur imposé. |
| SRC-D02 | Refuser ou réduire honnêtement une demande quand une recherche IA trouve des pages mais aucune méthode récurrente fiable. | Une recherche ponctuelle ne suffit pas à afficher une surveillance active. |
| SRC-D03 | Comparer sur les mêmes cas une méthode ciblée et une solution de recherche ou de surveillance tierce, sous réserve d’un plafond d’essai à fixer au document 07. | La validation de la méthode d’essai n’autorise aucune dépense ni abonnement. |

Les candidats définitifs, la cadence, le budget et les seuils de succès du pilote restent à décider dans les documents 07 et 09, après vérification des conditions en vigueur. Aucun agent ne doit transformer cette validation en contrat fournisseur ou achat implicite.
