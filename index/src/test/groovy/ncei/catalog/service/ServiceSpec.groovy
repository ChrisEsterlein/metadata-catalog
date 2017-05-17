package ncei.catalog.service

import groovy.json.JsonOutput
import org.apache.http.StatusLine
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.RestClient
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

@Unroll
class ServiceSpec extends Specification {

  RestClient mockRestClient = Mock(RestClient)
  IndexAdminService mockIndexAdminService = Mock(IndexAdminService)
  Service service = Spy(Service, constructorArgs: [mockRestClient, mockIndexAdminService])
//Spy(new Service(mockRestClient, mockIndexAdminService))

  @Ignore
  def 'JSON API response is expected JSON API format'() {
    setup:
    Map metadata = [id: 'abc', type: 'granule', attributes: [name: 'test']]
    IndexResponse response = new IndexResponse()//Mock(Response.class)
    StatusLine statusLine = Mock()
    String elasticsearchResponseStr = JsonOutput.toJson([
        "_index"  : "search_index",
        "_type"   : "granule",
        "_id"     : "abc",
        "_version": 1,
        "result"  : "created",
        "_shards" : [
            "total"     : 2,
            "successful": 1,
            "failed"    : 0
        ],
        "created" : true
    ])

    when:
    def result = service.insert(metadata)

    then: 'mock client and the responses provide the needed mocks'
    1 * mockRestClient.performRequest(*_) >> response
    1 * response.getStatusLine() >> statusLine
    1 * statusLine.getStatusCode() >> HttpServletResponse.SC_OK

    and: 'spy shows parseResponse is called'
    1 * service.parseResponse(_)

    and: 'result is expected format'
    result.data.id == metadata.id
  }

  def 'messages with [#missingCombination] missing are consumed and ignored'() {
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

  def 'Insert: messages with [#missingCombination] as the wrong Object type are consumed and ignored'() {
    when:
    def result = service.insert(metadata)

    then:
    result == null
    0 * mockRestClient.performRequest(*_)

    where:
    missingCombination | metadata
    'type'             | [type: 12]
    'id, type'         | [attributes: [name: 'test']]
    'type, attributes' | [id: 'abc']
    'id'               | [type: 'granule', attributes: [name: 'test']]
    'type'             | [id: 'abc', attributes: [name: 'test']]
    'attributes'       | [id: 'abc', type: 'granule']
  }
}
