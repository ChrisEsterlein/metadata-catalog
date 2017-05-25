# STORAGE
This is the Cassandra API for storing metadata.

## Prerequisites:
1. Install docker

## Developer setup
Run
-  ```./gradlew storage:bootrun```

OR
-  ```storage:build storage:dockerComposeUp``` 

to start this module.

Note: if you are running integration tests and make a change to the docker template, be sure to rerun build or assemble, otherwise changes to the docker-compose.yml will not take effect.

##Responses
All responses should follow the JSON API standard, look online for documentation.
