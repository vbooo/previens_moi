# Contrats et exploitation — contrôle du 27 novembre 2026

## Firecrawl

La documentation officielle [facturation](https://github.com/firecrawl/firecrawl-docs/blob/main/billing.mdx) exclut un accord de traitement des données (DPA) du forfait gratuit. La [grille officielle](https://www.firecrawl.dev/pricing) réserve cet accord aux niveaux qui l’incluent ; la simple disponibilité de crédits gratuits ne le démontre pas. La [politique du fournisseur](https://www.firecrawl.dev/privacy-policy?v=20261011) décrit des traitements et transferts ; elle ne constitue pas la preuve d’un accord spécifique conclu pour cette application.

État : aucun DPA signé/accepté pour Préviens-moi n’a été retrouvé dans le dossier local. Le traitement des demandes libres peut porter sur des données personnelles ; une mention dans la politique de confidentialité ne règle pas à elle seule ce point. Avant ouverture publique, obtenir un accord applicable au compte ou choisir une solution dont le cadre contractuel convient. Aucun achat, recharge, nouveau forfait, acceptation de conditions ou contact avec le fournisseur n’a été effectué ici. L’essai courant utilise des demandes techniques synthétiques et des pages publiques Python/Chrome, sans nom de client ni donnée utilisateur réelle envoyée dans ces sujets.

Une demande éventuelle au fournisseur peut être préparée ainsi, sans envoi : « Nous préparons une application française de suivi d’informations publiques. Notre compte utilise actuellement le forfait gratuit. Quel accord de traitement s’applique à nos requêtes API et contenus récupérés ? Pouvez-vous fournir un DPA applicable à ce compte, les sous-traitants, lieux, durées et garanties de transfert, sans changement de forfait automatique ? »

## Restauration

Après autorisation explicite de Fabien, la copie chiffrée Auth/application du 27 novembre a été restaurée dans une base locale jetable : 72 tables, empreintes identiques, intégrité contrôlée avant déchiffrement et copie altérée refusée. Le cloud n’a pas été modifié et toute exécution externe est fermée dans la restauration. Voir `validation/s3/cloud-backup-restore.json`.

La copie et sa clé séparée restent sur ce Mac dans des dossiers privés ignorés par Git. Ce contrôle ne démontre pas une reprise sur un nouveau projet géré, la récupération de Vault/SMTP/Firebase, une sauvegarde hors du Mac ou une politique d’exploitation périodique. Ces limites restent à traiter pour l’exploitation publique ; aucun stockage tiers n’est ajouté sans choix explicite.
