package trainmasterthesis.trainlogic;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import trainmasterthesis.colorsensorlogic.ColorSensorLogic.Sleeper;
import trainmasterthesis.colorsensorlogic.ColorSensorLogic.SleeperColor;
import trainmasterthesis.communication.Communication.Switch;

import com.bitreactive.library.mqtt.MQTTConfigParam;
import com.bitreactive.library.mqtt.MQTTMessage;
import com.bitreactive.library.mqtt.robustmqtt.RobustMQTT.Parameters;

import no.ntnu.item.arctis.runtime.Block;

public class TrainLogic extends Block {

	
	//public static EV3MediumRegulatedMotor motor;
	public String train_id = "train_1";
	public String destination = "station_1";
	public static BufferedWriter writer;
	
	/*public void InitMotors() {
		Port a = LocalEV3.get().getPort("A");
		motor = new EV3MediumRegulatedMotor(a);
		System.out.println("Train is ready!");*/
		/*motor.rotateTo(-40);
		Delay.msDelay(5000);
		motor.rotateTo(4);*/
		/*motor.rotateTo(0);
		Delay.msDelay(2000);
		motor.rotateTo(40);
		Delay.msDelay(4000);
		motor.setSpeed(1);
		motor.rotateTo(0);*/
		
		//motor.resetTachometer eller noe for å sette nytt nullpunkt --> motor.rotateTo(0) vil da rotere til dette punktet
		//return train_id;
	//}
	
	public Parameters initMQTTParam() {		
		MQTTConfigParam m = new MQTTConfigParam("192.168.0.100");
		m.addSubscribeTopic("IVProductionsTrainController");		
		Parameters p = new Parameters(m);
		
		return p;
	}
	
	public void handleMessage(MQTTMessage mqttMessage) {
		String initialRequestString = new String(mqttMessage.getPayload());
		System.out.println("Received command: "+initialRequestString);
		
		String[] requestList = initialRequestString.split(";");
		boolean sentFromController = requestList[0].toLowerCase() == "controller";
		String trainId = requestList[1];
		
		if (trainId.equals(train_id)) {
			String action = requestList[2];
			if(action.equalsIgnoreCase("setangle")) {
				int value = 0;
				try {
					value = Integer.parseInt(requestList[3]);
				} catch (Exception e) {
					System.out.println("Train received invalid speed value -> Speed is set to 0.");
				}
				System.out.println("Train-"+this.train_id+" has been commanded to rotate to value: "+value);			
				sendToBlock("ROTATEMOTOR",value);			
			}
			else if (action.equalsIgnoreCase("destination")) {
				String newDestination = requestList[3];
				System.out.println("New destination: "+newDestination);
				sendToBlock("SETDESTINATION", newDestination);
			}
			else if (action.equalsIgnoreCase("terminate")) {
				//motor.rotateTo(0); //stop train
				System.out.println("Train-"+this.train_id+" is terminating...");
				try {
					writer.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				sendToBlock("STOPANDTERMINATE");
			}
		}
		
		
	}

	public void stopAndTerminate() {
	}	
	
	public Switch SwitchAndZoneMapper(Sleeper sleeper) {
		Switch switcher = null;
		
		if(sleeper.prevDetectedColor == SleeperColor.BLUE && sleeper.detectedColor == SleeperColor.YELLOW){
			// SWITCH 1, ZONE 1
			switcher = new Switch(1,1, destination);
		}
		else if(sleeper.prevDetectedColor == SleeperColor.RED && sleeper.detectedColor == SleeperColor.GREEN){
			// SWITCH 1, ZONE 1
			switcher = new Switch(2,1, destination);
		}
		else if(sleeper.prevDetectedColor == SleeperColor.GREEN && sleeper.detectedColor == SleeperColor.BLUE){
			// SWITCH 1, ZONE 1
			switcher = new Switch(3,1, destination);
		}
		else if(sleeper.prevDetectedColor == SleeperColor.RED && sleeper.detectedColor == SleeperColor.BLUE){
			// SWITCH 1, ZONE 1
			switcher = new Switch(4,1, destination);
		}
		return switcher;
	}

	public int DestinationMapper(Sleeper sleeper) {
		
		// if (destination == sleeper.destination) { //STOP THE TRAIN }
		if(sleeper.prevDetectedColor == SleeperColor.WHITE && sleeper.detectedColor == SleeperColor.WHITE){
			// BERGEN F.EKS. Will always be WHITE WHITE atm
			return 1;
		}
		return 0;
	}

	public BufferedWriter writeToFileInit() {
		try {
			writer = new BufferedWriter(new FileWriter("statistics.txt"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return writer;
	}

	public String newDestination() {
		String newDestination = destination;
		while(destination.equals(newDestination)){
			Random rn = new Random();
			int randomStation = rn.nextInt(5) + 1;
			newDestination = "station_"+randomStation;
		}
		return newDestination;
	}
}
