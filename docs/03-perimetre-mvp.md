# Périmètre du MVP

Ce document définit le périmètre validé pour la première version Android publiable de Préviens-moi. Son objectif est de rendre explicite ce que l’utilisateur pourra faire, quelles demandes le service acceptera et dans quels cas il devra refuser ou réduire sa promesse. Les règles fines de chaque écran, de la détection et des sources seront précisées dans les documents 04 à 06.

Validé par Fabien le 2 octobre 2026, y compris les arbitrages sur l’anglais, le compte et le hors-ligne. Les paramètres toujours marqués « ouverts » ne doivent pas être décidés implicitement pendant le développement. Aucun fournisseur, montant, quota ou cadence chiffrée n’est choisi ici.

## Revue du 20 octobre 2026 — livrer Android sans promettre une couverture universelle

**Priorité confirmée par Fabien : sortir rapidement une application Android utile pour sa remise à niveau et ses entretiens, avec une dépense maîtrisée.** La rentabilité et la volonté de payer restent des hypothèses. Nous conservons la V1 gratuite bornée ; aucun abonnement utilisateur, publicité ou paiement n'est à développer maintenant. Cette revue ne prétend pas que le moteur est prêt pour la production.

### Promesse et surface à construire

« Décris ce que tu veux suivre. Nous t'indiquons ce que nous pouvons rechercher, puis nous te prévenons lorsqu'une nouvelle information vérifiée correspond à ta demande. »

L'accueil conserve **« De quoi veux-tu être prévenu ? »**. Pas de questionnaire de profil, pas de catalogue obligatoire, pas d'obligation générale de fournir une URL. Événements précis et suivis généraux restent possibles, en français et en anglais sous les conditions déjà validées.

La première tranche Android contient : saisie et clarifications, proposition à confirmer, identification à l'activation, liste des suivis, détail avec état et résultats sourcés, pause/reprise/modification/suppression, permission de notification et ouverture du bon résultat. Aucun abonnement, carte, widget, graphique ou moteur de recherche propriétaire ne s'ajoute à cette tranche.

### Ce que nous acceptons, proposons ou refusons

| Situation | Réponse attendue |
| --- | --- |
| Demande publique avec critères explicites et méthode qualifiée | Présenter portée, cadence et limites, puis activer seulement après confirmation et réservation de capacité. |
| Aucun résultat au premier passage, mais recherche récurrente adaptée | Proposer de continuer la recherche ; ne pas assimiler absence de résultat et impossibilité. Si l'utilisateur confirme, le suivi peut être actif sans résultat. |
| De nouvelles sources apparaissent plus tard | Les évaluer dans la portée convenue. La liste initiale n'est pas figée ; ne pas étendre silencieusement les critères. |
| Sujet général ou « nouvelles importantes » | Clarifier les faits qui intéressent la personne ; elle confirme la reformulation. |
| Seulement une partie est vérifiable | Expliquer la partie couverte et demander confirmation. |
| Méthode non qualifiée, besoin privé inaccessible, délai impératif non tenable | Refuser l'activation avec une raison précise ; ne pas inventer un suivi en attente. |
| Capacité financière épuisée | Afficher une limite de capacité, distincte d'une impossibilité technique. |

**La restriction à une seule source fixe n'est pas retenue.** La recherche périodique peut découvrir de nouveaux sites ; la surveillance de page connue constitue une autre méthode. Les prix, stocks, météo ou autres familles ne sont ni garantis globalement ni supprimés en bloc : une capacité non qualifiée ne doit pas être activée pour ce besoin. Aucun nouveau connecteur spécialisé n'est imposé avant la première tranche Android.

### Ce que les essais permettent réellement d'affirmer

F1–F3 étayent la surveillance d'une page connue et la transmission/journalisation dans les conditions du laboratoire. F0 montre une découverte possible, mais aussi des erreurs de récence. La compréhension libre, la nouveauté sémantique entre plusieurs sites et la découverte fiable dans la durée ne sont pas validées. Un score de confiance ne lève pas ces limites.

Nous pouvons développer immédiatement les parcours Android sur des données **explicitement simulées**, sans annoncer qu'un suivi distant tourne. La version publiable nécessite toujours une méthode réelle qualifiée, l'admission fiable, le service sécurisé, le push et les contrôles du document 09. Si la recherche large échoue encore au test ciblé d'intégration, Fabien arbitre une réduction explicite ou le report ; nous ne remplaçons pas discrètement le produit par un lecteur de pages.

### Paramètres proposés pour le pilote, pas encore validés

Pour chiffrer un démarrage : **un suivi actif par compte, une vérification quotidienne, capacité globale conservatrice de deux suivis de recherche Web** sur l'offre gratuite étudiée. Ce sont des propositions issues du scénario coûteux du [document 07](07-couts-et-modele-economique.md), pas une prévision d'usage ni des constantes de production. Deux places seraient insuffisantes pour une diffusion large ; l'augmentation attend une capacité financée ou des coûts mesurés plus bas. Une cadence quotidienne n'est pas adaptée à une promesse d'alerte urgente.

L'application reste accessible sans invitation ; la capacité limite les activations, pas le téléchargement. Quotas et cadence sont configurables côté service, affichés avant confirmation, et leur valeur sera arbitrée avant publication. Ces arbitrages n'empêchent pas de coder les états et parcours.

### Acceptation complémentaire de cette revue

| Cas | Attendu |
| --- | --- |
| REV-A01 — demande exploitable sans résultat initial | Une recherche récurrente peut être proposée ; aucun faux résultat et aucun refus motivé par la seule absence de résultat. |
| REV-A02 — nouveau domaine pertinent lors d'un contrôle ultérieur | Candidat évalué selon les critères existants, avec source et preuve ; aucune dépendance à une liste figée de domaines. |
| REV-A03 — découverte d'une vieille publication | Pas d'alerte de nouveauté fondée uniquement sur la date de découverte ou de mise à jour. |
| REV-A04 — zéro résultat / échec / quota | Trois états distincts ; seule une vérification réussie actualise la dernière réussite. |
| REV-A05 — activation simultanée au dernier emplacement | Une seule réservation réussit ; pas de dépassement par concurrence. |
| REV-A06 — version de démonstration | Données simulées signalées ; pas d'appel fournisseur, pas de prétendue surveillance réelle. |
| REV-A07 — plusieurs URL pour le même fait | Pas de notifications multiples pour le même fait ; sinon capacité de déduplication déclarée non validée. |

La réservation budgétaire et l'arrêt distant sont détaillés dans le document 07. Les cas REV complètent les cas MVP existants ; ils ne remplacent pas les exigences de publication.

## Objectif vérifiable de la première version

Une personne doit pouvoir décrire un événement attendu ou un sujet général, confirmer ce que l’application surveillera réellement, puis retrouver ses surveillances et recevoir une notification seulement lorsqu’une nouveauté vérifiée correspond à ses critères. Elle doit pouvoir reconnaître une panne ou une limite et garder le contrôle de ses surveillances.

Le MVP est une première version utile et publiable, pas une démonstration d’écran ou une promesse de couverture universelle. Il n’impose pas de catalogue de catégories. Une demande est évaluée selon sa faisabilité réelle avec les sources et capacités disponibles.

Le succès de la première version doit être évalué sur des surveillances qui tournent effectivement dans la durée. Un parcours de création réussi ou une recherche ponctuelle ne suffisent pas.

## Décisions héritées des documents validés

| Référence | Décision déjà validée | Conséquence pour le périmètre |
| --- | --- | --- |
| MVP-D01 | Android d’abord ; application dédiée aux alertes. | L’interface utilisateur initiale est Android, avec un service capable de continuer les vérifications en son absence. |
| MVP-D02 | La demande libre est l’entrée principale. | Aucun profil professionnel obligatoire ni choix imposé de plusieurs centres d’intérêt. |
| MVP-D03 | La France est prioritaire pour le lancement. | L'expérience française sera soignée ; les essais de capacité du moteur couvrent également des demandes internationales, sans liste de pays autorisés. |
| MVP-D04 | Accepter événements précis et suivis généraux. | Les deux formes sont représentées dans les essais et dans les parcours ; un sujet général peut nécessiter des critères complémentaires. |
| MVP-D05 | Prévoir français et anglais dès la conception. | Le lancement bilingue est retenu sous réserve de vérifier intégralement les deux langues ; la condition de sortie figure ci-dessous. |
| MVP-D06 | Favoriser la création correcte, la clarté et la pertinence. | Une alerte aux critères incompréhensibles ou au fonctionnement opaque ne répond pas au positionnement. |
| MVP-D07 | Préférer une méthode adaptée et économiquement soutenable. | Les sources gratuites ou peu coûteuses sont évaluées avant un appel IA systématique ; aucune technique précise n’est imposée à tous les sujets. |

Les références ci-dessus renvoient au [positionnement](01-produit-et-positionnement.md) et à la [stratégie de différenciation](02-concurrence-et-differenciation.md). Les arbitrages propres au MVP sont consignés dans la [décision 0003](decisions/0003-perimetre-mvp.md).

## Types de demandes retenus pour le MVP

Une demande peut entrer dans le MVP si elle décrit une information publique à rechercher dans la durée, si les critères de pertinence peuvent être explicités et si une méthode de vérification exploitable est identifiée. La disponibilité d’une source reste nécessaire ; la simple capacité d’un modèle à produire une réponse convaincante ne suffit pas.

| Forme | Exemple illustratif | Ce qu’il faut expliciter avant activation |
| --- | --- | --- |
| Événement attendu | « Préviens-moi quand les billets pour cet artiste à Lyon sont en vente. » | Artiste et événement, ville ou zone, sens exact de « en vente », éventuelle période. |
| Seuil ou changement observable | « Préviens-moi si le prix public de ce produit passe sous 150 €. » | Produit ou variante, source et type de prix, devise, conditions publiquement vérifiables. |
| Suivi général | « Préviens-moi des nouvelles importantes sur le cinéma japonais. » | Types de nouvelles souhaitées, éventuel intérêt pour les sorties en France, exclusions et définition compréhensible d’« important ». |

Ces exemples constituent un jeu de départ pour le cadrage et les tests. Ils ne garantissent pas qu’un site donné ou toute condition de prix sera pris en charge. L’application ne doit pas obliger la personne à choisir une de ces trois formes : elle traite sa demande libre et présente ensuite une proposition de surveillance.

La première version peut accepter d’autres sujets si la même capacité de vérification est démontrée. Elle doit aussi pouvoir dire qu’une demande particulière n’est pas prise en charge, même si son thème ressemble à un exemple réussi. Le détail du processus de qualification relève du document 06.

## Chaîne fonctionnelle retenue

| Référence | Capacité du MVP | Résultat attendu pour la personne |
| --- | --- | --- |
| MVP-R01 | Saisir librement une demande | Démarrer à partir de son besoin, en français ou en anglais selon la langue livrée. |
| MVP-R02 | Clarifier seulement les informations manquantes ou contradictoires | Conserver les contraintes déjà fournies ; permettre de répondre et de corriger. |
| MVP-R03 | Rechercher une méthode de vérification et établir l’état initial | Distinguer ce qui est déjà connu de ce qui devra être détecté plus tard. |
| MVP-R04 | Présenter une proposition avant activation | Voir les critères, le périmètre, les sources trouvées, la cadence et les limites ; pouvoir modifier ou abandonner. |
| MVP-R05 | Enregistrer une surveillance active et l’exécuter hors de l’application ouverte | Les vérifications prévues ne dépendent pas de la présence de l’écran au premier plan. |
| MVP-R06 | Produire un résultat sourcé en cas de nouveauté pertinente | Comprendre ce qui a changé et pourquoi cela répond aux critères. |
| MVP-R07 | Envoyer une notification lorsqu’elle est autorisée | Ouvrir le résultat pertinent depuis la notification ; aucune notification de nouveauté si rien de pertinent n’a été trouvé. |
| MVP-R08 | Consulter les surveillances et leur état | Voir au minimum le titre, l’état, la dernière vérification réussie et la prochaine vérification prévue lorsque connue. |
| MVP-R09 | Consulter les résultats et le dernier état de vérification | Retrouver les alertes émises avec leurs sources et comprendre la dernière réussite ou le dernier échec. Un journal exhaustif de toutes les vérifications n’est pas requis. |
| MVP-R10 | Modifier, suspendre, reprendre et supprimer une surveillance | Garder la maîtrise de sa demande et empêcher de nouveaux envois après suppression ou pause prise en compte. |
| MVP-R11 | Traiter les demandes et vérifications impossibles ou dégradées | Donner une raison compréhensible et une action possible ; ne pas annoncer une surveillance opérationnelle à tort. |
| MVP-R12 | Appliquer les limites d’usage et rendre leur effet visible | Comprendre ce qui peut être activé, quand les vérifications sont suspendues et comment elles reprennent. |

Le compte est requis à la première activation durable ; son mode d’identification reste à choisir. Les quotas, les délais et le détail de la conservation sont des paramètres ouverts. Leur présence dans la chaîne fonctionnelle impose une décision avant mise en production ; elle ne choisit pas leur valeur. La consultation hors ligne complète est reportée : lorsqu’il n’y a pas de réseau, l’application doit au minimum éviter de présenter des données anciennes comme actuelles et conserver une saisie en cours si c’est possible.

## Surface utilisateur minimale retenue

Le découpage des écrans peut évoluer, mais les besoins suivants doivent être servis :

| Espace ou étape | Informations et actions indispensables |
| --- | --- |
| Entrée | Question principale, champ libre, exemples facultatifs, conservation du texte en cas d’échec ou de retour. |
| Clarification | Une ou plusieurs questions utiles, réponses rapides ou libres, possibilité de corriger la demande. |
| Proposition de surveillance | Critères compréhensibles, couverture, cadence, sources trouvées, limites, état initial, modification et activation. |
| Liste des surveillances | Identité et état de chaque surveillance, dernière réussite, prochaine vérification si disponible, accès au détail. |
| Détail | Critères, résultats sourcés, dernier état de vérification, erreurs, modification, pause, reprise et suppression. |
| Résultat d’alerte | Événement, raisons du déclenchement, sources, dates et retour à la surveillance. |
| Réglages nécessaires | Langue si applicable, état des notifications, accès aux informations du compte et à la suppression des données selon le modèle retenu. |

La navigation exacte, les états chargement, vide et erreur, la taille des textes, l’accessibilité et les messages sont spécifiés dans le document 04. Une carte, un fil d’actualité général, des favoris et un widget ne sont pas nécessaires à cette chaîne.

## Comportement attendu quand la promesse ne tient pas

Une surveillance n’est active que si une méthode exploitable existe et si sa portée a été confirmée. Les situations suivantes doivent être distinguées dans la première version :

| Situation | Issue dans le MVP |
| --- | --- |
| Critère indispensable manquant | Demander une précision, puis réévaluer. |
| Demande seulement partiellement réalisable | Montrer ce qui peut être surveillé et demander l’accord avant de réduire la portée. |
| Aucune méthode exploitable disponible | Expliquer la limite et ne pas activer une surveillance présentée comme fonctionnelle. |
| Information déjà connue au départ | Montrer l’état initial sans créer artificiellement une nouveauté ultérieure. |
| Vérification terminée sans nouveauté | Enregistrer une réussite sans notifier un événement inexistant. |
| Échec de source ou de fournisseur | Enregistrer l’échec, conserver la date de dernière réussite et rendre la dégradation visible. |
| Quota ou budget applicable atteint | Suspendre les vérifications concernées et expliquer l’effet sur les alertes. |
| Notifications Android refusées | Laisser consulter les résultats dans l’application et expliquer que les notifications sont désactivées. |
| Appareil hors ligne | Signaler l’impossibilité de consulter des données fraîches ou d’envoyer une modification ; ne pas promettre qu’une action non transmise a pris effet. |

Le seuil à partir duquel une panne entraîne un message supplémentaire, la politique de reprise et les priorités entre fausses alertes et événements manqués seront précisés dans le document 05. Le MVP ne doit pas masquer la différence entre échec et absence de nouveauté.

## Conditions de lancement retenues

| Sujet | Proposition pour la première version | Statut et justification |
| --- | --- | --- |
| Plateforme | Android sur Play Store après une période de tests adaptée au compte développeur. | Plateforme validée ; étapes exactes de publication à vérifier dans le document 09. |
| Géographie | Marché initial français ; demande libre concernant tout lieu, avec activation selon la capacité effectivement vérifiée et les sources accessibles. | Le pays de lancement ne définit pas une limite géographique du moteur ; décision initiale précisée dans le document 10. |
| Langue | Français et anglais dès la première publication si les parcours, notifications, erreurs et réponses générées sont vérifiés dans les deux langues ; sinon, lancement en français avec l’anglais prêt dans la conception. | Arbitrage validé ; la vérification complète conditionne l’annonce de l’anglais au lancement. |
| Compte | Laisser formuler et prévisualiser une surveillance sans inscription ; demander un compte pour sa première activation durable. | Arbitrage validé ; récupération et effacement à détailler. |
| Fréquence | Afficher une cadence effectivement assurée pour chaque surveillance ; ne pas promettre de temps réel. | Valeur, personnalisation et éventuel réglage utilisateur à décider après essais de qualité et de coût. |
| Offre | Rendre les limites visibles avant activation. | Gratuité, essai, nombre de surveillances et prix restent ouverts dans le document 07. |
| Notifications | Demander l’autorisation lorsque l’utilisateur comprend pourquoi il en a besoin ; laisser un accès aux résultats en cas de refus. | Parcours proposé ; comportement précis à détailler dans le document 04. |
| Compte et données | Pouvoir supprimer les surveillances et les données personnelles selon la solution choisie. | Exigence de conception ; durée de conservation et procédure à définir dans le document 08. |

Un tableau d’exemples de demandes prises en charge sera construit à partir d’essais. Il ne sera pas déduit de la capacité générale d’un modèle ou d’une liste commerciale de cas d’usage.

## Hors du MVP retenu

| Capacité | Raison de la reporter | Condition pour y revenir |
| --- | --- | --- |
| Contenus derrière un compte tiers, captchas ou espaces privés | Accès, sécurité et maintenance à étudier séparément. | Consentement et technique fiables, vérification des droits d’usage. |
| Achat, réservation ou toute autre action transactionnelle | Nécessite des autorisations et des contrôles différents d’une simple alerte. | Nouvelle décision produit et parcours dédié. |
| Garantie de surveillance exhaustive du Web | Aucune source ne couvre tout et l’observation des manqués est difficile. | Ne pas la promettre ; préférer une couverture démontrée pour des besoins précis. |
| Alertes garanties instantanées ou à délai très court | Coût et fiabilité non établis ; rythme variable selon la source. | Mesure du délai et budget soutenable sur les demandes concernées. |
| Alertes critiques pour la sécurité des personnes | Une erreur ou un retard aurait des conséquences disproportionnées. | Étude et niveau de garantie distincts. |
| Suivi financier ou de disponibilité très volatile comme promesse de temps réel | Le contenu peut devenir périmé entre deux vérifications. | Source adaptée et délai validé ; information de fraîcheur visible. |
| Fil éditorial général, réseau social de surveillances, publicité, affiliation | Aucun de ces éléments n’est nécessaire à la première chaîne complète. | Preuve d’utilité et nouvelle décision de produit. |
| Carte, widget, assistant conversationnel permanent, navigation automatisée généralisée | Augmentent la surface et la maintenance sans établir la qualité des alertes. | Besoin utilisateur et coût justifiés. |
| Consultation hors ligne complète et journal exhaustif des vérifications | Ajoutent stockage, synchronisation et règles de conservation ; la valeur principale repose sur la détection fiable et l’alerte. | Demande observée et parcours de synchronisation spécifié. |
| Modèle IA propriétaire ou moteur de recherche Web construit intégralement en interne | La pertinence des méthodes existantes doit d’abord être mesurée. | Limite démontrée des fournisseurs ou avantage économique crédible. |

Une demande refusée au MVP n’est pas considérée comme un échec de compréhension lorsque sa limite est correctement expliquée. À l’inverse, accepter une demande qui ne peut pas être réellement surveillée est un défaut produit.

## Scénarios d’acceptation du périmètre

Les scénarios suivants forment le contrat de résultat validé à ce stade. Ils seront déclinés en tests d’interface, de logique et de chaîne complète dans le document 09. Les sources réelles, données initiales et délais mesurés doivent être fixés avant l’exécution des essais.

| Référence | Étant donné | Quand | Alors | Exigences |
| --- | --- | --- | --- | --- |
| MVP-A01 | L’application est ouverte pour la première fois. | Une personne exprime un événement précis. | Elle peut poursuivre vers une proposition sans questionnaire de profil. | MVP-R01, R02, R04 |
| MVP-A02 | Une demande de suivi général utilise le mot « important ». | La personne la soumet. | L’application permet de préciser les types de nouveautés pertinentes et présente les critères retenus. | MVP-R01, R02, R04 |
| MVP-A03 | Une source publique permet de constater l’état actuel. | La personne confirme la surveillance. | L’état initial est conservé ; la première vérification répétée ne lui présente pas cet état comme une nouveauté. | MVP-R03, R05 |
| MVP-A04 | Une nouveauté vérifiée correspond aux critères. | Une vérification aboutit. | Un résultat sourcé est créé et une notification est émise si elle est autorisée. | MVP-R06, R07 |
| MVP-A05 | La vérification retrouve une information déjà signalée sans changement significatif. | Elle aboutit de nouveau. | Aucun nouvel envoi identique n’est produit. | MVP-R05, R07 |
| MVP-A06 | Une demande nécessite un accès privé non disponible. | La faisabilité est évaluée. | La limite est expliquée ; la surveillance ne devient pas active. | MVP-R03, R11 |
| MVP-A07 | Seule une partie de la demande est vérifiable. | L’application propose une portée réduite. | Elle attend l’accord de la personne avant de l’activer. | MVP-R04, R11 |
| MVP-A08 | Une surveillance possède une dernière réussite connue. | La source échoue. | L’état d’échec est visible et la date de dernière réussite ne change pas. | MVP-R08, R09, R11 |
| MVP-A09 | Une surveillance est active. | La personne la suspend ou la supprime et l’action est prise en compte. | Aucune nouvelle notification n’est émise pour cette surveillance après cette prise en compte. | MVP-R10 |
| MVP-A10 | Les notifications sont refusées au niveau Android. | Une nouveauté est détectée. | Le résultat reste accessible dans l’application et l’absence de notification est compréhensible. | MVP-R06, R07 |
| MVP-A11 | Une limite d’usage est atteinte. | Une vérification est prévue. | Son exécution suit la politique retenue ; l’état et la reprise possible sont expliqués. | MVP-R12 |
| MVP-A12 | Le téléphone perd le réseau avant qu’une modification de surveillance soit transmise. | La personne tente de confirmer la modification. | L’application ne lui fait pas croire que la modification est effective ; elle indique l’état réel ou l’impossibilité de le connaître. | MVP-R10, R11 |

Ces scénarios ne fixent pas encore un fournisseur, un nombre d’alertes gratuites, une cadence ou une durée de conservation. Les références documentaires permettent aux futurs lots et agents de développement de retrouver la décision pertinente et les cas à vérifier.

## Conditions de sortie du MVP

La publication suppose les éléments suivants, à mesurer et préciser dans le document 09 :

1. Les parcours Android essentiels et les scénarios d’acceptation validés fonctionnent dans les langues effectivement annoncées.
2. Un jeu de demandes et d’événements de référence couvre les deux formes de suivi, les refus honnêtes, les doublons et les dégradations.
3. La qualité de détection, les fausses alertes, les événements manqués et le délai ont été observés avec les limites de la méthode explicites ; les seuils retenus sont atteints.
4. Les vérifications prévues sont observables et leurs échecs ne sont pas confondus avec une absence de nouveauté.
5. Le coût par utilisateur et le coût total du pilote restent dans le budget décidé, y compris lors de vérifications sans résultat.
6. Les données des comptes sont isolées et supprimables ; la notification refusée, la pause et la suppression se comportent comme annoncé.
7. L’accessibilité, la confidentialité, les textes du Store et les exigences de publication applicables sont vérifiés.

Aucun seuil chiffré de qualité, de délai ou de coût n’est inventé ici. Il faut les fixer avant de conclure que le MVP est prêt. Une démo réussie sur un seul exemple ne remplace pas cette décision.

## Arbitrages validés et questions restantes

| Référence | Décision validée le 2 octobre 2026 | Conséquence |
| --- | --- | --- |
| MVP-D08 | Viser l’anglais dès la première publication, seulement si tous les parcours, notifications, erreurs et réponses générées sont vérifiés ; sinon publier d’abord en français. | Concevoir dès maintenant les deux langues sans annoncer une couverture non testée. |
| MVP-D09 | Autoriser la formulation et la prévisualisation sans compte ; exiger un compte lors de la première activation durable. | Ne pas imposer une inscription avant que la personne puisse comprendre la surveillance proposée. |
| MVP-D10 | Reporter la consultation hors ligne complète. | En l’absence de réseau, ne jamais présenter une modification non transmise comme effective ni des données anciennes comme fraîches. |

Les paramètres suivants restent ouverts :

| Référence | Question ouverte | Piste à étudier | Conséquence du choix |
| --- | --- | --- | --- |
| MVP-O03 | Quelle cadence promettre au pilote ? | Afficher un rythme réellement assuré par surveillance et commencer sans promesse d’instantanéité. | Le choix entre cadence simple ou adaptée dépend des sources, du coût et du besoin ; valeur chiffrée à tester. |
| MVP-O04 | Quelle offre initiale ? | Décider après mesure des coûts ; annoncer les limites avant activation. | Influence la durée des essais utilisateurs, le budget et le risque d’abus. |
| MVP-O05 | Une surveillance d’événement précis s’arrête-t-elle après une première alerte ? | Décider selon le type de condition, avec comportement visible. | Influence les relances, les doublons, le coût et la compréhension utilisateur. |

Les trois questions restantes dépendent des essais de sources et de coûts ou des règles de fin de surveillance ; elles seront détaillées dans les documents 05 à 07. Aucun agent ne doit interpréter une recommandation de ce tableau comme une décision validée.
