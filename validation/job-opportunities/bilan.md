# Alertes d’emploi précises et répétitions — 4 novembre 2026

Le correctif est en service côté serveur. L’application connectée reste en **0.1.9** ; aucune réinstallation nécessaire. Fabien a validé les parcours physiques, puis l’observation nocturne a révélé ce défaut du moteur. La publication reste à préparer.

## Problème constaté

Quatre résultats enregistrés renvoyaient à la même page Jobijoba : résumés généraux d’offres, annonces anciennes du 24 septembre/20 octobre et identités variables. Une modification textuelle de liste était prise pour une nouvelle opportunité. Les preuves lues mentionnaient bien des scénaristes en alternance, mais ne suffisaient pas à identifier proprement chaque poste ni à garantir sa nouveauté. Il ne s’agissait pas d’un simple doublon de transport Firebase.

## Comportement corrigé

- Pour une recherche d’emploi explicite FR/EN, l’IA doit sélectionner **une annonce individuelle** : intitulé et employeur nommés, lieu/référence quand présents, fonctions qui correspondent au métier demandé. Un métier voisin ne suffit pas. Aucun nouveau catalogue de sites ou connecteur métier.
- Le serveur vérifie les champs et les citations dans **un même bloc continu réellement lu**. Les preuves ne peuvent pas être assemblées à partir de plusieurs fiches voisines. Des sections Missions/Profil au sein d’une seule fiche restent permises.
- L’identité de l’offre provient de l’employeur, de l’intitulé, du lieu et de la référence disponible. Une date de publication, un autre style de résumé ou un changement d’URL ne créent pas à eux seuls une nouvelle offre. La comparaison reste conservatrice quand une référence unique manque.
- Les offres connues, déjà visibles avec les mêmes preuves, et les publications antérieures au jour de départ ne déclenchent pas une nouvelle alerte. La date de mise à jour globale d’une liste n’est pas la date de publication d’une annonce. Une publication le jour du démarrage peut encore être prouvée nouvelle par son ajout observé.
- La première évaluation valable des suivis existants établit silencieusement la nouvelle référence. Les anciens résultats ne sont pas effacés. Ce passage prudent peut écarter une nouveauté présente à ce seul contrôle.
- Une vraie nouvelle offre peut toujours être signalée sur **la même page**. Les résultats d’emploi utilisent désormais « intitulé — employeur » comme titre. Le lien peut rester celui de la liste réellement lue : aucun lien direct non vérifié n’est inventé.
- Le cas générique des promotions reste ouvert : baisse de prix PS5 sur la même URL détectable, répétition du même prix ignorée. Les sources retenues, cadences et plafonds ne sont pas changés.

Le module `job-opportunity.ts` contient ces règles isolées. Le parseur et la décision de nouveauté existants les utilisent ; aucune nouvelle dépendance, table, méthode de recherche ou couche Android. L’historique envoyé au modèle est compacté aux identités utiles sans supprimer la mémoire conservée côté serveur.

## Vérifications

**124 tests serveur réussis**, dont 19 nouveaux cas ciblés ; types et **34 exemples du contrat API** valides. Cas utiles : résumé générique rejeté ; métier voisin et preuves mélangées rejetés ; même annonce/date modifiée/reprise ailleurs non répétée ; nouvelle offre sur la même URL admise ; offre ancienne ignorée ; mémoire relue après redémarrage et déploiement protégée ; demande statistique sur l’emploi non confondue avec une recherche de poste ; preuve salariale nouvelle sur une offre non datée conservée ; suivi ponctuel terminé après un résultat initial précis ; promotions non bloquées par URL.

**Trois appels IA réels**, sur des documents synthétiques contrôlés : annonce précise de scénariste acceptée, rédaction marketing rejetée, deux fiches de pays/métiers différents non assemblées. Trois réponses relues avec le code final et requêtes identiques, sans appel supplémentaire. Ce contrôle ciblé n’est ni le benchmark général différé ni une preuve de couverture Web.

Fonctions `worker` et `public` déployées et actives. Santé HTTP 200, worker sans autorisation 403, compréhension sans jeton 401. Aucun worker déclenché manuellement, aucune recherche Firecrawl ajoutée, aucun nouveau parcours Android requis par le contrat inchangé. Les preuves détaillées sont dans [checks.json](checks.json).

## Coût et suite

Coût estimé des trois appels : **0,002884 USD**, réservé avant chaque appel et enregistré sans répétition automatique. Les frais de qualification développeur sont classés dans leur registre S3 existant, séparément des suivis utilisateur : leur débit temporaire du compteur d’exécution a été transféré, pas remboursé ni effacé. Qualification historique + ces essais : 0,090130 USD ; avec les 0,10 USD maximum d’exécution, borne de 0,190130 USD, dans l’enveloppe existante de 0,40 USD. La borne cumulée conservatrice existante de 0,99587625 € reste sous **2 €** ; ce n’est pas une facture rapprochée. Plafonds d’exécution 100 000 micro-USD et 150 crédits inchangés, réservations inconnues conservées ; aucun abonnement ni recharge.

L’observation réelle des prochaines alertes reste nécessaire. L’IA peut encore manquer ou mal qualifier un cas non testé ; deux postes très similaires sans référence peuvent être regroupés ; une ancienne offre modifiée peut être écartée par la règle prudente de date. La recette UI/physique validée n’est pas une validation de sept jours du moteur. Quotas publics, budget d’exploitation, contrôles confidentialité/release et publication restent ouverts.
