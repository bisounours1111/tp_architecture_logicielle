## Lancement

1. Installer docker si ce n'est pas déjà fait
2. À la racine du projet pour lancer kafka lancez :
   ```bash
   docker compose up --build
   ```
3. Les services seront accessibles aux ports suivants :
   - Eureka : http://localhost:8761
   - Member Service : http://localhost:8081
   - Room Service : http://localhost:8082
   - Reservation Service : http://localhost:8083

## Documentation API (Swagger)

- Member Service : http://localhost:8081/swagger-ui.html
- Room Service : http://localhost:8082/swagger-ui.html
- Reservation Service : http://localhost:8083/swagger-ui.html
