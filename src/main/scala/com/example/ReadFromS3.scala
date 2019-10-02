package com.example

import org.apache.spark.sql.SparkSession

object ReadFromS3 extends App {

  System.setProperty("hadoop.home.dir", "/")

  val spark = SparkSession
    .builder
    .appName("SparkSessionZipsExample")
    .master("local")
    .getOrCreate()

  val accessKeyId = "xoxox"
  val secretAccessKey = "xoxox"
  val sc = spark.sparkContext
  sc.hadoopConfiguration.set("fs.s3a.impl","org.apache.hadoop.fs.s3a.S3AFileSystem")
//  sc.hadoopConfiguration.set("fs.s3a.impl","org.apache.hadoop.fs.s3a.S3AFileSystem")
  sc.hadoopConfiguration.set("fs.s3a.access.key", accessKeyId)
  sc.hadoopConfiguration.set("fs.s3a.secret.key", secretAccessKey)

  val df = spark.sqlContext.read
    .format("json")
//    .option("header", "true")
//    .option("inferSchema", "true")
    .load("s3a://ctm-bi-datastore-shadow/GenericModels/modelType=enquiry_details/year=2019/month=1/day=10/productType=car/00bdeefa-cd78-48e6-b363-1cde599f91d9.json.gz")

  df.show(5)
}
