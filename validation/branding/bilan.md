# Logo V1 — 20 novembre 2026

Logo validé : P avec cloche en réserve, vert graphite `#303D34` sur blanc cassé `#F6F6F0`, sans point jaune. Les écrans gardent leur thème actuel.

Le manifeste utilise désormais une icône de lancement adaptative avec fond et premier plan vectoriels, calque monochrome et ressources de repli API 24–25. Les deux notifications du client connecté utilisent une petite silhouette blanche transparente, sans fond carré ; cette ressource appartient au seul source set `remote`. L’ancienne icône bleue `ic_demo` est retirée.

Les sources sont dans [assets/branding](../../assets/branding/README.md). Le PNG pour la fiche Google Play est un carré opaque sRGB 32 bits de 512 × 512, sans arrondi ou ombre ajoutés. Le dessin reste entièrement dans le cercle central de sécurité des icônes adaptatives (rayon conservateur 31,54 dp pour 33 dp autorisés).

## Vérification

- Compilation APK/AAB release signés avec le package Play définitif, version **0.2.7 (21)**.
- Lint `demoDebug`, `remoteDebug`, `remoteRelease` : zéro erreur, zéro avertissement ; suggestions de mise à jour conservées comme informations.
- Signatures APK/AAB et contrôles binaires existants réussis : [rapport](release.json).
- Icône native réellement chargée dans les informations de l’application sur un émulateur API 36 : [capture](native-app-info.png). Application non démarrée ; aucune donnée utilisateur ni appel fournisseur.
- Aperçus vectoriels des masques [rond](launcher-round-preview.png) et [carré arrondi](launcher-squircle-preview.png) inspectés. Les anciens Android et les icônes thématiques ne sont pas déclarés qualifiés sur appareil par ces seuls contrôles.

Le bundle signé et son mapping sont conservés dans le dossier privé `.local/play-test/`, sous le préfixe `previens-moi-play-release-0.2.7-21`. Aucun import Play, publication, achat, recherche Web métier, envoi d’assistance ou modification du budget n’a été effectué. La qualification des surveillances en cours reste indépendante.
