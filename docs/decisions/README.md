# Historique des décisions

Conserver ici les décisions structurantes une fois prises. Les hypothèses et questions ouvertes restent dans les documents thématiques.

Chaque décision précise la date, le statut, le problème, les options étudiées, le choix, ses raisons, les conséquences et les conditions d’une éventuelle révision. Une décision remplacée reste consultable avec un lien vers celle qui la remplace.

Les décisions validées couvrent le produit, l'architecture et la qualité de publication. Les fournisseurs de surveillance, les quotas et les seuils de fonctionnement restent à qualifier après essais. Le socle du service est précisé en ADR 0011, sans adoption commerciale ou déploiement implicite.

## Décisions enregistrées

- [Positionnement initial du 2 octobre 2026](0001-positionnement-initial.md) : France prioritaire, événements précis et suivis généraux, amélioration progressive.

- [Priorités de différenciation du 2 octobre 2026](0002-priorites-de-differenciation.md) : parcours clair, pertinence, transparence et savoir-faire sur les sources, avec maîtrise des coûts.

- [Périmètre du MVP du 2 octobre 2026](0003-perimetre-mvp.md) : chaîne complète d’alerte, anglais sous condition de vérification, compte à l’activation et hors-ligne complet reporté.

- [Parcours et écrans du 2 octobre 2026](0004-parcours-et-ecrans.md) : accueil centré sur la demande, résultats par surveillance et adresse de page publique en secours après recherche automatique.

- [Règles des alertes du 2 octobre 2026](0005-regles-des-alertes.md) : fin des surveillances ponctuelles, reprise sans rattrapage après pause volontaire et notifications de nouveautés sans heures calmes réglables dans la V1.

- [Sources et stratégie de recherche du 2 octobre 2026](0006-sources-et-recherche.md) : méthode adaptée à chaque demande, refus honnête sans vérification récurrente et comparaison avant choix fournisseur.

- [Coûts et diffusion initiale du 2 octobre 2026](0007-couts-et-modele-economique.md) : essais plafonnés, V1 gratuite sur le Play Store sans invitation et capacité bornée.

- [Architecture et données du 3 octobre 2026](0008-architecture-et-donnees.md) : service central, Android MVVM et Clean Architecture, sources isolées par adaptateurs, compte e-mail et conservation limitée.

- [Qualité et publication du 3 octobre 2026](0009-qualite-et-publication.md) : production gratuite d'abord en France, preuves de qualité, sept jours d'essai, conformité Play et chemin de test conditionné par le compte réel.

- [Plan de validation des 3 et 12 octobre 2026](0010-plan-de-validation-technique.md) : audit comparatif de Yutori, Exa et Firecrawl, banc d'essai transversal sans sélection de pays ou de catégories, critères avant essais, puis tranche Android/service. Les exemples arbitraires et l'étude préalable des besoins français ont été retirés.

Révision du 12 octobre : document 10 comme plan courant, audit des services complets avant nouveau code moteur, playbook par lot et conservation des preuves historiques. Aucun fournisseur de production adopté.

- [Service réel S3 du 20 octobre 2026](0011-service-reel-s3.md) : Supabase pour API/base/identité, FCM pour push, code e-mail géré ; choix d’implémentation locale, ouverture cloud et facture complète encore conditionnelles.
