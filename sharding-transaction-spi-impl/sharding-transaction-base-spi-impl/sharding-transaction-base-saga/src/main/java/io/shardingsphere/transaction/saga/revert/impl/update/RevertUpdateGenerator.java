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

package io.shardingsphere.transaction.saga.revert.impl.update;

import com.google.common.base.Optional;
import io.shardingsphere.transaction.saga.revert.api.RevertSQLUnit;
import io.shardingsphere.transaction.saga.revert.impl.RevertSQLGenerator;
import io.shardingsphere.transaction.saga.revert.impl.RevertSQLContext;
import org.apache.shardingsphere.core.parse.old.lexer.token.DefaultKeyword;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Revert update generator.
 *
 * @author duhongjun
 */
public final class RevertUpdateGenerator implements RevertSQLGenerator {
    
    @Override
    public Optional<RevertSQLUnit> generateRevertSQL(final RevertSQLContext parameter) {
        UpdateRevertSQLContext updateParameter = (UpdateRevertSQLContext) parameter;
        if (updateParameter.getUndoData().isEmpty()) {
            return Optional.absent();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(DefaultKeyword.UPDATE).append(" ");
        builder.append(updateParameter.getActualTable()).append(" ");
        builder.append(DefaultKeyword.SET).append(" ");
        int pos = 0;
        int size = updateParameter.getUpdateColumns().size();
        for (String updateColumn : updateParameter.getUpdateColumns().keySet()) {
            builder.append(updateColumn).append(" = ?");
            if (pos < size - 1) {
                builder.append(",");
            }
            pos++;
        }
        builder.append(" ").append(DefaultKeyword.WHERE).append(" ");
        return Optional.of(fillWhereWithKeys(updateParameter, builder));
    }
    
    private RevertSQLUnit fillWhereWithKeys(final UpdateRevertSQLContext updateParameter, final StringBuilder builder) {
        int pos = 0;
        for (String key : updateParameter.getPrimaryKeyColumns()) {
            if (pos > 0) {
                builder.append(" ").append(DefaultKeyword.AND).append(" ");
            }
            builder.append(key).append(" = ? ");
            pos++;
        }
        RevertSQLUnit result = new RevertSQLUnit(builder.toString());
        for (Map<String, Object> each : updateParameter.getUndoData()) {
            List<Object> eachSQLParams = new LinkedList<>();
            result.getRevertParams().add(eachSQLParams);
            for (String updateColumn : updateParameter.getUpdateColumns().keySet()) {
                eachSQLParams.add(each.get(updateColumn.toLowerCase()));
            }
            for (String key : updateParameter.getPrimaryKeyColumns()) {
                Object value = updateParameter.getUpdateColumns().get(key);
                if (null != value) {
                    eachSQLParams.add(value);
                } else {
                    eachSQLParams.add(each.get(key));
                }
            }
        }
        return result;
    }
}
