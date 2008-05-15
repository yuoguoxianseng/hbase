/**
 * Copyright 2008 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hbase.client;

import java.io.IOException;

import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.filter.RowFilterInterface;
import org.apache.hadoop.hbase.io.RowResult;
import org.apache.hadoop.io.Text;
/**
 * Retryable scanner
 */
public class ScannerCallable extends ServerCallable<RowResult> {
  private long scannerId = -1L;
  private boolean instantiated = false;
  private boolean closed = false;
  private final Text[] columns;
  private final long timestamp;
  private final RowFilterInterface filter;

  ScannerCallable (HConnection connection, Text tableName, Text[] columns,
      Text startRow, long timestamp, RowFilterInterface filter) {
    super(connection, tableName, startRow);
    this.columns = columns;
    this.timestamp = timestamp;
    this.filter = filter;
  }
  
  /**
   * @param reload
   * @throws IOException
   */
  @Override
  public void instantiateServer(boolean reload) throws IOException {
    if (!instantiated || reload) {
      super.instantiateServer(reload);
      instantiated = true;
    }
  }
  
  /** {@inheritDoc} */
  public RowResult call() throws IOException {
    if (scannerId != -1L && closed) {
      server.close(scannerId);
      scannerId = -1L;
    } else if (scannerId == -1L && !closed) {
      // open the scanner
      scannerId = server.openScanner(
          this.location.getRegionInfo().getRegionName(), columns, row,
          timestamp, filter);
    } else {
      return server.next(scannerId);
    }
    return null;
  }
  
  /**
   * Call this when the next invocation of call should close the scanner
   */
  public void setClose() {
    closed = true;
  }
  
  /**
   * @return the HRegionInfo for the current region
   */
  public HRegionInfo getHRegionInfo() {
    if (!instantiated) {
      return null;
    }
    return location.getRegionInfo();
  }
}