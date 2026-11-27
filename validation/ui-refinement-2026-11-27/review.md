# Accueil et abonnement — 0.2.10 (25)

Demande de Fabien : commencer sans compte depuis l’accueil vide, garder le bouton flottant, clarifier le texte et présenter les avantages et conditions avec les composants Material 3 existants. Identité graphite/crème et logo P/cloche conservés. Référence visuelle : PremiumDialog.kt dans Mesures sur photos, lecture seule.

## Changements

Le bouton central Nouveau suivi suit Se connecter. Le texte précise que le compte est nécessaire à l’activation, mais pas au démarrage du formulaire. Le FAB ne dépend plus d’une liste non vide. Les avantages de Plus sont regroupés en trois lignes avec icônes et titres ; les conditions sont regroupées dans une carte. Gestion en bouton tonal, vérification en bouton contour. Les prix, droits, quotas et callbacks ne changent pas.

Le logo de l’app était déjà livré dans le launcher. Le logo absent signalé par Fabien concerne la fiche Play, dont l’icône est enregistrée dans le brouillon (`play-store-512.png`, 1/1), pas encore dans une fiche publique. Play signale toujours le nom temporaire unreviewed sur le canal interne. Le petit logo dans l’accueil et l’offre est cadré sans rogner la silhouette approuvée.

## Vérifications effectuées

- 87 tests unitaires demo et 103 remote passent. Build et Lint demo/remote debug et remote release passent, zéro erreur et zéro avertissement Lint (17 informations de dépendances).
- Émulateur API 36, sans intervention sur le téléphone e7df3e0b : 8 tests natifs SubscriptionUiTest/SupportUiTest passent en fenêtre téléphone anglaise ; deux parcours ciblés passent en français et en français sombre avec police 130 %.
- Même émulateur en classe de fenêtre tablette 1280×800 dp : les 8 tests passent. En portrait 800×1280 dp : les deux parcours ciblés passent. Le profil AVD tablette API35 n’a pas d’image système installée ; la classe de fenêtre est donc contrôlée avec l’image API36 existante et des métriques d’affichage explicites.
- Captures natives examinées dans `.local/publication-2026-11-27/ui-refinement/`. Les états d’achat utilisent des fixtures contrôlées, sans achat ni appel fournisseur. Le test réel hors connexion 0.2.9 est confirmé par Fabien dans ce chat.
- Contrôle statique du binaire signé, certificat de dépôt et disponibilité Play à consigner après construction/import finaux. Les ventes réelles et les budgets ne sont pas modifiés.

## Références publiques et limites

- Material 3 boutons : https://developer.android.com/develop/ui/compose/components/button
- Material 3 cartes : https://developer.android.com/develop/ui/compose/components/card
- Test interne et fiche temporaire : https://support.google.com/googleplay/android-developer/answer/9845334

Cette retouche valide la présentation et les parcours ciblés. Elle ne remplace pas les étapes de confidentialité, accès évaluateur, observation et ouverture commerciale restantes.
