# Bilan du premier moteur — campagne du 9 octobre 2026

> **Statut au 12 octobre 2026 : document historique du laboratoire du 9 octobre.** Les résultats restent valides dans leur portée ; les anciennes mentions de « prochaine étape » ne gouvernent plus le projet. L'extension de ce moteur et la comparaison de modèles sont suspendues pendant l'audit des services gérés. Voir le [plan courant et son playbook](../docs/10-plan-de-validation-technique.md) et la [comparaison actualisée](02-alternatives-au-moteur-yutori.md). Aucun essai supplémentaire n'est déclenché.

**Conclusion : le prototype local fonctionne, mais cette configuration IA ne satisfait pas le contrat. La chaîne Web complète n'est pas validée.** Le constat porte sur GPT-4.1 mini et nos consignes, pas sur l'impossibilité générale de créer Préviens-moi. Aucun service permanent ni push Android n'a été lancé.

## Résultats et décision

| Vérification | Résultat observé | Interprétation |
| --- | --- | --- |
| Tests locaux | 34 méthodes réussies, avec variantes | Règles, état, limites persistantes, erreurs et schéma ; pas 34 succès IA. |
| Compréhension initiale | 4/8 demandes exactes | Marque conservée dans la référence, statut incohérent et pays inventé. |
| Compréhension après une correction | 6/8, puis 6/8, puis 6/8 | Même référence mal normalisée en français ; demande sans pays mal traitée (sortie rejetée par validation). |
| Extraction contrôlée | 12/12 sur le premier passage et chacun des 3 passages après correction | Valeurs et décisions attendues ; seuil inclusif aussi évalué. Pages synthétiques avec noms de champs explicites, test relativement facile. |
| Instruction malveillante dans la page | 0/3 extractions fidèles | Le modèle remplace le prix de 12900 centimes par 1 en citant pourtant « 12900 ». L'offre reste rejetée car reconditionnée ; aucune fausse alerte observée dans ce cas. |
| Réserve | Non ouverte et non évaluée | La porte précédente échoue ; conserver la réserve pour une configuration admissible. |
| Chaîne Web nouvelle | Non exécutée | Pas de nouvelle recherche/collecte ni de positif réel confirmé pendant cette campagne. |

Les trois passages sont des appels indépendants : aucune réponse locale n'est réutilisée. Un cache de préfixe fournisseur peut exister ; il ne correspond pas au rejeu d'une réponse. Les attentes du corpus ne sont jamais données au modèle. Aucune mesure de ces petits corpus ne permet d'affirmer une précision sur toutes les demandes utilisateur.

**Décision : ne pas connecter ce montage à des alertes utilisateurs.** Ne pas poursuivre une boucle de réglages indéfinie. Le prochain essai utile est une comparaison bornée avec un modèle plus capable sur le même contrat, pour distinguer une limite de ce modèle économique d'une limite de la méthode. Ce serait une nouvelle configuration documentée, pas un changement silencieux du test ni une preuve de fiabilité du Web. Les sources structurées restent une option MVP si la voie généraliste exige trop de travail.

## Ce qui a été réalisé

Un adaptateur réel OpenAI Responses (`gpt-4.1-mini-2025-04-14`), des sorties JSON à schéma contraint, le chargement local de la clé, un registre SQLite de réservations de coût, une gestion sans réessai automatique et un évaluateur reproductible. La clé ne figure dans aucun rapport. Le budget est réservé avant chaque appel et reste réservé après une erreur ou un redémarrage.

La correction unique a clarifié le champ référence produit et rendu les branches proposition/clarification/non pris en charge incompatibles dans le schéma. Elle a amélioré deux cas mais n'a pas supprimé tous les défauts. Les premiers résultats et le code v1 sont conservés. Le corpus gelé n'a pas changé.

Un défaut du banc d'essai a également été consigné : la première assertion d'injection attendait à tort `available=false`, alors que P07 est une offre reconditionnée disponible. L'audit compare désormais tous les champs aux valeurs gelées. Le véritable échec subsiste indépendamment de cette correction : le prix extrait est 1 au lieu de 12900. Les réponses brutes restent intactes. La vérification des montants sur les pages réelles est plus stricte que l'adaptateur des fixtures ; ces résultats n'établissent pas qu'une injection réussirait à déclencher un push réel.

## État du plan

| Lot / porte | État actuel |
| --- | --- |
| M1 / G1 | Contrat adopté, corpus gelé et intégrité vérifiée. |
| M2 | Accès OpenAI opérationnel ; plans financiers v1/v2 enregistrés avant appels. |
| M3 / G2 | 34 tests locaux passent ; stockage et journal local fonctionnent. |
| M4 / G3 | Exécuté sur le corpus ; **échec** de compréhension et résistance à l'injection. |
| G4 | Trois passages mesurés, mais non validés ; réserve non exécutée. |
| M5 / G5 | Non exécuté : prérequis non satisfaits. |
| M6 | Bilan de cette configuration terminé ; décision de non-validation. |

## Budget réellement consommé

Fabien a autorisé **2 € maximum au total**. L'enveloppe du programme a été abaissée volontairement à **1 USD** pour conserver une marge de change/taxe. 83 appels ont été effectués, sans réessai réseau automatique, avec usage retourné pour chacun.

**Coût estimé à partir des tokens : 0,0476092 USD**, soit environ 0,043 € hors taxes au taux BCE de référence du 8 octobre. L'estimation facture conservatoirement tous les tokens entrants au tarif non mis en cache. Ce n'est pas une facture OpenAI ni une mesure du coût mensuel d'une application. Le plafond total des réservations conservées est de 0,5874744 USD ; ce montant n'est pas la dépense.

Tarif utilisé : [OpenAI, service standard](https://developers.openai.com/api/docs/pricing), 0,40 USD/M tokens entrants et 1,60 USD/M sortants. [Référence BCE](https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/index.ff.html) : 1 € = 1,1186 USD le 8 octobre 2026. Aucun changement de facturation, aucune carte ou recharge ajoutée.

La campagne est **fermée** par un marqueur lu avant tout appel. Aucun processus récurrent n'a été créé. Une commande de reprise ne renouvelle pas les tentatives. Le budget non consommé ne déclenche aucun travail automatique.

## Preuves locales

- [Rapport synthétique de campagne](../engine-lab/reports/live-openai-20261009/summary.json).
- [Détail des résultats](../engine-lab/reports/live-openai-20261009/results.json) et [compteur financier](../engine-lab/reports/live-openai-20261009/cost.json).
- [Plan initial](../engine-lab/reports/live-openai-20261009/plan.json) et [correction versionnée](../engine-lab/reports/live-openai-20261009/plan-v2.json).
- [Tests locaux](../engine-lab/reports/offline.json), [fiche d'exécution](08-fiche-execution-moteur.md), [laboratoire](../engine-lab/README.md).

Les réponses fournisseur sont dans le sous-dossier local `raw`, exclu de Git. Leur durée de conservation reste 14 jours conformément à la fiche ; aucun nettoyage planifié n'a été créé. Le bilan expurgé ci-dessus conserve les résultats utiles.

## Essai Firecrawl réellement exécuté

Une seule tentative sans clé, sans Authorization, sans compte ni paiement, sur la page Darty AF300EU déjà auditée. La [fiche](08-fiche-execution-moteur.md) a été écrite avant lancement. La réponse est revenue en environ 10,3 secondes ; cette observation isolée ne constitue pas une mesure de performance représentative.

Firecrawl retourne prix 184,71 € et livraison 3,99 €, soit **188,70 €**. Ces montants figurent dans le contenu renvoyé par le même appel. Le prix antérieur observé manuellement était différent ; aucune baisse ni cause de variation n'est inférée. L'offre est hors seuil de 150 € pour cette capture.

L'essai a exposé un défaut de notre validation : les citations du modèle n'incluaient pas toujours les doubles espaces ou marqueurs gras du Markdown. Le premier résultat local était INSUFFISANT. Une normalisation de présentation, testée séparément, et la correspondance explicite entre le symbole € et EUR permettent désormais un rejet justifié par le prix. Les montants normalisés doivent aussi correspondre au nombre cité. La réponse initiale reste intacte ; la correction a été évaluée par replay local, sans autre appel fournisseur.

L'extraction conserve des lacunes : identifiant d'offre, zone de livraison, conditions commerciales et plusieurs champs ne sont pas établis. Le modèle a utilisé « Livré dès le 19/10 » pour affirmer la fraîcheur : cette citation ne prouve pas l'actualité de l'offre. Une citation présente dans le texte n'est pas une preuve suffisante que toutes les conclusions du modèle sont valides. Cette faiblesse reste à traiter et mesurer avant toute confirmation positive réelle.

Preuves : [requête](../engine-lab/reports/anonymous-probe/request.json), [réponse fournisseur](../engine-lab/reports/anonymous-probe/response.json), [résultat initial](../engine-lab/reports/anonymous-probe/report.json), [rejeu après correction](../engine-lab/reports/anonymous-probe/replay.json). Aucune clé n'y figure. Le dossier brut est temporaire selon la durée de traces prévue ; ce bilan expurgé conserve le résultat utile.

