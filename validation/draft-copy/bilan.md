# Saisie et textes — 2 novembre 2026

La version connectée **0.1.7 (8)** retire « Reprendre mon brouillon » de l’accueil. « Nouveau suivi » conserve désormais une demande non confirmée, sa zone et son site préféré après retour ou réouverture. L’utilisateur peut l’effacer dans le formulaire ; une confirmation réussie libère la saisie. Ouvrir Nouveau après la modification d’un suivi existant crée une demande indépendante, sans conserver la cible de modification.

La confirmation et le détail masquent les résumés autonomes de pays déjà affichés dans « Zone concernée », dont « Le pays de surveillance est la France ». Le filtre de présentation reste volontairement conservateur : un critère contenant une condition supplémentaire ou un pays différent est conservé. Les critères enregistrés et le moteur ne changent pas.

65 tests unitaires remote, 82 demo et huit parcours natifs traversant la véritable activité passent : sept parcours fonctionnels, dont retour/recréation/effacement et pays unique, et un parcours visuel intégré. Les trois captures ciblées ont été inspectées puis revues indépendamment : **ship**, sans correction matérielle restante. Lint sans erreur ni avertissement, frontières Android et diff vérifiés. Les 13 suggestions de mise à jour par variante restent informatives. La première tentative native a été ignorée car le point d’accès local n’était pas configuré ; seuls les huit tests réellement exécutés après configuration comptent dans ce bilan.

APK cloud reconstruit sans service local, licence et absence des valeurs littérales de secrets serveur connus contrôlées. Les [preuves](checks.json) identifient l’APK et les captures ; les données des captures sont des fixtures locales, pas des résultats réels. Les contrôles précédents clair/sombre/texte agrandi restent dans leur bilan historique ; cette retouche ne revendique pas leur réexécution complète.

Coût fournisseur supplémentaire : **0 €**. Plafond de **2 €**, quotas et serveur inchangés. Aucun push GitHub. Installer la 0.1.7 par-dessus l’ancienne version pour conserver les données, puis vérifier sur le OnePlus : saisir, revenir à l’accueil, rouvrir Nouveau suivi et consulter un résumé avec une zone.

La passe UI V1 est acceptée avec ces retouches. Restent l’essai complet sur appareil, les contrôles de qualité/confidentialité/diffusion et l’observation prolongée prévus au plan. Ni la publication ni une nouvelle dépense ne sont déclenchées par ce lot.
