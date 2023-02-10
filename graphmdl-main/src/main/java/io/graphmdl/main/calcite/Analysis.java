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

package io.graphmdl.main.calcite;

import io.trino.sql.tree.QualifiedName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Analysis
{
    private final Set<QualifiedName> visitedTables = new HashSet<>();

    public void addVisitedTable(QualifiedName tableName)
    {
        visitedTables.add(tableName);
    }

    public List<QualifiedName> getVisitedTables()
    {
        return List.copyOf(visitedTables);
    }
}