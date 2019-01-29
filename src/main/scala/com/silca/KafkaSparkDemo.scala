package com.silca

import java.util.Properties

import net.liftweb.json.{DefaultFormats, parse}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.kafka.clients.producer._

import scala.util.{Failure, Success, Try}

/*** Create a RDD of lines from a kafka topic and insert in Mapr-DB database ***/

object KafkaSparkDemo {

  // Global configuration
  val spark_app_name= "SparkDemo"
  val spark_master_url = "spark://spark-master:7077"
  val spark_batch_interval = 10
  val topicreject = "error"
  val brokerlist = "kafka:9092"
  val topics  = List[String]("events")

  val groupid = "spark-consumer-group"
  val vertica_table = "test"

  val  props = new Properties()
  props.put("bootstrap.servers", "kafka:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[String, String](props)

  def GenericParseJsonInput[T: Manifest](json: String) = {

    implicit val formats = DefaultFormats
    Try(parse(json).extract[T]) match {
      case Success(value) => Tuple1(Some(value))
      case Failure(_) => Tuple1(None)
    }
  }

  def main(args: Array[String]) {
    //create a SparkSession
    val spark = SparkSession
      .builder()
      .appName(spark_app_name)
      .master(spark_master_url)
      .config("spark.ui.port", "7077")
      .config("spark.streaming.backpressure.enabled", "true")
      .config("spark.streaming.kafka.maxRatePerPartition", "1000")
      .getOrCreate()

    // create a SparkContext
    val ssc = new StreamingContext(spark.sparkContext, Seconds(spark_batch_interval))
    val sc = ssc.sparkContext

    // create a SQLContext
    val sqlContext = new SQLContext(sc)

    val param = Map[String, String](
      "bootstrap.servers" -> "kafka:9092",
      "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> "false",
      "group.id" -> groupid
    )

    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topics, param)
    )

    var offsetRanges = Array[OffsetRange]()

    stream
        .transform {
          rdd =>
            offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
            rdd
        }
        .map { x =>

          if (GenericParseJsonInput[test1](x.value().toString) == Tuple1(None)) {
            println("Failure")
            producer.send(new ProducerRecord(topicreject, x.value().toString))
            Tuple1(None)
          } else {
            GenericParseJsonInput[test1](x.value().toString)
          }

        }.foreachRDD { rdd =>
        try {
          val filename = topics + unix_timestamp().toString()
          rdd.filter(x => x._1.getOrElse(None) != None).map(_._1.get).saveAsTextFile("/mapr/demo.mapr.com/tmp/" + filename)

          /* // or this =>
          import spark.sqlContext.implicits._
          val df = rdd.filter(x => x._1.getOrElse(None) != None).map(_._1.get).toDF()
          df.write.format("json").save("hdfs://hostName[:port][/path][?options]")

          */
          stream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)

        } catch {
          case e: Throwable => println(e)
        }
      }

    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
   producer.close()

  }
}
