# Règles des alertes

Validé par Fabien le 2 octobre 2026. Ce document précise les résultats attendus d’une surveillance active, du premier constat à une notification ou à une absence justifiée de notification. Il décline le [périmètre du MVP](03-perimetre-mvp.md) et les [parcours validés](04-parcours-et-ecrans.md). Les arbitrages sur l’événement ponctuel, la reprise après pause volontaire et les heures calmes sont consignés dans la [décision 0005](decisions/0005-regles-des-alertes.md). Les méthodes de recherche et de preuve seront éprouvées dans le document 06, les cadences et plafonds économiques dans le document 07, et les tests de bout en bout dans le document 09. Les paramètres encore ouverts sont identifiés en fin de document.

## Vocabulaire et frontières

| Terme | Sens dans le produit |
| --- | --- |
| Surveillance | Demande et critères confirmés, méthode de vérification exploitable, rythme annoncé et état contrôlable. |
| Vérification | Exécution prévue du plan de surveillance. Elle peut réussir, être partielle, échouer ou ne pas être exécutée à cause d’une pause ou d’une limite. |
| Candidat | Information collectée pouvant correspondre à la surveillance, avant qualification. Un candidat n’est pas une alerte. |
| Événement | Fait ou changement identifié, avec ses attributs utiles : sujet, lieu, date, valeur, statut et sources selon le cas. |
| Résultat | Compte rendu conservé pour la personne lorsqu’une nouveauté pertinente est établie ; il porte ses preuves et dates. |
| Notification | Message envoyé au téléphone pour attirer l’attention sur un résultat ou, séparément, sur un problème opérationnel important. |
| État initial | Informations connues au moment où la surveillance devient active. Il sert de référence et n’est pas une nouveauté créée par l’activation. |

Un résultat peut exister sans notification reçue si la permission Android est refusée, le téléphone est hors ligne ou la livraison échoue. Une vérification réussie sans nouveauté ne crée pas un résultat d’alerte. La conservation des résultats et des diagnostics sera définie dans le document 08 ; un journal exhaustif de chaque vérification n’est pas prévu pour le MVP.

## Décisions héritées des documents validés

| Référence | Règle acquise | Origine |
| --- | --- | --- |
| AL-D01 | Aucune surveillance n’est annoncée active sans méthode exploitable, critères confirmés et portée comprise. | Documents 01, 03 et 04. |
| AL-D02 | Une notification de nouveauté exige une nouveauté pertinente, expliquée et sourcée. Aucun envoi de remplissage sans événement. | Documents 01 et 03. |
| AL-D03 | Échec de vérification et réussite sans nouveauté sont des états distincts ; la dernière réussite ne change pas lors d’un échec. | Documents 01, 03 et 04. |
| AL-D04 | Après prise en compte d’une pause ou suppression, aucune nouvelle notification liée à cette surveillance n’est émise. | Documents 03 et 04. |
| AL-D05 | La consultation hors ligne complète est reportée ; une action non transmise ne doit pas être annoncée comme effective. | Documents 03 et 04. |
| AL-D06 | Les résultats restent accessibles dans l’application même si les notifications sont refusées ; celles reçues ouvrent le résultat. | Documents 03 et 04. |
| AL-D07 | La cadence chiffrée et les quotas ne sont pas décidés. Un rythme réellement assuré doit être visible ; aucune instantanéité n’est promise. | Document 03. |
| AL-D08 | Une surveillance explicitement ponctuelle se termine après le premier résultat confirmé ; sa fin est annoncée avant activation. | Validé lors de la revue du document 05. |
| AL-D09 | Une pause volontaire ne produit pas de rattrapage automatique à la reprise : un nouvel état initial est établi et affiché. | Validé lors de la revue du document 05. |
| AL-D10 | La V1 n’ajoute ni heures calmes réglables ni résumé périodique autonome ; les notifications correspondent à des nouveautés vérifiées. | Validé lors de la revue du document 05. |

Les règles qui suivent complètent ces décisions. Leur validation n’équivaut pas à prouver qu’un fournisseur sait les tenir : la qualité et le coût devront être mesurés.

## États et résultats d’une vérification

Une surveillance conserve séparément son **état de contrôle** (active, en pause, suspendue par une limite, terminée après un événement ponctuel ou supprimée) et sa **santé** (dernière réussite, dernier échec, couverture diminuée). Une panne temporaire ne doit pas la transformer silencieusement en « en pause ». Inversement, une pause décidée par la personne n’est pas une panne.

| Issue d’une exécution prévue | Enregistrement et affichage retenus | Notification de nouveauté |
| --- | --- | --- |
| Réussite, aucun fait nouveau correspondant | Mettre à jour la date de dernière réussite ; état « aucune nouveauté trouvée » avec le périmètre effectivement vérifié. | Aucune. |
| Réussite, nouveauté vérifiée | Mettre à jour la dernière réussite, conserver le résultat sourcé et lier l’exécution au résultat. | Une, sous réserve des permissions et de la déduplication. |
| Réussite partielle | Montrer la portée non vérifiée et la date de dernière réussite complète séparément ; ne pas présenter le silence comme preuve d’absence. | Possible pour un événement prouvé par la partie fiable, avec indication de couverture réduite dans le résultat. |
| Échec | Conserver la dernière réussite ; enregistrer le dernier échec et sa cause exploitable. | Aucune nouveauté inférée de l’échec. |
| Non-exécution pour pause, quota ou budget | Montrer le motif et depuis quand la surveillance ne vérifie plus ; ne pas modifier la dernière réussite. | Aucune nouveauté issue d’une exécution absente. |

Le détail visible n’a pas besoin d’exposer toutes les tentatives, mais il doit donner à la personne assez d’information pour distinguer « vérifié et rien de nouveau », « vérification impossible » et « aucune vérification prévue ». Les notifications de problème opérationnel sont distinctes des notifications de nouveauté : leur seuil et leur fréquence sont des paramètres à fixer après essai, pas une raison d’envoyer une alerte à chaque panne.

## Mise en route et état initial

Avant activation, le service établit l’état connu à partir des sources exploitables. Il associe aux faits la date de publication lorsqu’elle est identifiable, la date de l’événement lorsqu’elle diffère, et la date de première observation par le service. Une date absente reste absente ; le moment de découverte n’est pas présenté comme date de publication.

La première exécution récurrente compare ses candidats à cette référence. Une annonce déjà visible lors de l’activation ne déclenche pas une alerte de « nouveauté ». Une nouvelle page qui répète plus tard la même annonce ne la transforme pas en nouvel événement. Si l’état initial est incomplet au point de ne pas pouvoir distinguer l’ancien du nouveau pour la condition promise, la surveillance ne doit pas être activée comme si elle était fiable ; elle demande une portée plus étroite, une autre source ou annonce sa limite avant confirmation.

Une information publiée avant l’activation mais découverte ensuite ne déclenche pas automatiquement une notification. Une information survenue après l’activation et découverte avec retard peut en déclencher une si son caractère nouveau est étayé, qu’elle correspond encore aux critères et que son utilité n’a pas expiré. Le retard et les dates sont alors visibles. Cette distinction protège aussi les reprises après panne.

## Qualification d’un candidat

Chaque candidat suit une suite logique traçable :

1. **Concordance** : il concerne le bon sujet, lieu, variante, période et type de changement. Les exclusions confirmées s’appliquent également.
2. **Nouveauté** : il diffère de l’état initial et des événements déjà signalés par un fait significatif, pas seulement par une formulation ou une nouvelle URL.
3. **Preuve** : des sources accessibles et adaptées étayent l’affirmation exacte qui serait envoyée. La méthode ne considère pas un résumé généré comme une source.
4. **Confiance opérationnelle** : aucune contradiction matérielle non résolue ni dégradation de couverture ne rend la conclusion trompeuse.
5. **Utilité temporelle** : l’événement n’est pas déjà périmé pour l’usage annoncé, sauf si la condition était précisément de connaître un événement passé.
6. **Autorisation d’envoi** : la surveillance est encore active, sa version de critères est la bonne et le résultat n’a pas déjà été envoyé.

Pour un suivi général, la proposition confirmée doit rendre le mot « important » opératoire : types d’événements inclus, sources ou champ observé, éventuelle zone, exclusions et raisons de la sélection. La seule présence d’un sujet dans un article, ou une appréciation opaque du modèle, ne suffisent pas. Pour un seuil, l’unité, la devise, la variante et la nature du prix ou de la valeur observée sont conservées ; un ancien prix de référence ne devient pas une baisse actuelle.

### Preuve, désaccord et prudence

Une source primaire adaptée peut suffire pour une annonce dont elle est responsable, par exemple un organisateur pour sa propre billetterie. Une source secondaire peut être utile, mais sa simple répétition par plusieurs sites ne crée pas plusieurs confirmations indépendantes. Le niveau de preuve dépend du type d’événement et du risque d’erreur ; le document 06 devra définir et tester les sources de référence pour les familles de cas effectivement prises en charge. Il n’existe ni règle universelle « deux liens suffisent », ni validation par la seule assurance du modèle.

Si des sources crédibles se contredisent sur un fait déterminant, le service ne notifie pas ce fait comme confirmé. Il peut conserver un diagnostic interne ou montrer une incertitude dans un résultat déjà consultable si cela apporte une information honnête, mais pas envoyer une notification formulée comme certaine. Si le désaccord révèle une perte de couverture durable, l’état de la surveillance devient « attention requise ». La priorité retenue pour la V1 est d’éviter les fausses alertes affirmatives tout en mesurant les événements manqués : le silence seul n’est pas une mesure de qualité.

### Identité de l’événement et doublons

Un même événement peut apparaître dans une source officielle, un flux et plusieurs reprises éditoriales. L’identité à rapprocher dépend du cas : sujet et édition d’un événement, identifiant officiel, combinaison produit-variante-prix-source, ou date et lieu d’une annonce. L’URL seule n’est pas une identité fiable, pas plus qu’un texte strictement identique n’est une preuve que deux faits sont identiques.

Le système conserve un lien entre le résultat envoyé et l’événement représenté. Une vérification répétée, un nouveau site qui reprend le fait ou un nouvel essai d’envoi ne doivent pas produire un deuxième message identique. Si deux événements réellement distincts correspondent à la même demande, ils restent séparés. Une règle de regroupement et ses cas limites seront inscrits dans le corpus d’essais du document 06.

### Corrections et changements matériels

Une correction est un changement d’un événement déjà connu, pas un nouvel événement indépendant. Elle mérite une nouvelle notification seulement si elle modifie de façon importante ce que la personne devrait croire ou faire, par exemple une date, un lieu, un statut d’annulation, une disponibilité annoncée à tort ou une valeur franchissant la condition inverse. Une ponctuation changée, une image remplacée ou une reprise sans nouveau fait ne la justifient pas.

Le résultat de correction pointe vers le résultat initial, indique ce qui a changé et les sources associées. Il ne supprime pas silencieusement la première alerte. Une surveillance ponctuelle terminée après son premier résultat ne promet pas de détecter des corrections ultérieures ; ce fait doit être compréhensible avant activation. Les corrections observées pendant une surveillance encore active suivent la même exigence de preuve que les premières alertes.

## Seuils, événements uniques et sujets continus

| Forme de demande | Interprétation retenue | Anti-répétition |
| --- | --- | --- |
| Événement unique explicite : « dès que la billetterie ouvre » | Surveiller l’apparition de la condition et présenter avant activation l’arrêt après le premier résultat confirmé. | Ne pas répéter l’ouverture à chaque vérification ; un suivi continu doit être formulé et confirmé comme une autre intention. |
| Seuil : « quand le prix passe sous 150 € » | Évaluer une valeur comparable et actuelle ; la première valeur connue constitue la référence. | Alerter au franchissement d’un état non satisfait vers satisfait, pas à chaque vérification sous le seuil. Le réarmement après remontée et le risque de fluctuations répétées restent à valider sous AL-O06. |
| Sujet continu : « nouvelles importantes sur… » | Comparer les nouveaux événements aux catégories et exclusions confirmées. | Un envoi par événement pertinent ou correction matérielle, même si plusieurs sources le rapportent. |

Un prix, un stock ou une disponibilité très volatile ne peut pas être promis comme temps réel. L’alerte indique l’heure du constat et, lorsque cela compte, le caractère potentiellement périssable du fait. Une condition déjà vraie à l’activation ne crée pas une première alerte artificielle : l’application explique l’état initial et demande une condition future pertinente.

## Émission et livraison des notifications

Un résultat qualifié est enregistré avant la tentative d’envoi. Avant de demander l’envoi, le service vérifie à nouveau l’état courant de la surveillance, la version des critères, l’identité du résultat et l’autorisation d’émettre. Cette seconde vérification évite qu’une recherche déjà lancée notifie après une pause, une suppression ou une modification prise en compte. Pour une surveillance ponctuelle, l’envoi de son premier résultat est décidé avant la transition à l’état terminé, ou atomiquement avec elle : terminer la surveillance ne doit pas annuler sa seule alerte légitime. L’émission est protégée contre les répétitions dues à des réessais techniques ; une livraison déjà remise au téléphone peut toutefois rester visible après une suppression.

Le texte de la notification annonce un fait court et vérifié, sans masquer une incertitude déterminante. Il ouvre le résultat exact et non une page d’accueil générique. Le résultat conserve les dates de publication et de vérification, les liens et la raison de concordance. Si la permission Android est refusée, la nouveauté reste consultable ; l’absence de push n’est pas présentée comme une absence d’événement. Les tentatives, succès et échecs de livraison doivent pouvoir être diagnostiqués sans exposer des demandes personnelles dans des journaux non nécessaires.

Une vérification sans nouveauté ne déclenche ni notification de « rien à signaler » ni résumé périodique autonome dans le MVP. La V1 n’ajoute pas d’heures calmes réglables ni de regroupement autonome ; les contrôles de notification du téléphone restent utilisables. Si ces fonctions sont ajoutées plus tard, il faudra distinguer heure de détection et heure d’envoi, et vérifier qu’un retard n’annule pas la valeur d’une alerte.

## Pause, modification, panne et reprise

### Pause et suppression demandées par la personne

La pause et la suppression prennent effet après confirmation par le service, conformément au document 04. Aucune nouvelle vérification n’est planifiée pendant une pause, et aucune nouvelle notification n’est émise après son effet, y compris si une recherche avait déjà commencé. Une notification déjà émise peut être livrée ou rester affichée plus tard par le système : le produit ne peut pas en promettre le retrait rétroactif.

La suppression termine la surveillance et suit la politique de données du document 08. La pause conserve ses critères et son dernier état connu. À la reprise d’une pause volontaire, le service établit un nouvel état initial et montre ce qui est alors connu, sans promettre un récapitulatif exhaustif. Il ne notifie pas automatiquement les événements de la période de pause. La personne doit comprendre ce comportement avant ou pendant la reprise ; une panne, qu’elle n’a pas choisie, suit une règle distincte ci-dessous.

### Modification des critères

Une modification produit une proposition à confirmer, comme lors de la création. Jusqu’à son acceptation effective, l’ancienne version reste la seule applicable. Une recherche en cours est évaluée contre la version toujours active ; avant l’envoi, la version est revérifiée. Une fois la modification prise en compte, le service établit une nouvelle référence pour les critères modifiés afin de ne pas transformer l’historique connu en avalanche de nouveautés. Les résultats antérieurs restent lisibles avec les critères et dates qui les expliquent, selon la conservation retenue.

### Panne ou perte de couverture

Une panne n’est pas une pause choisie. Le service conserve la dernière réussite, enregistre le dernier échec et tente la reprise selon une politique bornée de réessais qui évite une dépense illimitée. Après rétablissement, il compare les événements à la dernière référence fiable. Un fait apparu pendant la panne peut être signalé avec retard s’il est prouvé, toujours pertinent et encore utile ; le retard apparaît dans le résultat. Un événement déjà périmé n’est pas envoyé comme une alerte urgente. Si le service ne peut pas reconstruire ce qui s’est passé, il rend cette lacune visible au lieu d’affirmer qu’il n’y a rien eu.

Le seuil de dégradation visible dans la liste peut être immédiat après un échec ; le seuil d’un message de problème supplémentaire doit être défini selon la cadence promise et la gravité, avec une limitation des répétitions. Aucun nombre arbitraire d’échecs ou de minutes n’est validé ici. Le document 09 devra vérifier que le seuil choisi détecte une interruption réelle sans noyer la personne de messages techniques.

### Quota ou budget atteint

Une suspension pour limite d’usage est distincte d’une panne et d’une pause volontaire. Le service indique quand les vérifications cessent et comment elles pourront reprendre selon l’offre du document 07. La prochaine vérification n’est pas affichée comme prévue si elle ne l’est plus. Aucun résultat nouveau n’est inventé pendant l’interruption. Le rattrapage éventuel devra être compatible avec les mêmes règles de nouveauté, de preuve et de fraîcheur ; la limite économique ne peut pas rester invisible pour une surveillance annoncée active.

## Dates, conservation et traçabilité nécessaires

Pour pouvoir expliquer et tester une alerte, le système doit distinguer au minimum : création et activation de la surveillance ; version de critères ; date de publication de la source si connue ; date de l’événement si différente ; première observation par le service ; date de vérification réussie ou échouée ; date de qualification du résultat ; date de demande d’envoi et état de livraison si disponible. Le fuseau horaire et l’incertitude des dates doivent être représentables. Une date absente ne doit pas être comblée avec l’heure du serveur.

Ce besoin de traçabilité ne commande pas de stocker indéfiniment des pages entières, des demandes brutes ou tous les passages sans nouveauté. Le modèle de données, les durées, la confidentialité et la suppression sont décidés au document 08. Le corpus d’essais du document 06 doit fournir des exemples où les dates et les sources se contredisent, ainsi que des événements jamais publiés avec une date fiable.

## Exigences traçables validées à ce stade

| Référence | Exigence |
| --- | --- |
| AL-R01 | Établir un état initial avant l’activation et ne pas notifier cet état comme nouveauté. |
| AL-R02 | Séparer état de contrôle, santé de la surveillance et résultat de chaque vérification. |
| AL-R03 | Distinguer réussite sans nouveauté, réussite partielle, échec et non-exécution. |
| AL-R04 | Qualifier concordance, nouveauté, preuve, contradiction et utilité temporelle avant un envoi. |
| AL-R05 | Expliquer la pertinence et les sources de chaque résultat sans attribuer une date inconnue. |
| AL-R06 | Identifier les événements au-delà de l’URL et empêcher les envois répétés du même fait. |
| AL-R07 | Distinguer une correction matérielle d’une reprise ou modification cosmétique. |
| AL-R08 | Pour un seuil, alerter sur un franchissement pertinent, sans répéter chaque vérification satisfaisante. |
| AL-R09 | Vérifier état et version de la surveillance immédiatement avant toute émission. |
| AL-R10 | Conserver un résultat consultable même si la notification n’est pas livrée. |
| AL-R11 | Appliquer la pause, la suppression et la modification seulement après confirmation du service ; respecter leur effet sur les exécutions en cours. |
| AL-R12 | Après panne, reconnaître les événements encore utiles et rendre visibles les lacunes non reconstituables. |
| AL-R13 | Signaler clairement les suspensions de quota ou budget et leur effet sur les vérifications. |
| AL-R14 | Montrer un rythme de vérification assuré sans promettre l’instantanéité ; séparer détection et livraison. |
| AL-R15 | Rendre les décisions de qualification et d’envoi vérifiables dans un corpus d’essais, en limitant les données retenues. |
| AL-R16 | Terminer une surveillance explicitement ponctuelle après son premier résultat confirmé et rendre cette règle visible avant activation. |
| AL-R17 | Repartir d’un nouvel état initial après une pause volontaire, sans rattrapage automatique des événements de la pause. |

## Scénarios d’acceptation validés à ce stade

Les données et les sources sont contrôlées dans les tests du document 09. Ces scénarios définissent le résultat à observer ; les valeurs chiffrées de qualité, délai et coût seront fixées avant la décision de publication. Les essais de réarmement liés à AL-O06 devront être complétés après mesure.

| Référence | Étant donné | Quand | Alors | Exigences |
| --- | --- | --- | --- | --- |
| AL-A01 | Une annonce figure déjà dans l’état initial. | La première vérification la retrouve. | Aucune alerte de nouveauté n’est créée. | AL-R01 |
| AL-A02 | Une vérification aboutit sans fait correspondant. | Elle se termine. | La dernière réussite avance ; aucun résultat ni push de nouveauté n’est produit. | AL-R02, AL-R03 |
| AL-A03 | Une source indispensable échoue. | La vérification se termine. | L’état est échec ou partiel selon la couverture réelle ; la dernière réussite complète ne devient pas cette date. | AL-R02, AL-R03 |
| AL-A04 | Une nouveauté après activation correspond aux critères et est correctement étayée. | La vérification aboutit. | Un résultat explique le fait, la concordance, les sources et les dates ; une notification est demandée si autorisée. | AL-R04, AL-R05 |
| AL-A05 | Une deuxième page reprend un événement déjà signalé. | Elle est découverte. | Elle peut enrichir les sources du résultat, mais ne crée pas un second envoi du même événement. | AL-R06 |
| AL-A06 | Deux éditions distinctes d’un même festival sont annoncées. | Leurs faits sont vérifiés. | Elles peuvent produire deux résultats distincts si les critères couvrent les deux éditions. | AL-R06 |
| AL-A07 | Une date importante d’un résultat déjà notifié est corrigée par une source adaptée. | La surveillance est encore active. | Une correction liée au résultat initial peut être envoyée ; une retouche sans effet matériel ne l’est pas. | AL-R07 |
| AL-A08 | Un prix vérifiable reste sous le seuil après un premier franchissement notifié. | Plusieurs vérifications identiques aboutissent. | Aucun message identique n’est répété pour chaque vérification. | AL-R08 |
| AL-A09 | Deux sources crédibles se contredisent sur la date qui conditionne l’alerte. | Le candidat est qualifié. | Aucune notification formulant cette date comme certaine n’est envoyée ; l’incertitude est conservée. | AL-R04 |
| AL-A10 | La personne met en pause une surveillance pendant une recherche. | La pause est confirmée avant l’émission. | Aucune nouvelle notification liée à cette recherche n’est émise après cette confirmation. | AL-R09, AL-R11 |
| AL-A11 | La personne modifie les critères pendant une recherche. | La nouvelle version est confirmée avant l’émission. | Le résultat est réévalué selon la version applicable ; il n’est pas envoyé sur la seule base d’anciens critères. | AL-R09, AL-R11 |
| AL-A12 | La permission de notification est refusée. | Une nouveauté est qualifiée. | Le résultat reste consultable et l’interface signale l’absence de push. | AL-R10 |
| AL-A13 | La source est indisponible puis revient avec une information apparue pendant la panne. | La vérification reprend. | Si l’information est prouvée et encore utile, l’alerte peut partir avec son retard visible ; sinon, aucun message trompeusement urgent n’est envoyé. | AL-R12 |
| AL-A14 | Une limite d’usage interrompt une surveillance. | Une vérification devait avoir lieu. | Elle n’est pas comptée comme réussie sans nouveauté ; la suspension et la reprise possible sont visibles. | AL-R13 |
| AL-A15 | Un événement est daté sans date de publication fiable. | Un résultat est créé. | La date de publication est indiquée comme inconnue ; la date de vérification ne la remplace pas. | AL-R05 |
| AL-A16 | La tentative d’envoi est répétée après une réponse réseau incertaine. | Le service réessaie. | Un seul envoi logique est associé à l’événement et à la version de surveillance. | AL-R06, AL-R09 |
| AL-A17 | Une page publique fournie en secours est qualifiée. | Elle n’est pas accessible durablement ou ne prouve pas la condition. | Aucune surveillance active fondée sur cette page n’est annoncée. | AL-R04 |
| AL-A18 | La personne confirme une surveillance explicitement ponctuelle. | Le premier résultat pertinent est enregistré. | Son envoi est autorisé si les notifications le permettent, puis la surveillance passe à l’état terminé sans nouvelle vérification ; la fin était annoncée avant activation. | AL-R09, AL-R16 |
| AL-A19 | Une surveillance est en pause volontaire et un événement survient. | La personne la reprend. | Un nouvel état initial est montré ; l’événement de la pause ne déclenche pas de notification de rattrapage. | AL-R17 |

## Arbitrages validés et paramètres à mesurer

Fabien valide AL-D08 à AL-D10 : fin après un résultat pour les demandes explicitement ponctuelles, absence de rattrapage automatique après une pause volontaire, et report des heures calmes et résumés périodiques hors de la V1. Les paramètres suivants restent ouverts :

| Référence | Question | Recommandation pour la revue | Conséquence |
| --- | --- | --- | --- |
| AL-O04 | Après combien d’échecs ou de retard faut-il prévenir d’une panne de surveillance ? | Définir un seuil relatif à la cadence annoncée, tester sa pertinence et limiter les rappels ; aucun chiffre avant essai. | Compromis entre visibilité des pannes et bruit technique. |
| AL-O05 | Quel rythme de vérification et quelles limites de coût par surveillance ? | Mesurer la qualité et le coût selon les sources, puis afficher une cadence réellement tenue. | À trancher avec les documents 06 et 07 ; aucune fréquence n’est déduite de ce texte. |
| AL-O06 | Quand une condition à seuil peut-elle déclencher une deuxième alerte ? | Réarmer après l’observation fiable d’un retour de l’autre côté du seuil ; tester la répétition sur valeurs fluctuantes avant d’engager la règle. | Peut produire des alertes utiles lors de nouveaux franchissements ou, sans garde-fou, trop de bruit. |

Les trois questions restantes demandent des essais de sources, de délai, de coût et d’usage. Un agent de développement ne doit pas traiter ces pistes comme des décisions validées.
