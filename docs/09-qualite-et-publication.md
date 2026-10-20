# Qualité et publication

Version validée par Fabien le 3 octobre 2026 ; [décision 0009](decisions/0009-qualite-et-publication.md). Ce document fixe **comment prouver que la V1 est prête** et comment la publier sur Google Play sans confondre installation réussie, surveillance utile et promesse fiable. Il complète les [exigences produit](03-perimetre-mvp.md), les [règles d'alerte](05-regles-des-alertes.md), la [comparaison des sources](06-sources-et-strategie-de-recherche.md), les [limites de coût](07-couts-et-modele-economique.md) et l'[architecture](08-architecture-et-donnees.md). Les règles externes citées ont été consultées le 3 octobre 2026 et devront être revérifiées juste avant dépôt.

## Définition de « prêt à publier »

La publication signifie une V1 gratuite, accessible sans invitation en France sur le Play Store, avec des limites d'usage annoncées et appliquées côté service. L'extension à d'autres pays demandera une preuve de couverture et de support. Le lancement n'est pas une démo de l'écran d'accueil : une demande admissible doit être comprise, activée, contrôlée dans la durée, donner un résultat sourcé pertinent et rester maîtrisable par la personne. Une demande impossible doit être refusée honnêtement.

Le feu vert requiert **toutes** les portes suivantes. Une case « à corriger » ou « non mesurée » bloque le lancement si elle concerne la promesse principale, la sécurité, la suppression des données ou un coût non borné.

| Porte | Preuve attendue | Blocage typique |
| --- | --- | --- |
| Fonctionnement | Scénarios d'acceptation des documents 01 à 08 exécutés, avec résultat et version de l'application consignés. | Création seulement simulée, action non confirmée affichée comme réussie, notification impossible à ouvrir. |
| Qualité des alertes | Corpus de référence, essais contrôlés puis observations réelles ; erreurs et limites analysées. | Ancien fait présenté comme neuf, doublon, source inventée, demande non suivable activée. |
| Continuité | Contrôles dus effectivement exécutés ou état dégradé visible ; essai de panne et de reprise. | Silence d'une source interprété comme « rien de nouveau ». |
| Coût | Consommation mesurée, projection prudente et capacité limitée sous le plafond d'exploitation approuvé. | Nouveaux utilisateurs pouvant déclencher des vérifications non bornées. |
| Android | Parcours principaux, langues annoncées, accessibilité, permissions, compatibilité et version de production vérifiés. | Texte illisible, contenu masqué par les barres système, permission refusée qui casse l'app. |
| Données et sécurité | Contrôle d'accès, suppression, conservation, liens publics et déclarations Play éprouvés. | Accès au résultat d'un autre compte, suppression fictive, déclaration de données inexacte. |
| Distribution | Compte Play, piste de test et demandes de Google satisfaits ; fiche Store et fichier signé prêts. | Compte encore soumis à test fermé obligatoire ou accès de revue Google impossible. |

## Corpus d'évaluation et critères de vérité

Constituer le corpus **avant** de régler les prompts ou de choisir un fournisseur. Chaque fiche de cas décrit la phrase de départ, les précisions attendues, les critères confirmés, la source autorisée, son état initial, les événements injectés ou observés, le résultat attendu, la date de disponibilité du fait et la raison de la décision. Une personne révise les faits de référence ; le même modèle IA ne produit pas et ne note pas sa propre réponse.

Le jeu de départ doit comprendre les familles ci-dessous, en français et en anglais si l'anglais est annoncé. Les exemples servent à éprouver une méthode ; ils n'obligent pas le produit à accepter toutes les catégories du monde.

| Famille | Cas indispensables |
| --- | --- |
| Compréhension | Demande précise ; ville ou entité ambiguë ; correction d'un critère déjà donné ; sujet général avec « important » à préciser ; demande irréalisable. |
| Référence et nouveauté | Fait antérieur à l'activation ; nouveauté datée après activation ; publication tardive ; date de publication absente ; même fait repris par plusieurs sources. |
| Pertinence | Résultat positif ; mention superficielle du sujet ; exclusion explicite ; seuil avec unité, devise et variante ; source qui ne prouve pas le critère exact. |
| Vie d'une alerte | Doublon, correction importante, changement cosmétique, ponctuelle terminée, pause et reprise, modification des critères. |
| Panne et sécurité | Source vide ou indisponible, contradiction, budget atteint, URL locale ou redirigée, contenu Web tentant de dicter une instruction, compte d'un autre utilisateur. |
| Livraison | Permission refusée, jeton push expiré, téléphone éteint, session expirée au clic, résultat supprimé, réessai d'envoi. |

**Seuils minimaux retenus avant tout essai payant :** dans les cas contrôlés où la source et le fait attendu sont connus, aucune fausse notification affirmative, aucun doublon, aucun accès entre comptes et aucun contrôle prétendument réussi après une panne. Les nouveautés admissibles injectées dans la portée promise sont toutes détectées ou conduisent à un échec visible. Ces seuils évaluent la logique et l'intégration, **pas** la couverture du Web réel.

Pour les suivis généraux et les sources réelles, mesurer séparément : demandes correctement acceptées ou refusées, résultats pertinents parmi ceux notifiés, événements de référence manqués **dans les sources annoncées**, doublons, délai entre disponibilité et détection, contrôles en retard, coût par surveillance et cas nécessitant une intervention. Un « rappel » global de tout Internet n'est pas mesurable et ne sera pas revendiqué. Un petit nombre d'exemples ne justifie pas une précision commerciale en pourcentage. Après constitution du corpus réel et avant l'essai comparatif, consigner les seuils chiffrés de pertinence, délai, coût et volume minimal d'observations ; ne pas les ajuster après avoir vu les résultats pour déclarer le test réussi.

L'évaluation IA garde un sous-ensemble de cas qui n'a pas servi à ajuster les prompts. Les essais simulés vérifient les parcours ; des appels réels restent nécessaires pour juger compréhension et recherche. Les méthodes candidates sont comparées sur les mêmes cas, à source et cadence équivalentes, comme prévu aux documents 06 et 07.

## Pyramide de tests pendant la construction

| Niveau | Ce qui mérite un test automatisé | Quand l'exécuter |
| --- | --- | --- |
| Domaine Android | Reformulation des états, règles de commande/version, cas d'usage et erreurs ; faux repositories, sans service externe. | À chaque changement concerné et dans la CI. |
| Architecture Android | Le domaine n'importe ni Android, ni réseau, ni implémentations data ; les dépendances suivent le document 08. | À chaque demande de fusion. |
| Service | Règles d'état, état initial, nouveau/ancien, idempotence, correction, quota, ownership, suppression, réessai et coût. | À chaque demande de fusion. |
| Contrat API | Codes d'erreur, données essentielles, versions, pagination et compatibilité avec l'application précédente. | À chaque modification du contrat. |
| Intégration contrôlée | Source de test modifiable, planificateur, base, intention d'envoi et reprise après panne. | Avant fusion d'un changement de moteur ; obligatoirement avant chaque sortie. |
| Interface Compose | Création/clarification, proposition, liste, détail, états vide/erreur, pause, suppression et retour depuis notification. | Tests ciblés dans la CI si l'environnement le permet ; ensemble complet avant sortie. |
| Appareil réel et service de test | Livraison push, permission acceptée/refusée, compte e-mail, perte réseau, fermeture de l'app, installation/mise à jour. | Avant publication et après tout changement touchant ces parcours. |

Les tests Compose vérifient **le comportement et la sémantique** plutôt qu'une copie des détails d'implémentation. La documentation Android fournit des APIs pour rechercher des éléments, vérifier leurs attributs et déclencher des actions. [Tests Compose officiels](https://developer.android.com/develop/ui/compose/testing). Quelques captures de référence sont utiles pour les écrans critiques et les deux thèmes, mais leur volume ne remplace pas les essais d'accessibilité ou de parcours. Un profil de démarrage ou des optimisations supplémentaires ne sont ajoutés que si une mesure révèle un problème notable.

La CI construit l'application et le service, exécute les contrôles de format et de qualité, les tests unitaires et d'architecture, les tests de contrat puis les tests d'intégration réalisables sans fournisseur payant. Les clés, le certificat de signature et les comptes de production n'entrent pas dans le dépôt. Un échec bloquant interdit la fusion ; aucun achat de minutes de CI ou de service d'essai n'est implicite. La configuration exacte de la CI et des dépendances sera choisie avec le code, mais son résultat doit être visible sur chaque changement.

## Essais manuels et accessibilité

Avant sortie, Fabien et les quelques proches disponibles essaient le parcours **sans explication préalable** : formuler une demande, comprendre la proposition, l'activer, identifier pourquoi une alerte arrive, retrouver le résultat, mettre en pause et supprimer. Leur petit nombre peut révéler des incompréhensions, sans démontrer à lui seul la qualité de la recherche. Noter les tâches abandonnées, les mots mal compris et les attentes trompées, puis corriger et rejouer les scénarios concernés.

Tester au minimum l'appareil Android physique de Fabien et des émulateurs couvrant la plus ancienne version Android effectivement supportée, Android 13 pour la permission de notification, ainsi que la version ciblée pour le Play Store. Sur chaque parcours important : thème clair/sombre, grande police, clavier ouvert, rotation ou changement de taille d'écran, gestes de retour, barres système, TalkBack, ordre de lecture, noms accessibles, contraste et zones tactiles. Un écran tablette côte à côte n'est pas exigé par le MVP ; la version téléphone doit rester utilisable sur les tailles prises en charge. Android impose l'affichage bord à bord pour les apps ciblant API 35+ sur Android 15+ ; il faut vérifier les marges et l'absence de contenu caché. [Guide Android Compose bord à bord](https://developer.android.com/develop/ui/compose/system/setup-e2e).

La permission de notification est demandée au moment utile et son refus ne désactive pas le suivi côté service. Android 13 et supérieur introduit une permission runtime pour les notifications non exemptées ; les parcours accepté, refusé et réactivé sont donc testés. [Documentation Android](https://developer.android.com/develop/ui/compose/notifications/notification-permission). La V1 n'a pas besoin d'une permission de géolocalisation si la personne saisit elle-même sa zone ; toute permission supplémentaire exige une fonctionnalité réelle et une nouvelle vérification de la politique Play.

Le français est testé sur tous les écrans, erreurs, notifications et textes générés. L'anglais n'est affiché comme langue prise en charge que si les mêmes parcours et résultats sont relus et testés en anglais. Une source étrangère peut garder son titre original avec attribution, sans que la langue de l'interface change le lieu ou les critères d'une surveillance. Cette condition reprend le document 03.

## Essai de durée et décision de lancement

Avant publication publique, faire tourner au moins **sept jours consécutifs** de surveillances contrôlées et quelques surveillances réelles, sans laisser l'application ouverte en permanence. Pour chaque échéance, pouvoir distinguer réussite, retard, panne de source, quota ou pause. Tester une panne volontaire, une source redevenue disponible, la suppression de compte et la restauration de sauvegarde en environnement de test. Si Google impose un test fermé plus long au compte utilisé, cette période sert aussi à recueillir des retours humains, mais le simple écoulement des jours ne remplace pas les preuves techniques.

Le rapport de sortie contient : version exacte de l'app et du service, corpus et résultats, incidents ouverts, qualité et limites connues, dernière exécution des tests, coût mesuré et projection à capacité maximale, budget mensuel approuvé, politique de rétention effective, langues et pays annoncés, captures réelles et décisions de lancement. Un incident touchant la perte de couverture cachée, la divulgation de données, une fausse alerte affirmative contrôlée, une dépense non bornée ou une suppression inefficace bloque la sortie. Les défauts visuels mineurs peuvent être reportés s'ils ne rendent pas l'usage ou l'accessibilité trompeurs.

## Parcours Play Store vérifié au 3 octobre 2026

**Compte développeur à qualifier d'abord.** Google facture actuellement **25 $ US une seule fois** pour ouvrir un compte Play Console. Le type de compte et sa date de création modifient ensuite le chemin de publication. [Aide officielle Play Console](https://support.google.com/googleplay/android-developer/answer/6112435).

| Situation du compte | Chemin nécessaire selon l'aide Google actuelle |
| --- | --- |
| Compte personnel créé **après le 13 novembre 2023** | Test fermé avec **au moins 12 testeurs inscrits en continu pendant 14 jours** ; ensuite demande d'accès à la production et réponses sur le test et l'état de l'application. Le test ouvert n'est accessible qu'après cet accès. Deux ou trois proches ne remplissent pas cette condition. |
| Compte personnel plus ancien ou compte d'organisation | La règle particulière des 12 testeurs ne s'applique pas selon cette aide ; vérifier dans la Play Console les autres étapes et contrôles propres au compte. Un test interne avec les proches reste utile. |
| Aucun compte encore créé | Choisir le type de compte en connaissance des règles, coût et justificatifs, sans créer un compte d'organisation fictif pour éviter le test. |

La condition ci-dessus est celle de [l'aide officielle sur les nouveaux comptes personnels](https://support.google.com/googleplay/android-developer/answer/14151465), consultée le 3 octobre 2026. Elle conditionne **l'accès à la production**, pas la faisabilité technique de l'application. On peut développer et tester en interne pendant la préparation. Le statut réel du compte de Fabien est documenté ci-dessous ; aucune date de sortie publique ne peut être promise avant sa régularisation ou le choix d'un autre compte autorisé.

**État constaté sur les captures fournies par Fabien le 3 octobre 2026 :** la connexion au compte développeur historique « vboo » fonctionne, mais la Play Console indique que son profil et ses applications ont été retirés de Google Play le **29 janvier 2025** parce que le compte n'a pas été validé avant l'échéance. Elle indique aussi que les modifications ne seront pas publiées. Il s'agit donc d'un compte **accessible mais actuellement restreint**, et non d'un compte prêt à recevoir Préviens-moi. Une notification distincte signale un problème urgent lié au compte de paiement ; sa nature et son effet exact demandent lecture des détails. Les captures ne permettent pas de confirmer le type de compte ni de savoir si Google accepte encore sa régularisation. [Vérification des anciens comptes selon Google](https://support.google.com/googleplay/android-developer/answer/14177239).

**Prochaine preuve requise :** ouvrir « Afficher les détails » dans la Play Console, relever le statut exact, les étapes de vérification proposées et les éventuelles voies d'assistance ou de recours, puis traiter aussi l'alerte du compte de paiement. Vérifier ensuite que Google lève effectivement la restriction et autorise une nouvelle publication. Si ce compte historique est rétabli, la règle particulière des 12 testeurs pour les nouveaux comptes personnels ne s'applique pas à lui. Si sa régularisation échoue et qu'un nouveau compte personnel est nécessaire, appliquer le test fermé de 12 personnes pendant 14 jours et demander l'accès à la production ; ne pas présumer qu'une nouvelle inscription contourne une restriction de l'ancien compte.

L'adresse Gmail utilisée pour se connecter au compte peut rester liée à l'ancienne application : elle n'impose pas le nom public du développeur ni l'adresse de contact affichée sur Google Play. Vérifier ou mettre à jour le nom public, les coordonnées privées vérifiées et l'adresse publique de contact pour Préviens-moi. [Coordonnées exigées par Google Play](https://support.google.com/googleplay/android-developer/answer/10840893).

Pour une nouvelle application téléphone, la page officielle indique qu'à partir du **31 août 2026**, les nouvelles apps et mises à jour soumises à Google Play doivent cibler **Android 16, API 36 ou plus**. Vérifier à nouveau l'exigence au dépôt et tester les changements de comportement de cette cible ; la version minimale supportée est un choix produit distinct. [Exigence Android/Google Play](https://developer.android.com/google/play/requirements/target-sdk).

La livraison passe par un **Android App Bundle signé** avec Play App Signing, un nom de package choisi avec soin, un numéro de version croissant, une fiche Store et un e-mail de contact. [Création d'application Play Console](https://support.google.com/googleplay/android-developer/answer/9859152). Préparer icône, captures authentiques de la V1, courte et longue descriptions, catégorie, pays, tranche d'âge et déclaration de contenu. Le nom commercial reste provisoire tant que sa disponibilité et son usage n'ont pas été vérifiés. La fiche ne promet ni temps réel, ni « tout le Web », ni une langue ou une source non testée.

**Compte et confidentialité sont bloquants pour la publication.** La politique Google demande une politique de confidentialité publique, liée dans l'application et dans Play Console, qui décrit les données et leurs usages. Le formulaire « Sécurité des données » doit correspondre aux traitements réels, y compris ceux des SDK et fournisseurs. Puisque la V1 permet de créer un compte, l'utilisateur doit pouvoir demander sa suppression **dans l'app et depuis une ressource Web externe fonctionnelle** ; la simple désactivation du compte ne suffit pas. Une page publique minimale pour la politique et la suppression est donc nécessaire même si le produit reste Android. [Politique Google Play](https://support.google.com/googleplay/android-developer/answer/18258653), [guide de suppression](https://support.google.com/googleplay/android-developer/answer/13327111).

La connexion par code ou lien e-mail du document 08 pose un autre point à vérifier : Google doit pouvoir accéder aux fonctions protégées pour examiner l'application et demande des identifiants ou instructions **réutilisables et valides à tout moment**, même si le parcours habituel utilise un code à usage unique. Prévoir un compte de revue dédié, sans données privées, et une méthode d'accès conforme, sans porte dérobée globale ; si le fournisseur choisi ne le permet pas, adapter le parcours d'authentification **avant** le dépôt. [Exigences d'accès pour la revue Google](https://support.google.com/googleplay/android-developer/answer/15748846).

Déposer d'abord sur une piste interne pour tester l'installation issue de Play, puis suivre la piste fermée si le compte y est soumis, corriger les problèmes signalés, demander l'accès à la production et enfin publier dans les pays décidés. Le rapport de pré-lancement et Android vitals servent à détecter certains problèmes de stabilité et compatibilité, sans remplacer nos tests. [Pistes et déploiement](https://support.google.com/googleplay/android-developer/answer/9859348), [rapport de pré-lancement](https://support.google.com/googleplay/android-developer/answer/9842757), [Android vitals](https://support.google.com/googleplay/android-developer/answer/9844486).

## Après publication

Surveiller quotidiennement au début : contrôles en retard, sources dégradées, fausses alertes signalées, doublons, coût et capacité restante, demandes de suppression, plantages et avis. Un problème de couverture impose un état visible dans l'app ; un dépassement de budget suit les règles du document 07. Une mise à jour corrige les défauts avec nouveaux tests de non-régression et une version de service compatible. Les données personnelles ne deviennent pas un jeu de test sans traitement approprié. Le README public pourra montrer architecture, décisions et limites, captures et procédure locale de développement, sans exposer clés ou données utilisateurs.

## Exigences validées

| Référence | Exigence |
| --- | --- |
| QUAL-R01 | Relier les exigences des documents 01 à 08 à des scénarios exécutés et résultats consignés. |
| QUAL-R02 | Tester moteur, API, Android et chaîne complète avec données contrôlées, puis observer des sources réelles dans leur portée annoncée. |
| QUAL-R03 | Bloquer la sortie sur fausse alerte affirmative contrôlée, doublon, accès entre comptes, panne cachée ou dépense non bornée. |
| QUAL-R04 | Mesurer pertinence, manqués observables, délai, santé des sources et coût, sans revendiquer une couverture exhaustive du Web. |
| QUAL-R05 | Exécuter contrôles automatiques, tests de régression et vérifications de dépendances sur chaque changement pertinent. |
| QUAL-R06 | Vérifier permissions, accessibilité, appareil hors ligne, langues annoncées et versions Android supportées. |
| QUAL-R07 | Satisfaire les règles Play applicables au compte et à la date : cible API, piste, testeurs éventuels, bundle signé et déclarations. |
| QUAL-R08 | Publier politique de confidentialité, déclaration de données et parcours de suppression du compte dans l'app et sur le Web. |
| QUAL-R09 | Fournir un accès stable aux fonctions protégées pour la revue Google malgré la connexion e-mail à usage unique. |
| QUAL-R10 | N'ouvrir la production qu'après budget d'exploitation, capacité, rapport de sortie et surveillance opérationnelle établis. |

## Scénarios d'acceptation validés

| Référence | Situation | Résultat attendu |
| --- | --- | --- |
| QUAL-A01 | Une nouvelle page répète un événement antérieur à l'activation. | Aucun résultat de nouveauté ni notification ; la référence initiale l'explique. |
| QUAL-A02 | La source contrôlée publie un événement répondant aux critères. | Résultat sourcé unique ; notification si permise ; clic vers ce résultat. |
| QUAL-A03 | Une source échoue pendant l'essai de durée. | Échec, dernière réussite et couverture dégradée visibles ; jamais « rien à signaler » par défaut. |
| QUAL-A04 | Un cas irréalisable paraît plausible à un modèle IA. | Pas d'activation opérationnelle sans méthode récurrente et preuve de portée. |
| QUAL-A05 | Une demande contient une URL vers un réseau interne ou une instruction malveillante dans la page. | L'URL est bloquée ou le contenu traité comme donnée non fiable ; aucun accès interne ni changement de règle. |
| QUAL-A06 | L'utilisateur refuse les notifications sur Android 13+. | La surveillance et ses résultats restent consultables ; absence de push expliquée. |
| QUAL-A07 | Le téléphone est hors ligne lors d'une pause. | L'interface n'annonce pas une pause réalisée avant confirmation du service. |
| QUAL-A08 | Une personne supprime son compte depuis l'app ou utilise la voie Web après désinstallation. | Elle peut demander et obtenir l'effacement selon la politique publiée ; les surveillances cessent et ne réapparaissent pas après restauration. |
| QUAL-A09 | Une personne inconnue de Fabien installe la V1 publique. | Elle peut suivre le même parcours gratuit que les premiers testeurs, sous réserve des limites annoncées. |
| QUAL-A10 | Le compte Play est personnel et soumis à la règle des 12 testeurs. | La production attend le test fermé requis et l'accès accordé par Google ; les deux ou trois proches ne sont pas comptés comme une dispense. |
| QUAL-A11 | Google examine les fonctions après identification. | Le compte de revue et ses instructions sont utilisables sans dépendre d'un code temporaire reçu par Fabien. |
| QUAL-A12 | L'anglais est annoncé sur le Store. | Parcours, erreurs, notifications et résultats générés sont vérifiés en anglais ; sinon la fiche et l'app ne le promettent pas. |

## Décisions de publication validées

| Référence | Décision | Ce qu'elle conditionne |
| --- | --- | --- |
| QUAL-D01 | Viser une sortie **en production sur le Play Store**, gratuite, avec distribution initiale **en France** ; étendre les pays après preuve de couverture et de support. | Portée réelle de la V1 et textes de la fiche. |
| QUAL-D02 | Conserver une porte de qualité stricte : zéro échec critique du corpus contrôlé, comparaison réelle de sources, coût borné et **sept jours** d'essai de durée avant production. | Date de lancement ; des tests supplémentaires restent possibles si un échec survient. |
| QUAL-D03 | Préparer une page Web publique minimale pour politique de confidentialité et demande de suppression, ainsi qu'un compte de revue Google compatible avec la connexion e-mail. | Deux livrables indispensables à la publication, sans transformer le projet en application Web. |
| QUAL-D04 | Essayer d'abord de régulariser le compte historique accessible mais restreint ; n'utiliser la piste interne puis la production qu'après levée effective de la restriction. Si un nouveau compte personnel est nécessaire et autorisé par Google, appliquer le test fermé avec 12 inscrits pendant 14 jours, puis demander l'accès à la production. | Calendrier et nombre de personnes à mobiliser ; issue de la vérification du compte ancien à confirmer. |
| QUAL-D05 | Fixer les seuils numériques de pertinence, délai et coût **avant** les essais comparatifs, sur un corpus et des sources identifiés ; garder les seuils de sécurité contrôlés ci-dessus non négociables. | Décision de fournisseur et promesse affichée, sans métrique inventée à partir d'un exemple. |

Ces décisions n'engagent ni frais Play, ni dépense de fournisseur, ni publication immédiate. La **levée ou non de la restriction du compte historique** et les seuils chiffrés issus du corpus restent à établir pour transformer cette spécification en calendrier concret.
