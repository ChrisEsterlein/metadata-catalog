Metadata Catalog
===

[![Build Status](https://travis-ci.org/cires-ncei/metadata-catalog.svg?branch=master)](https://travis-ci.org/cires-ncei/metadata-catalog)

The intent of this project is to build a scalable system for storing canonical records of granule- and collection-level metadata to centralize metadata management and enable various ETL workflows.

## Artifacts

The travis build with automatically create snapshots of the images and full system docker-compose file (on Docker hub and JFrog Artifactory respectively).

To build and promote a set of snapshot artifacts, run ```bash promote.sh <version> <next version>```. This will make several commits: one to update the versions in the docker-compose and gradle.properties, one to tag the commit with the version (which triggers the build), and another to update the versions in the files again to reduce the likelihood of accidentally leaving versions the same and stepping on them later.

Any tag that begins with 'v' (case sensitive) is treated as a version and triggers the promotion part of the build. Manual promotion of artifacts can be accomplished by creating such a tag. 

## Deployment

The storage, index, and api modules are each running in their own container using spring's default port.  Only the api module has that port exposed externally.

NOTE: Make sure to do a clean if you make any changes to docker or gradle files.

* `./gradlew clean buildDeployment`
* `docker-compose -f build/docker-compose.yml -p metadata up -d`

### Verification of Containers Running
#### Storage Container
* `curl localhost:8081/collections`
#### Index Container
* `curl localhost:8082/search`
#### API Container
* `curl localhost:8083/index/search`
* `curl -XPOST -H "Content-Type: application/json" -d '{"collection_name": "collectionFace", "collection_schema": "a collection schema", "type":"fos", "collection_metadata": "{blah:blah}", "geometry": "point()"}' localhost:8083/storage1/collections`
* `curl localhost:8083/storage1/collections`

## Legal

This software was developed by research faculty members of the
University of Colorado under the Agile software development project number 1554688,
NOAA award number NA12OAR4320127 to CIRES. This code is licensed under GPL version 2.

© 2017 The Regents of the University of Colorado.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation version 2
of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
