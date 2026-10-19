package com.example.banks.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * SQL规则执行结果
 */
public class SqlRuleResult {
    private ParsedSqlRule rule;
    private Map<Long, Map<String, Double>> matchedAccounts = new HashMap<>();

    public ParsedSqlRule getRule() { return rule; }
    public void setRule(ParsedSqlRule rule) { this.rule = rule; }

    public Map<Long, Map<String, Double>> getMatchedAccounts() { return matchedAccounts; }

    public void setMatchedAccounts(Map<Long, Map<String, Double>> matchedAccounts) {
        this.matchedAccounts = matchedAccounts;
    }

    public void addMatchedAccount(Long accountId, Map<String, Double> indicators) {
        this.matchedAccounts.put(accountId, indicators);
    }
}


