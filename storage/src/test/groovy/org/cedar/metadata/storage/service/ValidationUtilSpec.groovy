package org.cedar.metadata.storage.service

import org.cedar.metadata.storage.util.ValidationUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import spock.lang.Specification

class ValidationUtilSpec extends Specification{

  @Value(value = 'classpath:testSchema.json')
  File jsonSchemaFile

  @Autowired
  ValidationUtil validationUtil

  Map jsonSchema

  def setup(){
    jsonSchema = jsonSchemaFile.text
  }

  def 'schema is valid'(){
    when:
    validationUtil

  }

}
