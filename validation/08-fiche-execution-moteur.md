# Fiche d'exécution du laboratoire moteur

Le 9 octobre 2026, Fabien autorise l'enchaînement des étapes. Les critères du document 07 sont adoptés comme contrat de laboratoire. Aucun service récurrent n'est autorisé. L'autorisation de dépense ultérieure du 9 octobre est décrite dans l'avenant ci-dessous ; le budget initial nul reste l'historique de la première sonde.

## Choix de réalisation

Python 3.9 ou supérieur, bibliothèque standard, tests unittest et stockage SQLite. Ces outils sont présents localement ; aucune dépendance installée. Le laboratoire est dans [engine-lab](../engine-lab/README.md). Il ne fixe pas le langage du backend final. Les fournisseurs sont séparés des contrats et des règles.

Le corpus v1 est généré avant l'implémentation du moteur et gelé par empreintes SHA-256. Les cas de réserve ont le même auteur que le code : leur évaluation ne serait pas indépendante. Ils ne sont pas encore utilisés pour qualifier une IA. La lecture des pages locales exploite une convention HTML synthétique, explicitement réservée aux fixtures. L'admission rejoue des sorties attendues ; elle ne mesure pas une compréhension du langage naturel.

## Accès et plafonds avant essai externe

Les variables EXA_API_KEY, FIRECRAWL_API_KEY, OPENAI_API_KEY et ANTHROPIC_API_KEY sont absentes de cet environnement. Aucune recherche de secrets dans les fichiers, lecture de clé du navigateur ou connexion à un autre compte n'est effectuée. Le compte Exa accessible dans le navigateur ne constitue pas une API intégrée.

Un essai préalable, plus petit que M4, vérifie uniquement l'accès anonyme documenté de Firecrawl. La [documentation Scrape](https://docs.firecrawl.dev/features/scrape) consultée le 9 octobre montre un appel sans Authorization. Le tarif documentaire est 1 crédit de scrape, plus 4 pour JSON et 4 pour le contrôle d'injection, soit **9 crédits nominaux maximum pour notre unique requête**. Ce chiffre n'est pas un débit observé.

- URL : page AF300EU Darty déjà auditée dans PRIX-01. Aucune page bloquée précédemment n'est contournée.
- Endpoint : POST https://api.firecrawl.dev/v2/scrape ; lecture fraîche, markdown et extraction JSON avec citations ; contrôle d'injection activé.
- Une seule tentative, timeout local de 50 secondes, pas de réessai, pas de redirection de l'appel API, pas de recherche ni lien marchand supplémentaire.
- Aucun header d'authentification, aucune clé lue, aucun compte facturable associé par le programme. Aucune carte, aucun achat ni changement de facturation.
- Le fichier de verrou persiste après erreur ; relancer la commande ne renouvelle pas l'appel.
- Si authentification exigée, refus, limite ou résultat inexploitable : conserver la réponse et arrêter. Ne pas basculer silencieusement sur un compte payant.

Cet essai ne mesure ni l'admission IA ni les trois passages contrôlés de G3/G4. L'absence d'accès utilisable laisse ces portes non exécutées. Un plan complet de consommation du modèle sera requis après sélection d'un accès et avant M4 ; les 50 € historiques ne sont pas utilisés.

## Contrôles et limites locales

L'adaptateur de fixture ne peut pas accepter une capture marquée Web réel. Les URLs d'entrée sont limitées à HTTPS public ; les redirections simulées sont contrôlées. Le client réseau de cette sonde n'accepte que l'URL Darty fixée dans le code. Le laboratoire ne prétend pas contrôler les redirections internes de Firecrawl ni fournir un client Web générique protégé contre tout rebinding DNS.

Le journal d'émission est local et transactionnel. Il n'est pas une preuve de livraison push exactement une fois. La politique de réarmement reste désactivée après un premier événement pour la même offre/version, conformément au point ouvert AL-O06.

Les rapports indiquent séparément les tests locaux, l'essai réseau éventuel et les étapes non exécutées. Les traces brutes externes doivent être nettoyées après 14 jours ; les corpus synthétiques et bilans expurgés peuvent être conservés. Aucun nettoyage automatique n'est planifié dans cette conversation.

## Avenant OpenAI — autorisation du 9 octobre 2026

Fabien a enregistré localement sa clé puis autorisé explicitement **2 € de plafond total pour la campagne**. Le laboratoire utilise uniquement ce fichier `.env`, ignoré par Git ; il ne recherche pas de secrets ailleurs. La clé n'est jamais ajoutée aux rapports.

Le [plan gelé avant appels](../engine-lab/reports/live-openai-20261009/plan.json) retient GPT-4.1 mini, snapshot `gpt-4.1-mini-2025-04-14`, comme référence économique, sans prétendre choisir le meilleur modèle actuel. [Tarif officiel consulté](https://developers.openai.com/api/docs/pricing) : 0,40 USD/M tokens entrants et 1,60 USD/M sortants, service standard. Réponses JSON à schéma contraint ; aucun outil de navigation autonome.

L'enveloppe logicielle est volontairement **1 USD maximum réservé**, avec 90 tentatives au plus pour conserver la possibilité d'une seule correction. Chaque requête réserve son coût conservateur avant l'envoi : taille UTF-8 du corps + 4096 tokens de marge, sortie limitée à 2200 tokens. La somme des réservations n'est jamais remboursée, même après erreur, redémarrage ou réponse moins coûteuse. Les limites et identifiants de tentatives persistent dans SQLite ; aucun réessai automatique. Requêtes de plus de 60 000 octets refusées. Le taux BCE du 8 octobre (1 € = 1,1186 USD) et cette enveloppe laissent une marge sous les 2 € autorisés, y compris pour conversion/taxe usuelles. Aucun réglage de facturation du compte n'est modifié.

Plan initial : 8 admissions et 12 extractions contrôlées, trois passages indépendants, 3 variantes d'injection, puis 2 admissions et 4 pages de réserve et 2 séquences locales, enfin 1 extraction exploratoire de la capture Darty existante si les portes le permettent. Jusqu'à 70 appels planifiés avant correction éventuelle. Les sorties attendues ne sont jamais transmises au modèle. Le cache de résultats du laboratoire n'est pas réutilisé entre passages ; un cache de préfixe propre au fournisseur peut exister et ses tokens sont conservés dans les réponses.

Les pages de travail ont des noms de champs explicites : ce test est plus facile que la lecture d'un site arbitraire. La capture Darty, si relue, est une archive datée ; ce n'est pas une nouvelle collecte. La découverte Web et les notifications Android ne sont pas validées par ces appels. Une erreur de corpus ou de consigne est consignée, jamais effacée par un ajustement silencieux des attentes.

### Clôture de cet avenant

83 appels réalisés ; une correction des consignes/schéma d'admission, versionnée dans `plan-v2.json`. Coût estimé 0,0476092 USD, réservations maximales cumulées 0,5874744 USD. Les essais contrôlés échouent au contrat G3 : réserve et archive Darty non relues avec OpenAI, aucune collecte Web nouvelle. Campagne fermée ; [bilan et suites](09-bilan-premier-moteur.md). Les mentions d'absence de clé et de zéro budget plus haut décrivent exclusivement l'état initial avant cet avenant.
