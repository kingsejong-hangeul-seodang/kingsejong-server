# syntax=docker/dockerfile:1
# 교정 엔진 이미지. 엔진 레포(Python-correction-engine)는 건드리지 않고, compose 가 엔진 소스를
# "engine" 이라는 이름의 빌드 재료(additional_contexts)로 넘겨준다.
# 정답 획 폰트·사전(data/)은 git에 없어서 이미지에 넣지 않고 deploy/engine-data 를 붙인다.

FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    TZ=Asia/Seoul
WORKDIR /app
COPY --from=engine requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt
COPY --from=engine app app
RUN useradd --system app && mkdir -p data/fonts data/extracted
USER app
EXPOSE 8000
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=4)"
CMD ["sh", "-c", "exec uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers ${ENGINE_WORKERS:-2}"]
