# Task Manager

A lightweight web app to create and track Tasks, Epics, and SubTasks. Data is stored in PostgreSQL.

# Stack

Frontend: HTML, CSS, JavaScript (served by Python’s http.server in Docker)

Backend: Java 21, Maven, custom REST API (no Spring Boot)

Database: PostgreSQL (Docker) + pgAdmin (Docker)


# Ports

Frontend: http://localhost:5500
Backend API: http://localhost:8080
PostgreSQL: localhost:5433 (mapped to container’s 5432)
pgAdmin: http://localhost:5050

1) Start the Database & Frontend (Docker)

stop and clean old stack (safe to run)
`docker compose down -v`
start postgres, pgadmin, and the Python static server for /front
`docker compose up -d`

Open Frontend: http://localhost:5500

pgAdmin available at: http://localhost:5050 for checking, altough DB schema is auto-applied from db/init.sql on first start.

2) Run the main class http.HttpTaskServer from your IDE.
The server must listen on port 8080.

3) Project Structure

project-root/
  docker-compose.yml     # containers (Postgres, pgAdmin, frontend)
  db/                    # database schema
    init.sql
  front/                 # static frontend
    index.html           # UI layout
    styles.css           # styling
    app.js               # JS logic
  src/main/java/
    http/                # HTTP server
        HttpTaskServer.java  # backend entry point
        BaseHttpHandler.java 
        CorsFilter.java     
    manager/             # task managers (InMemory, File, Postgres)
        TaskManager.java         
        InMemoryTaskManager.java 
        FileBackedTaskManager.java 
        PostgresTaskManager.java  
        Managers.java            
        Db.java                  
    model/               # domain models (Task, Epic, SubTask, Status)
        Task.java             
        Epic.java             
        SubTask.java          
        Status.java 
  README.md              # setup & usage
  pom.xml                # Maven build config

