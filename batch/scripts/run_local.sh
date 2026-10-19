#!/bin/bash
mvn clean package -DskipTests
spark-submit \
  --class com.example.spark.MainJob \
  --master local[*] \
  target/fraud-analysis-system-1.0-SNAPSHOT.jar \
  data/login_sample.csv \
  data/transfer_sample.csv \
  output/ \
  2025-11-01