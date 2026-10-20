# Contrat applicatif v1 — préparation S3

- `application-v1.openapi.json` : routes et formats métier, version de développement, aucune URL déployée.
- `application-v1.examples.json` : 20 exemples acceptés et 13 entrées refusées par le schéma. Sources `.invalid` synthétiques, aucun secret réel.
- [Spécification et cas d'intégration](../docs/11-service-et-contrat-s3.md) : règles qui ne se prouvent pas avec un schéma (propriété, transaction, expiration, budget, arrêt distant, qualité des résultats).

Deux fonctions d'entrée sont prévues : `public` (bootstrap et prévisualisations protégées par leur secret), `api` (JWT utilisateur). Webhooks et workers auront leur propre identité ; ils ne partagent pas un contournement JWT avec les routes utilisateur.

Installation du validateur dans un environnement isolé :

```sh
python3 -m venv .local/contracts-venv
.local/contracts-venv/bin/python -m pip install -r contracts/requirements.txt
.local/contracts-venv/bin/python scripts/check-service-contract.py
```

Le contrôle valide OpenAPI 3.1, résout ses références, vérifie les exemples selon JSON Schema 2020-12, les identifiants d'opération et les protections déclarées des routes. Il n'appelle aucun service. Le fournisseur d'identité conserve son propre contrat officiel ; il n'est pas réimplémenté ici.

Les tests Android chargent les mêmes exemples comme ressources **de tests uniquement**. Le client `data/remote` est préparé pour ce contrat ; il n'est pas encore raccordé aux ports métier ou à l'assemblage demo. Un contrat valide n'est pas un serveur livré.

Compatibilité : ignorer les champs supplémentaires dans une réponse, refuser un état inconnu sans l'interpréter comme un succès, ne jamais convertir une date sans heure en instant fictif. Le schéma des fixtures est volontairement strict pour contrôler la version produite par notre futur serveur.
