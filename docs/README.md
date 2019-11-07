#  Documentation 

## Continuous Integration 

folder contains docker configuration for sonarqube installation.
To execute sonar check it is required to run docker installation first

> docker-compose -d

 or by running shell script in this directory

> ./start-sonar.sh

After running that website will be available on default sonar port [localhost:9000](http://localhost:9000)

## Running Quality Check

To check plugin by sonar, run maven task:

> mvn clean verify sonar:sonar

Results will be available on your local website

## Feature List

* [Commerce Bean & Enums Generation Wizard](beangen/beangen.md)