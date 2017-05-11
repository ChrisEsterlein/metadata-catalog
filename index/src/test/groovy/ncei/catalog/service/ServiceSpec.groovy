package ncei.catalog.service

import org.elasticsearch.client.RestClient
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class ServiceSpec extends Specification {

  RestClient mockRestClient = Mock(RestClient)
  IndexAdminService mockIndexAdminService = Mock(IndexAdminService)
  Service service = new Service(mockRestClient, mockIndexAdminService)


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

}
