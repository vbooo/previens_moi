# Correction de l’admission — 21 octobre 2026

Les deux défauts initiaux sont corrigés dans le chemin d’interprétation du service. La dernière campagne réelle A5 passe **9/9**, sans changer les neuf demandes ni leurs résultats attendus. Aucun accès Supabase/Firebase dédié n’a été utilisé. Aucune surveillance ni notification n’a été activée.

La marque est séparée de la référence. Le format impose des branches de décision cohérentes. Une lecture locale prudente des formulations FR/EN explicites préserve référence, état, prix, comparaison et exclusions ; un pays absent déclenche une clarification. Les autres formulations conservent le chemin général du modèle, avec contrôle des extraits textuels pour le pays et les états. Les réponses de clarification commencent par produit et budget quand ces deux éléments manquent. Un article grammatical de salle n’est retiré que s’il est présent après « à » dans la demande ; une majuscule initiale propre reste conservée.

Cette lecture locale n’est pas un moteur général qualifié : une référence simple, un seul montant en euros et une formulation prise en charge sont nécessaires à son intervention. Les corrections structurées dans `answers`, les demandes ambiguës, négations, plusieurs produits, autres pays ou formulations restent sur le chemin général. Les neuf cas ont servi au développement ; 9/9 ne constitue pas une qualification sur un corpus indépendant et ne prouve ni couverture Web, ni récence, ni déduplication.

| Campagne | Résultat | Remarque |
| --- | --- | --- |
| A1 historique | 7/9 | Marque incluse ; statut compris avec pays absent |
| A2 | 7/9 | Pays remplacé par « / » ; états ajoutés dans le cas anglais |
| A3 | 6/9 | Extraits justificatifs omis ; article de salle conservé |
| A4 | 5/9 | Questions redondantes, état anglais omis et comparaison modifiée |
| A5 finale | **9/9** | Lecture littérale et interprétation contrôlée ; chaque réponse passe le parseur de production |

Les résultats intermédiaires et leurs réservations sont conservés. Chaque série a son manifeste figé, neuf appels maximum, aucun retry et verrou de lancement unique. Pas de sélection du meilleur résultat d’une répétition identique : les correctifs successifs sont explicités ci-dessus. Le modèle reste `gpt-4.1-mini-2025-04-14`, sans outil, `store:false`, sortie bornée à 2200 tokens. Les entrées A5 sont identiques à celles du manifeste A1.

Vérifications finales : **50 tests Deno réussis**, contrôle TypeScript réussi ; **29 scénarios d’intégration locaux réussis** ; contrat inchangé (15 opérations, 23 schémas, 34 exemples) et frontières Android passantes. Le code Android n’a pas changé ; les tests d’interface et compilations précédents ne sont pas présentés comme de nouvelles mesures.

Coût de ce correctif : **36 appels OpenAI**, environ **0,0288908 USD** supplémentaires. Cumul OpenAI estimé : **0,081134 USD**, non rapproché d’une facture. La borne prudente cumulée, incluant toutes les réservations historiques, les autres essais, change défavorable et taxes, est **1,9414335 €**, sous **2 €**. Aucune recharge, souscription, nouvelle dépense Exa ou Firecrawl. Aucun autre appel payant après A5.

Preuves : `admission-a2.json` à `admission-a5.json`, manifestes associés, `admission-a5-spec.json`, sorties `admission-correction-regressions.txt` et `admission-correction-check.txt`, `local-integration.json` et `costs.json`.

Le sprint 3 reste ouvert pour la qualification générale et les preuves externes. La création des projets dédiés Supabase et Firebase Spark reste nécessaire pour le webhook public et la réception FCM réelle ; elle ne bloquait pas ce correctif.

Après A5, les garde-fous locaux ont été resserrés pour ne pas ignorer des qualificatifs avant/après la référence. **Aucun nouvel appel payant** : les neuf réponses A5 enregistrées ont été rejouées avec le parseur final, résultat 9/9. Les 50 tests comprennent ces neuf régressions réelles. Le manifeste A5 conserve le hash au moment des appels ; `admission-final-replay.json` identifie les hashes et le commit finaux. Les dépendances existantes sont conservées.

Commit du correctif : `165bed64da809e65577a77cbeb4631b78d810439`. Les données synthétiques d’intégration ont été nettoyées ; capacités, qualifications et planification sont désactivées. Aucun secret ni projet externe nouveau requis pour cette correction.
