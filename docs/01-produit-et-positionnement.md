# Produit et positionnement

Préviens-moi est un projet d’application Android dédiée aux alertes personnelles. Une personne décrit ce qu’elle souhaite surveiller ; l’application l’aide à préciser son besoin, effectue les vérifications prévues et lui signale les nouveautés pertinentes avec leurs sources.

Version validée par Fabien le 2 octobre 2026. Le cadrage, les exigences POS-R et les scénarios POS-A sont approuvés. Les points explicitement ouverts restent à arbitrer dans les documents concernés ; la validation ne signifie pas que les tests ont été exécutés. Ce document fixe le sens du produit ; il ne constitue pas à lui seul un lot de développement exécutable.

## Objectif du projet

Construire un MVP professionnel, maintenable et publiable sur le Play Store, tout en accompagnant la remise à niveau Android de Fabien après son expérience de développeur de 2016 à 2022. Les choix techniques devront pouvoir être expliqués et justifiés. La qualité prime sur le calendrier initial d’un mois.

Le produit a une ambition commerciale à explorer. Son utilité, sa capacité à fidéliser et sa viabilité économique restent à démontrer. La présence de concurrents et leur nombre de téléchargements ne suffisent pas à établir ces éléments.

Le nom Préviens-moi est provisoire ; sa disponibilité commerciale n’est pas validée. Le présent cadrage ne choisit ni fournisseur d’IA, ni architecture, ni tarif public.

## Décisions validées

| Référence | Décision | Portée |
| --- | --- | --- |
| POS-D01 | Android est la première plateforme | Aucun développement iOS ou Web utilisateur n’est engagé par ce cadrage. |
| POS-D02 | L’application est dédiée à la surveillance et aux alertes | L’IA sert ce besoin ; elle n’est pas le produit généraliste proposé à l’utilisateur. |
| POS-D03 | L’entrée principale est « De quoi veux-tu être prévenu ? » | Des questions complémentaires précisent la demande lorsque nécessaire. Aucun questionnaire obligatoire sur le métier ni sélection imposée de plusieurs centres d’intérêt. |
| POS-D04 | La France est prioritaire au lancement | Cette priorité concerne les premiers utilisateurs et l'expérience locale ; la validation de la capacité générale couvre aussi des demandes et sources internationales. |
| POS-D05 | Les événements précis et les suivis généraux sont acceptés dans la vision du MVP | Un suivi général nécessite de définir ce qui est pertinent pour la personne. Cette décision ne garantit pas que toute demande sera techniquement prise en charge. |
| POS-D06 | Le français et l’anglais sont prévus dès la conception | Le document 03 vise l’anglais dès la première publication, sous réserve de vérifier intégralement les deux langues. |
| POS-D07 | Le produit s’améliore de version en version | L’amélioration progressive porte sur la qualité et les capacités. Chaque version annonce les capacités qu’elle possède effectivement. |
| POS-D08 | La documentation comprend des critères et tests d’acceptation | Les attentes sont définies pendant les spécifications ; les tests correspondants accompagnent l’implémentation. |

Les décisions du 2 octobre sur la priorité France, les deux formes de suivi et l’évolution progressive sont consignées dans [la décision initiale de positionnement](decisions/0001-positionnement-initial.md).

## Problème utilisateur à vérifier

Une personne attend une information ou souhaite suivre un sujet dans la durée. Elle doit aujourd’hui se souvenir de vérifier, répéter ses recherches, consulter plusieurs sources ou trier des notifications peu pertinentes. Elle peut perdre du temps ou découvrir trop tard une information utile.

Cette description est une hypothèse produit. Nous devons observer chez les premiers utilisateurs des besoins réels, leur méthode actuelle et les conséquences d’un événement manqué. Nous ne présumons pas que chaque sujet d’intérêt justifie une surveillance ou un abonnement.

Le service attendu est de déléguer ces vérifications tout en gardant la maîtrise de ce qui déclenche une notification. La personne doit pouvoir comprendre ce qui est surveillé et constater si la surveillance fonctionne.

## Public initial et contexte

Le public initial est constitué de personnes utilisant Android, principalement en France, qui souhaitent suivre une information dans le temps sans maîtriser les outils d’IA, les sources de données ou l’automatisation. Le besoin définit la cible ; aucun métier ni niveau technique n’est requis.

Une personne peut arriver avec un événement précis en tête ou avec un sujet encore mal délimité. Les exemples facilitent le démarrage mais ne constituent pas une liste exhaustive de besoins autorisés.

La langue de l’interface, celle des alertes, le lieu concerné et le pays des sources sont des paramètres distincts. Une demande en anglais peut concerner Lyon ; une demande en français peut concerner un événement au Japon. Une information indispensable absente ne doit pas être déduite arbitrairement de la langue du téléphone.

La priorité France guide les exemples de lancement et la qualité de l'expérience locale. L'essai du moteur généraliste couvre aussi des langues, pays et sources hors de France ; il ne demande pas une étude de besoins par pays. La liste des pays de distribution sur le Store sera décidée dans le périmètre et la publication ; elle ne découle pas automatiquement de la langue de l’interface.

## Promesse

« Décris ce que tu souhaites surveiller. Préviens-moi précise ta demande avec toi, effectue les vérifications et te signale les nouveautés qui correspondent à tes critères. »

Cette promesse engage le produit sur la compréhension de la demande, la continuité des vérifications annoncées et la pertinence des résultats. Elle ne signifie pas couverture exhaustive du Web, détection instantanée, compréhension parfaite ou garantie qu’aucun événement ne sera manqué.

L’activation doit correspondre à une capacité opérationnelle : une méthode de vérification exploitable, des critères explicites et un rythme annoncé. Une simple réponse affirmative du modèle ne prouve pas cette capacité. Une réussite initiale ne garantit pas que la source restera disponible ; le produit doit rendre une dégradation visible.

## Deux formes de surveillance

### Attendre un événement précis

La personne souhaite savoir si une condition se réalise. Exemple illustratif : « Préviens-moi quand la billetterie ouvre pour cet artiste à Lyon. » Le déclencheur est l’ouverture de la vente concernée, et non toute actualité de l’artiste.

L’application doit clarifier les éléments nécessaires : identité exacte, lieu, période ou seuil selon le besoin. Le fait qu’une surveillance se termine après la première alerte reste une règle à définir dans le document 05 ; le produit ne doit pas supposer que tout événement est à occurrence unique.

### Suivre les nouveautés sur un sujet

La personne veut connaître des changements pertinents dans un domaine. Exemple illustratif : « Préviens-moi des nouvelles importantes sur le cinéma japonais. » L’application peut préciser les types de nouvelles attendues, tels que les sorties en France, les annonces d’un réalisateur ou certains festivals.

« Important » ne doit pas rester un jugement invisible laissé au modèle. La proposition de surveillance doit expliciter les critères retenus et permettre leur correction. L’utilisateur n’est pas obligé de transformer son besoin en un seul événement ; plusieurs types de nouveautés peuvent constituer un suivi cohérent.

Les notifications sont déclenchées par la pertinence d’une nouveauté. L’ajout d’un résumé périodique indépendant des nouveautés constituerait une autre fonctionnalité, à décider dans le périmètre.

## Définition d’une alerte utile

Une alerte utile correspond aux critères confirmés, apporte une nouveauté ou une correction significative, permet de comprendre pourquoi elle a été envoyée et renvoie à des sources consultables. Son délai doit être compatible avec la surveillance annoncée et l’usage accepté.

Le résultat doit distinguer ce que les sources établissent, ce qui demeure incertain et la date de vérification. Une information ancienne découverte aujourd’hui n’est pas automatiquement un nouvel événement. La reprise d’une même annonce par plusieurs sites n’est pas automatiquement plusieurs nouveautés.

Un résultat initial peut être utile même si l’événement a déjà eu lieu. Il doit alors être présenté comme l’état connu lors de la création, sans simuler une détection ultérieure. Le parcours exact sera défini dans le document 04.

## Transparence et demandes difficiles

Exigence produit : l’application décrit ses capacités réelles, évite les affirmations non étayées et rend visible la différence entre une vérification réussie, une vérification impossible et une absence de nouveauté trouvée.

| Situation | Réponse attendue au niveau produit | Comportement à exclure |
| --- | --- | --- |
| La demande est ambiguë | Poser une question ciblée et préserver les contraintes déjà données. | Inventer un lieu, un budget ou le sens d’un mot subjectif. |
| La capacité nécessaire n’est pas disponible | Expliquer la limite actuelle et proposer une alternative si elle est pertinente. | Afficher une surveillance active que le service ne sait pas exécuter. |
| Aucune source exploitable n’a été trouvée | Décrire ce constat et proposer de préciser la demande ou d’apporter un lien. | Déclarer que l’information n’existe pas ou que personne ne peut la surveiller. |
| Seule une partie de la demande est réalisable | Montrer la portée réduite et demander son accord avant activation. | Retirer silencieusement une contrainte. |
| La vérification subit un échec temporaire | Distinguer l’échec de l’absence de résultat ; montrer la dégradation selon les règles de notification à définir. | Mettre à jour la dernière réussite comme si la vérification avait abouti. |
| Une source devient indisponible après activation | Rendre la limite visible et réévaluer la méthode de surveillance. | Continuer à laisser croire que la couverture précédente est assurée. |

Les motifs précis de non-prise en charge, les règles de reprise et la gestion des demandes hors périmètre seront définis dans les documents 03 à 06. Ce principe de transparence s’applique dès le premier parcours, pas uniquement à une page d’aide.

## Exigences transversales

Les références ci-dessous permettent de relier les objectifs produit aux scénarios et futurs tests. Elles n’ajoutent pas d’engagement non mesuré sur le délai, le coût ou la couverture.

| Référence | Exigence | Conséquence observable |
| --- | --- | --- |
| POS-R01 | Permettre d’exprimer le besoin directement | Le premier parcours ne requiert pas un profil professionnel ni trois centres d’intérêt. |
| POS-R02 | Préserver les contraintes de la demande | La reformulation conserve les conditions, exclusions et corrections de l’utilisateur. |
| POS-R03 | Faire confirmer la portée de la surveillance | La personne peut comprendre et corriger les critères avant activation. |
| POS-R04 | Vérifier la faisabilité avant d’annoncer une surveillance opérationnelle | Une demande sans méthode exploitable reste à clarifier ou non prise en charge. |
| POS-R05 | Justifier l’envoi d’une alerte | Le résultat explique la nouveauté, le lien avec les critères et les sources. |
| POS-R06 | Distinguer silence et panne | Une recherche échouée ne devient pas une vérification réussie sans nouveauté. |
| POS-R07 | Respecter l’attention de l’utilisateur | Un contenu sans nouveauté pertinente ne produit pas une alerte uniquement pour provoquer une ouverture. |
| POS-R08 | Séparer langue et géographie | Changer la langue ne déplace pas implicitement le lieu surveillé. |
| POS-R09 | Maintenir le contrôle lors des évolutions | Une mise à jour ne change pas silencieusement les critères confirmés. |
| POS-R10 | Choisir des méthodes économiquement adaptées | Les solutions gratuites ou peu coûteuses sont étudiées lorsqu’elles sont fiables et utilisables ; un appel IA n’est pas systématique. |

Les arbitrages de priorité entre précision, événements manqués, délai et coût seront définis avec des mesures dans les documents 05 à 09. Aucun pourcentage de fiabilité n’est acquis.

## Scénarios d’acceptation produit

Ces scénarios décrivent des résultats observables. Les données de test, les variantes et les seuils seront précisés avec les spécifications fonctionnelles. Ils ne sont pas encore des tests exécutés.

| Référence | Situation et action | Résultat attendu | Exigences |
| --- | --- | --- | --- |
| POS-A01 | Une personne démarre sa première surveillance. | Elle peut exprimer son besoin sans renseigner son métier ni sélectionner plusieurs intérêts. | POS-R01 |
| POS-A02 | Elle indique Lyon et un budget de 50 euros, puis corrige le lieu en Bordeaux. | La confirmation contient Bordeaux et 50 euros ; Lyon n’est plus un critère actif. | POS-R02, POS-R03 |
| POS-A03 | Elle demande des concerts « près de chez moi » alors qu’aucun lieu n’est connu. | L’application demande le lieu avant de confirmer une surveillance locale. | POS-R02, POS-R08 |
| POS-A04 | Elle demande un suivi général de nouvelles « importantes ». | La proposition expose des critères de pertinence et permet de les préciser ; aucun refus n’est fondé uniquement sur le caractère général du sujet. | POS-R02, POS-R03 |
| POS-A05 | La seule méthode identifiée requiert un accès non pris en charge. | L’application explique la limite et n’affiche pas de surveillance opérationnelle. | POS-R04 |
| POS-A06 | Une demande peut être satisfaite seulement dans une zone plus réduite. | La réduction est explicite et n’est appliquée qu’après accord. | POS-R02, POS-R03, POS-R04 |
| POS-A07 | La recherche répétée retrouve la même annonce sans changement matériel. | Aucun nouvel envoi n’est produit au seul motif d’une autre URL ou d’une reformulation. | POS-R05, POS-R07 |
| POS-A08 | La source ou le moteur échoue lors d’une vérification. | L’échec reste identifiable et la dernière vérification réussie n’est pas remplacée par cet échec. | POS-R06 |
| POS-A09 | L’utilisateur change la langue de français vers anglais. | La surveillance conserve son lieu et ses conditions ; seuls les éléments linguistiques concernés changent. | POS-R08 |
| POS-A10 | Une évolution du moteur est déployée. | Les critères utilisateur restent inchangés ; une modification de portée nécessaire repasse par confirmation. | POS-R03, POS-R09 |

## Mesure de l’utilité et de la fiabilité

Les mesures retenues doivent refléter le service rendu. L’ouverture quotidienne et le nombre de notifications ne sont pas des objectifs de réussite en eux-mêmes.

| Dimension | Mesure à détailler | Limite d’interprétation |
| --- | --- | --- |
| Première utilisation | Sur un essai observé, part des personnes parvenant à confirmer une surveillance sans assistance et difficultés rencontrées. | Distinguer abandon de parcours et demande non prise en charge ; conserver les deux résultats. |
| Compréhension | Capacité de la personne à expliquer avec ses mots ce qui déclenchera son alerte. | Un clic sur « confirmer » ne prouve pas cette compréhension. |
| Pertinence | Nombre d’alertes répondant aux critères parmi les alertes examinées. | Le retour volontaire des utilisateurs peut être biaisé ; le compléter par des cas évalués. |
| Détection | Nombre d’événements attendus détectés parmi un ensemble de référence connu. | Un taux de détection sur tout le Web est inconnaissable sans connaître les événements manqués. |
| Délai | Temps entre la disponibilité de l’information dans une source de référence et la détection puis l’envoi. | La date d’un article ne prouve pas toujours la date réelle de disponibilité ; expliciter la référence. |
| Fonctionnement | Part des vérifications prévues réellement abouties, avec erreurs et suspensions distinguées. | Les interruptions de quota ou de source ne doivent pas disparaître des comptes. |
| Valeur dans la durée | Utilité rapportée et souhait de conserver la surveillance après une période adaptée au besoin. | Une surveillance d’événement rare peut être utile même sans notification pendant un essai court. |

Les seuils d’acceptation et la méthode de collecte seront définis avant les essais concernés. Ils ne sont pas inventés dans ce document. L’évaluation des composants IA doit inclure des appels réels, plusieurs formulations, des contre-exemples et des cas qui n’ont pas servi à ajuster le système. Les tests avec réponses simulées servent à vérifier l’application, mais ne prouvent pas la qualité du modèle ou de la recherche.

## Amélioration de version en version

L’élargissement pourra porter sur les sources, les demandes couvertes, la compréhension, les langues, les délais ou le coût. Chaque évolution devra préciser le problème traité, le bénéfice attendu, les régressions à vérifier et les limites restantes.

Un retour tel que « déjà vu », « hors sujet » ou « événement manqué » peut alimenter l’amélioration. Cela ne suppose ni entraînement automatique sur les données des utilisateurs ni modification immédiate du moteur. Le recueil, l’usage et la conservation de ces retours seront définis avant leur implémentation.

La progression ne dispense pas une version de respecter ses engagements actuels. Un changement de modèle ou de source doit être réévalué sur les scénarios concernés et sur des cas de non-régression avant sa généralisation. La supervision et l’organisation des agents de développement seront définies dans un processus dédié ; aucune automatisation n’est activée par ce document.

## Positionnement et défenses à construire

L’hypothèse de différenciation est une expérience consacrée aux alertes, accessible sans connaissance de l’IA, avec un parcours direct et une expérience française soignée. Cette hypothèse ne constitue ni une exclusivité ni une preuve de demande.

La qualité de détection, la connaissance des sources, le coût maîtrisé et la confiance pourraient devenir des avantages durables. Leur réalité et la comparaison concurrentielle relèvent du [document 02](02-concurrence-et-differenciation.md). Les données personnelles ne constituent pas un avantage par leur simple accumulation.

## Articulation avec les autres spécifications

| Document | Décisions et détails qui lui appartiennent |
| --- | --- |
| [03 Périmètre du MVP](03-perimetre-mvp.md) | Capacités effectivement livrées, exclusions, langues de lancement, compte et limites de la première version. |
| [04 Parcours et écrans](04-parcours-et-ecrans.md) | Champs, messages, navigation, états d’écran, clarification et traitement visible des limites. |
| [05 Règles des alertes](05-regles-des-alertes.md) | Nouveauté, pertinence, doublons, preuves, fréquence, pause, reprise et fin d’une surveillance. |
| [06 Sources et stratégie de recherche](06-sources-et-strategie-de-recherche.md) | Qualification de la faisabilité, recherche, accès aux sources, limites et choix des méthodes. |
| [07 Coûts et modèle économique](07-couts-et-modele-economique.md) | Budget, quotas, coûts mesurés et modèle gratuit ou payant. |
| [08 Architecture et données](08-architecture-et-donnees.md) | Modèles de données, responsabilités, contrôles d’accès, synchronisation et conservation. |
| [09 Qualité et publication](09-qualite-et-publication.md) | Corpus d’évaluation, tests automatisés, seuils, preuves de réussite et conditions de publication. |

Un futur lot de développement devra référencer les exigences validées, les spécifications détaillées et les tests attendus. Une question ouverte ne vaut pas permission d’inventer un comportement produit. Toute contradiction doit être résolue avant l’implémentation concernée, sans bloquer les travaux indépendants.

## Points restant à confirmer

Les deux arbitrages de positionnement posés à Fabien sont tranchés : France prioritaire et prise en charge des deux formes de suivi. La rédaction approfondie, les exigences POS-R et les scénarios POS-A sont validés par Fabien. Leur déclinaison technique et les paramètres laissés ouverts seront précisés dans les documents suivants.

La décision conditionnelle sur l’anglais et les demandes exclues du MVP figure dans le document 03. Les cadences, les seuils de qualité et le budget restent à fixer dans les documents concernés. Leur absence ici ne signifie pas qu’ils sont laissés à la discrétion d’un agent de développement.
