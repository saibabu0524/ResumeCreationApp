# backend-api-patterns.md — FastAPI API Layer Patterns

> **Full details:** `backend/README.md` §Core Components, §Authentication, §Rate Limiting

---

## Route Handler Rules

```python
# ✅ CORRECT: thin handler — validate, delegate to crud/service, wrap response
@router.post("/resource", response_model=ApiResponse[ResourcePublic], status_code=201)
@limiter.limit("20/minute")
async def create_resource(
    request: Request,     # Required when using @limiter.limit
    payload: CreateRequest,
    db: DbSession,
    current_user: CurrentUser,
) -> ApiResponse[ResourcePublic]:
    item = await crud.create_item(db, payload, owner_id=current_user.id)
    return ApiResponse(data=ResourcePublic.model_validate(item), message="Created.")

# ❌ WRONG: business logic, raw DB queries, and ORM objects leaked in handler
@router.post("/resource")
async def create_resource(payload: dict, db: AsyncSession = Depends(get_db)):
    # Raw query in handler — move to crud/
    result = await db.execute(select(Resource).where(...))
    item = result.scalars().first()
    item.name = payload["name"]  # Logic in handler — move to crud/update_*
    return item  # Leaking ORM object — use a response schema
```

---

## Response Envelope

Every endpoint returns `ApiResponse[T]` from `schemas/common.py`:

```python
{
  "data": { ... },       # The actual payload — typed via Generic[DataT]
  "message": "...",      # Human-readable status string
  "success": true
}
```

Use `MessageResponse` for simple acknowledgements (logout, delete) that return no data.

---

## Authentication Dependency Hierarchy

```
get_current_user         → validates JWT, fetches User from DB
  └── get_current_active_user  → additionally checks is_active == True
        └── get_current_superuser  → additionally checks is_superuser == True
```

Use the pre-typed aliases in route signatures:

```python
from app.api.deps import CurrentUser, SuperUser, DbSession

async def my_route(current_user: CurrentUser, db: DbSession) -> ...:
    ...
```

---

## Rate Limiting

- `@limiter.limit("20/minute")` on every auth and mutation endpoint.
- The `request: Request` parameter **must** be the first parameter when using the decorator.
- Global default (100/minute) applies automatically without the decorator.
- For per-user limits on authenticated routes, swap the key function in `core/limiter.py`.

---

## Error Response Rules

| Situation | HTTP Code |
|---|---|
| Duplicate unique field (email) | 409 Conflict |
| Invalid credentials | 401 Unauthorized (generic message — prevent enumeration) |
| Inactive account | 400 Bad Request |
| Missing/invalid JWT | 401 Unauthorized |
| Insufficient permissions | 403 Forbidden |
| Resource not found | 404 Not Found |
| Wrong MIME type on upload | 415 Unsupported Media Type |
| File too large | 413 Request Entity Too Large |
| Rate limit exceeded | 429 Too Many Requests |

---

## Schema Rules

- Request schemas: `class XxxRequest(BaseModel)` — used for input validation.
- Response schemas: `class XxxPublic(BaseModel)` — safe subset of the table model, `model_config = {"from_attributes": True}`.
- Table schemas: `class Xxx(SQLModel, table=True)` — ORM entity; never return this directly in responses.
- Shared envelopes: `ApiResponse[T]`, `PaginatedResponse[T]`, `MessageResponse` in `schemas/common.py`.
