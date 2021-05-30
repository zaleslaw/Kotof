/*
 * Copyright 2020 JetBrains s.r.o. and Kotlin Deep Learning project contributors. All Rights Reserved.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dl.api.core.layer.activation

import org.tensorflow.Operand
import org.tensorflow.op.Ops

/**
 * Thresholded Rectified Linear Unit.
 *
 * It follows:
 * ```
 * f(x) = x,        if x > threshold
 * f(x) = 0,        otherwise
 * ```
 * @property [threshold] Threshold value for threshold activation.
 * @constructor Creates [ThresholdedReLU] object.
 * @since 0.3
 */
public class ThresholdedReLU(
    private val threshold: Float = 1.0f,
    name: String = ""
) : AbstractActivationLayer(name) {

    init {
        require(threshold >= 0.0f) { "Threshold $threshold should be >= 0.0." }
    }

    override fun forward(tf: Ops, input: Operand<Float>): Operand<Float> {
        return commonRelu(tf, input = input, threshold = threshold)
    }

    override fun toString(): String =
        "ThresholdedReLU(theta=$threshold)"
}
