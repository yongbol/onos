/*
 * Copyright 2017-present Open Networking Laboratory
 *
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

package org.onosproject.p4runtime.ctl;

import com.google.common.collect.Maps;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.TextFormat;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.slf4j.Logger;
import p4.config.P4InfoOuterClass.P4Info;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility class to deal with pipeconfs in the context of P4runtime.
 */
final class PipeconfHelper {

    private static final Logger log = getLogger(PipeconfHelper.class);

    // TODO: consider implementing this via a cache that expires unused browsers.
    private static final Map<PiPipeconfId, P4InfoBrowser> BROWSERS = Maps.newConcurrentMap();
    private static final Map<PiPipeconfId, P4Info> P4INFOS = Maps.newConcurrentMap();

    private PipeconfHelper() {
        // hide.
    }

    /**
     * Extracts and returns a P4Info protobuf message from the given pipeconf. If the pipeconf does not define any
     * extension of type {@link PiPipeconf.ExtensionType#P4_INFO_TEXT}, returns null;
     *
     * @param pipeconf pipeconf
     * @return P4Info or null
     */
    static P4Info getP4Info(PiPipeconf pipeconf) {
        return P4INFOS.computeIfAbsent(pipeconf.id(), piPipeconfId -> {
            if (!pipeconf.extension(P4_INFO_TEXT).isPresent()) {
                log.warn("Missing P4Info extension in pipeconf {}", pipeconf.id());
                return null;
            }

            InputStream p4InfoStream = pipeconf.extension(P4_INFO_TEXT).get();
            P4Info.Builder p4iInfoBuilder = P4Info.newBuilder();
            try {
                TextFormat.getParser().merge(new InputStreamReader(p4InfoStream), ExtensionRegistry.getEmptyRegistry(),
                                             p4iInfoBuilder);
            } catch (IOException ex) {
                log.warn("Unable to parse P4Info of pipeconf {}: {}", pipeconf.id(), ex.getMessage());
                return null;
            }

            return p4iInfoBuilder.build();
        });
    }

    /**
     * Returns a P4Info browser for the given pipeconf. If the pipeconf does not define any extension of type
     * {@link PiPipeconf.ExtensionType#P4_INFO_TEXT}, returns null;
     *
     * @param pipeconf pipeconf
     * @return P4Info browser or null
     */
    static P4InfoBrowser getP4InfoBrowser(PiPipeconf pipeconf) {
        return BROWSERS.computeIfAbsent(pipeconf.id(), (pipeconfId) -> {
            P4Info p4info = PipeconfHelper.getP4Info(pipeconf);
            if (p4info == null) {
                return null;
            } else {
                return new P4InfoBrowser(p4info);
            }
        });
    }
}
