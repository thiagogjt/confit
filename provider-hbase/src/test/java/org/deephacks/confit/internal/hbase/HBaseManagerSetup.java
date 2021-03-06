/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.confit.internal.hbase;

import org.deephacks.confit.spi.BeanManager;
import org.deephacks.confit.test.FeatureSetupTeardown;
import org.junit.Before;

@FeatureSetupTeardown(BeanManager.class)
public class HBaseManagerSetup {

    @Before
    public void before() throws Exception {
        HBaseUtil.deleteAllRows();
    }

}
