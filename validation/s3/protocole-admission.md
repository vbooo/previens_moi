# S3-03 — critères conservés avant tout nouvel essai fournisseur

20 octobre 2026. Aucun nouvel appel OpenAI, Exa ou Firecrawl dans cette exécution. Les campagnes historiques restent closes. Yutori reste exclu. Les preuves F1/F2/F3 et l'échec historique de compréhension/récence ne sont pas transformés en qualification générale.

Les références restent le corpus A1/REV des documents 03, 06, 09 et du laboratoire. Une future campagne doit reprendre leurs entrées exactes et leurs références indépendantes ; leur contenu n'est pas remplacé par un corpus fabriqué par le modèle évalué.

| Cas à conserver | Attendu avant autorisation d'alerte |
| --- | --- |
| Demande précise FR/EN | Critères, zone, cadence et méthode explicites ; pas de condition ajoutée. |
| Entité ambiguë ou « important » | Clarification identifiée et utile ; pas de portée inventée. |
| Correction de critère | Proposition versionnée ; ancienne réponse tardive ignorée. |
| Condition privée inaccessible | Limite expliquée, aucune activation. |
| REV-A01 : aucun résultat initial | Absence de résultat distincte d'impossibilité de surveiller ; baseline silencieux si méthode qualifiée. |
| REV-A02 : nouveau domaine pertinent | Preuve et critères contrôlés sans imposer une liste fermée de sites. |
| REV-A03 : ancienne publication découverte | Aucune nouveauté fondée seulement sur la découverte ou la mise à jour. |
| REV-A04 : zéro résultat, panne et quota | États différents, date de réussite inchangée après panne. |
| REV-A07 : même fait sur deux URL | Une identité métier et une intention ; l'URL seule ne déduplique pas. |
| Injection, contradiction et preuve absente | Pas de notification affirmative ; incident/limite explicite. |
| Pause, remplacement, résultat ponctuel | Génération actuelle avant émission ; l'envoi légitime du premier résultat ponctuel demeure possible. |

Seuils des cas contrôlés : zéro fausse notification affirmative, zéro doublon, zéro accès entre comptes et zéro panne annoncée comme réussite ; toute nouveauté admissible injectée est détectée ou produit un échec visible. Ne pas ajuster ces critères après l'essai.

Mesures réelles encore **non mesurées** en S3 : compréhension libre, précision des alertes, rappel dans les sources annoncées, fraîcheur des preuves, déduplication sémantique, délai et coût par contrôle. Les tests SQL et le rejeu hors ligne du laboratoire ne les mesurent pas.

Avant tout nouvel appel : relire le budget cumulé de 2 €, vérifier les soldes, réserver une borne complète (erreurs/reprises/change/taxes), geler nombre d'appels, modèle et tokens, références attendues et condition d'arrêt dans un manifeste horodaté. Aucune réservation financière nouvelle n'a été ouverte ici. Capacité et méthodes restent désactivées ; aucune exécution récurrente n'est financée ou qualifiée.
