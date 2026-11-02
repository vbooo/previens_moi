# Captures natives à valider

Les composables vivent uniquement dans androidTestRemote. Ils ne remplacent ni le thème ni les écrans livrés. Le test NativeUiProposalTest.captureProposalScreens nécessite uiProposal=true et BuildConfig.LOCAL_SERVICE=true.

Construire avec les paramètres locaux du projet, installer les APK app/test sur l’émulateur de qualification, puis invoquer uniquement ce test avec -e uiProposal true. Les images proposal-<état>.png sont enregistrées dans files de l’application de qualification ; les rapatrier dans .impeccable/review/proposal-2026-11-02.

Assembler les deux planches avec assemble-boards.swift en lui passant le chemin absolu des captures. Le rapport largeur/hauteur est lu depuis les PNG. Les légendes restent en dehors des écrans.

Couverture : cinq états clairs, deux sombres et le détail avec texte agrandi à 130 %, y compris son défilement vers les sources. Cela ne qualifie pas toutes les combinaisons d’état, thème, texte et appareil. La revue indépendante accepte la proposition à ce périmètre. La validation visuelle de Fabien reste nécessaire avant intégration.
