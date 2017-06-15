package ncei.catalog.service

import groovy.json.JsonOutput
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.RequestLine
import org.apache.http.StatusLine
import org.elasticsearch.client.Response
import org.elasticsearch.client.ResponseException
import org.elasticsearch.client.RestClient
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class ServiceSpec extends Specification {

  RestClient mockRestClient = Mock(RestClient)
  IndexAdminService mockIndexAdminService = Mock(IndexAdminService)
  Service service

  def setup() {
    mockIndexAdminService.createIndex('search_index') >> true
    service = new Service(mockRestClient, mockIndexAdminService)
    service.restClient = mockRestClient
    service.indexAdminService = mockIndexAdminService
  }

  private buildMockResponse(Map payload, int statusCode, String method = 'GET') {
    def mockEntity = Mock(HttpEntity)
    mockEntity.getContent() >> new ByteArrayInputStream(JsonOutput.toJson(payload).bytes)
    mockEntity.isRepeatable() >> true

    def mockRequestLine = Mock(RequestLine)
    mockRequestLine.getMethod() >> method
    mockRequestLine.getUri() >> 'testuri'

    def mockStatusLine = Mock(StatusLine)
    mockStatusLine.getStatusCode() >> statusCode

    def mockResponse = Mock(Response)
    mockResponse.getEntity() >> mockEntity
    mockResponse.getStatusLine() >> mockStatusLine
    mockResponse.getRequestLine() >> mockRequestLine
    mockResponse.getHost() >> new HttpHost('testhost', 1234)

    return mockResponse
  }

  def 'failure to create index throws exception'(){
    when:
    IndexAdminService mockAdminService = Mock(IndexAdminService)
    mockAdminService.createIndex('search_index') >> false
    new Service(mockRestClient, mockAdminService)

    then:
    thrown Exception
  }

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

    when:
    def result = service.upsert(metadata)

    then:
    1 * mockRestClient.performRequest(*_) >> buildMockResponse(elasticsearchResponse, created ? 201 : 200)

    and:
    result.id == metadata.id
    result.type == metadata.type
    result.attributes == metadata.attributes
    result.meta.created == created

    where:
    created << [true, false]
  }

  def 'Insert: messages with [#missingCombination] missing are ignored'() {
    when:
    def result = service.upsert(metadata)

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
    def result = service.upsert(metadata)

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

  def 'Delete: returns JSON API formatted information for existing resource'() {
    setup:
    def metadata = [id: 'abc', type: 'granule']
    def elasticsearchResponse = [
        "found"   : true,
        "_index"  : "search_index",
        "_type"   : metadata.type,
        "_id"     : metadata.id,
        "_version": 1,
        "result"  : 'deleted',
        "_shards" : [
            "total"     : 2,
            "successful": 1,
            "failed"    : 0
        ]
    ]

    when:
    def result = service.delete(metadata)

    then:
    1 * mockRestClient.performRequest(*_) >> buildMockResponse(elasticsearchResponse, 200)

    and:
    result.id == metadata.id
    result.type == metadata.type
    result.meta.deleted == true
  }

  def 'Delete: returns JSON API formatted information for nonexistent resource'() {
    setup:
    def metadata = [id: 'abc', type: 'granule']
    def elasticsearchResponse = [
        "found"   : false,
        "_index"  : "search_index",
        "_type"   : metadata.type,
        "_id"     : metadata.id,
        "_version": 1,
        "result"  : 'not_found',
        "_shards" : [
            "total"     : 2,
            "successful": 1,
            "failed"    : 0
        ]
    ]

    when:
    def result = service.delete(metadata)

    then:
    1 * mockRestClient.performRequest(*_) >> {
      throw new ResponseException(buildMockResponse(elasticsearchResponse, 404))
    }

    and:
    notThrown(Exception)
    result.id == metadata.id
    result.type == metadata.type
    result.meta.deleted == false
  }

  def 'Search: returns JSON API formatted information with query "#query" and has results="#hasResults"'() {
    setup:
    def searchHit = [
        "_index" : "search_index",
        "_type"  : "granule",
        "_id"    : "42",
        "_score" : 0.6931472,
        "_source": [
            "name": "test"
        ]
    ]
    def elasticsearchResponse = [
        "took"     : 2,
        "timed_out": false,
        "_shards"  : [
            "total"     : 5,
            "successful": 5,
            "failed"    : 0
        ],
        "hits"     : [
            "total"    : hasResults ? 1 : 0,
            "max_score": hasResults ? searchHit._score : 0.0,
            "hits"     : hasResults ? [searchHit] : []
        ]
    ]

    when:
    def result = service.search(query)

    then:
    1 * mockRestClient.performRequest(*_) >> buildMockResponse(elasticsearchResponse, 200)

    and:
    result == [data: (hasResults ? [[id: searchHit._id, type: searchHit._type, attributes: searchHit._source]] : []),
               meta: [totalResults: (hasResults ? 1 : 0), searchTerms: expSearchTerms, code: 200]]

    where:
    query       | hasResults | expSearchTerms
    'something' | true       | [q:"something"]
    'something' | false      | [q:"something"]
    null        | true       | [:]
    null        | false      | [:]
  }

  @Unroll
  def 'Search query #query offset #offset max #max properly built'() {

    expect:
    Service.buildSearchParams(query, offset, max) == expReducedSearchQuery

    where:
    query                            | offset | max  | expReducedSearchQuery
    null                             | null   | null | [:]
    "dataset:junk AND type:metadata" | null   | null | [q: "dataset:junk AND type:metadata"]
    null                             | "0"    | null | [from: "0"]
    null                             | null   | "5"  | [size: "5"]
    "dataset:junk AND type:metadata" | "1"    | "1"  | [q: "dataset:junk AND type:metadata", from: "1", size: "1"]
  }
}
