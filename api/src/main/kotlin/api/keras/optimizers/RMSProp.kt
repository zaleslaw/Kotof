package api.keras.optimizers

import api.KGraph
import org.tensorflow.Operand
import org.tensorflow.Output
import org.tensorflow.op.Ops
import org.tensorflow.op.core.Constant
import org.tensorflow.op.core.Gradients
import org.tensorflow.op.core.Variable
import java.util.*

private const val RMS = "rms"
private const val MG = "mg"
private const val MOMENTUM = "momentum"

class RMSProp(
    private val learningRate: Float = 0.001f,
    private val decay: Float = 0.9f,
    private val momentum: Float = 0.0f,
    private val epsilon: Float = 1e-10f,
    private val centered: Boolean = false,
    clipGradient: ClipGradientAction = NoClipGradient()
) : Optimizer(clipGradient) {

    private lateinit var epsilonConstant: Constant<Float>
    private lateinit var learningRateConst: Constant<Float>
    private lateinit var decayConst: Constant<Float>
    private lateinit var momentumConst: Constant<Float>

    override fun applyGradients(
        graph: KGraph,
        tf: Ops,
        weights: List<Variable<Float>>,
        gradients: Gradients
    ): List<Operand<Float>> {
        val targets: MutableList<Operand<Float>> =
            ArrayList()

        decayConst = tf.constant(decay, getDType())
        momentumConst = tf.constant(momentum, getDType())
        learningRateConst = tf.constant(learningRate, getDType())
        epsilonConstant = tf.constant(epsilon, getDType())

        for (i in weights.indices) {
            val variable = weights[i]
            val varName = variable.ref().op().name()

            val rmsSlot: Variable<Float> = getSlot(varName, RMS)
            val momentumSlot: Variable<Float> = getSlot(varName, MOMENTUM)

            if (centered) {
                val mgSlot: Variable<Float> = getSlot(varName, MG)
                targets.add(
                    tf.train.applyCenteredRmsProp(
                        variable, mgSlot, rmsSlot, momentumSlot,
                        learningRateConst,
                        decayConst,
                        momentumConst,
                        epsilonConstant,
                        clipGradient.clipGradient(tf, gradients.dy(i))
                    )
                )
            } else {
                targets.add(
                    tf.train.applyRmsProp(
                        variable, rmsSlot, momentumSlot,
                        learningRateConst,
                        decayConst,
                        momentumConst,
                        epsilonConstant,
                        gradients.dy(i)
                    )
                )
            }
        }


        return targets
    }

    private fun createRMSPropSlot(graph: KGraph, tf: Ops, v: Output<Float>) {
        val rmsInitializer: Operand<Float> = tf
            .fill(tf.shape(v), tf.dtypes.cast(tf.constant(1.0f), getDType()))
        createSlot(graph, tf, v.asOutput(), RMS, rmsInitializer)
        val momentumInitializer: Operand<Float> = tf
            .fill(tf.shape(v), tf.dtypes.cast(tf.constant(0.0f), getDType()))
        createSlot(graph, tf, v.asOutput(), MOMENTUM, momentumInitializer)
        if (centered) {
            val mgInitializer: Operand<Float> = tf
                .fill(
                    tf.shape(v),
                    tf.dtypes.cast(tf.constant(0.0f), getDType())
                ) // TODO: should have v.getType() call instead getDType
            createSlot(graph, tf, v.asOutput(), MG, mgInitializer)
        }
    }

    override fun createSlots(graph: KGraph, tf: Ops, variables: List<Output<Float>>) {
        for (v in variables) {
            createRMSPropSlot(graph, tf, v.asOutput())
        }
    }

    override fun getOptimizerName(): String {
        return "RMSProp"
    }
}