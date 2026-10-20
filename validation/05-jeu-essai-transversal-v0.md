# Jeu d'essai transversal v0 — admission et surveillance

Préparé le 4 octobre 2026. Mis à jour le 9 octobre 2026. **Corpus global non gelé ; deux recherches Exa et un test Firecrawl Search public exécutés ; comparaison ponctuelle PRIX-01 seulement, aucune surveillance récurrente.** Ces formulations reprennent les exemples discutés avec Fabien ; elles servent à éprouver les mécanismes et les refus honnêtes, pas à décider des catégories commerciales de Préviens-moi. La [matrice](04-matrice-de-faisabilite-et-couts.md) explique pourquoi quelques succès ne prouvent pas une couverture générale. Le [protocole](03-protocole-comparatif-des-moteurs.md) fixe les mesures de qualité.

| ID | Formulation d'essai | Difficulté principale | Réponse d'admission attendue avant tout résultat |
| --- | --- | --- | --- |
| A1 | « Préviens-moi dès qu'une nouvelle offre de développeur Android paraît sur Internet. » | Découverte de nouvelles annonces dispersées ; doublons et couverture inconnue. | Proposer une portée géographique et des critères d'emploi ; annoncer qu'aucune recherche ne couvre toutes les offres ni tous les sites. |
| A2 | « Préviens-moi quand un concert de mon artiste préféré près de chez moi est annoncé. » | Entité et lieu manquants ; annonce, prévente et mise en vente sont distinctes. | Demander artiste et zone, puis préciser le fait réellement surveillé. |
| A3 | « Préviens-moi s'il va pleuvoir demain pendant que mon linge sèche dehors. » | Prévision locale, créneau horaire, probabilité et incertitude. | Demander lieu et plage horaire ; annoncer un seuil de prévision, pas une certitude météorologique. |
| A4 | « Préviens-moi quand il y a une restriction d'eau dans ma commune. » | État administratif local qui peut être publié ou modifié sur une source connue. | Demander la commune ; ne promettre l'activation que si une source officielle exploitable et une cadence réelle sont identifiées. |
| A5 | « Préviens-moi si le prix de ce produit baisse. » | Variante, vendeur, livraison et prix personnalisé ; page existante plutôt que nouvelle publication. | Demander produit ou page, variante, type de prix et seuil ; refuser la promesse d'un prix exhaustif sur tous les vendeurs. |
| A6 | « Préviens-moi s'il se passe quelque chose d'important en politique. » | Critère subjectif et périmètre indéfini. | Demander quels faits, acteurs ou sujets comptent et dans quel pays ; ne pas activer sur le seul mot « important ». |
| A7 | « Préviens-moi dès que ma banque effectue un prélèvement. » | Donnée privée de compte. | Expliquer qu'un moteur Web n'a pas cet accès et refuser tant qu'aucune connexion bancaire autorisée n'existe. |
| A8 | « Préviens-moi quand cette page publique change sur le fond. » avec une URL fournie | Comparaison d'une page connue, bruit de mise en page et accès intermittent. | Vérifier que la page est accessible et préciser quels changements comptent ; distinguer une panne d'une absence de changement. |

Pour rendre l'admission testable sans inventer de capacité, **la référence proposée par Codex, encore à valider par Fabien**, est : **A1** demander une zone et, si indispensable, le type de poste ; **A2** demander l'artiste, la zone et le stade de l'annonce attendu ; **A3** demander le lieu, le créneau et un seuil de risque ; **A4** demander la commune puis vérifier une source officielle avant activation ; **A5** demander le produit exact, la variante, la source et le prix pertinent ; **A6** demander quels faits politiques comptent et où ; **A7** refuser faute d'accès bancaire autorisé ; **A8** vérifier l'URL et demander quels changements de fond comptent. Aucune des huit phrases initiales ne justifie à elle seule une promesse de détection exhaustive ou instantanée. Cette proposition n'est ni un jugement humain indépendant validé, ni un résultat du futur analyseur.

## Premier essai, volontairement réduit

**Règle d'arrêt de ce premier essai : 0 € facturé, aucun moyen de paiement ajouté, aucun abonnement et aucune surveillance récurrente créée.** Les offres gratuites affichées le 5 octobre 2026 sont de [10 $ de crédits mensuels chez Exa](https://exa.ai/pricing) et [1 000 crédits mensuels chez Firecrawl](https://www.firecrawl.dev/pricing), sans carte annoncée. Les deux [API de recherche Exa](https://exa.ai/docs/reference/search) et [Firecrawl](https://docs.firecrawl.dev/api-reference/endpoint/search) demandent une clé de compte ; un résultat obtenu avec un autre outil de recherche ne serait pas un essai de ces fournisseurs. Le 9 octobre, Fabien a créé son compte Exa et sa clé ; le tableau de bord affichait 20 $ de crédits gratuits et aucun moyen de paiement. La clé n’a été ni lue ni enregistrée par Codex. Les recherches ont été exécutées dans Search Playground ; l’authentification depuis un script local reste non testée.

1. **Référence d'admission, sans fournisseur :** faire relire à Fabien les réponses proposées pour A1 à A8, puis figer la question minimale et la limite à afficher. Les cas A6 et A7 ne doivent jamais devenir actifs sur la seule formulation initiale. Ce travail prépare un test futur de l'analyse automatique ; il ne prouve pas encore que l'IA interprète correctement les demandes.
2. **Contrôle d'intégration ponctuel, après obtention de clés gratuites :** utiliser un cas grand public issu des familles du protocole, indépendant du profil professionnel de Fabien, et une même recherche de dix résultats au maximum chez Exa Search et Firecrawl Search. Le cas de prix AF300EU ci-dessous remplace A1 pour la suite ; l’ancien appel A1 est conservé comme historique technique, pas comme référence produit. Préparer la requête exacte et noter l'heure avant le premier appel. **Maximum : un appel de recherche de base par fournisseur, sans extraction de pages, résumé IA, agent ni relance automatique.** Une erreur d'authentification ne justifie pas des appels répétés. Vérifier dans leurs tableaux de bord qu'aucune facture ni option payante n'est activée.
3. **Lecture humaine des résultats :** enregistrer pour chaque URL l'accessibilité, la source, la date vérifiable ou inconnue, la correspondance avec le métier et le lieu, les doublons et le coût indiqué. Classer tout ancien résultat comme état initial, jamais comme nouvelle alerte. Ce contrôle dit seulement si l'intégration fournit des candidats exploitables ; sans suivi dans le temps ni référence indépendante des faits nouveaux, il ne mesure pas le rappel du moteur.
4. **Décision de poursuite :** si les résultats sont interprétables et le coût borné, préparer un cas de découverte contrasté et un essai récurrent de sept jours. Sinon, corriger une erreur de formulation ou arrêter la piste ; ne pas convertir automatiquement les 50 € de plafond préparatoire en dépenses. Une page connue et une donnée structurée seront comparées uniquement pour leurs mécanismes, sans catalogue d'API par sujet.

Le premier critère de passage est donc modeste : **une clé gratuite utilisable, une réponse exploitable par fournisseur, une consommation vérifiée et aucune promesse de nouveauté déduite d'une recherche ponctuelle**. Les montants et conditions doivent être revérifiés au moment des appels. **Aucun fournisseur ni budget de production n'est validé par ce fichier.** La tranche Android du [plan](../docs/10-plan-de-validation-technique.md) peut avancer sur des données de démonstration annoncées comme telles.


## Essai grand public exécuté le 9 octobre 2026

**Correction demandée par Fabien :** partir des familles déjà retenues — prix/promotions/stock ; nouvelles annonces (logement, occasion, emploi) ; météo du quotidien ; événements/billetterie ; voyages ; actualité d’un sujet, dont société/politique. Ce sont des hypothèses de besoins, sans classement statistique prouvé. Ne plus choisir par défaut un besoin de développeur parce que Fabien est développeur. A1 reste un exemple historique, pas la demande de référence du produit.

**Cas PRIX-01, synthétique et critères figés avant appel :** « Préviens-moi si l’airfryer Ninja AF300EU neuf passe sous 150 €, livraison comprise en France métropolitaine, sans abonnement ni reprise. » Les occasions, autres références et promotions expirées sont exclues. La possibilité d’acheter et les frais doivent être vérifiables. L’observation actuelle constitue l’état initial ; elle ne prouve aucune baisse.

**Exécution :** un appel Exa Search Auto, 10 résultats, sans contenu additionnel, résumé ni agent. 2,788 s ; coût retourné 0,007 $. Dix URL distinctes, aucune date `publishedDate` retournée. Deux appels Search cumulés avec l’ancien essai Android, soit 0,014 $ de coûts retournés, couverts par les crédits gratuits ; tableau de bord à 19,99 $ après arrondi. Aucun abonnement ni surveillance créé.

**Audit par Codex :** dix résultats examinés via pages publiques et navigateur ; un marchand Pixmania suivi depuis Idealo. Ce travail vérifie les candidats ponctuellement et ne constitue pas un pipeline automatisé déjà développé. L’audit détaillé se trouve dans [audit.csv](preuves/2026-10-09-exa-prix/audit.csv) et [bilan.json](preuves/2026-10-09-exa-prix/bilan.json), avec [requête](preuves/2026-10-09-exa-prix/request.json), [réponse brute](preuves/2026-10-09-exa-prix/response.json) et captures.

- Amazon : AF300EU indisponible ; les prix bas visibles concernent des alternatives.
- Cdiscount : page de recherche mêlant références différentes et accessoires, pas une offre probante.
- Auchan : 139 €, mais « bientôt dispo » ; livraison domicile 13 €, soit 152 €. Relais gratuit annoncé mais disponibilité non confirmée.
- Darty : produit neuf à 184,37 € + 3,99 €, hors seuil.
- Dealabs : promotion publiée le 6 juin 2025, expirée le 7 juillet 2025.
- Phonandroid : article publié le 4 août 2025, promotion finie le 5 août 2025. Les actualités récentes en pied de page ne datent pas l’offre.
- Ubaldi : lecture Web à 180 € et délai de six semaines ; navigateur resté vide. Prix non revérifié en direct.
- E.Leclerc : indisponible, aucun prix exploitable.
- Idealo et Les Numériques : même offre Pixmania reprise, à dédupliquer ; le reconditionné à 129 € chez Les Numériques est exclu.
- Pixmania, preuve complémentaire : 143,90 € neuf + 4,99 € de livraison estimée = 148,89 €, vendu par Tienda CPU. Suffixe EU non confirmé, titre AF300 et puissance contradictoire avec les caractéristiques. **Candidat à confirmer**, pas alerte certaine. Autre offre PC Componentes indiquée par Idealo non vérifiée : navigation rejetée pour URL relative invalide ; aucun contournement.

**Verdict :** découverte de sources utile, preuve d’une alerte de prix non acquise. Aucune alerte certaine validée avec ces critères stricts ; cela ne prouve pas qu’aucune offre conforme existe. La réponse Search seule ne comporte aucun prix ni stock. Lire la source marchande et contrôler les variantes, livraison, disponibilité, expiration et doublons sont nécessaires. Ce besoin relève d’abord de la découverte de sources puis du suivi de leur état (D puis P), pas seulement de la détection de nouvelles pages.

**Limites financières :** 0,007 $ couvre la recherche seule. Les consultations réalisées par Codex ne démontrent ni le coût ni la fiabilité d’une extraction automatisée en production. Aucun prix de revient d’alerte ni fournisseur choisi.

**Suite ordonnée :** comparer la recherche Firecrawl sur la requête figée et auditer les candidats avec les mêmes critères ; comparer séparément l’accès automatisé aux preuves marchandes. Un second cas de découverte sera pris dans événements/billetterie pour éviter de conclure sur le moteur général à partir du seul prix. Il sera figé avant appel. Le test récurrent reste ultérieur ; aucune tâche automatique n’est active.


## Comparaison Firecrawl exécutée le 9 octobre 2026

**Accès réel :** Playground public Firecrawl, sans connexion, création de compte, carte ni paiement. Un seul lancement Search, dix résultats Web, extraction désactivée. Même requête exacte que pour Exa PRIX-01 ; pas de filtre pays ni période ajouté : France et critères figurent dans la requête. La requête sauvegardée avant lancement conserve son statut « prepared » ; le bilan et les captures attestent l'exécution. [Exécution visible](https://www.firecrawl.dev/playground/01a11fb6-2fa7-762f-8875-87a923887400).

**Périmètre de preuve :** démo Search réussie, pas intégration API authentifiée. Le JSON complet exige un compte ; seules les sorties publiques visibles sont conservées. Aucun accès caché ni contournement. [Requête](preuves/2026-10-09-firecrawl-prix/request.json), [résultats visibles](preuves/2026-10-09-firecrawl-prix/results-visible.txt), [audit des dix liens](preuves/2026-10-09-firecrawl-prix/audit.csv), [bilan](preuves/2026-10-09-firecrawl-prix/bilan.json), [capture](preuves/2026-10-09-firecrawl-prix/resultats-final.jpg).

| Résultat | Vérification et décision |
| --- | --- |
| SharkNinja | Page à 139,99 €, mais référence DZ300EU : autre modèle. Exclu. |
| YouTube | Vidéo d'avis ; titre et lecteur accessibles, contenu et date non vérifiés. Aucune preuve d'offre actuelle. |
| Amazon | AF300EU actuellement indisponible. Exclu. |
| Cdiscount | Liste mélangeant produits et accessoires ; aucune offre exacte validée. |
| Dodeal | AF300EU neuf à 145,78 €, livraison gratuite annoncée chez PC Componentes. Mise à jour du comparateur : 9 octobre 2026 à 06:37. Candidat, pas confirmation marchande. |
| Galaxus Pays-Bas | Extrait de recherche à 174,99 € ; consultation arrêtée au CAPTCHA. Prix et livraison non revérifiés. |
| Back Market | Après écran de sécurité transitoire, page accessible automatiquement : 129 € reconditionné, état correct. Exclu. 229 € est la comparaison avec le neuf. |
| MediaMarkt Belgique | Page à 239,99 €, hors seuil ; livraison annoncée dès le 12 octobre, destination France non établie. |
| Facebook | Vidéo promotionnelle ; titre visible, connexion demandée pour le contenu. Aucune offre actuelle vérifiée. |
| Darty | AF300EU neuf à 184,37 € + 3,99 € ; hors seuil. Livraison annoncée dès le 19 octobre. |

**Dates :** une mise à jour de comparateur, une date de livraison, une date d'avis et une date de publication sont des informations différentes. La mention « 2 days ago » de l'extrait Dodeal ne correspond pas à la mise à jour du 9 octobre affichée sur la page. Sans preuve marchande, ni cet extrait ni le comparateur ne permettent d'affirmer que l'offre est achetable maintenant. Les dates inconnues restent inconnues.

**Comparaison limitée :** les deux recherches remontent des sources candidates et des résultats hors critères. Amazon et Darty sont deux URL communes ; Cdiscount est aussi commun, avec une URL de recherche différente. Dodeal retrouve les pistes PC Componentes et Pixmania déjà indiquées par Idealo dans l'essai Exa : plusieurs comparateurs ne constituent pas plusieurs offres indépendantes. Aucune alerte certaine validée dans l'un ou l'autre audit. Cela ne prouve pas l'absence d'offre conforme. Un seul cas ne mesure ni la couverture générale, ni le rappel, ni la qualité des futurs changements détectés.

**Coût :** tarif documentaire Search de [2 crédits pour 10 résultats](https://www.firecrawl.dev/pricing), sans extraction. Aucun débit observé : nous avons utilisé la démo publique, sans compte Firecrawl. Ne pas transformer ce tarif en coût réel mesuré ni en budget d'alerte. Exa a retourné 0,007 $ pour sa recherche. Les lectures manuelles des sources ne mesurent pas le coût ou la robustesse d'un traitement automatisé. Les blocages vus dans notre navigateur ne constituent pas un test de Firecrawl Scrape.

**Décision :** ne choisir aucun fournisseur sur ce seul essai. Search découvre des pages ; une alerte exige encore d'extraire et vérifier les faits puis de comparer les observations dans le temps. Les critères en langage naturel ne sont pas des filtres garantis par ces moteurs.

**Prochaine étape précise :** figer un second cas grand public de découverte dans événements/billetterie, puis exécuter au maximum une recherche de dix résultats par fournisseur et vérifier les dates, annonces officielles et doublons. Ce cas doit départager la découverte d'annonces, différente d'un prix changeant sur une page existante. Ensuite seulement, essai séparé et borné d'extraction automatique sur une source accessible. Aucune surveillance récurrente ni dépense payante n'est activée.


## Suite révisée après la revue du moteur

Le 9 octobre 2026, Fabien a demandé de tester toute la chaîne avec un prototype rigoureux. Les suites proposées plus haut sont conservées comme historique des essais. L'ordre actuel est défini dans le [plan du premier moteur](06-plan-premier-moteur.md) : critères d'acceptation, corpus, règles locales, fournisseurs réels, Web puis bilan. La comparaison événements/billetterie attend ce bilan. Le [contrat initial](07-acceptation-premier-moteur.md) gouverne les nouveaux essais. Leur [bilan séparé](09-bilan-premier-moteur.md) décrit le moteur local exécuté et la sonde Firecrawl, sans réinterpréter les essais de recherche historiques.
