# Changes for data persistence

This document explains a set of changes made to `case-studies/OnlineBoutique`
**before** the PRIAM integration itself, in order to give the application a
genuine, durable data store and a real account system. It exists because,
in its upstream form, OnlineBoutique has no database and no accounts: the
only durable state is a Redis-backed shopping cart keyed by an anonymous,
48h session cookie, and every other piece of personal data collected at
checkout (email, address, credit card) is discarded the moment the HTTP
response is sent. That makes the application a weak evaluation target for
a GDPR rights-management platform like PRIAM — there would be little to
nothing for an access/rectification/erasure request to act on beyond the
cart. These changes are a deliberate, scoped addition to the benchmark
application, not part of upstream OnlineBoutique, made specifically to
create a realistic, durable data model prior to wiring PRIAM into it.

**Build status**: this was written in an environment without a Go
toolchain or a running Docker daemon, so **the code below has not been
compiled or run in this session**. See "What still needs to be done"
before relying on it.

## What was added

### 1. A real account system

- `POST /accounts/signup`, `GET /accounts/signup` — create an account
  (email + password, bcrypt-hashed, `golang.org/x/crypto/bcrypt`).
- `POST /accounts/login`, `GET /accounts/login` — authenticate and open a
  session.
- `GET /accounts/orders` — "My Orders": the durable order history for the
  logged-in account.
- A new cookie, `shop_user-id`, set on successful signup/login, distinct
  from the existing anonymous `shop_session-id` cookie that every visitor
  already gets (guest browsing/cart is untouched — the account layer is
  additive, not a replacement). `logoutHandler` already expired *every*
  cookie on the request before this change, so it clears `shop_user-id`
  automatically, with no code change needed there.

### 2. Durable storage of accounts and orders

A new SQLite database (`modernc.org/sqlite`, a pure-Go driver — chosen
specifically because the existing `Dockerfile` builds with
`CGO_ENABLED=0` against a `distroless/static` base image, which has no C
library for a cgo-based driver like `mattn/go-sqlite3` to link against),
opened once at startup (`initStore()` in `store.go`), with three tables:

```sql
users        (id, email UNIQUE, password_hash, created_at)
orders       (order_id, user_id, email, street_address, city, state,
              zip_code, country, currency_code, placed_at)
order_items  (order_id, product_id, quantity, cost_units, cost_nanos)
```

- **Credit card data is still never persisted anywhere** — this was a
  deliberate scope boundary, not an oversight: storing raw card numbers
  would raise PCI-DSS obligations on top of GDPR ones, and nothing in this
  benchmark's purpose requires it. Only the data that a real order
  history legitimately needs (shipping details, ordered items, price) is
  stored.
- `placeOrderHandler` (`handlers.go`) now calls `saveOrder(...)` right
  after a successful call to `checkoutservice`, using the exact same data
  it already had in hand (the `OrderResult` it renders on the confirmation
  page). **`checkoutservice` itself was not touched** — the persistence
  happens at the frontend, which is both where the full order (address +
  priced items) is already assembled, and the layer the PRIAM Provider
  bridge will read from as an ordinary SQL table.
- Orders are saved for **every** completed checkout, logged in or not —
  guest orders are stored with `user_id = NULL`. This keeps the existing
  guest-checkout flow completely unchanged (no login is required to buy
  something, exactly as before) while still making persistence
  unconditional and real, not something that only exists when convenient
  for the demo.
- The write is **best-effort**: if it fails, the error is logged
  (`log.WithField("error", err).Error(...)`) but the checkout response the
  user already received is not retroactively failed — consistent with how
  this codebase already treats other non-critical side effects (e.g.
  recommendation lookups).
- `db.SetMaxOpenConns(1)`: SQLite only supports one writer at a time; a
  single shared connection sidesteps `database is locked` errors under
  concurrent requests instead of tuning busy-timeouts. This only
  serializes the new account/order code path — cart, catalog, currency,
  etc. are untouched and keep talking to their own services as before.
- `ACCOUNT_DB_PATH` (env var, default `/src/onlineboutique.db` — `/src` is
  the existing `WORKDIR` in `Dockerfile`, guaranteed to exist in the built
  image, unlike an arbitrary new directory) controls where the SQLite file
  lives, so a real deployment can point it at a mounted volume.

## Files touched

| File | Status | What changed |
|---|---|---|
| `src/frontend/store.go` | new | SQLite schema + all account/order data-access functions |
| `src/frontend/accounts_handlers.go` | new | signup/login/orders HTTP handlers |
| `src/frontend/templates/signup.html` | new | signup form |
| `src/frontend/templates/login.html` | new | login form |
| `src/frontend/templates/orders.html` | new | order history page |
| `src/frontend/templates/header.html` | modified | Log In / Sign Up / My Orders / Log Out nav links |
| `src/frontend/main.go` | modified | `cookieUserID` constant, `initStore()` call at startup, 5 new routes |
| `src/frontend/handlers.go` | modified | `placeOrderHandler` persists the order; `injectCommonTemplateData` exposes `logged_in` |
| `src/frontend/validator/validator.go` | modified | `SignupPayload`, `LoginPayload` (reusing the existing `go-playground/validator` pattern already used for the checkout form) |

`src/frontend/checkoutservice`, `cartservice`, and every other upstream
microservice are **unchanged** — the new persistence lives entirely in
`frontend`, the one service that already terminates both the account
concept (session cookie) and the fully-assembled order.

## What still needs to be done

This was written without a working Go toolchain or a running Docker
daemon available in the session, so none of the following could be
verified yet — do not treat this as tested:

1. **Dependency resolution.** `modernc.org/sqlite` is a new module, not
   yet in `go.mod`/`go.sum`. Before building, run, from
   `src/frontend`:
   ```
   go get modernc.org/sqlite@latest
   go mod tidy
   ```
   `golang.org/x/crypto` (for `bcrypt`) is already a resolved dependency
   (currently listed `// indirect` in `go.mod` since nothing imported it
   directly before) — `go mod tidy` will simply promote it to a direct
   requirement, no version change needed.
2. **A real build.** `docker compose build frontend` (or `go build ./...`
   locally) has not been run against this code. Given this project's own
   integration playbook, expect the first attempt to surface at least
   minor issues (a typo, an import ordering nit) — check the real build
   output rather than assuming this compiles.
3. **No volume is wired yet.** OnlineBoutique has no `docker-compose.yml`
   upstream (it targets Kubernetes/GKE natively); whichever compose file
   ends up running `frontend` for the PRIAM integration should mount a
   volume at `ACCOUNT_DB_PATH` (or its default, `/src/onlineboutique.db`)
   so the SQLite file survives container recreation — otherwise
   persistence only holds for the container's lifetime, defeating the
   point of this change.
4. **No real-browser or curl test performed.** Signup, login, checkout,
   and the order-history page have not been exercised against a running
   instance.
5. **Cosmetic only:** the new header nav links reuse the existing
   `.cart-link` CSS class for simplicity; it renders correctly but was
   tuned for icon links, not text, so the header may look a little
   rough until it gets its own style rules.

## Deliberately out of scope here

- **No cart merge on login.** The cart stays keyed by `session_id`
  exactly as before; logging in does not currently reassign the guest
  cart to the account. Left out to keep this change bounded — it can be
  added later as a small, separate follow-up if needed.
- **No Keycloak/OIDC wiring.** This is local email/password auth only.
  Whether to hook `register_data_subject`/Keycloak provisioning into
  `signupHandler` is a PRIAM-integration-side decision, deliberately left
  for that next step rather than folded into this one.
