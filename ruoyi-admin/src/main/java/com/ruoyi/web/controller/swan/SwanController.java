package com.ruoyi.web.controller.swan;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.swan.service.ISwanApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Swan Partner API 管理控制器
 */
@RestController
@RequestMapping("/swan")
public class SwanController extends BaseController {

    @Autowired
    private ISwanApiService swanApiService;

    /**
     * 获取 Swan 账户列表
     */
    @PreAuthorize("@ss.hasPermi('swan:account:list')")
    @GetMapping("/accounts")
    public AjaxResult listAccounts(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String after) {
        Map<String, Object> result = swanApiService.listAccounts(pageSize, after);
        return AjaxResult.success(result);
    }

    /**
     * 获取指定账户的交易列表
     */
    @PreAuthorize("@ss.hasPermi('swan:transaction:list')")
    @GetMapping("/transactions")
    public AjaxResult listTransactions(
            @RequestParam String accountId,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String after) {
        Map<String, Object> result = swanApiService.listTransactions(accountId, pageSize, after);
        return AjaxResult.success(result);
    }

    /**
     * 获取 Onboarding 列表
     */
    @PreAuthorize("@ss.hasPermi('swan:onboarding:list')")
    @GetMapping("/onboardings")
    public AjaxResult listOnboardings(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String after) {
        Map<String, Object> result = swanApiService.listOnboardings(pageSize, after);
        return AjaxResult.success(result);
    }

    /**
     * 健康检查 / Token 验证
     */
    @PreAuthorize("@ss.hasPermi('swan:account:list')")
    @GetMapping("/ping")
    public AjaxResult ping() {
        String token = swanApiService.getAccessToken();
        if (token != null) {
            return AjaxResult.success("Swan API connected");
        }
        return AjaxResult.error("Failed to connect Swan API");
    }
}
