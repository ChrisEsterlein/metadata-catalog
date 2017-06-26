package org.cedar.metadata.storage.service

import org.cedar.metadata.storage.domain.CollectionMetadata
import org.cedar.metadata.storage.domain.MetadataRecord
import org.cedar.metadata.storage.domain.MetadataSchema
import org.cedar.metadata.storage.domain.MetadataSchemaRepository
import org.cedar.metadata.storage.util.ValidationUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.data.cassandra.repository.CassandraRepository
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
  MetadataSchema metadataSchema
  Iterable<MetadataRecord> resultList

  def collectionMetadataMap = [
      "name"     : "collectionFace",
      "type"     : "fos",
      "geometry" : "point()",
      "metadata_schema" : '10686c20-27cc-11e7-9fdf-ef7bfecc6188'
  ]

  def metadataSchemaMap = [
      "metadata_schema": "schemaFace"
  ]

  def setup(){
    repository = Mock(MetadataSchemaRepository)
    validationUtil = new ValidationUtil(metadataSchemaRepository:  repository)
    collectionMetadataMap.metadata = new ClassPathResource("bathymetryMetadata.json").getFile().text
    metadataSchemaMap.json_schema = new ClassPathResource("testSchema.json").getFile().text
    collectionMetadata = new CollectionMetadata(collectionMetadataMap)
    metadataSchema = new MetadataSchema(metadataSchemaMap)
  }

  def 'schema is valid'(){
    when:
    Boolean isValid = validationUtil.validate(collectionMetadata)

    then:
    1 * repository.findByMetadataId(_ as UUID) >> [metadataSchema]

    assert isValid
  }

}
