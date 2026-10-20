# Plan de construction du premier moteur

> **Statut au 12 octobre 2026 : document historique du laboratoire du 9 octobre.** Les résultats restent valides dans leur portée ; les anciennes mentions de « prochaine étape » ne gouvernent plus le projet. L'extension de ce moteur et la comparaison de modèles sont suspendues pendant l'audit des services gérés. Voir le [plan courant et son playbook](../docs/10-plan-de-validation-technique.md) et la [comparaison actualisée](02-alternatives-au-moteur-yutori.md). Aucun essai supplémentaire n'est déclenché.

Établi le 9 octobre 2026. Fabien a demandé un prototype rigoureux de toute la chaîne avant d'engager le développement complet. Ce plan détaille les étapes 1 et 2 du [plan général](../docs/10-plan-de-validation-technique.md). Les essais ponctuels Exa et Firecrawl restent des preuves de recherche seulement, conservées dans le [jeu d'essai historique](05-jeu-essai-transversal-v0.md).

## Objectif et limites

Transformer une demande explicite en critères, obtenir des observations sourcées, décider de leur correspondance, conserver un état et produire un événement d'alerte lors d'un changement pertinent. Le résultat doit être reproductible, explicable et borné en coût.

Le premier mécanisme évalué est un seuil de prix sur une offre de produit neuf. AF300EU sous 150 € livraison comprise sert de cas de départ, pas de constante dans le code. Les tests varieront modèle, seuil, langue et état. Il ne s'agit ni de retenir le shopping comme catégorie V1, ni de promettre tous les produits ou marchands.

La chaîne complète s'arrête pour ce lot à un événement enregistré dans un journal local. Android, FCM, comptes utilisateurs, hébergement public, abonnements et surveillance permanente sont hors lot. Une horloge contrôlée permet d'éprouver plusieurs observations sans attendre plusieurs jours. Le test de durée de sept jours prévu avant publication reste nécessaire ultérieurement.

## État et prochaines étapes

Les numéros M1 à M6 désignent les lots de ce moteur, pas de nouvelles phases du projet.

| Lot | Travail | Livrable et condition de sortie | État |
| --- | --- | --- | --- |
| M1 — Contrat d'acceptation | Définir entrées, sorties, preuves, cas, seuils et décisions de poursuite. | [Critères et cas](07-acceptation-premier-moteur.md), avec propositions chiffrées identifiées. | Contrat adopté pour laboratoire ; voir le bilan 09. |
| M2 — Corpus et contrats exécutables | Créer les pages et réponses contrôlées, les séquences horodatées, les attentes et les interfaces. Préparer les cas de réserve avant implémentation. | Fixtures versionnées, manifeste avec empreintes, commande de validation des données ; fiche technique et plafond d'appels avant accès aux fournisseurs. | Corpus et contrats locaux préparés ; accès modèle à résoudre. |
| M3 — Moteur local déterministe | Orchestration, décisions métier, état persistant, journal d'événements, gestion des erreurs et budget ; adaptateurs simulés explicites. | Cas de décision, transitions et incidents verts, redémarrage et répétition sans doublon. Le rapport indique ce qui est simulé. | Exécuté localement, 27 méthodes réussies. |
| M4 — Compréhension et extraction réelles | Brancher un modèle pour l'admission et une méthode réelle de lecture/extraction ; conserver mêmes contrats et mêmes attentes. | Mesures par étape sur corpus contrôlé, preuves consultables, aucun jugement humain substitué en cours d'exécution. | Sonde extraction réelle exécutée ; admission modèle non configurée. |
| M5 — Chaîne Web réelle bornée | Rejouer d'abord les résultats de recherche Exa/Firecrawl existants, lire les candidats automatiquement, puis réaliser une découverte fraîche bornée avec la configuration figée. | Rapport distinguant replay, lecture en direct et recherche fraîche ; consommations observées ; échecs qualifiés. | À faire. |
| M6 — Évaluation et décision | Exécuter les cas de réserve avec configuration gelée, examiner faux positifs, manqués, abstentions et coûts. | Décision : poursuivre ce mécanisme, le restreindre à des sources compatibles ou le retirer du MVP. | À faire. |

La suite immédiate est de compléter l’accès modèle et la fiche de consommation pour M4 ; état détaillé dans le [bilan](09-bilan-premier-moteur.md). Le cas événements/billetterie envisagé précédemment est différé jusqu'au bilan de cette chaîne. Le travail Android reste possible indépendamment avec données de démonstration, mais n'est ni commencé ni livré par ce plan.

## Construction proportionnée

Un seul programme local, avec commande de lancement et rapport lisible, suffit. Son organisation sépare : admission de la demande, acquisition, extraction des faits, décision, comparaison temporelle, stockage et émission simulée. Des interfaces remplacent recherche, lecture, modèle, horloge et stockage. Les décisions métier ne dépendent pas des objets d'un SDK fournisseur.

M2 a retenu Python, sa bibliothèque standard et SQLite pour le laboratoire ; le modèle reste à choisir selon les accès et crédits disponibles. La [fiche d’exécution](08-fiche-execution-moteur.md) relie ces choix au [document d'architecture](../docs/08-architecture-et-donnees.md), les outils déjà disponibles, le coût et la simplicité de reprise. Ce prototype ne fixe pas implicitement la stack du backend final. Pas de microservices, base vectorielle ou composant de réordonnancement spécialisé sans échec observé qui le justifie.

Chaque exécution conserve : identifiant, version du code et des critères, configuration fournisseur/modèle, entrées, empreintes des captures, sorties de chaque étape, causes d'incertitude, durée et unités consommées. Les sorties invalides échouent explicitement. Les secrets sont lus depuis l'environnement et ne figurent jamais dans les fixtures ou rapports. Les essais utilisent des demandes synthétiques et pages publiques ; les traces détaillées ont une durée de travail de 14 jours conformément au document 08, hors corpus synthétique et rapports expurgés. Les preuves historiques déjà conservées ne deviennent pas un stockage illimité de futures collectes.

## Trois niveaux de preuve séparés

1. **Local contrôlé** : entrées et changements connus. Prouve les règles et le comportement, sans prouver les API ni le Web.
2. **Fournisseur sur corpus contrôlé** : compréhension/extraction réellement appelées. Prouve une capacité sur ces exemples, pas sur tous les sites. Si les pages locales ne sont pas accessibles au fournisseur, prévoir un hébergement de test autorisé ou transmettre le contenu à l'extracteur ; cette dernière voie ne teste pas le scrape réseau.
3. **Web réel** : candidats, contenu et disponibilité non maîtrisés. Chaque tentative est conservée, y compris les blocages ; aucune recherche de remplacement silencieuse pour obtenir un succès.

Réutiliser une réponse de recherche est utile pour isoler lecture et extraction, mais sera nommé « replay de recherche ». Seul un nouvel appel réel permet de qualifier une exécution « découverte en direct ». L'audit humain est l'évaluation après exécution, pas une étape nécessaire à une alerte du prototype.

## Limites d'exécution et finances

Ce travail documentaire et les tests locaux n'engagent aucune dépense fournisseur. Le cadre initial reste **0 € facturé**, sans carte, abonnement, achat de crédits ni tâche récurrente. Les 50 € historiques ne sont pas une autorisation de les dépenser.

Avant M4, une fiche versionnée nommera modèle, méthode d'extraction, tarifs vérifiés, crédits disponibles, maximum de tokens, maximum d'appels par étape et maximum global. Aucune exécution réelle si son coût maximal ne peut être borné dans les crédits autorisés. Le coût des échecs et relances est inclus. À défaut de crédits pour un modèle, M3 reste possible ; M4 sera explicitement non exécuté et ne sera pas remplacé par les réponses de Codex dans la conversation.

Bornes techniques proposées pour M5 : 1 recherche fraîche, 10 résultats maximum, 3 pages candidates et 1 lien marchand au plus par candidat, soit 6 URL au maximum ; au plus 1 nouvelle tentative sur erreur transitoire par URL, aucune sur blocage d'accès. Concurrence 1. Le programme s'arrête à la première limite atteinte et déclare la couverture partielle. Les chiffres sont des bornes de laboratoire, pas une cadence ou un quota utilisateur validé.

Une seule configuration fournisseur est implémentée d'abord. Comparer tous les moteurs et tous les modèles multiplie le travail avant même de savoir où se situe l'échec. Un remplacement n'intervient qu'après diagnostic ; il produit une nouvelle version et conserve le résultat précédent.

## Mesurer sans améliorer artificiellement le résultat

Les attentes et sources de référence sont gelées avant l'exécution. Le développeur peut améliorer la configuration sur les cas de travail ; les cas de réserve ne sont pas utilisés pour ces réglages. Un même agent qui les prépare puis implémente ne constitue pas un évaluateur indépendant : le rapport le précise. Après consultation des résultats de réserve, une nouvelle itération exige un nouveau lot de réserve avant toute revendication de généralisation.

Une erreur est qualifiée : demande mal interprétée ; source non trouvée ; lecture impossible ; extraction incorrecte ; règle incorrecte ; état/doublon ; coût/limite ; vérité de référence incertaine. Un problème réparable du prototype doit être corrigé avant d'écarter un fournisseur. Inversement, accumuler des exceptions propres à chaque commerçant est un signal de restriction du périmètre, pas une réussite générique.

La première évaluation comporte une configuration initiale et au plus une itération de correction ciblée avant bilan. Ce point d'arrêt évite les réglages sans fin ; un échec supplémentaire n'établit pas une impossibilité théorique, mais interdit de présenter ce lot comme prêt.

## Arbitrages à présenter à Fabien

Les recommandations de ce plan sont : un mécanisme prix pour diagnostiquer toute la chaîne, des règles déterministes strictes et une obligation de détecter les cas positifs, puis une seule configuration réelle de fournisseurs sous crédits gratuits. Les seuils du document 07 et les bornes M5 ont été adoptés pour ce laboratoire lors de l’accord d’enchaînement, sans valoir engagement de production. Le choix du modèle et le plafond financier détaillé seront présentés en M2 avant les appels, sans réouvrir le cadrage Android.
