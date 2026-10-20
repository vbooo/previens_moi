# Matrice de faisabilité générale et coût d'une demande

Révision du 4 octobre 2026 après l'objection de Fabien : **ne pas construire un catalogue de sources pour chaque besoin possible**. Ce document prépare la [validation technique](../docs/10-plan-de-validation-technique.md). Il ne définit ni des catégories obligatoires, ni une promesse de couverture universelle, ni un fournisseur choisi.

## La question à laquelle le pilote doit répondre

Peut-on accepter une phrase libre, déterminer honnêtement si une surveillance est possible, puis détecter des nouveautés pertinentes à un coût compatible avec une V1 publique ? **Quelques cas réussis ne démontreront jamais que « n'importe quel sujet sur tout Internet » fonctionne.** Le résultat recherché est une frontière de capacité : ce que l'on peut annoncer sans tromper l'utilisateur, ce qui exige une source précise, ce qui exige une intégration spécialisée et ce qui doit être refusé.

Une IA peut clarifier la demande, proposer des requêtes, classer des résultats et aider à résumer une preuve. Elle ne possède pas un accès exhaustif au Web, ne voit pas les comptes privés sans autorisation, ne rend pas une donnée absente disponible et ne garantit pas qu'une page trouvée est à jour. Un fournisseur de recherche ou de surveillance résout **une partie** du problème ; notre service reste responsable de l'admission, des limites annoncées, de la preuve, des doublons, des pannes et des coûts.

## Les mécanismes à éprouver, indépendants des sujets

| Mécanisme | Fonctionnement | Peut convenir à | Limite à afficher |
| --- | --- | --- | --- |
| Découverte Web récurrente | Relancer une recherche large et dédupliquer ses résultats récents. | Nouvelles pages, annonces et articles publics indexés. | Couverture non exhaustive, indexation et délai variables ; un résultat absent ne prouve pas qu'aucun fait n'existe. |
| Changement d'une source connue | Comparer des versions d'une page ou d'un flux choisi. | Modification d'un contenu public accessible et stable. | Il faut connaître la bonne source ; une variation visuelle ou un accès bloqué peut tromper le détecteur. |
| Donnée structurée | Interroger une API ou un flux portant une valeur, un état ou une prévision. | Conditions mesurables lorsque la donnée existe et que son usage est autorisé. | Pas d'API universelle ; chaque intégration supplémentaire a un coût de maintenance et des droits propres. |
| Accès privé autorisé | Connexion au service concerné avec le consentement de l'utilisateur. | Données personnelles de compte, si une intégration existe. | Hors du moteur Web général ; sans accès autorisé, refuser la demande. |

Le produit peut garder **une entrée en langage naturel pour tout sujet** sans activer toute demande. L'admission peut répondre « surveillable dans cette portée », « à préciser », « possible avec une source fournie », « expérimental » ou « impossible aujourd'hui ». Une demande de prix, de météo ou d'actualité peut emprunter des mécanismes différents selon sa formulation ; aucun sujet n'est garanti à lui seul.

## Test transversal plutôt que catalogue de sources

Constituer un petit jeu de demandes portant sur des **formes de difficulté** : source inconnue ou connue, nouvelle page ou changement sur page existante, critère objectif ou subjectif, donnée publique ou privée, français ou anglais, France ou ailleurs, événement fréquent ou rare, délai souple ou urgent, prix/état stable ou personnalisé. Les familles évoquées par Fabien servent seulement à varier les sujets et à éviter un banc d'essai artificiellement facile. Aucun quota fixe du type « quatre événements, deux prix, une météo » n'est requis.

Pour chaque demande du pilote, noter avant essai : phrase exacte, interprétation confirmée, mécanisme proposé, portée vérifiable, source de référence **si elle existe**, exclusions, cadence, seuil de preuve, coût maximal et verdict d'admission. Ne chercher une source témoin que pour établir un résultat de référence dans le pilote ; **ne pas créer un adaptateur métier pour chaque demande**. Si aucun fait de référence n'est observable sur sept jours, le résultat est « non concluant », pas « réussi ».

Le pilote doit tester deux choses distinctes :

1. **Admission** : le système sait-il dire « je ne peux pas le garantir », clarifier une demande vague et refuser un accès privé ou une condition invérifiable ?
2. **Surveillance admise** : sur la portée annoncée, trouve-t-il les faits observables, évite-t-il les anciennes informations et les doublons, signale-t-il les erreurs et produit-il une preuve utilisable ?

Les contrôles d'une page de test modifiée volontairement restent utiles pour vérifier la logique technique, mais ne prouvent pas la couverture du Web. Le [protocole comparatif](03-protocole-comparatif-des-moteurs.md) mesure les vrais résultats, les ratés et les coûts. Les lectures déjà faites des données de Paris, de Service Public et d'Open-Meteo n'étaient que des exemples d'accessibilité : elles ne font pas de ces trois sujets le périmètre du produit.

## Modèle économique à valider avant la promesse V1

Le coût mensuel ne dépend pas du nombre de sujets imaginables, mais du **nombre de surveillances distinctes réellement exécutées**, de leur fréquence, du nombre de recherches/pages par passage et du travail de vérification. Modèle minimal :

`coût mensuel ≈ surveillances distinctes × passages mensuels × coût de recherche + vérification des résultats + service, stockage et notifications`

Si plusieurs personnes suivent exactement la même recherche, un seul contrôle peut parfois servir plusieurs abonnés ; des demandes très personnalisées se mutualisent peu. Une IA exécutée à chaque passage ou pour chaque résultat ajoute une dépense distincte. Une recherche vide peut aussi être facturée. Les crédits gratuits des prestataires permettent un pilote, pas une offre gratuite illimitée.

**Ordres de grandeur documentaires au 4 octobre 2026, hors autres coûts et sous réserve que chaque passage corresponde à une requête de base facturée :** [Exa Monitors](https://exa.ai/pricing) affiche 15 $ pour 1 000 requêtes, soit environ 0,45 $ pour une recherche quotidienne pendant 30 jours, 45 $ pour 100 surveillances distinctes, 450 $ pour 1 000. [Yutori Scouting](https://docs.yutori.com/pricing) affiche 0,35 $ par passage, soit 10,50 $ par surveillance quotidienne sur 30 jours et 1 050 $ pour 100 surveillances distinctes. [Firecrawl](https://www.firecrawl.dev/pricing) affiche notamment 1 crédit par page et par contrôle de page, avec 1 000 crédits gratuits mensuels ; ses recherches et son jugement ont d'autres unités. Ces multiplications ne sont **ni des devis, ni le coût total de Préviens-moi**, et seront confrontées à des factures réelles dans le pilote.

Une V1 gratuite peut être envisagée avec des **limites explicites** de demandes actives et de fréquence, une mutualisation lorsque c'est possible et un arrêt automatique aux plafonds. Elle ne peut pas promettre à tous une recherche mondiale, permanente, illimitée et gratuite sur n'importe quel critère. Le montant de 50 € déjà évoqué est uniquement le plafond d'une première campagne d'essai facturable, pas le budget d'exploitation mensuel.

## Décision attendue avant le développement complet

1. Tester d'abord la capacité générale et l'admission sur une matrice de difficultés, sans chercher à répertorier les sources du monde.
2. Chiffrer le coût complet par surveillance active et pour plusieurs tailles d'usage, avec le taux de mutualisation réellement observé.
3. Si la couverture ou le coût échouent, **réduire la promesse V1** avant de coder la publicité et l'onboarding. Les intégrations de sources directes viendront plus tard seulement pour les demandes fréquentes ou particulièrement utiles qui le justifient.

À ce jour, ces capacités générales ne sont **pas prouvées** : aucun moteur de surveillance n'a été essayé avec nos demandes et aucune économie de production n'a été mesurée.
