/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.core.util;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author andrew00x
 */
public class FileLineConsumer implements LineConsumer {
    private final File   file;
    private final Writer writer;
    private final AtomicBoolean isClosed;

    public FileLineConsumer(File file) throws IOException {
        this.file = file;
        writer = Files.newBufferedWriter(file.toPath(), Charset.defaultCharset());
        isClosed = new AtomicBoolean(false);
    }

    public File getFile() {
        return file;
    }

    @Override
    public void writeLine(String line) throws IOException {
        if (!isClosed.get()) {
            try {
                if (line != null) {
                    writer.write(line);
                }
                writer.write('\n');
                writer.flush();
            } catch (IOException e) {
                if ("Stream closed".equals(e.getMessage())) {
                    throw new ConsumerAlreadyClosedException(e.getMessage());
                }
                throw e;
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            writer.close();
        }
    }
}
