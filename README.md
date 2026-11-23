# POSHarmoni Dashboard

Server-side **Vaadin 24** administration dashboard for the Harmoni POS platform.
It is the operator-facing UI for the catalogue, organisation, promotions,
customers, tables, kitchen routing, reports and access control. It does not own
data: every screen reads and writes through the **API gateway**, which routes to
the auth, menu, customer and order services.

## Tech stack

| Concern | Choice |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.2.4 |
| UI | Vaadin 24.6.5 (Flow, server-side components, React/Vite frontend) |
| Charts | CSS bars and stacks (no commercial Vaadin components) |
| HTTP client | Spring WebFlux `WebClient` (blocking and async clients) |
| JSON | Jackson 2.18.8 (pinned) |
| Build | Maven (`./mvnw` or `mvn`), GraalVM native plugin |

## Architecture

The dashboard is a **server-side Vaadin application**. The browser holds a thin
client; the component tree, state and event handling live on the server. Views
are Spring-managed route components, and data access is done through reactive
REST clients that call the gateway.

```
com.harmoni.menu.dashboard
├── DashboardApplication        # @SpringBootApplication, @Theme, @Push
├── layout
│   ├── MainLayout              # app shell: header, drawer, route guard, toasts
│   ├── LoginView               # /login
│   ├── DashboardView           # / (KPIs, panels)
│   ├── AbstractListView        # shared grid/search/brand/pagination base
│   ├── navigation/SideNavMenu  # permission-aware navigation
│   ├── organization            # brand / chain / store / user / tier
│   ├── menu                    # category / customization / product / promotion
│   ├── customer                # customer list + detail
│   ├── setting                 # service / table / floor
│   ├── report                  # settlement / top products / daily / sales / order volume
│   ├── security                # RBAC: employees, roles, permissions, access denied
│   ├── component               # PageHeader, PaginationBar, StoreCascade, TabManager, ...
│   └── util                    # UiUtil, Css, Permissions, Messages helpers
├── service
│   ├── AccessService           # signed-in user, store, brand, chain
│   ├── AuthorizationService    # permissions, role codes, scopes
│   ├── ScopeService            # brand/chain/store scope resolution
│   └── data.rest               # RestClient* (blocking) and AsyncRestClient* (async)
├── event                       # click listeners and persist/delete handlers
├── component                   # Broadcaster / BroadcastMessage
├── configuration               # @ConfigurationProperties for every URL group
├── dto                         # request/response DTOs
├── exception                   # business exceptions
└── util                        # Messages (i18n), ObjectUtil, VaadinSessionUtil, ImageUtil
```

### UI shell and routing

- `DashboardApplication` enables the `dashboard` Lumo theme and server push
  (`@Push(transport = LONG_POLLING)`), so async callbacks can update the UI.
- `MainLayout` is the `AppLayout` shell: drawer toggle, logo, palette switcher,
  language selector, light/dark toggle and user menu. It also:
  - guards every route in `beforeEnter` — no JWT in the session forwards to
    `/login`; a route annotated with `@RequiresPermission` forwards to
    `/access-denied` when the user lacks the permission;
  - listens to `Broadcaster` messages and shows error toasts (bad request,
    forbidden) or returns to login on unauthorized.
- `SideNavMenu` builds the grouped navigation and hides items the user cannot
  access (`AuthorizationService.hasPermission`).

### Routes

| Route | View | Purpose |
| --- | --- | --- |
| `/login` | `LoginView` | Sign in |
| `/` | `DashboardView` | Today's sales, transactions, payment split, top menu |
| `/organization` | `OrganizationLayout` | Brand → chain → store tree |
| `/tier` | `TierLayout` | Price/menu/service tiers |
| `/tier-menu`, `/tier-price`, `/tier-service` | tier sub-views | Tier composition |
| `/category` | `CategoryLayout` | Categories |
| `/customization` | `CustomizationLayout` | Customizations and options |
| `/product` | `ProductLayout` | Products, SKUs, images, customizations |
| `/promotion` | `PromotionLayout` | Promotions, rules, targets, scopes, schedules |
| `/customers` | `CustomerLayout` | Customer list and detail |
| `/loyalty` | `LoyaltyLayout` | Loyalty balance, tier, point history and redemption |
| `/campaigns` | `CampaignsLayout` | Campaigns, voucher issuing and redemption |
| `/customer-analytics` | `CustomerAnalyticsLayout` | Sales totals, customer metrics and insights |
| `/ai-sales-assistant` | `AiSalesAssistantLayout` | AI ordering assistant chat |
| `/recommendations` | `RecommendationsLayout` | AI recommendations |
| `/kitchen` | `KitchenLayout` | Kitchen stations, printers, routing |
| `/table` | `TableLayout` | Floors and tables |
| `/service` | `ServiceLayout` | Services and sub-services |
| `/report` | `ReportLayout` | Five order-service reports in tabs |
| `/employees` | `EmployeeRoleLayout` | Employees and roles |
| `/access-control` | `AccessControlLayout` | Roles and permissions (redirects to employees) |
| `/access-denied` | `AccessDeniedView` | Permission denied |

### Shared UI building blocks

- `AbstractListView` — the base for grid list views: UI locking via
  `UiUtil.safeAccess`, broadcast refresh registration with automatic unregister
  on detach, the standard search filter, the brand selector, the grid + skeleton
  slot and the pagination footer.
- `AbstractReportView` — the base for the five reports: builds the body once and
  reloads when the shared `ReportFilterBar` (brand/chain/store + date range)
  changes.
- `StoreCascade` — the brand → chain → store cascade used by forms.
- `TabManager` — opens/selects editor tabs (product, promotion, tier, ...).
- `UiUtil` — toasts, retry prompts, buttons, safe UI access.
- `Messages` — static accessor for `messages.properties` /
  `messages_id.properties`, using the session locale.

## Security and scope

### Authentication

`LoginView` delegates to `LoginEventListener`, which calls the auth service
through the gateway, then loads the user's authorities (`/api/v1/auth/me/authorities`)
and profile. The JWT, refresh token, user detail and authorities are stored in
the `VaadinSession` (`VaadinSessionUtil`).

### Authorization

- `AuthorizationService` exposes the session's permissions, role codes and
  scoped assignments.
- `@RequiresPermission` on a route is enforced by `MainLayout.beforeEnter`.
- `SideNavMenu` and individual actions (add/edit/delete buttons) are gated with
  `Permissions.*` constants.
- `RbacProperties.enabled` hides the access-control module until the backend is
  deployed.

### Organisation scope

`ScopeService` resolves the signed-in user's brand/chain/store scope from the
RBAC assignments (or the session store as a fallback) and exposes a
`ScopeSnapshot` used to filter brand/chain/store selectors and to decide whether
they can be changed. Brand owners can switch between their assigned brands;
chain- and store-scoped users are pinned to their brand.

## REST layer

Two client families share the same error handling:

- `RestClientService` — blocking `WebClient` calls used by forms and actions.
- `AsyncRestClientBase` — callback-based async calls used by list/report loads.

Both map HTTP statuses to typed exceptions:

| Status | Exception | UI behaviour |
| --- | --- | --- |
| `400` | `BusinessBadRequestException` | Bad-request toast |
| `401` | `TokenRefreshRequiredException` | Refresh the token and retry; log out if it still fails |
| `403` | `BusinessForbiddenException` | Permission toast (no retry) |
| `500` | `BusinessServerRequestException` | Error toast with retry |

`TokenRefreshService` transparently refreshes an expired access token using the
stored refresh token and retries the request. `Broadcaster`/`BroadcastMessage`
fan out refresh and error events to the open views; `MainLayout` turns the error
events into toasts.

## Configuration

All backend URLs are externalised in `application.properties` (and
`application.properties.example`). They all point at the gateway by default:

```properties
server.port=8282
server.servlet.session.timeout=1440m

menu.url=http://localhost:8080
setting.url=http://localhost:8080
auth.url=http://localhost:8080
customer.url=http://localhost:8080
report.url=http://localhost:8080

auth.url.login=${auth.url}/api/v1/auth/login
auth.url.refresh-token=${auth.url}/api/v1/auth/refresh-token

rbac.url=${menu.url}
rbac.enabled=true
```

The gateway routes each path to the owning service:

| Path | Service |
| --- | --- |
| `/api/v1/auth/**` | auth service |
| `/api/v1/customers/**` | customer service |
| `/api/v1/reports/**` | order service |
| everything else | menu service |

Each URL group has a `@ConfigurationProperties` class (`MenuProperties`,
`AuthProperties`, `CustomerProperties`, `ReportProperties`, `RbacProperties`,
`KitchenProperties`, `SettingProperties`, ...), so changing an endpoint is a
config edit, not a code change.

## UI theme and i18n

- The custom **Lumo** theme lives in `frontend/themes/dashboard/` (tokens,
  colour schemes, per-module CSS). Dark mode is the default; the header toggles
  light/dark.
- UI strings are in `messages.properties` (English) and
  `messages_id.properties` (Indonesian). The header language selector stores the
  choice per session.

## Running locally

```bash
# 1. Start the gateway and the backend services (auth, menu, customer, order)
# 2. Run the dashboard
./mvnw spring-boot:run
```

Open `http://localhost:8282/login`. For remote debugging:

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=6006"
```

Dev runs use the live Vite dev server, so frontend changes appear on browser
refresh. Build a production frontend bundle with the `production` profile:

```bash
./mvnw -Pproduction package
```

## Testing

```bash
./mvnw test
```

Tests cover the scope service, the REST clients, the promotion editor and
preview, the product form/SKU sections, the report date handling, the floor
dialog, the tab manager, the focus-mode toggle, message locales and the Spring
context.

## Notes

- **The dashboard owns no data.** Every screen is a view over the gateway APIs;
  the backend services remain the source of truth.
- **Server-side UI.** State lives in the Vaadin session; `@Push` keeps async
  callbacks in sync with the browser.
- **Permission-aware by default.** Navigation, routes and actions are gated by
  the same `Permissions` constants, and the backend enforces them again.
- **Scope-aware selectors.** Brand/chain/store selectors are filtered and
  locked according to the signed-in user's RBAC scope.
- **Token refresh is transparent.** A 401 triggers a refresh-and-retry; only a
  second failure logs the user out.
