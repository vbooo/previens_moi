# S4 — comprendre, confirmer, préparer

Décision validée le 23 octobre 2026. Priorité : application Android native compréhensible et présentable en entretien. Interface Material sobre ; conception visuelle et architecture de l’information approfondies dans un lot ultérieur. Le benchmark de demandes reste différé, sans date imposée.

## Parcours livré

1. **Nouveau suivi** ouvre une demande libre. **Comprendre ma demande** appelle uniquement l’IA d’interprétation. Aucun site n’est recherché ou lu avant cette réponse. L’IA peut demander une précision.
2. **Voici ce que j’ai compris** affiche les critères et les limites de l’essai. L’utilisateur peut modifier sa demande ou **confirmer et préparer le suivi**. Aucune source n’est présentée comme vérifiée à ce stade.
3. Le suivi passe **En préparation**. L’application peut être quittée : le serveur conserve le travail, recherche les sources puis vérifie leur adéquation et établit une première lecture silencieuse.
4. Une notification distincte annonce que le suivi est prêt ou que sa préparation a échoué. Elle ouvre le suivi ; elle ne crée pas un faux résultat de recherche.
5. Une fois actif, le suivi général est vérifié quotidiennement avec la couverture partielle de l’essai. Les résultats nouveaux utilisent le circuit de notification existant. Un changement substantiel des critères n’est pas accepté automatiquement.

L’accueil montre d’abord les suivis et leur état. Un bouton **Actualiser** et le geste pour rafraîchir sont disponibles ; **Connecté** rend la session visible. La confirmation ne bloque plus l’écran en attendant les lectures Web. Une requête d’interprétation reste un appel réseau : le délai réel dépend du modèle et de la connexion, aucune garantie de 1–2 secondes n’est annoncée.

## Comprendre l’architecture pour un entretien

- **Android / Compose** affiche l’état. Le ViewModel gère la saisie, la reformulation et la confirmation ; ses StateFlow mettent l’écran à jour. Le repository traduit les réponses HTTP en états du produit. Le journal chiffré conserve les identifiants nécessaires à une reprise sans double commande.
- **Supabase / PostgreSQL** conserve le compte, les suivis et le travail à exécuter. Une transaction attribue chaque étape à un worker avec une durée limitée et réserve son coût avant l’appel. Confirmer crée une préparation durable ; Android n’a pas à rester ouvert.
- **Worker serveur** recherche et lit les sources avec Firecrawl, puis demande au modèle existant de vérifier leur adéquation. Une étape séparée établit la référence initiale ; seul son succès rend le suivi actif. Les critères confirmés sont conservés.
- **Firebase Cloud Messaging** transporte seulement des identifiants. **WorkManager** relit le suivi sous la session de son propriétaire avant d’afficher une notification Android. Un ancien message ne doit pas réactiver un suivi annulé.

Il s’agit d’une évolution des composants existants, sans nouveau modèle, fournisseur ou couche d’architecture. L’ancien endpoint d’évaluation reste compatible ; Android utilise le nouvel endpoint de compréhension.

## Vérifications proportionnées

Tests locaux : compréhension sans recherche, confirmation durable, séparation couverture/référence initiale, échec explicite, pas de résultat inventé, message tardif après annulation et effacement du terminal. Tests Android : parcours complet avec fixtures, actualisation, français/anglais, thème sombre et grande police. Ces fixtures n’établissent ni la qualité sémantique générale ni la livraison FCM réelle ; le contrôle cloud est documenté séparément dans `validation/workflow/`.

Le plafond de 2 € cumulés et les compteurs existants sont conservés. Aucun abonnement, reset de compteur ou achat de crédits. Le pilote historique sur URL a atteint son enveloppe gratuite : son suivi est conservé suspendu et son objet fournisseur arrêté, sans relancer la lecture incertaine.

## Suite vers une première version

Se poser ensemble sur les écrans, les informations essentielles, les textes et le design. Puis vérifier les conditions de diffusion : observation dans la durée avec enveloppe explicite, confidentialité, accessibilité et version publiable. Le déblocage Play Console et GitHub restent des actions séparées de Fabien. La refonte visuelle et un benchmark exhaustif ne sont pas ajoutés à ce sprint.
