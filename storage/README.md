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
The storage module sends a http response and rabbit message for every CRUD action. 
Their bodies follow the JsonApi specification at jsonapi.org 

example:
{
    data: [
            {
            type: <resource type>, 
            attributes: {
                <the record saved>
                }
            }
        ], 
    meta: {
        action: <CRUD action>
    }
}
 