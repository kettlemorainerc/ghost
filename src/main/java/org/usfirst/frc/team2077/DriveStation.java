/*----------------------------------------------------------------------------*/
/* Copyright (c) 2020 FRC Team 2077. All Rights Reserved.                     */
/* Open Source Software - may be modified and shared by FRC teams.            */
/*----------------------------------------------------------------------------*/

package org.usfirst.frc.team2077;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj2.command.*;
import edu.wpi.first.wpilibj2.command.button.*;
import org.usfirst.frc.team2077.commands.*;

public class DriveStation {
    private final DriveJoystick driveStick;
    private final Joystick technicalStick;

    public DriveStation(RobotHardware hardware) {
//        driveStick = getFlysky();
        driveStick = getJoystick();

        technicalStick = getTechnicalJoystick();
//        technicalStick = getNumpad();

        bind(hardware);
    }

    public void bind(RobotHardware hardware) {
        hardware.position.setDefaultCommand(new CardinalMovement(hardware, driveStick));
        hardware.heading.setDefaultCommand(new RotationMovement(hardware, driveStick));

        bindDriverControl(hardware, driveStick);
        bindTechnicalControl(hardware, technicalStick);
    }

    private static void bindDriverControl(RobotHardware hardware, Joystick primary) {}

    private void bindTechnicalControl(RobotHardware hardware, Joystick secondary) {
        useCommand(secondary, 1, new PrimeAndShoot(hardware));

        useCommand(secondary, 2, new Obtainer(hardware, false));
        useCommand(secondary, 3, new Obtainer(hardware, true));

//        useCommand(secondary, 4, new AlignToShadow());
    }

    /** Normal (brighter/silver) joystick that supports rotation */
    private static DriveJoystick getJoystick() {
        return new DriveJoystick(0).setSensitivity(.2, 2.5);
    }

    /** Flysky Drone Controller */
    private static DriveJoystick getFlysky() {
        return new DriveJoystick(2, 4).setDriveSensitivity(.3, 1)
                                   .setRotationSensitivity(.05, 1);
    }

    /** Currently the darker joystick that doesn't support rotation */
    private static Joystick getTechnicalJoystick() {
        return new Joystick(1);
    }

    private static Joystick getNumpad() {
        return new Joystick(5);
    }

    /** bind command to the given joystick button */
    public static void useCommand(Joystick joystick, int button, BindableCommand command) {
        command.bind(new JoystickButton(joystick, button));
    }
}
