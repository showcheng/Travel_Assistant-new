package com.travel.ai.service.impl;

import com.travel.ai.dto.ChatRequest;
import com.travel.ai.dto.ChatResponse;
import com.travel.ai.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI服务实现类
 * 优先使用GLM模型，API不可用时自动降级到本地智能回复
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private volatile dev.langchain4j.model.chat.ChatLanguageModel chatLanguageModel;
    private volatile boolean glmAvailable = true;
    private volatile long lastGlmCheckTime = 0;
    private static final long GLM_RETRY_INTERVAL = 60_000; // 1分钟后重试GLM

    // ========== 本地知识库 ==========

    private static final Map<String, String> SCENIC_SPOTS = new LinkedHashMap<>();
    private static final Map<String, String> POLICIES = new LinkedHashMap<>();
    private static final Map<String, String> ROUTES = new LinkedHashMap<>();
    private static final Map<String, String> FOODS = new LinkedHashMap<>();
    private static final List<String> GREETING_RESPONSES = new ArrayList<>();
    private static final Map<String, String> PRICE_INFO = new LinkedHashMap<>();
    private static final List<String> TIPS = new ArrayList<>();
    private static final List<String> GENERAL_RESPONSES = new ArrayList<>();
    private static final Random RANDOM = new Random();

    static {
        // 景点知识
        SCENIC_SPOTS.put("故宫|紫禁城", "故宫博物院是中国最大的古代文化艺术博物馆，位于北京中心。开放时间：旺季8:30-17:00，淡季8:30-16:30，周一闭馆。门票：旺季60元，淡季40元。推荐路线：午门→太和殿→乾清宫→御花园→神武门，游览约3-4小时。建议提前网上预约购票。");
        SCENIC_SPOTS.put("长城|万里长城|八达岭", "长城是世界文化遗产，八达岭长城是最著名的段落。开放时间：旺季6:30-19:00，淡季7:00-18:00。门票：40元（旺季），35元（淡季）。建议游览时间3-4小时，穿舒适的运动鞋。可乘坐877路公交或S2线火车前往。");
        SCENIC_SPOTS.put("天坛", "天坛是明清两代帝王祭天的场所，是世界文化遗产。开放时间：旺季6:00-21:00，淡季6:30-21:00。门票：联票34元，优惠票17元。主要景点：祈年殿、回音壁、圜丘。建议游览2-3小时。");
        SCENIC_SPOTS.put("颐和园", "颐和园是中国古典园林之首，昆明湖与万寿山相映成趣。开放时间：旺季6:30-18:00，淡季7:00-17:00。门票：联票60元。推荐游览路线：东宫门→仁寿殿→佛香阁→长廊→十七孔桥，约3-4小时。");
        SCENIC_SPOTS.put("西湖", "杭州西湖是世界文化遗产，有'人间天堂'之美誉。全天免费开放（部分景点收费）。推荐路线：断桥残雪→白堤→苏堤→雷峰塔→三潭印月。可以骑行环湖约15公里，也可以乘船游览。");
        SCENIC_SPOTS.put("兵马俑|秦始皇", "秦始皇兵马俑是世界第八大奇迹，位于西安。开放时间：旺季8:30-18:00，淡季8:30-17:30。门票：120元。建议先看一号坑（最大），再看二号坑和三号坑，游览约2-3小时。");
        SCENIC_SPOTS.put("黄山", "黄山以奇松、怪石、云海、温泉'四绝'闻名。旺季门票190元，淡季150元。建议游览2天，山顶住宿需提前预订。必看景点：迎客松、光明顶、飞来石、莲花峰。");
        SCENIC_SPOTS.put("张家界", "张家界国家森林公园以独特的石英砂岩峰林地貌闻名。门票225元（含环保车，4天有效）。主要景点：袁家界（阿凡达取景地）、天子山、金鞭溪、黄石寨。建议游览2-3天。");
        SCENIC_SPOTS.put("丽江|古城", "丽江古城是世界文化遗产，纳西族传统聚落。古城维护费50元（已取消强制收取）。推荐：四方街、木府、黑龙潭公园、束河古镇。晚上可以体验纳西族篝火晚会。");
        SCENIC_SPOTS.put("三亚|海南", "三亚是中国最南端的热带滨海旅游城市。主要景点：亚龙湾、蜈支洲岛、南山文化旅游区、天涯海角。最佳旅游季节：10月至次年3月。推荐体验：潜水、海鲜大餐、热带雨林探险。");

        // 政策信息
        POLICIES.put("退票|退款|退", "退票政策：\n• 使用前3天以上退票：全额退款\n• 使用前1-2天退票：收取10%手续费\n• 当天退票：收取30%手续费\n• 过期或已使用不可退\n\n因天气等不可抗力因素导致无法游览，可全额退款。");
        POLICIES.put("改签|换日期", "改签政策：\n• 使用前1天以上可免费改签一次\n• 当天改签需支付10%手续费\n• 每张票仅限改签一次\n• 改签需在原使用日期前操作");
        POLICIES.put("优惠|折扣|特价|便宜", "当前优惠活动：\n• 提前7天预订享9折优惠\n• 家庭套票（2大1小）享85折\n• 学生凭学生证享半价优惠\n• 60岁以上老人凭身份证享半价\n• 军人、残疾人凭有效证件免费");
        POLICIES.put("发票|开票", "发票说明：\n• 支付成功后可在订单详情中申请电子发票\n• 发票将在申请后1-3个工作日内发送至您的邮箱\n• 如需增值税专用发票，请联系客服400-888-8888");

        // 路线推荐
        ROUTES.put("北京", "北京经典三日游路线：\n\nDay1：天安门广场→故宫博物院→景山公园→王府井大街\nDay2：八达岭长城→明十三陵→鸟巢水立方夜景\nDay3：颐和园→圆明园→清华大学/北京大学→三里屯\n\n预算参考：约2000-3000元/人");
        ROUTES.put("上海", "上海经典三日游路线：\n\nDay1：外滩→南京路步行街→豫园→城隍庙\nDay2：迪士尼乐园一日游\nDay3：陆家嘴→东方明珠→田子坊→新天地\n\n预算参考：约2500-4000元/人");
        ROUTES.put("西安", "西安经典三日游路线：\n\nDay1：秦始皇兵马俑→华清宫→大唐不夜城\nDay2：西安城墙→陕西历史博物馆→大雁塔\nDay3：回民街→华清池→骊山\n\n预算参考：约1500-2500元/人");
        ROUTES.put("云南|大理|丽江", "云南大理丽江五日游：\n\nDay1：抵达大理→大理古城\nDay2：洱海环游→双廊古镇\nDay3：崇圣寺三塔→丽江古城\nDay4：玉龙雪山→蓝月谷\nDay5：束河古镇→返程\n\n预算参考：约4000-6000元/人");
        ROUTES.put("三亚|海南", "三亚四日海滨度假：\n\nDay1：亚龙湾海滩休闲\nDay2：南山文化旅游区→天涯海角\nDay3：蜈支洲岛一日游（潜水体验）\nDay4：海棠湾免税店→返程\n\n预算参考：约5000-8000元/人");
        ROUTES.put("成都", "成都经典三日游：\n\nDay1：大熊猫繁育研究基地→宽窄巷子→锦里\nDay2：都江堰→青城山→春熙路\nDay3：武侯祠→杜甫草堂→九眼桥酒吧街\n\n预算参考：约1500-2500元/人");

        // 美食推荐
        FOODS.put("北京", "北京必吃美食：北京烤鸭（全聚德、大董）、炸酱面、豆汁儿、卤煮火烧、涮羊肉、驴打滚、糖葫芦。推荐去簋街、南锣鼓巷品尝。");
        FOODS.put("成都|四川", "成都必吃美食：火锅（小龙坎、蜀大侠）、串串香、担担面、龙抄手、夫妻肺片、麻婆豆腐、钟水饺。推荐去锦里、建设路小吃街。");
        FOODS.put("西安", "西安必吃美食：肉夹馍、羊肉泡馍、凉皮、biangbiang面、甑糕、灌汤包。推荐去回民街、永兴坊。");
        FOODS.put("上海", "上海必吃美食：小笼包（南翔馒头店）、生煎包、红烧肉、蟹粉豆腐、葱油拌面、排骨年糕。推荐去城隍庙、南京路。");
        FOODS.put("云南|大理|丽江", "云南必吃美食：过桥米线、汽锅鸡、鲜花饼、野生菌火锅、饵丝、烤乳扇。大理推荐酸辣鱼，丽江推荐腊排骨火锅。");
        FOODS.put("三亚|海南", "三亚必吃美食：海南鸡饭、清补凉、文昌鸡、加积鸭、和乐蟹、东山羊。推荐去第一市场买海鲜加工，新鲜又实惠。");

        // 价格信息
        PRICE_INFO.put("故宫", "故宫门票：旺季60元，淡季40元，珍宝馆10元，钟表馆10元");
        PRICE_INFO.put("长城", "八达岭长城门票：旺季40元，淡季35元；慕田峪长城：40元；缆车单程100元");
        PRICE_INFO.put("天坛", "天坛门票：联票34元，优惠票17元；单买门票15元");
        PRICE_INFO.put("颐和园", "颐和园门票：联票60元，单买门票30元");
        PRICE_INFO.put("兵马俑", "兵马俑门票：120元，含秦始皇陵遗址公园");
        PRICE_INFO.put("黄山", "黄山门票：旺季190元，淡季150元；索道单程80-90元");

        // 旅游小贴士
        TIPS.add("旅游小贴士：出行前请关注目的地天气预报，准备好合适的衣物。夏季注意防晒，冬季注意保暖。");
        TIPS.add("旅游小贴士：购买旅游意外保险很重要，保费低但保障全面，建议每次出行都购买。");
        TIPS.add("旅游小贴士：贵重物品请随身携带，在景区内注意保管好手机和钱包。");
        TIPS.add("旅游小贴士：建议提前预订热门景点门票，很多景点需要预约才能入园。");
        TIPS.add("旅游小贴士：尊重当地风俗习惯，文明旅游，不乱扔垃圾，不在文物上刻字。");
        TIPS.add("旅游小贴士：山区旅游请穿防滑鞋，注意安全警示标志，不要攀爬未开放的景点。");

        // 问候回复
        GREETING_RESPONSES.add("您好！我是智慧旅游助手，很高兴为您服务！我可以帮您推荐景点、规划路线、查询价格、解答旅游问题。请问有什么可以帮您的？");
        GREETING_RESPONSES.add("你好！欢迎使用智慧旅游助手！无论是景点推荐、路线规划还是旅游攻略，我都可以帮您。请问您想去哪里旅游呢？");
        GREETING_RESPONSES.add("嗨！我是您的旅游小助手，随时准备为您服务。您想了解哪些旅游信息呢？");

        // 通用回复
        GENERAL_RESPONSES.add("这是一个很好的问题！旅游方面我比较擅长，比如景点推荐、路线规划、价格查询等，欢迎随时问我。如果您有具体的旅游目的地，我可以给您更详细的建议。");
        GENERAL_RESPONSES.add("感谢您的提问！作为旅游助手，我可以帮您：\n1. 推荐热门景点和旅游路线\n2. 查询门票价格和优惠政策\n3. 提供美食和住宿建议\n4. 解答退改签等政策问题\n\n请问您具体想了解什么呢？");
    }

    public AIServiceImpl(dev.langchain4j.model.chat.ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }

            String aiResponse = getAIResponse(request.getMessage());

            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .response(aiResponse)
                    .intentType("GENERAL")
                    .tokens(aiResponse.length())
                    .timestamp(LocalDateTime.now())
                    .finished(true)
                    .build();

        } catch (Exception e) {
            log.error("AI对话处理失败", e);
            String fallback = generateLocalResponse(request.getMessage());
            return ChatResponse.builder()
                    .sessionId(request.getSessionId())
                    .response(fallback)
                    .intentType("GENERAL")
                    .tokens(fallback.length())
                    .timestamp(LocalDateTime.now())
                    .finished(true)
                    .build();
        }
    }

    @Override
    public String chatStream(ChatRequest request) {
        return chatStream(request.getMessage());
    }

    @Override
    public String chatStream(String message) {
        try {
            if (glmAvailable && chatLanguageModel != null) {
                String response = chatLanguageModel.generate(message);
                log.info("GLM模型回复成功: length={}", response.length());
                return response;
            }
        } catch (Exception e) {
            log.warn("GLM模型调用失败，切换到本地回复: {}", e.getMessage());
            glmAvailable = false;
            lastGlmCheckTime = System.currentTimeMillis();
        }

        // GLM不可用，尝试定期重试
        if (!glmAvailable && System.currentTimeMillis() - lastGlmCheckTime > GLM_RETRY_INTERVAL) {
            tryGlmRecovery();
        }

        return generateLocalResponse(message);
    }

    /**
     * 尝试恢复GLM连接
     */
    private void tryGlmRecovery() {
        try {
            if (chatLanguageModel != null) {
                String test = chatLanguageModel.generate("hi");
                if (test != null && !test.isEmpty()) {
                    glmAvailable = true;
                    log.info("GLM模型连接恢复");
                }
            }
        } catch (Exception e) {
            lastGlmCheckTime = System.currentTimeMillis();
            log.debug("GLM模型仍不可用，继续使用本地回复");
        }
    }

    /**
     * 获取AI响应（优先GLM，降级到本地）
     */
    private String getAIResponse(String message) {
        return chatStream(message);
    }

    /**
     * 基于规则的本地智能回复
     */
    private String generateLocalResponse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "您好，请问有什么可以帮您的？";
        }

        String msg = message.trim();

        // 1. 问候
        if (matchAny(msg, "你好|您好|嗨|hello|hi|早上好|晚上好|在吗")) {
            return pickRandom(GREETING_RESPONSES);
        }

        // 2. 景点查询
        for (Map.Entry<String, String> entry : SCENIC_SPOTS.entrySet()) {
            if (matchAny(msg, entry.getKey())) {
                return entry.getValue();
            }
        }

        // 3. 路线推荐
        for (Map.Entry<String, String> entry : ROUTES.entrySet()) {
            if (matchAny(msg, "路线|行程|怎么安排|攻略|几天") && matchAny(msg, entry.getKey())) {
                return entry.getValue();
            }
        }
        // 通用路线查询
        if (matchAny(msg, "推荐.*路线|旅游路线|行程安排|旅游攻略")) {
            return "我可以为您推荐以下热门路线：\n\n1. 北京三日游（故宫+长城+颐和园）\n2. 西安三日游（兵马俑+城墙+大雁塔）\n3. 云南五日游（大理+丽江+玉龙雪山）\n4. 三亚四日游（海滨度假）\n5. 成都三日游（熊猫+美食+都江堰）\n\n您想了解哪个目的地的详细路线呢？";
        }

        // 4. 美食推荐
        for (Map.Entry<String, String> entry : FOODS.entrySet()) {
            if (matchAny(msg, "美食|吃什么|好吃|小吃|特产|餐厅") && matchAny(msg, entry.getKey())) {
                return entry.getValue();
            }
        }
        if (matchAny(msg, "美食|吃什么|好吃|小吃|推荐.*吃")) {
            return "各城市美食推荐：\n\n• 北京：北京烤鸭、炸酱面、涮羊肉\n• 成都：火锅、串串香、担担面\n• 西安：肉夹馍、羊肉泡馍、凉皮\n• 上海：小笼包、生煎包、葱油拌面\n• 云南：过桥米线、鲜花饼、野生菌火锅\n• 三亚：海南鸡饭、清补凉、海鲜大餐\n\n告诉我您的目的地，我给您更详细的推荐！";
        }

        // 5. 价格查询
        for (Map.Entry<String, String> entry : PRICE_INFO.entrySet()) {
            if (matchAny(msg, "多少钱|价格|票价|门票") && matchAny(msg, entry.getKey())) {
                return entry.getValue();
            }
        }

        // 6. 政策咨询
        for (Map.Entry<String, String> entry : POLICIES.entrySet()) {
            if (matchAny(msg, entry.getKey())) {
                return entry.getValue();
            }
        }

        // 7. 订单查询
        if (matchAny(msg, "订单|我的订单|查询订单|买了什么|购买记录")) {
            return "您可以点击页面导航栏中的【我的订单】查看所有订单信息。如有订单相关问题，也可以告诉我订单号，我帮您查询详情。";
        }

        // 8. 支付相关
        if (matchAny(msg, "支付|付款|怎么付|支付宝|微信")) {
            return "我们支持以下支付方式：\n• 支付宝\n• 微信支付\n• 银行卡支付\n\n下单后选择您方便的支付方式即可。如遇支付问题，请联系客服400-888-8888。";
        }

        // 9. 住宿推荐
        if (matchAny(msg, "住宿|酒店|住哪|宾馆|民宿")) {
            return "住宿建议：\n\n• 经济型：青年旅舍、快捷酒店，100-300元/晚\n• 舒适型：三星/四星酒店，300-600元/晚\n• 豪华型：五星酒店、度假村，600元以上/晚\n\n建议提前在旅游平台预订，旺季价格会上浮30%-50%。";
        }

        // 10. 天气相关
        if (matchAny(msg, "天气|穿什么|带什么衣服|冷不冷|热不热")) {
            return "旅游出行建议关注目的地天气预报：\n\n• 春季（3-5月）：薄外套+长裤，早晚温差大\n• 夏季（6-8月）：防晒衣+短袖，注意防晒\n• 秋季（9-11月）：薄外套+长裤，秋高气爽\n• 冬季（12-2月）：羽绒服+保暖内衣，注意防寒\n\n建议出行前1-2天查看具体天气预报。";
        }

        // 11. 交通相关
        if (matchAny(msg, "怎么去|交通|机票|火车|高铁|飞机")) {
            return "旅游交通建议：\n\n• 飞机：提前2-4周预订，关注特价机票\n• 高铁：适合中短途出行，舒适便捷\n• 自驾：适合周边游，注意路况和停车\n• 景区内：建议步行或乘坐景区观光车\n\n告诉我您的出发地和目的地，我可以给更具体的交通建议。";
        }

        // 12. 旅游小贴士
        if (matchAny(msg, "注意|贴士|建议|提醒|tips")) {
            return pickRandom(TIPS);
        }

        // 13. 景点推荐（通用）
        if (matchAny(msg, "推荐|有什么.*景点|好玩|值得去|想去.*旅游|打算.*旅游")) {
            return "为您推荐热门旅游景点：\n\n🏛 历史文化：故宫、兵马俑、长城、天坛\n🏔 自然风光：黄山、张家界、九寨沟、西湖\n🏖 海滨度假：三亚、厦门、青岛、北海\n🌿 民族风情：丽江、大理、桂林、凤凰古城\n\n您对哪个类型或目的地比较感兴趣？我可以给您更详细的推荐和攻略！";
        }

        // 14. 感谢/告别
        if (matchAny(msg, "谢谢|感谢|thanks|拜拜|再见|好的")) {
            return "不客气！祝您旅途愉快！如果还有其他问题，随时可以问我。😊";
        }

        // 15. 默认通用回复
        return pickRandom(GENERAL_RESPONSES);
    }

    private boolean matchAny(String text, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text).find();
    }

    private String pickRandom(List<String> list) {
        if (list.isEmpty()) return "";
        return list.get(RANDOM.nextInt(list.size()));
    }

    @Override
    public void clearHistory(String sessionId) {
        log.info("清除对话历史: sessionId={}", sessionId);
    }

    @Override
    public String getHistory(String sessionId, Integer limit) {
        log.info("获取对话历史: sessionId={}, limit={}", sessionId, limit);
        return "history-not-implemented";
    }
}
