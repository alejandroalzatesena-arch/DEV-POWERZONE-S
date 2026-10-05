"""PowerZone AI service.

Servicio independiente de FastAPI para recomendaciones y futuras integraciones
de IA (Sprint 11). En los sprints 0-2 solo expone salud y un stub de rutinas.
"""

import os
from datetime import datetime, timezone

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

API_KEY = os.getenv("AI_SERVICE_API_KEY", "internal-ai-key")

app = FastAPI(
    title="PowerZone AI Service",
    version="0.1.0",
    description="Microservicio de IA para recomendaciones de entrenamiento.",
)


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    timestamp: datetime


class RoutineSuggestionRequest(BaseModel):
    goal: str = Field(description="strength | hypertrophy | weight_loss | endurance")
    experience_level: str = Field(description="beginner | intermediate | advanced")
    days_per_week: int = Field(ge=2, le=7)


class RoutineSuggestionResponse(BaseModel):
    summary: str
    focus: list[str]
    source: str = "powerzone-ai"


def require_api_key(x_api_key: str | None = Header(default=None)) -> None:
    if x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Invalid API key")


@app.get("/health", response_model=HealthResponse, tags=["system"])
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        service="powerzone-ai",
        version="0.1.0",
        timestamp=datetime.now(timezone.utc),
    )


@app.post(
    "/v1/routines/suggest",
    response_model=RoutineSuggestionResponse,
    dependencies=[Depends(require_api_key)],
    tags=["routines"],
)
def suggest_routine(payload: RoutineSuggestionRequest) -> RoutineSuggestionResponse:
    focus_by_goal = {
        "strength": ["sentadilla", "peso muerto", "press banca"],
        "hypertrophy": ["pecho", "espalda", "pierna"],
        "weight_loss": ["circuito full body", "cardio"],
        "endurance": ["cardio", "resistencia muscular"],
    }
    focus = focus_by_goal.get(payload.goal, ["full body"])
    return RoutineSuggestionResponse(
        summary=(
            f"Plan de {payload.days_per_week} dias para nivel "
            f"{payload.experience_level} orientado a {payload.goal}."
        ),
        focus=focus,
    )
