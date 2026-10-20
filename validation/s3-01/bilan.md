# Démarrage du sprint 3 — 20 octobre 2026

**S3 a démarré ; il n'est pas terminé.** S3-01 fournit les choix d'implémentation locale, les responsabilités, le contrat HTTP et les scénarios de coût. Sa clôture pour l'ouverture distante reste conditionnelle : domaine d'expédition, sauvegarde, coût de l'analyse qualifiée et capacité réelle ne sont pas encore établis. Une première brique S3-02, le client HTTP Android, est préparée et testée localement.

## Livré

- [ADR 0011](../../docs/decisions/0011-service-reel-s3.md) : Supabase pour API/base/identité, FCM pour notifications, code e-mail géré ; aucun abonnement ni déploiement.
- [Spécification 11](../../docs/11-service-et-contrat-s3.md) : accès, commandes en attente, reçus, transactions, arrêt distant, dates, preuves, conservation et cas S3-A01 à A15 avant implémentation serveur.
- [Contrat OpenAPI](../../contracts/application-v1.openapi.json) : 15 opérations, 23 schémas ; prévisualisation sans compte protégée par secret distinct du JWT utilisateur.
- [Exemples](../../contracts/application-v1.examples.json) : 20 formes valides et 13 invalides. Source `.invalid`, aucune donnée utilisateur.
- Client Android dans `data/remote` : Retrofit/OkHttp, DTO, états HTTP distincts, annulation conservée, délais bornés, pas de suivi automatique des redirections ni de répétition automatique des commandes. Les secrets sont masqués dans les représentations diagnostiques concernées.
- Scénarios économiques distinguant estimations, tarifs et inconnues. Aucun budget d'exploitation autorisé implicitement.

## Vérifications exécutées

| Contrôle | Résultat / portée |
| --- | --- |
| OpenAPI et exemples | 33/33 attendus, dont 13 refus ; [preuve](contract-check.json). Validation de format seulement, pas d'authentification réelle. |
| Tests Android JVM | **35/35**, dont 24 tests S2 conservés et 11 tests client HTTP ; [détail](android-check.json). Serveur MockWebServer local, aucun appel fournisseur. |
| Compilation | APK demo réussi ; [journal](build.txt). Retrofit/converter 3.0.0, OkHttp/MockWebServer 5.5.0 résolus depuis Maven Central. |
| Lint | 0 erreur, 0 avertissement ; 7 indications de mise à jour non bloquantes. |
| Frontières et manifeste | Domaine/présentation conformes ; inspection du binaire sans permission INTERNET. |
| Installation/lancement | Réussi après reprise ; processus présent et Activity au premier plan. [Tentatives](launch-check.json). |

Révision des sources testées : `2f78dc5b1c9d9193937868df2bddc99a028c9d23`. Empreinte de l'APK dans `android-check.json`. Les commandes de reproduction sont dans le [guide Android](../../android/README.md) et le [guide du contrat](../../contracts/README.md).

Les tests client couvrent réponse 202 encore en attente, deux requêtes de même commande, erreurs 429/503, absence de fuite de secret lors d'une redirection, succès vide distinct de réponse mal formée, état d'analyse inconnu, champs additionnels, réponse 204, annulation, panne transport et refus d'URL HTTP hors tests locaux. Publication inconnue et date d'événement sans heure sont conservées sans inventer un instant.

Les cinq parcours Compose sur trois configurations du sprint 2 restent des preuves de cette livraison antérieure ; ils n'ont pas été rejoués dans ce lot sans modification d'écran. Le nouvel APK a été relancé pour vérifier son démarrage après ajout des dépendances. Ce contrôle ne remplace pas la future matrice d'intégration avec authentification et réseau réel.

## Incidents traités

- Première exécution : le nouveau MockWebServer exigeait un démarrage explicite ; corrigé, puis toute la suite rejouée. Aucun test retiré.
- La dépendance OkHttp Android ajoutait INTERNET par son manifeste. Retrait explicite dans la variante demo ; le contrôle sait distinguer un retrait de permission d'un ajout et vérifie toujours le manifeste fusionné. L'inspection aapt2 du binaire confirme l'absence de permission.
- Installation ADB en streaming restée sans réponse : interrompue ; réinstallation sans streaming réussie. Premier processus arrêté par Android pour dépassement de son délai d'attachement, sans crash applicatif dans le journal consulté ; deuxième lancement froid réussi en 5 062 ms. Ce chiffre n'est pas une mesure de performance de production.
- Validateur Python : avertissement urllib3/LibreSSL de l'environnement système, sans effet sur la validation hors ligne. Aucune requête réseau de ce validateur.

## Ce qui n'est pas livré

Pas de backend métier, migration PostgreSQL, test RLS réel, connexion e-mail, session chiffrée persistée, envoi FCM ou surveillance nouvelle. Les DTO ne sont pas raccordés aux ports ou aux ViewModels. L'application ouverte reste la demo. Docker existant a été démarré pour préparer l'environnement local ; aucun projet Supabase, conteneur Supabase ou compte cloud n'a été créé. Aucun push Git ou CI distante.

Le [chiffrage](cost-scenarios.json) est conditionnel, pas une facture ni une preuve de gratuité de production. Plafond historique de 2 € cumulés inchangé ; **aucun appel fournisseur et aucune dépense supplémentaire**. Les quotas d'offre restent proposés, pas activés. Aucun monitor distant réactivé.

## Suite précise

**S3-02 local :** migrations PostgreSQL et API métier, Auth locale avec boîte e-mail de test, vérification d'isolation A/B et d'idempotence sur vraie base, puis raccordement des ports Android. Ne pas présenter les tests MockWebServer comme ces tests d'isolation. L'environnement local permet d'avancer sans domaine et sans compte cloud.

**Avant ouverture distante :** résoudre les inconnues S3-01, configurer les projets gratuits dédiés et le transport de codes e-mail, puis faire les preuves d'identité et de push réelles. Le domaine sert uniquement à l'expéditeur du code de connexion ; il n'est pas un prérequis pour coder ni pour installer l'application. Aucun achat demandé à Fabien à ce stade.


Reprise du 20 octobre : l’implémentation locale du backend, de l’identité et de l’assemblage Android est désormais dans le [bilan S3 courant](../s3/bilan.md). Ce bilan de démarrage reste la preuve historique du contrat à son état initial ; ne pas l’utiliser comme statut actuel de S3-02.
