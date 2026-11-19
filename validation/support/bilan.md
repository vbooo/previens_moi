# Assistance — version 0.2.4 (18)

## Livré

- Bouton Aide fixe en bas à gauche de Mes suivis, même sans connexion ; formulaire natif Material 3 pour question, problème ou idée/avis.
- Envoi intégré, destinataire privé côté serveur, e-mail de réponse facultatif. Brouillon conservé à la fermeture et en cas d’échec, bouton désactivé pendant l’envoi, réception confirmée clairement.
- Réception serveur idempotente, boîte privée, plafond indépendant de 3 messages/24 h par réseau haché et 20/24 h globalement. Envoi OVH TLS ; aucun appel IA ou Firecrawl.
- Contenu effacé de la file après acceptation SMTP. Conservation des reçus et échecs limitée par nettoyage quotidien après 30 jours, approuvée explicitement par Fabien puis activée.

## Preuves obtenues

| Contrôle | Résultat |
|---|---|
| Tests unitaires Android remote | 84 réussis |
| Tests unitaires Android demo | 87 réussis |
| Tests du service sans accès fournisseurs | 143 réussis |
| Contrat API | 20 opérations, 28 schémas, 45 exemples vérifiés |
| SQL local | Base vide jetable ; répétition simultanée sans doublon, limites, accès privés, deux workers distincts, effacement et conservation des échecs |
| Android natif | 2 tests exécutés en français clair et anglais sombre à 130 %, soit 4 exécutions réussies |
| Clavier réel | Capture ADB française ; défilement jusqu’au bouton d’envoi au-dessus du clavier |
| Lint debug et release | Aucune erreur ni avertissement |
| Envoi en ligne | Un message synthétique accepté par SMTP, une répétition HTTP identique, une seule ligne et un seul envoi ; contenu ensuite effacé de la file |
| Version Play optimisée | APK/AAB signés, package définitif, minSdk >=24, HTTPS et sauvegardes vérifiés, pas de destinataire ni secret SMTP dans l’APK |

Les rapports `checks.json`, `sql.json`, `delivery.json` et `release.json` détaillent ces contrôles. Les captures conservées sont synthétiques. La revue visuelle indépendante a demandé un espacement vertical de 8 dp entre les rangées de choix en grande police ; la correction a été notée résolue, disposition **ship**, pour cette seule extension. Les documents du système visuel existant ont été préservés.

## Limites et suite

L’acceptation SMTP ne prouve ni l’arrivée en boîte principale ni la lecture. L’e-mail facultatif est indispensable pour une réponse personnelle ; aucun dialogue de messagerie interne n’est ajouté. TalkBack et l’intégralité des formats/tablettes ne sont pas qualifiés par ces captures. La couverture anglaise du moteur de recherche reste un lot séparé.

Le serveur est raccordé. Le fichier signé est prêt dans `.local/play-test/previens-moi-play-release-0.2.4-18.aab` ; **il n’a pas encore été téléversé sur Google Play**. Il faut publier cette version dans le test interne puis mettre à jour l’app du téléphone. Aucun achat réel ni vente publique n’a été ouvert.

Prochaine passe UI demandée : reprendre l’accueil et ses espaces inutiles, puis identité/logo et qualification FR/EN. Le contexte produit et le récapitulatif visuel secondaire comportent une dérive historique déjà présente ; ils n’ont pas été réécrits dans ce lot.

Coût supplémentaire des tests OpenAI/Firecrawl : **0 €**. Les plafonds de surveillance et la politique d’achat test n’ont pas été augmentés.
