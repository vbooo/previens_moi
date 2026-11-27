# Statistiques d’exploitation

Le bilan `scripts/user-stats.py` lit les données existantes avec une transaction PostgreSQL READ ONLY. Il ne déclenche aucun contrôle fournisseur, ne change aucun quota et ne crée pas d’API d’administration publique. Exécution locale : `.local/contracts-venv/bin/python scripts/user-stats.py`. La configuration et le résultat restent privés, sous `.local/` ; aucun identifiant, e-mail, texte, IP ou jeton n’est sélectionné.

Mesures disponibles : comptes présents et créés sur 30 jours ; comptes ayant créé au moins un suivi parmi les enregistrements conservés ; utilisateurs avec un suivi actif ; suivis par état ; achats et utilisateurs par état d’abonnement, en séparant les achats de test ; contrôles sur 14 jours ; résultats sur 90 jours ; préparations anonymes sur 24 heures ; livraison FCM sur 24 heures.

Il n’existe pas encore de tableau de bord ni de suivi des écrans. Un utilisateur avec un suivi actif n’est pas une mesure de visite quotidienne ou mensuelle. Les demandes anonymes ne permettent pas de compter les personnes distinctes. Les suppressions et purges empêchent de reconstruire un historique exhaustif ; les compteurs présents ne sont pas un entonnoir permanent de conversion. Un envoi FCM accepté n’est pas une preuve de lecture.

Les tests sont inclus dans les comptes et suivis ; seuls les achats portent déjà un indicateur de test fiable. Ne pas présenter les chiffres actuels comme une audience commerciale. Une future conservation de statistiques historiques agrégées ou un tableau de bord constitue un lot séparé ; aucun SDK de tracking n’a été ajouté ici.
