/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.graphmdl.sqlrewrite.analyzer;

import io.graphmdl.base.CatalogSchemaTableName;
import io.graphmdl.base.GraphMDL;
import io.graphmdl.base.SessionContext;
import io.trino.sql.tree.AliasedRelation;
import io.trino.sql.tree.DefaultTraversalVisitor;
import io.trino.sql.tree.Node;
import io.trino.sql.tree.QualifiedName;
import io.trino.sql.tree.Table;
import io.trino.sql.tree.TableSubquery;

import static io.graphmdl.sqlrewrite.Utils.toCatalogSchemaTableName;

public class ScopeAnalyzer
{
    private ScopeAnalyzer()
    {
    }

    public static ScopeAnalysis analyze(GraphMDL graphMDL, Node node, SessionContext sessionContext)
    {
        ScopeAnalysis analysis = new ScopeAnalysis();
        Visitor visitor = new Visitor(graphMDL, analysis, sessionContext);
        visitor.process(node, null);
        return analysis;
    }

    static class Visitor
            extends DefaultTraversalVisitor<Void>
    {
        private final GraphMDL graphMDL;
        private final ScopeAnalysis analysis;
        private final SessionContext sessionContext;

        public Visitor(GraphMDL graphMDL, ScopeAnalysis analysis, SessionContext sessionContext)
        {
            this.graphMDL = graphMDL;
            this.analysis = analysis;
            this.sessionContext = sessionContext;
        }

        @Override
        protected Void visitTable(Table node, Void context)
        {
            if (isBelongToGraphMDL(node.getName())) {
                analysis.addUsedGraphMDLObject(node);
            }
            return null;
        }

        @Override
        protected Void visitTableSubquery(TableSubquery node, Void context)
        {
            return null;
        }

        @Override
        protected Void visitAliasedRelation(AliasedRelation node, Void context)
        {
            analysis.addAliasedNode(node.getRelation(), node.getAlias().getValue());
            return super.visitAliasedRelation(node, context);
        }

        private boolean isBelongToGraphMDL(QualifiedName graphMDLObjectName)
        {
            CatalogSchemaTableName catalogSchemaTableName = toCatalogSchemaTableName(sessionContext, graphMDLObjectName);
            String tableName = catalogSchemaTableName.getSchemaTableName().getTableName();
            return catalogSchemaTableName.getCatalogName().equals(graphMDL.getCatalog())
                    && catalogSchemaTableName.getSchemaTableName().getSchemaName().equals(graphMDL.getSchema())
                    && (graphMDL.listModels().stream().anyMatch(model -> model.getName().equals(tableName))
                    || graphMDL.listMetrics().stream().anyMatch(metric -> metric.getName().equals(tableName)));
        }
    }
}