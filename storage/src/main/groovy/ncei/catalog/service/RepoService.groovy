package ncei.catalog.service

import ncei.catalog.domain.CollectionMetadata
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.domain.MetadataRecord
import ncei.catalog.domain.MetadataSchema
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.stereotype.Component

import javax.servlet.http.HttpServletResponse

@Component
class RepoService {

  @Autowired
  MessageService messageService
  
   Map save(CassandraRepository repositoryObject, MetadataRecord metadataRecord) {
    Map saveDetails = [:]

    UUID metadataId = findId(metadataRecord.asMap())
    //Make sure record does not already exist
    Iterable result = repositoryObject.findByMetadataId(metadataId)
    if (result) {
      saveDetails.code = HttpServletResponse.SC_CONFLICT
      saveDetails.message = "Record already exists"
      return saveDetails
    } else { //create a new one
      //save the row
      def saveResult = repositoryObject.save(metadataRecord)
      saveDetails."${getTableFromClass(metadataRecord)}" = saveResult
      saveDetails.recordsCreated = 1
      saveDetails.code = HttpServletResponse.SC_CREATED
      messageService.notifyIndex(metadataRecord.asMap())
    }

    saveDetails
  }

   Map update(CassandraRepository repositoryObject, MetadataRecord metadataRecord) {
    Map updateDetails = [:]
    UUID metadataId = findId(metadataRecord.asMap())
    //get existing row
    Iterable result = repositoryObject.findByMetadataId(metadataId)
    if (result) {
      //optimistic lock
      if (result.first().last_update != metadataRecord.last_update) {
        updateDetails.message = 'You are not editing the most recent version.'
        updateDetails.code = HttpServletResponse.SC_CONFLICT
        return updateDetails
      } else {
        metadataRecord.last_update = new Date()
        updateDetails.totalResultsUpdated = 1
        updateDetails.code = HttpServletResponse.SC_OK
        messageService.notifyIndex(metadataRecord.asMap())
      }
    } else {
      updateDetails.code = HttpServletResponse.SC_NOT_FOUND
      return updateDetails
    }

    //save the row
    String resource = getTableFromClass(metadataRecord)
    updateDetails."${resource}" = repositoryObject.save(metadataRecord)

    updateDetails
  }

   List list(CassandraRepository repositoryObject, Map params = null) {

    Boolean showVersions = params?.showVersions
    Boolean showDeleted = params?.showDeleted
    UUID metadataId = params ? findId(params) : null

    //the iterable returned from the repository class
    Iterable allResults

    if (metadataId) {
      if (showVersions) {
        allResults = repositoryObject.findByMetadataId(metadataId)
      } else {
        allResults = repositoryObject.findByMetadataIdLimitOne(metadataId)
      }
    } else { //find all
      allResults = repositoryObject.findAll()
    }

    if (showDeleted && showVersions) {
      return allResults.asList()
    } else if (showVersions) {
      List deletedList = []
      return allResults.findAll { mR -> //mR for metadataRecord
        UUID id = findId(mR.asMap())
        if (mR.deleted) {
          deletedList.add(id)
        }
        (!mR.deleted && !(id in deletedList))
      }
    } else {
      return getMostRecent(allResults, showDeleted)
    }
  }

   List getMostRecent(Iterable allResults, Boolean showDeleted) {
    Map idMostRecentMap = [:]
    List mostRecent
    allResults.each { mR ->
      if (!mR.deleted || mR.deleted == showDeleted) {
        String metadataId = findId(mR.asMap())
        if (idMostRecentMap[metadataId]) {
          if (idMostRecentMap[metadataId].last_update < mR.last_update) {
            idMostRecentMap[metadataId] = mR
          }
        } else {
          idMostRecentMap[metadataId] = mR
        }
      }
    }
    mostRecent = idMostRecentMap.collect { key, value ->
      value
    }
    mostRecent
  }

   Map purge(CassandraRepository repositoryObject, Map params) {
    UUID metadataId = findId(params)

    Map purgeDetails = [:]
    purgeDetails.searchTerms = params
    int count = 0
    Iterable<MetadataRecord> items

    if (metadataId) {
      items = repositoryObject.findByMetadataId(metadataId)
    } else {
      purgeDetails.message = 'An ID parameter is required to purge'
      purgeDetails.totalResultsDeleted = count
      purgeDetails.code = HttpServletResponse.SC_BAD_REQUEST
      return purgeDetails
    }

    items.each {
      UUID id = findId(it.asMap())
      delete(repositoryObject, id, it.last_update)
      messageService.notifyIndex(it.asMap())
      count++
    }

    purgeDetails.totalResultsDeleted = count
    purgeDetails.code = HttpServletResponse.SC_OK
    purgeDetails
  }

   def delete(CassandraRepository repositoryObject, UUID id, Date timestamp = null) {
    if (timestamp) {
      repositoryObject.deleteByMetadataIdAndLastUpdate(id, timestamp)
    } else {
      Iterable mR = repositoryObject.findByMetadataId(id as UUID)
      if (mR) {
        timestamp = mR.first().last_update as Date
      } else {
        throw RuntimeException("No such id")
      }
      repositoryObject.deleteByMetadataIdAndLastUpdate(id, timestamp)
    }
  }

   Map softDelete(CassandraRepository repositoryObject, UUID id, Date timestamp) {
    Iterable rowToBeDeleted = repositoryObject.findByIdAndLastUpdate(id, timestamp)
    if (rowToBeDeleted) {
      def record = rowToBeDeleted.first()
      if (record.last_update != timestamp) {
        return [success: false, message: 'You are not editing the most recent version.']
      }
      record.last_update = new Date()
      record.deleted = true
      MetadataRecord newRecord = repositoryObject.save(record)
      messageService.notifyIndex(newRecord.asMap())
      [deleted: newRecord, success: true, message: 'Successfully deleted row with id: ' + id]
    } else {
      [success: false]
    }
  }

   def getTableFromClass(def metadataRecord) {
    switch (metadataRecord.class) {
      case CollectionMetadata:
        return 'collection'
        break
      case GranuleMetadata:
        return 'granule'
        break
      case MetadataSchema:
        return 'schema'
        break
    }
  }

   UUID findId(Map metadataRecord) {
    String id = metadataRecord.find { (it.key =~ /_id/ && it.key != 'tracking_id') }?.value
    if (!id) {
      return null
    }
    return UUID.fromString(id)
  }

}
