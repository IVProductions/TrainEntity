package trainmasterthesis.motor;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.Port;
import no.ntnu.item.arctis.runtime.Block;

public class Motor extends Block {

	public static EV3MediumRegulatedMotor motor;
	
	public void initMotor() {
		Port a = LocalEV3.get().getPort("A");
		motor = new EV3MediumRegulatedMotor(a);
		System.out.println("\n--------------------------------------\n\n   -> TRAIN MOTOR IS READY <-\n\n--------------------------------------\n\n");
	}

	public void rotateToValue(int value) {
		motor.rotateTo(value);
	}

	public void stopMotor() {
		System.out.println("Train has reached its destination");			
		motor.rotateTo(0);	
	}

	public void stopAndTerminate() {
		motor.rotateTo(0);	
		sendToBlock("TERMINATE");
	}

}
