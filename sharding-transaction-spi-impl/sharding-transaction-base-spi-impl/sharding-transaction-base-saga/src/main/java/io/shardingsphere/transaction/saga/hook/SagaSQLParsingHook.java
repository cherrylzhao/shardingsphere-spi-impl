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

package io.shardingsphere.transaction.saga.hook;

import io.shardingsphere.transaction.saga.SagaShardingTransactionManager;
import io.shardingsphere.transaction.saga.context.SagaTransaction;
import org.apache.shardingsphere.core.metadata.table.ShardingTableMetaData;
import org.apache.shardingsphere.core.parse.hook.ParsingHook;
import org.apache.shardingsphere.core.parse.parser.sql.SQLStatement;

/**
 * Saga SQL parsing hook.
 *
 * @author yangyi
 */
public final class SagaSQLParsingHook implements ParsingHook {
    
    private final SagaTransaction sagaTransaction = SagaShardingTransactionManager.getCurrentTransaction();
    
    private String sql;
    
    @Override
    public void start(final String sql) {
        this.sql = sql;
    }
    
    @Override
    public void finishSuccess(final SQLStatement sqlStatement, final ShardingTableMetaData shardingTableMetaData) {
        if (null != sagaTransaction) {
            sagaTransaction.nextBranchTransactionGroup(sql, sqlStatement, shardingTableMetaData);
        }
    }
    
    @Override
    public void finishFailure(final Exception cause) {
    }
}
