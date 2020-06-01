package api.keras.activations

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.tensorflow.EagerSession
import org.tensorflow.Operand
import org.tensorflow.op.Ops

internal class ReluActivationTest : ActivationTest() {

    @Test
    fun apply() {
        val input = floatArrayOf(-100f, -10f, -1f, 0f, 1f, 10f, 100f)
        val actual = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val expected = floatArrayOf(
            0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            10f, 100f
        )

        EagerSession.create().use { session ->
            val tf = Ops.create(session)
            val instance: ReluActivation<Float> = ReluActivation()
            val operand: Operand<Float> = instance.apply(tf, tf.constant(input))
            operand.asOutput().tensor().copyTo(actual)

            assertArrayEquals(
                expected,
                actual,
                EPS
            )
        }
    }
}