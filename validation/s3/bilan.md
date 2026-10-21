# Sprint 3 — bilan du complément du 20 octobre 2026

**Mise à jour du 21 octobre :** les deux défauts d’admission sont corrigés ; A5 passe **9/9**, avec **50 tests Deno** et **29 intégrations**. Les campagnes intermédiaires et coûts sont conservés dans le [rapport de correction](correction-admission.md). Le bilan du 20 octobre ci-dessous reste historique.

**S3 reste ouvert.** Les raccordements locaux, les reprises, l'effacement et les étapes fournisseur ont été complétés et testés. Les projets Supabase/Firebase dédiés manquent pour les essais externes. L'admission A1 réellement exécutée obtient **7/9** ; ses deux échecs restent enregistrés. Aucun moteur général qualifié, aucune surveillance activée, aucune notification FCM réelle annoncée comme reçue.

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
