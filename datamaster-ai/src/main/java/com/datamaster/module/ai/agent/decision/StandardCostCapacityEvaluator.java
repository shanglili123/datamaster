package com.datamaster.module.ai.agent.decision;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ai.agent.decision.DecisionModels.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Fixed arithmetic model. No model-generated formula or executable expression is evaluated. */
@Component
public class StandardCostCapacityEvaluator {
    public void compute(CalculationResult result, BigDecimal demand, BigDecimal existing,
                        BigDecimal cost, BigDecimal additional, BigDecimal budget, boolean unlimited) {
        BigDecimal supply = existing.add(additional);
        result.setTotalCost(cost.toPlainString());
        result.setAdditionalSupply(additional.toPlainString());
        result.setAvailableSupply(supply.toPlainString());
        result.setRemainingGap(demand.subtract(supply).max(BigDecimal.ZERO).toPlainString());
        result.setCoveragePercent(demand.signum() == 0 ? "100" : supply.min(demand)
                .multiply(new BigDecimal("100")).divide(demand, 6, RoundingMode.HALF_UP).toPlainString());
        result.setBudgetStatus(unlimited ? "NOT_REQUIRED" : budget == null ? "UNKNOWN"
                : cost.compareTo(budget) <= 0 ? "PASS" : "FAIL");
        result.setStatus("COMPUTED");
        result.setFormula("cost=sum(quantity*unitPrice)+fixedCost; supply=existing+sum(quantity*outputPerUnit); gap=max(demand-supply,0)");
        result.getLimitations().add("固定费每候选计入一次；成本为干预增量，不是企业经营总成本");
        result.getLimitations().add("产出系数是已确认假设；不证明交期、物流、设备冲突或业务硬约束可行");
    }

    static BigDecimal decimal(String value) {
        if (value == null || !value.matches("(?:0|[1-9][0-9]{0,17})(?:\\.[0-9]{1,8})?"))
            throw new ServiceException("测算值必须为非负十进制字符串，最多18位整数和8位小数");
        return new BigDecimal(value);
    }
}
