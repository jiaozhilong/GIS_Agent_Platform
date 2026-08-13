package com.jiaozhilong.gisagent.project;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductCatalogService {
    private static final String OFFICIAL_URL = "https://help.supermap.com/iManager_K8S/zh/GettingStarted/ProductLine.htm";

    public record CatalogItem(String id, String name, String category, String description,
                              List<String> capabilities, String officialUrl) {}

    private final List<CatalogItem> catalog = List.of(
            item("iserver", "SuperMap iServer", "云 GIS 服务器", "分布式、可扩展的企业级 GIS 服务与云原生空间计算平台", "服务发布与管理", "空间分析", "空间大数据", "GeoAI", "三维服务"),
            item("iportal", "SuperMap iPortal", "云 GIS 服务器", "GIS 资源中心、用户中心和应用中心", "资源整合共享", "服务注册", "权限控制", "零代码应用"),
            item("imanager", "SuperMap iManager", "云 GIS 服务器", "面向 Kubernetes 的 GIS 云原生运维管理中心", "云原生运维", "弹性伸缩", "监控告警", "基础设施管理"),
            item("online", "SuperMap Online", "云 GIS 服务器", "数据、服务、分析与展示一体化的在线 GIS 平台", "公有云 GIS", "在线分析", "数据上云", "开发者服务"),
            item("imagex-server", "SuperMap ImageX Server", "云 GIS 服务器", "云原生遥感 GIS 一体化服务器", "遥感处理", "智能解译", "DOM 生产", "分布式计算"),
            item("agentx-server", "SuperMap AgentX Server", "云 GIS 服务器", "面向复杂 GIS 任务的地理空间智能体服务平台", "Chatflow Agent", "Workflow Agent", "Autonomous Agent", "MCP 工具协同"),
            item("iedge", "SuperMap iEdge", "边缘 GIS", "就近发布、代理、缓存与实时分析的边缘 GIS 平台", "边缘发布", "服务代理", "缓存加速", "边缘分析"),
            item("iobjects-cpp", "SuperMap iObjects C++", "组件端 GIS", "C++ 跨平台二三维 GIS 组件开发平台", "C++ 开发", "二三维一体化", "空间分析"),
            item("iobjects-java", "SuperMap iObjects Java", "组件端 GIS", "Java 跨平台二三维 GIS 组件开发平台", "Java 开发", "业务系统集成", "空间计算"),
            item("iobjects-dotnet", "SuperMap iObjects .NET", "组件端 GIS", ".NET 二三维 GIS 组件开发平台", ".NET 开发", "业务系统集成", "桌面应用"),
            item("iobjects-python", "SuperMap iObjects Python", "组件端 GIS", "面向 Python 的空间数据处理与分析开发平台", "Python 开发", "数据处理", "GeoAI", "自动化"),
            item("iobjects-spark", "SuperMap iObjects for Spark", "组件端 GIS", "面向 Spark 的分布式大数据 GIS 组件", "Spark", "分布式分析", "空间大数据"),
            item("hifi-3d", "SuperMap Hi-Fi 3D SDKs", "组件端 GIS", "融合 Unreal Engine 与 Unity 的高保真三维 GIS SDK", "UE5", "Unity", "高保真渲染", "数字孪生"),
            item("idesktopx", "SuperMap iDesktopX", "桌面端 GIS", "跨平台空间数据生产、分析、制图与处理自动化平台", "数据生产", "地图制图", "空间分析", "处理自动化"),
            item("iexplorer3d", "SuperMap iExplorer3D", "桌面端 GIS", "基于 UE5 的海量三维场景高保真浏览软件", "三维浏览", "高保真渲染", "数字孪生"),
            item("imaritime", "SuperMap iMaritimeEditor", "桌面端 GIS", "跨平台电子海图生产与多标准一体化编辑软件", "电子海图", "S-101", "海图编辑", "信创"),
            item("imagex-pro", "SuperMap ImageX Pro", "桌面端 GIS", "跨平台遥感影像生产、处理与质检软件", "DOM/DSM/DEM", "遥感生产", "影像质检"),
            item("transformx", "SuperMap TransformX", "桌面端 GIS", "跨平台二维、三维空间数据批量转换与处理软件", "数据转换", "批量处理", "国产化"),
            item("iclient-js", "SuperMap iClient JavaScript", "Web 端 GIS", "统一的现代 Web GIS JavaScript 客户端开发平台", "WebGIS", "二维可视化", "前端开发"),
            item("iclient3d", "SuperMap iClient3D for WebGL", "Web 端 GIS", "无插件、跨平台的 Web 三维 GIS 客户端开发平台", "WebGL", "三维可视化", "空间分析"),
            item("imobile", "SuperMap iMobile for Android / iOS", "移动端 GIS", "面向在线离线、二三维移动 GIS 应用的全功能开发平台", "移动采集", "AR 地图", "路径导航", "移动三维"),
            item("imobile-lite", "SuperMap iMobile Lite for Android / iOS", "移动端 GIS", "面向在线应用的开源轻量移动 GIS SDK", "轻量 SDK", "在线底图", "在线分析")
    );
    private final Map<String, CatalogItem> byId = catalog.stream().collect(Collectors.toUnmodifiableMap(CatalogItem::id, Function.identity()));

    public List<CatalogItem> list() { return catalog; }

    public List<ProjectDtos.ProductMatch> match(String demand, String evidence) {
        String selectionText = demand.toLowerCase(Locale.ROOT);
        String capabilityText = (demand + "\n" + evidence).toLowerCase(Locale.ROOT);
        LinkedHashSet<String> ids = new LinkedHashSet<>(List.of("iserver", "iportal", "idesktopx", "iclient-js"));
        add(ids, selectionText, List.of("云原生", "kubernetes", "运维", "监控"), "imanager");
        add(ids, selectionText, List.of("智能体", "大模型", "知识库", "agent", "ai"), "agentx-server", "iobjects-python");
        add(ids, selectionText, List.of("三维", "二三维", "数字孪生", "bim", "ue5", "unity"), "iclient3d", "hifi-3d", "iexplorer3d");
        add(ids, selectionText, List.of("遥感", "影像", "dom", "dem", "dsm"), "imagex-server", "imagex-pro");
        add(ids, selectionText, List.of("边缘", "低时延", "缓存加速"), "iedge");
        add(ids, selectionText, List.of("大数据", "spark", "分布式分析"), "iobjects-spark");
        add(ids, selectionText, List.of("java", "spring", "二次开发", "业务集成"), "iobjects-java");
        add(ids, selectionText, List.of(".net", "c#"), "iobjects-dotnet");
        add(ids, selectionText, List.of("c++"), "iobjects-cpp");
        add(ids, selectionText, List.of("移动", "外业", "采集", "android", "ios"), "imobile", "imobile-lite");
        add(ids, selectionText, List.of("转换", "格式转换", "数据迁移"), "transformx");
        add(ids, selectionText, List.of("海图", "海洋", "航海"), "imaritime");
        add(ids, selectionText, List.of("公有云", "在线gis", "数据上云"), "online");

        List<ProjectDtos.ProductMatch> matches = new ArrayList<>();
        int index = 0;
        for (String id : ids) {
            CatalogItem product = byId.get(id);
            if (product == null) continue;
            List<String> capabilities = product.capabilities().stream()
                    .filter(capability -> capabilityText.contains(capability.substring(0, Math.min(2, capability.length())).toLowerCase(Locale.ROOT)))
                    .toList();
            if (capabilities.isEmpty()) capabilities = product.capabilities().subList(0, Math.min(3, product.capabilities().size()));
            int score = Math.max(68, 96 - index * 3 + Math.min(2, capabilities.size()));
            boolean recommended = index < 6;
            matches.add(new ProjectDtos.ProductMatch(product.id(), product.name(), product.category(), Math.min(98, score),
                    capabilities, recommended ? List.of("部署规模与许可组合需在深化设计阶段确认") : List.of("按专项需求选配"), recommended));
            index++;
        }
        return matches;
    }

    private static CatalogItem item(String id, String name, String category, String description, String... capabilities) {
        return new CatalogItem(id, name, category, description, List.of(capabilities), OFFICIAL_URL);
    }

    private static void add(LinkedHashSet<String> ids, String text, List<String> keywords, String... productIds) {
        if (keywords.stream().anyMatch(text::contains)) ids.addAll(List.of(productIds));
    }
}
