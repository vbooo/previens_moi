# Ouverture des notifications et textes — 2 novembre 2026

**Version connectée 0.1.4, code 5.** Le résultat est ouvert avant sa lecture serveur ; le contenu se charge sur place. « Trouvé dès la première recherche » est retiré de la fiche et de la liste des informations, en français et en anglais. La distinction de provenance reste interne au moteur et aux données : aucun critère ou résultat n’est supprimé.

L’ancien parcours créait d’abord la pile sur l’accueil et attendait le résultat authentifié avant de naviguer. Le nouveau parcours initialise directement la destination de la notification et y montre un chargement. Une session en cours de restauration ne déclenche pas prématurément l’écran de connexion. Une session réellement absente requiert toujours la connexion ; son annulation revient à l’accueil.

Une erreur de lecture conserve l’écran de résultat avec une action d’actualisation si nécessaire. Un contenu supprimé affiche son indisponibilité. La lecture courante possède un numéro distinct : une ancienne réponse du même résultat ne peut pas être prise pour une nouvelle ouverture. Chaque nouvel intent reçoit aussi une clé d’événement. Une fois le résultat ouvert, Retour retrouve son suivi parent ; si la personne quitte pendant le chargement, une réponse tardive ne la ramène pas de force au résultat.

La destination en attente est enregistrée lors de la sauvegarde d’activité ; l’extra de résultat consommé est retiré de l’intent. Cela n’est pas présenté comme une nouvelle campagne de qualification de mort de processus.

## Vérifications

55 tests unitaires remote et 72 demo passent (cas communs exécutés dans les deux variantes). Douze parcours natifs passent sur l’émulateur API 36 : cinq cas ciblés d’ouverture, deux de synchronisation et cinq régressions d’interface/commandes. Les tests ciblés réemploient le routage de production et les écrans natifs avec une session et un serveur injectés, volontairement lents. Ils contrôlent l’absence d’accueil pendant l’ouverture, la connexion, les erreurs, les notifications répétées et le retour utilisateur. Le cas répété a permis de corriger la confusion possible avec l’ancienne réponse terminale.

Les captures chargement/résultat ont été inspectées ; la mention supprimée n’apparaît plus. Les deux variantes compilent, Lint passe sans erreur ni avertissement et les frontières Android passent. Le contrôle ciblé des secrets serveur connus dans les APK n’en trouve aucun ; l’APK connecté conserve les paramètres publics cloud. [Preuves détaillées](checks.json).

Aucun changement serveur, nouvelle recherche, donnée utilisateur modifiée, quota relevé ou push GitHub. **Coût fournisseur supplémentaire : 0 €, plafond cumulé de 2 € inchangé.** La réception réelle FCM et son délai sur OnePlus n’ont pas été requalifiés ici.

Pour mettre à jour sans effacer les données : lancer `remoteDebug` dans Android Studio, ou utiliser `.local/builds/notification-opening-2026-11-02/Previens-moi-0.1.4-test.apk`. Le téléphone n’a pas été installé dans ce lot.
