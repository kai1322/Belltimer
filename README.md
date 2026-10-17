# Bell-timer

แอป Android เตือนเสียงระฆังซ้ำตามช่วงเวลา (1 นาที – 4 ชั่วโมง) ไม่มีโฆษณา ไม่ขอสิทธิ์อินเทอร์เน็ต

- ใช้ `AlarmManager.setExactAndAllowWhileIdle` คำนวณเวลาจาก "เวลากดเริ่ม" ทุกรอบ (start + n × interval) ไม่เลื่อนสะสม
- เสียง "ปิ๊ง ปิ๊ง" สร้างด้วยโค้ด (additive synthesis) ปรับจำนวนรอบ 1–3 และความดังได้
- แจ้งเตือนเงียบ แสดงเวลาครั้งถัดไป มีปุ่มหยุด
- ตั้งต่ออัตโนมัติหลังรีสตาร์ท / เปลี่ยนเวลาเครื่อง / อัปเดตแอป

## Build
push ขึ้น GitHub → Actions → Build APK → ดาวน์โหลด artifact `Bell-timer-debug-apk`
