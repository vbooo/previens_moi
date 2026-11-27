# Observation réelle — départ et critères conservés

L’enveloppe de 6 EUR cumulés a été approuvée et appliquée. Un compte ordinaire distinct, sans droit Plus offert, réalise l’observation via les endpoints publics/authentifiés normaux. Ses identifiants restent uniquement dans `.local/observation/` avec droits privés. Aucun envoi de code e-mail, aucun réarmement d’essai utilisateur existant et aucune notification factice ne sont générés.

## Déroulement J0 du 27 novembre 2026

1. Première demande : nouvelles versions stables de Chrome pour ordinateur, sur le blog officiel. Confirmation à 18:29:19 UTC. La préparation n’a pas créé d’état de référence : arrêt visible, état dégradé, cinq crédits Web comptabilisés. Ce passage échoué est conservé comme incident et ne compte pas comme surveillance réussie. Aucun rejeu automatique de requête fournisseur.
2. Le suivi synthétique arrêté a été supprimé par la commande normale ; le compte, sa date de fin d’essai et ses coûts ont été conservés. Une seule nouvelle demande a été autorisée dans l’enveloppe : annonces de versions stables de Python sur le site officiel, hors alpha, bêta et release candidate.
3. État de référence effectivement enregistré à **18:39:04.697 UTC / 19:39 heure de Paris**. Suivi actif, distant en marche, état sain. Deux sources retenues : https://www.python.org/downloads/ et https://www.python.org/downloads/release/python-3124/. La deuxième est une archive : elle appartient au contexte initial et ne constitue pas une annonce nouvelle du seul fait de sa découverte.
4. Cadence confirmée : 24 heures ; première vérification due le 28 novembre à 18:39 UTC. L’essai du compte se termine le 4 décembre à 18:29:19 UTC ; il n’a pas été prolongé ou réinitialisé après l’incident. Il restera donc au plus six contrôles quotidiens après cet état de référence, et non sept contrôles réussis déjà acquis.

## Attendus fixés avant les contrôles suivants

- Vérifier les deux pages annoncées à chaque passage, distinguer un échec de source d’une absence de nouveauté, et conserver les preuves exactes des résultats.
- Un numéro de version présent à J0, la réapparition d’une archive ou un changement de mise en page ne suffit pas à créer une nouvelle annonce.
- Une annonce candidate doit concerner une version stable, être publiée sur une des sources retenues et porter une différence établie par rapport à l’état de référence. Aucun pourcentage d’exhaustivité du Web ne sera déduit.
- Évaluer séparément pertinence, doublons, contrôles tardifs, erreurs/rétablissement, coûts réels et réservations inconnues. Une acceptation FCM n’est pas une preuve d’affichage sur téléphone.
- À la fin ou en cas d’arrêt, présenter les passages réellement exécutés et les incidents. Le premier échec interdit de qualifier ce lot de sept jours sans incident ; sa préparation ne doit pas être effacée du bilan.

Après préparation : dix crédits Web utilisés sur les trente réservés au compte, vingt restant engagés. Les deux pages quotidiennes restent finançables dans cette enveloppe jusqu’à la fin effective de l’essai ; aucun nouveau budget mensuel ou fournisseur n’est ouvert. Les 39 crédits et 20000 micro-USD historiques incertains restent réservés.

La version Android de référence est 0.2.12 (27), migration serveur 47. Les relevés suivants utilisent la lecture seule ; ils ne réveillent pas le moteur ni les fournisseurs. Le cron existant continue les vérifications normales, application fermée. Aucune nouvelle automatisation de suivi du travail n’est créée. L’observation serveur n’est pas un test de notification sur le téléphone personnel de Fabien.
