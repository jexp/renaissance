package org.renaissance.apache.spark

import java.nio.file.{Path, Paths}

import org.apache.commons.io.FileUtils
import org.apache.spark.mllib.classification.NaiveBayesModel
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.renaissance.{Config, License, RenaissanceBenchmark}

class NaiveBayes extends RenaissanceBenchmark {
  override def description(): String =
    "Runs the multinomial naive Bayes algorithm from the Spark MLlib."

  override def defaultRepetitions = 30

  override def licenses(): Array[License] = License.create(License.APACHE2)

  val SMOOTHING = 1.0

  // TODO: Consolidate benchmark parameters across the suite.
  //  See: https://github.com/D-iii-S/renaissance-benchmarks/issues/27
  val THREAD_COUNT = Runtime.getRuntime.availableProcessors

  val naiveBayesPath = Paths.get("target", "naive-bayes")

  val outputPath = naiveBayesPath.resolve("output")

  val inputFile = "sample_libsvm_data.txt.zip"

  val bigInputFile = naiveBayesPath.resolve("bigfile.txt")

  var sc: SparkContext = null

  var data: RDD[LabeledPoint] = null

  var bayesModel: NaiveBayesModel = null

  var tempDirPath: Path = null

  def prepareInput() = {
    FileUtils.deleteDirectory(naiveBayesPath.toFile)
    val text = ZipResourceUtil.readZipFromResourceToText(inputFile)
    for (i <- 0 until 8000) {
      FileUtils.write(bigInputFile.toFile, text, true)
    }
  }

  def loadData() = {
    val num_features = 692
    data = sc
      .textFile(bigInputFile.toString)
      .map { line =>
        val parts = line.split(" ")
        val features = new Array[Double](num_features)
        parts.tail.foreach { part =>
          val dimval = part.split(":")
          val index = dimval(0).toInt - 1
          val value = dimval(1).toInt
          features(index) = value
        }
        new LabeledPoint(parts(0).toDouble, Vectors.dense(features))
      }
      .cache()
  }

  def setUpSpark() = {
    val conf = new SparkConf()
      .setAppName("naive-bayes")
      .setMaster(s"local[$THREAD_COUNT]")
      .set("spark.local.dir", tempDirPath.toString)
      .set("spark.sql.warehouse.dir", tempDirPath.resolve("warehouse").toString)
    sc = new SparkContext(conf)
    sc.setLogLevel("ERROR")
  }

  override def setUpBeforeAll(c: Config): Unit = {
    tempDirPath = RenaissanceBenchmark.generateTempDir("naive_bayes")
    setUpSpark()
    prepareInput()
    loadData()
  }

  override def tearDownAfterAll(c: Config): Unit = {
    // Dump output.
    FileUtils.write(outputPath.toFile, bayesModel.labels.mkString("labels: ", ", ", "\n"), true)
    FileUtils.write(outputPath.toFile, bayesModel.pi.mkString("a priori: ", ", ", "\n"), true)
    FileUtils.write(
      outputPath.toFile,
      bayesModel.theta.zipWithIndex
        .map {
          case (cls, i) =>
            cls.mkString(s"class $i: ", ", ", "")
        }
        .mkString("thetas:\n", "\n", ""),
      true
    )

    sc.stop()
  }

  def runIteration(c: Config): Unit = {
    // Using full package name to avoid conflicting with the renaissance benchmark class name.
    val bayes = new org.apache.spark.mllib.classification.NaiveBayes()
      .setLambda(SMOOTHING)
      .setModelType("multinomial")
    bayesModel = bayes.run(data)
  }
}