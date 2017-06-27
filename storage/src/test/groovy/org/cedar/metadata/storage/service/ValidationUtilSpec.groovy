package org.cedar.metadata.storage.service

import groovy.json.JsonSlurper
import org.cedar.metadata.storage.domain.CollectionMetadata
import org.cedar.metadata.storage.domain.MetadataRecord
import org.cedar.metadata.storage.domain.MetadataSchema
import org.cedar.metadata.storage.domain.MetadataSchemaRepository
import org.cedar.metadata.storage.util.ValidationUtil
import org.springframework.core.io.ClassPathResource
import spock.lang.Specification

class ValidationUtilSpec extends Specification{

//  @Value(value = 'classpath:testSchema.json')
//  Resource jsonSchemaFile
//
//  @Value(value = 'classpath:bathymetryMetadata.json')
//  Resource metadata

  ValidationUtil validationUtil

  MetadataSchemaRepository repository
  CollectionMetadata collectionMetadata
  MetadataSchema bathyMetadataSchema
  MetadataSchema regionMetadataSchema
  MetadataSchema linkMetadataSchema
  MetadataSchema linkEntryMetadataSchema
  MetadataSchema fileReferenceMetadataSchema

  def collectionMetadataMap = [
      "name"     : "collectionFace",
      "type"     : "fos",
      "geometry" : "point()",
      "metadata_schema" : '10686c20-27cc-11e7-9fdf-ef7bfecc6188'
  ]

  def metadataSchemaMap = [:]

  def setup(){
    repository = Mock(MetadataSchemaRepository)
    validationUtil = new ValidationUtil(metadataSchemaRepository:  repository)
    collectionMetadataMap.metadata = new ClassPathResource("bathymetryMetadata.json").getFile().text
    metadataSchemaMap.name = 'BathymetricProduct'
    metadataSchemaMap.json_schema = new ClassPathResource("bathymetricProductSchema.json").getFile().text
    collectionMetadata = new CollectionMetadata(collectionMetadataMap)
    bathyMetadataSchema = new MetadataSchema(metadataSchemaMap)

    metadataSchemaMap.name = 'Region'
    metadataSchemaMap.json_schema = new ClassPathResource("regionSchema.json").getFile().text
    regionMetadataSchema = new MetadataSchema(metadataSchemaMap)

    metadataSchemaMap.name = 'FileReference'
    metadataSchemaMap.json_schema = new ClassPathResource("fileReferenceSchema.json").getFile().text
    fileReferenceMetadataSchema = new MetadataSchema(metadataSchemaMap)

    metadataSchemaMap.name = 'Link'
    metadataSchemaMap.json_schema = new ClassPathResource("linkSchema.json").getFile().text
    linkMetadataSchema = new MetadataSchema(metadataSchemaMap)

    metadataSchemaMap.name = 'LinkEntry'
    metadataSchemaMap.json_schema = new ClassPathResource("linkEntrySchema.json").getFile().text
    linkEntryMetadataSchema = new MetadataSchema(metadataSchemaMap)
  }

  def 'schema is valid'(){
    when:
    Boolean isValid = validationUtil.validate(collectionMetadata)

    then:
    1 * repository.findByMetadataId(_ as UUID) >> [bathyMetadataSchema]
    1 * repository.findBySchemaName('Region') >> [regionMetadataSchema]
    1 * repository.findBySchemaName('FileReference') >> [fileReferenceMetadataSchema]
    1 * repository.findBySchemaName('Link') >> [linkMetadataSchema]
    1 * repository.findBySchemaName('LinkEntry') >> [linkEntryMetadataSchema]

    assert isValid
  }

}
