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
import java.util.Date;
import java.util.TimerTask;
import java.util.Timer;
import java.util.TimerTask;

import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.robotics.Color;
import no.ntnu.item.arctis.runtime.Block;

public class ColorSensorLogic extends Block implements Runnable {
	
	private int currentTrack = 0;
	private int previousTrack = 0;
	private int globalSleeperkNr = 0;
	private int currentGlobalSleeperNr = 0;
	private EV3ColorSensor colorSensor;
	private EV3ColorSensor colorSensorSide;
	private int detectedColorId;
	private int detectedColorIdSide;
	private boolean trainHasStopped = true;
	private int prevDetectedColorId = -1;
	private int prevDetectedColorId2 = -1;
	private boolean isDestination = false;
	public static BufferedWriter writer;
	//private int prevDetectedColorIdSide = -1;

	public String printPosition = "";
	private int currentZone = 0;
	private int previousZone = 0;
	private boolean canSwitchZone = true;
	private boolean canSwitchTrack = false;
	private boolean detectedZoneTransitionSleeper = false;
	private boolean startTrack = true;
	private boolean firstTrackInZone = false;
	private boolean canWritePositionAndSpeed = false;
	
	//used to measure speed
	public long registeredTime = System.nanoTime()/1000000;
	public long lastRegisteredTime = System.nanoTime()/1000000;
	public double timeDifference;
	public double sleeperDistance = 65;
	private double currentSpeed;
	private double previousSpeed;
	private boolean standby = false;
	private int position = 0;
	
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
	public boolean odd = true;
	
	public Timer timer;

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
	int counter = 0;
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
		boolean test = false;
		
		boolean first = true;
		
		
		boolean startWriting = false;
		while(colorSensorThread==thisThread) {
			
			if(test){
				detectedColorId = colorSensor.getColorID();
				if(detectedColorId != prevDetectedColorId){
					if(detectedColorId == GREEN){
						try {
							writeToFile(""+counter);
							//writeToFile("train is set to standby, it is considered stopped");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
						//if(first){
							counter = 0;
							first = false;
						//}
						
					}
					else {
						
						if(detectedColorId == BROWN && prevDetectedColorId == BLACK){
							counter++;
							if(odd){
								//testCalculateSpeed();
								odd = false;
							}
							else {
								odd = true;
							}
						}
						
					}
					lastRegisteredTime = registeredTime;
					
//					try {
//						writeToFile("Id:"+detectedColorId);
//						//writeToFile("train is set to standby, it is considered stopped");
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						//e.printStackTrace();
//					}
				}
				prevDetectedColorId = detectedColorId;
				
				try {
					Thread.sleep(14); //the thread is put to sleep to avoid random detection of colors in the transition between two colors
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				detectedColorId = colorSensor.getColorID();
				
				while(standby) {
					checkIfReady();
					synchronized(this) { 
						try {
							/*try {
								//writeToFile("Number of sleepers: "+position);
								//writeToFile("train is set to standby, it is considered stopped");
							} //catch (IOException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
							//}*/
							this.wait(); //the color sensor thread sleeps while the pod is standby
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}	
					}
				}
				if(canSwitchZone){
					zoneSleeper(detectedColorId);
				}
				
				if(canSwitchTrack && !startTrack && detectedColorId != BROWN && detectedColorId != BLACK){
					trackSleeper(detectedColorId);
				}
				if(detectedColorId != prevDetectedColorId) {	
					// BLACK/GREY -> BROWN - Calculate speed
					if(detectedColorId == BROWN && prevDetectedColorId == BLACK){
						calculateSpeed();		
						if(detectedZoneTransitionSleeper){
							canSwitchZone = true;
							detectedZoneTransitionSleeper = false;
						}
						if(!startTrack && !canSwitchZone){
							canSwitchTrack = true;
						}
						
						// Initial. Do not send subtrack when entering the first zone controller
						if(startTrack && !canSwitchZone){
							startTrack = false;
						}
					}
					lastRegisteredTime = registeredTime;	
				}
				
				prevDetectedColorId = detectedColorId;
				
				if(isDestination && position >= 10){
					sendToBlock("DESTINATION", 0);
					isDestination = false;
				}
				//prevDetectedColorId = detectedColorId;
				/* IMPORTANT LOGIC FOR DETECTING SWITCH SLEEPERS
				if (detectedColorId != prevDetectedColorId)  {	
					if (detectedColorId != 7 && detectedColorId != 13 && detectedColorId != 6) {
						//System.out.println(colorToString(prevDetectedColorId)+" -> "+colorToString(detectedColorId));
						//System.out.println("DetectedId: "+detectedColorId+", "+colorToString(detectedColorId)+". Last color was: "+prevDetectedColorId+", "+colorToString(prevDetectedColorId));
						SleeperDetecter(detectedColorId, prevDetectedColorId);
					}
				}
				*/
				
				
				previousSpeed = currentSpeed;
				
				// TRAIN HAS STOPPED AT ONE COLOR 
				if(((System.nanoTime()/1000000)-lastRegisteredTime>500) && isStopping){
					/*try {
						//writeToFile("STOPPED AT ONE COLOR");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					isStopping = false;
					standby = true;
				}
				else if(((System.nanoTime()/1000000)-lastRegisteredTime>500) && !isStopping && !startTrack){
					// Train is stuck in a turn. Speed it up 
					//sendToBlock("SPEED_UP",30);
				}
				
				try {
					Thread.sleep(14); //the thread is put to sleep to avoid random detection of colors in the transition between two colors
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			

		} 
		
	}
	
	public boolean firstRun = true;
	
	class CheckTimer extends TimerTask {
		public void run() {
			if(!firstRun){
				sendToBlock("CHECK_IF_READY", ""+position);
			}
			else {
				firstRun = false;
			}
		}
	}

	
	private void checkIfReady() {
		// TODO Auto-generated method stub
		firstRun = true;
		System.out.println("CHECK IF READY TIMER STARTED");
		int timeinterval = 5 * 1000;
		timer = new Timer();
		timer.schedule(new CheckTimer(), 5000, timeinterval);
	}

	private void trackSleeper(int color) {
		canWritePositionAndSpeed = true;
		//registeredTime = System.nanoTime()/1000000;
		switch(color) {
			case 1: {
				// GREEN - Track transition to next zone
				System.out.println("GREEN TRANSITION TRACK");
				detectedZoneTransitionSleeper = true;
				canSwitchTrack = false;
				position = 0;
				if(firstTrackInZone){
					sendToBlock("UNSUBSCRIBE_FROM_PREV_ZONE");
					firstTrackInZone = false;
				}
				sendToBlock("SLEEPER_TRACK","GREEN");
				
				previousTrack = currentTrack;
				currentTrack = mapCurrentTrack(currentTrack, "GREEN");
				break;
			}
			case 2: {
				System.out.println("BLUE SUB TRACK ");
				canSwitchTrack = false;
				position = 0;
				if(firstTrackInZone){
					sendToBlock("UNSUBSCRIBE_FROM_PREV_ZONE");
					firstTrackInZone = false;
				}
				sendToBlock("SLEEPER_TRACK","BLUE");
				isDestination = true;
				
				previousTrack = currentTrack;
				currentTrack = mapCurrentTrack(currentTrack, "BLUE");
				break;
			}
			case 3: {
				System.out.println("YELLOW SUB TRACK");
				canSwitchTrack = false;
				position = 0;
				if(firstTrackInZone){
					sendToBlock("UNSUBSCRIBE_FROM_PREV_ZONE");
					firstTrackInZone = false;
				}
				sendToBlock("SLEEPER_TRACK","YELLOW");
				
				previousTrack = currentTrack;
				currentTrack = mapCurrentTrack(currentTrack, "YELLOW");
				break;
			}
			case 0: {
				System.out.println("RED SUB TRACK");
				canSwitchTrack = false;
				position = 0;
				if(firstTrackInZone){
					sendToBlock("UNSUBSCRIBE_FROM_PREV_ZONE");
					firstTrackInZone = false;
				}
				sendToBlock("SLEEPER_TRACK","RED");
				
				if(currentTrack == 0){
					currentTrack = 1;
					previousTrack = 22;
				}
				else {
					previousTrack = currentTrack;
					currentTrack = mapCurrentTrack(currentTrack, "RED");
				}
				break;
			}
			/*case 6: {
				System.out.println("WHITE SUB TRACK");
				canSwitchTrack = false;
				position = 0;
				if(firstTrackInZone){
					sendToBlock("UNSUBSCRIBE_FROM_PREV_ZONE");
					firstTrackInZone = false;
				}
				sendToBlock("SLEEPER_TRACK","WHITE");
				break;
			}*/
			default: {
				System.out.println("WHAT COLOR IS THIS? id: "+color);
				break;
			}
		}
	}

	private int mapCurrentTrack(int currentTrack, String color) {
		// TODO Auto-generated method stub
		switch(currentTrack){
		case 1:
			if(color.equals("YELLOW")){
				globalSleeperkNr = 89;
				return 3;
			}
			else if(color.equals("GREEN")){
				globalSleeperkNr = 47;
				return 2;
			}
			break;
		case 2:
			globalSleeperkNr = 517;
			return 18;
		case 3:
			if(color.equals("BLUE")){
				globalSleeperkNr = 128;
				return 5;
			}
			else if(color.equals("YELLOW")){
				globalSleeperkNr = 111;
				return 4;
			}
			break;
		case 4:
			globalSleeperkNr = 147;
			return 6;
		case 5:
			globalSleeperkNr = 147;
			return 6;
		case 6:
			if(color.equals("BLUE")){
				globalSleeperkNr = 238;
				return 7;
			}
			else if(color.equals("YELLOW")){
				globalSleeperkNr = 257;
				return 8;
			}
			break;
		case 7: 
			globalSleeperkNr = 274;
			return 9;
		case 8:
			globalSleeperkNr = 274;
			return 9;
		case 9:
			if(color.equals("YELLOW")){
				globalSleeperkNr = 286;
				return 10;
			}
			else if(color.equals("GREEN")){
				globalSleeperkNr = 303;
				return 11;
			}
			break;
		case 10:
			globalSleeperkNr = 672;
			return 23;
		case 11:
			if(color.equals("BLUE")){
				globalSleeperkNr = 401;
				return 15;
			}
			else if(color.equals("YELLOW")){
				globalSleeperkNr = 380;
				return 14;
			}
			break;
		case 12:
			globalSleeperkNr = 424;
			return 16;
		case 13:
			globalSleeperkNr = 424;
			return 16;
		case 14:
			globalSleeperkNr = 443;
			return 17;
		case 15:
			globalSleeperkNr = 443;
			return 17;
		case 16:
			globalSleeperkNr = 517;
			return 18;
		case 17: 
			globalSleeperkNr = 545;
			return 19;
		case 18:
			globalSleeperkNr = 545;
			return 19;
		case 19:
			if(color.equals("BLUE")){
				globalSleeperkNr = 590;
				return 21;
			}
			else if(color.equals("YELLOW")){
				globalSleeperkNr = 573;
				return 20;
			}
			break;
		case 20:
			globalSleeperkNr = 609;
			return 22;
		case 21:
			globalSleeperkNr = 609;
			return 22;
		case 22:
			globalSleeperkNr = 0;
			return 1;
		case 23: 
			if(color.equals("BLUE")){
				globalSleeperkNr = 361;
				return 13;
			}
			else if(color.equals("YELLOW")){
				globalSleeperkNr = 344;
				return 12;
			}
			break;
		}
		return 1;
	}

	// Detects Zone controller
	private void zoneSleeper(int color) {
		if(color != BROWN && color != BLACK){
			if(currentTrack == 2 || currentTrack == 16 || currentTrack == 17){
				if(currentZone != 4){
					previousZone = currentZone;
					currentZone = 4;
					canSwitchZone = false;
					detectedZoneTransitionSleeper = false;
					firstTrackInZone = true;
					sendToBlock("NEWZONE", "zonecontroller_"+currentZone);
				}
			}
			else if(currentTrack == 22 || currentTrack == 0){
				if(currentZone != 1){
					previousZone = currentZone;
					currentZone = 1;
					canSwitchZone = false;
					detectedZoneTransitionSleeper = false;
					firstTrackInZone = true;
					sendToBlock("NEWZONE", "zonecontroller_"+currentZone);
				}
			}
			else if(currentTrack == 6){
				if(currentZone != 2){
					previousZone = currentZone;
					currentZone = 2;
					canSwitchZone = false;
					detectedZoneTransitionSleeper = false;
					firstTrackInZone = true;
					sendToBlock("NEWZONE", "zonecontroller_"+currentZone);		
				}
			}
			else if(currentTrack == 23 || currentTrack == 11){
				if(currentZone != 3){
					previousZone = currentZone;
					currentZone = 3;
					canSwitchZone = false;
					detectedZoneTransitionSleeper = false;
					firstTrackInZone = true;
					sendToBlock("NEWZONE", "zonecontroller_"+currentZone);
				}
			}
		}
		
		/*
		switch(color){
			case 3:
				// YELLOW Zone 1
				if(currentZone != 1){
					previousZone = currentZone;
					currentZone = 1;
					canSwitchZone = false;
					detectedZoneTransitionSleeper = false;
					firstTrackInZone = true;
					sendToBlock("NEWZONE", "zonecontroller_"+currentZone);
				}
				break;
			case 1:
				// GREEN Zone 2
				if(currentZone != 2){
					previousZone = currentZone;
					currentZone = 2;
					canSwitchZone = false;
					detectedZoneTransitionSleeper = false;
					firstTrackInZone = true;
					sendToBlock("NEWZONE", "zonecontroller_"+currentZone);
				}
				break;
			case 2:
				// BLUE Zone 3
				if(currentZone != 3){
					previousZone = currentZone;
					currentZone = 3;
					canSwitchZone = false;
					detectedZoneTransitionSleeper = false;
					firstTrackInZone = true;
					sendToBlock("NEWZONE", "zonecontroller_"+currentZone);
				}
				break;
			case 0:
				// RED Zone 4
				if(currentZone != 4){
					previousZone = currentZone;
					currentZone = 4;
					canSwitchZone = false;
					detectedZoneTransitionSleeper = false;
					firstTrackInZone = true;
					sendToBlock("NEWZONE", "zonecontroller_"+currentZone);
				}
				break;
			default:
				break;
		}*/
	}

	private void testCalculateSpeed() {
		sleeperDistance = 65;
		registeredTime = System.nanoTime()/1000000;
		timeDifference = Double.parseDouble(Long.toString(registeredTime - lastRegisteredTime));
		double calculatedSpeed = sleeperDistance/timeDifference;
		currentSpeed = calculatedSpeed;
		try{
			writeToFile(counter+" "+currentSpeed);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int timepoint = 1;
	class PrintTimer extends TimerTask {
	    public void run() {
	    	if(!startTrack){
	    		try{
	    			writeToFile(timepoint+","+printPosition);
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	timepoint++;
	       
	    }
	 }
	
	public void startPrintTimer () {
		int timeinterval = 1000;
		Timer printTimer = new Timer();
		printTimer.schedule(new PrintTimer(), 1000, timeinterval);
		
	}
	private void calculateSpeed() {
		// TODO Auto-generated method stub
		if(odd){
			registeredTime = System.nanoTime()/1000000;
			timeDifference = Double.parseDouble(Long.toString(registeredTime - lastRegisteredTime));
			double calculatedSpeed = sleeperDistance/timeDifference;
			currentSpeed = calculatedSpeed;
			odd = false;
		}
		else {
			odd = true;
		}
				
		
		
		if(currentSpeed > (previousSpeed+0.05) && isStopping){
			/*try {
				writeToFile("STOPPED AT A COLOR TRANSITION");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			// THIS HAPPENS WHEN THE TRAIN STOPS AT A COLOR TRANSITION. IT WILL THEN DETECT ALOT OF GREY/BROWN AND SPEED VARIABLE IS WRONG
			isStopping = false;
			standby = true;
			
			
			//else if(currentSpeed > (previousSpeed+0.05) && !isStopping && !startTrack){
			// Train is stuck in a turn. Speed it up 
			//sendToBlock("SPEED_UP",30);
		//}
		}
		else {
			position++;
			if(currentZone != 0 && canWritePositionAndSpeed){
				//try {
					currentGlobalSleeperNr = globalSleeperkNr + position;
					if(isValidSleeper(currentTrack, currentGlobalSleeperNr)){
						if(position < 10){
							// Get previous track
							String string = "";
							for (int i = (10-position); i > 0; i--) {
								string += getPreviousTrackSleeper(i)+",";
							}
							for (int i = 1; i < position; i++) {
								string += globalSleeperkNr +i+",";
							}
							printPosition = string+currentGlobalSleeperNr;
							//writeToFile(new Date().getTime() + ","+string+currentGlobalSleeperNr);
						}
						else {
							printPosition = (currentGlobalSleeperNr-9)+","+(currentGlobalSleeperNr-8)+","+(currentGlobalSleeperNr-7)+","+(currentGlobalSleeperNr-6)+","+(currentGlobalSleeperNr-5)+","+(currentGlobalSleeperNr-4)+","+(currentGlobalSleeperNr-3)+","+(currentGlobalSleeperNr-2)+","+(currentGlobalSleeperNr-1)+","+currentGlobalSleeperNr;
							//writeToFile(new Date().getTime() + ","+(currentGlobalSleeperNr-9)+","+(currentGlobalSleeperNr-8)+","+(currentGlobalSleeperNr-7)+","+(currentGlobalSleeperNr-6)+","+(currentGlobalSleeperNr-5)+","+(currentGlobalSleeperNr-4)+","+(currentGlobalSleeperNr-3)+","+(currentGlobalSleeperNr-2)+","+(currentGlobalSleeperNr-1)+","+currentGlobalSleeperNr);
						}
					}
					else {
						//writeToFile("!NOT VALID! "+ new Date().getTime() + ", Position: "+position);
					}
				//} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				//}
				sendToBlock("SPEED_POSITION",position+";"+currentSpeed);
			}
			//sendToBlock("SPEEDANDPOS",position+);
			/*if(true){
				try {
					
					//writeToFile(calculatedSpeed+"");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}*/
		}
		
	}

	private int getPreviousTrackSleeper(int position) {
		// TODO Auto-generated method stub
		switch(previousTrack){
		case 1: return 48 - position;
		case 2: return 90 - position;
		case 3: return 112 - position;
		case 4: return 129 - position;
		case 5: return 148 - position;
		case 6: return 239 - position;
		case 7: return 258 - position;
		case 8: return 275 - position;
		case 9: return 287 - position;
		case 10: return 304 - position;
		case 11: return 345 - position;
		case 12: return 362 - position;
		case 13: return 381 - position;
		case 14: return 402 - position;
		case 15: return 425 - position;
		case 16: return 444 - position;
		case 17: return 518 - position;
		case 18: return 546 - position;
		case 19: return 574 - position;
		case 20: return 591 - position;
		case 21: return 610 - position;
		case 22: return 673 - position;
		case 23: return 706 - position;
		}
		return 0;
	}

	private boolean isValidSleeper(int currentTrack, int currentGlobalSleeperNr) {
		// TODO Auto-generated method stub
		switch (currentTrack) {
		case 1: 
			if (currentGlobalSleeperNr > 47){ 
				return false;
			}
			else {
				return true;
			}
		case 2: 
			if (currentGlobalSleeperNr > 89){ 
				return false;
			}
			else {
				return true;
			}
		case 3:
			if (currentGlobalSleeperNr > 111){ 
				return false;
			}
			else {
				return true;
			}
		case 4: 
			if (currentGlobalSleeperNr > 128){ 
				return false;
			}
			else {
				return true;
			}
		case 5:
			if (currentGlobalSleeperNr > 147){ 
				return false;
			}
			else {
				return true;
			}
		case 6: 
			if (currentGlobalSleeperNr > 238){ 
				return false;
			}
			else {
				return true;
			}
		case 7: 
			if (currentGlobalSleeperNr > 257){ 
				return false;
			}
			else {
				return true;
			}
		case 8:
			if (currentGlobalSleeperNr > 274){ 
				return false;
			}
			else {
				return true;
			}
		case 9:
			if (currentGlobalSleeperNr > 286){ 
				return false;
			}
			else {
				return true;
			}
		case 10:
			if (currentGlobalSleeperNr > 303){ 
				return false;
			}
			else {
				return true;
			}
		case 11: 
			if (currentGlobalSleeperNr > 344){ 
				return false;
			}
			else {
				return true;
			}
		case 12: 
			if (currentGlobalSleeperNr > 361){ 
				return false;
			}
			else {
				return true;
			}
		case 13: 
			if (currentGlobalSleeperNr > 380){ 
				return false;
			}
			else {
				return true;
			}
		case 14: 
			if (currentGlobalSleeperNr > 401){ 
				return false;
			}
			else {
				return true;
			}
		case 15: 
			if (currentGlobalSleeperNr > 424){ 
				return false;
			}
			else {
				return true;
			}
		case 16:
			if (currentGlobalSleeperNr > 443){ 
				return false;
			}
			else {
				return true;
			}
		case 17: 
			if (currentGlobalSleeperNr > 517){ 
				return false;
			}
			else {
				return true;
			}
		case 18: 
			if (currentGlobalSleeperNr > 545){ 
				return false;
			}
			else {
				return true;
			}
		case 19:
			if (currentGlobalSleeperNr > 573){ 
				return false;
			}
			else {
				return true;
			}
		case 20: 
			if (currentGlobalSleeperNr > 590){ 
				return false;
			}
			else {
				return true;
			}
		case 21: 
			if (currentGlobalSleeperNr > 609){ 
				return false;
			}
			else {
				return true;
			}
		case 22: 
			if (currentGlobalSleeperNr > 672){ 
				return false;
			}
			else {
				return true;
			}
		case 23: 
			if (currentGlobalSleeperNr > 705){ 
				return false;
			}
			else {
				return true;
			}
		default:
			return false;
		}
	}

	// Write to file for statistics
	public static void writeToFile(String input) throws IOException{
	    writer.write(input+"\n");
	}
	
	//This method finds the correct zone controller and switch based on the detected color combination and then sends a request 
	/*public void SleeperDetecter(int detectedColor, int prevDetectedColor) {
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
	}*/

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
		isStopping = true;
	}

	public synchronized void wakeUpThread() {
		System.out.println("\n--------------------------------------\n\n  ->       WAKING UP      <-\n\n--------------------------------------\n\n");
		isStopping = false;
		try{
			timer.cancel();
		}
		catch(Exception e){
			System.out.println("Something went wrong with timer.cancel();");
		}
		standby = false;
		notify();
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
	
	*/
}
