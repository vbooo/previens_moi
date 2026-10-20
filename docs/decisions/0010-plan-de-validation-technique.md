# Décision 0010 — Valider le moteur généraliste avant de construire la V1

Date : 3 octobre 2026. Statut : méthode révisée après les corrections de Fabien ; fournisseur non choisi.

## Contexte

La première version de ce plan promouvait des exemples arbitraires en cas de test. La seconde imposait une recherche de besoins français avant de traiter la question technologique. Fabien a rejeté ces deux raccourcis : le produit vise des demandes libres, potentiellement dans tout pays, et la question immédiate est de savoir si un moteur généraliste peut les surveiller avec fiabilité.

## Décision

Suivre le [document 10](../10-plan-de-validation-technique.md) : **(0) auditer et comparer les moteurs récurrents Yutori, Exa et Firecrawl, (1) préparer un banc d'essai transversal avec seuils et faits de référence, (2) essayer la surveillance réelle et contrôlée, (3) décider la promesse et les quotas soutenables, (4) réaliser une tranche Android/service, (5) construire et publier la V1**. Les sujets de test servent à éprouver les frontières du moteur, pas à définir un catalogue de catégories ou de pays autorisés.

La France reste le marché initial déjà validé, sans restriction géographique automatique de la saisie ou des sujets surveillés. Les candidats et tarifs de la [première comparaison](../../validation/02-alternatives-au-moteur-yutori.md) motivent l'ordre des essais, pas l'adoption d'un fournisseur. La première passe documentaire ne dépense rien ; le plafond de 50 € concerne seulement une éventuelle campagne d'essais facturables sur 30 jours et n'autorise aucun achat.

## Conséquences

Le protocole transversal comparant Exa et Firecrawl, puis Yutori sur les cas difficiles, a été rédigé dans [validation/03](../../validation/03-protocole-comparatif-des-moteurs.md). Le 4 octobre, une dérive consistant à chercher une source de prix précise et un quota fixe de cas a été retirée : la [matrice de faisabilité et de coûts](../../validation/04-matrice-de-faisabilite-et-couts.md) teste d'abord les mécanismes, l'admission honnête et le coût par surveillance. Après précision de l'objectif principal de Fabien — monter en compétences Android et montrer une réalisation aux recruteurs sans dépense excessive — la tranche Compose/MVVM/Clean Architecture démarre **en parallèle** du petit essai de surveillance. Elle utilise au départ des données de démonstration identifiées comme telles ; la publication exige un parcours réel éprouvé et un coût d'exploitation borné. Aucun essai réel, quota de production ou promesse de couverture mondiale exhaustive n'est encore validé. Les recherches commerciales par pays sont distinctes de cette validation technologique.


## Révision validée le 12 octobre 2026 — appliquer les conditions de passage

Fabien valide le recadrage après la campagne du 9 octobre : reprendre l'audit des **services complets de surveillance** et la répartition fourni / à développer avant tout nouvel investissement dans le moteur maison. La comparaison du 3 octobre décrivait déjà ces services ; les conditions de passage n'ont pas suffisamment gouverné l'implémentation suivante.

Le document 10 devient la référence unique de progression et contient le playbook de chaque lot : objectif, responsabilités, critères avant exécution, budget, arrêt, preuves et bilan. Les documents existants sont actualisés ; pas de multiplication de plans concurrents. Le laboratoire demeure une preuve historique, sans imposer l'architecture de production. Sa poursuite et le remplacement du modèle ne constituent plus la prochaine action.

La recommandation technique issue de l'audit est de garder Exa Monitors comme premier candidat D et Firecrawl Monitor pour P. Elle reste **une recommandation d'essai**, pas un fournisseur de production validé par Fabien. La prochaine étape prépare le contrat d'essai, sans activer de service récurrent aujourd'hui.

Le plafond de 2 € déjà autorisé reste cumulatif, compte tenu des consommations antérieures. Aucun renouvellement implicite, abonnement ou recharge. Le budget mensuel sera arbitré après mesure. Les recherches, corrections documentaires et choix réversibles nécessaires n'exigent pas une permission répétée. Une automatisation ou un skill ne sera ajouté que pour une procédure stabilisée et utile ; aucun n'est créé à cette étape.

## Point de conduite du 12 octobre 2026 après F2

Yutori retiré par Fabien. Les paragraphes précédents relatent l’ordre historique ; le plan 10 porte l’état courant. F2 complète le journal local avec 13 tests d’acceptation, sans appel ni paiement. La prochaine preuve recherchée concerne la livraison signée réelle Firecrawl, puis l’admission phrase/source ; la découverte générale n’est toujours pas validée. Aucun choix de fournisseur de production, changement de périmètre ni quota public n’est décidé implicitement. Firecrawl sur source connue est seulement le candidat technique recommandé pour la suite.

## Point du 15 octobre 2026 après F3

F3 clos : quatre messages signés issus de deux passages réels, vérification locale, rapprochement API et journal, deux crédits gratuits, monitor en pause. Le transport testé ne résout ni découverte ni admission. La suite A1 éprouve explicitement la différence entre critères compris et couverture démontrée avant activation. Aucun changement de promesse produit ni choix de fournisseur de production implicitement validé. Aucun accès utilisateur supplémentaire requis pour F3.

## Révision du 20 octobre 2026 — découpage exécutable

Fabien précise que les sprints servent à découper le travail, sans imposer un nouvel arbitrage à chaque étape. Le document 10 contient les tickets S1-01 à S4-04, leurs dépendances et leurs preuves de fin. La spécification est dans le document 08. L'autonomie porte sur les choix techniques réversibles ; dépenses, accès manquants et changements de promesse restent explicitement traités.

S0 documentaire est terminé. S1 commence par Android Studio et l'environnement, puis une matrice de build éprouvée et le projet compilable. S2 couvre les parcours simulés, S3 l'intégration et ses risques, S4 la publication. Pas de promesse de quatre semaines, pas de nouvelle étude générale de fournisseurs, pas d'automatisation récurrente ni de travail visuel avant le socle. Les téléchargements locaux déjà réalisés sont un inventaire, pas un sprint achevé.
