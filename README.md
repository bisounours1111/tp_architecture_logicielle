## Prérequis

- Java 17
- Maven 3.9+
- Docker et Docker Compose

## Lancement

1. Démarrer Kafka et Zookeeper depuis la racine du projet :

```bash
docker compose up -d
```

2. Démarrer le serveur de configuration :

```bash
cd config-server
./mvnw spring-boot:run
```

3. Démarrer le serveur Eureka :

```bash
cd discovery-server
./mvnw spring-boot:run
```

4. Démarrer ensuite les services métiers et la gateway dans l'ordre de votre choix :

```bash
cd room-service
./mvnw spring-boot:run
```

```bash
cd member-service
./mvnw spring-boot:run
```

```bash
cd reservation-service
./mvnw spring-boot:run
```

```bash
cd api-gateway
./mvnw spring-boot:run
```

## Ports par défaut

Les ports sont externalisés via le Config Server dans le dépôt Git référencé par `config-server`.

- API Gateway : `8080`
- Room Service : `8081`
- Member Service : `8082`
- Reservation Service : `8083`
- Eureka : `8761`
- Config Server : `8888`

## Swagger

- Room Service : [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- Member Service : [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
- Reservation Service : [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)

## Scénarios métier couverts

- création de salles et de membres
- réservation après validation du membre et de la salle
- refus d'une réservation si le créneau chevauche une réservation déjà confirmée
- annulation et complétion d'une réservation
- complétion automatique des réservations expirées
- suspension automatique d'un membre quand son quota de réservations actives est atteint
- désuspension automatique quand une réservation est libérée

## Tests API

Un script Python `test_complete_api.py`, généré avec l'aide d'une IA, permet de couvrir de manière automatisée l'ensemble des tests fonctionnels exposés par les services via l'API Gateway.

Ce script vérifie notamment les routes CRUD, les règles métier de réservation, les quotas de membres, ainsi que les propagations Kafka visibles depuis l'API.

Pour l'exécuter depuis le dossier parent de `TP` :

```bash
python3 test_complete_api.py
```

## Design Pattern

Le fichier `DESIGN_PATTERN.md` justifie l'utilisation du State Pattern dans `reservation-service` pour gérer le cycle de vie d'une réservation.
