/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.android.apps.forscience.whistlepunk.sensordb;

import android.test.AndroidTestCase;

import com.google.android.apps.forscience.whistlepunk.Arbitrary;
import com.google.common.collect.Range;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class SensorDatabaseTest extends AndroidTestCase {
    private static final String TEST_DATABASE_NAME = "test.db";

    public void testAddScalarReading() {
        SensorDatabaseImpl db = new SensorDatabaseImpl(getContext(), TEST_DATABASE_NAME);
        long timestamp = Arbitrary.integer();
        double value = Arbitrary.doubleFloat();
        db.addScalarReading("tag", 0, timestamp, value);
        List<ScalarReading> readings = ScalarReading.slurp(db.getScalarReadings("tag",
                TimeRange.oldest(Range.closed(timestamp - 1, timestamp + 1)), 0, 0));
        assertEquals(Arrays.asList(new ScalarReading(timestamp, value)), readings);
    }

    public void testAddScalarReadingLimits() {
        SensorDatabaseImpl db = new SensorDatabaseImpl(getContext(), TEST_DATABASE_NAME);
        db.addScalarReading("tag", 0, 1, 1.0);
        db.addScalarReading("tag", 0, 2, 2.0);
        db.addScalarReading("tag", 0, 3, 3.0);
        int limit = 2;
        List<ScalarReading> readings = ScalarReading.slurp(db.getScalarReadings("tag",
                TimeRange.oldest(Range.closed(0L, 4L)), 0, limit));
        assertEquals(Arrays.asList(new ScalarReading(1, 1.0), new ScalarReading(2, 2.0)), readings);
    }

    public void testAddScalarReadingNoLimits() {
        SensorDatabaseImpl db = new SensorDatabaseImpl(getContext(), TEST_DATABASE_NAME);
        db.addScalarReading("tag", 0, 1, 1.0);
        db.addScalarReading("tag", 0, 2, 2.0);
        db.addScalarReading("tag", 0, 3, 3.0);
        List<ScalarReading> readings = ScalarReading.slurp(db.getScalarReadings("tag",
                TimeRange.oldest(Range.<Long>all()), 0, 0));
        assertEquals(Arrays.asList(new ScalarReading(1, 1.0), new ScalarReading(2, 2.0),
                new ScalarReading(3, 3.0)), readings);
    }

    public void testAddScalarReadingTags() {
        SensorDatabaseImpl db = new SensorDatabaseImpl(getContext(), TEST_DATABASE_NAME);
        db.addScalarReading("tag", 0, 1, 1.0);
        db.addScalarReading("tag", 0, 2, 2.0);
        db.addScalarReading("other", 0, 3, 3.0);
        List<ScalarReading> readings = ScalarReading.slurp(db.getScalarReadings("tag",
                TimeRange.oldest(Range.closed(0L, 4L)), 0, 0));
        assertEquals(Arrays.asList(new ScalarReading(1, 1.0), new ScalarReading(2, 2.0)), readings);
    }

    public void testAddScalarReadingLimitsNewestFirst() {
        SensorDatabaseImpl db = new SensorDatabaseImpl(getContext(), TEST_DATABASE_NAME);
        db.addScalarReading("tag", 0, 1, 1.0);
        db.addScalarReading("tag", 0, 2, 2.0);
        db.addScalarReading("tag", 0, 3, 3.0);
        int limit = 2;
        List<ScalarReading> readings = ScalarReading.slurp(db.getScalarReadings("tag",
                TimeRange.newest(Range.closed(0L, 4L)), 0, limit));
        assertEquals(Arrays.asList(new ScalarReading(3, 3.0), new ScalarReading(2, 2.0)), readings);
    }

    public void testAddScalarReadingRange() {
        SensorDatabaseImpl db = new SensorDatabaseImpl(getContext(), TEST_DATABASE_NAME);
        db.addScalarReading("tag", 0, 1, 1.0);
        db.addScalarReading("tag", 0, 2, 2.0);
        db.addScalarReading("tag", 0, 3, 3.0);
        db.addScalarReading("tag", 0, 4, 4.0);
        List<ScalarReading> readings = ScalarReading.slurp(db.getScalarReadings("tag",
                TimeRange.oldest(Range.closedOpen(2L, 4L)), 0, 0));
        assertEquals(Arrays.asList(new ScalarReading(2, 2.0), new ScalarReading(3, 3.0)), readings);

        readings = ScalarReading.slurp(db.getScalarReadings("tag",
                TimeRange.oldest(Range.open(2L, 4L)), 0, 0));
        assertEquals(Arrays.asList(new ScalarReading(3, 3.0)), readings);

        readings = ScalarReading.slurp(db.getScalarReadings("tag",
                TimeRange.oldest(Range.openClosed(2L, 4L)), 0, 0));
        assertEquals(Arrays.asList(new ScalarReading(3, 3.0), new ScalarReading(4, 4.0)), readings);
    }

    public void testTiers() {
        SensorDatabaseImpl db = new SensorDatabaseImpl(getContext(), TEST_DATABASE_NAME);
        db.addScalarReading("tag", 0, 0, 0.0);
        db.addScalarReading("tag", 1, 1, 1.0);

        List<ScalarReading> tier0 = ScalarReading.slurp(db.getScalarReadings("tag",
                TimeRange.oldest(Range.<Long>all()), 0, 0));
        assertEquals(Arrays.asList(new ScalarReading(0, 0.0)), tier0);

        List<ScalarReading> tier1 = ScalarReading.slurp(db.getScalarReadings("tag",
                TimeRange.oldest(Range.<Long>all()), 1, 0));
        assertEquals(Arrays.asList(new ScalarReading(1, 1.0)), tier1);
    }

    public void testFirstTagAfter() {
        SensorDatabaseImpl db = new SensorDatabaseImpl(getContext(), TEST_DATABASE_NAME);
        db.addScalarReading("tagBefore", 0, 1, 1.0);
        db.addScalarReading("tagAfter", 0, 3, 2.0);
        assertEquals("tagAfter", db.getFirstDatabaseTagAfter(2));
    }

    public void testFirstTagAfterWithMultipleAfters() {
        SensorDatabaseImpl db = new SensorDatabaseImpl(getContext(), TEST_DATABASE_NAME);
        db.addScalarReading("tagBefore", 0, 1, 1.0);
        db.addScalarReading("tagAfter", 0, 3, 2.0);
        db.addScalarReading("tagFurtherAfter", 0, 5, 3.0);
        assertEquals("tagAfter", db.getFirstDatabaseTagAfter(2));
    }

    public void testDeleteReadings() {
        SensorDatabaseImpl db = new SensorDatabaseImpl(getContext(), TEST_DATABASE_NAME);
        db.addScalarReading("tag", 0, 0, 0.0);
        db.addScalarReading("tag", 0, 1, 1.0);
        db.addScalarReading("tag", 0, 101, 2.0);
        db.addScalarReading("tag", 0, 102, 2.0);
        db.addScalarReading("tag", 0, 103, 2.0);
        db.addScalarReading("tag2", 0, 0, 1.0);

        assertEquals(2, db.getScalarReadings("tag",
                TimeRange.oldest(Range.closed(0L, 1L)), 0, 0).size());

        assertEquals(3, db.getScalarReadings("tag",
                TimeRange.oldest(Range.closed(101L, 103L)), 0, 0).size());

        assertEquals(1, db.getScalarReadings("tag2",
                TimeRange.oldest(Range.closed(0L, 1L)), 0, 0).size());

        // Delete first set of readings.
        db.deleteScalarReadings("tag", TimeRange.newest(Range.closed(0L, 1L)));

        assertEquals(0, db.getScalarReadings("tag",
                TimeRange.oldest(Range.closed(0L, 1L)), 0, 0).size());

        // Make sure other records for that tag are unaffected.
        assertEquals(3, db.getScalarReadings("tag",
                TimeRange.oldest(Range.closed(101L, 103L)), 0, 0).size());

        // Make sure tag 2 is unaffected.
        assertEquals(1, db.getScalarReadings("tag2",
                TimeRange.oldest(Range.closed(0L, 1L)), 0, 0).size());

    }

    @Override
    protected void setUp() throws Exception {
        File dbtest = getContext().getDatabasePath(TEST_DATABASE_NAME);
        if (dbtest.exists()) {
            dbtest.delete();
            fail("Found DB at " + dbtest + " that should have been cleaned up last time.");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        getContext().getDatabasePath(TEST_DATABASE_NAME).delete();
    }
}
