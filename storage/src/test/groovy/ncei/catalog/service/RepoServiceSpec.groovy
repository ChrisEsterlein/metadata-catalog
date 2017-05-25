package ncei.catalog.service

import groovy.util.logging.Slf4j
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.domain.GranuleMetadataRepository
import ncei.catalog.domain.MetadataRecord
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

@Slf4j
@Unroll
class RepoServiceSpec extends Specification {

  RepoService repoService
  MessageService messageService
  GranuleMetadataRepository granuleMetadataRepository
  HttpServletResponse response
  Date now = new Date()

  final def granuleMetadataMap = [
      "id"             : UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6188"),
      "tracking_id"    : "test-id-1",
      "filename"       : "test.txt",
      "dataset"        : "test-dataset-1",
      "type"           : "file",
      "size_bytes"     : 1024,
      "metadata"       : "{blah: blah}",
      "metadata_schema": "schema",
      "geometry"       : "point(1.1, 1.1)",
      "collections"    : ["FOS"]
  ]

  GranuleMetadata granuleMetadata

  def setup() {
    messageService = Mock()
    repoService = new RepoService(messageService: messageService)

    granuleMetadataRepository = Mock()
    response = Mock()

    granuleMetadata = new GranuleMetadata(granuleMetadataMap)
  }

  def 'save new'() {
    setup: 'findByMetadataId returns something false'

    when: 'calling service save'
    Map result = repoService.save(response, granuleMetadataRepository, granuleMetadata)

    then: 'findByMetadataId returns nothing'
    1 * granuleMetadataRepository.findByMetadataId(_) >> []

    and: 'the metadata is saved to the repository'
    1 * granuleMetadataRepository.save(granuleMetadata) >> granuleMetadata

    and: 'an insert notification is sent'
    1 * messageService.notifyIndex({
      it.data[0].meta.action == 'insert'
    })

    and: 'the status is good'
    1 * response.setStatus(HttpServletResponse.SC_CREATED)
    result.data[0].meta.action == 'insert'
    result.errors == null
  }

  def 'save conflict'() {
    setup: 'findByMetadataId returns an object'

    when: 'calling service save'
    Map result = repoService.save(response, granuleMetadataRepository, granuleMetadata)

    then: 'findByMetadataId returns a list with a granule'
    1 * granuleMetadataRepository.findByMetadataId(_) >> [granuleMetadata]

    and: 'nothing is saved'
    0 * granuleMetadataRepository.save(_)

    and: 'there is no notification'
    0 * messageService.notifyIndex(_)

    and: 'the conflict is returned'
    1 * response.setStatus(HttpServletResponse.SC_CONFLICT)
    result.data == null
    !result.errors.isEmpty()
  }

  def 'list by id, show versions (soft deleted)'() {
    setup: 'multiple versions of the granule returned'
    UUID uuid = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6123")
    when: 'list granules for id with showVersions'
    Map result = repoService.list(response, granuleMetadataRepository, [id: uuid.toString(), showVersions: true])
    then: 'findByMetadataId returns all the rows'
    1 * granuleMetadataRepository.findByMetadataId(_) >> [
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now,
            "deleted"    : true
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(1)
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(2)
        ]),
    ]
    and: 'granule not found'
    result.errors != null
    result.errors == ['No results found.']
//    1 * response.setStatus(HttpServletResponse.SC_NOT_FOUND)
  }

  def 'list by id, show versions'() {
    setup: 'multiple versions of the granule returned'
    UUID uuid = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6123")
    when: 'list granules for id with showVersions'
    Map result = repoService.list(response, granuleMetadataRepository, [id: uuid.toString(), showVersions: true])
    then: 'findByMetadataId returns all the rows'
    1 * granuleMetadataRepository.findByMetadataId(_) >> [
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(1)
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(2)
        ]),
    ]
    and: 'all results for granule returned'
    result.data[0].meta.action == 'read'
    1 * response.setStatus(HttpServletResponse.SC_OK)
  }

  def 'list by id, show versions (soft delete reverted)'() {
    setup: 'multiple versions of the granule returned'
    UUID uuid = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6123")
    when: 'list granules for id with showVersions'
    Map result = repoService.list(response, granuleMetadataRepository, [id: uuid.toString(), showVersions: true])
    then: 'findByMetadataId returns all the rows'
    1 * granuleMetadataRepository.findByMetadataId(_) >> [
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now,
            "metadata"   : "{fourth: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(1),
            "metadata"   : "{third: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(2),
            "deleted"    : true,
            "metadata"   : "{second: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(3),
            "metadata"   : "{first: true}"
        ]),
    ]
    and: 'entries since it was deleted for granule returned'
    result.data[0].meta.action == 'read'
    result.data.size_bytes == 4
    1 * response.setStatus(HttpServletResponse.SC_OK)
    result.data[0].attributes.metadata == "{fourth: true}"
    result.data[1].attributes.metadata == "{third: true}"
  }

  def 'list all, show versions (soft delete reverted)'() {
    setup: 'multiple versions of the granule2 returned'
    UUID uuid = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6123")
    UUID uuid2 = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6567")
    UUID uuid3 = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6987")
    when: 'list granules with showVersions'
    Map result = repoService.list(response, granuleMetadataRepository, [showVersions: true])
    then: 'findAll returns all the rows'
    1 * granuleMetadataRepository.findAll() >> [
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now,
            "metadata"   : "{fourth: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(1),
            "metadata"   : "{third: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(2),
            "deleted"    : true,
            "metadata"   : "{second: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(3),
            "metadata"   : "{first: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid2,
            "last_update": now.minus(2),
            "deleted"    : true,
            "metadata"   : "{second: true, deleted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid2,
            "last_update": now.minus(3),
            "metadata"   : "{first: true, deleted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid3,
            "last_update": now.minus(1),
            "metadata"   : "{first: true, not-deleted-at-all: true}"
        ]),
    ]
    and: 'entries since it was deleted for granule returned'
    result.data[0].meta.action == 'read'
    result.data.size_bytes == 5
    1 * response.setStatus(HttpServletResponse.SC_OK)
    and: 'entries since it was deleted for granule returned'
    result.data[0].attributes.id == uuid
    result.data[1].attributes.id == uuid
    result.data[2].attributes.id == uuid
    result.data[3].attributes.id == uuid
    result.data[4].attributes.id == uuid3
    result.data[0].attributes.metadata == "{fourth: true, reverted: true}"
    result.data[1].attributes.metadata == "{third: true, reverted: true}"
    result.data[4].attributes.metadata == "{first: true, not-deleted-at-all: true}"
  }

  def 'list all (soft delete reverted)'() {
    setup: 'multiple versions of the granule2 returned'
    UUID uuid = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6123")
    UUID uuid2 = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6567")
    UUID uuid3 = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6987")
    when: 'list granules'
    Map result = repoService.list(response, granuleMetadataRepository)
    then: 'findAll returns all the rows'
    1 * granuleMetadataRepository.findAll() >> [
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now,
            "metadata"   : "{fourth: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(1),
            "metadata"   : "{third: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(2),
            "deleted"    : true,
            "metadata"   : "{second: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(3),
            "metadata"   : "{first: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid2,
            "last_update": now.minus(2),
            "deleted"    : true,
            "metadata"   : "{second: true, deleted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid2,
            "last_update": now.minus(3),
            "metadata"   : "{first: true, deleted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid3,
            "last_update": now.minus(1),
            "metadata"   : "{first: true, not-deleted-at-all: true}"
        ]),
    ]
    and: 'entries since it was deleted for granule returned'
    result.data[0].meta.action == 'read'
    result.data.size_bytes == 2
    1 * response.setStatus(HttpServletResponse.SC_OK)
    and: 'entries with the latest not deleted are returned'
    result.data[0].attributes.id == uuid
    result.data[1].attributes.id == uuid3
    result.data[0].attributes.metadata == "{fourth: true, reverted: true}"
    result.data[1].attributes.metadata == "{first: true, not-deleted-at-all: true}"
  }

  def 'list all, show deleted (soft delete)'() {
    setup: 'multiple versions of the granule2 returned'
    UUID uuid = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6123")
    UUID uuid2 = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6567")
    when: 'list granules with showDeleted'
    Map result = repoService.list(response, granuleMetadataRepository, [showDeleted: true])
    then: 'findAll returns all the rows'
    1 * granuleMetadataRepository.findAll() >> [
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now,
            deleted      : true
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(1),
        ]),
        new GranuleMetadata([
            "id"         : uuid2,
            "last_update": now.minus(3),
        ]),
    ]
    and: 'no entries are excluded (limit 1 per id)'
    result.data[0].meta.action == 'read'
    result.data.size_bytes == 2
    1 * response.setStatus(HttpServletResponse.SC_OK)
    and: 'entries with the latest not deleted are returned'
    result.data[0].attributes.id == uuid
    result.data[1].attributes.id == uuid2
  }

  def 'list by id, show deleted (soft delete)'() {
    setup: 'multiple versions of the granule2 returned'
    UUID uuid = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6123")
    when: 'list granules by id with showDeleted'
    Map result = repoService.list(response, granuleMetadataRepository, [id: uuid.toString(), showDeleted: true])
    then: 'findByMetadataIdLimitOne returns a deleted row'
    1 * granuleMetadataRepository.findByMetadataIdLimitOne(_) >> [
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now,
            "deleted"    : true,
            "metadata"   : "{second: true}"
        ])
    ]
    and: 'one result is returned'
    result.data[0].meta.action == 'read'
    result.data.size_bytes == 1
    1 * response.setStatus(HttpServletResponse.SC_OK)
    and: 'entries with the latest not deleted are returned'
    result.data[0].attributes.id == uuid
    result.data[0].attributes.metadata == "{second: true}"
  }

  def 'list all, show versions and show deleted (soft delete reverted)'() {
    setup: 'multiple versions of the granule2 returned'
    UUID uuid = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6123")
    UUID uuid2 = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6567")
    UUID uuid3 = UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6987")
    when: 'list granules with showVersions and showDeleted'
    Map result = repoService.list(response, granuleMetadataRepository, [showDeleted: true, showVersions: true])
    then: 'findAll returns all the rows'
    1 * granuleMetadataRepository.findAll() >> [
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now,
            "metadata"   : "{fourth: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(1),
            "metadata"   : "{third: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(2),
            "deleted"    : true,
            "metadata"   : "{second: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid,
            "last_update": now.minus(3),
            "metadata"   : "{first: true, reverted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid2,
            "last_update": now.minus(2),
            "deleted"    : true,
            "metadata"   : "{second: true, deleted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid2,
            "last_update": now.minus(3),
            "metadata"   : "{first: true, deleted: true}"
        ]),
        new GranuleMetadata([
            "id"         : uuid3,
            "last_update": now.minus(1),
            "metadata"   : "{first: true, not-deleted-at-all: true}"
        ]),
    ]
    and: 'everything is returned'
    result.data[0].meta.action == 'read'
    result.data.size_bytes == 7
    1 * response.setStatus(HttpServletResponse.SC_OK)
  }

  def 'only latest version is sent during recover'() {
    setup: 'mock out results from cassandra'
    List results = []

    UUID uniqueId = UUID.randomUUID()
    results << new GranuleMetadata(["id": uniqueId])

    UUID sharedId = UUID.randomUUID()
    results << new GranuleMetadata(["id": sharedId])
    results << new GranuleMetadata(["id": sharedId])

    when:
    repoService.recover(response, granuleMetadataRepository)

    then:

    1 * granuleMetadataRepository.findAll() >> results

    1 * messageService.notifyIndex({ it.data[0].id == uniqueId })
    1 * messageService.notifyIndex({ it.data[0].id == sharedId })

  }

  def 'appropriate action is sent in message'() {
    setup:

    List<MetadataRecord> results = []

    results << new GranuleMetadata(["deleted": false])
    results << new GranuleMetadata(["deleted": true])

    when:
    repoService.recover(response, granuleMetadataRepository)

    then:
    1 * granuleMetadataRepository.findAll() >> results

    1 * messageService.notifyIndex({ it.data[0].meta.action == 'update' })
    1 * messageService.notifyIndex({ it.data[0].meta.action == 'delete' })

  }
}