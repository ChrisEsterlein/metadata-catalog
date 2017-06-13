# API
This is the API for all the Storage and Index.

## Developer setup
Run
-  ```./gradlew api:bootrun```

OR
-  ```api:build api:dockerComposeUp``` 

to start this module.

Note: if you are running integration tests and make a change to the docker template, be sure to rerun build or assemble, otherwise changes to the docker-compose.yml will not take effect.

## Legacy Endpoints Supported
###POST to metadata-catalog/granules
For saving granules as metadata-recorder does.
###GET to metadata-catalog/search
For searching as catalog-ETL does.

## Responses
All responses should follow the JSON API standard, look online for documentation.
