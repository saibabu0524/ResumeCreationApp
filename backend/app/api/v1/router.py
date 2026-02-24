"""v1 API router aggregator.

Register every route module here.  The router is then mounted in ``main.py``
under ``/api/v1``.
"""

from fastapi import APIRouter

from app.api.v1.routes import auth, resume, uploads, users

v1_router = APIRouter()

v1_router.include_router(auth.router)
v1_router.include_router(users.router)
v1_router.include_router(uploads.router)
v1_router.include_router(resume.router)
