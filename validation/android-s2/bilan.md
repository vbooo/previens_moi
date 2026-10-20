# Sprint 2 — parcours Android simulé

Livré localement le 20 octobre 2026. **S2-01 à S2-06 terminés dans leur périmètre de démonstration.** L’APK permet de parcourir le produit de bout en bout sur un service local simulé ; aucun moteur, compte réel ou serveur n’a été créé. Aucun appel fournisseur ni dépense supplémentaire.

## Livrables par tâche

| Ticket | Réalisé | Preuve principale |
| --- | --- | --- |
| S2-01 | Modèles/ports, erreurs typées, scénarios déterministes, reçus de commande, horloge injectable, quota atomique | 12 tests de contrat du service demo |
| S2-02 | Saisie, suivi ponctuel/continu, clarification, proposition, consentement à la portée réduite ; brouillon DataStore, Navigation3 typée | Tests ViewModel C09/C10, réponse tardive/correction/annulation ; parcours Compose |
| S2-03 | Compte fictif au moment de l’activation, confirmation distincte après connexion, liste et détail | Parcours nominal, activation sans résultat initial, anti-double effet |
| S2-04 | Pause/reprise, remplacement des critères, suppression confirmée ; état confirmé conservé en erreur | Contrats et tests ViewModel ; parcours Compose avec échec puis reprise |
| S2-05 | Résultat fictif sourcé, publication inconnue distincte de l’observation/événement ; ancienne version du plan conservée | C11/C12/C13 ; affichage « ancienne version », publication inconnue |
| S2-06 | Tests de parcours et revue FR/EN, téléphone/tablette, sombre/police 1,3 ; mort réelle du processus | Matrice ci-dessous, captures, JSON du test de processus, revue indépendante |

## Résultats vérifiés

**24 tests unitaires réussis, zéro échec :** 12 contrats, 8 tests de création, 2 tests de rafraîchissement S1 conservés, 2 tests des mutations de suivi. Les assertions portent sur les comportements observables et invariants, pas sur le détail des composants Compose.

**5 parcours Compose distincts**, exécutés intégralement dans chacune des trois configurations suivantes, soit **15 exécutions réussies** lors de la matrice finale :

| Configuration | Résultat final | Journal |
| --- | --- | --- |
| Téléphone API 36 ARM64, 1080 × 2400, français, clair, police 1,0 | 5/5 | [Téléphone FR](phone-fr-tests.txt) |
| Même téléphone, anglais, sombre, police 1,3 | 5/5 | [Téléphone EN](phone-en-dark-large-tests.txt) |
| Tablette API 35 ARM64, 2560 × 1600, français, clair, police 1,0 | 5/5 | [Tablette FR](tablet-fr-tests.txt) |

Les cinq parcours couvrent création/recréation/retour/résultat, clarification et consentement partiel, pause échouée puis reprise/modification/suppression, panne distincte d’absence de résultat, refus simulé puis correction et brouillon conservé.

**Mort réelle du processus : réussie.** Brouillon préparé dans l’interface ; application mise en arrière-plan ; ancien PID absent après `am kill` ; relance de la tâche avec nouveau PID. Brouillon présent, réinitialisation expliquée, ancienne proposition non confirmable, aucune activation automatique. [Preuve structurée](process-restoration.json) · [Capture restaurée](process-restored.png). Le test utilise l’application débogable sur un émulateur dédié et efface ses données demo au début. Le script prévoit aussi un arrêt sous l’UID demo si Android diffère `am kill` ; ce repli n’a pas été nécessaire lors du passage réussi enregistré.

Compilation APK réussie. **Lint : zéro erreur, zéro avertissement**, six indications de mises à jour de versions gardées informatives. Contrôle des frontières domaine/présentation et manifeste fusionné réussi. Inspection du binaire : **aucune permission INTERNET**. Aucun secret métier dans les sources Android. CI distante non exécutée ; les tests sur appareils restent locaux.

## Couverture des cas d’acceptation

| Cas du document 08 | Couverture exécutée |
| --- | --- |
| C01–C08 | `DemoWatchContractTest` : expiration exacte, rejeu après réponse perdue, conflit de contenu, concurrence, session/version, échec de mutation, source dégradée, suppression |
| C09 | `CreateViewModelTest` : réponse A retardée, correction en B, annulation ignorée par le faux fournisseur ; A ne revient pas |
| C10 | ViewModel et Compose : suivi général, clarification, portée partielle, consentement, compte fictif puis confirmation |
| C11 | Contrat et Compose : activation sans résultat initial ; absence de source dans un scénario ne devient pas refus définitif |
| C12 | Contrat et Compose : pause/reprise sans rattrapage, remplacement, résultat antérieur conservant ses critères |
| C13 | Contrat et Compose : publication inconnue affichée comme telle ; observation/événement distincts |
| C14 | ViewModel, recréation/retour Compose et script de mort réelle du processus ; aucune réactivation automatique |

Cette couverture valide les contrats **du double de service**. Les mêmes assertions devront être adaptées au service réel, complétées par les contrôles d’identité et les essais réseau en S3.

## Captures et revue indépendante

[Accueil FR](../../.impeccable/review/phone-fr-home.png) · [Proposition FR](../../.impeccable/review/phone-fr-proposal.png) · [Détail FR](../../.impeccable/review/phone-fr-detail.png) · [Résultat FR](../../.impeccable/review/phone-fr-finding.png) · [Portée partielle EN sombre, police 1,3](../../.impeccable/review/phone-en-dark-large-partial.png) · [Accueil tablette](../../.impeccable/review/tablet-fr-home.png) · [Détail tablette](../../.impeccable/review/tablet-fr-detail.png).

Le reviewer indépendant a rendu **ship pour la démonstration S2**, à partir de ces sept vues et du code : aucun obstacle matériel constaté. Ce verdict ne certifie pas l’ensemble d’une application de production. Le contrôle documentaire confirme la continuité du Material 3 S1 : palettes et typographie natives, composants standards, largeur centrée de 720 dp, défilement et marges système. Aucun nouveau système graphique ni DESIGN.md créé.

Remarque non bloquante conservée : « Sinon… » / « Otherwise… » dans une proposition ponctuelle pourrait devenir une formulation autonome. Dérive préexistante signalée : PRODUCT.md disait encore qu’aucune capture Android n’existait ; ce document de direction n’a pas été réécrit dans ce lot.

Impeccable a servi de grille de contrôle et de revue des écrans. Ce n’est **ni une dépendance Android, ni une bibliothèque embarquée, ni une technologie à apprendre pour utiliser le code**. La réalisation reste Kotlin/Compose/Material3.

## Échecs rencontrés et corrections

- Lint a détecté une formulation de quantité et d’anciennes ressources S1 devenues inutilisées. Libellé rendu indépendant du pluriel, ressources obsolètes retirées ; Lint passe sans désactivation des règles.
- Les premières passes EN et tablette ont parfois interrogé le nouvel écran avant la fin de l’évaluation ou de la confirmation asynchrone. Les tests attendent maintenant des états visibles précis avec un délai borné et initialisent leur saisie indépendamment. Les cinq parcours complets ont ensuite été rejoués, sans retirer d’assertion métier.
- Le pilote de mort de processus a rencontré un dump UI initial non disponible, puis un tutoriel de stylet du clavier virtuel qui interceptait les frappes. L’outil attend le focus et la saisie effective, isole ce tutoriel sur l’émulateur et vérifie réellement la disparition du PID avant d’annoncer une réussite.
- La protection des évaluations tardives couvre aussi une annulation sans changement de texte : un identifiant d’évaluation empêche une réponse annulée de réafficher une proposition.

## Traçabilité et reproduction

Révision des sources testées : `a3b4465346da034e1e7d268f1f6b61327daa9c04`.

APK : `android/app/build/outputs/apk/demo/debug/app-demo-debug.apk` (généré, non versionné).

SHA-256 : `0c4b339c73a45557a2d692c4f24ab6a783743286b4d2b763ed4804342a5200b7`.

[Résultats structurés](resultats.json) · [Guide et commandes](../../android/README.md) · [Plan actif](../../docs/10-plan-de-validation-technique.md).

Les tests unitaires et de contrats se relancent avec `testDemoDebugUnitTest`, Lint avec `lintDemoDebug`, les cinq parcours avec `connectedDemoDebugAndroidTest`. Pour reproduire la matrice, installer les APK app/test puis régler langue applicative, mode sombre et taille de police avant de lancer `FoundationsTest` via le runner Android. L’argument optionnel `capturePrefix` enregistre les captures natives dans le dossier externe de la demo, récupérable par adb. Les captures sont des preuves inspectées, pas des comparaisons automatiques pixel à pixel.

## Limites et prochaine étape

La saisie n’est pas comprise par une IA ; le scénario choisi pilote la réponse. Capacité 1 et intervalle 24 h sont des fixtures, pas les quotas d’une offre commerciale. Aucun contrôle périodique réel. Comptes/suivis/résultats sont en mémoire, seul le brouillon est durable. Pas de push, réseau de production ou publication.

TalkBack complet, mesures exhaustives contraste/cibles, animation prédictive, appareil physique/performance, API 23 et 37 en exécution restent à traiter. La tablette utilise une colonne adaptée, pas encore une vue maître-détail à deux panneaux. Aucun run GitHub Actions distant. Android Studio 2026.2 est présent sur le poste ; son assistant de première configuration peut encore être à terminer par Fabien. Les builds et émulateurs vérifiés ici utilisent les outils locaux du projet.

**Prochain ticket : S3-01, spécification et chiffrage du service réel.** Choisir hébergement, identité, base et push ; rédiger l’ADR et le contrat HTTP à partir des ports S2 ; calculer la facture et les garde-fous. Soumettre toute nouvelle dépense ou réduction de promesse avant engagement. Ne pas relancer une recherche ouverte de fournisseurs ni raccorder un moteur supposé fiable. Le plafond historique de 2 € cumulés demeure ; **S2 a consommé 0 € d’API fournisseur**.
