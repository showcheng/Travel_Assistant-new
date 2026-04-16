-- 插入商品测试数据（UTF-8编码）
USE travel_assistant;

INSERT INTO product (name, category_id, type, price, original_price, stock, sales, cover_image, detail, status) VALUES
('故宫博物院成人票', 1, 1, 60.00, 80.00, 1000, 0, 'https://example.com/images/forbidden-city.jpg', '北京故宫博物院成人门票，包含故宫主要景点参观权限', 1),
('长城一日游', 1, 1, 188.00, 238.00, 500, 0, 'https://example.com/images/great-wall.jpg', '八达岭长城一日游，含交通和导游服务', 1),
('天坛公园门票', 1, 1, 35.00, 45.00, 2000, 0, 'https://example.com/images/temple-of-heaven.jpg', '北京天坛公园联票，包含祈年殿、回音壁等景点', 1),
('北京特产礼盒', 2, 2, 128.00, 168.00, 300, 0, 'https://example.com/images/gift-box.jpg', '北京特产礼盒，包含烤鸭、果脯等特色食品', 1),
('故宫文创纪念品', 2, 2, 58.00, 78.00, 800, 0, 'https://example.com/images/souvenir.jpg', '故宫文创纪念品，包含书签、明信片等', 1);
