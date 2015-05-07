package trainmasterthesis.communication;

import trainmasterthesis.colorsensorlogic.ColorSensorLogic.SleeperColor;

import java.awt.event.ActionListener;
import java.util.UUID;
import java.awt.event.ActionEvent;

import javax.swing.Timer;

import com.bitreactive.library.mqtt.MQTTConfigParam;
import com.bitreactive.library.mqtt.MQTTMessage;

import no.ntnu.item.arctis.runtime.Block;
import lejos.hardware.Button;

public class Communication extends Block {

	public boolean isStopping = false;
	public int currentSpeedValue = 30;
	public boolean stoppedEmergeny = false;
	public boolean hasStopped = false;
	public boolean collisionDetection = true;
	public String currentRequestId = "";
	public java.lang.String lastTopic = "";
	public java.lang.String currentTopic = "";
	public java.lang.String currentZoneController_Id;
	public java.lang.String train_Id;
	public java.lang.String currentSwitch_Id;
	public java.lang.String destination = "";
	public java.lang.String requestId;
	public int currentTrackId = 22;
	public static class Switch {
		public int switch_id;
		public int zone_id;
		public String destination_id;
		
		public Switch(int switch_id, int zone_id, String destination) {
			this.switch_id = switch_id;
			this.zone_id = zone_id;
			this.destination_id = destination;
		}
	}

	
	public MQTTConfigParam initPublish() {
		MQTTConfigParam m = new MQTTConfigParam("192.168.0.100");
		return m;
	}
	
	public long time = System.nanoTime()/1000000;
	public long timeNow = System.nanoTime()/1000000;
	public double timeDifference;
	
	public void handleZoneControllerMessage(MQTTMessage mqttMessage) {
		String initialResponseString = new String(mqttMessage.getPayload());
		
		if(initialResponseString.equalsIgnoreCase("starttimer")){
			System.out.println("receivied starttimer");
			sendToBlock("STARTPRINTTIMER");
			return;
		}
		String[] responseList = initialResponseString.split(";");
		
		// UNSUBSCRIBE from last topic
		if(responseList[0].equals(train_Id) && responseList[2].equals("unsubscribe")){
			if(!responseList[1].equals("")){
				// There is a topic to unsubscribe from
				System.out.println("UNSUBSCRIBE from "+responseList[1]);
				sendToBlock("UNSUBSCRIBE", responseList[1]); 
			}
			return;
		}
		
		// This message belongs to this train
		if(responseList[0].equals(currentTopic) && responseList[1].equals(train_Id)){

			
			// Get current track for train
			if(responseList[2].equals("currenttrack")){
				currentTrackId = Integer.parseInt(responseList[3]);
				System.out.println("NEW TRACK "+currentTrackId);
				double differenceTime = System.nanoTime()/1000000 -timeNowTrack;
				//System.out.println("Response time for currentTrack: "+differenceTime);
				
				if(responseList[4].equals("slowspeed")){
					sendToBlock("SETMOTORANGLE",15);
				}
				else if(responseList[4].equals("normalspeed")){
					sendToBlock("SETMOTORANGLE",30);
				}
				return;
			}
			

			// Stop train before switch
			if(responseList[2].equals("waitforintersection")){
				collisionDetection = false;		
				sendToBlock("SETMOTORANGLE",0);
				currentSpeedValue = 0;
				isStopping = true;
				System.out.println("Received: waitforintersection");
				return;
			}
			
			// The train is good to go for switch
			if(responseList[2].equals("intersectionclear")){
				collisionDetection = true;		
				sendToBlock("SETMOTORANGLE",30);
				currentSpeedValue = 30;
				isStopping = false;
				System.out.println("Received: intersectionclear");
				return;
			}
			// Logic for collisiion detection
			if(collisionDetection){	
				if(responseList[2].equals("stop")){
					if(!isStopping){
						sendToBlock("SETMOTORANGLE",0);
						currentSpeedValue = 0;
						isStopping = true;
					}
					return;
				}
				
				if(responseList[2].equals("ok")){
					if(hasStopped){
						sendToBlock("SETMOTORANGLE",30);
						currentSpeedValue = 30;
						isStopping = false;
						hasStopped = false;
					}		
					return;
				}
			}
			// Stop train
			

			// Slow down train
			/*if(responseList[2].equals("slowspeed")){
				if(!isStopping){
					sendToBlock("SETMOTORANGLE",15);
					currentSpeedValue = 15;
				}
				return;
			}*/
			
			

			
			
			

		}
	}

	public boolean sameAsLastTopic(String currenTopic) {
		return currentTopic == lastTopic;
	}

	
	public MQTTMessage requestAdvice() {
		
		time = System.nanoTime()/1000000;
		
		// JSON MQTT Message
		// MqttMessage message = new MqttMessage();
		// message.setPayload("{foo: bar, lat: 0.23443, long: 12.3453245}".getBytes());
		// client.publish("foo", message);
		requestId = UUID.randomUUID().toString();

		//Create message that explains what switch the train is approaching, together with speed, length, direction. Also include "from" part
		String request = train_Id+";"+currentZoneController_Id+";"+currentSwitch_Id+";"+destination+";"+requestId; //FOR EXAMPLE: "train1;switch2;1.5;0.35;west";
		byte[] bytes = request.getBytes();
		//String topic = "IVProductionsSwitchController";
		MQTTMessage message = new MQTTMessage(bytes, currentTopic);
		message.setQoS(0);
		return message;
	}

	public String[] subscribeFormat(String topic) {
		String[] topicHolder = new String[1];
		topicHolder[0] = topic;
		return topicHolder;
	}

	public String[] unsubscribeFormat(String topic) {
		String[] topicHolder = new String[1];
		topicHolder[0] = topic;
		System.out.println("UNSUBSCRIBE topicHolder: "+topicHolder[0]);
		return topicHolder;
	}

	public String setTopicControllerSwitch(Switch switcher) {
		currentTopic = "zonecontroller_"+switcher.zone_id;
		currentZoneController_Id = "zonecontroller_"+switcher.zone_id;
		currentSwitch_Id = "switch_"+switcher.switch_id;
		destination = switcher.destination_id;
		return currentTopic;
	}

	public MQTTConfigParam initSubscribe() {
		MQTTConfigParam m = new MQTTConfigParam("192.168.0.100");
		return m;
	}

	public String setNewTopic(String topic) {
		lastTopic = currentTopic;
		currentTopic = topic;
		return currentTopic;
	}

	public MQTTMessage sendNewZoneRequest() {
		System.out.println("Subscribe to "+currentTopic);
		requestId = UUID.randomUUID().toString();
		String request = train_Id+";"+currentTopic+";;"+"newzone;"+currentTrackId+";"+destination+";"+requestId; 
		byte[] bytes = request.getBytes();
		MQTTMessage message = new MQTTMessage(bytes, currentTopic);
		message.setQoS(0);
		return message;
	}

	double timeNowTrack = 0.0;
	public MQTTMessage newTrackRequest(String color) {
		timeNowTrack = System.nanoTime()/1000000;
		requestId = UUID.randomUUID().toString();
		String request = train_Id+";"+currentTopic+";;"+"colorsleeper;"+color+";"+destination+";"+requestId;
		byte[] bytes = request.getBytes();
		MQTTMessage message = new MQTTMessage(bytes, currentTopic);
		message.setQoS(0);
		return message;
	}

	public String unSubscribeLastTopic() {
		String sendLastTopic = lastTopic;
		lastTopic = "";
		return sendLastTopic;
	}

	public void sendUnSubscribeRequest(String lastTopic) {
		requestId = UUID.randomUUID().toString();
		String request = train_Id+";"+lastTopic+";"+"unsubscribe"; //FOR EXAMPLE: "train1;switch2;1.5;0.35;west";
		byte[] bytes = request.getBytes();
		MQTTMessage message = new MQTTMessage(bytes, lastTopic);
		message.setQoS(0);
		if(lastTopic.equals("")){
			sendToBlock("INVALID_TOPIC");
		}
		else {
			sendToBlock("VALID_TOPIC",message);
		}
	}

	double timeForNewPos = 0;
	public MQTTMessage newSpeedPositionRequest(String speedPosition) {
		
		timeForNewPos = System.nanoTime()/1000000;
		requestId = UUID.randomUUID().toString();
		String request = train_Id+";"+currentTopic+";"+lastTopic+";"+"speedposition;"+speedPosition+";"+destination+";"+requestId;
		byte[] bytes = request.getBytes();
		MQTTMessage message = new MQTTMessage(bytes, currentTopic);
		message.setQoS(0);
		if(!lastTopic.equals("")){
			MQTTMessage message2 = new MQTTMessage(bytes, lastTopic);
			message2.setQoS(0);
			sendToBlock("ZONE_TRANSITION", message2);
		}
		
		
		return message;
	}
	
	

	public void ZONE_TRANSITION() {
	}

	public MQTTMessage checkIfReady(String position) {
		if(collisionDetection){
			hasStopped = true;
			requestId = UUID.randomUUID().toString();
			String request = train_Id+";"+currentTopic+";"+";"+"requeststart"+";"+position+";"+destination+";"+requestId;
			byte[] bytes = request.getBytes();
			MQTTMessage message = new MQTTMessage(bytes, currentTopic);
			message.setQoS(0);
			return message;
		}
		else {
			return null;
		}
		
	}

	public void LEDStation(String station) {
		if(station.equals("station_1")){
			Button.LEDPattern(1);
		}
		else if(station.equals("station_2")){
			Button.LEDPattern(2);
		}
		else if(station.equals("station_3")){
			Button.LEDPattern(3);
		}
		else if(station.equals("station_4")){
			Button.LEDPattern(4);
		}
		else if(station.equals("station_5")){
			Button.LEDPattern(5);
		}
	}


}
