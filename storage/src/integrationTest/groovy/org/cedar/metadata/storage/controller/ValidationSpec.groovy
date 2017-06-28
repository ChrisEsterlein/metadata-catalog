package org.cedar.metadata.storage.controller

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.cedar.metadata.storage.domain.CollectionMetadata
import org.cedar.metadata.storage.domain.CollectionMetadataRepository
import org.cedar.metadata.storage.domain.MetadataSchema
import org.cedar.metadata.storage.domain.MetadataSchemaRepository
import org.cedar.metadata.storage.util.ValidationUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import spock.lang.Specification

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.equalTo


class ValidationSpec extends Specification {
  @Autowired
  CollectionMetadataRepository collectionMetadataRepository

  @Autowired
  MetadataSchemaRepository metadataSchemaRepository

  MetadataSchema bathyMetadataSchema
  MetadataSchema regionMetadataSchema
  MetadataSchema linkMetadataSchema
  MetadataSchema linkEntryMetadataSchema
  MetadataSchema fileReferenceMetadataSchema
  CollectionMetadata collectionMetadata

  def collectionMetadataMap = [
      "name"     : "collectionFace",
      "type"     : "bathyProduct",
      "geometry" : "point()",
      "metadata_schema" : 'BathymetricProduct'
  ]

  def metadataSchemaMap = [:]

  def setup(){
    collectionMetadataRepository.deleteAll()
    metadataSchemaRepository.deleteAll()

    collectionMetadataMap.metadata = new ClassPathResource("bathymetryMetadata.json").getFile().text
    collectionMetadata = new CollectionMetadata(collectionMetadataMap)
//    collectionMetadataRepository.save(collectionMetadata)

    metadataSchemaMap.name = 'BathymetricProduct'
    metadataSchemaMap.json_schema = new ClassPathResource("bathymetricProductSchema.json").getFile().text
    bathyMetadataSchema = new MetadataSchema(metadataSchemaMap)
    metadataSchemaRepository.save(bathyMetadataSchema)

    metadataSchemaMap.name = 'Region'
    metadataSchemaMap.json_schema = new ClassPathResource("regionSchema.json").getFile().text
    regionMetadataSchema = new MetadataSchema(metadataSchemaMap)
    metadataSchemaRepository.save(regionMetadataSchema)

    metadataSchemaMap.name = 'FileReference'
    metadataSchemaMap.json_schema = new ClassPathResource("fileReferenceSchema.json").getFile().text
    fileReferenceMetadataSchema = new MetadataSchema(metadataSchemaMap)
    metadataSchemaRepository.save(fileReferenceMetadataSchema)

    metadataSchemaMap.name = 'Link'
    metadataSchemaMap.json_schema = new ClassPathResource("linkSchema.json").getFile().text
    linkMetadataSchema = new MetadataSchema(metadataSchemaMap)
    metadataSchemaRepository.save(linkMetadataSchema)

    metadataSchemaMap.name = 'LinkEntry'
    metadataSchemaMap.json_schema = new ClassPathResource("linkEntrySchema.json").getFile().text
    linkEntryMetadataSchema = new MetadataSchema(metadataSchemaMap)
    metadataSchemaRepository.save(linkEntryMetadataSchema)

  }

  def 'submit a collection referencing existing schema'(){

    expect: 'we post, the metadata is validated, record is created'
    Map collectionMetadata = RestAssured.given()
        .body(collectionMetadataMap)
        .contentType(ContentType.JSON)
        .when()
        .post('/collections')
        .then()
        .assertThat()
        .statusCode(201)  //should be a 201
        .body('data[0].attributes.name', equalTo(collectionMetadataMap.name))
        .body('data[0].attributes.metadata_schema', equalTo(collectionMetadataMap.metadata_schema))
        .body('data[0].attributes.metadata', equalTo(collectionMetadataMap.metadata))
        .body('data[0].attributes.geometry', equalTo(collectionMetadataMap.geometry))
        .body('data[0].attributes.type', equalTo(collectionMetadataMap.type))
        .extract()
        .path('data[0].attributes')

  }


}
