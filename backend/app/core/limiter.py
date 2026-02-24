"""slowapi rate limiter singleton.

Import ``limiter`` wherever you need to apply the ``@limiter.limit(...)``
decorator, or attach the exception handler to the FastAPI app.

Usage
-----
In ``main.py``::

    from slowapi import _rate_limit_exceeded_handler
    from slowapi.errors import RateLimitExceeded
    from app.core.limiter import limiter

    app.state.limiter = limiter
    app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

On a specific route::

    @router.post("/login")
    @limiter.limit("20/minute")
    async def login(request: Request, ...):
        ...
"""

from slowapi import Limiter
from slowapi.util import get_remote_address

# Key function defaults to the client's IP address.
# For authenticated routes, swap out ``get_remote_address`` for a custom
# function that returns ``str(current_user.id)`` to rate-limit per user.
limiter = Limiter(key_func=get_remote_address, default_limits=["100/minute"])
