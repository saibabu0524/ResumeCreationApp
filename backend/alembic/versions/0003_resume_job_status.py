"""Add job status tracking columns to tailored_resumes table."""
from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision: str = "0003_resume_job_status"
down_revision: str = "0002_stored_resumes"
branch_labels: str | None = None
depends_on: str | None = None


def upgrade() -> None:
    # Use batch mode for SQLite compatibility (SQLite doesn't support ALTER COLUMN directly).
    with op.batch_alter_table("tailored_resumes") as batch_op:
        # Track background job lifecycle.
        batch_op.add_column(
            sa.Column("status", sa.String(), nullable=False, server_default="queued")
        )
        batch_op.add_column(
            sa.Column("error_message", sa.Text(), nullable=True)
        )
        # stored_filename is now nullable — set by the worker when the job completes.
        batch_op.alter_column(
            "stored_filename",
            existing_type=sa.String(),
            nullable=True,
        )


def downgrade() -> None:
    with op.batch_alter_table("tailored_resumes") as batch_op:
        batch_op.drop_column("error_message")
        batch_op.drop_column("status")
        batch_op.alter_column(
            "stored_filename",
            existing_type=sa.String(),
            nullable=False,
        )
