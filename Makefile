.PHONY: start stop restart build logs db-reset

# App starten
start:
	docker compose up -d
	cd frontend && ng serve

# App im Hintergrund starten
start-detached:
	docker compose up -d

# App stoppen
stop:
	docker compose down

# Backend neu bauen und starten
restart: stop
	docker compose build backend
	docker compose up

# Nur Backend neu bauen
build:
	docker compose build backend

# Logs anzeigen
logs:
	docker compose logs -f backend

# Datenbank zurücksetzen (Vorsicht: löscht alle Daten!)
db-reset:
	docker compose down -v
	docker compose up