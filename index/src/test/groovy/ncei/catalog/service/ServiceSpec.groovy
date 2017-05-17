package ncei.catalog.service

import groovy.json.JsonOutput
import org.apache.http.HttpEntity
import org.apache.http.StatusLine
import org.elasticsearch.client.Response
import org.elasticsearch.client.RestClient
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class ServiceSpec extends Specification {

  RestClient mockRestClient = Mock(RestClient)
  IndexAdminService mockIndexAdminService = Mock(IndexAdminService)
  Service service = Spy(Service, constructorArgs: [mockRestClient, mockIndexAdminService])

  def 'Insert: returns JSON API formatted information when created is #created'() {
    setup:
    def metadata = [id: 'abc', type: 'granule', attributes: [name: 'test']]
    def elasticsearchResponse = [
        "_index"  : "search_index",
        "_type"   : metadata.type,
        "_id"     : metadata.id,
        "_version": 1,
        "result"  : created ? 'created' : 'updated',
        "_shards" : [
            "total"     : 2,
            "successful": 1,
            "failed"    : 0
        ],
        "created" : created
    ]
    def contentStream = new ByteArrayInputStream(JsonOutput.toJson(elasticsearchResponse).bytes)
    def mockEntity = Mock(HttpEntity)
    mockEntity.getContent() >> contentStream
    def mockStatusLine = Mock(StatusLine)
    mockStatusLine.getStatusCode() >> (created ? 201 : 200)
    def mockResponse = Mock(Response)
    mockResponse.getEntity() >> mockEntity
    mockResponse.getStatusLine() >> mockStatusLine

    when:
    def result = service.insert(metadata)

    then:
    1 * mockRestClient.performRequest(*_) >> mockResponse

    and:
    result.data.id == metadata.id
    result.data.type == metadata.type
    result.data.attributes == metadata.attributes
    result.data.meta.created == created

    where:
    created << [true, false]
  }

  def 'Insert: messages with [#missingCombination] missing are ignored'() {
    when:
    def result = service.insert(metadata)

    then:
    result == null
    0 * mockRestClient.performRequest(*_)

    where:
    missingCombination     | metadata
    'id, type, attributes' | [:]
    'id, attributes'       | [type: 'granule']
    'id, type'             | [attributes: [name: 'test']]
    'type, attributes'     | [id: 'abc']
    'id'                   | [type: 'granule', attributes: [name: 'test']]
    'type'                 | [id: 'abc', attributes: [name: 'test']]
    'attributes'           | [id: 'abc', type: 'granule']
  }

  def 'Insert: messages with invalid values for [#invalidCombination] are ignored'() {
    when:
    def result = service.insert(metadata)

    then:
    result == null
    0 * mockRestClient.performRequest(*_)

    where:
    invalidCombination | metadata
    'type'             | [type: 12]
    'id, type'         | [attributes: [name: 'test']]
    'type, attributes' | [id: 'abc']
    'id'               | [type: 'granule', attributes: [name: 'test']]
    'type'             | [id: 'abc', attributes: [name: 'test']]
    'attributes'       | [id: 'abc', type: 'granule']
  }
}
