> Suite livrée le 23 octobre : [S4 — comprendre, confirmer, préparer en arrière-plan](../workflow/bilan.md). Le bilan S3 ci-dessous conserve son périmètre et ses preuves historiques.

# Sprint 3 — clôture technique du 22 octobre 2026

**S3-01 à S3-06 terminés pour le service d’essai borné décrit ci-dessous.** Le moteur général est raccordé et déployé : demande libre → analyse des critères → recherche Web → lecture effective → proposition → confirmation → état initial silencieux → vérifications récurrentes → fait nouveau sourcé → notification Firebase → résultat Android. Cela ne qualifie ni une couverture universelle du Web ni une V1 publiable.

L’application installée sur l’émulateur utilisateur conserve sa session et son suivi existant. Les corrections d’états et de libellés distinguent le suivi réel de la démonstration ; la refonte UI/UX et de l’architecture de l’information reste à concevoir avec Fabien.

| Ticket | Preuve de clôture technique |
| --- | --- |
| S3-01 | Supabase Free EU, Auth e-mail/SMTP OVH, PostgreSQL, Edge Functions, planificateur et FCM Spark opérationnels. Prévision complète de l’essai borné et arrêt financier documentés ; aucun budget mensuel ni achat autorisé. |
| S3-02 | API/propriété/session/reprises Android et contrats éprouvés. 34 exemples de contrat ; 31 scénarios PostgreSQL/Auth/Edge locaux. |
| S3-03 | Analyse libre corrigée : 12/12 cas d’intention finaux, dont un cas logiciel indépendant ; 9/9 jugements de portée/prédicat. Recherche et lecture réelles sans URL produisent une proposition Python. Cycle complet contrôlé avec source fictive : état initial vide, mauvaise ville sans résultat, trois dates distinctes, répétition inchangée sans doublon. |
| S3-04 | Budget réservé avant appel, transactions/résultats/outbox, génération, pause, quota et suppression testés. Compte/source de qualification supprimés par le parcours réel ; comptabilité opaque conservée. Sauvegarde chiffrée cloud restaurée localement : 58 tables / 197 lignes identiques, copie refermée et base jetable retirée. |
| S3-05 | Réception FCM réelle, refus de permission conservant l’accès au résultat, véritable clic dans le volet système, résultat supprimé et reconnexion après expiration locale/rejet réel du refresh vérifiés sur un second émulateur. |
| S3-06 | Preuves, références attendues, échecs et coûts rapprochés ; version utilisateur installée et service d’essai ouvert avec arrêt automatique aux quotas. Observation de sept jours non commencée : elle appartient à S4. |

## Limites réellement laissées

- Surveillance générale toutes les **24 h** : une requête, trois candidats au maximum et **une page lue par passage**, en rotation. Les sources lues figurent dans la proposition partielle ; une source protégée, non sélectionnée ou inadéquate peut conduire à un refus. Un seul fait est évalué par passage.
- Une page nouvellement trouvée sans date de publication vérifiée ne suffit pas à prouver une nouveauté. Un changement constaté depuis une lecture antérieure constitue une autre preuve, explicitement nommée. Publication et date d’événement restent distinctes.
- La demande LibreOffice sans URL a été comprise, mais la page ArchWiki trouvée ne permettait pas la surveillance demandée : refus conservé, aucune activation. La proposition Python prouve découverte/lecture/admission ; son activation n’a pas été simulée. Le cycle positif complet est qualifié séparément sur une source publique fictive maîtrisée.
- Les passages de contrôle ont été accélérés sur le **seul compte fictif**, notamment vers les tours lisant sa source fournie. Ce n’est ni une mesure de couverture Web ni une observation de 24 h ou de sept jours.
- Une place globale et un suivi actif par compte. Pour activer un autre suivi, mettre l’ancien en pause et attendre son arrêt confirmé. L’essai général dispose de **12 crédits gratuits supplémentaires**, sans remise à zéro des 39 unités déjà imputées ; limite totale 51, plafond structurel 64. L’enveloppe modèle déployée reste 0,10 USD, déjà incluse dans les 0,40 USD communs. Aucun achat ou abonnement.
- Le test d’expiration force la date locale et un refresh fictif invalide ; il vérifie le rejet réel Auth et l’effacement du coffre Android. Il ne prétend pas avoir attendu l’expiration naturelle d’un JWT.
- La sauvegarde et sa clé sont sur ce Mac, dans deux dossiers séparés : perte du Mac, bascule vers un nouveau projet cloud et sauvegarde des secrets SMTP/FCM/Vault non couvertes. Destination indépendante et exploitation publique restent S4.

## Vérifications et dépenses

**86 tests de service**, contrôle de types, **34 exemples de contrat**, 31 intégrations locales existantes, tests transactionnels du moteur et des lectures préalables fournisseur. **50 tests unitaires demo / 28 remote**, sans additionner les parties communes comme des cas distincts ; APK utilisateur et APK de tests compilées. Les scénarios Android cloud sont des preuves distinctes. Captures natives examinées sur l’accueil français et le résultat anglais ; aucune validation complète de design, accessibilité ou performance n’en est déduite.

Les échecs restent archivés : fausse occurrence à partir d’une absence, confusion fiction/réalité, citations reformulées, référence produit omise et clarifications redondantes. Les corrections ont été rejouées sur les mêmes attendus ; le cas Thunderbird est un contrôle logiciel indépendant. Les échecs du harnais Android (clic d’accessibilité, redémarrage par instrumentation et mauvaise option d’activation du bootstrap) ont été remplacés par un toucher réel de System UI et une vérification externe de l’écran ; aucun résultat applicatif n’a été fabriqué pour faire passer le test.

**267 appels modèle cumulés, 0,182842 USD estimés** d’usage connu, réservations inconnues nulles au bilan. Borne prudente avec toute l’enveloppe encore autorisée, change et taxes : **0,99587625 € / 2 € cumulés**. Cela reste distinct d’une facture. Firecrawl : **915 crédits gratuits** rapprochés ; aucun abonnement, recharge, Blaze ou budget mensuel. Les objets monitor du compte sont en pause sans passage en vol ; le planificateur applicatif décide des réveils.

Preuves principales : [chaîne cloud](general-cloud-chain.json), [Android cloud](general-android-cloud.json), [déploiement et quotas](general-deployment.json), [budget](general-budget-final.json), [restauration](cloud-backup-restore.json), [types et régressions](general-service-tests.txt), [intention finale](general-intent-format-v3-proof-result.json), [jugements](general-requested-scope-proof-result.json).

**Suite :** conception commune des écrans/design/architecture de l’information, observation S4 sur sept jours avec enveloppe définie, accessibilité/performance/release, confidentialité et sauvegarde indépendante, déblocage Play et publication. GitHub reste différé à la demande de Fabien ; changements conservés dans Git local.

## Historique antérieur à la clôture

Les états ouverts, accès manquants et anciens budgets ci-dessous sont des instantanés historiques, remplacés pour l’état courant par le bilan ci-dessus.

# Sprint 3 — état au 21 octobre 2026

**Le premier pilote réel de changements textuels est vérifié et ouvert pour un essai limité. S3 reste ouvert et la V1 générale n’est pas terminée.** La saisie libre d’événements ou de sujets, la découverte et le jugement général restent dans le périmètre initial ; ce pilote ne les remplace pas.

Le faux message d’échec d’actualisation à la déconnexion est remplacé par une invitation à se connecter. Les lignes privées et le bouton d’actualisation ne sont plus affichés sans compte. Vérification Android en français et anglais, avec un état contenant volontairement une ancienne erreur et un ancien suivi.

La demande `Préviens-moi de tout changement sur https://…` (ou sa forme anglaise `Notify me of any change on https://…`) produit une proposition partielle pour une URL publique, avec critères, exclusions et intervalle. Après confirmation, première lecture silencieuse, comparaison textuelle, preuve sourcée et notification Android. Maximum 20 lignes / 1 000 caractères ; même modification exacte dédupliquée. Ce n’est pas un jugement de pertinence, de nouveauté Web ou de récence de publication.

**Preuve réelle :** source publique fictive modifiée par HTTP, lectures Firecrawl réelles, webhook signé reçu dans Supabase, rapprochement API, résultat créé par le worker et relu sous authentification, notification FCM reçue sur l’émulateur. Pause puis suppression distante confirmées par absence HTTP 404 ; compte, source et données de contrôle retirés. Le second contrôle a été avancé uniquement pour ce suivi de test : aucune heure complète ou observation de sept jours n’est revendiquée. Un réveil automatique du worker par le planificateur cloud a ensuite été observé sans suivi utilisateur actif. [Preuves](page-change-pilot.json).

Un premier essai a exposé la comparaison erronée de l’ordre des champs JSON : corrigée, régression ajoutée et vrai message reçu rejoué avec succès. Un essai préliminaire avait rencontré un HTTP 429 ; arrêt confirmé et nettoyage des deux essais échoués. Aucun échec n’a été transformé en état initial valide.

**Vérifications utiles :** 57 tests Deno, 31 intégrations locales PostgreSQL/Auth/Edge, 49 tests unitaires demo et 27 remote (partie commune : ne pas additionner comme méthodes distinctes), deux parcours de déconnexion FR/EN. APK recompilées, clés privilégiées contrôlées absentes. L’enregistrement du terminal et la réception réelle sont des tests cloud séparés, sans nouvelle demande de code e-mail.

**État laissé :** 29 migrations et quatre fonctions déployées. Un emplacement global, contrôles horaires, jusqu’à 12 crédits gratuits supplémentaires pour l’essai utilisateur. Seule la proposition textuelle explicite est ouverte ; admission IA désactivée, aucune nouvelle clé/permission payante activée. Pause ou suppression retire l’ancien objet fournisseur ; une reprise établit un nouvel état initial. L’interface indique la portée limitée. Le planificateur peut continuer sans cette conversation ni l’ordinateur.

**Budget :** 4 crédits Firecrawl gratuits consommés pendant ce complément, solde 981 avant l’essai utilisateur. Aucun nouvel appel OpenAI/Exa, abonnement, recharge ou plan payant. Borne conservatrice cumulée inchangée : **1,9414335 € / 2 €**, distincte d’une facture rapprochée. [Registre](costs.json). Git reste local ; aucun push.

**Pour finir la V1 :** qualifier indépendamment la compréhension libre, la découverte de sources, la récence et les jugements sémantiques ; vérifier la déduplication entre sources, les cas limites de notification dans la chaîne déployée et la restauration cloud. S4 reste l’observation de sept jours, les contrôles de diffusion, confidentialité/accessibilité/performance et la publication après régularisation du compte Play. Aucun accès supplémentaire n’est nécessaire pour essayer ce pilote. [Plan initial](../../docs/10-plan-de-validation-technique.md).

## Historique des compléments précédents

Les chiffres, gardes fermées et accès manquants ci-dessous sont les états historiques de leurs dates ; l’état courant est celui décrit ci-dessus.

### Bilan du complément du 20 octobre 2026

**Mise à jour du 21 octobre :** les deux défauts d’admission sont corrigés ; A5 passe **9/9**, avec **50 tests Deno** et **29 intégrations**. Les campagnes intermédiaires et coûts sont conservés dans le [rapport de correction](correction-admission.md). Le bilan du 20 octobre ci-dessous reste historique.

**Raccordement cloud du 21 octobre :** Supabase dédié déployé (26 migrations et 4 fonctions). Deux tests Android cloud passent : compte synthétique et enregistrement du terminal, puis réception FCM réelle. Une seule notification envoyée ; son ouverture affiche le résultat simulé attendu. Données de test supprimées et exécution refermée. SMTP OVH temporaire configuré pour les tests : un vrai e-mail reçu, code vérifié via Auth et compte serveur accessible (HTTP 200). La session de test serveur a été révoquée ; la vérification du code via le vrai composant Android, l’écran connecté, le stockage chiffré, la recréation, la restauration après arrêt complet et la déconnexion passent. Le test e-mail a été scindé après expiration du délai de saisie humain ; aucun passage UI continu complet revendiqué. Aucune qualification de surveillance ni observation S4 ajoutée. [Preuves et limites](cloud-connection.json). Aucun nouvel appel OpenAI, Exa ou Firecrawl ; borne cumulée inchangée, 1,9414335 € sur 2 €.

**Historique du 20 octobre — S3 reste ouvert.** Les raccordements locaux, les reprises, l'effacement et les étapes fournisseur ont été complétés et testés. Les projets Supabase/Firebase dédiés manquent pour les essais externes. L'admission A1 réellement exécutée obtient **7/9** ; ses deux échecs restent enregistrés. Aucun moteur général qualifié, aucune surveillance activée, aucune notification FCM réelle annoncée comme reçue.

Travail dans le dépôt existant `/Users/fabienbouvet/Documents/Emploi/Mobile/Android/previens-moi`, sur `main`, sans dépôt distant ni publication. Le socle précédent était enregistré dans `c658b4f` et `914518e`. Le complément de code et tests est enregistré dans **`68c6755`**. Les conteneurs Doklear restent inchangés. Projet local distinct `previens-moi-s3` ; les **26 migrations** ont aussi été réinstallées depuis une base de test vide.

## Travail terminé dans ce complément

- **Android connecté :** questions identifiées, choix et réponses obligatoires, réponses conservées, explications de refus affichées, polling borné. Identifiant, requête et secret de prévisualisation enregistrés **avant** l'appel dans un fichier chiffré Keystore exclu des sauvegardes. Après mort du processus, même analyse ; stockage défaillant ou secret expiré : aucun nouvel appel automatique. Une analyse en attente est distinguée d'une activation en attente.
- **Admission serveur :** appel OpenAI structuré, contrôle des incohérences, séparation compréhension/capacité vérifiée ; file et réservations durables. Réponse perdue : réservation maintenue et absence de réessai aveugle. Coût sous-estimé : gel, résultat de quota explicite. Une interprétation ne peut pas fabriquer une proposition activable.
- **Exécution fournisseur :** création rapprochée par identité durable après réponse perdue, conservation d'un identifiant tardif pendant pause, étapes création/lancement/polling, monitor en pause au repos, réservation par passage, récurrence sans réutiliser l'ancienne réservation, signature et lecture API concordantes avant état initial silencieux ou contrôle inchangé. Arrêt/suppression vérifiés par API avant confirmation ; perte d'un dernier bail visible et capacité gelée.
- **Envois concurrents :** prise d'émission atomique sérialisée avec pause/suppression, trace indépendante survivant à la suppression du résultat, attente des émissions engagées avant arrêt confirmé. FCM data-only, expiration aussi pendant WorkManager, déduplication d'affichage persistée par compte. La prise locale avant affichage peut perdre ce push si le processus meurt à cette frontière ; le résultat reste dans l'application. Aucune garantie exactement une fois revendiquée.
- **Effacement :** fermeture des accès, arrêt et suppression des identités fournisseur, purge des données privées puis suppression Auth via API Admin, reprise après interruption. Les seuls coûts non rapprochés restent en borne minimale sans propriétaire ni plan. Marqueurs rejoués après restauration ; ancienne identité refusée et demandes d'arrêt recréées.
- **Planification :** réveil du worker préparé mais désactivé. Signature unique valable 90 secondes dans la file HTTP ; aucun secret durable n'y est envoyé. Rétention horaire locale conservée. Sauvegarde hors du volume Docker, restauration dans une base jetable distincte et rejeu d'un effacement postérieur au snapshot vérifiés.

## Résultat A1 réel, sans changement du seuil

Neuf cas figés avant les appels : A01–A08 historiques et D01 Accor Arena. Même modèle `gpt-4.1-mini-2025-04-14`, sortie structurée, aucun outil, aucun stockage demandé au fournisseur, aucune recherche ni création de monitor.

**7 réussites / 9.** A01 inclut la marque dans la référence (`Ninja AF300EU` au lieu de `AF300EU`). A04 associe une question de pays à un statut « compris » et une zone vide. Cette incohérence est rejetée par le validateur du service ; elle n'est pas transformée en activation. Les autres cas, dont référence fictive conservée sans inventer de source, compte privé refusé et demande de concerts comprise sans prétendre couvrir toutes les annonces, passent cette exécution.

[Résultats](admission-a1.json) · [Manifeste figé](admission-a1-manifest.json). Le seuil reste zéro critère explicite déformé et zéro activation sans capacité prouvée. Ce passage échoué ne qualifie pas l'admission générale. Il ne mesure pas la découverte, le rappel, la récence ou la déduplication sémantique.

## Vérifications finales

| Vérification | Résultat et portée |
| --- | --- |
| Installation neuve | 26 migrations appliquées dans le seul projet local dédié |
| PostgreSQL/Auth/Edge réels locaux | **29 scénarios réussis**, dont worker HTTP, effacement Auth effectif, vraie concurrence de transactions pause/envoi, files et budget, récurrence, réponse de création tardive et restauration. Les confirmations fournisseur restent des fixtures explicites. [Rapport](local-integration.json) |
| TypeScript/Deno | **31 tests réussis** : admission, reprise/effacement, signatures worker, FCM et fournisseur avec HTTP injecté. Aucun Google ni nouvelle surveillance réelle dans ces tests. [Sortie](typescript-tests.txt) |
| Android unitaires | **49 demo et 27 remote**, une partie commune ; zéro échec. Ne pas additionner comme méthodes distinctes. [Rapport](android-check.json) |
| Android interface | **3 parcours remote en FR et EN**, sombre, police 1,3 : véritable code Mailpit, session et fichier d'analyse chiffrés, questions et refus, doublon local persistant. Les questions sont un port de test explicite, pas une preuve NLP. **5 parcours demo** rejoués |
| Mort du processus | Commande pending relue sans nouvelle activation ; analyse pending reprise depuis son fichier chiffré sans nouvel identifiant ni nouvelle évaluation. Fixtures locales contrôlées. [Commande](android-process-restoration.json), [analyse](android-evaluation-restoration.json) |
| Sauvegarde/restauration | Archive hors du volume de base, base jetable distincte, compte ressuscité refusé après rejeu. Ce n'est pas une sauvegarde cloud indépendante ni un test de récupération des clés. [Rapport](local-backup-restore.json) |
| Contrat/frontières/Lint | 15 opérations, 23 schémas, **34 exemples** ; frontières passantes ; deux APK compilées ; Lint sans erreur/avertissement ; marqueurs de secrets privilégiés absents des APK contrôlées |
| État Firecrawl existant | **985 crédits**, trois anciens suivis Préviens-moi listés en pause sans passage courant. Deux lectures seulement, aucune mutation ni consommation supplémentaire. Portée limitée à la page retournée. [État](firecrawl-readonly-state.json) |

Les captures de `android-phone/` proviennent de l'émulateur. Elles ne prouvent pas API23, tablette, téléphone physique, TalkBack, performance ni qualité de livraison réelle. La première reprise d'analyse a révélé une capture d'interface indisponible au démarrage : le harnais attend désormais un arbre valide avant d'agir ; l'assertion de même identifiant et les limites de polling sont conservées.

## État des six tâches

| Tâche | Situation actuelle | Condition de clôture restante |
| --- | --- | --- |
| S3-01 | Architecture et outillage conservés ; budget cumulé tracé ; contrôle des fichiers de raccordement prêt | Coût d'ouverture complet et sauvegarde indépendante à établir avant exploitation |
| S3-02 | Raccordements Android, questions/polling, reprises chiffrées, service local/Auth/isolation testés | Projet Supabase dédié, sessions et codes e-mail externes ; aucune délivrabilité publique prétendue |
| S3-03 | Admission réellement évaluée, file et étapes fournisseur bornées implémentées | **A1 échoué 7/9** ; découverte, nouveauté/récence, jugement général et déduplication sémantique non qualifiés. Le chemin actuel rejette visiblement un changement sans jugement qualifié ; il ne publie pas d'affirmation générale |
| S3-04 | Transactions, budget, reprises, arrêt/suppression, émission concurrente, purge Auth et restauration éprouvés localement | Webhook public, arrêt/coût/suppression fournisseur réellement confirmés via la chaîne déployée ; limites des messages déjà en vol à qualifier |
| S3-05 | Transport serveur et garde Android, expiration et déduplication testés | Projet Firebase Spark et **réception FCM réelle**, permission refusée, retards, session expirée au clic et résultat supprimé dans cette chaîne |
| S3-06 | Preuves et préparation actualisées ; exécution désactivée | Chaîne réelle et références attendues avant observation ; les sept jours restent S4 |

Le manque d'accès n'efface donc pas les échecs de qualité. Ils restent des conditions explicites de S3 ; aucune case n'est fermée par la seule présence de clés ou par des doubles réseau.

## Budget et état laissé

**Neuf appels OpenAI : 0,004634 USD estimés supplémentaires**, cumul OpenAI **0,0522432 USD**. Enveloppe de la campagne 0,10 USD, sans réessai automatique. Le contrôle préalable conservateur, incluant les réservations historiques entières et des marges change/taxes, reste à **1,3827645 € sous le plafond inchangé de 2 € cumulés**. Ce montant est une borne de contrôle, pas une facture convertie. Exa inchangé à 0,02 USD affiché arrondi ; Firecrawl reste à 15 crédits gratuits consommés et 985 disponibles. Aucun abonnement, recharge, Blaze ou nouveau budget. [Registre](costs.json).

Aucune capacité réelle ouverte, méthodes non qualifiées, admission/fournisseur/FCM désactivés et réveil du worker inactif. Les fixtures de surveillance sont retirées à la fin ; aucun suivi distant nouveau créé. Aucun Git push, aucun déploiement public ni publication Play. [État final](local-safety.json).

## Accès à préparer

Fabien a confirmé l'absence de projets dédiés. Les essais externes nécessitent un projet **Supabase gratuit en UE** et un projet **Firebase Spark**, application Android `fr.previensmoi.prototype.remote`. Les trois fichiers attendus dans `.local/cloud/` sont `supabase.env`, `google-services.json` et `firebase-service-account.json`. Aucun secret à mettre dans la conversation. Le contrôle `scripts/check-cloud-config.py` est purement local ; il n'a constaté aucun de ces raccordements. [Préparation](external-readiness.json).

Le transport SMTP de test Supabase reste limité aux membres de l'équipe et à deux messages/heure ; les premiers essais peuvent utiliser ces adresses. L'envoi public de codes et une éventuelle nécessité de domaine/SMTP restent distincts, sans achat demandé maintenant. Les accès permettront d'éprouver les preuves externes ; ils n'autorisent pas à déclarer les critères de qualité A1/REV satisfaits.

## Complément du 22 octobre — moteur général en cours

Les preuves actuelles et le travail restant sont détaillés dans la section « Avancement moteur du 22 octobre » du [plan courant](../../docs/10-plan-de-validation-technique.md). Les échecs intermédiaires sont conservés. Le contrôle final confirme le pilote utilisateur actif/sain, zéro méthode générale qualifiée dans le cloud et admission générale désactivée.

Les nouveaux composants isolés passent 76 tests de service et 34 exemples de contrat ; deux jugements nouveaux sur des pages réellement découvertes passent après correction de citations inventées. L’intégration complète au service récurrent reste ouverte. Les nouveaux tests cloud d’accès aux résultats passent et leurs identités synthétiques ont été supprimées. Après autorisation explicite de Fabien, la sauvegarde chiffrée et sa restauration locale isolée passent : 53 tables et 147 lignes identiques ; intégrité vérifiée, exécution refermée et base de test supprimée. Le cloud actif reste intact. [Preuve et limites](cloud-backup-restore.json). La perte du Mac et la bascule vers un nouveau projet cloud ne sont pas couvertes. **S3 reste ouvert ; aucune observation S4 n’est déclenchée.**

Le registre financier actualisé totalise 186 appels modèle, 0,118928 USD estimés cumulés et une enveloppe conservatrice sous 1 EUR pour le travail autorisé, sans prétendre disposer d’une facture rapprochée. GitHub reste différé à la demande de Fabien.
