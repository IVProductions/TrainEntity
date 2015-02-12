package trainmasterthesis.switchctrcom;

import com.bitreactive.library.mqtt.MQTTMessage;

import no.ntnu.item.arctis.runtime.Block;

public class SwitchCtrCom extends Block {
	
	private final String switchController1_topic = "IVProductionsSwitchController1";
	private final String switchController2_topic = "IVProductionsSwitchController2";
	public int trainId;
	
	
    //find MQTT topic from incoming color, to communicate with specifi switchcontroller
	public String getSwitchTopicFromSleeperColorId(int sleeperColorId) {
		switch (sleeperColorId) {
		case 1: return switchController1_topic;
		case 2: return switchController2_topic;
		}
		return "";
	}

	public int getSwitchIdFromSleeperColorId(int sleeperColorId) {
		switch (sleeperColorId) {
		case 1: return 1;
		case 2: return 2;
		}
		return 0;
	}

	public void init(int trainId) {
		System.out.println("Switch Controller Communication block initiated..");
		this.trainId = trainId;
	}
	
	
	public MQTTMessage createMQTTMessage(String colorString) {
		String switchControllerTopic = getSwitchTopicFromSleeperColorId(1);
		if (switchControllerTopic == "") {
			System.out.println("Could not find topic for color id");
			//return;
		}
		int switchId = getSwitchIdFromSleeperColorId(1);
		if (switchId == 0) {
			System.out.println("Could not find switch Id for color id");
		}
		String request = trainId+";"+switchId; //include speed, train length etc.
		byte[] bytes = request.getBytes();
		
		MQTTMessage message = new MQTTMessage(bytes, switchControllerTopic);
		message.setQoS(2);
		return message;
	}
		
}
