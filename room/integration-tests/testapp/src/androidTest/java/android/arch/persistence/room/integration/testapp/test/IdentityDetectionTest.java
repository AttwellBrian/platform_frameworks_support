/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.arch.persistence.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.integration.testapp.TestDatabase;
import android.arch.persistence.room.integration.testapp.vo.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class IdentityDetectionTest {
    static final String TAG = "IdentityDetectionTest";
    static final String DB_FILE_NAME = "identity_test_db";
    TestDatabase mTestDatabase;
    @Before
    public void createTestDatabase() {
        deleteDbFile();
    }

    @Test
    public void reOpenWithoutIssues() {
        openDb();
        mTestDatabase.getUserDao().insert(TestUtil.createUser(3));
        closeDb();
        openDb();
        User[] users = mTestDatabase.getUserDao().loadByIds(3);
        assertThat(users.length, is(1));
    }

    @Test
    public void reOpenChangedHash() {
        openDb();
        mTestDatabase.getUserDao().insert(TestUtil.createUser(3));
        // change the hash
        SupportSQLiteDatabase db = mTestDatabase.getOpenHelper().getWritableDatabase();
        db.execSQL("UPDATE " + Room.MASTER_TABLE_NAME + " SET `identity_hash` = ?"
                + " WHERE id = 42", new String[]{"bad hash"});
        closeDb();
        Throwable[] exceptions = new Throwable[1];
        try {
            openDb();
            mTestDatabase.getUserDao().loadByIds(3);
        } catch (Throwable t) {
            exceptions[0] = t;
            mTestDatabase = null;
        }
        assertThat(exceptions[0], instanceOf(IllegalStateException.class));
    }

    private void closeDb() {
        mTestDatabase.getOpenHelper().close();
    }

    private void openDb() {
        mTestDatabase = Room.databaseBuilder(InstrumentationRegistry.getTargetContext(),
                TestDatabase.class, DB_FILE_NAME).build();
    }

    @After
    public void clear() {
        try {
            if (mTestDatabase != null) {
                closeDb();
            }
            deleteDbFile();
        } finally {
            Log.e(TAG, "could not close test database");
        }
    }

    private void deleteDbFile() {
        File testDb = InstrumentationRegistry.getTargetContext().getDatabasePath(DB_FILE_NAME);
        testDb.delete();
    }
}
