package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static FareCalculatorService fareCalculator;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
        fareCalculator = new FareCalculatorService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculator);
        int next = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        parkingService.processIncomingVehicle();
        //Récupere le ticket pour le véhicule immaticulé "ABCDEF"
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        //On vérifie que le ticket existe
        Assertions.assertNotNull(ticket);
        //On vérifie que le ticket est associé à un parking
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        //On vérifie que le parking est en status availibility: false
        Assertions.assertNotNull(parkingSpot);
        //On vérifie que la place qui était disponible soit bien celle retourné.
        Assertions.assertEquals(next, parkingSpot.getId());
        Assertions.assertFalse(parkingSpot.isAvailable());
    }

    @Test
    public void testParkingLotExit() throws InterruptedException{
        testParkingACar();
        //On crée le service ParckingService et ici on va surcharger le calcul du prix car on veut update la valeur du prix en base de donnée
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, new FareCalculatorService() {
        	@Override
        	public void calculateFare(Ticket ticket) {
        		ticket.setPrice(42);
        	}
        });
        //On procède à la sortie du véhicule
        parkingService.processExitingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        //On vérifie que le ticket existe
        Assertions.assertNotNull(ticket);
        //On vérifie que le ticket a bien une date de sortie de véhicule
        Assertions.assertNotNull(ticket.getOutTime());
        //On vérifie que le price existe est positifs
        Assertions.assertEquals(42,ticket.getPrice());
        //TODO: check that the fare generated and out time are populated correctly in the database
    }

}
