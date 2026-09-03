package vn.rikkei.exam.restaurantreservation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
public class McpClientConfig {

    @Value("${mcp.antigravity.enabled:false}")
    private boolean enabled;

    @Value("${mcp.antigravity.endpoint:}")
    private String endpoint;

    @Value("${mcp.antigravity.token:}")
    private String token;

    @Bean
    public SyncMcpToolCallbackProvider mcpToolCallbackProvider() {
        if (!enabled) {
            log.info("[MCP] MCP Antigravity không được bật (enabled=false). Bỏ qua kết nối.");
            return new SyncMcpToolCallbackProvider(List.of());
        }

        if (endpoint == null || endpoint.isBlank()) {
            log.warn("[MCP] MCP_ANTIGRAVITY_ENDPOINT chưa cấu hình. Bỏ qua kết nối.");
            return new SyncMcpToolCallbackProvider(List.of());
        }

        if (token == null || token.isBlank()) {
            log.warn("[MCP] MCP_ANTIGRAVITY_TOKEN chưa cấu hình. Bỏ qua kết nối.");
            return new SyncMcpToolCallbackProvider(List.of());
        }

        try {
            log.info("[MCP] Đang kết nối MCP Antigravity tại endpoint: {}", endpoint);

            // Cấu hình Authorization Bearer token an toàn qua requestBuilder, không log token
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + token);

            McpClientTransport transport = HttpClientSseClientTransport.builder(endpoint)
                    .requestBuilder(reqBuilder)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            McpSyncClient mcpClient = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(15))
                    .build();

            mcpClient.initialize();
            log.info("[MCP] Kết nối MCP Antigravity thành công!");

            return new SyncMcpToolCallbackProvider(List.of(mcpClient));

        } catch (Exception e) {
            // Xử lý và log lỗi rõ ràng khi MCP không sẵn sàng, bảo mật không lộ token
            log.error("[MCP] Kết nối MCP Antigravity thất bại (dịch vụ chưa sẵn sàng hoặc lỗi cấu hình): {}. Tiếp tục khởi chạy không có MCP.",
                    e.getMessage());
            return new SyncMcpToolCallbackProvider(List.of());
        }
    }
}
