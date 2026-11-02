# Interface approuvée — 2 novembre 2026

La version connectée **0.1.6 (7)** intègre le rendu Material 3 validé par Fabien : fond chaud, actions graphite, création ambre, statut actif vert, préparation ambre et pause neutre. Les composants Android restent natifs, avec la police système. La liste, le détail, la création, les résultats et le compte partagent désormais cette présentation.

Les dates sont côte à côte avec leur libellé au-dessus ; elles s’empilent lorsque le texte est agrandi. L’actualisation reste dans la barre supérieure et par geste sur la liste. Le chargement initial présente une ligne de progression et une structure de liste, sans annoncer prématurément un compte vide. Les titres de section, surfaces et boutons suivent les maquettes validées. L’accès au compte utilise les initiales du compte connecté.

La présentation ne change pas les règles du moteur : les états restent fournis par le service, les commandes en cours ne sont pas rejouées, et les limites réelles restent accessibles. Les textes génériques répétés sont retirés ; les erreurs et restrictions utiles restent affichées. La permission de notification utilise une ligne compacte avec son action. La confirmation repart en haut lors du changement d’étape, sans perdre la sélection de ses options.

## Vérifications

- 63 tests unitaires remote et 80 demo : aucun échec.
- 21 tests natifs remote uniques : six parcours complets avec la véritable activité, Hilt, ViewModels, DataStore, Keystore et repositories HTTP ; un parcours visuel intégré ; quatorze régressions de composants, navigation, synchronisation et pause/reprise.
- Le parcours visuel intégré est également repassé en sombre et à 130 % de taille de texte. Ces deux répétitions ne sont pas comptées comme de nouveaux tests uniques.
- Cinq parcours natifs de simulation passent après correction d’un callback qui effaçait la saisie avant analyse. Cette variante reste séparée de l’application connectée.
- Compilation des deux variantes, Lint sans erreur ni avertissement, frontières Android et diff contrôlés. Les treize suggestions de mise à jour par variante restent informatives.
- 28 captures natives téléphone API 36 : neuf écrans/états en clair, sombre et texte agrandi, plus l’erreur au chargement initial. La revue indépendante les a toutes comparées aux neuf références approuvées : **ship**, sans correction matérielle restante dans ce périmètre.
- APK connecté reconstruit avec configuration cloud, sans option de service local. Licence Material Symbols et absence des valeurs littérales de secrets serveur connus vérifiées ; cela ne constitue pas un audit de sécurité général.

Les [preuves détaillées](checks.json) contiennent les empreintes des captures et des APK. Les captures sous `.impeccable/review/approved-ui-2026-11-02/` traversent les véritables écrans face à des réponses HTTP locales contrôlées ; leurs offres et dates sont illustratives. Elles ne prouvent pas une recherche fournisseur, un e-mail ou un push réel. Les 105 tests serveur du lot précédent ne sont pas réannoncés comme une nouvelle exécution : aucun code serveur n’a changé ici.

Pendant la vérification, une contrainte de mesure des lignes Material a été corrigée en déplaçant la mesure adaptative hors du ListItem. Le harnais fait défiler entièrement les commandes avant de les cliquer, y compris à texte agrandi. Les exécutions finales ci-dessus passent.

## Essai sur le téléphone

Installer `Previens-moi-0.1.6-test.apk` depuis `.local/builds/approved-ui-2026-11-02/`, par-dessus l’application existante, sans la désinstaller. Vérifier la liste, un détail actif/en pause, Nouveau suivi vierge, la confirmation et un résultat déjà enregistré. Le thème suit celui du téléphone ; les boutons et textes doivent rester lisibles avec une taille de police augmentée.

Aucune recherche réelle n’est nécessaire pour vérifier cette passe visuelle. Confirmer un nouveau suivi ou reprendre une surveillance peut ensuite déclencher le moteur et consommer l’enveloppe existante.

Coût fournisseur supplémentaire de ce lot : **0 €**. Plafond de **2 €** et quotas inchangés. Aucun serveur, abonnement, base ou secret modifié ; aucun push GitHub. L’essai OnePlus de cet APK reste à effectuer par Fabien. La matrice complète des versions Android, TalkBack sur appareil, l’observation prolongée, la signature et les contrôles de publication restent ouverts. Cette livraison ne signifie pas « prête à publier à 100 % ».
