#!/bin/bash

# 知识库虚拟数据批量上传脚本

BASE_URL="http://localhost:8086/api/knowledge"

echo "=== 开始批量上传虚拟知识库数据 ==="
echo ""

# 1. 景点介绍类文档
echo "1. 上传景点介绍类文档..."

curl -s -X POST $BASE_URL/upload \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Beijing Forbidden City Comprehensive Guide",
    "category": "ATTRACTION",
    "content": "The Forbidden City in Beijing, also known as the Imperial Palace, was the royal palace during the Ming and Qing dynasties. It is located at the center of Beijings central axis and covers an area of about 720,000 square meters. The Forbidden City is the worlds largest and best-preserved ancient wooden architectural complex. The palace buildings are arranged along the north-south central axis and divided into the Outer Court and Inner Court. The main buildings of the Outer Court include the Hall of Supreme Harmony, Hall of Central Harmony, and Hall of Preserving Harmony, where the emperor held grand ceremonies. The Inner Courts main buildings include the Palace of Heavenly Purity, Hall of Union, and Palace of Earthly Tranquility, which were the living quarters for the emperor and empress. The Forbidden City is not only Chinas cultural treasure but also a world cultural heritage site, attracting numerous visitors every year.",
    "fileType": "text",
    "userId": 1
  }'

curl -s -X POST $BASE_URL/upload \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Great Wall of China Badaling Section",
    "category": "ATTRACTION",
    "content": "The Great Wall of China is one of the most iconic landmarks in the world. The Badaling section, located about 70 kilometers northwest of Beijing, is the most visited and best-preserved section. Built during the Ming Dynasty, it stretches over 7,000 meters across mountain ridges. The wall at Badaling averages 7.8 meters high and 6.5 meters wide at the top, wide enough for five horses or ten soldiers to march side by side. This section was opened to tourists in 1957 and has received over 500 heads of state and government since then. Visitors can take cable cars for easy access or hike along the steep steps for a more challenging experience.",
    "fileType": "text",
    "userId": 1
  }'

curl -s -X POST $BASE_URL/upload \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Summer Palace Royal Garden",
    "category": "ATTRACTION",
    "content": "The Summer Palace, also known as Yiheyuan, is a massive ensemble of lakes, gardens and palaces in Beijing. It was an imperial garden in the Qing Dynasty and covers an area of 2.9 square kilometers, three-quarters of which is water. The main features include Kunming Lake and Longevity Hill. The palace complex features over 3,000 structures including pavilions, towers, and bridges. The Summer Palace has been listed as a UNESCO World Heritage Site since 1998. Key attractions include the Long Corridor, the Marble Boat, and the Seventeen-Arch Bridge. It is a perfect example of Chinese landscape garden design.",
    "fileType": "text",
    "userId": 1
  }'

echo "✅ 景点介绍类文档上传完成"
echo ""

# 2. 政策说明类文档
echo "2. 上传政策说明类文档..."

curl -s -X POST $BASE_URL/upload \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Ticket Refund and Exchange Policy",
    "category": "POLICY",
    "content": "Refund and Exchange Policy: Tickets can be refunded or exchanged up to 24 hours before the visit date. For refunds requested within 7 days of purchase, a full refund will be processed. For refunds requested between 8-30 days, a 10% processing fee will apply. No refunds are available within 24 hours of the visit time. In case of extreme weather conditions forcing attraction closure, full refunds will be automatically processed. Refunds are processed within 5-7 business days to the original payment method. Exchange requests are subject to availability and price differences.",
    "fileType": "text",
    "userId": 1
  }'

curl -s -X POST $BASE_URL/upload \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Student Discount Policy",
    "category": "POLICY",
    "content": "Student Discount Eligibility: Full-time students aged 18-23 with valid student ID cards are eligible for discounted admission tickets. The discount typically ranges from 20-50% off regular adult pricing. Graduate students and part-time students are not eligible unless specifically stated. Student IDs must be presented at the ticket counter along with a valid government-issued ID card. Online bookings require students to show their physical student ID upon entry; failure to present valid documentation will require payment of the full price difference.",
    "fileType": "text",
    "userId": 1
  }'

echo "✅ 政策说明类文档上传完成"
echo ""

# 3. 路线推荐类文档
echo "3. 上传路线推荐类文档..."

curl -s -X POST $BASE_URL/upload \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Beijing One Day Cultural Tour Route",
    "category": "ROUTE",
    "content": "Recommended One Day Cultural Itinerary: Start at Tiananmen Square at 8:00 AM, visit the Forbidden City from 8:30-12:00. Lunch at nearby traditional restaurant (12:00-13:00). Afternoon visit to Temple of Heaven (13:30-15:30). Late afternoon exploration of Summer Palace (16:00-18:00). Evening dinner at Quanjude Roast Duck restaurant (19:00-20:30). Total cost approximately 800-1000 RMB including tickets, meals and transportation. This route covers the most essential cultural sites in Beijing and is recommended for first-time visitors.",
    "fileType": "text",
    "userId": 1
  }'

curl -s -X POST $BASE_URL/upload \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Family Friendly Two Day Tour",
    "category": "ROUTE",
    "content": "Family Oriented Two Day Itinerary: Day 1 - Beijing Zoo in the morning (9:00-12:00), lunch and rest (12:00-14:00), Science and Technology Museum afternoon (14:30-17:00). Day 2 - Olympic Park morning (9:00-12:00), lunch and rest (12:00-14:00), China Science and Technology Museum afternoon (14:30-17:00). This route is designed specifically for families with children ages 6-12, with interactive exhibits and child-friendly facilities. Each location has baby care rooms, family restrooms, and kid-friendly dining options.",
    "fileType": "text",
    "userId": 1
  }'

echo "✅ 路线推荐类文档上传完成"
echo ""

# 4. 常见问题类文档
echo "4. 上传常见问题类文档..."

curl -s -X POST $BASE_URL/upload \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Opening Hours and Best Visiting Time",
    "category": "FAQ",
    "content": "Opening Hours Information: Most attractions in Beijing are open from 8:00 AM to 5:00 PM (April-October) and 8:30 AM to 4:30 PM (November-March). Last entry is typically 1 hour before closing. Best visiting times are weekday mornings when crowds are thinner. Summer weekends and Chinese public holidays are extremely crowded with wait times up to 2-3 hours. The least crowded months are November through March (excluding Chinese New Year period). Spring (April-May) and Autumn (September-October) offer the most pleasant weather conditions.",
    "fileType": "text",
    "userId": 1
  }'

curl -s -X POST $BASE_URL/upload \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Transportation Guide to Major Attractions",
    "category": "FAQ",
    "content": "Transportation Options: Subway Line 1 serves Tiananmen Square, Forbidden City, and Wangfujing. Bus routes 5, 58, 101, 103, 109, 124 connect to Summer Palace. For Great Wall Badaling, take bus 877 from Deshengmen or 919 from Dongzhimen. Didi (Chinese Uber) and taxi services are widely available but traffic can be severe during rush hours. Rental cars with drivers cost approximately 800-1200 RMB per day. Public transportation is most reliable during congestion periods. Consider purchasing a Beijing Transportation Card for convenient metro and bus access.",
    "fileType": "text",
    "userId": 1
  }'

echo "✅ 常见问题类文档上传完成"
echo ""

echo "=== 虚拟知识库数据构建完成 ==="
echo ""
echo "已上传文档总数待确认..."
echo "请检查数据库和向量存储状态。"
