<p align="center"><a href="https://spring.io/projects/spring-boot" target="_blank"><img src="https://raw.githubusercontent.com/github/explore/80688e429a7d4ef2fca1e82350fe8e3517d3494d/topics/spring-boot/spring-boot.png" width="100"></a></p>

# Delivery Service

This project was generated with Spring Framework

## Development server

Run `mvn spring-boot:run` for a dev server. Navigate to `http://localhost:3001/`. The application will automatically reload if you change any of the source files.

## Environment Serve
```bash
# Development
$ mvn spring-boot:run

# Production
$ mvn clean install
```

## Build

From your local pc run
```bash
# make sure you are in correct working directory
[user@localpc]$ pwd
/home/user/path-to-your-project/delivery-service
...
..
[user@localpc]$ docker run -it --rm --platform linux/amd64 -v ./:/home/docker/Software -w /home/docker/Software openjdk:8-alpine sh
```

in docker shell
```bash
[user@docker]$ ./mvnw clean install -DskipTests
```

After the process done you should see folder `target` will have the following files & folder.

```bash
.
└── target/
    ├── classes
    ├── generated-sources
    ├── libs/
    ├── maven-archiver
    ├── maven-status
    └── delivery-service-<version>.jar
```

Exit the docker shell 

```bash
[user@docker]$ exit
...
..
...
[user@localpc]$
```

## Build Docker Image

Please make sure you are building the correct app version. There are 2 files `pom.xml` (java version) & `.env` (docker version) that need to be change (for the version).

> Important: both `pom.xml` & `.env` need to be the same version

To build the docker image, simply run `docker compose --profile prod build --push`, to build and push the docker image to docker registry.

```bash
[user@localpc]$ docker compose --profile prod build --push
```

> Note: If you having problem pushing docker image to docker registry. See [add docker registry](http://localhost)

## Container Architecture
This docker image is based on [openjdk:8-alpine](https://hub.docker.com/_/openjdk) image (see Dockerfile). For more configuration, see https://hub.docker.com/_/openjdk

## Deployment
This project encourange developer to use docker to run the application for production.

Simply run :

```bash
docker compose up -d
```


## Further help
To get more help on the Java CLI use `java --help` or go check out the Java Official page.
