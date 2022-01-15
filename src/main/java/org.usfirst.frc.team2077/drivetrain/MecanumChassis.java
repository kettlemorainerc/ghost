/*----------------------------------------------------------------------------*/
/* Copyright (c) 2020 FRC Team 2077. All Rights Reserved.                     */
/* Open Source Software - may be modified and shared by FRC teams.            */
/*----------------------------------------------------------------------------*/

package org.usfirst.frc.team2077.drivetrain;

import org.usfirst.frc.team2077.*;
import org.usfirst.frc.team2077.drivetrain.MecanumMath.*;
import org.usfirst.frc.team2077.drivetrain.SparkNeoDriveModule.DrivePosition;
import org.usfirst.frc.team2077.math.AccelerationLimits;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.function.*;

import static java.util.stream.Collectors.*;
import static org.usfirst.frc.team2077.Robot.*;
import static org.usfirst.frc.team2077.drivetrain.MecanumMath.VelocityDirection.*;
import static org.usfirst.frc.team2077.math.AccelerationLimits.Type.*;

public class MecanumChassis extends AbstractChassis {
	private static final double WHEELBASE = 20.375; // inches
	private static final double TRACK_WIDTH = 22.625; // inches
	private static final double WHEEL_RADIUS = 4.0; // inches
	private static final double EAST_ADJUSTMENT = .65;

	private final MecanumMath mecanumMath_;

	private static EnumMap<WheelPosition, DriveModuleIF> buildDriveModule() {
		EnumMap<WheelPosition, DriveModuleIF> driveModule = new EnumMap<>(WheelPosition.class);

		for(WheelPosition pos: WheelPosition.values()) {
			driveModule.put(pos, new SparkNeoDriveModule(DrivePosition.forWheelPosition(pos)));
		}

		return driveModule;
//		return new DriveModuleIF[]{
//			new SparkNeoDriveModule(SparkNeoDriveModule.DrivePosition.FRONT_RIGHT),  // northeast (right front)
//			new SparkNeoDriveModule(SparkNeoDriveModule.DrivePosition.BACK_RIGHT),  // southeast (right rear)
//			new SparkNeoDriveModule(SparkNeoDriveModule.DrivePosition.BACK_LEFT),  // southwest (left rear)
//			new SparkNeoDriveModule(SparkNeoDriveModule.DrivePosition.FRONT_LEFT)   // northwest (left front)
//		};
	}

	public MecanumChassis() {
		this(buildDriveModule(), Clock::getSeconds);
	}

	MecanumChassis(EnumMap<WheelPosition, DriveModuleIF> driveModule, Supplier<Double> getSeconds) {
		super(driveModule, WHEELBASE, TRACK_WIDTH, WHEEL_RADIUS, getSeconds);

		mecanumMath_ = new MecanumMath(wheelbase_, trackWidth_, wheelRadius_, wheelRadius_, 1, 180 / Math.PI);

		// north/south speed conversion from 0-1 range to DriveModule maximum (inches/second)
		maximumSpeed_ = driveModule_.values()
									.stream()
									.map(DriveModuleIF::getMaximumSpeed)
									.min(Comparator.naturalOrder())
									.orElseThrow();
//		Math.min(
//			Math.min(driveModule_[0].getMaximumSpeed(), driveModule_[1].getMaximumSpeed()),
//			Math.min(driveModule_[2].getMaximumSpeed(), driveModule_[3].getMaximumSpeed())
//		);
		// rotation speed conversion from 0-1 range to DriveModule maximum (degrees/second)
		maximumRotation_ = mecanumMath_.forward(MecanumMath.mapOf(
			WheelPosition.class,
			-maximumSpeed_,
			-maximumSpeed_,
			maximumSpeed_,
			maximumSpeed_
		)).get(ROTATION);

		// lowest chassis speeds supportable by the drive modules
		minimumSpeed_ = maximumSpeed_ * .1; // TODO: Test and configure.
		minimumRotation_ = maximumRotation_ * .1;

		System.out.println(getClass().getName() +
						   "MAXIMUM SPEED:" +
						   Math.round(maximumSpeed_ * 10.) / 10. +
						   " IN/SEC MAXIMUM ROTATION:" +
						   Math.round(maximumRotation_ * 10.) / 10. +
						   " DEG/SEC");
		System.out.println(getClass().getName() +
						   "MINIMUM SPEED:" +
						   Math.round(minimumSpeed_ * 10.) / 10. +
						   " IN/SEC MINIMUM ROTATION:" +
						   Math.round(minimumRotation_ * 10.) / 10. +
						   " DEG/SEC");
		AccelerationLimits a = getAccelerationLimits();
		System.out.println(getClass().getName() + "ACCELERATION:"
						   + Math.round(a.get(NORTH, ACCELERATION) * 10.) / 10. + "/" + Math.round(a.get(NORTH, DECELERATION) * 10.) / 10. + "/"
						   + Math.round(a.get(EAST, ACCELERATION) * 10.) / 10. + "/" + Math.round(a.get(EAST, DECELERATION) * 10.) / 10. + "/"
						   + Math.round(a.get(ROTATION, ACCELERATION) * 10.) / 10. + "/" + Math.round(a.get(ROTATION, DECELERATION) * 10.) / 10.);
	}

	@Override
	public void setVelocity(double north, double east, double clockwise, AccelerationLimits accelerationLimits) {
		setVelocity(north, east, accelerationLimits);
		setRotation(clockwise, accelerationLimits);
	}

	@Override
	public void setVelocity(double north, double east, AccelerationLimits accelerationLimits) {
		//		northSet_ = north;
		targetVelocity.put(NORTH, north);
		//		northAccelerationLimit_ = accelerationLimits[0];
		this.accelerationLimits.set(NORTH, accelerationLimits.get(NORTH));
//		northAccelerationLimit_ = accelerationLimits.get(NORTH);

		//		eastSet_ = east;
		targetVelocity.put(EAST, east);
//		eastAccelerationLimit_ = accelerationLimits[1];
		this.accelerationLimits.set(EAST, accelerationLimits.get(EAST));
//		eastAccelerationLimit_ = accelerationLimits.get(EAST);
	}

	@Override
	public void setRotation(double clockwise, AccelerationLimits accelerationLimits) {
//		clockwiseSet_ = clockwise;
		targetVelocity.put(ROTATION, clockwise);
		this.accelerationLimits.set(ROTATION, accelerationLimits.get(ROTATION));
//		rotationAccelerationLimit_ = accelerationLimits.get(ROTATION);
//		rotationAccelerationLimit_ = accelerationLimits[2];
	}

	@Override
	protected void updatePosition() {
//		System.out.println("Updating Position of: " + this);
		// chassis velocity from internal set point
		velocitySet_ = getVelocityCalculated();
		// chassis velocity from motor/wheel measurements

		EnumMap<WheelPosition, Double> wheelVelocities = driveModule_.entrySet()
																	 .stream()
																	 .map(e -> new SimpleEntry<>(e.getKey(), e.getValue().getVelocity()))
																	 .collect(toMap(
																	 	Entry::getKey,
																		Entry::getValue,
																		Math::min,
																		() -> new EnumMap<>(WheelPosition.class)
																	 ));
		velocityMeasured_ = mecanumMath_.forward(wheelVelocities);

		// TODO: E/W velocities are consistently lower than those calculated from wheel speeds.
		// TODO: Measure actual vs measured E/W distances and insert an adjustment factor here.
		// TODO: Put the adjustment factor in constants.
		velocitySet_.compute(EAST, (k, v) -> v * EAST_ADJUSTMENT); // just a guess
		velocityMeasured_.compute(EAST, (k, v) -> v * EAST_ADJUSTMENT); // just a guess

		// update position with motion since last update
		positionSet_.moveRelative(
			velocitySet_.get(NORTH) * timeSinceLastUpdate_,
			velocitySet_.get(EAST) * timeSinceLastUpdate_,
			velocitySet_.get(ROTATION) * timeSinceLastUpdate_
		);
		positionMeasured_.moveRelative(
			velocityMeasured_.get(NORTH) * timeSinceLastUpdate_,
			velocityMeasured_.get(EAST) * timeSinceLastUpdate_,
			velocityMeasured_.get(ROTATION) * timeSinceLastUpdate_
		);
		if(robot_.angleSensor_ != null) { // TODO: Confirm AngleSensor is actually reading. Handle bench testing.
			double[] pS = positionSet_.get();
			double[] pM = positionMeasured_.get();
			pS[2] = pM[2] = robot_.angleSensor_.getAngle(); // TODO: conditional on gyro availability
			positionSet_.set(pS[0], pS[1], pS[2]);
			positionMeasured_.set(pM[0], pM[1], pM[2]);
		}
	}

	@Override
	protected void updateDriveModules() {
//		System.out.println("Updating drive modules: " + this);
		EnumMap<VelocityDirection, Double> botVelocity = getVelocityCalculated();

		// compute motor speeds
		EnumMap<WheelPosition, Double> wheelSpeed = mecanumMath_.inverse(botVelocity);

		// scale all motors proportionally if any are out of range
		double max = wheelSpeed.values()
		                       .stream()
					  .map(vel -> Math.abs(vel) / maximumSpeed_)
					  .max(Comparator.naturalOrder())
		              .map(val -> Math.max(1, val))
					  .orElseThrow();

		for (WheelPosition position : WheelPosition.values()) {
			driveModule_.get(position).setVelocity(
				wheelSpeed.get(position) / max
			);
		}
	}

	@Override
	public String toString() {
//		return String.format(
//			"MecanumChassis\n\t" +
//			"Velocity: %s\n\t" +
//			"Set Velocity: %s\n\t" +
//			"MeasuredVelocity: %s\n\t" +
//			"Drive Modules: %s\n\t" +
//			"Set Position: %s\n\t" +
//			"Measured Position: %s\n",
//			velocity,
//			velocitySet_,
//			velocityMeasured_,
//			driveModule_,
//			positionSet_,
//			positionMeasured_
//		);
		return "V:" +
			   Math.round(velocity.get(NORTH) * 10.) / 10. +
			   "/" +
			   Math.round(velocity.get(EAST) * 10.) / 10. +
			   "/" +
			   Math.round(velocity.get(ROTATION) * 10.) / 10.
			   +
			   " W:" +
			   driveModule_;
	}
}