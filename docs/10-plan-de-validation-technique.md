# Plan de validation et conduite du projet

Mis à jour le 2 novembre 2026. **Document de référence pour l'ordre du travail et l'état courant.** Le recadrage méthodologique a été validé par Fabien ; les choix de fournisseur de production restent ouverts. Les documents 01 à 09 conservent les décisions produit, Android et qualité déjà validées.

## Position actuelle

**Correctif du 2 novembre livré — version connectée 0.1.2.** Synchronisation automatique au premier plan, relecture sur réception du push, notification du résultat trouvé dès la première recherche et textes allégés. Le scénario préparation → terminé passe sans actualisation ni push dans le test natif, avec annulation en arrière-plan/déconnexion. 52 tests unitaires remote, 69 demo, 8 parcours natifs et un rendu sombre/anglais passent ; Lint des deux variantes et frontières passent. Aucun changement serveur, nouvel essai fournisseur ou coût supplémentaire. Mise à jour et essai sur le OnePlus encore nécessaires. Voir [le bilan du correctif](../validation/foreground-sync/bilan.md).

**État courant : S0 à S3 clos pour leur périmètre d’essai, S4 « comprendre puis préparer » livré, première passe UI Material 3 intégrée.** La variante `remoteDebug` utilise le service réel ; `demoDebug` reste un simulateur hors réseau. Les corrections du 29 octobre ont été vérifiées sur des copies des demandes Montpellier/PS5 et sur l’émulateur, sans observation longue ni publication revendiquée. Voir [les contrôles de préparation](../validation/preparation/checks.json).

**Lot terminé — stabilisation V1, le 1er novembre 2026.** La version connectée **0.1.1** est prête pour l’essai OnePlus. Les 59 signalements Lint sont corrigés ; les deux variantes passent sans erreur ni avertissement. Les 42 + 64 tests unitaires Android, les 101 tests service, les contrats, les frontières et les parcours natifs passent dans le périmètre du [bilan de stabilisation](../validation/stabilization/bilan.md). Une précision facultative ne bloque plus « Continuer ». Le README, le guide Android et la CI locale sont actualisés. Architecture, design, données et plafonds conservés ; aucun nouvel appel fournisseur payant, coût supplémentaire nul. L’essai physique et les contrôles de publication restent à faire. GitHub attend la création du dépôt distant.

La suite reste : test de cette version sur le OnePlus, finitions UI/UX à partir des retours, observation de sept jours à organiser dans le budget disponible, puis contrôles release/confidentialité/Play et publication. Aucun suivi de sept jours ni version publiable n’est actuellement revendiqué. Les bilans S3/S4 datés sont historiques ; leurs anciens quotas, décomptes de tests et indications « prochaine étape » ne remplacent pas cet état courant.

## Historique des qualifications et décisions

Les résultats ci-dessous sont datés et conservent les limites constatées à chaque étape.

## Historique des qualifications et décisions

Les résultats ci-dessous sont datés et conservent les limites constatées à chaque étape.

D0 : format Exa Monitor non conforme ; recherche Search pertinente mais dates de publication parfois fausses/non étayées. F0 : Firecrawl retrouve une vraie annonce récente, confirmée par un complément d'audit, mais retient aussi un ancien guide en confondant publication et mise à jour. La découverte avec preuve de récence reste non validée.

**F1 : les quatre cas passent sur une seule page synthétique.** État initial silencieux par notre règle ; bruit de présentation rejeté ; ajout exact détecté avec artiste/date et diff ; relecture identique sans doublon. Cela étaye la voie page connue, sans valider la découverte Web, l'admission libre ni tous les sites. Aucun webhook Firecrawl ou push Android testé. La fixture a été supprimée et les suivis restent en pause, sans passage courant.

F1 : 6 crédits, solde 987 ; cumul Firecrawl F0+F1 : 13 crédits, aucun paiement/recharge. Cinq tests locaux F1 passent. **Y1 annulé par Fabien** : inscription impossible selon son retour et coût jugé élevé. Aucun appel Yutori exécuté ; réserve de 0,50 USD libérée, aucune clé à fournir. La préparation est conservée comme historique, non exécutable.

Le bilan du 9 octobre reste acquis : recherches ponctuelles Exa/Firecrawl, une extraction Firecrawl, laboratoire local avec 34 tests réussis, puis 83 appels OpenAI. La configuration IA échoue à ses critères de compréhension et de résistance aux instructions présentes dans les pages. La chaîne Web complète, la surveillance récurrente et Android ne sont pas validés. Coût estimé de la campagne IA : 0,0476092 USD ; consulter le [bilan et les preuves](../validation/09-bilan-premier-moteur.md).

Les étapes M1 à M6 de [l'ancien laboratoire](../validation/06-plan-premier-moteur.md) sont son historique, pas le plan actif. Leur poursuite est suspendue : pas de comparaison de modèles ni d'extension du moteur maison avant justification par l'audit des services gérés. Le code et les résultats restent conservés.

**Correction de méthode :** la comparaison du 3 octobre mentionnait déjà Exa Monitors et Firecrawl Monitor. Le défaut a aussi été de ne pas faire respecter cette analyse avant l'implémentation. La correction consiste à rendre explicite, avant chaque lot, ce que fournit le prestataire et ce que nous devons développer.

## Résultat F3 — 15 octobre 2026

[Rapport audité](../engine-lab/reports/firecrawl-delivery-result-20261015.json). Deux passages réels de la fiche officielle déjà connue ; quatre messages reçus (`monitor.page` et `monitor.check.completed` par passage), HMAC vérifié sur les corps bruts. Identité du suivi/passage et source concordantes ; résultat de fin rapproché de l'API authentifiée, puis écrit dans le journal F2. État initial silencieux ; seconde lecture identique et silencieuse ; messages altérés rejetés ; rejeux sans nouvel événement. Huit tests de réception et les 13 tests F2 passent.

**Arrêt et coût :** suivi confirmé en pause, aucun passage courant ; boîte temporaire supprimée. Deux crédits gratuits (987 → 985), cumul Firecrawl F0+F1+F3 : 15 crédits ; aucun achat/recharge. Le champ de date future est conservé malgré la pause. Clé API et secret HMAC conservés localement, jamais publiés.

**Portée :** le message a été capturé dans une boîte temporaire puis vérifié localement. Nous n'avons pas déployé un serveur public rejetant une signature incorrecte avant stockage. Aucun changement réel positif, notification Android ou essai de sept jours : F3 valide ce transport et son raccordement local, pas tout le produit. Le besoin d'accès Firecrawl est résolu ; rien de plus à fournir pour ce lot clos.

## Résultat F2 et recommandation actuelle — 12 octobre 2026

**F2 terminé : 13 tests d’acceptation locaux passent**, avec rejeu des quatre sorties F1 : état initial silencieux, bruit ignoré, un candidat documenté pour l’ajout, relecture silencieuse. Le journal persiste sur disque ; doublons après réouverture, pause/suppression, ancienne version, preuves absentes, source inattendue et réceptions désordonnées sont contrôlés. [Rapport reproductible](../engine-lab/reports/delegated-journal-result-20261012.json). Réseau fournisseur : zéro ; coût supplémentaire : zéro. Ce n’est pas un nouvel essai de pertinence du prestataire.

**Recommandation technique, pas adoption de production :** poursuivre avec Firecrawl pour la branche « source connue », sans ajouter de fournisseur ni de seconde IA maintenant. Exa reste une piste de découverte, pas une surveillance validée. Yutori est exclu. Une branche technique sur URL connue ne transforme pas l’onboarding en formulaire obligatoire d’URL : la saisie libre et les suivis généraux restent le produit validé.

**Limites importantes :** le contrôle de F2 vérifie qu’une citation figure réellement dans le diff enregistré. Il ne vérifie pas le sens de toutes les demandes. La déduplication couvre une même preuve textuelle, pas toutes ses paraphrases ou plusieurs sites. Les résultats trop anciens sont supprimés du parcours d’alerte, sans réconciliation automatique. Aucun endpoint public, push Android ou moteur d’admission libre n’est livré. La suppression F2 est un blocage logique d’essai, pas l’effacement de compte de production.

## Objectif et décisions conservées

Priorité à une réalisation Android native que Fabien peut comprendre et défendre en entretien, sans dépense excessive. Une application parfaite ou couvrant le Web entier n'est pas un préalable. France prioritaire ; événements précis et suivis généraux ; entrée en langage naturel et clarification si nécessaire. Les familles déjà validées sont dans le [protocole](../validation/03-protocole-comparatif-des-moteurs.md) : elles diversifient les essais, sans devenir un catalogue obligatoire.

MVVM et Clean Architecture, dans un seul module Gradle initial, restent le cadre Android. Les secrets, les quotas et la surveillance durable relèvent du service central. Une API/RSS directement adaptée reste préférable lorsqu'elle couvre le besoin ; nous ne dressons pas un annuaire de sources pour tous les sujets possibles. Aucun score de confiance fourni par une IA ne remplace une preuve vérifiable.

## Ordre du travail et conditions de passage

| Étape | Question et livrable | Condition de passage / état |
| --- | --- | --- |
| **0 — Audit des services gérés** | Qui fournit recherche, lecture, comparaison, jugement, planification, historique et webhook ? [Comparaison](../validation/02-alternatives-au-moteur-yutori.md), tarifs, inconnues, responsabilités. | Chaque capacité est documentée, testée ou inconnue. Les restrictions affectant l'essai sont identifiées. **Revue actualisée ; aucune adoption de production.** |
| **1 — Contrat d'essai** | Remplir le [protocole](../validation/03-protocole-comparatif-des-moteurs.md) : demandes exactes issues des familles validées, référence indépendante, attendus, faux positifs, coût maximal et arrêt. | Cas et seuils gelés avant appels ; accès et arrêt distant vérifiés ; aucune dépense au-delà de l'autorisation existante. **D0/F0/F1 gelés et exécutés ; Y1 annulé, aucun accès demandé.** |
| **2 — Chaîne déléguée minimale** | Un prestataire à la fois → réception sécurisée → contrôle minimal des preuves et doublons → journal d'événements. Bilan des passages, y compris silencieux ou échoués. | Aucun échec critique sur les cas contrôlés ; cas positifs effectivement détectés. Une absence de positif réel reste non concluante. Coûts rapprochés des compteurs fournisseur. **F1/F2 réussis dans leur périmètre ; F3 livraison signée réelle rapprochée de l’API et journalisée localement. Admission libre, découverte, récepteur de production et push restent ouverts.** |
| **3 — Décision MVP** | Retenir les capacités démontrées, limites, cadence, quotas, coût mensuel et adaptateurs nécessaires. | Décision écrite avec hypothèses économiques et arbitrages produit présentés à Fabien. **Revue du 20 octobre livrée dans les documents 03 et 07. Cadre produit conservé ; plafond et quotas proposés, non autorisés. Ces paramètres ne bloquent pas le socle Android.** |
| **4 — Tranche Android** | Saisie → clarification/proposition → suivi → preuve → notification, avec architecture et tests utiles. | Une démonstration locale peut démarrer en parallèle sur repository simulé explicite ; l'intégration réelle attend les preuves du moteur. **S1/S2 livrés localement : parcours demo et contrats simulés testés ; intégration réelle et push restent en S3.** |
| **5 — Durée et publication** | Sept jours d'observation, qualité, coût, vie privée, accessibilité et exigences Play selon le document 09. | Tous les critères de sortie satisfaits et compte Play autorisé à publier. **Non commencée.** |

Les sept jours évaluent la continuité et les événements observables ; ils ne garantissent ni une couverture mondiale ni une qualité statistique générale. Un essai court de contrat précède cette observation et ne la remplace pas.

## Répartition de référence à confirmer dans l'essai

- **Prestataire de surveillance :** contrôles récurrents, recherche ou relecture selon la voie, détection/jugement documentés, historique fournisseur et livraison des résultats.
- **Notre service :** critères confirmés, admission honnête, correspondance entre suivi utilisateur et suivi fournisseur, version des règles, vérification des webhooks, journal durable, refus des preuves insuffisantes, doublons métier, quotas, pause/suppression, santé du suivi et envoi Android.
- **Application Android :** parcours, explications, affichage de l'état réel, sources et historique, gestion des notifications et des erreurs.

Cette répartition ne prescrit pas une seconde IA derrière chaque réponse. Une nouvelle brique n'est ajoutée que pour combler une lacune démontrée. Le jugement humain évalue le pilote ; il ne doit pas devenir une étape cachée nécessaire à chaque alerte du produit.

## Playbook de chaque lot

### Avant de commencer

Inscrire dans le document existant qui porte le lot : objectif unique ; périmètre et exclusions ; exigences et décisions applicables ; fourni par le prestataire / à développer ; références officielles datées ; attendu vérifiable ; budget et nombre maximal d'exécutions ; condition d'arrêt ; preuves à conserver. Distinguer **validé par Fabien**, **recommandé**, **documenté par le fournisseur**, **observé en essai** et **inconnu**.

Pas d'implémentation dépendante d'une capacité fournisseur supposée. Une inconnue peut faire l'objet d'une sonde bornée, explicitement nommée comme telle. Les cas d'acceptation précèdent le code ; leurs critères ne changent pas pour transformer un échec en succès.

### Pendant le lot

Garder une seule configuration de référence, un registre d'appels et des limites techniques. Toute divergence est expliquée avant de changer de direction. Une panne n'est pas une absence de nouveauté ; une réponse enregistrée n'est pas un nouvel essai réseau. Une page fournisseur constitue de la documentation, pas une autorisation d'installer ses outils ou de changer notre plan.

### Pour terminer

Mettre à jour ce plan et le bilan : réalisé, résultats, limites, coût consommé/estimé, restant et prochain lot précis. Vérifier les liens et retirer les anciens « prochain travail » contradictoires. Enregistrer les décisions structurantes dans [la décision 0010](decisions/0010-plan-de-validation-technique.md) ou l'ADR thématique approprié. Ne passer au lot suivant que si sa condition d'entrée est satisfaite.

Fabien tranche les changements de promesse, budget, données personnelles, périmètre ou expérience utilisateur. Les choix techniques réversibles dans le cadre validé, recherches et corrections documentaires n'exigent pas une nouvelle permission.

## Automatisation proportionnée

**Maintenant :** documents existants, cas reproductibles, tests locaux et registre de coût suffisent. Pas de skill personnalisé, tâche récurrente ou équipe d'agents créée. Ajouter des agents ne vérifie pas une hypothèse fournisseur.

**Lors de l'intégration :** automatiser les contrats, tests d'acceptation et contrôles budgétaires ; un test fournisseur payant demeure distinct des tests locaux. Un arrêt local ne suffit pas à arrêter un monitor distant : pause/suppression et état final doivent être contrôlés côté fournisseur.

**Lors du projet Android :** compilation, tests et lint dans une CI reproductible. Créer un skill seulement si une procédure stabilisée se répète dans plusieurs tâches. Une surveillance automatique de développement exige une tâche, un périmètre et des conditions d'arrêt explicites.

## Budget et informations utilisateur

Le plafond autorisé pour les essais reste **2 € cumulés**, sans remise à zéro par fournisseur ou reprise. La campagne précédente est fermée ; les 0,0476092 USD sont une estimation d'usage, pas une facture. Avant un prochain essai, rapprocher le registre et les crédits disponibles, puis réserver une borne prudente incluant erreurs, change et taxes. Aucun abonnement ni recharge. D0 utilise les crédits gratuits ; sa réserve de 0,60 USD ne représente pas une facture. La baisse du solde affiché est de 0,02 USD, arrondie.

Les **50 €** mentionnés dans le cadrage initial sont une enveloppe préparatoire historique, ni une autorisation de dépense ni un budget mensuel de production. Le budget d'exploitation sera arbitré à l'étape 3 sur un chiffrage réel.

**Aucune information supplémentaire de Fabien n'est nécessaire pour achever l'audit et préparer le contrat d'essai.** Si un accès manque, le demander précisément quand la configuration est prête, en vérifiant d'abord les accès déjà disponibles sans afficher les secrets. Le retour Google sur le compte Play sera intégré lorsqu'il arrivera ; il ne bloque pas ces travaux.

## Backlog de réalisation — 20 octobre 2026

Fabien demande un découpage du travail, pas un nouveau verrou d'approbation entre chaque ticket. **Un seul sprint d'implémentation actif à la fois.** Les choix techniques réversibles sont délégués ; un changement de promesse, une dépense nouvelle, un accès manquant ou une action bloquée est expliqué précisément. Pas de tâche récurrente ni de skill de supervision nécessaire : versionnement, tests, CI et bilan de sprint suffisent.

Ce découpage remplace l'expression trop large « contrat et socle ». Les sprints regroupent des livrables, sans prétendre qu'ils prennent chacun une semaine. Pas de date ferme avant que l'environnement soit opérationnel et que la première tranche ait permis de mesurer le rythme, y compris l'apprentissage de Fabien. Une tâche en échec reste ouverte ; un fichier généré n'est pas une preuve de fonctionnement.

### Préparation S0 — terminée pour autoriser S1

Spécification Android, contrat applicatif v0 et responsabilités : document 08. Backlog ci-dessous. Enveloppes et règles produit : documents 03/07, sans autorisation de dépense additionnelle. Les numéros de dépendances non éprouvés sont une tâche technique S1-02, pas un arbitrage produit. Le backend détaillé et la facture réelle seront décidés au ticket S3-01, avant réseau de production. Le socle Android S1 et les contrats simulés S2 sont livrés ; les adaptateurs réels et leur validation restent en S3.

### Sprint 1 — environnement et fondations

Objectif : Fabien peut ouvrir le projet dans Android Studio, lancer l'application et retrouver une compilation vérifiée. Budget : aucun appel métier payant, aucun abonnement ; téléchargements d'outils gratuits seulement. Surface : écran minimal de démonstration, sans chantier visuel ni moteur.

| Ticket | Travail / éléments concernés | Dépendance | Critère de fin vérifiable |
| --- | --- | --- | --- |
| S1-01 | Installer/configurer Android Studio Apple Silicon, SDK, runtime et émulateur ; documenter les chemins locaux | S0 | IDE lancé, SDK et JDK identifiés, émulateur ARM64 démarré et visible par adb. Ne pas confondre archive téléchargée et outil opérationnel. |
| S1-02 | Résoudre la matrice IDE/AGP/Gradle/Kotlin/Compose/Hilt-KSP ; catalogue et wrapper | S1-01 | Versions exactes stables, dépendances résolues et module vide compilé. Aucun alpha ou contournement silencieux ; adaptation technique justifiée si incompatibilité. |
| S1-03 | Initialiser Git local, exclusions, dossier `android/`, module `:app`, variante demo ; conserver le laboratoire séparé | S1-02 | Aucun `.env`, secret, `.local`, cache, binaire de build ou preuve privée suivi. Wrapper et sources suivis ; aucun dépôt distant créé ni push implicite. |
| S1-04 | Poser frontières présentation/domaine/data, Hilt, ViewModel/StateFlow et un écran minimal FR/EN | S1-03 | Application ouverte sur émulateur avec mention de démonstration ; injection d'un faux repository, retour système fonctionnel. Pas de permission réseau demo. |
| S1-05 | Ajouter un test significatif de transition ViewModel, contrôle de frontières et lint ; préparer la CI | S1-04 | `assembleDemoDebug`, `testDemoDebugUnitTest`, `lintDemoDebug` et contrôle d'architecture passent localement. Workflow sans secrets métier, dépôt distant à raccorder ensuite. |
| S1-06 | Livrer preuve et courte explication d'architecture | S1-05 | APK identifié par empreinte/révision, capture d'émulateur, versions réellement utilisées et commandes de reproduction. Rapport distinguant contrôles locaux et CI distante non exécutée. |

**Fin de sprint :** APK installé et lançable, construction reproductible, contrôles passants, limites documentées. Expliquer à Fabien le trajet action → ViewModel → repository → état. Aucune autorisation supplémentaire requise pour poursuivre les tâches locales dans ce cadre.

### Sprint 2 — parcours Android sur service simulé

Objectif : tester le produit Android et apprendre ses composants avant de payer des recherches. Les fixtures ne prouvent ni l'intelligence ni la fiabilité du futur moteur. Réseau métier : zéro.

**Préparation vérifiée le 20 octobre :** le [document 08](08-architecture-et-donnees.md#contrat-dimplémentation-s2-01--préparation-du-20-octobre-2026) précise maintenant mutations, rejeu, concurrence, expiration, restauration et cas S2-C01 à C14. Ces attendus ont depuis été implémentés et vérifiés : **S2-01 à S2-06 terminés**, selon le [bilan et les preuves](../validation/android-s2/bilan.md). Les comportements visibles restent dans le document 04. Pas de nouvelle stack, dépense ni choix produit requis pour démarrer. La connexion IDE non vérifiée de S1 reste un contrôle de prise en main à faire, sans bloquer les builds CLI déjà éprouvés.

Ordre par ticket : préciser l’attendu → écrire les tests pertinents → implémenter → contrôler le comportement → relire la modification → enregistrer le bilan. Une ambiguïté technique est résolue et documentée ; une modification de la promesse produit est soumise à Fabien avant code. Le fonctionnement simulé de S2 ne sera pas présenté comme une surveillance Web réelle.

| Ticket | Travail / éléments concernés | Dépendance | Critère de fin vérifiable |
| --- | --- | --- | --- |
| S2-01 | Modèles, erreurs et repository demo conformes au contrat v0 ; fixtures et scénarios de test | S1 | Répétition de commande, expiration, conflit, quota et source dégradée testables sans réseau ni temporisation réelle. |
| S2-02 | Saisie, clarification, proposition ; brouillon et navigation typée | S2-01 | Critères présentés avant confirmation ; correction invalide l'ancien résultat ; rotation et réponse tardive couvertes. |
| S2-03 | Identification simulée à l'activation, liste et détail | S2-02 | Parcours nominal complet ; compte demo signalé, aucune collecte de vraies coordonnées ; état vide distinct de panne. |
| S2-04 | Pause, reprise, modification et suppression | S2-03 | Action visible seulement après confirmation du faux service ; échec/conflit récupérables ; anciennes versions non affichées comme actuelles. |
| S2-05 | Résultats sourcés fictifs, états et restauration | S2-03 | Publication inconnue reste inconnue ; démonstration réinitialisée expliquée après perte de sa mémoire ; pas de faux lien officiel. |
| S2-06 | Tests de parcours Compose et revue FR/EN, sombre, grandes polices, compact/large | S2-04, S2-05 | Cas REV-A01/A04/A06 et cas MVP correspondants couverts ; captures sur émulateur, obstacles matériels corrigés, comportements non testés signalés. |

**Fin de sprint :** parcours de bout en bout démontrable, tests utiles, aucun faux suivi distant. Explication de StateFlow, cycle de vie, navigation, injection et tests. Une demande libre dans cette variante ne prétend jamais avoir été comprise par une IA.

### Sprint 3 — première intégration réelle

Objectif : remplacer les adaptateurs simulés par un service réel à portée qualifiée. Les tâches S3-02 à S3-06 se déroulent après S3-01 ; pas de backend implicite dérivé du laboratoire.

| Ticket | Travail / éléments concernés | Dépendance | Critère de fin vérifiable |
| --- | --- | --- | --- |
| S3-01 | Choisir backend, hébergement, identité e-mail, base, push ; spécifier API HTTP et chiffrer exploitation | S2 | ADR, contrat versionné, facture prévisionnelle complète, limites et stratégie d'arrêt. Toute dépense nouvelle et changement de périmètre arbitrés avant engagement. |
| S3-02 | Adaptateur Android remote, session, erreurs et règles de propriété côté service | S3-01 | Tests de contrat ; accès croisé entre deux comptes refusé ; secrets absents de l'APK. |
| S3-03 | Admission libre et surveillance récurrente bornée ; reprises ciblées des cas A1/REV | S3-01 | Cas et seuils figés avant essais ; bilan de compréhension, nouveauté/récence, déduplication et coût. Un échec ne devient pas un succès par changement du seuil. |
| S3-04 | Réception sécurisée, journal de résultats, quota atomique et pause/suppression distante | S3-02, S3-03 | Signature invalide rejetée avant traitement, événements rejoués sans double effet, arrêt distant confirmé et dépassement simulé maîtrisé. |
| S3-05 | Push, permission Android et ouverture du résultat | S3-04 | Notification réelle depuis un résultat admissible ; refus de permission conserve l'accès au résultat ; session expirée et résultat supprimé gérés. |
| S3-06 | Vérifier la chaîne et déclencher observation | S3-05 | Référence de résultats attendus, coûts rapprochés, périodes sans nouveauté/échec distinguées. Cas non couverts explicités ; aucune promesse universelle. |

Si le moteur échoue, présenter une réduction précise ou un report à Fabien ; ne pas poursuivre une recherche ouverte de nouveaux fournisseurs. Les 2 € cumulés d'essais restent le plafond autorisé. Une impossibilité liée au budget n'empêche pas de conserver la démonstration Android acquise.

### Sprint 4 — qualité et publication

| Ticket | Travail | Dépendance | Critère de fin vérifiable |
| --- | --- | --- | --- |
| S4-01 | Sept jours d'observation réelle avec incidents, résultats et coûts | S3-06 | Rapport selon document 09, sans transformer sept jours silencieux en preuve de détection. Les corrections significatives conduisent à réévaluer les preuves concernées. |
| S4-02 | Accessibilité, tailles d'écran, langues, performance et build release | S3 + corrections S2 | Parcours vérifiés ; R8 et profil de démarrage traités conformément au document 09 ; aucun mode demo dans le binaire publiable. |
| S4-03 | Confidentialité, suppression de compte/données, sauvegardes et arrêt financier | S3 | Scénarios ARCH/COST critiques exercés ; configuration et politique correspondent réellement au service. |
| S4-04 | Compte Play, identité publique, signature, fiche Store et diffusion | S4-01 à S4-03 | Situation réelle du compte vérifiée, tests Google applicables satisfaits, dossier prêt ; confirmation de soumission et statut rapportés sans garantir l'approbation. |

### Règles de conduite et preuves

Statuts autorisés : `à faire`, `en cours`, `bloqué (raison précise)`, `terminé (preuve)`. Un seul ticket principal en cours ; des lectures ou téléchargements indépendants peuvent être groupés. Ne pas lancer de refonte esthétique pendant l'installation, de paiement pendant les écrans, ou de comparatif fournisseur pendant les fondations.

Pour terminer un ticket : exigences reliées, comportement vérifié, tests appropriés, preuve enregistrée, limites exposées, documentation utile actualisée. Pas de multiplication des tests pour des changements purement éditoriaux. Avant toute génération de secrets ou signature release, vérifier le stockage sécurisé ; aucun ajout Git global non inspecté dans ce dépôt contenant un laboratoire privé.

À chaque fin de sprint : démonstration, explication pédagogique courte, résultat des contrôles, dépenses et prochaine tâche. Le bilan est une information, pas une demande automatique de permission. La progression continue dans le périmètre autorisé sauf demande d'arrêt de Fabien ou vrai blocage.

### État après livraison du sprint 2

- **S0 terminé :** spécification v0 et backlog rédigés ; aucune compatibilité binaire prétendue validée.
- **S1-01 terminé :** Android Studio installé/lancé, SDK/JDK vérifiés, émulateur API 36 ARM64 démarré et vu par adb. L’inspection de la fenêtre IDE a expiré ; aucune synchronisation IDE n’est déclarée vérifiée.
- **S1-02 terminé :** matrice compilée et catalogue exact, wrapper SHA-256 ; Navigation 3 1.1.0 retenu pour conserver API 23.
- **S1-03 terminé :** dépôt Git local, `android/:app`, sources demo séparées, secrets/laboratoire/outils exclus ; aucun remote créé.
- **S1-04 terminé :** écran FR/EN, faux repository injecté, ViewModel/StateFlow, navigation typée et retour/recréation vérifiés sur appareil ; APK sans INTERNET.
- **S1-05 terminé localement :** 2 tests unitaires, 1 test Android, compilation, Lint et contrôle des frontières passent. Workflow CI préparé, jamais exécuté à distance.
- **S1-06 terminé :** [bilan, capture et empreinte](../validation/android-s1/bilan.md), [guide développeur](../android/README.md).
- **S2-01 à S2-06 terminés :** parcours simulés livrés ; 24 tests unitaires et 5 parcours Compose dans trois configurations réussis ; mort réelle du processus, Lint et frontières vérifiés. [Bilan et preuves S2](../validation/android-s2/bilan.md). Coût fournisseur : zéro. S3/S4 restent à faire ; le moteur réel, l’admission libre, la publication Play et les performances/accessibilité complètes restent non validés.
- **S3-01 à S3-06 terminés techniquement le 22 octobre :** service d’essai borné général raccordé et chaîne cloud/Android qualifiée, coûts rapprochés, sauvegarde cloud chiffrée restaurée localement. [Bilan courant, références, limites et échecs conservés](../validation/s3/bilan.md).
- **S4 à faire :** sept jours d’observation, conception UI/UX et architecture de l’information avec Fabien, qualité/release/confidentialité/sauvegarde indépendante, Play et publication. Aucune observation de sept jours ni V1 publiable n’est revendiquée.

Les documents produit 01 à 09 et leurs conditions de clôture restent applicables. Aucun nouveau budget mensuel, abonnement ou périmètre final réduit n’est validé par ce pilote.


## Suite de S3 — découpage du 22 octobre 2026

Fabien confirme avoir reçu puis ouvert la notification du changement réellement détecté avec son compte. Le raccordement technique d’une page connue est acquis ; il ne clôture pas la surveillance générale du périmètre initial. Les travaux suivants sont ordonnés autour du résultat produit, sans ajouter de connecteurs ou de catégories par anticipation.

| Lot | Travail concret | Condition de sortie |
| --- | --- | --- |
| S3-M1 — demande et plan de surveillance | Conserver les critères d’une demande libre, découvrir et vérifier des sources, établir comment les relire et annoncer leur couverture réelle. Relier l’interprétation à la qualification : actuellement, le worker comprend une demande mais renvoie encore un refus pour la surveillance générale. | Plan activable seulement si les critères décisifs sont observables dans des sources vérifiées et si le coût tient dans le plafond. Questions utiles ou impossibilité explicite sinon. Une URL proposée par un modèle ne vaut pas validation. |
| S3-M2 — résultat pertinent et prouvé | À partir des contenus observés, vérifier le fait, les critères confirmés et les preuves ; distinguer publication, observation et date de l’événement. | Les cas positifs de référence produisent une preuve sourcée ; mauvais sujet, lieu, date, condition ou preuve insuffisante ne déclenchent pas d’alerte affirmative. Les critères et références sont fixés avant les essais. |
| S3-M3 — nouveautés dans la durée | Relier ce jugement au moteur récurrent existant : état initial, faits nouveaux, même annonce reprise ailleurs, correction et suivi ponctuel/continu. | Cas utile de demande libre exécuté de bout en bout sur le service déployé, sans notification répétée pour le même fait ni confusion entre panne et absence de nouveauté. Les fixtures restent identifiées et ne prouvent pas la découverte Web. |
| S3-M4 — clôture opérationnelle | Compléter seulement les vérifications critiques encore ouvertes : récupération cloud, incidents d’envoi et accès au résultat après expiration de session ou suppression. Rapprocher les coûts et les preuves du sprint. | Critères S3 effectivement satisfaits ; aucune clôture par présence de code ou par seul succès d’un test local. Préparation de l’observation S4, sans annoncer les sept jours déjà accomplis. |

Les essais restent proportionnés aux changements : tests ciblés et régressions concernées ; aucun nouvel appel payant sans borne compatible avec les **2 € cumulés**. Aucun abonnement, recharge ou nouveau budget implicite. L’ouverture de nouvelles méthodes reste conditionnée à leurs preuves ; le pilote textuel actuel conserve son enveloppe séparée de crédits gratuits.

**UX/UI reportées à un chantier dédié, à cadrer avec Fabien avant implémentation.** Définir d’abord les actions attendues et les parcours, puis les écrans, les informations réellement utiles, les états (connexion, préparation, actif, résultat, panne), les libellés et la navigation ; travailler ensuite la hiérarchie visuelle et le design sur des maquettes. Ne pas faire correspondre automatiquement un écran à chaque objet technique. Les problèmes fonctionnels qui empêchent un test ou donnent un état faux restent des corrections S3 ; la refonte d’interface n’est pas intégrée silencieusement au lot moteur.

Défauts déjà relevés par Fabien : état connecté peu visible, retour manuel après connexion, longueur et jargon de la proposition, acceptation « portée réduite » incomprise, attente affichée en rouge, mentions de démo/fictif dans les écrans connectés, répétition de la preuve et dates techniques peu compréhensibles dans le résultat. Ils constituent des entrées de cadrage UX, pas une liste de textes à retoucher isolément sans revoir l’information affichée.

### Avancement moteur du 22 octobre — état vérifié

Les composants de compréhension, de preuves et de nouveauté ont été ajoutés au service, mais **ne sont pas encore raccordés au worker déployé pour la surveillance générale**. Le pilote de changement textuel demeure seul activable dans l’application. Aucun ticket S3 supplémentaire n’est déclaré terminé par ces composants isolés.

- Compréhension/jugement : campagne initiale 14/22 puis seconde 17/22 conservées ; corrections du parseur, lot nouveau de réserve 10/10. La relecture locale de 34 réponses passe sans nouvel appel : ce n’est pas une nouvelle mesure indépendante du modèle.
- Découverte réelle : deux campagnes de lecture initiales échouées conservées ; deux demandes ont ensuite chacune trouvé et lu deux pages publiques. Les résultats de recherche servent à découvrir des URL, jamais de preuves. Une page inaccessible est signalée sans effacer les autres sources lues.
- Preuves : premier jugement Web 1/2, avec une citation inventée rejetée. Correction par sélection d’identifiants de passages existants ; nouvelle campagne Web 2/2 : pages scientifiques hors critères rejetées et version LibreOffice courante prouvée. Cela valide ces exemples, pas la couverture générale du Web.
- Nouveauté : état initial silencieux, répétition entre sources, redémarrage, publication ancienne/inconnue et suivi ponctuel vérifiés localement. Persistance transactionnelle, collecte récurrente et notification générale déployée restent à raccorder.
- Régressions utiles : 76 tests du service, contrôle des types et 34 exemples du contrat API réussis. Aucun nouveau build Android ni nouveau parcours de notification général revendiqué.
- Accès aux résultats cloud : 200 propriétaire, 401 sans session, 404 résultat supprimé et 401 après suppression de l’identité synthétique. Fixtures nettoyées. L’essai initial exigeait à tort le corps d’erreur applicatif malgré un rejet de passerelle ; il reste conservé. Le parcours Android avec session réellement expirée et permission refusée reste distinct.
- Sauvegarde/récupération : après le refus initial du contrôle automatique, Fabien a explicitement autorisé la copie sensible chiffrée sur son Mac. Le 22 octobre à 12:19 UTC, un snapshot cohérent Auth/métier a été chiffré, authentifié puis restauré dans une base locale jetable : 53 tables, 147 lignes, empreintes identiques. Archive altérée rejetée avant déchiffrement ; exécution refermée dans la copie ; base jetable supprimée. Cloud actif inchangé et zéro appel payant. Archive et clé dans deux dossiers privés ignorés par Git sur le Mac. Cela prouve la reconstruction des données cloud sur un environnement local distinct ; ni bascule vers un nouveau projet cloud, ni sauvegarde Vault/SMTP/Firebase, ni protection contre perte du Mac. [Preuve](../validation/s3/cloud-backup-restore.json).

Le rapprochement des 128 anciennes réponses terminées ramène leur coût estimé conservateur à 0,081134 USD. Les 58 nouveaux appels ont consommé 0,037794 USD estimés, sans réponse de coût inconnu. L’enveloppe nouvelle totale de 0,40 USD, marges de change/taxes et ancienne provision d’autres frais comprises, borne le cumul à 0,99587625 EUR, sous les 2 EUR autorisés. Ce n’est pas un rapprochement de facture ni un budget mensuel. Solde Firecrawl relu : 957 crédits gratuits ; aucun abonnement ou recharge. Voir les rapports `general-*-result.json`, `general-current-parser-replay.json`, `cloud-result-access.json`, `general-state-checkpoint.json` et `costs.json` dans `validation/s3/`.

Suite technique sans arbitrage produit : terminer le raccordement demande → proposition vérifiée → collectes récurrentes → mémoire des faits et résultats, préserver les réservations de coût avant appels et éprouver cette chaîne déployée. L’autorisation de copie sensible du test de récupération a été donnée et ce test est maintenant exécuté. La refonte UX/UI reste à cadrer avec Fabien ensuite.


### Clôture du 22 octobre — état courant

Les paragraphes d’avancement M1–M4 précédents sont historiques. M1 analyse libre, M2 découverte et preuve, M3 cycle récurrent/état initial/nouveauté et M4 résultat/notification sont raccordés. Qualification générale bornée : 24 h, une page par passage, proposition partielle et quota fini ; elle ne garantit pas de trouver toute source ni tout événement. La proposition Python sans URL est prouvée, le refus de la page LibreOffice inadéquate est conservé. Le cycle positif complet, les cas négatifs et les limites Android sont éprouvés séparément sur une source publique fictive. Voir le bilan pour ne pas confondre portée contrôlée et couverture Web.
