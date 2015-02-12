package trainmasterthesis.lightsensorlogic;

import lejos.hardware.Button;
import lejos.hardware.sensor.EV3ColorSensor;
import no.ntnu.item.arctis.runtime.Block;

public class LightSensorLogic extends Block implements Runnable {
	public java.lang.Thread colorSensorThread;
	private EV3ColorSensor colorSensor;
	
	//colors
		private Integer NONE = -1;
		private Integer RED = 0;
		private Integer GREEN = 1;
		private Integer BLUE = 2;
		private Integer YELLOW = 3;
		private Integer WHITE = 6;
		private Integer BLACK = 7; //also grey, same as colorless sleepers
		private Integer BROWN = 13;//same as table
	
	public void init() {
		Button.LEDPattern(2); //Red constant light
		colorSensorThread = new Thread(this);
		colorSensorThread.start(); //start thread 
	}

	//this method is called from init (thread.start())
	@Override
	public void run() {
		
		
	}

	//terminates thread
	public void stopThread() {
		colorSensorThread = null;
	}

}
