package ncei.catalog.service

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class RepoServiceSpec extends Specification {

  def 'find id test'() {
    setup:
    Map object = [
            tracking_id: 'wrong id',
            granule_id : UUID.fromString('95f5bea0-31c8-11e7-a2e3-bb6760fe9882')
    ]

    when:
    UUID id = RepoService.findId(object)

    then:
    assert id == object.granule_id


  }

}
