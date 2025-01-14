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
package io.accio.testing.bigquery;

import io.accio.base.CatalogSchemaTableName;
import io.accio.preaggregation.TaskInfo;
import io.accio.preaggregation.dto.PreAggregationTable;
import io.airlift.units.Duration;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.accio.base.metadata.StandardErrorCode.NOT_FOUND;
import static io.accio.preaggregation.TaskInfo.TaskStatus.DONE;
import static io.accio.preaggregation.TaskInfo.TaskStatus.RUNNING;
import static io.accio.testing.WebApplicationExceptionAssert.assertWebApplicationException;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestReloadPreAggregation
        extends AbstractPreAggregationTest
{
    private Path accioMDLFilePath;

    @Override
    protected Optional<String> getAccioMDLPath()
    {
        try {
            accioMDLFilePath = Files.createTempFile("acciomdl", ".json");
            rewriteFile("pre_agg/pre_agg_reload_1_mdl.json");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Optional.of(accioMDLFilePath.toString());
    }

    @Test
    public void testReloadPreAggregation()
            throws IOException, ExecutionException, InterruptedException, TimeoutException
    {
        String beforeName = "Revenue";
        CatalogSchemaTableName beforeCatalogSchemaTableName = new CatalogSchemaTableName("canner-cml", "tpch_tiny", beforeName);
        String beforeMappingName = getDefaultPreAggregationInfoPair(beforeName).getRequiredTableName();
        assertPreAggregation(beforeName);

        rewriteFile("pre_agg/pre_agg_reload_2_mdl.json");
        reloadAccioMDL();
        reloadPreAggregation();

        assertPreAggregation("Revenue_After");

        List<Object[]> tables = queryDuckdb("show tables");
        Set<String> tableNames = tables.stream().map(table -> table[0].toString()).collect(toImmutableSet());
        assertThat(tableNames).doesNotContain(beforeMappingName);
        assertThat(preAggregationManager.preAggregationScheduledFutureExists(beforeCatalogSchemaTableName)).isFalse();
        assertThatThrownBy(() -> getDefaultPreAggregationInfoPair(beforeMappingName).getRequiredTableName()).isInstanceOf(NullPointerException.class);

        rewriteFile("pre_agg/pre_agg_reload_1_mdl.json");
        reloadAccioMDL();
        TaskInfo taskInfo = reloadPreAggregationAsync();
        assertThat(taskInfo.getTaskStatus()).isEqualTo(RUNNING);
        taskInfo = waitUntilFinished(taskInfo.getTaskId());
        assertPreAggregation("Revenue");
        assertThat(taskInfo.getCatalogName()).isEqualTo("canner-cml");
        assertThat(taskInfo.getSchemaName()).isEqualTo("tpch_tiny");
        assertThat(taskInfo.getTaskStatus()).isEqualTo(DONE);
        assertThat(taskInfo.getStartTime()).isNotNull();
        assertThat(taskInfo.getEndTime()).isNotNull();
        assertThat(taskInfo.getEndTime()).isAfter(taskInfo.getStartTime());

        PreAggregationTable preAggregationTable = taskInfo.getPreAggregationTables().get(0);
        assertThat(preAggregationTable.getErrorMessage()).isEmpty();
        assertThat(preAggregationTable.getName()).isEqualTo("Revenue");
        assertThat(preAggregationTable.getRefreshTime()).isEqualTo(Duration.valueOf("5m"));
        assertThat(preAggregationTable.getCreateDate()).isNotNull();

        rewriteFile("pre_agg/pre_agg_reload_3_mdl.json");
        reloadAccioMDL();
        taskInfo = reloadPreAggregationAsync();
        assertThat(taskInfo.getTaskStatus()).isEqualTo(RUNNING);
        taskInfo = waitUntilFinished(taskInfo.getTaskId());
        preAggregationTable = taskInfo.getPreAggregationTables().get(0);
        assertThat(preAggregationTable.getErrorMessage()).isPresent();
        assertThat(taskInfo.getEndTime()).isAfter(taskInfo.getStartTime());

        assertWebApplicationException(() -> getTaskInfoByTaskId(randomUUID().toString()))
                .hasHTTPStatus(404)
                .hasErrorCode(NOT_FOUND)
                .hasErrorMessageMatches("Task .* not found.");
    }

    private void assertPreAggregation(String name)
    {
        CatalogSchemaTableName mapping = new CatalogSchemaTableName("canner-cml", "tpch_tiny", name);
        String mappingName = getDefaultPreAggregationInfoPair(name).getRequiredTableName();
        List<Object[]> tables = queryDuckdb("show tables");
        Set<String> tableNames = tables.stream().map(table -> table[0].toString()).collect(toImmutableSet());
        assertThat(tableNames).contains(mappingName);
        assertThat(preAggregationManager.preAggregationScheduledFutureExists(mapping)).isTrue();
    }

    private void rewriteFile(String resourcePath)
            throws IOException
    {
        Files.copy(Path.of(requireNonNull(getClass().getClassLoader().getResource(resourcePath)).getPath()), accioMDLFilePath, REPLACE_EXISTING);
    }

    private TaskInfo waitUntilFinished(String taskId)
            throws InterruptedException, ExecutionException, TimeoutException
    {
        return supplyAsync(() -> {
            TaskInfo taskInfo;
            do {
                taskInfo = getTaskInfoByTaskId(taskId);
                try {
                    SECONDS.sleep(1L);
                }
                catch (InterruptedException ignored) {
                }
            }
            while (taskInfo.inProgress());
            return taskInfo;
        }, Executors.newSingleThreadExecutor()).get(10, SECONDS);
    }
}
