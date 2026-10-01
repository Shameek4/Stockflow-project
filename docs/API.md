# API reference

JSON endpoints live under `/api`. Browser authentication uses a session cookie.
GET `/api/csrf` is public and returns `token`, `headerName`, and `parameterName`.
For login, POST form-encoded `username`, `password`, and that CSRF parameter to `/login`.
After successful login (204), fetch a fresh CSRF token, since authentication rotates it.
Send the session cookie on each request and the returned CSRF header on every mutation.
POST `/logout` also requires that header. Browser UI handles this automatically.

| Method | Path | Permission | Purpose |
| --- | --- | --- | --- |
| GET | /api/csrf | Public | CSRF token |
| GET | /api/me | Signed in | Current username and roles |
| GET | /api/products?q=mouse | Signed in | Products including archived records |
| POST | /api/products | Admin | Create product |
| PUT | /api/products/{id} | Admin | Edit product metadata |
| POST | /api/products/{id}/stock | Admin | Change stock |
| POST | /api/products/{id}/archive | Admin | Archive product |
| GET | /api/orders | Signed in | Orders, newest first |
| POST | /api/orders | Signed in | Create order |
| POST | /api/orders/{id}/cancel | Signed in | Cancel and restore stock |
| GET | /api/dashboard | Signed in | Summary metrics |
| GET | /api/movements | Signed in | Latest 100 quantity movements |

Product create/edit:

```json
{"sku":"KB-002","name":"Keyboard","category":"Accessories","price":1499.00,"stock":20,"lowStockThreshold":5}
```

On edit, stock must match the current database stock. Use stock adjustment to change quantity.
SKUs are normalized to uppercase and must contain only letters, digits, underscores or hyphens.

Stock adjustment:

```json
{"delta":10,"reason":"Supplier delivery"}
```

Order creation:

```json
{"customer":"Shameek","items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}
```

No duplicate product IDs are allowed in a single order. Unit prices are taken from the database,
not the client. Limits: 50 lines/order, 10,000 units/line, 1,000,000 stock units/product.
Order totals are limited to 999,999,999,999.99. Cancellation is idempotent. Archived products remain available for historical records and
stock restoration but cannot be ordered or adjusted.

Responses: 201 for new products/orders, 200 for reads/updates, 400 for invalid JSON or fields,
401 when signed out, 403 for missing CSRF or insufficient permission, 404 for missing records,
409 for business conflicts. Most application errors return `{ "message": "...", "status": 409 }`;
Spring Security's CSRF/authorization errors may use the framework's default error response.
