/*
 * Copyright 2020 JetBrains s.r.o. and Kotlin Deep Learning project contributors. All Rights Reserved.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package examples.transferlearning

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import io.jhdf.HdfFile
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.TrainableModel
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.core.shape.reshape3DTo1D
import org.jetbrains.kotlinx.dl.api.core.shape.tail
import org.jetbrains.kotlinx.dl.api.inference.keras.loadWeights
import org.jetbrains.kotlinx.dl.datasets.Dataset
import org.jetbrains.kotlinx.dl.datasets.image.ImageConverter
import java.io.File
import java.io.FileReader
import java.util.*

/**
 * This examples demonstrates the inference concept on VGG'16 model:
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
 * @see <a href="https://drive.google.com/drive/folders/1283PvmF8TykZ70NVbLr1-gW0I6Y2rQ6Q">
 *     VGG'16 weights and model could be loaded here.</a>
 * @see <a href="https://arxiv.org/abs/1409.1556">
 *     Very Deep Convolutional Networks for Large-Scale Image Recognition (ICLR 2015).</a>
 * @see <a href="https://keras.io/api/applications/vgg/#vgg16-function">
 *    Detailed description of VGG'16 model and an approach to build it in Keras.</a>
 */
fun main() {
    val jsonConfigFile = getVGG16JSONConfigFile()
    val model = Sequential.loadModelConfiguration(jsonConfigFile)

    val imageNetClassLabels = prepareHumanReadableClassLabels()

    model.use {
        it.compile(
            optimizer = Adam(),
            loss = Losses.MAE,
            metric = Metrics.ACCURACY
        )
        println(it.kGraph)

        it.summary()

        val hdfFile = getVGG16WeightsFile()

        it.loadWeights(hdfFile)

        for (i in 1..8) {
            val inputStream = Dataset::class.java.classLoader.getResourceAsStream("datasets/vgg/image$i.jpg")
            val floatArray = ImageConverter.toRawFloatArray(inputStream)

            val xTensorShape = it.inputLayer.input.asOutput().shape()
            val tensorShape = longArrayOf(
                1,
                *tail(xTensorShape)
            )

            val inputData = preprocessInput(floatArray, tensorShape, inputType = InputType.CAFFE)

            val res = it.predict(inputData, "Activation_predictions")
            println("Predicted object for image$i.jpg is ${imageNetClassLabels[res]}")

            val top5 = predictTop5Labels(it, inputData, imageNetClassLabels)

            println(top5.toString())
        }
    }
}

public fun preprocessInput(floatArray: FloatArray, tensorShape: LongArray? = null, inputType: InputType): FloatArray {
    return when (inputType) {
        InputType.TF -> floatArray.map { it / 127.5f - 1 }.toFloatArray()
        InputType.CAFFE -> caffeStylePreprocessing(floatArray, tensorShape!!)
        InputType.TORCH -> torchStylePreprocessing(floatArray, tensorShape!!)
    }
}

fun torchStylePreprocessing(input: FloatArray, tensorShape: LongArray): FloatArray {
    val height = tensorShape[1].toInt()
    val width = tensorShape[2].toInt()
    val channels = tensorShape[3].toInt()

    val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
    val std = floatArrayOf(0.229f, 0.224f, 0.225f)

    val scaledInput = input.map { it / 255f }.toFloatArray()
    val reshapedInput = reshapeInput(scaledInput, tensorShape)

    for (i in 0 until height) {
        for (j in 0 until width) {
            for (k in 0 until channels) {
                reshapedInput[i][j][k] = (reshapedInput[i][j][k] - mean[k]) / std[k]
            }
        }
    }

    return reshape3DTo1D(reshapedInput, height * width * channels)
}

fun caffeStylePreprocessing(input: FloatArray, tensorShape: LongArray): FloatArray {
    val height = tensorShape[1].toInt()
    val width = tensorShape[2].toInt()
    val channels = tensorShape[3].toInt()

    val mean = floatArrayOf(103.939f, 116.779f, 123.68f)

    val reshapedInput = reshapeInput(input, tensorShape)

    for (i in 0 until height) {
        for (j in 0 until width) {
            for (k in 0 until channels) {
                reshapedInput[i][j][k] = reshapedInput[i][j][k] - mean[k]
            }
        }
    }

    return reshape3DTo1D(reshapedInput, height * width * channels)
}

fun reshapeInput(inputData: FloatArray, tensorShape: LongArray): Array<Array<FloatArray>> {
    val height = tensorShape[1].toInt()
    val width = tensorShape[2].toInt()
    val channels = tensorShape[3].toInt()
    val reshaped = Array(
        height
    ) { Array(width) { FloatArray(channels) } }

    var pos = 0
    for (i in 0 until height) {
        for (j in 0 until width) {
            for (k in 0 until channels) {
                reshaped[i][j][k] = inputData[pos]
                pos++
            }
        }
    }

    return reshaped
}

fun predictTop5Labels(
    it: TrainableModel,
    floatArray: FloatArray,
    imageNetClassLabels: MutableMap<Int, String>
): MutableMap<Int, Pair<String, Float>> {
    val predictionVector = it.predictSoftly(floatArray).toMutableList()
    val predictionVector2 = it.predictSoftly(floatArray).toMutableList() // get copy of previous vector

    val top5: MutableMap<Int, Pair<String, Float>> = mutableMapOf()
    for (j in 1..5) {
        val max = predictionVector2.maxOrNull()
        val indexOfElem = predictionVector.indexOf(max!!)
        top5[j] = Pair(imageNetClassLabels[indexOfElem]!!, predictionVector[indexOfElem])
        predictionVector2.remove(max)
    }

    return top5
}

fun prepareHumanReadableClassLabels(): MutableMap<Int, String> {
    val pathToIndices = "/datasets/vgg/imagenet_class_index.json"

    fun parse(name: String): Any? {
        val cls = Parser::class.java
        return cls.getResourceAsStream(name)?.let { inputStream ->
            return Parser.default().parse(inputStream, Charsets.UTF_8)
        }
    }

    val classIndices = parse(pathToIndices) as JsonObject

    val imageNetClassIndices = mutableMapOf<Int, String>()

    for (key in classIndices.keys) {
        imageNetClassIndices[key.toInt()] = (classIndices[key] as JsonArray<*>)[1].toString()
    }
    return imageNetClassIndices
}

/** Returns JSON file with model configuration, saved from Keras 2.x. */
private fun getVGG16JSONConfigFile(): File {
    val properties = Properties()
    val reader = FileReader("data.properties")
    properties.load(reader)

    val vgg16JSONModelPath = properties["vgg16JSONModelPath"] as String

    return File(vgg16JSONModelPath)
}

/** Returns .h5 file with model weights, saved from Keras 2.x. */
private fun getVGG16WeightsFile(): HdfFile {
    val properties = Properties()
    val reader = FileReader("data.properties")
    properties.load(reader)

    val vgg16h5WeightsPath = properties["vgg16h5WeightsPath"] as String

    return HdfFile(File(vgg16h5WeightsPath))
}




