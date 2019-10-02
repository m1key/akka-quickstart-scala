package com.example

import com.example.JsonToParquet.spark
import org.apache.spark.sql.SparkSession

object ReadParquet extends App {

  System.setProperty("hadoop.home.dir", "/")

  val spark = SparkSession
    .builder
    .appName("SparkSessionZipsExample")
    .master("local")
    .getOrCreate()

  import spark.implicits._

  val peopleDF = spark.read.json("/Users/michalhuniewicz/Downloads/014dfde8-d3f6-4003-90f1-c3149ad75032.json")

  peopleDF.show(5)

  val parquetFileDF = spark.read.parquet("/Users/michalhuniewicz/Downloads/014dfde8-d3f6-4003-90f1-c3149ad75032.parquet")
  parquetFileDF.show(5)

  spark.close()

}
