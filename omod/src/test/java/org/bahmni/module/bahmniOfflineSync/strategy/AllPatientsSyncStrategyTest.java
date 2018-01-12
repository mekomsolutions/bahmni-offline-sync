package org.bahmni.module.bahmniOfflineSync.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.bahmni.module.bahmniOfflineSync.eventLog.EventLog;
import org.ict4h.atomfeed.server.domain.EventRecord;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.transaction.PlatformTransactionManager;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class AllPatientsSyncStrategyTest {
    
	private AllPatientsSyncStrategy allPatientsSyncStrategy;

	 @Rule
	    public ExpectedException expectedException = ExpectedException.none();

	    @Mock
	    private PatientService patientService;
	    @Mock
	    private EncounterService encounterService;
	    @Mock
	    private PlatformTransactionManager platformTransactionManager;
	    @Mock
	    private AddressHierarchyService addressHierarchyService;
	    @Mock
	    private LocationService locationService;
	    private Patient patient;
	    Encounter encounter;
	    private String encounterUuid = "ff17adba-9750-462e-be29-e35091af93df";
	    private String patientUuid = "ff17adba-9750-462e-be29-e35091af93dd";

	    @Before
	    public void setUp() throws Exception {
	        initMocks(this);
	        PowerMockito.mockStatic(Context.class);
	        Mockito.when(Context.getPatientService()).thenReturn(patientService);
	        Mockito.when(Context.getEncounterService()).thenReturn(encounterService);
	        Mockito.when(Context.getLocationService()).thenReturn(locationService);
	        List<PlatformTransactionManager> registeredComponents = new ArrayList<PlatformTransactionManager>();
	        registeredComponents.add(platformTransactionManager);
	        Mockito.when(Context.getRegisteredComponents(PlatformTransactionManager.class)).thenReturn(registeredComponents);
	        allPatientsSyncStrategy = new AllPatientsSyncStrategy();
	        patient = new Patient();
	        patient.setUuid(patientUuid);
	        encounter = new Encounter();
	        encounter.setPatient(patient);
	        encounter.setUuid(encounterUuid);
	    }
    @Test
    public void shouldGetCategoryList() throws Exception {
        List<String> categories = allPatientsSyncStrategy.getEventCategoriesList();
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
    	expectedFilter.add(AllPatientsSyncStrategy.FILTER_UUID);
    	
    	assertEquals(expectedFilter, allPatientsSyncStrategy.getFilterForDevice("","","").get("patient"));
    	assertEquals(expectedFilter, allPatientsSyncStrategy.getFilterForDevice("","","").get("encounter"));
    	assertEquals(new ArrayList<String>(), allPatientsSyncStrategy.getFilterForDevice("","","").get("addressHierarchy"));
    	assertEquals(new ArrayList<String>(), allPatientsSyncStrategy.getFilterForDevice("","","").get("offline-concepts"));
    	assertEquals(new ArrayList<String>(), allPatientsSyncStrategy.getFilterForDevice("","","").get("forms"));
        assertEquals(5, allPatientsSyncStrategy.getFilterForDevice("providerUuid", "addressUuid", "locationUuid").size());
    }

    @Test
    public void shouldSetFilterForAllEncounters() {
    	
    	PowerMockito.mockStatic(Context.class);
    	
    	List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er = new EventRecord("uuid", "encounter", "", "url/"+ encounterUuid, new Date(), "Encounter");
        eventRecords.add(er);
        List<EventLog> eventLogs = allPatientsSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(AllPatientsSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(), eventLogs.get(0).getCategory());
    }

    @Test
    public void shouldSetFilterForAllPatients() {
    	
    	PowerMockito.mockStatic(Context.class);
    	
    	List<EventRecord> eventRecords = new ArrayList<EventRecord>();
        EventRecord er = new EventRecord("uuid", "patient", "", "url/"+ patientUuid, new Date(), "Patient");
        eventRecords.add(er);
        List<EventLog> eventLogs = allPatientsSyncStrategy.getEventLogsFromEventRecords(eventRecords);
        assertEquals(eventRecords.size(), eventLogs.size());
        assertEquals(AllPatientsSyncStrategy.FILTER_UUID, eventLogs.get(0).getFilter());
        assertEquals(er.getCategory(), eventLogs.get(0).getCategory());
    }
     
}
