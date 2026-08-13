# POSHarmoni Dashboard

Admin dashboard for maintaining a restaurant/menu management system. It is a
server-side Java application built with **Vaadin 24 (Flow)** and **Spring Boot 3.2**.

## Stack

- Java 21
- Spring Boot 3.2.4
- Vaadin 24.6.5 (Flow + React dev bundle via Vite)
- Spring WebFlux (reactive REST client)
- Project Lombok

## Running locally

```bash
./mvnw spring-boot:run
```

The app listens on `http://localhost:8282/login` (port set via `server.port`
in `application.properties`). For remote debugging, start with:

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=6006"
```

## Configuration

Copy the example and point the API base URLs at your backend:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `application.properties` to set `auth.url`, `menu.url`, and `setting.url`.

## UI theme

The app uses a custom **Lumo** theme (`frontend/themes/dashboard/`):
- Brand / dark mode is on by default (`@Theme(value = "dashboard", variant = Lumo.DARK)`).
- Light/dark can be toggled from the header.

## Layout

- `MainLayout` — app shell: sidebar toggle, branded logo, a light/dark toggle and a
  user menu dropdown (avatar + _Sign out_).
- `SideNavMenu` — collapsible navigation grouped into Admin, Menu and Settings.

## Modules

See the package structure under `src/main/java/com/harmoni/menu/dashboard/`:

- `dto` — shared DTOs.
- `configuration` — properties and Spring config.
- `service` — reactive REST clients (`RestClient*`, `AsyncRestClient*`).
- `event` — click listeners and `ConfirmDialog`-based delete handlers.
- `layout` — views: `organization` (brand/chain/store/user/tier), `menu`
  (category/customization/product) and `setting` (service).
- `util` — `AccessService`, `ObjectUtil`, `LoadingBar`, `UiUtil`.
