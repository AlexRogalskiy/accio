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

package io.accio.main.server.module;

import com.google.inject.Binder;
import io.accio.main.web.AccioExceptionMapper;
import io.accio.main.web.PreAggregationResource;
import io.accio.main.web.ReloadResource;
import io.airlift.configuration.AbstractConfigurationAwareModule;

import static io.airlift.jaxrs.JaxrsBinder.jaxrsBinder;

public class WebModule
        extends AbstractConfigurationAwareModule
{
    @Override
    protected void setup(Binder binder)
    {
        jaxrsBinder(binder).bind(ReloadResource.class);
        jaxrsBinder(binder).bind(PreAggregationResource.class);
        jaxrsBinder(binder).bindInstance(new AccioExceptionMapper());
    }
}
