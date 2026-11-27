# Préparation finale — 27 novembre 2026

Fabien valide la 0.2.10 et les essais téléphone. La 0.2.11 (26) ajoute le lien vers la politique publique accessible avant/après connexion et une connexion standard par mot de passe existant pour préparer un compte d’évaluation Google. Aucun mot de passe dans les ressources ou le bundle, aucun cas particulier d’authentification selon l’adresse ou le rôle d’évaluateur. Les sessions sont validées par Supabase et enregistrées dans le stockage protégé existant. Les mots de passe saisis ne sont pas sauvegardés avec l’état de l’écran ; après tentative, le champ est vidé. Le code e-mail reste le parcours initial.

## Pages publiées

- Confidentialité : https://vbooo.github.io/app-policies/previens-moi/fr.html
- Suppression : https://vbooo.github.io/app-policies/previens-moi/suppression.html
- Contact : https://vbooo.github.io/app-policies/previens-moi/
- Dépôt partagé : https://github.com/vbooo/app-policies (commit 8a36c5e). Les autres applications ne sont pas modifiées.

Les pages répondent HTTP 200 en HTML. La politique nomme l’éditeur, les données de compte/demandes/achats/FCM/assistance, les traitements OpenAI/Firecrawl/Supabase/Google/OVH/Gmail, les durées/critères réellement connus, les droits, le retrait des données et les limites des sauvegardes. Région de base constatée : eu-west-1. Aucun hébergement intégralement européen ni rétention nulle fournisseur n’est promis. La conservation manuelle des échanges mail reprend la règle du service partagé : traitement puis 12 mois maximum après clôture.

## Vérifications

88 tests unitaires demo et 105 remote passent ; builds et Lint demo/remote debug et remote release passent. Les régressions natives contrôlées vérifient la confidentialité sans compte, le passage au mode mot de passe, l’erreur d’authentification et le champ vidé, sans mail réel. Trois captures de l’app réelle en français, 1080×1920 JPEG, sont produites sans soumission ni accès fournisseur. Le certificat correspond à la clé de dépôt existante.

Le compte d’essai utilisateur normal contact@fabienbouvet.fr est préparé uniquement après accord explicite de Fabien. L’autorisation a été demandée à la suite d’un refus du contrôle automatique ; il s’agit d’un utilisateur dans le projet Préviens-moi existant, pas d’un nouveau projet. Les informations restent dans `.local/play-review/access.json`, ignorées par Git ; aucune session ni clé d’administration n’est fournie à Google. La vérification du mot de passe serveur est suivie d’une déconnexion. Aucune dépense ou souscription n’est offerte au compte.

## Exploitation et limites avant production

`scripts/user-stats.py` fournit des compteurs agrégés privés en lecture seule ; voir docs/18-statistiques.md. Ce n’est pas un tableau de bord de navigation ni un suivi permanent des conversions. Aucune audience commerciale ne peut être déduite des comptes de test.

L’accord sur l’UI ne remplace pas les portes d’exploitation : sept jours consécutifs d’observation restent à prouver, avec budget de recherche insuffisant pour une nouvelle réservation de sept jours dans le reliquat de test. Les plafonds d’origine ne sont ni relevés ni réinitialisés. `billing_policy.test_only` et le blocage de surveillance Plus restent en place. Les accords fournisseurs et l’exploitation des sauvegardes doivent être suivis avant ouverture publique ; la notice décrit les traitements mais ne crée pas un accord Firecrawl qui n’a pas été vérifié. Aucune vente réelle ni publication en production n’est déclenchée par ce lot.

## Mails à test@example.com signalés par Fabien

Deux rejets SMTP visent cette adresse fictive à 16 h 46 et 16 h 49 ; le domaine refuse le courrier. Les tests exécutés ici ne contiennent ni ce texte « Great app! » ni des envois réels à cette adresse. L’identité d’essai préexistait au 17 novembre et n’a aucune connexion réussie au contrôle du 27 novembre. Des robots Google peuvent saisir des formulaires après import ; aucun rapport pré-lancement disponible ne permet d’attribuer avec certitude ces messages à Google. Ne pas attribuer un expéditeur sans preuve ni désactiver l’authentification, l’assistance ou les contrôles Google pour masquer ces retours.


## Diffusion du correctif OTP — 27 novembre, 18 h 49

La 0.2.11 (26), reconstruite avec le refus des codes vers les domaines de documentation example.com, example.net et example.org, est accessible aux testeurs internes. La CI du commit `8e69628` passe : Android/tests/Lint, serveur/contrat et recherche de secrets. Les 105 tests unitaires remote passent, ainsi que Lint release, l’inspection de l’APK et les vérifications des signatures APK/AAB. Le ReTrace est joint ; Google ne signale qu’une recommandation de symboles de débogage natifs. Aucun appareil précédemment compatible n’est perdu.

La fiche française est enregistrée comme prête à envoyer pour examen, avec le logo, l’image de présentation et trois captures natives 1080×1920, dans l’ordre accueil, création, assistance. Catégorie Productivité ; contact public et site d’assistance publiés dans les paramètres de la fiche. La déclaration de données est préparée ; sa validation dépend de la cible, elle-même conditionnée par l’accès évaluateur.

Le contrôle des journaux distingue les deux applications : les nouveaux envois vers test@example.com correspondent au serveur Préviens-moi. Le dernier appel vient du client Android okhttp/5.5.0 et d’une plage publique Google ; un test Google est probable, sans identifiant de test permettant l’attribution certaine. Le chat « GTA VI - reprise dev » décrit séparément un code à son identifiant fictif dédié, suivi d’un correctif de connexion et de la soumission 0.1.1. Aucun paramètre ni binaire GTA n’est modifié ici.

La correction Android diffusée n’est pas un filtrage serveur global : les anciens binaires ou appels directs restent capables de demander un code vers ces domaines. Aucun e-mail de vérification réel n’a été émis pendant les contrôles de ce correctif.


## Classification et dernière dépendance de configuration

Après accord explicite de Fabien sur les conditions IARC, questionnaire enregistré le 27 novembre à 18 h 58. La page affiche « État de l’examen par l’IARC : Terminée ». L’application n’inclut pas de média violent/adulte dans son binaire et ne permet pas d’échanges publics entre utilisateurs ; ses résultats Web et textes analysés par IA peuvent faire référence à des thèmes adultes sans en être l’objet principal. Les références textuelles possibles sont déclarées, sans prétendre à un filtrage thématique absent. Résultat européen : PEGI Accord parental, achats intégrés. Le public cible 18+ reste une déclaration distincte, confirmée par Fabien.

Le questionnaire Sécurité des données est sauvegardé avec huit catégories et les liens publics de suppression ; son aperçu attend la cible. Le partage de requêtes/critères est déclaré, sans revendiquer une exception générale de sous-traitant non vérifiée pour tous les fournisseurs. Les finalités d’analyse comprennent les statistiques opérationnelles agrégées ; aucun SDK publicitaire ou nouveau suivi de navigation n’est ajouté.

Le plan `plan-acces-et-qualification.md` propose, sans application à ce relevé, un accès Plus offert ordinaire côté serveur et un plafond de 6 EUR cumulés pour terminer les essais et l’observation. Le solde fournisseur Firecrawl est de 988 crédits sur le plan gratuit à 1000, vérifié en lecture seule ; l’admission locale ne dispose que de 16 crédits non consommés/non réservés. Les 39 crédits et 20000 micro-USD réservés incertains restent conservés. Aucune recharge, nouvelle souscription fournisseur ou hausse de plafond n’a été exécutée. Aucune vente réelle ouverte.
