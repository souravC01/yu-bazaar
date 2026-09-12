FROM python:3.12-alpine

WORKDIR /app

COPY render_redirect.py .

USER nobody

EXPOSE 10000

CMD ["python", "render_redirect.py"]
