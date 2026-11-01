# Stabilisation V1 — 1er novembre 2026

**Le lot est terminé pour son périmètre local. La version connectée 0.1.1 est prête à être essayée sur le OnePlus.** Ce n’est pas encore une version destinée à la publication Play.

## Ce qui change

- Les 59 erreurs Lint de la version connectée sont corrigées. Le contrôle de la simulation a aussi révélé 20 ressources mal placées : elles sont désormais réservées à la variante qui les utilise. Aucun problème n’est masqué par une baseline ou une désactivation de règle.
- Les textes abandonnés et deux icônes inutilisées sont retirés. Les ressources encore utiles restent disponibles dans leur variante. La licence des icônes officielles est conservée sans modification et incluse dans les deux APK.
- Le nombre de sources s’accorde correctement : « 1 source vérifiée », « 2 sources vérifiées ».
- Une question facultative pouvait bloquer « Continuer » lorsqu’elle restait vide. Elle peut maintenant être passée ; une précision obligatoire reste nécessaire. Le test vérifie aussi l’enchaînement des questions et la conservation des réponses.
- Les sélecteurs des anciens tests de connexion suivent les écrans actuels. Le README, le guide Android et le plan décrivent le fonctionnement livré et distinguent la version réelle du simulateur.

L’architecture et le design sont conservés. Aucun changement du moteur serveur, de quota ou de donnée utilisateur n’a été effectué.

## Vérifications

| Contrôle | Résultat |
| --- | --- |
| Compilation et Lint des deux variantes | Réussis ; 0 erreur, 0 avertissement. 13 suggestions de versions informatives par variante. |
| Tests unitaires Android | 42 remote + 64 demo réussis ; certaines bases de test sont communes. |
| Service | Vérification de types et 101 tests réussis, avec réponses injectées. |
| Contrat API | 34 exemples validés, 17 opérations, 24 schémas ; aucun réseau. |
| Frontières Android | Domaine et présentation conformes ; manifeste demo sans INTERNET. |
| Émulateur API 36, français / clair | 9 tests remote réussis et 1 intégration Auth locale volontairement ignorée faute de configuration locale ; 5 tests demo réussis. |
| Émulateur API 36, anglais / sombre / texte à 130 % | 6 tests remote réussis. |
| Accord singulier/pluriel | Captures de 1 et 2 sources, français et anglais. |
| APK connecté | Version 0.1.1, code 2 ; configuration cloud publique présente, mode local désactivé. |
| Secrets serveur connus | Aucun des secrets locaux recherchés trouvé littéralement dans les deux APK. Ce contrôle ciblé ne remplace pas un audit de sécurité. |

Les parcours couvrent création/confirmation, connexion simulée, préparation, résultat, pause/reprise, échec confirmé et actualisation. Un ancien test supposait encore un message d’erreur au lieu du bouton désactivé et plusieurs questions sur un même écran : il a été adapté au parcours actuel, sans supprimer les assertions de réponse requise, d’identifiants de réponses et de refus explicite.

Les captures natives inspectées montrent les états partiel et échec, en clair et sombre avec texte agrandi. Elles utilisent des données de test ; elles ne prouvent pas la pertinence de toutes les recherches. Les titres français des fixtures sont volontairement conservés même dans le test des libellés anglais.

Les [résultats détaillés](checks.json) recensent les empreintes des APK, captures et journaux locaux. Les commandes reproductibles sont dans le [guide Android](../../android/README.md). Les journaux et captures restent dans `.local/`, hors Git. La CI est configurée localement ; elle n’a pas tourné sur GitHub.

## Tester maintenant

Sur le Mac, ouvrir `android/` dans Android Studio, sélectionner `remoteDebug`, connecter le OnePlus puis lancer ▶. L’APK final est aussi conservé dans `.local/builds/stabilization-2026-11-01/Previens-moi-0.1.1-test.apk` à la racine du dépôt. Mettre à jour l’application existante, sans la désinstaller ni effacer ses données.

Essayer une création, les éventuelles précisions, la confirmation, puis pause/reprise et actualisation. Vérifier aussi que quitter puis rouvrir l’application conserve la session et les suivis. Les essais déclenchant une vraie recherche restent soumis au budget existant.

**Coût supplémentaire de ce lot : 0 €.** Aucun appel fournisseur payant, aucun achat, aucune augmentation des plafonds et aucun push GitHub. Le plafond cumulé de 2 € est conservé ; ce bilan n’affirme pas qu’il reste 2 € disponibles.

## Ce qui reste après ce lot

Le test physique de cette nouvelle version sur le OnePlus reste à faire. Les livraisons e-mail et FCM réelles, les scripts de restauration après mort du processus, TalkBack, la matrice complète d’API/appareils, les performances et les sept jours de surveillance ne sont pas requalifiés ici. Les essais réels précédents restent documentés dans leurs bilans datés.

Ensuite : corrections issues de ton essai et finitions UI/UX ciblées, observation longue organisée selon le budget restant, puis confidentialité, sauvegarde indépendante, signature release et Play Console. Le benchmark et la modularisation restent différés.
