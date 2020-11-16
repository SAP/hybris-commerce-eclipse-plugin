#!/usr/bin/env bash

# pulling sonarqube
docker-compose up -d

cd ..

mvn clean verify sonar:sonar -Dsonar.login=admin -Dsonar.password=bitnami


