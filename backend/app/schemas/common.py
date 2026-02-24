"""Shared response envelope and pagination schemas.

Every API response is wrapped in ``ApiResponse[T]`` to give clients a
consistent shape regardless of the endpoint.  Errors fall back to
FastAPI's default ``HTTPException`` format with an extra ``detail`` field.
"""

from __future__ import annotations

from typing import Generic, TypeVar

from pydantic import BaseModel

DataT = TypeVar("DataT")


class ApiResponse(BaseModel, Generic[DataT]):
    """Standard response envelope used by all endpoints.

    Example::

        {
            "data": { ... },
            "message": "User registered successfully.",
            "success": true
        }
    """

    data: DataT | None = None
    message: str = "OK"
    success: bool = True


class PaginatedResponse(BaseModel, Generic[DataT]):
    """Paginated list response.

    Example::

        {
            "items": [ ... ],
            "total": 100,
            "page": 1,
            "page_size": 20,
            "pages": 5
        }
    """

    items: list[DataT]
    total: int
    page: int
    page_size: int
    pages: int


class MessageResponse(BaseModel):
    """Simple acknowledgement response."""

    message: str
    success: bool = True
