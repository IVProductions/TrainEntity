package trainmasterthesis.trainlogic;

import com.bitreactive.library.mqtt.MQTTConfigParam;
import com.bitreactive.library.mqtt.MQTTMessage;
import com.bitreactive.library.mqtt.robustmqtt.RobustMQTT.Parameters;

import no.ntnu.item.arctis.runtime.Block;

public class TrainLogic extends Block {

	
	//public static EV3MediumRegulatedMotor motor;
	public String train_id = "train_1";
	public String destination = "station_1";
	
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
		MQTTConfigParam m = new MQTTConfigParam("dev.bitreactive.com");
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
				int value = Integer.parseInt(requestList[3]);
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
				sendToBlock("STOPANDTERMINATE");
			}
		}
		
		
	}

	public void stopAndTerminate() {
	}	
}
