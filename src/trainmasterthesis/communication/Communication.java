package trainmasterthesis.communication;

import trainmasterthesis.colorsensorlogic.ColorSensorLogic.SleeperColor;

import com.bitreactive.library.mqtt.MQTTConfigParam;
import com.bitreactive.library.mqtt.MQTTMessage;

import no.ntnu.item.arctis.runtime.Block;

public class Communication extends Block {

	public java.lang.String lastTopic;
	public java.lang.String currentTopic;
	public java.lang.String currentZoneController_Id;
	public java.lang.String train_Id;
	public java.lang.String currentSwitch_Id;
	public java.lang.String destination;
	
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
		MQTTConfigParam m = new MQTTConfigParam("dev.bitreactive.com");
		return m;
	}
	
	public void handleZoneControllerMessage(MQTTMessage mqttMessage) {
		//LOGIC FOR MESSAGES SENT FROM ZONE CONTROLLER
		String initialRequestString = new String(mqttMessage.getPayload());
		String[] requestList = initialRequestString.split(";");
		String sentFromZoneController_Id = requestList[0];
		String sentToTrain_Id = requestList[1];
		if (sentFromZoneController_Id != currentZoneController_Id || sentToTrain_Id != train_Id) return;		//FIRST MAKE SURE IT IS SENT FROM THE CORRECT ZONE CONTROLLER
		//boolean sentFromController = requestList[0].toLowerCase() == "controller";
		//int trainId = Integer.parseInt(requestList[1]);
		
	}

	public boolean sameAsLastTopic(String currenTopic) {
		return currentTopic == lastTopic;
	}

	public MQTTMessage requestAdvice() {
		// JSON MQTT Message
		// MqttMessage message = new MqttMessage();
		// message.setPayload("{foo: bar, lat: 0.23443, long: 12.3453245}".getBytes());
		// client.publish("foo", message);

		//Create message that explains what switch the train is approaching, together with speed, length, direction. Also include "from" part
		String request = train_Id+";"+currentZoneController_Id+";"+currentSwitch_Id+";"+destination; //FOR EXAMPLE: "train1;switch2;1.5;0.35;west";
		byte[] bytes = request.getBytes();
		//String topic = "IVProductionsSwitchController";
		MQTTMessage message = new MQTTMessage(bytes, currentTopic);
		message.setQoS(2);
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
		MQTTConfigParam m = new MQTTConfigParam("dev.bitreactive.com");
		return m;
	}


}
