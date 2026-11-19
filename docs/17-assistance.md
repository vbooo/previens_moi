# Assistance intégrée

Lot du 19 novembre 2026, version Android 0.2.4 (18).

## Parcours

Un bouton secondaire **Aide** reste en bas à gauche de **Mes suivis**, y compris avant la connexion. Le bouton principal **Nouveau suivi** reste à droite. L’aide ouvre un formulaire Material 3 : **Question**, **Problème** ou **Idée / avis**, un message de 2 000 caractères maximum, puis un e-mail facultatif pour recevoir une réponse.

Fermer le formulaire conserve le brouillon. Pendant l’envoi, les champs et le bouton sont désactivés. Une erreur garde le texte ; un nouvel essai inchangé conserve le même identifiant. Le succès affiche **« Merci, ton message a bien été reçu. »** : il prouve la réception durable par le serveur, pas la lecture par une personne. Après cette réception, le texte et l’e-mail sont retirés du brouillon local.

Gmail ne s’ouvre pas. L’adresse destinataire est exclusivement dans les secrets serveur. Un utilisateur qui ne laisse pas son e-mail peut envoyer son avis, mais ne pourra pas recevoir de réponse personnelle. Répondre depuis une boîte personnelle révèle évidemment l’adresse de cette boîte dans l’e-mail de réponse ; prévoir une adresse dédiée avant lancement public.

## Mise en œuvre et limites

- `SupportViewModel` gère l’état de l’interface et le brouillon via `SavedStateHandle`. L’interface `SupportRepository` sépare la présentation de Retrofit et du serveur.
- `POST /public/v1/support` est utilisable sans compte, notamment pour signaler un problème de connexion. Le contrat interdit les destinataires arbitraires, les pièces jointes et les champs supplémentaires.
- L’API valide le contenu, enregistre un identifiant aléatoire et une empreinte, puis renvoie une réception. Les répétitions identiques ne créent pas un second message.
- Une seule table privée, sans accès des rôles mobiles, sert de boîte de réception et de file d’envoi. Le worker existant relève cette file environ chaque minute. Une réservation SQL empêche deux workers de prendre simultanément le même message.
- Envoi texte par la messagerie OVH déjà disponible, en TLS sur le port 465. Aucun LLM ni Firecrawl. Destinataire et expéditeur fixes côté serveur ; seul `Reply-To` peut venir de l’e-mail facultatif validé.
- Protection d’essai : 3 messages sur 24 heures par adresse réseau hachée, 20 sur 24 heures pour toute l’application. Le plafond global protège aussi en cas de changement ou d’usurpation de l’adresse réseau. Ce dispositif borné n’est pas une défense anti-abus complète pour un lancement à grande échelle.
- Le contenu et l’e-mail sont effacés de la file après acceptation SMTP. Les reçus, empreintes et messages non livrés âgés de plus de 30 jours sont supprimés par un nettoyage quotidien, avec accord explicite de Fabien le 19 novembre 2026. La copie reçue dans la boîte mail suit ensuite sa propre conservation.
- Un essai SMTP interrompu peut avoir été accepté sans retour certain. Il passe en échec à examiner, sans réenvoi automatique susceptible de dupliquer le mail. Les diagnostics indiquent uniquement un événement et la référence ; ni message, ni e-mail, ni mot de passe.

Le service d’assistance ne modifie ni les plafonds de surveillance, ni les droits d’abonnement. La politique de confidentialité et la déclaration Play devront décrire ces retours volontaires, l’e-mail facultatif, la version/langue de l’app et le hachage réseau avant publication publique.

## Configuration privée

`.local/cloud/support-secrets.env` contient `SUPPORT_IP_SALT` et `SUPPORT_MAIL_CONFIG_B64`, installés dans les secrets Supabase. Base64 est un encodage de transport, **pas un chiffrement**. Ce fichier reste privé, permissions 600, ignoré par Git. Aucun de ces secrets ne doit entrer dans Gradle ou l’APK.

`.local/cloud/support-settings.json` conserve le destinataire choisi par Fabien et l’état du raccordement. La messagerie source reste dans les fichiers SMTP privés existants. Voir [les emplacements et sauvegardes](15-fichiers-locaux-et-sauvegardes.md).

## Vérification

Les tests couvrent la validation, l’absence de destinataire libre, l’état d’envoi, le brouillon conservé, la répétition d’une réception, les limites SQL, les accès privés, l’effacement après acceptation SMTP et l’échec conservé pour examen. Les captures et tests natifs vérifient le formulaire en français et en anglais, le thème sombre, la grande police et le clavier. Le détail des résultats effectivement obtenus se trouve dans `validation/support/`.

Pour un test manuel : ouvrir **Mes suivis → Aide**, choisir une catégorie, écrire un message et envoyer. Une adresse de réponse est facultative. Vérifier la confirmation dans l’app, puis la réception dans la boîte destinataire. Un envoi accepté par SMTP ne garantit ni classement en boîte principale ni lecture.

## Suite demandée par Fabien

La prochaine passe UI doit revoir **l’accueil**, notamment les espaces libres excessifs en haut et la présentation de l’état sans suivi. Elle reste distincte de ce lot. Restent ensuite l’identité/logo, la qualification complète FR/EN, les exigences de publication et l’observation réelle sur sept jours. Aucun nouveau forfait annuel ou à vie, achat réel, ouverture des ventes ou test IA payant n’est ajouté ici.
