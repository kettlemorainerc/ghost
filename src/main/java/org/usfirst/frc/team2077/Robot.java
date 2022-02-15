/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2019 FIRST. All Rights Reserved.                        */
/* Copyright (c) 2020 FRC Team 2077. All Rights Reserved.                     */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.usfirst.frc.team2077;

import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Subsystem;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import org.usfirst.frc.team2077.commands.AutonomousCheck;
import org.usfirst.frc.team2077.commands.NewLauncher;
import org.usfirst.frc.team2077.drivetrain.*;
import org.usfirst.frc.team2077.drivetrain.MecanumMath.VelocityDirection;
import org.usfirst.frc.team2077.sensors.*;
import org.usfirst.frc.team2077.subsystems.*;

import java.util.EnumMap;

import static org.usfirst.frc.team2077.drivetrain.MecanumMath.VelocityDirection.*;

public class Robot extends TimedRobot {

	// Everything "global" hangs off the single instance of Robot,
	// either directly or under one of the above public members.
	public static Robot robot_ = null;


	// Other globally accessible objects...
	// Using a constructed constants object instead of statics.
	// Put globally accessible system constants in the Constants class.
	// Other code can access them through Robot.robot_.constants_.<FIELD_NAME>.
	public Constants constants_ = new Constants();
	// Drive station controls.
	public DriveStation driveStation_;
	// Inter-process data exchange.
	public NetworkTableInstance networkTableInstance_;
	// Sensors.
	public AngleSensor angleSensor_;
	public AnalogSettings analogSettings_;
	// Drive train, including:
	//   Controller/motor/wheel/encoder units for each wheel.
	//   Logic for applying robot level functionality to individual wheels.
	public AbstractChassis chassis_;
	// Subsystems
	//    The position/heading subsystems operate as flags to allow control
	//    of chassis rotation to move between commands independently of positioning.
	public Subsystem position_;
	public Subsystem heading_;
//	public Subsystem target_;
	//    Aiming system for elevating ball launcher and pointing the robot. Displayed on DS video.
//	public Crosshairs crosshairs_;
	//    Ball launcher with ajustable elevation and speed based on range to target.
//	public LauncherIF launcher_;
	public SimpleDriveSubsys simpleDriveSubsys_;
	public LauncherIF launcher_;


	//public TestLauncher tLauncher_; // Bringing back support for the TestLauncher Class though the old instance name
	// public Telemetry telemetry_;
	public Launcher testLauncher_; // low-level control for testing
	public NewLauncher newLauncher_; // low-level control for testing
	// Default commands
	//    Autonomous selected via drive station dashboard.
	protected Command autonomous_;
	//    Default teleop robot drive.
	protected Command drive_;
	//    Continuous update of target range and direction based on robot motion.
//	protected Command track_;
	//    Operator input of target position relative to robot using the stick.
//	protected Command aim_;
	//    Continuous update of launcher elevation for target range.
//	protected Command range_;

	// This class will be instantiated exactly once, via frc.robot.Main.
	// The constructor initializes the globally accessible static instance,
	// all other initialization happens in robotInit().
	public Robot() {
		robot_ = this;
	}

	/**
	 * Run once on startup.
	 */
	@Override
	public void robotInit() {
		SmartDashboard.putBoolean("Run Autonomous", false);
		networkTableInstance_ = NetworkTableInstance.getDefault();
		angleSensor_ = new AngleSensor();

		//analogSettings_ = new AnalogSettings(1, 2, 3);


		setupDriveTrain();
		chassis_.setPosition(-180, 0, 0); // TODO: Initialize from Smart Dashboard
//		EnumMap<VelocityDirection, Double> p = robot_.chassis_.getPosition();
//		robot_.crosshairs_.set(Math.atan2(-p.get(EAST), -p.get(NORTH)),
//							   Math.sqrt(p.get(NORTH) * p.get(NORTH) + p.get(EAST) * p.get(EAST)));
//
//		System.out.println("CROSSHAIRS:" + crosshairs_);
		setupController();
	}

	public void setupDriveTrain() {
		chassis_ = new MecanumChassis();

		//   These dummy subsystems support separate command ownership of robot motion and rotation.
		position_ = new SubsystemBase() {
		};
		heading_ = new SubsystemBase() {
		};
		// telemetry_ = new Telemetry();

		launcher_ = new Launcher();
		testLauncher_ = launcher_ instanceof Launcher ? (Launcher) launcher_ : null;

		newLauncher_ = new NewLauncher();

//		simpleDriveSubsys_ = new SimpleDriveSubsys();
//		testLauncher_ = launcher_ instanceof Launcher ? (Launcher) launcher_ : null;

//		crosshairs_ = new Crosshairs();
	}

	public void setupController() {
		// Container for remote control software objects.
		driveStation_ = new DriveStation(position_, chassis_);
	}

	/**
	 * Called every robot packet (generally about 50x/second) no matter the mode.
	 * Use this for items like diagnostics that you want run during disabled,
	 * autonomous, teleoperated and test.
	 * <p>
	 * This runs after the mode specific periodic functions, but before
	 * LiveWindow and SmartDashboard integrated updating.
	 */
	@Override
	public void robotPeriodic() {
		// Runs the Scheduler.  This is responsible for polling buttons, adding newly-scheduled
		// commands, running already-scheduled commands, removing finished or interrupted commands,
		// and running subsystem periodic() methods.  This must be called from the robot's periodic
		// block in order for anything in the Command-based framework to work.
		CommandScheduler.getInstance()
						.run();

//		chassis_.driveModule_.values().forEach(module -> {
//			SmartDashboard.putNumber(module.getWheelPosition() + " RPM", ((SparkNeoDriveModule) module).getRPM());
//		});

//		SmartDashboard.putNumber("range to target", robot_.crosshairs_.getRange());
	}

	// The robot and the drive station exchange data packets around 50x/second so long
	// as they are connected and the robot program is running (hasn't crashed or exited).
	// This packet excahange is what keeps the DS related software objects, i.e. Joysticks,
	// in the robot code updated with their position, etc on the the actual DS, and what
	// keeps "Robot Code" indicator on the DS green.
	//
	// Each time a DS packet is received, the underlying WPILIB code calls one or more
	// xxxPeriodic() methods in this class, first a mode-specific one and then robotPeriodic().
	//
	// Each time the robot mode (disabled, autonomous, teleop, test) changes the appropriate
	// xxxInit() method is called. The robotInit() method is called only once at startup.

	/**
	 * Called once each time the robot enters disabled mode.
	 * Note that in competition the robot may (or may not?) be
	 * disabled briefly between autonomous and teleop.
	 */
	@Override
	public void disabledInit() {
	}

	/**
	 * Called periodically while robot is disabled.
	 */
	@Override
	public void disabledPeriodic() {
	}

	/**
	 * Called once each time the robot enters autonomous mode.
	 */
	@Override
	public void autonomousInit() {


		 if (autonomous_ != null) {
		   autonomous_.schedule();
		 }
//		if(driveStation_ != null) {
//			driveStation_.cancel();
//		}
		autonomous_ = new AutonomousCheck();
		autonomous_.schedule();
	}

	/**
	 * Called periodically during autonomous.
	 */
	@Override
	public void autonomousPeriodic() {
	}

	/**
	 * Called once each time the robot enters teleop (operator controlled) mode.
	 */
	@Override
	public void teleopInit() {

		// This makes sure that the autonomous stops running when
		// teleop starts running. If you want the autonomous to
		// continue until interrupted by another command, remove
		// this line or comment it out.
		 if (autonomous_ != null) {
		 	autonomous_.cancel();
		 }
	}

	private static void printWheelInfo(SparkNeoDriveModule.DrivePosition position) {
		System.out.printf(
			"[%s set RPM: %s][%s reported RPM %s]",
			position,
			((SparkNeoDriveModule) robot_.chassis_.driveModule_.get(position.WHEEL_POSITION)).getSetPoint(),
			position,
			((SparkNeoDriveModule) robot_.chassis_.driveModule_.get(position.WHEEL_POSITION)).getRPM()
		);
	}


	private long debug = 0;
	/**
	 * Called periodically during teleop.
	 */
	@Override
	public void teleopPeriodic() {
	}

	/**
	 * Called once each time the robot enters test mode.
	 */
	@Override
	public void testInit() {
		// Cancels all running commands at the start of test mode.
		CommandScheduler.getInstance()
						.cancelAll();

	}

	/**
	 * Called periodically during test.
	 */
	@Override
	public void testPeriodic() {

	}
}
