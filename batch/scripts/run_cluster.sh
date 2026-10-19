#!/bin/bash
spark-submit \
  --class com.example.spark.MainJob \
  --master yarn \
  --deploy-mode cluster \
  --executor-memory 8G \
  --total-executor-cores 32 \
  fraud-analysis-system-1.0-SNAPSHOT.jar \
  hdfs:///data/login.parquet \
  hdfs:///data/transfer.parquet \
  hdfs:///output/result \
  2025-11-01