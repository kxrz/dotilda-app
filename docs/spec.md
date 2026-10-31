# Dotilda - spec app journaling e-ink

Oct 28, 2026 - @Florent Bertiaux

Une app de journaling douce et ludique pour le Mudita Kompakt et les liseuses Android 11+, petite soeur de readme.club, pensée comme un Petit Bambou de l'écriture. Gratuite à 100 % et open source (Apache 2.0).

## Nom et personnage

Nom retenu : **Dotilda**, contraction de Dot et Matilda. C'est à la fois le nom de l'app et celui du personnage qui guide l'utilisateur : le point qui pose la question du jour et veille sur la galaxie. Sa devise : *nulla dies sine linea*, "pas un jour sans une ligne", d'après Pline l'Ancien racontant que le peintre Apelle traçait au moins un trait chaque jour.

**La voix de Dotilda**

| Moment | Ce que fait Dotilda | Exemple de ton |
| --- | --- | --- |
| Question du jour | Pose la question et la signe | "Dotilda asks: what made you smile today?" |
| Après une entrée | Remplit le point, un mot bref | "Noted. One more dot in your sky." |
| Retour après une pause | Accueille sans reproche | "There you are. Your sky kept your place." |
| Fin de parcours | Trace la constellation et la nomme | "Seven days, seven dots. I'll call this one Slow." |
| Bilan du mois | Commente avec tendresse | "A cloudy start, a sunny finish. Rather well done." |

Personnalité : une gouvernante victorienne bienveillante, un brin malicieuse, jamais moralisatrice. Phrases courtes, pas d'exclamations en série, aucune culpabilité. Visuellement, Dotilda est un simple point à l'encre avec deux détails (un chignon, des lunettes rondes), lisible à 16 pixels pour l'icône. L'icône de l'app reprend ce dessin en trait, comme les icônes du Kompakt.

**Les poses de Dotilda.** Sur e-ink, pas d'animation fluide : Dotilda change de pose d'un coup, comme une page qu'on tourne (un seul rafraîchissement). Quatre poses : neutre, lunettes au bout du nez (elle pose la question), lunettes de travers (une autre question, un brin malicieuse), petit sourire (c'est noté). Sur l'écran Done, elle remonte ses lunettes et sourit, en une seule étape, jamais en boucle.

Conventions : Dotilda s'écrit avec une majuscule, comme un prénom. L'interface est en anglais. La devise latine reste une référence interne : elle n'apparaît pas dans l'app. Le module minuteur garde son nom latin, **Spira**, "respire" : c'est l'exercice que Dotilda propose avant d'écrire.

Domaine réservé : dotilda.com. Reste à vérifier : les handles réseaux et l'absence d'app homonyme sur le Play Store et F-Droid.

## Concept et principes

Une question par jour, une réponse courte, et une galaxie de points qui se remplit au fil des jours, un point par jour. L'utilisateur passe moins de 2 minutes par jour dans l'app : c'est un rituel, pas un fil.

1. **Une seule interaction par jour.** Un rendez-vous à l'heure choisie, pas de flux infini. Un bouton "Another question" permet de changer, trois fois max par jour. Ensuite, Dotilda propose simplement de noter son état d'esprit (la météo intérieure), ou de revenir demain.
2. **Rien ne meurt.** Sauter des jours ne casse rien : la place du jour reste un petit point discret, sans reproche. Pas de streak punitive, pas de compteur rouge.
3. **Des formats variés.** Huit types de questions alternent pour que l'écriture reste un jeu, y compris les jours sans énergie.
4. **Récompenser le recul.** Les vrais moments forts sont les bilans (mois, saison, année) et les questions miroir, où l'on se relit.
5. **Hors ligne et privé.** Aucun compte, aucune donnée envoyée. Tout reste sur l'appareil, exportable à tout moment. Seule exception réseau : la vérification des mises à jour, qui ne transmet rien sur l'utilisateur et se désactive dans Settings.
6. **Pensé pour l'e-ink.** Un point qui se remplit plutôt qu'une animation ; contraste fort, gros caractères, zéro scroll.

Références d'esprit : Petit Bambou pour la douceur et les parcours, readme.club pour le ton et le design, les carnets papier pour la matière.

## Types de questions

Huit formats, chacun avec sa saisie et sa restitution dans les bilans. Le moteur alterne les formats pour éviter deux jours identiques d'affilée.

| Type | Exemple | Saisie | Restitution |
| --- | --- | --- | --- |
| One line | What made you smile today? | Texte, 140 caractères max | Citation dans le bilan mensuel |
| Inner weather | How's the weather inside? | 5 pictos soleil à orage | Frise météo du mois |
| One word | Today in one word? | Un mot | Nuage de mots mensuel |
| This or that | Tea or coffee kind of day? | 2 boutons | Statistiques ludiques en fin de saison |
| Three things | 3 small wins this week | 3 lignes courtes | Liste dans le bilan |
| Photo of the day | Show me your desk | Appareil photo, image tramée | Planche contact mensuelle |
| Mirror | What are you looking forward to? | Texte ; même question tous les 30 jours | "30 days ago, you wrote..." affiché avant la saisie |
| Letter to future me | Write to yourself in 6 months | Texte long, scellé | Rouvre à la date choisie, comme un cadeau |

Règles du moteur :

- **Jours lourds** : si la météo intérieure de la veille est "orage", proposer un format léger (This or that, One word).
- **Rythme** : au moins une Mirror par mois et une Letter par saison.
- **Banque de départ** : plus de 200 questions en anglais, taguées par format, thème et parcours, livrées en JSON dans l'app. Le but : que l'utilisateur ne sente jamais qu'il tourne en rond.
- **Changer de question** : 3 fois max par jour, sans jamais retomber sur une question déjà proposée ce jour-là. Au-delà, seule la météo intérieure reste proposée.

## Gamification

Trois boucles imbriquées : les points récompensent la régularité, les parcours tracent des constellations, les bilans récompensent le recul. Aucune ne punit l'absence.

**La galaxie (boucle quotidienne)**

- Un point par jour, bien rangé : 12 lignes (les mois) de 31 colonnes (les jours). L'année entière tient sur un écran, dans l'esprit de Year in Pixels, mais en points.
- Le point prend la forme de la réponse. La météo intérieure devient une phase de lune (point plein pour le soleil, simple anneau pour l'orage). Photo = point cerclé, Mirror = double cercle, Letter = losange scellé qui s'ouvre à sa date.
- Jour sans entrée : un tout petit point gris, une respiration dans la grille, jamais un trou ni une alerte.
- Pas de trait pour une semaine (ou un mois) complète : les points restent libres. La régularité se voit à la densité du ciel, pas à une ligne qui récompenserait la série. Les seuls traits possibles sont ceux des constellations de parcours (sprint 4).
- Les années s'empilent dans la **Galaxy** : une page par année à feuilleter ; toucher un point rouvre l'entrée du jour.

**Les parcours (boucle hebdomadaire)**

- Des séries thématiques de 5 à 10 questions, une par jour : "7 days to slow down", "Gratitude", "Season review", "Unplugged".
- Chaque parcours terminé devient **une constellation, nommée d'après le parcours** : ses jours s'allument en étoiles (quatre petits rayons autour du point), sans aucun trait entre eux, et son nom s'affiche sous la galaxie.
- Un seul parcours actif à la fois ; hors parcours, la question du jour vient de la banque générale.

**Les bilans (boucles longues)**

- **Mois** : une page générée avec la frise météo, le nuage de mots, une citation de soi et la planche photo.
- **Saison** : le trimestre en points, les stats This or that, la lettre scellée de la saison précédente qui se rouvre.
- **Année** : le journal complet exporté en **livre EPUB**, avec en couverture la galaxie de l'année, à lire sur sa liseuse.

Garde-fous : pas de notifications de relance, pas de classement, pas de partage social intégré. Le seul rappel est le rendez-vous quotidien choisi par l'utilisateur.

## Écrans

Neuf écrans, une navigation à plat : l'accueil mène à tout, chaque écran revient à l'accueil en un geste ou un bouton.

| Écran | Contenu | Actions |
| --- | --- | --- |
| Today (accueil) | La ligne du mois en points, la question du jour, le parcours en cours | Répondre, autre question (3 max), ouvrir le carnet |
| Answer | Question + saisie selon le type ; pour Mirror, la réponse d'il y a 30 jours | Valider, passer à Spira avant d'écrire |
| Done | Le point du jour qui se remplit, constellation si parcours fini | Retour accueil |
| Notebook | Entrées par mois, une page par jour, pagination | Lire, filtrer par type ou parcours |
| Journeys | Parcours disponibles, en cours, terminés | Démarrer un parcours |
| Galaxy | Toutes les années en points, constellations des parcours | Feuilleter les années, toucher un point pour relire le jour |
| Review | Bilan du mois ou de la saison | Lire, exporter en image |
| Spira | Minute de respiration et minuteur | Lancer, régler |
| Settings | Heure du rendez-vous, sauvegarde, export EPUB, à propos | Exporter, importer |

Onboarding : un écran d'accueil où Dotilda se présente en trois promesses courtes (accessible ensuite via Settings > About Dotilda), le choix de l'heure du rappel, puis la première question tout de suite.

## Module Spira

Spira comble un manque réel du Kompakt (pas de minuteur ni de chrono en stock) et sert de sas avant d'écrire. Il vit dans Dotilda en v1 ; une sortie en app autonome se décidera selon l'usage.

- **Minute de respiration** : un cercle qui change de taille par paliers (inspire 4 s, expire 6 s), sans animation fluide : 3 états fixes, compatibles e-ink. Vibration légère à chaque changement.
- **Minuteur** : durée réglable par gros boutons +1 / +5 min, alarme à la fin, fonctionne écran éteint.
- **Chronomètre** : démarrer, arrêter, tours ; affichage rafraîchi chaque seconde en mode rapide, chaque minute sinon pour économiser la batterie.
- **Lien avec le journal** : depuis Answer, un bouton "breathe first" lance une minute puis revient à la question.

## Contraintes e-ink et Kompakt

Cible principale : Mudita Kompakt (MuditaOS K, base AOSP 12, sans services Google). Cible secondaire : liseuses Android 11+, comme l'app readme.club.

- **Thème MMD** : utiliser la bibliothèque Mudita Mindful Design (Compose, Apache 2.0) pour un rendu natif Kompakt ; ripples désactivés, palette monochrome.
- **Zéro animation** : transitions instantanées, aucun scroll ; pagination partout, comme sur une liseuse.
- **Illustrations** : les points sont dessinés en Canvas Compose, noir pur, sans dégradés ; la galaxie est redessinée une fois par entrée, jamais animée.
- **Photos** : tramage Floyd-Steinberg en 4 niveaux de gris côté appareil, stockées déjà tramées.
- **Rafraîchissement** : rafraîchissement complet après chaque entrée validée et à chaque changement de mois dans le carnet, pour effacer le ghosting.
- **Saisie** : questions courtes, champs larges, compatibles avec le clavier Kompakt (38 langues depuis la 1.6) et FUTO.
- **Écran du Kompakt** (relevé sur MuditaOS K 1.6.1, API 31) : 480 × 800 px, 213 dpi (densité ×1,33), soit **360 × 564 dp** utiles pour l'app.
- **Boutons physiques** : volume haut = page précédente (`KEYCODE_VOLUME_UP`, 24), volume bas = page suivante (`KEYCODE_VOLUME_DOWN`, 25), uniquement sur les écrans paginés ; ailleurs, le volume garde son rôle. La touche retour sous l'écran envoie `KEYCODE_BACK` (4), le retour système standard.
- **Batterie** : aucun service en arrière-plan ; un seul rappel quotidien planifié avec AlarmManager.
- **Notifications** : la 1.6 gère la vibration des alertes d'apps tierces ; vérifier sur le launcher stock comment s'affiche le rappel.

## Architecture et modèle de données

Même socle que l'app readme.club pour réutiliser le savoir-faire : Kotlin, Jetpack Compose avec MMD (`com.mudita:MMD`), Room comme source unique, minSdk 30. Aucun réseau. Identifiant : `com.dotilda.app`.

**Stack**

- UI : Compose + MMD ; navigation Compose à plat.
- Données : Room (SQLite) ; banque de questions et parcours en JSON dans les assets, resynchronisés à chaque lancement (une mise à jour de l'app peut corriger ou ajouter des questions). Préférences dans DataStore.
- Photos : CameraX, tramage puis stockage dans le dossier privé de l'app.
- Rappel : AlarmManager exact, réarmé au redémarrage.
- Export : génération EPUB 3 locale (XHTML + images dans un zip), sauvegarde JSON + photos dans un zip, via le sélecteur de fichiers Android.

**Tables**

| Table | Champs principaux | Rôle |
| --- | --- | --- |
| Question | id, type, text, options, tags, journeyId?, mirrorGroup? | Banque de questions |
| Entry | id, date, questionId, type, payload (JSON), photoPath? | Les réponses ; la première du jour donne sa forme au point |
| Journey | id, title, length, constellationName | Parcours |
| JourneyProgress | journeyId, step, startedAt, completedAt? | Avancement |
| Constellation | id, journeyId, dates, completedAt | Constellations tracées par les parcours |

Le payload JSON varie selon le type (texte, mot, index météo, choix, trois lignes, lettre avec sa date de déscellement) pour garder une seule table Entry simple à exporter. La forme du point se calcule depuis l'entrée du jour : pas de table de cache. Les préférences (heure du rendez-vous, compteur de changements de question du jour) vivent dans DataStore.

## Modèle et distribution

Dotilda est **gratuite à 100 %** : pas d'achat, pas d'abonnement, pas de licence, pas de publicité. Tous les parcours, bilans et exports sont inclus. Le code est **ouvert** sous licence Apache 2.0, comme MMD (le dépôt reste privé pour le moment).

**Mises à jour dans l'app** (même mécanique que l'app readme.club) : au lancement, l'app lit `manifest.json` sur la dernière release GitHub (version, URL de l'APK, SHA-256, notes du CHANGELOG). Si une version plus récente existe, Today l'annonce discrètement ; l'écran de mise à jour montre les notes, télécharge l'APK, vérifie son empreinte et ouvre l'installeur d'Android, qui demande confirmation. Les builds de test (`com.dotilda.app.debug`) ne se mettent pas à jour. Les releases doivent être téléchargeables sans compte GitHub.

**Distribution**

1. APK sur GitHub Releases, installable via Mudita Center ou Obtainium.
2. Fiche dans le Community App Ranking du forum Mudita.
3. Candidature à la plateforme Inkapps dès l'ouverture de MuditaOS K 2.0.
4. Page dédiée et article sur readme.club ; F-Droid à étudier une fois le dépôt public.

## Plan en sprints

Six sprints courts, chacun livrable sur le Kompakt de test. La v1 publique sort après le sprint 3 ; 4 et 5 enrichissent. Chaque push compile un APK de debug via GitHub Actions (onglet Actions, artefact `dotilda-debug-apk`).

En parallèle des sprints 1 et 2 : rédaction de la banque complète (plus de 200 questions).

1. ✅ **Sprint 0, socle** : projet Kotlin + Compose + MMD, Room, import JSON des questions, écran Diagnostics pour relever les keycodes et la résolution du Kompakt.
   - Fait quand : l'app s'installe via Mudita Center et affiche une question tirée de la banque.
2. ✅ **Sprint 1, rituel** : écrans Today, Answer, Done ; les formats One line, One word, Inner weather, This or that ; rappel quotidien.
   - Fait quand : une semaine d'usage réel sans bug, rappel reçu chaque jour à l'heure.
3. ✅ **Sprint 2, carnet et galaxie** : Notebook paginé, galaxie de l'année et ligne du mois, encodage des formes de points ; formats Three things et Mirror.
   - Fait quand : chaque entrée remplit le bon point avec la bonne forme, et les jours vides restent discrets.
4. ✅ **Sprint 3, Spira et sauvegarde** (v0.4.0) : module Spira complet, export et import zip, onboarding ; publication GitHub + Community App Ranking.
   - Fait quand : restauration complète d'un journal sur un second appareil.
5. ✅ **Sprint 4, profondeur** (v0.5.0) : parcours et constellations, bilan mensuel, Photo of the day avec tramage, Letter to future me.
   - Fait quand : un parcours de 7 jours se termine en traçant sa constellation.
6. **Sprint 5, recul** : bilans de saison, export EPUB annuel, styles de points et de galaxie.
   - Fait quand : l'EPUB généré s'ouvre proprement dans KOReader.

## CLAUDE.md

Voir `CLAUDE.md` à la racine du dépôt.

## Questions ouvertes

- [ ] Réserver les handles Dotilda et vérifier les homonymes sur les stores (dotilda.com réservé).
- [x] Langue : anglais seul en v1, textes dans `strings.xml` pour ouvrir d'autres langues ensuite.
- [x] Open source, Apache 2.0 (dépôt privé pour le moment).
- [x] Disposition de la galaxie : 12 lignes de 31 (les mois), points espacés pour ne jamais former de barre, aujourd'hui marqué par un petit ▲.
- [x] Relever sur le Kompakt de test : résolution et keycodes des boutons (voir Contraintes e-ink).
- [ ] Vérifier l'affichage du rappel sur le launcher stock (sprint 1).
- [ ] Le Kompakt expose-t-il une API pour forcer un rafraîchissement complet, ou faut-il passer par un écran noir flash ?
- [x] Banque de plus de 200 questions : premier jet rédigé par Claude, puis réécrit par Florent.
- [x] Monétisation : aucune, l'app est gratuite à 100 %.
