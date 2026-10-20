# Protocole comparatif des moteurs de surveillance

## Prochain lot A1 — admission honnête, critères préparés le 15 octobre

Objectif : séparer « comprendre ce que la personne veut » de « pouvoir le surveiller ». Le contrat d'admission historique A08 proposait un modèle fictif parce que ses critères étaient compréhensibles : cette sortie ne doit jamais suffire à rendre un suivi activable. Les critères compris, la couverture vérifiée, les questions et l'activation sont des états distincts.

Périmètre initial : interprétation et décision d'admission, sans création de monitor ni activation automatique. Réutiliser les formulations historiques A01 à A08, et D01 « nouveaux concerts annoncés à l'Accor Arena Paris », plutôt que choisir de nouveaux exemples faciles. Les résultats d'une vérification de source doivent être fournis séparément au modèle ; l'IA ne peut pas attester elle-même qu'un accès a réussi.

Acceptation avant implémentation :

1. A01/A02 : mêmes critères FR/EN, y compris seuil strict, neuf, livraison, pays et exclusions ; « critères compris » ne signifie pas « surveillance activable ».
2. A03/A04 : question utile pour l'information manquante, sans inventer budget, produit ou lieu.
3. A05/A06 : respecter la modification sur le reconditionné et distinguer « sous » de « maximum ».
4. A07 : expliquer qu'un compte bancaire privé n'est pas accessible par notre surveillance Web ; aucune activation proposée.
5. A08 : conserver la référence saisie mais ne pas inventer de marchand/source ni prétendre que le modèle existe.
6. D01 sans source vérifiée : demande comprise, recherche de source nécessaire ; pas d'activation universelle. Une seule fiche de concert connue ne couvre pas tous les futurs concerts de la salle.
7. Source trouvée mais conditions non observables, source inaccessible ou API en erreur : expliciter couverture insuffisante ou panne temporaire ; aucun de ces cas n'est « rien de nouveau ».
8. Texte d'une page contenant des instructions : ne jamais l'utiliser comme autorité pour modifier les critères confirmés. L'échec d'injection observé le 9 octobre reste une régression obligatoire, non effacé par le succès de F3.

Seuil : zéro activation sans preuve de capacité, zéro critère explicite déformé sur les cas de ce lot. Si le modèle échoue, conserver l'échec et recommander le parcours contraint nécessaire, sans multiplier les ajustements jusqu'à faire passer les mêmes cas. Ce lot ne mesure ni la couverture globale ni toutes les familles ; les suivis généraux restent une exigence du produit à éprouver avant de les promettre.

Prochaine préparation technique : schéma de sortie distinct pour compréhension et faisabilité, règles d'activation déterministes, puis essai IA borné avec tarif et réservation dans le plafond cumulatif avant exécution. **Aucun appel IA A1 exécuté, aucun budget supplémentaire accordé par cette préparation.** Les résultats décideront si le périmètre produit exige un arbitrage de Fabien.

## F3 — résultat du 15 octobre 2026 : livré, vérifié et clos

[Rapport audité](../engine-lab/reports/firecrawl-delivery-result-20261015.json). Deux passages complets : premier `new` transformé en état initial silencieux ; second `same`, silencieux. Quatre messages réels vérifiés sur corps bruts ; monitor, passage et source concordants. Résumés signés rapprochés de l'API authentifiée puis journalisés. Corps altéré rejeté et rejeu sans candidat supplémentaire. Zéro notification utilisateur, zéro candidat d'alerte ; aucune nouveauté réelle dans cette observation.

Deux crédits gratuits, solde 987 → 985 ; cumul Firecrawl : 15 crédits. Statut final `paused`, `currentCheckId=null`, champ de prochaine date conservé au 17 octobre ; boîte temporaire supprimée. Huit tests de réception et 13 tests F2 passent. Campagne fermée, ne pas relancer. Secret d'accès disponible, aucune intervention supplémentaire de Fabien nécessaire pour F3.

### Préparation et périmètre enregistrés avant les appels

Reconnexion effectuée par Fabien ; secret de signature récupéré via les réglages officiels, conservé dans le `.env` privé, jamais dans les rapports. Solde avant lancement : 987 crédits gratuits sur 1000. [Contrat figé](../engine-lab/firecrawl-delivery-contract.json) avec empreinte, deux passages / quatre crédits maximum, un monitor isolé ; aucune recharge. Source relue : fiche officielle Guy2Bezbar déjà utilisée dans F0. Les schémas `monitor.page` et `monitor.check.completed` et la signature ont été relus dans les documentations officielles le 15 octobre.

Huit tests locaux de réception passent : signature correcte, corps altéré, signature absente/mal formée/incorrecte, autre source/monitor, divergence API/webhook, échec, partiel et type inconnu. Les 13 tests du journal F2 passent également. Ces tests précèdent l'exécution réelle ; leur succès n'est pas un succès de livraison distante.

Réception retenue pour cet essai : boîte Webhook.site temporaire avec expiration demandée d'une heure et suppression finale ; corps brut récupéré puis HMAC vérifié localement. Le message signé de fin de passage doit correspondre au résultat obtenu par API authentifiée avant entrée dans le journal. Le secret HMAC n'est jamais transmis à la boîte. Aucun serveur public avec rejet à l'entrée ni push Android testé. Aucune surveillance conservée active après le lot.

## Historique : F2 terminé / F3 préparé — 12 octobre 2026

F2 : **13 tests locaux réussis**, avec `python3 replay_delegated.py` dans `engine-lab`. Le rejeu produit « état initial silencieux → bruit → un candidat documenté → inchangé ». [Rapport](../engine-lab/reports/delegated-journal-result-20261012.json), avec empreintes du code et de l'entrée. Zéro appel fournisseur, zéro coût, zéro push. Une panne ou preuve insuffisante ultérieure annule aussi les candidats encore en attente. Ce choix conservateur de laboratoire n'établit pas une garantie d'envoi en production. La réception hors ordre peut supprimer un résultat ancien ; pas de mesure de rappel déduite de ce test.

**F3 — contrat préparé, non exécuté :** vérifier la réception d'un véritable message Firecrawl signé puis son rattachement à un passage enregistré. Source publique déjà auditée dans F0 : [fiche Guy2Bezbar sur le site de l'Accor Arena](https://www.accorarena.com/fr/programmation/guy2bezbar--ffc3f). Intention étroite du test : changements du contenu de cette fiche ; ne pas assimiler ce contrôle à la découverte de nouveaux concerts.

- Répartition : Firecrawl relit et livre ; notre réception conserve les octets, vérifie la signature avant traitement, identifie monitor/passage/source et journalise sans doublon. Un format inconnu est rejeté, sans normalisation inventée.
- Entrée : accès au secret de signature distinct de la clé API, page lisible et schéma de livraison vérifié dans la documentation. Secret absent du `.env` ; navigateur déconnecté, reconnexion demandée à Fabien. Aucun secret publié dans les rapports.
- Limites : un nouveau monitor isolé, deux passages manuels maximum, une page par passage, maximum quatre crédits gratuits. Vérifier le solde avant ; aucun achat, aucune relance automatique d'un POST incertain. Pas de réactivation D0/F0/F1. Pause distante et contrôle de fin même après erreur.
- Attendus : livraison signée retrouvée pour le passage attendu ; signature altérée rejetée localement ; message rejoué sans nouvel événement ; premier état silencieux ; passage échoué distinct d'un résultat inchangé ; arrêt et consommation vérifiés. Sans nouveau changement réel, détection positive non concluante.
- Réception : une boîte temporaire ne constitue pas un endpoint de production. Si elle est utilisée pour capturer les octets, le bilan dira « capture puis vérification locale », sans prétendre au rejet à l'entrée d'un serveur public. Un récepteur public avec rejet avant stockage demeure une validation séparée avant publication.
- Arrêt : absence de signature, erreur de contrat, budget atteint, authentification impossible ou état distant ambigu. Ne pas masquer l'échec en désactivant la vérification.

Références officielles relues le 12 octobre : [Monitor](https://docs.firecrawl.dev/features/monitoring), [signature HMAC sur le corps brut](https://docs.firecrawl.dev/webhooks/security). Le passage réel exige encore la vérification du schéma exact de la configuration et du message ; aucun appel F3 n'est autorisé par la seule présence de cette préparation sans satisfaire ces conditions d'entrée.

## Lot F2 — journal local, contrat avant implémentation, 12 octobre 2026

**Objectif :** compléter notre côté de l'étape 2, en rejouant les résultats F1 déjà enregistrés. Aucun nouvel appel fournisseur, aucune dépense, aucune notification. Ce lot ne teste ni un webhook Firecrawl réel, ni la recherche de sources, ni une nouvelle IA.

Entrée : représentation interne des quatre résultats F1, obtenus par l'API lors de F1. Le format interne n'est pas présenté comme le format d'un webhook. L'adaptateur réseau et son authentification restent à intégrer. Le journal SQLite est un composant de laboratoire ; il ne choisit pas la technologie du backend de production.

**Acceptation gelée :** (1) état initial identifié explicitement et silencieux ; (2) bruit et relecture silencieux ; (3) un seul candidat pour l'ajout F1 avec citation présente dans le diff ; (4) répétition du même passage, y compris après réouverture de la base, sans nouvel événement ; (5) même preuve reçue sous un autre identifiant sans doublon ; (6) preuve absente ou non conforme : insuffisant ; (7) échec/partiel distinct du silence et sans alerte ; (8) livraison après pause/suppression ou ancienne version sans candidat ; (9) source ou monitor inattendu rejeté ; (10) passage antérieur ignoré ; (11) réception avant l'état initial différée, rejouable après celui-ci ; (12) événement en attente annulé lors d'une pause ou révision.

La comparaison d'une citation au diff prouve seulement la cohérence du résultat enregistré. Elle ne prouve pas la vérité de la page, la pertinence de toute demande ni la résistance du juge aux injections. Les changements autres qu'un ajout explicite restent insuffisants dans cette sonde. Budget : **0 €**, réseau interdit dans l'exécution F2. Sortie : tests locaux, rapport du rejeu, limites et prochaine étape consignés dans le plan 10.

## Résultat F1 — quatre situations contrôlées, 12 octobre 2026

**Les quatre situations passent sur cette page fictive simple.** [Rapport, jugements et différences](../engine-lab/reports/firecrawl-controlled-result-20261012.json). Aucun nouveau modèle ajouté. Firecrawl a effectué lecture fraîche, comparaison et jugement ; la règle de premier état silencieux est appliquée par notre sonde.

| Étape | Fournisseur | Résultat attendu côté application | Verdict |
| --- | --- | --- | --- |
| Deux concerts initiaux | `new`, sans jugement | État de référence, aucune alerte | Conforme |
| Titre de rubrique et accueil modifiés | `changed`, `meaningful=false` | Aucune alerte | Conforme |
| Ajout de Lune Ambrée, 26 novembre 2026 à 20 h | `changed`, `meaningful=true`, ligne ajoutée fournie | Un candidat d'alerte exact | Conforme |
| Relecture identique | `same`, sans nouveau jugement | Aucun doublon | Conforme |

**Audit humain :** le diff ne montre que les deux changements de texte lors du bruit ; le juge explique correctement leur absence d'intérêt. Pour l'ajout, artiste et date correspondent exactement à la fixture gelée ; la preuve est présente dans `meaningfulChanges` et le diff. Les quatre passages sont `completed`, coûts confirmés : 1 + 2 + 2 + 1 = **6 crédits**. Compte : 993 → 987, cohérent avec les passages, sur 12 réservés. Total Firecrawl F0+F1 : 13 crédits, aucun paiement ni recharge. Cinq tests locaux de sonde réussis.

**Arrêt :** monitor confirmé en pause sans passage courant ; champ `nextRunAt` conservé comme dans F0. Fixture temporaire supprimée. Aucune notification Android/email/Slack envoyée, webhook non configuré. La décision locale aurait produit un seul candidat d'alerte ; ce n'est pas une livraison mobile testée.

**Ce que cela valide :** un mécanisme de changement sur une source connue et accessible, avec filtrage d'un bruit simple, preuve d'ajout et répétition. **Ce que cela ne valide pas :** découverte de sources inconnues, sites bloqués/dynamiques, toutes les langues/demandes, interprétation automatique de la demande initiale, résistance aux injections, qualité statistique ni durée. Garder les échecs de récence F0 : F1 ne les efface pas.

**Y1 annulé par Fabien après F1**, sans appel exécuté. Aucune clé à fournir. Préparation conservée ci-dessous comme historique ; prochain travail : synthèse des capacités et validations restantes selon le plan 10.



## Lot Y1 — annulé par Fabien, jamais exécuté

**Décision courante :** abandon demandé, inscription impossible selon le retour utilisateur et coût jugé élevé. Réserve libérée. Les instructions de préparation ci-dessous sont historiques et ne doivent plus être exécutées ; ne pas demander la clé.

Troisième candidat demandé par Fabien après F0 ; garder l'intention D01 et le même objectif de récence que Firecrawl, sans fournir les réponses trouvées pendant l'audit. [Préparation](../engine-lab/yutori-contract-preparation.json) : un Scout, **un passage maximum**, intervalle d'un jour comme protection puis pause après lecture, aucune notification email/webhook, résultat privé. Le SDK officiel décrit création, suivi, rapports et pause ; aucune commande de déclenchement manuel n'est exposée dans l'interface examinée. Ne pas inventer de mode identique à Firecrawl. Un passage initial teste pertinence et sortie, pas récurrence ni nouveauté prospective.

Prix public vérifié : 0,35 USD/passage ; réserve 0,50 USD dans les 2 EUR globaux, pas de recharge ni abonnement. Avant création : clé, crédits du compte, démarrage effectif et arrêt à contrôler. `/v1/usage` documente les limites et l'activité, pas un solde monétaire : ne pas le présenter comme une preuve de gratuité. `YUTORI_API_KEY` absente au début de ce tour ; ajout demandé pendant F1. Figer le contrat d'exécution après ces vérifications, sans appeler le service payant sur la seule préparation. Références : [tarifs](https://yutori.com/pricing), [SDK officiel](https://github.com/yutori-ai/yutori-sdk-python/blob/main/api.md).

## Lot F1 — changement contrôlé, contrat gelé avant exécution

Validation autorisée par Fabien le 12 octobre après présentation des quatre étapes. Une page fictive publique temporaire, même URL, quatre versions : état initial avec deux concerts ; modification de rubrique/présentation ; ajout de Lune Ambrée le 26 novembre 2026 ; relecture identique. [Contrat et empreintes](../engine-lab/firecrawl-controlled-contract.json). Attendu côté application : 0/0/1/0 alerte. La première découverte est un état initial, quelle que soit l'étiquette fournisseur. Le positif doit fournir le nom et la date exacts ; un changement brut sans bon jugement ne suffit pas. Aucun email/Slack/push, lecture API uniquement.

Firecrawl fournit lecture fraîche (`maxAge=0`), comparaison Markdown, juge et historique. Notre sonde change la fixture, vérifie son contenu public exact, borne les passages, conserve les preuves et applique uniquement la règle de premier état silencieux. Aucun second modèle, ni règles adaptées après résultat pour faire réussir le test. Hébergement proposé : réponse HTML d’une URL Webhook.site temporaire, sans donnée privée, expiration 1 h, historique entrant désactivé, suppression à la fin. Vérifier création/mise à jour/lecture avant création du monitor ; si l'accès gratuit est refusé, ne pas payer.

Plafond : un monitor, quatre passages, **12 crédits disponibles** (coût documentaire attendu au plus 8 pour une page simple et son juge), sans recharge, dans les 2 EUR globaux déjà autorisés. Pas de nouvelle recherche F0. Programmation distante à deux jours : contrôler la date puis exécuter les passages manuels ; pause finale vérifiée. Échecs de création/déclenchement à issue inconnue jamais répétés. Pour les 429, respecter le délai indiqué ; seules lectures et pause idempotente peuvent être reprises de façon bornée. Arrêter si le budget, les preuves ou l'intégrité de la fixture ne sont pas vérifiables. Le test n'évalue ni l'indexation Web, ni tous les sites réels, ni la livraison HMAC.


## Résultat F0 — 12 octobre 2026

**Deux passages terminés sur le même suivi D01**, sans modifier la requête ni le goal. Premier passage : trois pages, deux retenues, une écartée, cinq crédits. Second passage : 3 pages, 0 nouveau résultat selon le prestataire, 2 crédits. Total rapproché du compte : **7 crédits**, de 1 000 à 993, sur 30 réservés ; aucun paiement/recharge. [Rapport audité](../engine-lab/reports/firecrawl-result-20261012.json).

**Audit de pertinence, distinct du transport :**

- Le guide Sortiraparis est pertinent pour découvrir les concerts, mais sa `datePublished` est le 20 février 2023 et sa `dateModified` le 6 octobre 2026. Le juge appelle cette dernière une publication et considère les concerts listés comme de nouvelles annonces. Ce raisonnement ne prouve pas leur nouveauté : échec du critère de dates, malgré une confiance élevée.
- La page officielle Guy2Bezbar confirme un concert le 17 avril 2027. Elle ne justifie pas la récence invoquée par le juge. Un article supplémentaire, retrouvé pendant l'audit manuel et publié le 6 octobre, annonce la billetterie des 6/7 octobre : c'est donc un candidat récent crédible, mais cette corroboration n'était pas fournie par Firecrawl. Cela ne valide pas une vérification automatisée et ce fait précède l'activation.
- L'annonce Leboncoin est écartée par le prestataire. Sa lecture indépendante a échoué : on ne comptabilise pas cette exclusion comme une validation sémantique complète.

Sources : [guide](https://www.sortiraparis.com/scenes/concert-musique/guides/289810-accor-arena-paris-prochains-concerts-a-ne-pas-manquer), [page officielle](https://www.accorarena.com/fr/programmation/guy2bezbar--ffc3f), [article complémentaire](https://www.sortiraparis.com/scenes/concert-musique/articles/353381-guy2bezbar-en-concert-a-paris-bercy-en-avril-2027). Dates et lectures conservées dans les preuves locales. Le premier passage sert d'état initial, pas de notifications utilisateur.

**Exploitation observée :** création sans passage initial ; exécution en pause refusée HTTP 409 ; activation avec cron éloigné, lancement manuel et pause fonctionnent. `nextRunAt` reste renseigné en pause : le contrôle initial de nullité était une hypothèse incorrecte de notre sonde. Format horodaté fournisseur adapté à Python 3.9. Des HTTP 429 ont persisté malgré un espacement de 23 secondes ; cet espacement fixe ne garantit donc pas le respect du quota. Les opérations refusées ont été inspectées séparément après le délai indiqué. Une future intégration devra traiter les limites par opération et `retry_after_seconds`, avec reprise sûre des lectures et de la pause ; jamais répéter aveuglément un lancement à issue inconnue. Aucun résultat inconnu n'a été relancé. Diagnostics et refus conservés, aucun autre monitor créé. Cinq tests locaux passent.

**Arrêt vérifié :** statut `paused`, aucun passage courant. La date du 14 octobre reste enregistrée mais le suivi est en pause ; ne pas dire que ce champ est nul. Email et Slack non tentés, aucun webhook ni push Android. Firecrawl a en revanche enregistré une notification dans son propre tableau de bord. Livraison HMAC, événement positif après activation et test dans la durée restent non validés.

**Conclusion limitée à ce cas :** Firecrawl livre des résultats exploitables et un jugement explicable, mais la détection de « nouvelle annonce » reste insuffisamment prouvée. Un bon lien ne suffit pas à valider tout le moteur ; un faux positif ne prouve pas que tous les usages échouent. Aucun fournisseur de production retenu, aucune seconde IA ajoutée.


## Diagnostic complémentaire D0 — 12 octobre 2026

**Clôture :** un suivi, deux passages Monitor terminés, un appel Search de comparaison, zéro notification. Les deux livraisons Monitor ont été vérifiées ; le suivi est confirmé en pause, sans programmation. D02 non exécuté. Le contrat original reste en échec ; ce diagnostic ne change pas ses critères.

La lecture distante confirme que `outputSchema` est identique au schéma envoyé et conservé à la création. La [documentation Monitor](https://exa.ai/docs/monitors/quickstart) promet une sortie conforme, sans exception documentée pour un résultat vide ou un premier passage. Le second passage retourne le même objet `changeCount/changes/targets` que le premier, au lieu de `facts`. Cela écarte une différence limitée au premier passage ; une branche interne « aucun changement » reste une hypothèse. Nous ne savons pas confirmer la cause côté prestataire.

La [recherche classique](https://exa.ai/docs/reference/search), avec exactement la même requête et le même schéma, retourne 10 liens et 9 faits au bon format. Elle ne dispose pas de l'historique/filtrage temporel du Monitor : cette comparaison isole partiellement le problème de format, sans prouver que le Monitor aurait dû notifier les événements anciens.

**Audit des neuf faits :** les artistes et dates de concert concordent avec la [programmation officielle](https://www.accorarena.com/fr/programmation?view=calendar&year=2027). Les liens cités ont tous été ouverts (lecture Web puis lecture HTTP publique lorsque nécessaire). Quatre dates de publication affirmées au 12 octobre contredisent les métadonnées de leurs pages : Stevie Wonder, 9 septembre ; Gradur, Hatsune Miku et Noah Kahan, 27 juin. Une cinquième, Korn, n'est pas étayée par la page examinée. Les quatre autres dates de publication sont étayées par les articles cités : The Strokes, 13 avril ; Niall Horan, 24 mars ; JB Mpiana, 8 septembre ; Angèle, 2 octobre. Les neuf dates de concert correctes ne signifient donc pas neuf nouvelles annonces. La date déclarée par un site est elle-même une preuve à qualifier, pas une garantie absolue de première publication.

Tous les champs étaient accompagnés de `confidence=high`, y compris les dates contestées. Un score de confiance ne suffit donc pas à déclencher une alerte. Notre audit manuel mesure cette limite ; il n'est pas une solution de production. Aucun de ces neuf faits n'est validé comme une nouveauté postérieure à l'activation.

**Coût :** 0,007 USD estimé dans la réponse Search. Solde gratuit affiché : 19,99 USD avant D0, 19,98 avant la comparaison, 19,97 après. La baisse arrondie totale est 0,02 USD ; ce n'est pas une facture détaillée. La réserve comptable de 0,60 USD est entièrement allouée et ferme ce lot, sans représenter le montant dépensé. Aucun paiement ni recharge. Budget autorisé global inchangé : 2 EUR.

**Décision technique réversible :** ne pas retenir Exa Monitors comme fondation validée tant que l'écart de sortie n'est pas expliqué. Conserver Search comme candidat de recherche ; sa synthèse de dates n'est pas fiable telle quelle pour prouver la nouveauté. Passer à la comparaison Firecrawl déjà prévue, sans entreprendre une longue reconstruction de moteur maison. [Résultats et audit ligne par ligne](../engine-lab/reports/monitor-result-20261012.json).

### Reproduction pour le support Exa — brouillon, non envoyé

On 2026-10-12, POST /monitors accepted and persisted an object outputSchema with required `facts` array (title, summary, eventDate, publishedAt, stage). GET confirmed the schema remained unchanged after status-only PATCH requests. With status active and trigger=null, two manual runs completed but both returned `output.results=[]` and `output.content={"changeCount":0,"changes":[],"targets":[...]}`, without `facts` or grounding. The same query and schema worked with POST /search. Is an undocumented empty-result output contract expected? How is the first-run time window chosen? Separately, triggering a paused monitor returned HTTP 400 "Monitor must be active to trigger", contrary to the quickstart and OpenAPI. Please clarify supported behavior. No credentials or webhook URLs are included here; run IDs remain in local evidence and can be provided privately if necessary.

### Lot F0 — contrat gelé avant exécution le 12 octobre 2026

Clé acceptée, 1 000 crédits disponibles et aucun monitor préexistant. [Contrat exact](../engine-lab/firecrawl-contract.json) et empreinte enregistrés avant création. Même intention D01, même requête prévue, une cible search, fenêtre 7 jours, dix résultats, juge activé. Le goal demande une preuve datée récente et distingue publication/concert/vérification ; la fenêtre passée sert au diagnostic de pertinence, pas à déclarer ces annonces nouvelles après activation. Premier passage = état initial, zéro notification utilisateur.

Un seul suivi, deux passages maximum, 30 crédits réservés sur le solde disponible ; estimation documentaire maximale 24 crédits pour deux recherches et vingt jugements. Aucun abonnement/recharge, plafond financier global 2 EUR inchangé. La création exige une programmation : choisir un cron à deux jours, confirmer sa prochaine date, puis pause. Un éventuel passage lancé à la création est compté ; dans ce cas arrêter le diagnostic et ne pas ajouter de passage. La création n'est jamais répétée après une issue inconnue. Avant chaque passage manuel : contrôle du budget, activation avec date éloignée, déclenchement unique, pause immédiate et vérification distante. Pause finale dans tous les cas ; résultat incomplet/panne distinct d'un résultat vide.

**Limite explicitée avant test :** lecture authentifiée des résultats par API. Aucun webhook configuré : le secret HMAC de compte Firecrawl est distinct de la clé API et n'est pas disponible localement. Ce lot ne validera donc pas la livraison webhook, ni Android, ni la surveillance prospective. Cet ajustement évite de prétendre avoir validé une chaîne complète ou d'ajouter une seconde IA. Firecrawl fournit recherche, lecture, jugement et historique ; le laboratoire borne les appels et audite les preuves.

Acceptation : source pertinente et lisible, échecs distincts du silence, aucune date affirmative fausse, répétition observée sans notification, état final en pause et consommation rapprochée. Deux passages sans vrai nouvel événement ne valident pas le rappel. Toute modification ultérieure des critères serait un nouveau lot. Les inconnues création/déclenchement sont testées comme telles, sans transposer Exa. Références : [recherche](https://docs.firecrawl.dev/features/monitoring-web-scale), [arrêt/coûts](https://docs.firecrawl.dev/features/monitoring), [sécurité webhook](https://docs.firecrawl.dev/webhooks/security).

**Calibration d'accès, avant recherche :** création réussie, aucun passage initial, solde inchangé. Le statut `paused` est confirmé mais `nextRunAt` reste renseigné ; le contrôle exigeant sa nullité était trop strict. Un HTTP 429 documente une limite de trois requêtes/minute pour l'opération concernée. Les traces initiales sont conservées. Sans recréer le suivi ni changer le goal, tester explicitement un déclenchement manuel **en pause**, puis attendre 22 secondes entre lectures du passage. En cas de refus, arrêter cette tentative ; pas de relance payante automatique. Les deux passages/30 crédits restent les mêmes plafonds, pas un nouveau lot.

Le déclenchement en pause retourne HTTP 409 explicite, aucun passage et aucun crédit consommé. Procédure définitive pour les deux passages : activation avec cron distant vérifié → lancement unique → pause ; espacement de 22 secondes entre mutations pour respecter la limite observée. Aucun refus HTTP n'est compté comme un passage terminé. Aucune création supplémentaire ni extension du plafond.


## Résultat D0 — 12 octobre 2026

[Bilan enregistré](../engine-lab/reports/monitor-result-20261012.json). Clé acceptée ; un suivi créé et un passage terminé. Webhook authentifié, cohérent avec la réponse API ; rejeu ignoré localement. État final : pause, aucun prochain passage. Zéro notification utilisateur, D02 non exécuté.

Deux déclenchements en pause ont reçu HTTP 400 « Monitor must be active to trigger » ; le second était un diagnostic explicite du premier refus. Une correction limitée au mode actif sans planification a permis un passage. La création confirme le schéma `facts` demandé ; le résultat contient à la place `changeCount=0`, `changes=[]`, trois `targets` et `results=[]`, sans grounding. Le contrôle de format échoue, sans modifier les attendus. Ces URLs cibles ne constituent pas trois faits trouvés ni trois preuves validées. Pertinence, rappel et nouveauté : non évalués.

Réserve comptable consommée : 0,40 USD sur 0,60 USD de réserve maximale, **pas une facture**. Débit exact non rapproché. Arrêt au contrôle de sortie ; prochaine étape : expliquer l'écart de contrat avant d'étendre l'essai. Le bloc de préparation ci-dessous conserve le contexte antérieur à l'ajout de la clé.

## Lot D0 préparé le 12 octobre 2026 — contrat Exa Monitors

Configuration gelée avant implémentation dans [monitor-contract.json](../engine-lab/monitor-contract.json), avec [empreinte](../engine-lab/monitor-contract.sha256). Deux demandes synthétiques issues des familles validées : **D01**, nouveau concert annoncé à l'Accor Arena de Paris ; **D02**, annonces et débats sur la politique du logement en France. Le choix teste respectivement lieu/type/date et suivi général/distinction proposition-décision. Il ne prétend pas mesurer la popularité de ces sujets. France et français seulement pour D0 ; anglais et autres pays restent à évaluer.

**Répartition :** Exa effectue recherche, synthèse, références, historique et livraison. Le programme local borne les appels, recueille les résultats, vérifie format/signature et journalise sans doublon. Aucun second LLM ni moteur de classement. L'analyse humaine des preuves après exécution mesure la qualité ; elle n'est pas présentée comme une étape automatisée de l'application.

**Acceptation :** zéro signature invalide acceptée, doublon de livraison enregistré comme événement distinct ou panne présentée comme succès. Chaque fait affirmé doit avoir des références lisibles, conformes au sujet, avec des dates et un stade exacts ; sinon « insuffisant » ou « incorrect ». Résultat vide : transport éventuellement valide, rappel non concluant. Le premier passage constitue l'état initial, sans alerte de nouveauté. Deux passages rapprochés ne prouvent pas la découverte prospective. Aucun push utilisateur dans D0.

La [page officielle Stevie Wonder](https://www.accorarena.com/fr/programmation/stevie-wonder--1b0e7), lue avant activation, indique un concert le 15 octobre 2026. Sa date de publication n'est pas établie : date future du concert ne signifie pas nouvelle annonce. Pour D02, la recherche publique retrouve une [publication de juillet](https://www.vie-publique.fr/loi/303813-relance-logement-projet-de-loi-jeanbrun), mais sa lecture directe est bloquée. Aucun état actuel ni rappel prospectif ne sera validé à partir de cet extrait ancien. Références et exclusions exactes dans le contrat.

**Accès observé :** compte Exa connecté, Free tier, 19,99 USD disponibles, aucun moyen de paiement. La clé Exa n'était pas présente dans le fichier local lors de la préparation ; son ajout a été demandé à Fabien. Aucune autre clé n'est recherchée dans ses fichiers personnels.

**Limites :** deux créations, deux déclenchements par suivi, dix résultats maximum par passage, sans Contents ni relance automatique. Quatre passages coûtent théoriquement 0,06 USD au tarif de base ; réserver conservatoirement **0,60 USD** pour les six opérations de création/déclenchement. Conserver les 0,0476092 USD OpenAI et une marge historique Exa de 0,02 USD d'après l'affichage arrondi. Le plafond autorisé reste 2 EUR cumulés. Le test utilise les crédits gratuits ; aucun abonnement/recharge. Rapprocher les compteurs après exécution, sans confondre réserve et facture.

La [spécification Exa](https://exa.ai/docs/exa-spec.json) exige un webhook à la création. Une boîte [Webhook.site](https://docs.webhook.site/api/tokens.html) temporaire peut servir à D0 : expiration demandée d'une heure, uniquement demandes fictives et informations publiques, aucune donnée utilisateur ni clé. L'URL de consultation et le secret de signature restent dans les fichiers locaux ignorés. Cette boîte est consultable par qui connaît son URL : elle ne convient pas à la production. La sonde vérifie localement la signature des livraisons ; elle ne valide pas un serveur public qui rejetterait les messages avant de les accepter.

**Arrêt — corrigé après essai :** aucun `trigger` lors de la création ; contrôler `trigger=null` et `nextRunAt=null`. La documentation annonçait un déclenchement manuel en pause, mais l'API l'a refusé (HTTP 400). Le passage a fonctionné en état actif sans programmation ; la pause et l'absence de prochaine exécution ont ensuite été vérifiées. Une issue réseau inconnue conserve sa réservation et interdit la répétition automatique. Registre persistant, limite globale de requêtes, date d'expiration et contrôle de l'empreinte du contrat avant lancement. Pas d'essai si clé absente ou contrat expiré.

**Livrable :** rapport séparant transport, qualité sémantique, coût et durée. La signature correcte ne prouve pas la véracité d'une réponse. La sonde ne valide ni admission en langage naturel, ni notifications Android, ni surveillance de sept jours. Un blocage d'accès n'est pas un échec de pertinence.

> **Révision d'ordre du 12 octobre 2026.** Ce protocole reste le cadre des essais D/P/S. Le [plan 10](../docs/10-plan-de-validation-technique.md) gouverne le travail courant : audit actualisé, puis fiches exactes, seuils et coûts avant appels. Une seule intégration commence, Exa Monitors pour D ; Firecrawl sert à P ou à une comparaison D justifiée. L'essai de contrat court précède les sept jours d'observation ci-dessous. Un passage Monitor manuel a été exécuté le 12 octobre ; aucun test de durée. Le plafond de 2 € est cumulatif, pas renouvelé par lot.

Version de travail du 4 octobre 2026, état actualisé le 9 octobre — **deux recherches Exa et un test Firecrawl Search public exécutés (voir jeu d’essai 05) ; comparaison ponctuelle PRIX-01 seulement, intégration API Firecrawl et suivi récurrent non exécutés**. Ce protocole applique le [plan de validation](../docs/10-plan-de-validation-technique.md), les [règles d'alerte](../docs/05-regles-des-alertes.md), les [portes de qualité](../docs/09-qualite-et-publication.md) et la [comparaison documentaire des fournisseurs](02-alternatives-au-moteur-yutori.md). Il évalue une capacité technique sur des demandes libres ; les sujets retenus pour l'essai ne deviendront pas des catégories obligatoires de l'application.

## La question exacte

Pour une demande exprimée librement et clarifiée, pouvons-nous découvrir des faits nouveaux, prouver qu'ils correspondent à la condition annoncée, éviter de les répéter et le faire à une cadence et un coût soutenables ? La réponse doit être **par type de surveillance et par portée démontrée**, pas « tous les besoins du monde sont couverts ».

Trois mécanismes doivent être évalués séparément ; leur comparaison porte sur l'adéquation à la demande et le coût complet, pas sur un classement unique de fournisseurs :

| Voie | Fait à détecter | Candidats comparables | Risque principal |
| --- | --- | --- | --- |
| **D — Découverte** | Une nouvelle page ou annonce pertinente apparaît sur le Web ; la source précise n'est pas forcément connue à l'avance. | [Exa Monitors](https://exa.ai/docs/monitors/quickstart) et [Firecrawl Monitor `search`](https://docs.firecrawl.dev/features/monitoring-web-scale) sur les mêmes demandes. Yutori retiré des essais sur décision de Fabien. | La bonne page ne remonte jamais dans la recherche ; une absence de résultat ne prouve pas une absence d'événement. |
| **P — Page ou source connue** | Une information change sur une page ou un flux déterminé. | [Firecrawl Monitor `scrape` ou `crawl`](https://docs.firecrawl.dev/features/monitoring) et, si la source le permet, accès direct à une API ou un flux . | Changement de présentation, accès bloqué, extraction incorrecte ou page surveillée qui n'est pas la source probante. |
| **S — Donnée structurée évolutive** | Une valeur, un état officiel ou une prévision évolue pour un lieu et une période définis. | API ou flux spécialisé autorisé ; pour la météo quotidienne, évaluer par exemple les données horaires et la probabilité de pluie d'[Open-Meteo](https://open-meteo.com/en/docs), sous réserve de ses conditions d'utilisation. | Confondre prévision et fait certain, mauvaise localisation ou mauvais créneau horaire ; une source gratuite pour l'essai peut ne pas l'être pour une exploitation commerciale. |

Exa documente surtout les **nouveaux résultats** de recherches répétées ; il ne sera pas déclaré défaillant parce qu'une page connue change sans créer un nouveau résultat. Firecrawl distingue explicitement recherche de nouvelles pages et comparaison de pages existantes. [Exa](https://exa.ai/docs/monitors/quickstart), [Firecrawl](https://docs.firecrawl.dev/features/monitoring). L'admission d'une demande impossible, la clarification, le compte utilisateur et la notification Android sont des responsabilités de notre service et seront testées séparément ; leur absence dans une API de recherche n'est pas comptée comme un défaut de cette API.

## Constitution du corpus avant les appels

Les fiches contiendront les **formulations exactes** de demandes et une justification de sélection. Pour éviter les exemples sortis du chapeau, partir des familles grand public retenues ci-dessous ; les exemples concrets peuvent provenir de Fabien, de proches ou être synthétiques et signalés comme tels, sans en déduire leur popularité. Le métier de Fabien ne justifie pas de privilégier les offres Android ou l’actualité logicielle Compléter au besoin par des cas techniques explicitement étiquetés « synthétiques » et dérivés des exigences du document 09. Une demande ne sera pas choisie parce qu'un fournisseur la réussit déjà. Une seule période de calibration peut corriger une erreur de configuration ; les demandes, critères et seuils d'évaluation seront ensuite figés avant l'observation comparée.

Le jeu initial doit rester **petit, transversal et choisi pour révéler des limites**, pas pour cocher un quota de sujets ou de sources. Sa taille et la répartition entre découverte D, page connue P et donnée structurée S seront fixées dans les fiches avant les appels, selon le coût estimé et les mécanismes à départager. Il couvrira collectivement : événement précis et suivi général, changement d'une valeur ou d'un état, français et anglais, France et hors de France, source connue ou inconnue, fait fréquent et fait rare. Des demandes ambiguës et non vérifiables servent au parcours d'admission et **ne créent pas de surveillance payante**. Même un pilote réussi ne permettra aucune estimation statistique de la qualité mondiale. La [matrice de faisabilité](04-matrice-de-faisabilite-et-couts.md) précise ce que chaque mécanisme peut et ne peut pas promettre.

Les familles discutées le 4 octobre 2026 servent à **varier les sujets d'essai**, sans classement chiffré prouvé ni obligation de toutes les couvrir : prix/promotions/retours en stock ; nouvelles occasions (logement, occasion, emploi) ; météo utile au quotidien, telle que pluie prévue pendant le séchage du linge, autant que risques officiels ; événements et billetterie ; prix/disponibilités de voyage ; évolution d'un sujet suivi, notamment actualité sociale ou politique. Pour cette dernière famille, distinguer une décision ou annonce définie d'une demande subjective comme « préviens-moi si quelque chose d'important se passe ». Les sujets ne déterminent ni la méthode technique ni l'admission : les fiches précisent la condition vérifiable et, **lorsqu'elle est identifiable**, une source de référence. Une demande concernant un compte privé, comme un prélèvement bancaire ou un colis individuel, relève de l'admission et requiert un accès autorisé ; elle ne devient pas surveillable par la seule recherche Web.

Chaque fiche de demande comporte : identifiant, origine (personne ou cas synthétique), phrase littérale, langue, lieu et fuseau, condition confirmée, exclusions, type D/P/S, portée et sources plausibles, cadence envisagée, raison de pouvoir ou non la suivre, risque de fausse alerte, besoin de clarification, date de gel. Les demandes des proches ne sont enregistrées qu'avec leur accord ; supprimer ou anonymiser tout détail personnel inutile.

## Vérité de référence indépendante

Une fiche d'événement de référence note : identifiant canonique du fait, source primaire ou preuve publique, URL, texte ou capture minimale autorisée, date de l'événement si connue, date de publication visible, première observation indépendante, éventuelle correction, critères de la demande qu'il satisfait et jugement humain signé. Les faits antérieurs à l'activation sont marqués **état initial**, jamais nouveautés. Une reprise éditoriale du même fait garde le même identifiant canonique. Les URLs sont des preuves, pas à elles seules des identités d'événement.

Pour la voie D, observer prospectivement une ou plusieurs sources de référence **définies avant l'essai** et leurs nouveautés pendant sept jours ; relever aussi les résultats que les fournisseurs proposent hors de ces sources. Le rappel mesuré concerne seulement les événements effectivement connus dans cette portée. Si aucun événement positif n'arrive, la mesure de rappel est « non concluante » ; on n'invente pas un succès sur la base du silence. Un fait publié avant l'essai peut servir au test de configuration, sans être compté comme découverte prospective.

Pour la voie P et la logique du service, une page publique HTTPS contrôlée par le projet pourra recevoir des changements datés : état initial, vraie nouveauté, changement cosmétique, correction matérielle, retrait et panne. Cette page sert à tester extraction, états et notifications ; **elle ne prouve pas** qu'un moteur de recherche découvrira une page nouvellement publiée ou non indexée. Aucun site tiers ne sera modifié pour l'essai.

Pour la voie S, archiver chaque réponse structurée avec son horodatage de publication ou de mise à jour, sa zone et sa période de validité. Si la donnée est une **prévision**, le fait déclencheur est le franchissement d'un seuil de prévision explicite, pas la certitude que le phénomène arrivera. Pour la pluie et le linge, définir avant l'essai le lieu, la fenêtre de séchage, la variable utilisée, le seuil, le délai de prévenance et la règle évitant une notification à chaque fluctuation de prévision. Comparer ensuite prévision et observation pour mesurer la qualité pratique, sans réécrire rétroactivement la condition annoncée.

## Configuration et déroulement identiques

1. Documenter les accès, tarifs du jour, crédits gratuits, limites de dépense et conditions d'usage. Ne créer aucune surveillance payante sans la fiche de coût prévue au document 07. Mettre les clés uniquement dans les secrets locaux du service de test, jamais dans Android, le dépôt ou la conversation.
2. Pour chaque demande D, confirmer une même intention et une même cadence quotidienne. Exa reçoit la recherche conforme à son API. Firecrawl reçoit des mots-clés `queries` et un `goal` ; noter le temps et, s'il y en a, le coût nécessaires à cette conversion. Ne pas améliorer un seul fournisseur après avoir vu ses résultats. Limiter explicitement requêtes et résultats par passage pour borner le coût.
3. Déclencher un passage manuel de vérification de contrat, puis faire tourner les surveillances pendant **sept jours consécutifs**. Collecter les passages programmés, y compris sans résultat, leurs statuts, retards, candidats, preuves et consommations. Les deux fournisseurs sont évalués sur la même période ; noter toute différence de fréquence réelle.
4. Pour chaque demande P, tester la page ou le flux identifié avec Firecrawl et la méthode directe si elle existe et est autorisée. Éprouver les changements contrôlés séparément des événements réels.
5. Pour chaque demande S, tester d'abord un flux ou une API spécialisée, vérifier ses conditions d'usage pour une application publique, puis mesurer la fraîcheur des données, les seuils et les changements de prévision. Ne pas faire payer un moteur de recherche Web pour une donnée structurée disponible directement.
6. Rejouer les cas d'admission, doublon, correction, panne et reprise dans le service avec des sorties enregistrées des fournisseurs. Aucun échec du webhook n'est traité comme « rien de nouveau ». Avant une intégration de bout en bout, vérifier les webhooks et leur signature ; Exa documente une [signature HMAC](https://exa.ai/docs/monitors/quickstart), Firecrawl des [webhooks signés](https://docs.firecrawl.dev/features/monitoring).
7. Yutori retiré des essais par Fabien. Une limite de recherche constatée doit être documentée ; tout remplacement fournisseur éventuel demande une justification explicite, sans relancer Y1.

## Mesures et verdicts

| Mesure | Définition / interprétation |
| --- | --- |
| Pertinence | Candidats pouvant mener à une alerte réellement conformes aux critères confirmés, après lecture de la preuve ; relever chaque faux positif et sa cause. Une mention du sujet n'est pas automatiquement un événement. |
| Faits manqués observables | Faits de référence publiés après activation, dans les sources et la période annoncées, que le moteur n'a pas remontés. Un fait hors portée est consigné séparément. |
| Doublons | Nombre de notifications supplémentaires pour un même événement canonique ; distinguer correction matérielle et nouvelle reprise d'un fait ancien. |
| Délai | Publication visible → première détection → résultat vérifié → notification ; indiquer les dates incertaines et les contrôles dus en retard. |
| Continuité | Passages attendus/exécutés/échoués, absence de résultat, accès refusé, retard, reprise et lacune de couverture. |
| Coût | Création et reformulation, chaque passage même vide, requêtes additionnelles, résultats jugés, extraction, IA, réessais, stockage et livraison ; rapprocher compteurs et factures/crédits fournisseur. |

Pour chaque demande et chaque voie, le verdict est **capacité démontrée dans une portée explicite**, **capacité partielle à annoncer**, **échec**, ou **preuve insuffisante**. Le prix le plus bas ne gagne pas si des faits de référence sont manqués ou si des résultats non prouvés seraient notifiés.

Les blocages immédiats repris du document 09 s'appliquent aux cas contrôlés : aucune fausse notification affirmative, aucun fait ancien présenté comme nouveau, aucun doublon, aucune source inventée et aucune panne masquée en contrôle réussi. Pour les observations réelles, les seuils chiffrés de pertinence, délai et coût par surveillance seront écrits **après gel des demandes et avant le premier essai comparatif** ; ils ne seront pas ajustés pour faire réussir un fournisseur. Avec un petit nombre de demandes et sept jours, tout pourcentage restera une mesure du pilote, pas une promesse produit globale. Si les événements de référence sont trop rares, prolonger ou déclarer le résultat non concluant.

## Conditions d'entrée et sortie de cette étape

**Avant le premier appel fournisseur :** fiches de demandes remplies, faits de référence ou sources de référence identifiés, configuration et cadence gelées, seuils réels chiffrés fixés, limites d'exécution et de crédits documentées, traitement des données vérifié, clés disponibles dans un environnement de test. Exa annonce [10 $ de crédit mensuel sans moyen de paiement](https://exa.ai/pricing) ; Firecrawl annonce [1 000 crédits mensuels sans carte](https://www.firecrawl.dev/pricing). Ces offres doivent être revérifiées au moment de l'inscription ; les crédits gratuits ne sont ni une garantie de capacité de production ni une raison de sauter les limites de dépense.

**En sortie :** tableau de tous les passages, références et décisions humaines, coûts réels, échecs et limites ; recommandation par voie D/P/S, avec besoin éventuel d'un autre fournisseur et promesse V1 révisée. Le protocole prépare une décision de moteur et une tranche Android/service ; il ne vaut pas encore preuve de publication.
