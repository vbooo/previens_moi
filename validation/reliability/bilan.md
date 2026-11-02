# Parcours et diagnostics — 2 novembre 2026

La version connectée **0.1.5 (6)** corrige les deux défauts signalés : la liste attend sa première réponse avant d'afficher un compte vide, et « Nouveau suivi » ouvre un formulaire vierge. Un brouillon existant reste accessible par « Reprendre mon brouillon », jusqu'à ce qu'une nouvelle demande le remplace. Modifier un suivi conserve bien sa demande existante.

La restauration tardive du brouillon ne peut plus remplacer une saisie plus récente. La fermeture/réouverture de l'activité conserve la saisie et permet de confirmer une proposition restaurée. Une déconnexion ou un changement de compte écarte les anciennes réponses de lecture/commande et quitte les vues privées ; la déconnexion ne laisse plus un chargement indéfini. Une panne au premier chargement affiche un échec de lecture, sans prétendre que le compte n'a aucun suivi. Une panne ultérieure garde les données déjà confirmées.

Les traces Android sont disponibles sous le tag `PreviensMoi` : type d'action, résultat, durée, statut HTTP et UUID de requête validé. Les fonctions serveur publient le même UUID dans l'en-tête `X-Request-Id`, le corps d'erreur et leur trace structurée. Aucun texte de demande, e-mail, code, jeton, URL ou message d'exception brut n'entre dans ces nouvelles traces applicatives. Cela ne couvre pas les journaux propres à Android ou à l'infrastructure Supabase. Voir [le guide de diagnostic](../../docs/diagnostics.md).

## Vérifications

- 63 tests unitaires remote et 80 demo, sans échec.
- 18 parcours natifs uniques sur l'émulateur API 36 : six traversent la véritable activité, Hilt, les ViewModels, DataStore, Keystore et les clients HTTP face à un service local contrôlé ; douze conservent les régressions de composants/navigation. Les six parcours complets sont repassés avec les traces finales, dont la confirmation après recréation de l’activité.
- 105 tests serveur, dont trois sur la corrélation et la confidentialité des diagnostics ; contrôle de types réussi.
- Lint des deux variantes : aucune erreur ni avertissement ; contrôle des frontières Android et diff sans anomalie.
- Inspection groupée des trois états modifiés : chargement, accueil avec brouillon, nouveau formulaire vierge. Aucun débordement observé ; la passe visuelle dédiée reste à faire.
- Diagnostics déployés sur `public`, `api`, `webhook`, `worker`. Santé publique 200 et refus authentification 401 relus sans compte ; UUID présent et corrélé sur l'erreur. Aucune migration, modification de quota ou invocation manuelle du moteur.
- APK réel reconstruit sans configuration locale ; licence et recherche de secrets serveur connus contrôlées. Cette recherche de valeurs littérales n'est pas un audit de sécurité général.

Les [preuves détaillées](checks.json) séparent tests locaux et vérifications du service. Les deux premières tentatives du harnais natif ont nécessité de corriger ses interactions de défilement et ses connexions HTTP entre tests ; les exécutions finales passent. Aucune nouvelle recherche fournisseur ou livraison FCM réelle n'a été déclenchée.

## Nettoyage proportionné

La remise à zéro du formulaire est centralisée au lieu d'être recopiée dans Nouveau/Modifier/Effacer. Un test erroné de suppression a été retiré : une liste locale encore vide ne prouve pas que le suivi enregistré n'existe plus. La validation authentifiée et versionnée reste côté service. Un ancien callback de prévisualisation inutilisé a été retiré de l'accueil connecté. Aucun module, bibliothèque de journalisation ou couche de cas d'usage supplémentaire n'a été ajouté ; MockWebServer est réservé aux tests.

## Essai et suite

Installer la version connectée 0.1.5 sur le OnePlus, sans désinstaller l'ancienne pour garder la session. Vérifier le lancement, Nouveau suivi vierge, reprise d'un brouillon, pause/reprise, puis ouverture d'une notification disponible. Le téléphone n'a pas été utilisé par les tests automatisés.

Coût supplémentaire des essais : **0 €**. Plafond de 2 € et quotas conservés. Pas de push GitHub. Ce lot est terminé pour son périmètre ; restent l'essai physique, la passe UI, puis les contrôles et l'observation avant publication. Aucun statut « 100 % prêt à publier » n'est revendiqué.
