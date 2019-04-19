/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shardingsphere.transaction.saga.revert.execute.insert;

import com.google.common.base.Optional;
import io.shardingsphere.transaction.saga.revert.engine.RevertSQLUnit;
import io.shardingsphere.transaction.saga.revert.execute.RevertSQLExecuteWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.parse.antlr.sql.statement.dml.InsertStatement;
import org.apache.shardingsphere.core.parse.old.lexer.token.DefaultKeyword;
import org.apache.shardingsphere.core.parse.old.parser.context.insertvalue.InsertValue;
import org.apache.shardingsphere.core.parse.old.parser.expression.SQLExpression;
import org.apache.shardingsphere.core.parse.old.parser.expression.SQLNumberExpression;
import org.apache.shardingsphere.core.parse.old.parser.expression.SQLPlaceholderExpression;
import org.apache.shardingsphere.core.parse.old.parser.expression.SQLTextExpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Insert revert SQL execute wrapper.
 *
 * @author duhongjun
 * @author zhaojun
 */
@RequiredArgsConstructor
public final class InsertRevertSQLExecuteWrapper implements RevertSQLExecuteWrapper<InsertRevertSQLContext> {
    
    private final String actualTable;
    
    private final InsertStatement insertStatement;
    
    private final List<Object> actualSQLParameters;
    
    private final List<String> primaryKeyColumns;
    
    private final boolean containGenerateKey;
    
    @Override
    public InsertRevertSQLContext createRevertSQLContext() {
        InsertRevertSQLContext result = new InsertRevertSQLContext(actualTable, insertStatement.getColumnNames(),
            primaryKeyColumns, actualSQLParameters, insertStatement.getValues().size(), containGenerateKey);
        Iterator<String> columnNamesIterator = insertStatement.getColumnNames().iterator();
        Iterator actualSQLParameterIterator = actualSQLParameters.iterator();
        for (InsertValue each : insertStatement.getValues()) {
            result.getInvertValues().add(createInsertGroup(each, columnNamesIterator, actualSQLParameterIterator, primaryKeyColumns));
        }
        return result;
    }
    
    @Override
    public Optional<RevertSQLUnit> generateRevertSQL(final InsertRevertSQLContext insertRevertSQLContext) {
        RevertSQLUnit result = new RevertSQLUnit(generateSQL(insertRevertSQLContext));
        fillRevertParams(result, insertRevertSQLContext);
        return Optional.of(result);
    }
    
    private Map<String, Object> createInsertGroup(final InsertValue insertValue, final Iterator<String> columnNamesIterator, final Iterator actualSQLParameterIterator, final List<String> keys) {
        Map<String, Object> result = new HashMap<>();
        for (SQLExpression expression : insertValue.getAssignments()) {
            String columnName = columnNamesIterator.next();
            if (!keys.contains(columnName)) {
                continue;
            }
            if (expression instanceof SQLPlaceholderExpression) {
                result.put(columnName, actualSQLParameterIterator.next());
            } else if (expression instanceof SQLTextExpression) {
                result.put(columnName, ((SQLTextExpression) expression).getText());
            } else if (expression instanceof SQLNumberExpression) {
                result.put(columnName, ((SQLNumberExpression) expression).getNumber());
            }
        }
        return result;
    }
    
    private String generateSQL(final InsertRevertSQLContext revertSQLContext) {
        StringBuilder builder = new StringBuilder();
        builder.append(DefaultKeyword.DELETE).append(" ");
        builder.append(DefaultKeyword.FROM).append(" ");
        builder.append(revertSQLContext.getActualTable()).append(" ");
        builder.append(DefaultKeyword.WHERE).append(" ");
        boolean firstItem = true;
        for (String each : revertSQLContext.getPrimaryKeys()) {
            if (firstItem) {
                firstItem = false;
                builder.append(" ").append(each).append(" =?");
            } else {
                builder.append(" ").append(DefaultKeyword.AND).append(each).append(" =?");
            }
        }
        return builder.toString();
    }
    
    private void fillRevertParams(final RevertSQLUnit revertSQLUnit, final InsertRevertSQLContext revertSQLContext) {
        if (revertSQLContext.isContainGenerateKey()) {
            int eachParameterSize = revertSQLContext.getParameters().size() / revertSQLContext.getBatchSize();
            for (int i = 0; i < revertSQLContext.getBatchSize(); i++) {
                Collection<Object> revertParameters = new LinkedList<>();
                int primaryKeyIndex = (i + 1) * eachParameterSize - 1;
                revertParameters.add(revertSQLContext.getParameters().get(primaryKeyIndex));
                revertSQLUnit.getRevertParams().add(revertParameters);
            }
            return;
        }
        for (Map<String, Object> each : revertSQLContext.getInvertValues()) {
            revertSQLUnit.getRevertParams().add(each.values());
        }
    }
}
