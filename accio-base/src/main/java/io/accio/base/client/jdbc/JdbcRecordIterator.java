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

package io.accio.base.client.jdbc;

import io.accio.base.Parameter;
import io.accio.base.client.Client;

import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class JdbcRecordIterator
        extends BaseJdbcRecordIterator<Object[]>
{
    public static JdbcRecordIterator of(Client client, String sql)

            throws SQLException
    {
        return of(client, sql, emptyList());
    }

    public static JdbcRecordIterator of(Client client, String sql, List<Parameter> parameters)
            throws SQLException
    {
        return new JdbcRecordIterator(client, sql, parameters);
    }

    private JdbcRecordIterator(Client client, String sql, List<Parameter> parameters)
            throws SQLException
    {
        super(client, sql, parameters);
    }

    @Override
    public Object[] getCurrentRecord()
            throws SQLException
    {
        List<Object> builder = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            if (resultSet.getMetaData().getColumnType(i) == Types.BLOB) {
                Blob blob = resultSet.getBlob(i);
                byte[] bytes = blob.getBytes(0, (int) blob.length());
                builder.add(bytes);
            }
            else if (resultSet.getMetaData().getColumnType(i) == Types.SMALLINT) {
                builder.add(resultSet.getShort(i));
            }
            else {
                builder.add(resultSet.getObject(i));
            }
        }
        return builder.toArray();
    }
}
