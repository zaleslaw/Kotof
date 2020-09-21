package api.keras.activations

import api.keras.util.getDType
import org.tensorflow.Operand
import org.tensorflow.op.Ops

/**
 * Neural network hyper-parameter, activation function of a node defines the output of that node given an input or set of inputs.
 */
enum class Activations {
    /**
     * Linear unit. Returns unmodified input.
     *
     * NOTE: Doing nothing useful. Returns to ancient times of linear perceptron.
     *
     * Calls [LinearActivation] under the hood.
     */
    Linear,

    /**
     * Sigmoid activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * sigmoid(x) = 1 / (1 + exp(-x))
     * ```
     *
     * For small values (<-5), `sigmoid` returns a value close to zero, and for large values (>5)
     * the result of the function gets close to 1.
     *
     * NOTE: Sigmoid is equivalent to a 2-element Softmax, where the second element is
     * assumed to be zero. The sigmoid function always returns a value between 0 and 1.
     *
     * Calls [SigmoidActivation] under the hood.
     *
     */
    Sigmoid,

    /**
     * Hyperbolic tangent activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * tanh(x) = sinh(x)/cosh(x) = ((exp(x) - exp(-x))/(exp(x) + exp(-x)))
     * ```
     *
     * Calls [TanhActivation] under the hood.
     */
    Tanh,

    /**
     * Rectified linear unit (ReLU).
     *
     * With default values, this returns the standard ReLU activation:
     * `max(x, 0)`, the element-wise maximum of 0 and the input tensor.
     *
     * Calls [ReluActivation] under the hood.
     */
    Relu,

    /**
     * Computes Rectified Linear 6:
     * ```
     * min(max(features, 0), 6)
     * ```
     * Calls [Relu6Activation] under the hood.
     *
     * @see <a href="http://www.cs.utoronto.ca/~kriz/conv-cifar10-aug2010.pdf">
     *     Convolutional Deep Belief Networks on CIFAR-10. A. Krizhevsky</a>
     */
    Relu6,

    /**
     * Exponential Linear Unit.
     *
     * The exponential linear unit (ELU) with `alpha > 0` is:
     * `x` if `x > 0` and `alpha * (exp(x) - 1)` if `x < 0`
     *
     * For this implementations alpha is equal to 1.0.
     *
     * The ELU hyper-parameter `alpha` controls the value to which an
     * ELU saturates for negative net inputs. ELUs diminish the
     * vanishing gradient effect.
     *
     * ELUs have negative values which pushes the mean of the activations closer to zero.
     *
     * Mean activations that are closer to zero enable faster learning as they
     * bring the gradient closer to the natural gradient.
     *
     * ELUs saturate to a negative value when the argument gets smaller.
     * Saturation means a small derivative which decreases the variation
     * and the information that is propagated to the next layer.
     *
     * Calls [EluActivation] under the hood.
     *
     * @see <a href="https://arxiv.org/abs/1511.07289">Fast and Accurate Deep Network Learning by Exponential Linear Units
     * (ELUs) (Clevert et al, 2016)</a>
     */
    Elu,

    /**
     * Scaled Exponential Linear Unit (SELU).
     *
     * The Scaled Exponential Linear Unit (SELU) activation function is defined as:
     * ```
     *  if x > 0: return scale * x
     *  if x < 0: return scale * alpha * (exp(x) - 1)
     * ```
     * where `alpha` and `scale` are pre-defined constants (`alpha=1.67326324` and `scale=1.05070098`).
     *
     * Basically, the SELU activation function multiplies `scale` (> 1) with the
     * output of the `tf.keras.activations.elu` function to ensure a slope larger
     * than one for positive inputs.
     *
     * NOTE: The values of `alpha` and `scale` are
     * chosen so that the mean and variance of the inputs are preserved
     * between two consecutive layers as long as the weights are initialized
     * correctly (see [api.keras.initializers.LeCunNormal] initializer)
     * and the number of input units is "large enough"
     * (see reference paper for more information).
     *
     * Calls [SeluActivation] under the hood.
     *
     * @see <a href="https://arxiv.org/abs/1706.02515">Klambauer et al., 2017</a>
     */
    Selu,

    /**
     * Softmax converts a real vector to a vector of categorical probabilities.
     * The elements of the output vector are in range (0, 1) and sum to 1.
     *
     * Softmax is often used as the activation for the last
     * layer of a classification network because the result could be interpreted as
     * a probability distribution.
     *
     * Calls [SoftmaxActivation] under the hood.
     */
    Softmax,

    /**
     *
     */
    LogSoftmax,

    /**
     * Exponential activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * exp(x)
     * ```
     *
     * Calls [ExponentialActivation] under the hood.
     */
    Exponential,

    /**
     * Softplus activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * softplus(x) = log(exp(x) + 1)
     * ```
     *
     * Calls [SoftPlusActivation] under the hood.
     */
    SoftPlus,

    /***
     * Softsign activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * softsign(x) = x / (abs(x) + 1)
     * ```
     *
     * Calls [SoftSignActivation] under the hood.
     */
    SoftSign,

    /**
     * Hard sigmoid activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * if x < -2.5: return 0
     * if x > 2.5: return 1
     * if -2.5 <= x <= 2.5: return 0.2 * x + 0.5
     * ```
     * A faster approximation of the sigmoid activation.
     *
     * Calls [HardSigmoidActivation] under the hood.
     */
    HardSigmoid,

    /**
     * Swish activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * swish(x) = x * sigmoid(x)
     * ```
     *
     * It is a smooth, non-monotonic function that consistently matches
     * or outperforms ReLU on deep networks, it is unbounded above and
     * bounded below.
     *
     * Calls [SwishActivation] under the hood.
     *
     * @see <a href="https://arxiv.org/abs/1710.05941">Ramachandran et al., 2017</a>
     */
    Swish;

    companion object {
        /**
         * Converts [activationType] to the appropriate [Activation] sub-class.
         */
        fun convert(activationType: Activations): Activation {
            return when (activationType) {
                Sigmoid -> SigmoidActivation()
                Linear -> LinearActivation()
                Tanh -> TanhActivation()
                Relu -> ReluActivation()
                Relu6 -> Relu6Activation()
                Elu -> EluActivation()
                Selu -> SeluActivation()
                Softmax -> SoftmaxActivation()
                LogSoftmax -> LogSoftmaxActivation()
                Exponential -> ExponentialActivation()
                SoftPlus -> SoftPlusActivation()
                SoftSign -> SoftSignActivation()
                HardSigmoid -> HardSigmoidActivation()
                Swish -> SwishActivation()
            }
        }
    }
}

/**
 * @see [Activations.Linear]
 */
class LinearActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return if (name.isNotEmpty()) {
            features
        } else {
            tf.withName("Activation_$name").identity(features)
        }
    }
}

/**
 * @see [Activations.Sigmoid]
 */
class SigmoidActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return if (name.isNotEmpty()) {
            tf.math.sigmoid(features)
        } else {
            tf.withName("Activation_$name").math.sigmoid(features)
        }
    }
}

/**
 * @see [Activations.Relu]
 */
class ReluActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return if (name.isEmpty()) {
            tf.nn.relu(features)
        } else {
            tf.withName("Activation_$name").nn.relu(features)
        }
    }
}

/**
 * @see [Activations.Relu6]
 */
class Relu6Activation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return if (name.isNotEmpty()) {
            tf.nn.relu6(features)
        } else {
            tf.withName("Activation_$name").nn.relu6(features)
        }
    }
}

/**
 * @see [Activations.Tanh]
 */
class TanhActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return if (name.isNotEmpty()) {
            tf.math.tanh(features)
        } else {
            tf.withName("Activation_$name").math.tanh(features)
        }
    }
}

/**
 * @see [Activations.Elu]
 */
class EluActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return if (name.isNotEmpty()) {
            tf.nn.elu(features)
        } else {
            tf.withName("Activation_$name").nn.elu(features)
        }
    }
}

/**
 * @see [Activations.Selu]
 */
class SeluActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return if (name.isNotEmpty()) {
            tf.nn.selu(features)
        } else {
            tf.withName("Activation_$name").nn.selu(features)
        }
    }
}

/**
 * Internal class, wrapping TensorFlow operand
 * ```
 * tf.nn.softmax
 * ```
 *
 * For each batch `i` and class `j` we have
 *
 * ```
 * softmax[i, j] = exp(logits[i, j]) / sum_j(exp(logits[i, j]))
 * ```
 *
 * @see [Activations.Softmax] for explanation.
 */
class SoftmaxActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return if (name.isNotEmpty()) {
            tf.nn.softmax(features)
        } else {
            tf.withName("Activation_$name").nn.softmax(features)
        }
    }
}

/**
 * @see [Activations.LogSoftmax]
 */
class LogSoftmaxActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return tf.nn.logSoftmax(features)
    }
}

/**
 * @see [Activations.Exponential]
 */
class ExponentialActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return tf.math.exp(features)
    }
}

/**
 * @see [Activations.SoftPlus]
 */
class SoftPlusActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        val one: Operand<Float> = tf.dtypes.cast(tf.constant(1), getDType())

        return tf.math.log(tf.math.add(tf.math.exp(features), one))
    }
}

/**
 * @see [Activations.SoftSign]
 */
class SoftSignActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return tf.nn.softsign(features)
    }
}

/**
 * @see [Activations.HardSigmoid]
 */
class HardSigmoidActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        val point2: Operand<Float> = tf.dtypes.cast(tf.constant(0.2), getDType())
        val point5: Operand<Float> = tf.dtypes.cast(tf.constant(0.5), getDType())

        return tf.math.add(tf.math.mul(features, point2), point5)
    }
}

/**
 * @see [Activations.Swish]
 */
class SwishActivation : Activation {
    override fun apply(tf: Ops, features: Operand<Float>, name: String): Operand<Float> {
        return tf.math.mul(features, tf.math.sigmoid(features))
    }
}