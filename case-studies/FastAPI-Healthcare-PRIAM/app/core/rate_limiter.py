
from typing import Callable
import redis
from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.status import HTTP_429_TOO_MANY_REQUESTS

from app.core.config import settings


class RateLimiter(BaseHTTPMiddleware):
    def __init__(
        self,
        app,
        redis_url: str = settings.REDIS_URL,
        rate_limit_per_minute: int = 60
    ):
        super().__init__(app)
        self.redis = redis.from_url(redis_url)
        self.rate_limit = rate_limit_per_minute
        self.window = 60

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        if request.url.path in ["/health", "/docs", "/redoc", "/openapi.json"]:
            return await call_next(request)

        client_ip = request.client.host

        key = f"rate_limit:{client_ip}:{request.url.path}"

        current = self.redis.get(key)
        current = int(current) if current else 0

        if current >= self.rate_limit:
            return Response(
                content="Rate limit exceeded. Please try again later.",
                status_code=HTTP_429_TOO_MANY_REQUESTS,
                headers={"Retry-After": str(self.window)}
            )

        pipe = self.redis.pipeline()
        pipe.incr(key)
        pipe.expire(key, self.window)
        pipe.execute()

        return await call_next(request)
