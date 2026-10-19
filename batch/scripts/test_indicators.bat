@echo off
echo 测试指标A
curl -X POST http://localhost:8083/api/spark/analyze -H "Content-Type: application/json" -d "{\"indicator_code\": \"INDICATOR_A\", \"time_window_days\": 30, \"large_amount_threshold\": 50000}"

echo.
echo 测试指标B
curl -X POST http://localhost:8083/api/spark/analyze -H "Content-Type: application/json" -d "{\"indicator_code\": \"INDICATOR_B\", \"time_window_days\": 30, \"large_amount_threshold\": 50000}"

echo.
echo 测试指标C
curl -X POST http://localhost:8083/api/spark/analyze -H "Content-Type: application/json" -d "{\"indicator_code\": \"INDICATOR_C\", \"time_window_days\": 30, \"min_single_amount\": 0.01}"

pause
