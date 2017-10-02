/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.sqlbrite3;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.util.Arrays;
import java.util.Collection;

import io.reactivex.functions.Function;

import static com.squareup.sqlbrite3.TestDb.EmployeeTable.ID;
import static com.squareup.sqlbrite3.TestDb.EmployeeTable.NAME;
import static com.squareup.sqlbrite3.TestDb.EmployeeTable.USERNAME;
import static com.squareup.sqlbrite3.TestDb.ManagerTable.EMPLOYEE_ID;
import static com.squareup.sqlbrite3.TestDb.ManagerTable.MANAGER_ID;
import static net.sqlcipher.database.SQLiteDatabase.CONFLICT_FAIL;

final class TestDb extends SQLiteOpenHelper {
  static final String TABLE_EMPLOYEE = "employee";
  static final String TABLE_MANAGER = "manager";

  static final String SELECT_EMPLOYEES =
      "SELECT " + USERNAME + ", " + NAME + " FROM " + TABLE_EMPLOYEE;
  static final String SELECT_MANAGER_LIST = ""
      + "SELECT e." + NAME + ", m." + NAME + " "
      + "FROM " + TABLE_MANAGER + " AS manager "
      + "JOIN " + TABLE_EMPLOYEE + " AS e "
      + "ON manager." + EMPLOYEE_ID + " = e." + ID + " "
      + "JOIN " + TABLE_EMPLOYEE + " as m "
      + "ON manager." + MANAGER_ID + " = m." + ID;
  static final Collection<String> BOTH_TABLES =
      Arrays.asList(TABLE_EMPLOYEE, TABLE_MANAGER);

  public TestDb(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
    super(context, name, factory, version);
  }

  interface EmployeeTable {
    String ID = "_id";
    String USERNAME = "username";
    String NAME = "name";
  }

  static final class Employee {
    static final Function<Cursor, Employee> MAPPER = new Function<Cursor, Employee>() {
      @Override public Employee apply(Cursor cursor) {
        return new Employee( //
            cursor.getString(cursor.getColumnIndexOrThrow(EmployeeTable.USERNAME)),
            cursor.getString(cursor.getColumnIndexOrThrow(EmployeeTable.NAME)));
      }
    };

    final String username;
    final String name;

    Employee(String username, String name) {
      this.username = username;
      this.name = name;
    }

    @Override public boolean equals(Object o) {
      if (o == this) return true;
      if (!(o instanceof Employee)) return false;
      Employee other = (Employee) o;
      return username.equals(other.username) && name.equals(other.name);
    }

    @Override public int hashCode() {
      return username.hashCode() * 17 + name.hashCode();
    }

    @Override public String toString() {
      return "Employee[" + username + ' ' + name + ']';
    }
  }

  interface ManagerTable {
    String ID = "_id";
    String EMPLOYEE_ID = "employee_id";
    String MANAGER_ID = "manager_id";
  }

  private static final String CREATE_EMPLOYEE = "CREATE TABLE " + TABLE_EMPLOYEE + " ("
      + EmployeeTable.ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, "
      + EmployeeTable.USERNAME + " TEXT NOT NULL UNIQUE, "
      + EmployeeTable.NAME + " TEXT NOT NULL)";
  private static final String CREATE_MANAGER = "CREATE TABLE " + TABLE_MANAGER + " ("
      + ManagerTable.ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, "
      + ManagerTable.EMPLOYEE_ID + " INTEGER NOT NULL UNIQUE REFERENCES " + TABLE_EMPLOYEE + "(" + EmployeeTable.ID + "), "
      + ManagerTable.MANAGER_ID + " INTEGER NOT NULL REFERENCES " + TABLE_EMPLOYEE + "(" + EmployeeTable.ID + "))";

  long aliceId;
  long bobId;
  long eveId;

  @Override public void onCreate(@NonNull SQLiteDatabase db) {
    db.execSQL("PRAGMA foreign_keys=ON");

    db.execSQL(CREATE_EMPLOYEE);
    db.insertWithOnConflict(TABLE_EMPLOYEE, null, employee("alice", "Alice Allison"), CONFLICT_FAIL);
    db.insertWithOnConflict(TABLE_EMPLOYEE, null, employee("bob", "Bob Bobberson"), CONFLICT_FAIL);
    db.insertWithOnConflict(TABLE_EMPLOYEE, null, employee("eve", "Eve Evenson"), CONFLICT_FAIL);

    db.execSQL(CREATE_MANAGER);
    db.insertWithOnConflict(TABLE_MANAGER, null, manager(eveId, aliceId), CONFLICT_FAIL);
  }

  static ContentValues employee(String username, String name) {
    ContentValues values = new ContentValues();
    values.put(EmployeeTable.USERNAME, username);
    values.put(EmployeeTable.NAME, name);
    return values;
  }

  static ContentValues manager(long employeeId, long managerId) {
    ContentValues values = new ContentValues();
    values.put(ManagerTable.EMPLOYEE_ID, employeeId);
    values.put(ManagerTable.MANAGER_ID, managerId);
    return values;
  }

  @Override
  public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
    throw new AssertionError();
  }
}
