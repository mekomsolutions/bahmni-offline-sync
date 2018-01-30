package org.bahmni.module.bahmniOfflineSync.strategy;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.codehaus.jackson.map.ObjectMapper;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.openmrs.Concept;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;

public class AddressEntriesSyncStrategy extends AbstractOfflineSyncStrategy {

	public static final String FILTER_UUID = "c34d4f2b-6fcf-4e33-8966-85fea5dee4db";
	public static final  String ADDRESS_ENTRIES_GP_NAME = "bahmniOfflineSync.addressEntriesSyncStrategy.addressEntries";

	private Log log = LogFactory.getLog(this.getClass());

	public AddressEntriesSyncStrategy() throws SQLException {
		super();

	}

	public Map<String, List<String>> getFilterForDevice(String providerUuid, String addressUuid, String loginLocationUuid) {
		Map<String, List<String>> categoryFilterMap = new HashMap<String, List<String>>();

		ArrayList<String> filters = new ArrayList<String>();
		filters.add(FILTER_UUID);

		categoryFilterMap.put("patient", filters);
		categoryFilterMap.put("encounter", filters);
		categoryFilterMap.put("addressHierarchy", filters);
		categoryFilterMap.put("offline-concepts", filters);
		categoryFilterMap.put("forms", filters);

		return categoryFilterMap;
	}

	@Override
	public List<String> getEventCategoriesList() {
		List<String> eventCategoryList = new ArrayList<String>();

		eventCategoryList.add("patient");
		eventCategoryList.add("encounter");
		eventCategoryList.add("addressHierarchy");
		eventCategoryList.add("offline-concepts");
		eventCategoryList.add("forms");

		return eventCategoryList;
	}

	@Override
	public List<EventLog> getEventLogsFromEventRecords(List<EventRecord> eventRecords) {
		List<EventLog> eventLogs = new ArrayList<EventLog>();

		for (EventRecord er : eventRecords) {
			EventLog eventLog = new EventLog(er.getUuid(),er.getCategory(),er.getTimeStamp(),er.getContents(), er.getUuid(), er.getUuid());
			String category = er.getCategory();
			String uuid = getUuidFromURL(er.getContents());
			String filter = null;

			if (!uuid.isEmpty()) {
				if (category.equalsIgnoreCase("all-concepts")) {
					if (isOfflineConceptEvent(uuid)) {
						eventLog.setCategory("offline-concepts");
						filter = evaluateFilterForConcept(uuid);
					} else {
						eventLog.setCategory("concepts");
					}
				}

				if (category.equalsIgnoreCase("Patient")|| category.equalsIgnoreCase("LabOrderResults"))
					filter = evaluateFilterForPatient(uuid);
				else if (category.equalsIgnoreCase("Encounter") || category.equalsIgnoreCase("SHREncounter"))
					filter = evaluateFilterForEncounter(uuid);
				else if (category.equalsIgnoreCase("addressHierarchy"))
					filter = evaluateFilterForAddressHierarchy(uuid);
			}
			eventLog.setFilter(filter);

			eventLogs.add(eventLog);
		}

		return eventLogs;
	}

	private boolean isOfflineConceptEvent(String eventUuid) {
		final Concept concept = conceptService.getConceptByUuid(eventUuid);
		final Concept offlineConcept = conceptService.getConceptByName("Offline Concepts");
		return offlineConcept != null && offlineConcept.getSetMembers().contains(concept);
	}

	protected String evaluateFilterForPatient(String patientUuid) {
		return FILTER_UUID;
	}

	private String evaluateFilterForEncounter(String encounterUuid) {
		return FILTER_UUID;
	}

	private String evaluateFilterForConcept(String conceptUuid) {
		return FILTER_UUID;
	}
	
	private String evaluateFilterForAddressHierarchy(String addressEntryUuid) {
		String eventFilter = null;

		AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
		AddressHierarchyEntry addressEntryFromEvent = addressHierarchyService.getAddressHierarchyEntryByUuid(addressEntryUuid);

		ObjectMapper mapper = new ObjectMapper();
		AdministrationService admniService = Context.getAdministrationService();
		String entriesToSyncAsJSON = admniService.getGlobalProperty(ADDRESS_ENTRIES_GP_NAME);
		if (entriesToSyncAsJSON == null) {
			log.error(ADDRESS_ENTRIES_GP_NAME + " is not found. Please set this Global Property value when using the " + this.getClass().getSimpleName());
			eventFilter = FILTER_UUID;
		} else if (entriesToSyncAsJSON == "") {
			log.warn(ADDRESS_ENTRIES_GP_NAME + " is emtpy. Syncing all locations...");
			eventFilter = FILTER_UUID;
		} else {
			List<AddressHierarchyEntry> addressEntriesToSync = null;
			try {
				addressEntriesToSync = mapper.readValue(entriesToSyncAsJSON, mapper.getTypeFactory().constructCollectionType(List.class, AddressHierarchyEntry.class));
				for (AddressHierarchyEntry entry : addressEntriesToSync ) {
					if (entry.getUserGeneratedId() == null) {
						log.error(entry.getName() + "/" + entry.getUuid() 
								+ " does not have a user-generated ID set. A 'User-generated ID' is needed by "
								+ this.getClass().getSimpleName() + " in order to retrieve the address events to be synced");
					}
					// AdressEntries are quite empty. Retrieving the full objects
					entry = addressHierarchyService.getAddressHierarchyEntryByUserGenId(entry.getUserGeneratedId());
					if (addressEntryFromEvent.getId().equals(entry.getId())) {
						eventFilter = FILTER_UUID;
					} else {
						AddressHierarchyEntry parent = addressEntryFromEvent.getParent();
						while (parent != null) {
							if (entry.getId().equals(parent.getId())) {
								eventFilter = FILTER_UUID;
							}
							parent = parent.getParent();
						}
					}
				}
			} catch (IOException e) {
				log.error("Unable to convert global property to Java object", e);
				log.error("Global Property: "+ ADDRESS_ENTRIES_GP_NAME + " Value:" + admniService.getGlobalProperty(ADDRESS_ENTRIES_GP_NAME));
			}

		}
		return eventFilter;
	}
}
