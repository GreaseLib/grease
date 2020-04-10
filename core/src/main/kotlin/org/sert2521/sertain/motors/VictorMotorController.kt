package org.sert2521.sertain.motors

import com.ctre.phoenix.motorcontrol.ControlMode as PhoenixControlMode
import com.ctre.phoenix.motorcontrol.can.VictorSPX

class VictorMotorController(
        id: VictorId,
        vararg followerIds: MotorId,
        configure: MotorController.() -> Unit = {}
) : PhoenixMotorController() {
    override val baseController = VictorSPX(id.number)

    private var master: MotorController? = null
        set(value) {
            if (value is PhoenixMotorController) {
                baseController.follow(value.baseController)
            }
            field = value
        }

    private val followers: MutableMap<MotorId, MotorController> = with(mutableMapOf<MotorId, MotorController>()) {
        followerIds.forEach {
            set(it, motorController(it).apply {
                master = this
            })
        }
        toMutableMap()
    }

    override fun eachMotor(configure: MotorController.() -> Unit) {
        apply(configure)
        eachFollower(configure)
    }

    override fun eachFollower(configure: MotorController.() -> Unit) {
        followers.forEach {
            it.value.apply(configure)
        }
    }

    override val controlMode: ControlMode
        get() = when (baseController.controlMode) {
            PhoenixControlMode.PercentOutput -> ControlMode.PERCENT_OUTPUT
            PhoenixControlMode.Position -> ControlMode.POSITION
            PhoenixControlMode.Velocity -> ControlMode.VELOCITY
            PhoenixControlMode.Current -> ControlMode.CURRENT
            PhoenixControlMode.Disabled -> ControlMode.DISABLED
            else -> throw IllegalStateException("Invalid control mode.")
        }
    override var brakeMode: Boolean = false
        set(value) {
            eachFollower {
                brakeMode = value
            }
            baseController.setNeutralMode(ctreNeutralMode(value))
            field = value
        }
    override var pidfSlot: Int = 0
        set(value) {
            baseController.selectProfileSlot(value, 0)
            field = value
        }

    override var inverted: Boolean
        get() = baseController.inverted
        set(value) {
            baseController.inverted = value
            eachFollower {
                inverted = value
            }
        }
    override var sensorInverted: Boolean = false
        set(value) {
            baseController.setSensorPhase(value)
            field = value
        }

    override var openLoopRamp: Double = 0.0
        set(value) {
            baseController.configOpenloopRamp(value, 20)
        }

    override var closedLoopRamp: Double = 0.0
        set(value) {
            baseController.configClosedloopRamp(value, 20)
        }

    override var minOutputRange: ClosedRange<Double> = 0.0..0.0
        set(value) {
            baseController.configNominalOutputForward(value.endInclusive, 20)
            baseController.configNominalOutputReverse(value.endInclusive, 20)
            field = value
        }

    override var maxOutputRange: ClosedRange<Double> = -1.0..1.0
        set(value) {
            baseController.configPeakOutputForward(value.endInclusive, 20)
            baseController.configPeakOutputReverse(value.start, 20)
            field = value
        }

    override val percentOutput: Double
        get() = baseController.motorOutputPercent

    override var position: Int
        get() = baseController.getSelectedSensorPosition(0)
        set(value) {
            baseController.selectedSensorPosition = value
        }

    override val velocity: Int
        get() = baseController.getSelectedSensorVelocity(0)

    override fun setPercentOutput(output: Double) {
        baseController.set(PhoenixControlMode.PercentOutput, output)
    }

    override fun setTargetPosition(position: Int) {
        baseController.set(PhoenixControlMode.Position, position.toDouble())
    }

    override fun setTargetVelocity(velocity: Int) {
        baseController.set(PhoenixControlMode.Velocity, velocity.toDouble())
    }

    override fun setCurrent(current: Double) {
        baseController.set(PhoenixControlMode.Current, current)
    }

    override fun disable() {
        baseController.neutralOutput()
    }

    override fun updatePidf(slot: Int, pidf: MotorPidf) {
        with(pidf) {
            baseController.apply {
                config_kP(slot, kp)
                config_kI(slot, ki)
                config_kD(slot, kd)
                config_kF(slot, kf)
                config_IntegralZone(slot, integralZone)
                configAllowableClosedloopError(slot, allowedError)
                configMaxIntegralAccumulator(slot, maxIntegral)
                configClosedLoopPeakOutput(slot, maxOutput)
                configClosedLoopPeriod(slot, period)
            }
        }
    }

    init {
        eachMotor { baseController.setNeutralMode(ctreNeutralMode(brakeMode)) }
        baseController.apply {
            configClosedloopRamp(closedLoopRamp)
            configOpenloopRamp(openLoopRamp)
            configNominalOutputReverse(minOutputRange.start)
            configNominalOutputForward(minOutputRange.endInclusive)
            configPeakOutputReverse(maxOutputRange.start)
            configPeakOutputForward(maxOutputRange.endInclusive)
            selectProfileSlot(pidfSlot, 0)
            pidf.toMap().forEach {
                updatePidf(it.key, it.value)
            }
        }
        configure()
    }
}
