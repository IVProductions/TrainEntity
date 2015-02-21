package trainmasterthesis.colorsensorlogic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;

import lejos.hardware.Button;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.robotics.Color;
import no.ntnu.item.arctis.runtime.Block;

public class ColorSensorLogic extends Block implements Runnable {
	
	
	
	private EV3ColorSensor colorSensor;
	private EV3ColorSensor colorSensorSide;
	private int detectedColorId;
	private int detectedColorIdSide;
	private boolean trainHasStopped = true;
	private int prevDetectedColorId = -1;
	private int prevDetectedColorId2 = -1;
	public static BufferedWriter writer;
	//private int prevDetectedColorIdSide = -1;

	//used to measure speed
	public long registeredTime = System.nanoTime()/1000000;
	public long lastRegisteredTime = System.nanoTime()/1000000;
	public double timeDifference;
	public double sleeperDistance = 32.5;

	//colors
	private Integer NONE = -1;
	private Integer RED = 0;
	private Integer GREEN = 1;
	private Integer BLUE = 2;
	private Integer YELLOW = 3;
	private Integer WHITE = 6;
	private Integer BLACK = 7; //also grey, same as colorless sleepers
	private Integer BROWN = 13;//same as table
	public java.lang.Thread colorSensorThread;
	//public String train_id;
	public java.lang.String destination;
	public boolean isStopping = false;
	public java.lang.String train_id;

	public static class Sleeper {
		public SleeperColor prevDetectedColor;
		public SleeperColor detectedColor;
		//public double measuredSpeed;
		//public DecimalFormat numberFormat = new DecimalFormat("#.00"); //used to format the speed to two decimals
		
		public Sleeper(SleeperColor prevdetected, SleeperColor detected) {
			this.detectedColor = detected;
			this.prevDetectedColor = prevdetected;
			//measuredSpeed = speed;
		}
		/*public String toString() {
			return String.valueOf(detectedColor)+","+numberFormat.format(measuredSpeed);
		}*/
	}
	
	public static enum SleeperColor {
		NONE, RED, GREEN, BLUE, YELLOW, WHITE, BLACK, BROWN;
	}

	//this method is called from init (thread.start())
	@Override
	public void run() {
		
		Thread thisThread = Thread.currentThread();
		colorSensor = new EV3ColorSensor(SensorPort.S1);
		colorSensor.setFloodlight(Color.WHITE);
		try {
			colorSensorSide = new EV3ColorSensor(SensorPort.S2);
			colorSensorSide.setFloodlight(Color.WHITE);
		} catch (Exception e) {
			System.out.println("Side sensor is off. Ignoring this sensor.");
		}
		
		System.out.println("\n--------------------------------------\n\n  -> COLOR SENSORS ARE READY <-\n\n--------------------------------------\n\n");
		
		int counter = 103;
		while(colorSensorThread==thisThread) {
			detectedColorId = colorSensor.getColorID();
			if(detectedColorId != prevDetectedColorId) {
				if(detectedColorId == 13){
					registeredTime = System.nanoTime()/1000000;
					timeDifference = Double.parseDouble(Long.toString(registeredTime - lastRegisteredTime));
					double calculatedSpeed = sleeperDistance/timeDifference;
					try {
						writeToFile(calculatedSpeed+"");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					lastRegisteredTime = registeredTime;
					counter--;
					//System.out.println(counter);
				}
				if(detectedColorId == 0){
					counter = 103;
					try {
						writeToFile("0");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//System.out.println(counter);
				}
			}
			/*
			try {
				detectedColorIdSide = colorSensorSide.getColorID();
			} catch (Exception e) {
				detectedColorIdSide = -1;  //if side sensor is off, it won't detect anything
			}
			
			if(detectedColorIdSide != -1 && !isStopping){
				isStopping = true;
				// SHOULD MAKE UNIQUE COLOR COMBINATIONS FOR DESTINATIONS
				Sleeper sleeper = new Sleeper(SleeperColor.WHITE, SleeperColor.WHITE);
				sendToBlock("DESTINATION", sleeper);
			}
			
			/*
			if (trainHasStopped) {
				//don't process color logic -> make thread sleep to save resources
				synchronized(this) { 
					try {
						this.wait();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}	
				}
			}
			*/
			/*
			if(detectedColorId != prevDetectedColorId) {
				try {
					writeToFile(""+detectedColorId);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}*/
			prevDetectedColorId = detectedColorId;
			/*
			if (detectedColorId != prevDetectedColorId)  {	
				if (detectedColorId != 7 && detectedColorId != 13 && detectedColorId != 6) {
					//System.out.println(colorToString(prevDetectedColorId)+" -> "+colorToString(detectedColorId));
					//System.out.println("DetectedId: "+detectedColorId+", "+colorToString(detectedColorId)+". Last color was: "+prevDetectedColorId+", "+colorToString(prevDetectedColorId));
					SleeperDetecter(detectedColorId, prevDetectedColorId);
				}
			}*/
			try {
				Thread.sleep(14); //the thread is put to sleep to avoid random detection of colors in the transition between two colors
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} 
		
	}
	
	// Write to file for statistics
	public static void writeToFile(String input) throws IOException{
	    writer.write(input+"\n");
	}
	
	//This method finds the correct zone controller and switch based on the detected color combination and then sends a request 
	public void SleeperDetecter(int detectedColor, int prevDetectedColor) {
		Sleeper sleeper = null;
		String colorCombination = prevDetectedColor+""+detectedColor;
		this.prevDetectedColorId = this.detectedColorId;
		//String topicAndSwitchId;
		switch(colorCombination) {	
		
		// BLUE - YELLOW
		case "23":
			sleeper = new Sleeper(SleeperColor.BLUE, SleeperColor.YELLOW);
			break;
			//topicAndSwitchId = "zonecontroller_1;switch_1"; 
			
		// RED - GREEN
		case "01":
			sleeper = new Sleeper(SleeperColor.RED, SleeperColor.GREEN);
			break;
			//topicAndSwitchId = "zonecontroller_1;switch_2"; 
			
		// GREEN - BLUE
		case "12":
			sleeper = new Sleeper(SleeperColor.GREEN, SleeperColor.BLUE);
			break;
			//topicAndSwitchId = "zonecontroller_1;switch_3"; 
			
		// RED - BLUE
		case "02":
			sleeper = new Sleeper(SleeperColor.RED, SleeperColor.BLUE);
			break;
			//topicAndSwitchId = "zonecontroller_1;switch_4"; 
		
		default: return;		
		}
		
		sendToBlock("SLEEPER_TRACK", sleeper);
		//sendToBlock("COMMUNICATEWITHZONECONTROLLER", topicAndSwitchId+";"+destination);
	}

	public void init(BufferedWriter writer) {
		this.writer = writer;
		Button.LEDPattern(2); //Red constant light
		colorSensorThread = new Thread(this);
		colorSensorThread.start(); //run run() method
		//sendToBlock("INITOK");
	}

	private static String colorToString(int colorId) {
		switch (colorId) {
		case 0: return "RED";
		case 1: return "GREEN";
		case 2: return "BLUE";
		case 3: return "YELLOW";
		case 6: return "WHITE";
		case 7: return "BLACK";
		case 13: return "BROWN";
		}
		return "NONE";
	}

	//terminates thread
	public void stopThread() {
		colorSensorThread = null;
		System.out.println("Color sensor thread has been terminated..");
	}

	public void motorStopped() {
		isStopping = false;
		System.out.println("The motor has stopped..");
	}

}
