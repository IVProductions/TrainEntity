package trainmasterthesis.motor;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.Port;
import no.ntnu.item.arctis.runtime.Block;

public class Motor extends Block {

	public static EV3MediumRegulatedMotor motor;
	
	private int speedLevel = 0;
	private int currentValue = 0;
	private boolean shouldstop = false;
	private boolean destination = false;
	
	public void initMotor() {
		Port a = LocalEV3.get().getPort("A");
		motor = new EV3MediumRegulatedMotor(a);
		System.out.println("\n--------------------------------------\n\n   -> TRAIN MOTOR IS READY <-\n\n--------------------------------------\n\n");
	}

	public void rotateToValue(int value) {
			switch(value){
			case 0: speedLevel = 0; break;
			case 15: speedLevel = 1;break;
			case 30: speedLevel = 2;break;
			case 45: speedLevel = 3;break;
			case 60: speedLevel = 4;break;
			case 75: speedLevel = 5;break;
			case 90: speedLevel = 6;break;
			case 105: speedLevel = 7;break;
			default: speedLevel = 0; break;
			}
			
			if(currentValue != value){
				currentValue = value;
				motor.rotateTo(value);
				//System.out.println("motor rotated to "+value);
			}
			
			if(speedLevel == 0){
				//System.out.println("should stop");
				shouldstop = true;
				sendToBlock("ISSTOPPING");
			}
			else if(shouldstop && speedLevel > 0){
				shouldstop = false;
				sendToBlock("NOTIFYCOLORSENSOR");
			}		
		
	}

	public void stopMotor() {
		//System.out.println("Train has reached its destination");			
		motor.rotateTo(0);	
	}

	public void stopAndTerminate() {
		motor.rotateTo(0);	
		sendToBlock("TERMINATE");
	}

	public void speedUp(int value) {
		currentValue = value;
		speedLevel = 2;
		motor.rotateTo(value);
	}

	public void stopForSwitch() {
		currentValue = 0;
		motor.rotateTo(0);
		sendToBlock("STOPPEDFORSWITCH");
	}




}
