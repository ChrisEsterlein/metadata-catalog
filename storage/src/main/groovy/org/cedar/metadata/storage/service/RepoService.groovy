package org.cedar.metadata.storage.service

import groovy.util.logging.Slf4j
import org.cedar.metadata.storage.domain.MetadataRecord
import org.cedar.metadata.storage.util.ValidationUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.stereotype.Component

import javax.servlet.http.HttpServletResponse

@Slf4j
@Component
class RepoService {

  @Autowired
  MessageService messageService

  @Autowired
  ValidationUtil validationUtil

  final String INSERT = 'insert'
  final String DELETE = 'delete'
  final String UPDATE = 'update'
  final String READ = 'read'

  void recover(HttpServletResponse response, CassandraRepository repositoryObject) {
    Iterable results = repositoryObject.findAll()
    List sentIds = []
    results.each {
      log.debug("Resending to rabbit, record : $it")
      if (!(it.id in sentIds)) {
        sentIds.add(it.id)
        String action = it.deleted ? DELETE : UPDATE
        messageService.notifyIndex([data: [createDataItem(it, action)]])
      }
    }
    response.status = HttpServletResponse.SC_OK
  }

  Map save(HttpServletResponse response, CassandraRepository repositoryObject, MetadataRecord metadataRecord) {
    log.info("Attempting to save ${metadataRecord.class} with id: ${metadataRecord.id}")
    log.debug("Metadata record ${metadataRecord.id}- ${metadataRecord.asMap()}")
    Map saveDetails = [:]
    UUID metadataId = metadataRecord.id
    //Make sure record does not already exist
    Iterable result = repositoryObject.findByMetadataId(metadataId)
    if (result) {
      log.info("Conflicting record: ${metadataId} already exists")
      log.debug("Existing record: ${result.first()}")
      response.status = HttpServletResponse.SC_CONFLICT
      saveDetails.errors = ['Record already exists']
      return saveDetails
    } else { //create a new one
      //save the row
      log.debug("Validating new record: ${metadataRecord}")

      validationUtil.validate(metadataRecord)

      log.info("Saving new record: ${metadataRecord.id}")
      MetadataRecord saveResult = repositoryObject.save(metadataRecord)
      log.debug("Response from cassandra for record with id ${metadataRecord.id}: $saveResult")
      saveDetails.data = []
      saveDetails.data.add(createDataItem(metadataRecord, INSERT))
      response.status = HttpServletResponse.SC_CREATED
      messageService.notifyIndex(saveDetails)
    }
    saveDetails
  }

  Map update(HttpServletResponse response, CassandraRepository repositoryObject, MetadataRecord metadataRecord, Date previousUpdate = null) {
    log.info("Attempting to update ${metadataRecord.class} with id: ${metadataRecord.id}")
    Map updateDetails = [:]
    UUID metadataId = metadataRecord.id
    //get existing row
    def existingRecord = repositoryObject.findByMetadataIdLimitOne(metadataId)?.first()
    if (existingRecord) {
      if (optimisticLockIsBlocking(existingRecord, previousUpdate)) {
        log.info("Failing update for out-of-date version: $metadataId")
        updateDetails.errors = ['You are not editing the most recent version.']
        response.status = HttpServletResponse.SC_CONFLICT
        return updateDetails
      } else {
        log.info("Updating record with id: $metadataId")
        log.debug("Updated record: ${metadataRecord}")
        metadataRecord.last_update = new Date()
        MetadataRecord record = repositoryObject.save(metadataRecord)
        updateDetails.data = [createDataItem(record, UPDATE)]
        response.status = HttpServletResponse.SC_OK
        messageService.notifyIndex(updateDetails)
      }
    } else {
      log.debug("No record found for id: $metadataId")
      updateDetails.errors = ['Record does not exist.']
      response.status = HttpServletResponse.SC_NOT_FOUND
      return updateDetails
    }
    updateDetails
  }

  Map list(HttpServletResponse response, CassandraRepository repositoryObject, Map params = null) {
    log.info("Fulfilling list request with params: $params")
    Map listDetails = [:]
    Boolean showVersions = params?.showVersions
    Boolean showDeleted = params?.showDeleted
    String paramId = params?.id
    UUID metadataId = paramId ? UUID.fromString(paramId) : null

    //the iterable returned from the repository class
    Iterable<MetadataRecord> allResults

    //find by id if specified, else select all
    if (metadataId) {
      if (showVersions) {
        log.info("Querying for all results with id: $metadataId")
        allResults = repositoryObject.findByMetadataId(metadataId)
      } else {
        log.info("Querying for latest result with id: $metadataId")
        allResults = repositoryObject.findByMetadataIdLimitOne(metadataId)
      }
    } else {
      log.info("Querying for all records")
      allResults = repositoryObject.findAll()
    }

    listDetails.data = []
    allResults.groupBy { it.id }.findResults {
      // filter out deleted results
      return showDeleted || !it.value[0].deleted ? it : null
    }.collectMany {
      // show all versions or only the first
      return showVersions ? it.value : [it.value[0]]
    }.each { mR ->
      listDetails.data.add(createDataItem(mR, READ))
    }

    //build response
    if (listDetails.data) {
      response.status = HttpServletResponse.SC_OK
    } else if (paramId) {
      listDetails.remove('data')
      listDetails.errors = ['No records exist with id: ' + paramId]
      response.status = HttpServletResponse.SC_NOT_FOUND
    }
    listDetails
  }

  Map purge(HttpServletResponse response, CassandraRepository repositoryObject, Map params) {
    UUID metadataId = UUID.fromString(params.id)
    log.warn("Purging all records with id: $metadataId")
    Map purgeDetails = [:]
    Iterable<MetadataRecord> items

    if (metadataId) {
      items = repositoryObject.findByMetadataId(metadataId)
    } else {
      log.warn("Failing purge request")
      purgeDetails.errors = ['An ID parameter is required to purge']
      response.status = HttpServletResponse.SC_BAD_REQUEST
      return purgeDetails
    }

    if (items) {
      purgeDetails.data = []
      items.each {
        purgeDetails.data.add(createDataItem(it, DELETE))
        UUID id = it.id
        delete(repositoryObject, id, it.last_update)
      }
      response.status = HttpServletResponse.SC_OK
    } else {
      purgeDetails.errors = ['No records exist with id: ' + metadataId]
      response.status = HttpServletResponse.SC_NOT_FOUND
    }
    purgeDetails
  }

  private def delete(CassandraRepository repositoryObject, UUID id, Date timestamp = null) {
    log.info("Deleting record with id: $id, timestamp: $timestamp")

    if (!timestamp) {
      log.debug("Looking for latest record to delete for id: $id")
      Iterable mR = repositoryObject.findByMetadataId(id as UUID)
      if (mR) {
        timestamp = mR.first().last_update as Date
      } else {
        log.warn("Unable to find timestamp to delete id: $id")
      }
    }

    repositoryObject.deleteByMetadataIdAndLastUpdate(id, timestamp)
  }

  Map softDelete(HttpServletResponse response, CassandraRepository repositoryObject, UUID id, Date timestamp) {
    log.info("Attempting soft delete record with id: $id, timestamp: $timestamp")
    Map deleteDetails = [:]
    def recordToDelete = repositoryObject.findByMetadataIdLimitOne(id)?.first()
    if (recordToDelete) {
      if (optimisticLockIsBlocking(recordToDelete, timestamp)) {
        log.info("Failing soft delete on out-of-date record with id: $id, timestamp: $timestamp")
        deleteDetails.errors = ['You are not deleting the most recent version.']
        response.status = HttpServletResponse.SC_CONFLICT
      } else {
        recordToDelete.last_update = new Date()
        recordToDelete.deleted = true
        log.info("Soft delete successful for record with id: $id")
        MetadataRecord newRecord = repositoryObject.save(recordToDelete)
        deleteDetails.data = [createDataItem(newRecord, DELETE)]
        response.status = HttpServletResponse.SC_OK
        messageService.notifyIndex(deleteDetails)
      }
      return deleteDetails
    } else {
      log.warn("Failing soft delete for non-existant record with id: $id")
      deleteDetails.errors = ['No record found with id: ' + id]
      response.status = HttpServletResponse.SC_NOT_FOUND
      return deleteDetails
    }
  }

  private static Map createDataItem(MetadataRecord metadataRecord, String action) {
    [id: metadataRecord.id, type: metadataRecord.recordTable(), attributes: metadataRecord, meta: [action: action]]
  }

  private static boolean optimisticLockIsBlocking(def record, Date previousUpdate) {
    return record && previousUpdate && record.last_update != previousUpdate
  }

}
