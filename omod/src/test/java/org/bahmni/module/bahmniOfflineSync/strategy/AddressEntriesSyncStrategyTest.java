package org.bahmni.module.bahmniOfflineSync.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.transaction.PlatformTransactionManager;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class AddressEntriesSyncStrategyTest {

	private AddressEntriesSyncStrategy addressEntriesSyncStrategy;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private PatientService patientService;
	@Mock
	private EncounterService encounterService;
	@Mock
	private ConceptService conceptService;
	@Mock
	private PlatformTransactionManager platformTransactionManager;
	@Mock
	private AddressHierarchyService addressHierarchyService;
	@Mock
	private AdministrationService adminService;
	@Mock
	private LocationService locationService;

	private Patient patient;
	private Encounter encounter;
	private Concept concept;
	private Concept offlineConcept;

	private String encounterUuid = "ff17adba-9750-462e-be29-e35091af93df";
	private String patientUuid = "ff17adba-9750-462e-be29-e35091af93dd";
	private String conceptUuid = "ff17adba-9750-462e-be29-e35091af93ab";

	private AddressHierarchyEntry europe;
	private AddressHierarchyEntry france;
	private AddressHierarchyEntry paris;
	private AddressHierarchyEntry champsElysees;
	private AddressHierarchyEntry germany;
	private AddressHierarchyEntry berlin;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		PowerMockito.mockStatic(Context.class);
		when(Context.getPatientService()).thenReturn(patientService);
		when(Context.getEncounterService()).thenReturn(encounterService);
		when(Context.getConceptService()).thenReturn(conceptService);
		when(Context.getLocationService()).thenReturn(locationService);
		when(Context.getAdministrationService()).thenReturn(adminService);
		when(Context.getService(AddressHierarchyService.class)).thenReturn(addressHierarchyService);

		List<PlatformTransactionManager> registeredComponents = new ArrayList<PlatformTransactionManager>();
		registeredComponents.add(platformTransactionManager);
		when(Context.getRegisteredComponents(PlatformTransactionManager.class)).thenReturn(registeredComponents);

		addressEntriesSyncStrategy = new AddressEntriesSyncStrategy();
		patient = new Patient();
		patient.setUuid(patientUuid);
		encounter = new Encounter();
		encounter.setPatient(patient);
		encounter.setUuid(encounterUuid);

		concept = new Concept();
		concept.setUuid(conceptUuid);

		// Set the 'Offline Concepts' set and members
		offlineConcept = new Concept();
		ConceptName name = new ConceptName();
		name.setName("Offline Concepts");
		offlineConcept.setNames(Arrays.asList(name));
		offlineConcept.addSetMember(concept);

		// Setup an AH of 4 Address Entries of different levels
		europe = new AddressHierarchyEntry();
		AddressHierarchyLevel l1 = new AddressHierarchyLevel();
		l1.setLevelId(1);
		europe.setLevel(l1);
		europe.setName("Europe");
		europe.setUserGeneratedId("01");
		europe.setUuid("cb9483eb-c125-4dcf-a215-db88d6e4c59e");

		france = new AddressHierarchyEntry();
		AddressHierarchyLevel l2 = new AddressHierarchyLevel();
		l2.setLevelId(2);
		france.setLevel(l2);
		france.setName("France");
		france.setParent(europe);
		france.setUserGeneratedId("0101");
		france.setUuid("8b3f02f2-d4fc-442f-9d74-ce73605106ab");

		paris = new AddressHierarchyEntry();
		AddressHierarchyLevel l3 = new AddressHierarchyLevel();
		l3.setLevelId(3);
		paris.setLevel(l3);
		paris.setName("Paris");
		paris.setParent(france);
		paris.setUserGeneratedId("010101");
		paris.setUuid("6eba31d7-2048-4c53-a466-43848fb50c66");

		champsElysees = new AddressHierarchyEntry();
		AddressHierarchyLevel l4 = new AddressHierarchyLevel();
		l4.setLevelId(4);
		champsElysees.setLevel(l4);
		champsElysees.setName("Champs-Elysées");
		champsElysees.setParent(paris);
		champsElysees.setUserGeneratedId("01010101");
		champsElysees.setUuid("9b4b8b13-ed65-4b98-a158-c1c78d7da6ed");

		germany = new AddressHierarchyEntry();
		l2.setLevelId(2);
		germany.setLevel(l2);
		germany.setName("Germany");
		germany.setParent(europe);
		germany.setUserGeneratedId("0102");
		germany.setUuid("9b231a83-8f31-45ec-9ef7-1baa468002ee");

		berlin = new AddressHierarchyEntry();
		l3.setLevelId(3);
		berlin.setLevel(l3);
		berlin.setName("Berlin");
		berlin.setParent(germany);
		berlin.setUserGeneratedId("010201");
		berlin.setUuid("3edaa90f-9409-438d-9b66-d40b82310ec4");

	}

	@Test
	public void shouldGetCategoryList() throws Exception {
		List<String> categories = addressEntriesSyncStrategy.getEventCategoriesList();
		assertTrue(categories.contains("patient"));
		assertTrue(categories.contains("encounter"));
		assertTrue(categories.contains("addressHierarchy"));
		assertTrue(categories.contains("offline-concepts"));
		assertTrue(categories.contains("forms"));
		assertTrue(categories.size() == 5);
	}

	@Test
	public void shouldGetFilterForDevice() {
		ArrayList<String> expectedFilter = new ArrayList<String>();
		expectedFilter.add(AddressEntriesSyncStrategy.FILTER_UUID);

		assertEquals(expectedFilter, addressEntriesSyncStrategy.getFilterForDevice("","","").get("patient"));
		assertEquals(expectedFilter, addressEntriesSyncStrategy.getFilterForDevice("","","").get("encounter"));
		assertEquals(expectedFilter, addressEntriesSyncStrategy.getFilterForDevice("","","").get("addressHierarchy"));
		assertEquals(expectedFilter, addressEntriesSyncStrategy.getFilterForDevice("","","").get("offline-concepts"));
		assertEquals(expectedFilter, addressEntriesSyncStrategy.getFilterForDevice("","","").get("forms"));
		assertEquals(5, addressEntriesSyncStrategy.getFilterForDevice("providerUuid", "addressUuid", "locationUuid").size());
	}

	@Test
	public void shouldSetFilterForAllEncounters() {
		List<EventRecord> eventRecords = new ArrayList<EventRecord>();
		EventRecord er = new EventRecord("uuid", "encounter", "", "url/"+ encounterUuid, new Date(), "Encounter");
		eventRecords.add(er);
		List<EventLog> eventLogs = addressEntriesSyncStrategy.getEventLogsFromEventRecords(eventRecords);
		assertEquals(eventRecords.size(), eventLogs.size());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
		assertEquals(er.getCategory(), eventLogs.get(0).getCategory());
	}

	@Test
	public void shouldNotSetFilterForOtherCategories() {
		List<EventRecord> eventRecords = new ArrayList<EventRecord>();
		EventRecord er = new EventRecord("uuid", "lab", "", "url/"+ encounterUuid, new Date(), "lab");
		eventRecords.add(er);
		List<EventLog> eventLogs = addressEntriesSyncStrategy.getEventLogsFromEventRecords(eventRecords);
		assertEquals(1, eventLogs.size());
		assertEquals(null, eventLogs.get(0).getFilter());
	}

	@Test
	public void shouldSetFilterForAllOfflineConcepts() {
		List<EventRecord> eventRecords = new ArrayList<EventRecord>();
		EventRecord er = new EventRecord("uuid", "all-concepts", "", "url/"+ conceptUuid, new Date(), "all-concepts");
		eventRecords.add(er);
		when(conceptService.getConceptByUuid(conceptUuid)).thenReturn(concept);
		when(conceptService.getConceptByName("Offline Concepts")).thenReturn(offlineConcept);

		List<EventLog> eventLogs = addressEntriesSyncStrategy.getEventLogsFromEventRecords(eventRecords);
		assertEquals(eventRecords.size(), eventLogs.size());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
		assertEquals("offline-concepts", eventLogs.get(0).getCategory());
	}

	@Test
	public void shouldSetFilterWhenAddressEventExactMatchesGP() {
		String globalPropertyAsJSON = "[{\"userGeneratedId\":\"010101\"}, {\"userGeneratedId\":\"0101\"}]";
		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn(globalPropertyAsJSON);

		List<EventLog> eventLogs = setupEventLogsAndFilters(europe, france, paris, champsElysees);
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(1).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(2).getFilter());
	}

	@Test
	public void shouldSetFilterWhenAddressEventIsChildOfAnAddressInGP() {
		String globalPropertyAsJSON = "[{\"userGeneratedId\":\"010101\"}, {\"userGeneratedId\":\"0101\"}]";
		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn(globalPropertyAsJSON);

		String champsFilter = setupEventLogsAndFilters(europe, france, paris, champsElysees).get(2).getFilter();
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, champsFilter);
	}

	@Test
	public void shouldSetFilterOnAllLocationsWhenGPIsEmptyOrNull() {

		List<EventLog> eventLogs = new ArrayList<>();

		when(adminService.getGlobalProperty(any())).thenReturn(null);
		eventLogs = setupEventLogsAndFilters(europe, france, paris, champsElysees);

		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(1).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(2).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(3).getFilter());

		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn("");
		eventLogs = setupEventLogsAndFilters(europe, france, paris, champsElysees);

		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(1).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(2).getFilter());

	}

	@Test
	public void shouldNotFailWhenAddressHasNoUserGenId() {

		String globalPropertyAsJSON = "";
		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn(globalPropertyAsJSON);

		// Create a corrupted address with no UserGeneratedId set
		AddressHierarchyEntry noUserGenIdAddress = new AddressHierarchyEntry();
		noUserGenIdAddress.setName("Address_With_No_UserGenID");
		noUserGenIdAddress.setParent(paris);
		noUserGenIdAddress.setId(5);
		noUserGenIdAddress.setUuid("1b35005c-a22e-4950-a787-646bbe8388b3");

		List<EventLog> eventLogs = setupEventLogsAndFilters(europe, france, paris, champsElysees, noUserGenIdAddress);

		assertEquals(null, eventLogs.get(4).getFilter());
	}

	@Test
	public void shouldSetFiltersForChildrenAndParentsOfAnEntry() {

		String globalPropertyAsJSON = "[{\"userGeneratedId\":\"010101\"}]";
		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn(globalPropertyAsJSON);

		List<EventLog> eventLogs = setupEventLogsAndFilters(europe, france, paris, champsElysees, berlin, germany);

		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(1).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(2).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(3).getFilter());
		assertEquals(null, eventLogs.get(4).getFilter());
		assertEquals(null, eventLogs.get(5).getFilter());

		globalPropertyAsJSON = "[{\"userGeneratedId\":\"010201\"}]";
		when(adminService.getGlobalProperty(AddressEntriesSyncStrategy.ADDRESS_ENTRIES_GP_NAME)).thenReturn(globalPropertyAsJSON);

		eventLogs = setupEventLogsAndFilters(europe, france, paris, champsElysees, berlin, germany);

		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
		assertEquals(null, eventLogs.get(1).getFilter());
		assertEquals(null, eventLogs.get(2).getFilter());
		assertEquals(null, eventLogs.get(3).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(4).getFilter());
		assertEquals(AddressEntriesSyncStrategy.FILTER_UUID, eventLogs.get(5).getFilter());

	}

	private List<EventLog> setupEventLogsAndFilters(AddressHierarchyEntry... addresses) {

		List<EventRecord> eventRecords = new ArrayList<EventRecord>();
		List<EventLog> eventLogs = new ArrayList<EventLog>();

		for (AddressHierarchyEntry address : addresses) {
			when(addressHierarchyService.getAddressHierarchyEntryByUuid(address.getUuid())).thenReturn(address);
			when(addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getUserGeneratedId())).thenReturn(address);
			EventRecord er  = new EventRecord(UUID.randomUUID().toString(), "address", "", "url/"+ address.getUuid(),new Date(),"addressHierarchy");
			eventRecords.add(er);
		}

		eventLogs = addressEntriesSyncStrategy.getEventLogsFromEventRecords(eventRecords);

		return eventLogs;
	}


}
