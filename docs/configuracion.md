# Configuración

## Requisitos del entorno

| Herramienta | Versión mínima |
|-------------|----------------|
| JDK | 21 |
| Maven | 3.9 |
| MongoDB | 6.x |
| Python (MkDocs) | 3.10+ |

## Variables de entorno

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `SPRING_APPLICATION_NAME` | Nombre del servicio | `payment` |
| `SERVER_PORT` | Puerto HTTP del servicio | `8080` |
| `SPRING_DATA_MONGODB_URI` | URI de conexión a MongoDB | `mongodb://localhost:27017/payment` |

## Archivo `application.properties`

```properties
spring.application.name=payment
server.port=8080
spring.data.mongodb.uri=mongodb://localhost:27017/payment
```

!!! tip "Perfiles de Spring"
    Para entornos distintos se recomienda usar perfiles (`dev`, `staging`, `prod`) con archivos `application-{profile}.properties`.

## Ejecución local

### Compilar y ejecutar

```bash
mvn clean spring-boot:run
```

### Ejecutar pruebas

```bash
mvn test
```

### Generar JAR

```bash
mvn clean package
java -jar target/payment-0.0.1-SNAPSHOT.jar
```

## Documentación MkDocs

### Instalar dependencias

```bash
python -m pip install mkdocs mkdocs-material
```

### Servidor de desarrollo

```bash
python -m mkdocs serve
```

La documentación estará disponible en `http://127.0.0.1:8000`.

### Build de producción

```bash
python -m mkdocs build
```

El sitio estático se genera en la carpeta `site/`.

## Estructura de configuración del proyecto

```
proyecto/
├── mkdocs.yml
├── docs/
│   └── assets/
│       └── stylesheets/
│           └── extra.css
├── pom.xml
└── src/
    └── main/
        ├── java/
        └── resources/
            └── application.properties
```
