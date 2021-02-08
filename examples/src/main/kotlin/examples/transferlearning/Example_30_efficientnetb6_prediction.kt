/*
 * Copyright 2020 JetBrains s.r.o. and Kotlin Deep Learning project contributors. All Rights Reserved.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package examples.transferlearning


import io.jhdf.HdfFile
import org.jetbrains.kotlinx.dl.api.core.Functional
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.inference.keras.loadWeights
import org.jetbrains.kotlinx.dl.datasets.Dataset
import org.jetbrains.kotlinx.dl.datasets.image.ImageConverter
import java.io.File
import java.io.FileReader
import java.util.*


// TODO: write about my experience and unhandled exception with strange layer names
/**
 * 2021-02-04 16:38:00.413198: I tensorflow/core/platform/cpu_feature_guard.cc:142] Your CPU supports instructions that this TensorFlow binary was not compiled to use: AVX2
Exception in thread "main" java.lang.IllegalArgumentException: invalid name: 'conv1/conv_conv2d_kernel' does not match the regular expression [A-Za-z0-9.][A-Za-z0-9_.\-]*
at org.tensorflow.op.NameScope.checkPattern(NameScope.java:129)
at org.tensorflow.op.NameScope.withName(NameScope.java:48)
at org.tensorflow.op.Scope.withName(Scope.java:126)
at org.tensorflow.op.Ops.withName(Ops.java:4572)
at org.jetbrains.kotlinx.dl.api.core.layer.twodim.Conv2D.build(Conv2D.kt:106)
at org.jetbrains.kotlinx.dl.api.core.Functional.compile(Functional.kt:265)
at org.jetbrains.kotlinx.dl.api.core.Functional.compile(Functional.kt:242)
at org.jetbrains.kotlinx.dl.api.core.TrainableModel.compile$default(TrainableModel.kt:76)
at examples.transferlearning.Example_19_efficientnetb6_predictionKt.main(Example_19_efficientnetb6_prediction.kt:55)
at examples.transferlearning.Example_19_efficientnetb6_predictionKt.main(Example_19_efficientnetb6_prediction.kt)
 */
/**
 * This examples demonstrates the inference concept on VGG'19 model:
 *
 * Weights are loaded from .h5 file, configuration is loaded from .json file.
 *
 * Model predicts on a few images located in resources.
 *
 * No additional training.
 *
 * No new layers are added.
 *
 * NOTE: The specific image preprocessing is not implemented yet (see Keras for more details).
 *
 * @see <a href="https://drive.google.com/drive/folders/1P1BlCNXdeXo_9u6mxYnm-N_gbOn_VhUA">
 *     VGG'19 weights and model could be loaded here.</a>
 * @see <a href="https://arxiv.org/abs/1409.1556">
 *     Very Deep Convolutional Networks for Large-Scale Image Recognition (ICLR 2015).</a>
 * @see <a href="https://keras.io/api/applications/vgg/#vgg19-function">
 *    Detailed description of VGG'19 model and an approach to build it in Keras.</a>
 */
fun main() {
    val jsonConfigFile = getefficientnetb6JSONConfigFile()
    val model = Functional.loadModelConfiguration(jsonConfigFile)

    val imageNetClassLabels = prepareHumanReadableClassLabels()

    model.use {
        for (layer in it.layers) {
            layer.isTrainable = false
        }
        it.layers.last().isTrainable = true

        // it.amountOfClasses = 1000

        it.compile(
            optimizer = Adam(),
            loss = Losses.MAE,
            metric = Metrics.ACCURACY
        )

        it.summary()

        val hdfFile = getefficientnetb6WeightsFile()

        it.loadWeights(hdfFile)

        for (i in 1..8) {
            val inputStream = Dataset::class.java.classLoader.getResourceAsStream("datasets/vgg528/image$i.jpg")
            val floatArray = ImageConverter.toRawFloatArray(inputStream)

            val res = it.predict(floatArray)
            println("Predicted object for image$i.jpg is ${imageNetClassLabels[res]}")

            val top5 = predictTop5Labels(it, floatArray, imageNetClassLabels)

            println(top5.toString())
        }
    }
}

/** Returns JSON file with model configuration, saved from Keras 2.x. */
private fun getefficientnetb6JSONConfigFile(): File {
    val properties = Properties()
    val reader = FileReader("data.properties")
    properties.load(reader)

    val efficientnetb6JSONModelPath = properties["efficientnetb6JSONModelPath"] as String

    return File(efficientnetb6JSONModelPath)
}

/** Returns .h5 file with model weights, saved from Keras 2.x. */
private fun getefficientnetb6WeightsFile(): HdfFile {
    val properties = Properties()
    val reader = FileReader("data.properties")
    properties.load(reader)

    val efficientnetb6h5WeightsPath = properties["efficientnetb6h5WeightsPath"] as String

    return HdfFile(File(efficientnetb6h5WeightsPath))
}


