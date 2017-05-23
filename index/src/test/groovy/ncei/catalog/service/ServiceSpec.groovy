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
  Service service = Spy(Service, constructorArgs: [mockRestClient, mockIndexAdminService])

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

  def 'Search: returns JSON API formatted information with query [#query] and matching results [#hits]'() {
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
            "total"    : hits ? 1 : 0,
            "max_score": hits ? searchHit._score : 0.0,
            "hits"     : hits ? [searchHit] : []
        ]
    ]

    when:
    def result = service.search(query)

    then:
    1 * mockRestClient.performRequest(*_) >> buildMockResponse(elasticsearchResponse, 200)

    and:
    result == [data: (hits ? [[id: searchHit._id, type: searchHit._type, attributes: searchHit._source]] : []),
        meta: [totalResults: (hits ? 1 : 0), code: 200, searchTerms: query]]

    where:
    query           | hits
    [q:'something'] | true
    [q:'something'] | false
    [:]             | true
    [:]             | false
  }

  @Unroll
  def 'Search query #searchParams properly built'() {

    expect:
    Service.buildSearchParams(searchParams) == expReducedSearchQuery

    where:
    searchParams                          | expReducedSearchQuery
    [:]                                   | [:]
    [q: "dataset:junk AND type:metadata"] | [q: "dataset:junk AND type:metadata"]
    [from: 0]                             | [from: 0]
    [size: 5]                             | [size: 5]
    [q: "dataset:junk AND type:metadata", from: 1, size: 1] | [q: "dataset:junk AND type:metadata", from: 1, size: 1]
    [q: "dataset:junk AND type:metadata", from: 1, size: 1, j: "junk"] | [q: "dataset:junk AND type:metadata", from: 1, size: 1]
  }
}
