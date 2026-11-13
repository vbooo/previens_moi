# Dépôt public et CI — 13 novembre 2026

Dépôt choisi par Fabien : [vbooo/previens_moi](https://github.com/vbooo/previens_moi), public. Le code et son historique local sont conservés ; aucun historique réécrit pour la publication. Les secrets, données de laboratoire, sauvegardes, paramètres privés du client et réglages propres à Studio restent exclus.

## Contrôle avant envoi

42 commits et 1 042 versions de fichiers examinés. Comparaison avec les six valeurs serveur locales connues et recherche de formats sensibles, puis Gitleaks 8.30.1 officiel avec archive vérifiée par son empreinte. Aucun secret confirmé. Les 22 signalements de Gitleaks concernaient exclusivement le champ `factKey` : empreinte SHA-256 calculée par le code, pas une clé d’API. L’exception associe ce champ exact aux seuls rapports de qualification concernés et à la règle générique ; aucun dossier ou commit entier exclu. Deux contrôles négatifs confirment qu’une clé générique et une empreinte hors de ce chemin restent détectées.

151 images historiques examinées localement par OCR, sans transmission à un service, sans texte OCR enregistré et sans adresse e-mail personnelle détectée ; trois captures historiques à risque relues visuellement. Ce contrôle complémentaire n’est pas une preuve absolue d’absence de toute donnée personnelle dans une image. Le marqueur littéral `BEGIN PRIVATE KEY` du script de vérification est une condition de contrôle, pas une clé embarquée.

Une copie composée uniquement des fichiers Git compile avec JDK 17, SDK local et `localService=true`, sans `client.properties`, `.local` ni réglage de daemon de Studio : deux variantes, 147 tests unitaires et Lint passent. Les 124 tests serveur, 34 exemples du contrat et Actionlint passent également. L’environnement exact et les limites sont dans [checks.json](checks.json).

## CI publique

Trois jobs sur Ubuntu standard : Android (compilation, tests, Lint, frontières), serveur/contrat (Deno 2.5.4 et validation OpenAPI), et analyse de l’historique par Gitleaks. Actions épinglées à leurs commits officiels, permissions du jeton limitées à la lecture, annulation des anciens runs d’une même branche. Aucun compte cloud, secret fournisseur, appel payant ou base active utilisés. Les tests PostgreSQL et les parcours Android instrumentés restent des qualifications explicites distinctes.

Les rapports et l’APK `demoDebug` hors ligne sont conservés sept jours dans les artefacts Android. L’APK connecté ne peut pas être configuré avec les accès privés du développeur depuis la CI. Pas de déploiement automatique Supabase ou Google Play. Les [résultats réels et le statut courant](https://github.com/vbooo/previens_moi/actions/workflows/android.yml) font foi ; ce bilan décrit les contrôles locaux et la configuration, sans inventer un résultat distant.

Le README présente le parcours, trois captures natives, le lancement sans compte, les points d’entrée du code, les choix d’architecture et les limites. La licence du code n’a pas été choisie à la place de Fabien. Les suppressions d’imports préexistantes de MainActivity restent hors du commit ; le plan Gemini et le réglage JDK généré par Studio sont conservés localement et ignorés.

Aucune dépense fournisseur supplémentaire ni nouvel abonnement. Les quotas et le plafond d’essai de 2 € de l’application sont inchangés. Publication Play, règles d’exploitation publique, confidentialité/release et observation prolongée restent à traiter séparément.
