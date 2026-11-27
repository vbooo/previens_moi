# Préparation finale — 27 novembre 2026

Fabien valide la 0.2.10 et les essais téléphone. La 0.2.11 (26) ajoute le lien vers la politique publique accessible avant/après connexion et une connexion standard par mot de passe existant pour préparer un compte d’évaluation Google. Aucun mot de passe dans les ressources ou le bundle, aucun cas particulier d’authentification selon l’adresse ou le rôle d’évaluateur. Les sessions sont validées par Supabase et enregistrées dans le stockage protégé existant. Les mots de passe saisis ne sont pas sauvegardés avec l’état de l’écran ; après tentative, le champ est vidé. Le code e-mail reste le parcours initial.

## Pages publiées

- Confidentialité : https://vbooo.github.io/app-policies/previens-moi/fr.html
- Suppression : https://vbooo.github.io/app-policies/previens-moi/suppression.html
- Contact : https://vbooo.github.io/app-policies/previens-moi/
- Dépôt partagé : https://github.com/vbooo/app-policies (commit 8a36c5e). Les autres applications ne sont pas modifiées.

Les pages répondent HTTP 200 en HTML. La politique nomme l’éditeur, les données de compte/demandes/achats/FCM/assistance, les traitements OpenAI/Firecrawl/Supabase/Google/OVH/Gmail, les durées/critères réellement connus, les droits, le retrait des données et les limites des sauvegardes. Région de base constatée : eu-west-1. Aucun hébergement intégralement européen ni rétention nulle fournisseur n’est promis. La conservation manuelle des échanges mail reprend la règle du service partagé : traitement puis 12 mois maximum après clôture.

## Vérifications

88 tests unitaires demo et 104 remote passent ; builds et Lint demo/remote debug et remote release passent. Les régressions natives contrôlées vérifient la confidentialité sans compte, le passage au mode mot de passe, l’erreur d’authentification et le champ vidé, sans mail réel. Trois captures de l’app réelle en français, 1080×1920 JPEG, sont produites sans soumission ni accès fournisseur. Le certificat correspond à la clé de dépôt existante.

Le compte d’essai utilisateur normal contact@fabienbouvet.fr est préparé uniquement après accord explicite de Fabien. L’autorisation a été demandée à la suite d’un refus du contrôle automatique ; il s’agit d’un utilisateur dans le projet Préviens-moi existant, pas d’un nouveau projet. Les informations restent dans `.local/play-review/access.json`, ignorées par Git ; aucune session ni clé d’administration n’est fournie à Google. La vérification du mot de passe serveur est suivie d’une déconnexion. Aucune dépense ou souscription n’est offerte au compte.

## Exploitation et limites avant production

`scripts/user-stats.py` fournit des compteurs agrégés privés en lecture seule ; voir docs/18-statistiques.md. Ce n’est pas un tableau de bord de navigation ni un suivi permanent des conversions. Aucune audience commerciale ne peut être déduite des comptes de test.

L’accord sur l’UI ne remplace pas les portes d’exploitation : sept jours consécutifs d’observation restent à prouver, avec budget de recherche insuffisant pour une nouvelle réservation de sept jours dans le reliquat de test. Les plafonds d’origine ne sont ni relevés ni réinitialisés. `billing_policy.test_only` et le blocage de surveillance Plus restent en place. Les accords fournisseurs et l’exploitation des sauvegardes doivent être suivis avant ouverture publique ; la notice décrit les traitements mais ne crée pas un accord Firecrawl qui n’a pas été vérifié. Aucune vente réelle ni publication en production n’est déclenchée par ce lot.

## Mails à test@example.com signalés par Fabien

Deux rejets SMTP visent cette adresse fictive à 16 h 46 et 16 h 49 ; le domaine refuse le courrier. Les tests exécutés ici ne contiennent ni ce texte « Great app! » ni des envois réels à cette adresse. L’identité d’essai préexistait au 17 novembre et n’a aucune connexion réussie au contrôle du 27 novembre. Des robots Google peuvent saisir des formulaires après import ; aucun rapport pré-lancement disponible ne permet d’attribuer avec certitude ces messages à Google. Ne pas attribuer un expéditeur sans preuve ni désactiver l’authentification, l’assistance ou les contrôles Google pour masquer ces retours.
