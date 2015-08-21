/*
 *     Copyright 2015 IBM Corp.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.ibm.bms.clientsdk.android.core;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

/**
 * Created by cirilla on 8/12/15.
 */
public class TempTest extends TestCase {

    @SmallTest
    public void testMultiply() {

        assertEquals("10 x 5 must be 50", 50, 10 * 5);
    }
}
