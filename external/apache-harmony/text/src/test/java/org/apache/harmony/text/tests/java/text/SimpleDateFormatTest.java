/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.harmony.text.tests.java.text;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;


public class SimpleDateFormatTest extends junit.framework.TestCase {

    static SimpleDateFormat format = new SimpleDateFormat("", Locale.ENGLISH);

    static SimpleDateFormat pFormat = new SimpleDateFormat("", Locale.ENGLISH);

    static class TestFormat extends junit.framework.TestCase {
        boolean testsFailed = false;

        public TestFormat(String name) {
            super(name);
        }

        public void test(String pattern, Calendar cal, String expected,
                int field) {
            StringBuffer buffer = new StringBuffer();
            FieldPosition position = new FieldPosition(field);
            format.applyPattern(pattern);
            format.format(cal.getTime(), buffer, position);
            String result = buffer.toString();
            if (!System.getProperty("java.vendor", "None").substring(0, 3)
                    .equals("Sun")) {
                assertTrue("Wrong format: \"" + pattern + "\" expected: "
                        + expected + " result: " + result, result
                        .equals(expected));
                assertTrue("Wrong begin position: " + pattern + " expected: "
                        + expected + " field: " + field, position
                        .getBeginIndex() == 1);
                assertTrue("Wrong end position: " + pattern + " expected: "
                        + expected + " field: " + field,
                        position.getEndIndex() == result.length());
            } else {
                // Print the failure but don't use assert as this
                // will stop subsequent tests from running
                if (!result.equals(expected)) {
                    System.out
                            .println("Wrong format: \"" + pattern
                                    + "\" expected: " + expected + " result: "
                                    + result);
                    testsFailed = true;
                }
            }
        }

        public boolean testsFailed() {
            return testsFailed;
        }

        public void parse(String pattern, String input, Date expected,
                int start, int end) {
            pFormat.applyPattern(pattern);
            ParsePosition position = new ParsePosition(start);
            Date result = pFormat.parse(input, position);
            assertTrue("Wrong result: " + pattern + " input: " + input
                    + " expected: " + expected + " result: " + result, expected
                    .equals(result));
            assertTrue("Wrong end position: " + pattern + " input: " + input,
                    position.getIndex() == end);
        }

        public void verifyFormatTimezone(String timeZoneId, String expected1,
                String expected2, Date date) {
            format.setTimeZone(SimpleTimeZone.getTimeZone(timeZoneId));
            format.applyPattern("z, zzzz");
            assertEquals("Test z for TimeZone : " + timeZoneId, expected1,
                    format.format(date));

            format.applyPattern("Z, ZZZZ");
            assertEquals("Test Z for TimeZone : " + timeZoneId, expected2,
                    format.format(date));
        }
    }

    /**
     * @tests java.text.SimpleDateFormat#SimpleDateFormat()
     */
    public void test_Constructor() {
        // Test for method java.text.SimpleDateFormat()
        SimpleDateFormat f2 = new SimpleDateFormat();
        assertTrue("Wrong class", f2.getClass() == SimpleDateFormat.class);
        assertTrue("Wrong default", f2.equals(DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())));
        assertTrue("Wrong symbols", f2.getDateFormatSymbols().equals(
                new DateFormatSymbols()));
        assertTrue("Doesn't work",
                f2.format(new Date()).getClass() == String.class);
    }

    /**
     * @tests java.text.SimpleDateFormat#SimpleDateFormat(java.lang.String)
     */
    public void test_ConstructorLjava_lang_String() {
        // Test for method java.text.SimpleDateFormat(java.lang.String)
        SimpleDateFormat f2 = new SimpleDateFormat("yyyy");
        assertTrue("Wrong class", f2.getClass() == SimpleDateFormat.class);
        assertEquals("Wrong pattern", "yyyy", f2.toPattern());
        assertTrue("Wrong locale", f2.equals(new SimpleDateFormat("yyyy",
                Locale.getDefault())));
        assertTrue("Wrong symbols", f2.getDateFormatSymbols().equals(
                new DateFormatSymbols()));
        assertTrue("Doesn't work",
                f2.format(new Date()).getClass() == String.class);

        // Invalid constructor value.
        try {
            new SimpleDateFormat(
                    "this is an invalid simple date format");
            fail("Expected test_ConstructorLjava_lang_String to throw IAE.");
        } catch (IllegalArgumentException ex) {
            // expected
        } 

        // Null string value
        try {
            new SimpleDateFormat(null);
            fail("Expected test_ConstructorLjava_lang_String to throw NPE.");
        } catch (NullPointerException ex) {
            // expected
        } 
    }

    /**
     * @tests java.text.SimpleDateFormat#SimpleDateFormat(java.lang.String,
     *        java.text.DateFormatSymbols)
     */
    public void test_ConstructorLjava_lang_StringLjava_text_DateFormatSymbols() {
        // Test for method java.text.SimpleDateFormat(java.lang.String,
        // java.text.DateFormatSymbols)
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.ENGLISH);
        symbols.setEras(new String[] { "Before", "After" });
        SimpleDateFormat f2 = new SimpleDateFormat("y'y'yy", symbols);
        assertTrue("Wrong class", f2.getClass() == SimpleDateFormat.class);
        assertEquals("Wrong pattern", "y'y'yy", f2.toPattern());
        assertTrue("Wrong symbols", f2.getDateFormatSymbols().equals(symbols));
        assertTrue("Doesn't work",
                f2.format(new Date()).getClass() == String.class);
    }

    /**
     * @tests java.text.SimpleDateFormat#SimpleDateFormat(java.lang.String,
     *        java.util.Locale)
     */
    public void test_ConstructorLjava_lang_StringLjava_util_Locale() {
        // Test for method java.text.SimpleDateFormat(java.lang.String,
        // java.util.Locale)
        SimpleDateFormat f2 = new SimpleDateFormat("'yyyy' MM yy",
                Locale.GERMAN);
        assertTrue("Wrong class", f2.getClass() == SimpleDateFormat.class);
        assertEquals("Wrong pattern", "'yyyy' MM yy", f2.toPattern());
        assertTrue("Wrong symbols", f2.getDateFormatSymbols().equals(
                new DateFormatSymbols(Locale.GERMAN)));
        assertTrue("Doesn't work",
                f2.format(new Date()).getClass() == String.class);
    }

    /**
     * @tests java.text.SimpleDateFormat#applyLocalizedPattern(java.lang.String)
     */
    public void test_applyLocalizedPatternLjava_lang_String() {
        // Test for method void
        // java.text.SimpleDateFormat.applyLocalizedPattern(java.lang.String)
        SimpleDateFormat f2 = new SimpleDateFormat("y", new Locale("de", "CH"));
        f2.applyLocalizedPattern("GuMtkHmsSEDFwWahKz");
        String pattern = f2.toPattern();
//        assertTrue("Wrong pattern: " + pattern, pattern
//                .equals("GyMdkHmsSEDFwWahKz"));

        // test the new "Z" pattern char
        f2 = new SimpleDateFormat("y", new Locale("de", "CH"));
        f2.applyLocalizedPattern("G u M t Z");
        pattern = f2.toPattern();
//        assertTrue("Wrong pattern: " + pattern, pattern.equals("G y M d Z"));

        // test invalid patterns
//        try {
//            f2.applyLocalizedPattern("b");
//            fail("Expected IllegalArgumentException for pattern with invalid pattern letter: b");
//        } catch (IllegalArgumentException e) {
//        }

//        try {
//            f2.applyLocalizedPattern("y");
//            fail("Expected IllegalArgumentException for pattern with invalid pattern letter: y");
//        } catch (IllegalArgumentException e) {
//        }

        try {
            f2.applyLocalizedPattern("a '");
            fail("Expected IllegalArgumentException for pattern with unterminated quote: a '");
        } catch (IllegalArgumentException e) {
        }

        try {
            f2.applyLocalizedPattern(null);
            fail("Expected NullPointerException for null pattern");
        } catch (NullPointerException e) {
        }
    }

    /**
     * @tests java.text.SimpleDateFormat#applyPattern(java.lang.String)
     */
    public void test_applyPatternLjava_lang_String() {
        // Test for method void
        // java.text.SimpleDateFormat.applyPattern(java.lang.String)
        SimpleDateFormat f2 = new SimpleDateFormat("y", new Locale("de", "CH"));
        f2.applyPattern("GyMdkHmsSEDFwWahKz");
        assertEquals("Wrong pattern", "GyMdkHmsSEDFwWahKz", f2.toPattern());

        // test invalid patterns
        try {
            f2.applyPattern("b");
            fail("Expected IllegalArgumentException for pattern with invalid patter letter: b");
        } catch (IllegalArgumentException e) {
        }

//        try {
//            f2.applyPattern("u");
//            fail("Expected IllegalArgumentException for pattern with invalid patter letter: u");
//        } catch (IllegalArgumentException e) {
//        }

        try {
            f2.applyPattern("a '");
            fail("Expected IllegalArgumentException for pattern with unterminated quote: a '");
        } catch (IllegalArgumentException e) {
        }

        try {
            f2.applyPattern(null);
            fail("Expected NullPointerException for null pattern");
        } catch (NullPointerException e) {
        }
    }

    /**
     * @tests java.text.SimpleDateFormat#clone()
     */
    public void test_clone() {
        // Test for method java.lang.Object java.text.SimpleDateFormat.clone()
        SimpleDateFormat f2 = new SimpleDateFormat();
        SimpleDateFormat clone = (SimpleDateFormat) f2.clone();
        assertTrue("Invalid clone", f2.equals(clone));
        clone.applyPattern("y");
        assertTrue("Format modified", !f2.equals(clone));
        clone = (SimpleDateFormat) f2.clone();
        // Date date = clone.get2DigitYearStart();
        // date.setTime(0);
        // assertTrue("Equal after date change: " +
        // f2.get2DigitYearStart().getTime() + " " +
        // clone.get2DigitYearStart().getTime(), !f2.equals(clone));
    }

    /**
     * @tests java.text.SimpleDateFormat#equals(java.lang.Object)
     */
    public void test_equalsLjava_lang_Object() {
        // Test for method boolean
        // java.text.SimpleDateFormat.equals(java.lang.Object)
        SimpleDateFormat format = (SimpleDateFormat) DateFormat.getInstance();
        SimpleDateFormat clone = (SimpleDateFormat) format.clone();
        assertTrue("clone not equal", format.equals(clone));
        format.format(new Date());
        assertTrue("not equal after format", format.equals(clone));
    }

    public void test_equals_afterFormat() {
        // Regression test for HARMONY-209
        SimpleDateFormat df = new SimpleDateFormat();
        df.format(new Date());
        assertEquals(df, new SimpleDateFormat());
    }

    /**
     * @tests java.text.SimpleDateFormat#formatToCharacterIterator(java.lang.Object)
     */
    public void test_formatToCharacterIteratorLjava_lang_Object() {

        try {
            // Regression for HARMONY-466
            new SimpleDateFormat().formatToCharacterIterator(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
        
        // Test for method formatToCharacterIterator(java.lang.Object)
        new Support_SimpleDateFormat(
                "test_formatToCharacterIteratorLjava_lang_Object")
                .t_formatToCharacterIterator();
    }

    /**
     * @tests java.text.SimpleDateFormat#format(java.util.Date,
     *        java.lang.StringBuffer, java.text.FieldPosition)
     */
    public void test_formatLjava_util_DateLjava_lang_StringBufferLjava_text_FieldPosition() {
        // Test for method java.lang.StringBuffer
        // java.text.SimpleDateFormat.format(java.util.Date,
        // java.lang.StringBuffer, java.text.FieldPosition)

        new Support_SimpleDateFormat(
                "test_formatLjava_util_DateLjava_lang_StringBufferLjava_text_FieldPosition")
                .t_format_with_FieldPosition();

        TestFormat test = new TestFormat(
                "test_formatLjava_util_DateLjava_lang_StringBufferLjava_text_FieldPosition");

        Calendar cal = new GregorianCalendar(1999, Calendar.JUNE, 2, 15, 3, 6);
        test.test(" G", cal, " AD", DateFormat.ERA_FIELD);
        test.test(" GG", cal, " AD", DateFormat.ERA_FIELD);
        test.test(" GGG", cal, " AD", DateFormat.ERA_FIELD);
        test.test(" G", new GregorianCalendar(-1999, Calendar.JUNE, 2), " BC",
                DateFormat.ERA_FIELD);

        test.test(" y", cal, " 99", DateFormat.YEAR_FIELD);
        test.test(" yy", cal, " 99", DateFormat.YEAR_FIELD);
        test.test(" yy", new GregorianCalendar(2001, Calendar.JUNE, 2), " 01",
                DateFormat.YEAR_FIELD);
        test.test(" yy", new GregorianCalendar(2000, Calendar.JUNE, 2), " 00",
                DateFormat.YEAR_FIELD);
        test.test(" yyy", new GregorianCalendar(2000, Calendar.JUNE, 2), " 2000",
                DateFormat.YEAR_FIELD);
        test.test(" yyy", cal, " 1999", DateFormat.YEAR_FIELD);
        test.test(" yyyy", cal, " 1999", DateFormat.YEAR_FIELD);
        test.test(" yyyyy", cal, " 01999", DateFormat.YEAR_FIELD);

        test.test(" M", cal, " 6", DateFormat.MONTH_FIELD);
        test.test(" M", new GregorianCalendar(1999, Calendar.NOVEMBER, 2),
                " 11", DateFormat.MONTH_FIELD);
        test.test(" MM", cal, " 06", DateFormat.MONTH_FIELD);
        test.test(" MMM", cal, " Jun", DateFormat.MONTH_FIELD);
        test.test(" MMMM", cal, " June", DateFormat.MONTH_FIELD);
        // It is an ICU defect.
        //test.test(" MMMMM", cal, " June", DateFormat.MONTH_FIELD);

        test.test(" d", cal, " 2", DateFormat.DATE_FIELD);
        test.test(" d", new GregorianCalendar(1999, Calendar.NOVEMBER, 12),
                " 12", DateFormat.DATE_FIELD);
        test.test(" dd", cal, " 02", DateFormat.DATE_FIELD);
        test.test(" dddd", cal, " 0002", DateFormat.DATE_FIELD);

        test.test(" h", cal, " 3", DateFormat.HOUR1_FIELD);
        test.test(" h", new GregorianCalendar(1999, Calendar.NOVEMBER, 12),
                " 12", DateFormat.HOUR1_FIELD);
        test.test(" hh", cal, " 03", DateFormat.HOUR1_FIELD);
        test.test(" hhhh", cal, " 0003", DateFormat.HOUR1_FIELD);

        test.test(" H", cal, " 15", DateFormat.HOUR_OF_DAY0_FIELD);
        test.test(" H",
                new GregorianCalendar(1999, Calendar.NOVEMBER, 12, 4, 0), " 4",
                DateFormat.HOUR_OF_DAY0_FIELD);
        test.test(" H", new GregorianCalendar(1999, Calendar.NOVEMBER, 12, 12,
                0), " 12", DateFormat.HOUR_OF_DAY0_FIELD);
        test.test(" H", new GregorianCalendar(1999, Calendar.NOVEMBER, 12),
                " 0", DateFormat.HOUR_OF_DAY0_FIELD);
        test.test(" HH", cal, " 15", DateFormat.HOUR_OF_DAY0_FIELD);
        test.test(" HHHH", cal, " 0015", DateFormat.HOUR_OF_DAY0_FIELD);

        test.test(" m", cal, " 3", DateFormat.MINUTE_FIELD);
        test.test(" m", new GregorianCalendar(1999, Calendar.NOVEMBER, 12, 4,
                47), " 47", DateFormat.MINUTE_FIELD);
        test.test(" mm", cal, " 03", DateFormat.MINUTE_FIELD);
        test.test(" mmmm", cal, " 0003", DateFormat.MINUTE_FIELD);

        test.test(" s", cal, " 6", DateFormat.SECOND_FIELD);
        test.test(" s", new GregorianCalendar(1999, Calendar.NOVEMBER, 12, 4,
                47, 13), " 13", DateFormat.SECOND_FIELD);
        test.test(" ss", cal, " 06", DateFormat.SECOND_FIELD);
        test.test(" ssss", cal, " 0006", DateFormat.SECOND_FIELD);

        test.test(" S", cal, " 0", DateFormat.MILLISECOND_FIELD);
        Calendar temp = new GregorianCalendar();
        temp.set(Calendar.MILLISECOND, 961);

        test.test(" SS", temp, " 96", DateFormat.MILLISECOND_FIELD);
        test.test(" SSSS", cal, " 0000", DateFormat.MILLISECOND_FIELD);

        test.test(" SS", cal, " 00", DateFormat.MILLISECOND_FIELD);

        test.test(" E", cal, " Wed", DateFormat.DAY_OF_WEEK_FIELD);
        test.test(" EE", cal, " Wed", DateFormat.DAY_OF_WEEK_FIELD);
        test.test(" EEE", cal, " Wed", DateFormat.DAY_OF_WEEK_FIELD);
        test.test(" EEEE", cal, " Wednesday", DateFormat.DAY_OF_WEEK_FIELD);
        // ICU defect
        //test.test(" EEEEE", cal, " Wednesday", DateFormat.DAY_OF_WEEK_FIELD);

        test.test(" D", cal, " 153", DateFormat.DAY_OF_YEAR_FIELD);
        test.test(" DD", cal, " 153", DateFormat.DAY_OF_YEAR_FIELD);
        test.test(" DDDD", cal, " 0153", DateFormat.DAY_OF_YEAR_FIELD);

        test.test(" F", cal, " 1", DateFormat.DAY_OF_WEEK_IN_MONTH_FIELD);
        test.test(" F", new GregorianCalendar(1999, Calendar.NOVEMBER, 14),
                " 2", DateFormat.DAY_OF_WEEK_IN_MONTH_FIELD);
        test.test(" FF", cal, " 01", DateFormat.DAY_OF_WEEK_IN_MONTH_FIELD);
        test.test(" FFFF", cal, " 0001", DateFormat.DAY_OF_WEEK_IN_MONTH_FIELD);

        test.test(" w", cal, " 23", DateFormat.WEEK_OF_YEAR_FIELD);
        test.test(" ww", cal, " 23", DateFormat.WEEK_OF_YEAR_FIELD);
        test.test(" wwww", cal, " 0023", DateFormat.WEEK_OF_YEAR_FIELD);

        test.test(" W", cal, " 1", DateFormat.WEEK_OF_MONTH_FIELD);
        test.test(" W", new GregorianCalendar(1999, Calendar.NOVEMBER, 14),
                " 2", DateFormat.WEEK_OF_MONTH_FIELD);
        test.test(" WW", cal, " 01", DateFormat.WEEK_OF_MONTH_FIELD);
        test.test(" WWWW", cal, " 0001", DateFormat.WEEK_OF_MONTH_FIELD);

        test.test(" a", cal, " PM", DateFormat.AM_PM_FIELD);
        test.test(" a", new GregorianCalendar(1999, Calendar.NOVEMBER, 14),
                " AM", DateFormat.AM_PM_FIELD);
        test.test(" a", new GregorianCalendar(1999, Calendar.NOVEMBER, 14, 12,
                0), " PM", DateFormat.AM_PM_FIELD);
        test.test(" aa", cal, " PM", DateFormat.AM_PM_FIELD);
        test.test(" aaa", cal, " PM", DateFormat.AM_PM_FIELD);
        test.test(" aaaa", cal, " PM", DateFormat.AM_PM_FIELD);
        test.test(" aaaaa", cal, " PM", DateFormat.AM_PM_FIELD);

        test.test(" k", cal, " 15", DateFormat.HOUR_OF_DAY1_FIELD);
        test.test(" k",
                new GregorianCalendar(1999, Calendar.NOVEMBER, 12, 4, 0), " 4",
                DateFormat.HOUR_OF_DAY1_FIELD);
        test.test(" k", new GregorianCalendar(1999, Calendar.NOVEMBER, 12, 12,
                0), " 12", DateFormat.HOUR_OF_DAY1_FIELD);
        test.test(" k", new GregorianCalendar(1999, Calendar.NOVEMBER, 12),
                " 24", DateFormat.HOUR_OF_DAY1_FIELD);
        test.test(" kk", cal, " 15", DateFormat.HOUR_OF_DAY1_FIELD);
        test.test(" kkkk", cal, " 0015", DateFormat.HOUR_OF_DAY1_FIELD);

        test.test(" K", cal, " 3", DateFormat.HOUR0_FIELD);
        test.test(" K", new GregorianCalendar(1999, Calendar.NOVEMBER, 12),
                " 0", DateFormat.HOUR0_FIELD);
        test.test(" KK", cal, " 03", DateFormat.HOUR0_FIELD);
        test.test(" KKKK", cal, " 0003", DateFormat.HOUR0_FIELD);

        format.setTimeZone(TimeZone.getTimeZone("EST"));
        test.test(" z", cal, " GMT-05:00", DateFormat.TIMEZONE_FIELD);
        Calendar temp2 = new GregorianCalendar(1999, Calendar.JANUARY, 12);
        test.test(" z", temp2, " GMT-05:00", DateFormat.TIMEZONE_FIELD);
        test.test(" zz", cal, " GMT-05:00", DateFormat.TIMEZONE_FIELD);
        test.test(" zzz", cal, " GMT-05:00", DateFormat.TIMEZONE_FIELD);
        test.test(" zzzz", cal, " GMT-05:00",
                DateFormat.TIMEZONE_FIELD);
        test.test(" zzzz", temp2, " GMT-05:00",
                DateFormat.TIMEZONE_FIELD);
        test.test(" zzzzz", cal, " GMT-05:00",
                DateFormat.TIMEZONE_FIELD);

        format.setTimeZone(new SimpleTimeZone(60000, "ONE MINUTE"));
        test.test(" z", cal, " GMT+00:00", DateFormat.TIMEZONE_FIELD);
        test.test(" zzzz", cal, " GMT+00:00", DateFormat.TIMEZONE_FIELD);
        format.setTimeZone(new SimpleTimeZone(5400000, "ONE HOUR, THIRTY"));
        test.test(" z", cal, " GMT+00:00", DateFormat.TIMEZONE_FIELD);
        format
                .setTimeZone(new SimpleTimeZone(-5400000,
                        "NEG ONE HOUR, THIRTY"));
        test.test(" z", cal, " GMT+00:00", DateFormat.TIMEZONE_FIELD);

        format.applyPattern("'Mkz''':.@5");
        assertEquals("Wrong output", "Mkz':.@5", format.format(new Date()));

        assertTrue("Tests failed", !test.testsFailed());

        // Test invalid args to format.
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        try {
            dateFormat.format(null, new StringBuffer(), new FieldPosition(1));
            fail("Expected test to throw NPE.");
        } catch (NullPointerException ex) {
            // expected
        } 
    }

    /**
     * @tests java.text.SimpleDateFormat#format(java.util.Date)
     */
    public void test_timeZoneFormatting() {
        // tests specific to formatting of timezones
        Date summerDate = new GregorianCalendar(1999, Calendar.JUNE, 2, 15, 3,
                6).getTime();
        Date winterDate = new GregorianCalendar(1999, Calendar.JANUARY, 12)
                .getTime();

        TestFormat test = new TestFormat(
                "test_formatLjava_util_DateLjava_lang_StringBufferLjava_text_FieldPosition");

        test.verifyFormatTimezone("PST", "PDT, Pacific Daylight Time",
                "-0700, GMT-07:00", summerDate);
        test.verifyFormatTimezone("PST", "PST, Pacific Standard Time",
                "-0800, GMT-08:00", winterDate);

        test.verifyFormatTimezone("GMT-7", "GMT-07:00, GMT-07:00",
                "-0700, GMT-07:00", summerDate);
        test.verifyFormatTimezone("GMT-7", "GMT-07:00, GMT-07:00",
                "-0700, GMT-07:00", winterDate);

        // Pacific/Kiritimati is one of the timezones supported only in mJava
        test.verifyFormatTimezone("Pacific/Kiritimati", "GMT+14:00, Line Islands Time",
                "+1400, GMT+14:00", summerDate);
        test.verifyFormatTimezone("Pacific/Kiritimati", "GMT+14:00, Line Islands Time",
                "+1400, GMT+14:00", winterDate);

        test.verifyFormatTimezone("EST", "GMT-05:00, GMT-05:00",
                "-0500, GMT-05:00", summerDate);
        test.verifyFormatTimezone("EST", "GMT-05:00, GMT-05:00",
                "-0500, GMT-05:00", winterDate);

        test.verifyFormatTimezone("GMT+14", "GMT+14:00, GMT+14:00",
                "+1400, GMT+14:00", summerDate);
        test.verifyFormatTimezone("GMT+14", "GMT+14:00, GMT+14:00",
                "+1400, GMT+14:00", winterDate);
    }

    /**
     * @tests java.text.SimpleDateFormat#get2DigitYearStart()
     */
    public void test_get2DigitYearStart() {
        // Test for method java.util.Date
        // java.text.SimpleDateFormat.get2DigitYearStart()
        SimpleDateFormat f1 = new SimpleDateFormat("y");
        Date date = f1.get2DigitYearStart();
        Calendar cal = new GregorianCalendar();
        int year = cal.get(Calendar.YEAR);
        cal.setTime(date);
        assertTrue("Wrong default year start",
                cal.get(Calendar.YEAR) == (year - 80));
    }

    /**
     * @tests java.text.SimpleDateFormat#getDateFormatSymbols()
     */
    public void test_getDateFormatSymbols() {
        // Test for method java.text.DateFormatSymbols
        // java.text.SimpleDateFormat.getDateFormatSymbols()
        SimpleDateFormat df = (SimpleDateFormat) DateFormat.getInstance();
        DateFormatSymbols dfs = df.getDateFormatSymbols();
        assertTrue("Symbols identical", dfs != df.getDateFormatSymbols());
    }

    /**
     * @tests java.text.SimpleDateFormat#parse(java.lang.String,
     *        java.text.ParsePosition)
     */
    public void test_parseLjava_lang_StringLjava_text_ParsePosition() {
        // Test for method java.util.Date
        // java.text.SimpleDateFormat.parse(java.lang.String,
        // java.text.ParsePosition)
        TestFormat test = new TestFormat(
                "test_formatLjava_util_DateLjava_lang_StringBufferLjava_text_FieldPosition");

        Calendar cal = new GregorianCalendar(1970, Calendar.JANUARY, 1);
        Date time = cal.getTime();
        test.parse("h", " 12", time, 1, 3);
        test.parse("H", " 0", time, 1, 2);
        test.parse("k", " 24", time, 1, 3);
        test.parse("K", " 0", time, 1, 2);

        cal = new GregorianCalendar(1970, Calendar.JANUARY, 1, 1, 0);
        time = cal.getTime();
        test.parse("h", "1", time, 0, 1);
        test.parse("H", "1 ", time, 0, 1);
        test.parse("k", "1", time, 0, 1);
        test.parse("K", "1", time, 0, 1);

        cal = new GregorianCalendar(1970, Calendar.JANUARY, 1, 11, 0);
        time = cal.getTime();
        test.parse("h", "0011 ", time, 0, 4);
        test.parse("K", "11", time, 0, 2);
        cal = new GregorianCalendar(1970, Calendar.JANUARY, 1, 23, 0);
        time = cal.getTime();
        test.parse("H", "23", time, 0, 2);
        test.parse("k", "23", time, 0, 2);

        test.parse("h a", " 3 AM", new GregorianCalendar(1970,
                Calendar.JANUARY, 1, 3, 0).getTime(), 1, 5);
        test.parse("K a", " 3 pm ", new GregorianCalendar(1970,
                Calendar.JANUARY, 1, 15, 0).getTime(), 1, 5);
        test.parse("m:s", "0:59 ", new GregorianCalendar(1970,
                Calendar.JANUARY, 1, 0, 0, 59).getTime(), 0, 4);
        test.parse("m:s", "59:0", new GregorianCalendar(1970, Calendar.JANUARY,
                1, 0, 59, 0).getTime(), 0, 4);
        test.parse("ms", "059", new GregorianCalendar(1970, Calendar.JANUARY,
                1, 0, 0, 59).getTime(), 0, 3);

        cal = new GregorianCalendar(1970, Calendar.JANUARY, 1);
        test.parse("S", "0", cal.getTime(), 0, 1);
        cal.setTimeZone(TimeZone.getTimeZone("HST"));
        cal.set(Calendar.MILLISECOND, 999);
        test.parse("S z", "999 HST", cal.getTime(), 0, 7);

        cal = new GregorianCalendar(1970, Calendar.JANUARY, 1);
        cal.set(Calendar.ERA, GregorianCalendar.BC);
        test.parse("G", "Bc ", cal.getTime(), 0, 2);

        test.parse("y", "00", new GregorianCalendar(2000, Calendar.JANUARY, 1)
                .getTime(), 0, 2);
        test.parse("y", "99", new GregorianCalendar(1999, Calendar.JANUARY, 1)
                .getTime(), 0, 2);
        test.parse("y", "1", new GregorianCalendar(1, Calendar.JANUARY, 1)
                .getTime(), 0, 1);
        test.parse("y", "-1", new GregorianCalendar(-1, Calendar.JANUARY, 1)
                .getTime(), 0, 2);
        test.parse("y", "001", new GregorianCalendar(1, Calendar.JANUARY, 1)
                .getTime(), 0, 3);
        test.parse("y", "2005",
                new GregorianCalendar(2005, Calendar.JANUARY, 1).getTime(), 0,
                4);
        test.parse("yy", "00", new GregorianCalendar(2000, Calendar.JANUARY, 1)
                .getTime(), 0, 2);
        test.parse("yy", "99", new GregorianCalendar(1999, Calendar.JANUARY, 1)
                .getTime(), 0, 2);
        test.parse("yy", "1", new GregorianCalendar(1, Calendar.JANUARY, 1)
                .getTime(), 0, 1);
        test.parse("yy", "-1", new GregorianCalendar(-1, Calendar.JANUARY, 1)
                .getTime(), 0, 2);
        test.parse("yy", "001", new GregorianCalendar(1, Calendar.JANUARY, 1)
                .getTime(), 0, 3);
        test.parse("yy", "2005", new GregorianCalendar(2005, Calendar.JANUARY,
                1).getTime(), 0, 4);
        test.parse("yyy", "99", new GregorianCalendar(99, Calendar.JANUARY, 1)
                .getTime(), 0, 2);
        test.parse("yyy", "1", new GregorianCalendar(1, Calendar.JANUARY, 1)
                .getTime(), 0, 1);
        test.parse("yyy", "-1", new GregorianCalendar(-1, Calendar.JANUARY, 1)
                .getTime(), 0, 2);
        test.parse("yyy", "001", new GregorianCalendar(1, Calendar.JANUARY, 1)
                .getTime(), 0, 3);
        test.parse("yyy", "2005", new GregorianCalendar(2005, Calendar.JANUARY,
                1).getTime(), 0, 4);
        test.parse("yyyy", "99", new GregorianCalendar(99, Calendar.JANUARY, 1)
                .getTime(), 0, 2);
        test.parse("yyyy", "  1999", new GregorianCalendar(1999,
                Calendar.JANUARY, 1).getTime(), 2, 6);
        test.parse("MM'M'", "4M",
                new GregorianCalendar(1970, Calendar.APRIL, 1).getTime(), 0, 2);
        test.parse("MMM", "Feb", new GregorianCalendar(1970, Calendar.FEBRUARY,
                1).getTime(), 0, 3);
        test.parse("MMMM d", "April 14 ", new GregorianCalendar(1970,
                Calendar.APRIL, 14).getTime(), 0, 8);
        test.parse("MMMMd", "April14 ", new GregorianCalendar(1970,
                Calendar.APRIL, 14).getTime(), 0, 7);
        test.parse("E w", "Mon 12", new GregorianCalendar(1970, Calendar.MARCH,
                16).getTime(), 0, 6);
        test.parse("Ew", "Mon12", new GregorianCalendar(1970, Calendar.MARCH,
                16).getTime(), 0, 5);
        test.parse("M EE ''W", "5 Tue '2", new GregorianCalendar(1970,
                Calendar.MAY, 5).getTime(), 0, 8);
        test.parse("MEE''W", "5Tue'2", new GregorianCalendar(1970,
                Calendar.MAY, 5).getTime(), 0, 6);
        test.parse("MMM EEE F", " JUL Sunday 3", new GregorianCalendar(1970,
                Calendar.JULY, 19).getTime(), 1, 13);
        test.parse("MMMEEEF", " JULSunday3", new GregorianCalendar(1970,
                Calendar.JULY, 19).getTime(), 1, 11);

        cal = new GregorianCalendar(1970, Calendar.JANUARY, 1);
        cal.setTimeZone(TimeZone.getTimeZone("GMT+0:1"));
        cal.set(Calendar.DAY_OF_YEAR, 243);
        test.parse("D z", "243 GMT+0:0", cal.getTime(), 0, 11);
        cal.setTimeZone(TimeZone.getTimeZone("EST"));
        cal.set(1970, Calendar.JANUARY, 1, 4, 30);
        test.parse("h:m z", "4:30 GMT-5 ", cal.getTime(), 0, 10);
        test.parse("h z", "14 GMT-24 ", new Date(51840000), 0, 9);
        test.parse("h z", "14 GMT-23 ", new Date(133200000), 0, 9);
        test.parse("h z", "14 GMT-0001 ", new Date(54000000), 0, 11);
        test.parse("h z", "14 GMT+24 ", new Date(48960000), 0, 9);
        test.parse("h z", "14 GMT+23 ", new Date(-32400000), 0, 9);
        test.parse("h z", "14 GMT+0001 ", new Date(46800000), 0, 11);
        test.parse("h z", "14 +0001 ", new Date(46800000), 0, 8);
        test.parse("h z", "14 -0001 ", new Date(54000000), 0, 8);

        test.parse("yyyyMMddHHmmss", "19990913171901", new GregorianCalendar(
                1999, Calendar.SEPTEMBER, 13, 17, 19, 01).getTime(), 0, 14);

        Date d = new Date(1015822800000L);
        SimpleDateFormat df = new SimpleDateFormat("", new Locale("en", "US"));
        df.setTimeZone(TimeZone.getTimeZone("EST"));

        try {
            df.applyPattern("dd MMMM yyyy EEEE");
            String output = df.format(d);
            Date date = df.parse(output);
            assertTrue("Invalid result 1: " + date, d.equals(date));

            df.applyPattern("dd MMMM yyyy F");
            output = df.format(d);
            date = df.parse(output);
            assertTrue("Invalid result 2: " + date, d.equals(date));

            df.applyPattern("dd MMMM yyyy w");
            output = df.format(d);
            date = df.parse(output);
            assertTrue("Invalid result 3: " + date, d.equals(date));

            df.applyPattern("dd MMMM yyyy W");
            output = df.format(d);
            date = df.parse(output);
            assertTrue("Invalid result 4: " + date, d.equals(date));

            df.applyPattern("dd MMMM yyyy D");
            date = df.parse("5 January 2002 70");
            assertTrue("Invalid result 5: " + date, d.equals(date));

            df.applyPattern("W w dd MMMM yyyy EEEE");
            output = df.format(d);
            date = df.parse("3 12 5 March 2002 Monday");
            assertTrue("Invalid result 6: " + date, d.equals(date));

            df.applyPattern("w W dd MMMM yyyy EEEE");
            output = df.format(d);
            date = df.parse("12 3 5 March 2002 Monday");
            assertTrue("Invalid result 6a: " + date, d.equals(date));

            df.applyPattern("F dd MMMM yyyy EEEE");
            output = df.format(d);
            date = df.parse("2 5 March 2002 Monday");
            assertTrue("Invalid result 7: " + date, d.equals(date));

            df.applyPattern("w dd MMMM yyyy EEEE");
            output = df.format(d);
            date = df.parse("11 5 January 2002 Monday");
            assertTrue("Invalid result 8: " + date, d.equals(date));

            df.applyPattern("w dd yyyy EEEE MMMM");
            output = df.format(d);
            date = df.parse("11 5 2002 Monday January");
            assertTrue("Invalid result 9: " + date, d.equals(date));

            df.applyPattern("w yyyy EEEE MMMM dd");
            output = df.format(d);
            date = df.parse("17 2002 Monday March 11");
            assertTrue("Invalid result 10: " + date, d.equals(date));

            df.applyPattern("dd D yyyy MMMM");
            output = df.format(d);
            date = df.parse("5 70 2002 January");
            assertTrue("Invalid result 11: " + date, d.equals(date));

            df.applyPattern("D dd yyyy MMMM");
            output = df.format(d);
            date = df.parse("240 11 2002 March");
            assertTrue("Invalid result 12: " + date, d.equals(date));
        } catch (ParseException e) {
            fail("unexpected: " + e);
        }
    }

    /**
     * @tests java.text.SimpleDateFormat#set2DigitYearStart(java.util.Date)
     */
    public void test_set2DigitYearStartLjava_util_Date() {
        // Test for method void
        // java.text.SimpleDateFormat.set2DigitYearStart(java.util.Date)
        SimpleDateFormat f1 = new SimpleDateFormat("yy");
        f1.set2DigitYearStart(new GregorianCalendar(1950, Calendar.JANUARY, 1)
                .getTime());
        Calendar cal = new GregorianCalendar();
        try {
            cal.setTime(f1.parse("49"));
            assertEquals("Incorrect year 2049", 2049, cal.get(Calendar.YEAR));
            cal.setTime(f1.parse("50"));
            int year = cal.get(Calendar.YEAR);
            assertTrue("Incorrect year 1950: " + year, year == 1950);
            f1.applyPattern("y");
            cal.setTime(f1.parse("00"));
            assertEquals("Incorrect year 2000", 2000, cal.get(Calendar.YEAR));
            f1.applyPattern("yyy");
            cal.setTime(f1.parse("50"));
            assertEquals("Incorrect year 50", 50, cal.get(Calendar.YEAR));
        } catch (ParseException e) {
            fail("ParseException");
        }
    }

    /**
     * @tests java.text.SimpleDateFormat#setDateFormatSymbols(java.text.DateFormatSymbols)
     */
    public void test_setDateFormatSymbolsLjava_text_DateFormatSymbols() {
        // Test for method void
        // java.text.SimpleDateFormat.setDateFormatSymbols(java.text.DateFormatSymbols)
        SimpleDateFormat f1 = new SimpleDateFormat("a");
        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setAmPmStrings(new String[] { "morning", "night" });
        f1.setDateFormatSymbols(symbols);
        DateFormatSymbols newSym = f1.getDateFormatSymbols();
        assertTrue("Set incorrectly", newSym.equals(symbols));
        assertTrue("Not a clone", f1.getDateFormatSymbols() != symbols);
        String result = f1.format(new GregorianCalendar(1999, Calendar.JUNE,
                12, 3, 0).getTime());
        assertEquals("Incorrect symbols used", "morning", result);
        symbols.setEras(new String[] { "before", "after" });
        assertTrue("Identical symbols", !f1.getDateFormatSymbols().equals(
                symbols));
    }

    /**
     * @tests java.text.SimpleDateFormat#toLocalizedPattern()
     */
    public void test_toLocalizedPattern() {
        // Test for method java.lang.String
        // java.text.SimpleDateFormat.toLocalizedPattern()
        SimpleDateFormat f2 = new SimpleDateFormat("GyMdkHmsSEDFwWahKz",
                new Locale("de", "CH"));
        String pattern = f2.toLocalizedPattern();
        assertTrue("Wrong pattern: " + pattern, pattern
                .equals("GuMtkHmsSEDFwWahKz"));

        // test the new "Z" pattern char
        f2 = new SimpleDateFormat("G y M d Z", new Locale("de", "CH"));
        pattern = f2.toLocalizedPattern();
        assertTrue("Wrong pattern: " + pattern, pattern.equals("G u M t Z"));
    }

    /**
     * @tests java.text.SimpleDateFormat#parse(java.lang.String,
     *        java.text.ParsePosition)
     */
    public void test_parse_with_spaces() {
        // Regression for HARMONY-502
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        df.setLenient(false);

        char allowed_chars[] = { 0x9, 0x20 };
        String allowed_char_names[] = { "tab", "space" };
        for (int i = 0; i < allowed_chars.length; i++) {
            Date expected = new GregorianCalendar(1970, Calendar.JANUARY, 1, 9,
                    7, 6).getTime();
            ParsePosition pp = new ParsePosition(0);
            Date d = df.parse(allowed_chars[i] + "9:07:06", pp);
            assertNotNull("hour may be prefixed by " + allowed_char_names[i], d);
            assertEquals(expected, d);

            pp = new ParsePosition(0);
            d = df.parse("09:" + allowed_chars[i] + "7:06", pp);
            assertNotNull("minute may be prefixed by " + allowed_char_names[i],
                    d);
            assertEquals(expected, d);

            pp = new ParsePosition(0);
            d = df.parse("09:07:" + allowed_chars[i] + "6", pp);
            assertNotNull("second may be prefixed by " + allowed_char_names[i],
                    d);
            assertEquals(expected, d);
        }

        char not_allowed_chars[] = {
        // whitespace
                0x1c, 0x1d, 0x1e, 0x1f, 0xa, 0xb, 0xc, 0xd, 0x2001, 0x2002,
                0x2003, 0x2004, 0x2005, 0x2006, 0x2008, 0x2009, 0x200a, 0x200b,
                0x2028, 0x2029, 0x3000,
                // non-breaking space
                0xA0, 0x2007, 0x202F };

        for (int i = 0; i < not_allowed_chars.length; i++) {
            ParsePosition pp = new ParsePosition(0);
            Date d = df.parse(not_allowed_chars[i] + "9:07", pp);
            assertNull(d);

            pp = new ParsePosition(0);
            d = df.parse("09:" + not_allowed_chars[i] + "7", pp);
            assertNull(d);

            pp = new ParsePosition(0);
            d = df.parse("09:07:" + not_allowed_chars[i] + "6", pp);
            assertNull(d);
        }
    }
}
